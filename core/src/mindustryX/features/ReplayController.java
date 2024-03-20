package mindustryX.features;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.ui.dialogs.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

import static mindustry.Vars.*;

/**
 * 回放录制
 * 原作者 cong0707, 原文件路径 mindustry.arcModule.ReplayController
 * WayZer修改优化
 */
public class ReplayController{
    public static boolean recording, replaying;

    private static Writes writes;
    private static float startTime;
    private static final ByteBuffer tmpBuf = ByteBuffer.allocate(32768);
    private static final Writes tmpWr = new Writes(new ByteBufferOutput(tmpBuf));
    private static ReplayData now = null;

    public ReplayController(){
        Events.run(EventType.Trigger.update, () -> {
            if(state.getState() == GameState.State.menu && !netClient.isConnecting()){
                replaying = false;
                stopPlay();
            }
        });
    }

    private static class ReplayData{
        int version;
        Date time;
        String ip;
        String name;
        long length;
        private final IntIntMap packetCount = new IntIntMap();

        ReplayData(int version, Date time, String ip, String name){
            this.version = version;
            this.time = time;
            this.ip = ip;
            this.name = name;
        }
    }

    public static void onConnect(String ip){
        if(!Core.settings.getBool("replayRecord")) return;
        if(replaying) return;
        var file = saveDirectory.child(new Date().getTime() + ".mrep");
        try{
            writes = new Writes(new DataOutputStream(new DeflaterOutputStream(file.write(false, 8192))));
        }catch(Exception e){
            Log.err("创建回放出错!", e);
            return;
        }
        boolean anonymous = Core.settings.getBool("anonymous", false);
        writes.i(Version.build);
        writes.l(new Date().getTime());
        writes.str(anonymous ? "anonymous" : ip);
        writes.str(anonymous ? "anonymous" : Vars.player.name.trim());
        startTime = Time.time;
        recording = true;
        Log.info("录制中: @", file.absolutePath());
    }

    public static void stop(){
        recording = false;
        try{
            writes.close();
        }catch(Exception ignored){
        }
        writes = null;
    }

    public static void onClientPacket(Packet p){
        if(!recording || p instanceof Streamable) return;
        if(p instanceof Disconnect){
            stop();
            Log.info("录制结束");
            return;
        }
        try{
            byte id = Net.getPacketId(p);
            try{
                writes.f(Time.time - startTime);
                writes.b(id);
                tmpBuf.position(0);
                p.write(tmpWr);
                int l = tmpBuf.position();
                writes.s(l);
                writes.b(tmpBuf.array(), 0, l);
            }catch(Exception e){
                net.disconnect();
                Core.app.post(() -> ui.showException("录制出错!", e));
            }
        }catch(Exception e){
            Log.err(e);
        }
    }

    //replay

    public static Reads createReads(Fi input){
        try{
            return new Reads(new DataInputStream(new InflaterInputStream(input.read(32768))));
        }catch(Exception e){
            Core.app.post(() -> ui.showException("读取回放失败!", e));
        }
        return null;
    }

    public static void startPlay(Fi input){
        try(Reads r = createReads(input)){
            int version = r.i();
            Date time = new Date(r.l());
            String ip = r.str();
            String name = r.str();
            Log.info("version: @, time: @, ip: @, name: @", version, time, ip, name);
            now = new ReplayData(version, time, ip, name);
            while(true){
                long l = r.l();
                byte id = r.b();
                r.skip(r.us());
                now.packetCount.put(id, now.packetCount.get(id, 0) + 1);
                now.length = l;
            }
        }catch(Exception e){
            if(!(e.getCause() instanceof EOFException)){
                Log.err(e);
                return;
            }
        }

        Reads reads = createReads(input);
        reads.skip(12);
        reads.str();
        reads.str();
        replaying = true;

        ui.loadfrag.show("@connecting");
        ui.loadfrag.setButton(() -> {
            replaying = false;
            ui.loadfrag.hide();
            netClient.disconnectQuietly();
        });

        logic.reset();
        net.reset();
        netClient.beginConnecting();
        Reflect.set(net, "active", true);
        Reflect.set(net, "server", true);
        Packets.Connect c = new Packets.Connect();
        c.addressTCP = now.ip;
        net.handleClientReceived(c);

        startTime = Time.time;
        Threads.daemon("Replay Controller", () -> {
            try{
                while(replaying){
                    float nextTime = reads.f();
                    Packet p = Net.newPacket(reads.b());
                    p.read(reads, reads.us());
                    while(Time.time - startTime < nextTime)
                        Thread.yield();
                    Core.app.post(() -> net.handleClientReceived(p));
                }
            }catch(Exception e){
                replaying = false;
                reads.close();
                net.disconnect();
                Core.app.post(() -> logic.reset());
            }
        });
    }

    public static void stopPlay(){
        replaying = false;
    }


    public static void showInfo(){
        BaseDialog dialog = new BaseDialog("回放统计");
        var replay = now;
        if(replay == null){
            dialog.cont.add("未加载回放!");
            return;
        }
        dialog.cont.add("回放版本:" + replay.version).row();
        dialog.cont.add("回放创建时间:" + replay.time).row();
        dialog.cont.add("服务器ip:" + replay.ip).row();
        dialog.cont.add("玩家名:" + replay.name).row();
        int secs = (int)(replay.length / 1000000000);
        dialog.cont.add("回放长度:" + (secs / 3600) + ":" + (secs / 60 % 60) + ":" + (secs % 60)).row();
        dialog.cont.pane(t -> replay.packetCount.keys().toArray().each(b ->
        t.add(Net.newPacket((byte)b).getClass().getSimpleName() + " " + replay.packetCount.get(b)).row())).growX().row();
        dialog.addCloseButton();
        dialog.show();
    }
}
