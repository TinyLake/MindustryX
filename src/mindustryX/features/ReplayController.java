package mindustryX.features;

import arc.*;
import arc.files.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.ui.dialogs.*;
import mindustryX.VarsX;
import mindustryX.features.SettingsV2.*;

import java.io.*;
import java.util.*;

import static mindustry.Vars.*;

/**
 * 回放录制
 * 原作者 cong0707, 原文件路径 mindustry.arcModule.ReplayController
 * WayZer修改优化
 */
public class ReplayController{
    private static final CheckPref enable = new CheckPref("replayRecord");

    public static boolean replaying;

    private static ReplayData.Writer writer;
    private static ReplayData.Reader reader;

    public static void init(){
        Events.run(EventType.Trigger.update, () -> {
            if(replaying && state.isMenu() && !netClient.isConnecting()){
                stopPlay();
            }
        });
        Events.on(ClientServerConnectEvent.class, (e) -> stopPlay());
        {
            Table buttons = Vars.ui.join.buttons;
            buttons.button(mindustryX.bundles.UiTextBundle.i("加载回放文件"), Icon.file, () -> { // 原文本:加载回放文件
                FileChooser.setLastDirectory(saveDirectory);
                platform.showFileChooser(true, mindustryX.bundles.UiTextBundle.i("打开回放文件"), "mrep", f -> Core.app.post(() -> ReplayController.startPlay(f))); // 原文本:打开回放文件
            });
        }
        {
            var pausedDialog = Vars.ui.paused;
            pausedDialog.shown(() -> {
                if(!replaying) return;
                pausedDialog.cont.row()
                .button(mindustryX.bundles.UiTextBundle.i("查看录制信息"), Icon.fileImage, ReplayController::showInfo).name("ReplayInfo") // 原文本:查看录制信息
                .size(0, 60).colspan(pausedDialog.cont.getColumns()).fill();
            });
        }
    }

    public static void onConnect(String ip){
        if(!enable.get() || LogicExt.contentsCompatibleMode) return;
        if(replaying) return;
        var file = saveDirectory.child(new Date().getTime() + ".mrep");
        ReplayData.Writer writer;
        try{
            writer = new ReplayData.Writer(file.write(false, 8192));
        }catch(Exception e){
            Log.err(mindustryX.bundles.UiTextBundle.i("创建回放出错!"), e); // 原文本:创建回放出错!
            return;
        }
        boolean anonymous = Core.settings.getBool("anonymous", false);
        ReplayData header = new ReplayData(Version.build, new Date(), anonymous ? "anonymous" : ip, anonymous ? "anonymous" : Vars.player.name.trim());
        writer.writeHeader(header);
        Log.info(VarsX.getUiTextBundle().recording(file.absolutePath()));
        ReplayController.writer = writer;
    }

    public static void onClientPacket(Packet p){
        if(writer == null) return;
        if(p instanceof Disconnect){
            writer.close();
            writer = null;
            Log.info(mindustryX.bundles.UiTextBundle.i("录制结束")); // 原文本:录制结束
            return;
        }
        try{
            writer.writePacket(p);
        }catch(Exception e){
            net.disconnect();
            Log.err(e);
            Core.app.post(() -> ui.showException(mindustryX.bundles.UiTextBundle.i("录制出错!"), e)); // 原文本:录制出错!
        }
    }

    //replay

    public static void startPlay(Fi input){
        try{
            reader = new ReplayData.Reader(input);
            Log.infoTag("Replay", reader.getMeta().toString());
        }catch(Exception e){
            Core.app.post(() -> ui.showException(mindustryX.bundles.UiTextBundle.i("读取回放失败!"), e)); // 原文本:读取回放失败!
        }

        replaying = true;
        ui.loadfrag.show("@connecting");
        ui.loadfrag.setButton(ReplayController::stopPlay);

        logic.reset();
        net.reset();
        netClient.beginConnecting();
        Reflect.set(net, "active", true);

        Threads.daemon("Replay Controller", () -> {
            float startTime = Time.time;
            try{
                while(replaying){
                    var info = reader.nextPacket();
                    Packet packet = reader.readPacket(info);
                    while(Time.time - startTime < info.getOffset())
                        Thread.sleep(1);
                    Core.app.post(() -> {
                        if(!replaying) return;
                        try{
                            net.handleClientReceived(packet);
                        }catch(Exception e){
                            stopPlay();
                            net.handleException(e);
                        }
                    });
                }
            }catch(EOFException e){
                replaying = false;
                showInfo();
            }catch(Exception e){
                replaying = false;
                ui.showException("Replay Error", e);
            }finally{
                stopPlay();
            }
        });
    }

    public static void stopPlay(){
        if(!replaying){
            if(reader != null){
                reader.close();
                reader = null;
            }
            return;
        }
        Log.infoTag("Replay", "stop");
        replaying = false;
        reader.close();
        reader = null;
        net.disconnect();
        ui.loadfrag.hide();
        Core.app.post(() -> logic.reset());
    }


    public static void showInfo(){
        BaseDialog dialog = new BaseDialog(mindustryX.bundles.UiTextBundle.i("回放统计")); // 原文本:回放统计
        if(reader == null){
            dialog.cont.add(mindustryX.bundles.UiTextBundle.i("未加载回放!")); // 原文本:未加载回放!
            return;
        }
        var replay = reader.getMeta();
        dialog.cont.add(VarsX.getUiTextBundle().playbackVersion(String.valueOf(replay.getVersion()))).row(); // 原文本:回放版本:
        dialog.cont.add(VarsX.getUiTextBundle().replayCreationTime(String.valueOf(replay.getTime()))).row(); // 原文本:回放创建时间:
        dialog.cont.add(VarsX.getUiTextBundle().serverIp(replay.getServerIp())).row(); // 原文本:服务器ip:
        dialog.cont.add(VarsX.getUiTextBundle().playerName(replay.getRecordPlayer())).row(); // 原文本:玩家名:

        if(reader.getSource() != null){
            var tmpReader = new ReplayData.Reader(reader.getSource());
            var packets = tmpReader.allPacket();
            tmpReader.close();

            dialog.cont.add(VarsX.getUiTextBundle().packetCount(packets.size())).row(); // 原文本:数据包总数：
            int secs = (int)(packets.get(packets.size() - 1).getOffset() / 60);
            dialog.cont.add(VarsX.getUiTextBundle().playbackLength((secs / 3600) + ":" + (secs / 60 % 60) + ":" + (secs % 60))).row(); // 原文本:回放长度:
            dialog.cont.pane(t -> {
                t.defaults().pad(2);
                for(var packet : packets){
                    t.add(Strings.format("+@s", Strings.fixed(packet.getOffset() / 60f, 2)));
                    t.add(Net.newPacket(packet.getId()).getClass().getSimpleName()).fillX();
                    t.add("L=" + packet.getLength());
                    t.row();
                }
            }).growX().row();
        }
        dialog.addCloseButton();
        dialog.show();
    }
}
