package mindustryX.features.ui;

import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.struct.Queue;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.*;
import mindustryX.features.*;
import mindustryX.features.ui.comp.*;

import java.text.*;
import java.util.*;

import static mindustry.Vars.*;

//move from mindustry.arcModule.ui.dialogs.MessageDialog
public class ArcMessageDialog extends BaseDialog{
    public static final Queue<Msg> msgList = new Queue<>();//队头为新添加的
    private final ObjectSet<Type> hiddenTypes = new ObjectSet<>();
    private int maxMsgRecorded;

    private static int id = 0;
    private static final GridTable msgTable = new GridTable();
    private static final GridTable chooseTable = new GridTable();

    public ArcMessageDialog(){
        super("ARC-中央监控室");
        if(Core.settings.getInt("maxMsgRecorded") == 0) Core.settings.put("maxMsgRecorded", 500);
        maxMsgRecorded = Core.settings.getInt("maxMsgRecorded");

        cont.add(chooseTable).fillX().row();
        chooseTable.background(Tex.pane);
        chooseTable.defaults().width(180f).left().padRight(4);
        for(Type type : Type.values()){
            chooseTable.check("[#" + type.color.toString() + "]" + type.name, (b) -> {
                if(!b) hiddenTypes.add(type);
                else hiddenTypes.remove(type);
            }).checked((b) -> !hiddenTypes.contains(type)).get().left();
        }

        cont.pane(msgTable).maxWidth(1200 - 1).fillX().growY().scrollX(false).row();
        msgTable.defaults().minWidth(600).growX().padBottom(15f);

        cont.table(t -> {
            t.add("最大储存聊天记录(过高可能导致卡顿)：");
            t.field(maxMsgRecorded + "", text -> {
                int record = Math.min(Math.max(Integer.parseInt(text), 1), 9999);
                maxMsgRecorded = record;
                Core.settings.put("maxMsgRecorded", record);
            }).valid(Strings::canParsePositiveInt).width(200f).get();
            t.row();
            t.add("超出限制的聊天记录将在载入地图时清除").color(Color.lightGray).colspan(2);
        }).row();

        addCloseButton();
        buttons.button("清空", Icon.trash, msgTable::clearChildren);
        buttons.button("导出", Icon.upload, this::exportMsg).name("导出聊天记录");

        Events.on(EventType.WorldLoadEvent.class, e -> {
            addMsg(new Msg(Type.eventWorldLoad, "载入地图： " + state.map.name()));
            addMsg(new Msg(Type.eventWorldLoad, "简介： " + state.map.description()));
            while(msgTable.getChildren().size >= maxMsgRecorded) msgTable.getChildren().get(0).remove();
        });

        Events.on(EventType.WaveEvent.class, e -> {
            if(state.wavetime < 60f) return;
            addMsg(new Msg(Type.eventWave, "波次： " + state.wave + " | " + ShareFeature.INSTANCE.waveInfo(state.wave)));
        });
    }

    private final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public void addMsg(Msg msg){
        id++;
        msgTable.table(Tex.whitePane, t -> {
            t.setColor(msg.msgType.color);
            t.marginTop(5);
            t.visible(() -> !hiddenTypes.contains(msg.msgType));

            t.table(Tex.whiteui, tt -> {
                tt.color.set(msg.msgType.color);

                if(msg.msgType == Type.chat)
                    tt.add(msg.sender != null ? msg.sender : msg.msgType.name).style(Styles.outlineLabel).left().width(300f);
                else
                    tt.add(msg.msgType.name).style(Styles.outlineLabel).color(msg.msgType.color).left().width(300f);

                tt.add(timeFormat.format(msg.time)).style(Styles.outlineLabel).color(msg.msgType.color).left().padLeft(20f).width(100f);

                if(msg.msgLoc != null){
                    tt.button("♐： " + (int)(msg.msgLoc.x / tilesize) + "," + (int)(msg.msgLoc.y / tilesize), Styles.logict, () -> {
                        control.input.panCamera(msg.msgLoc);
                        MarkerType.mark.at(Tmp.v1.scl(msg.msgLoc.x, msg.msgLoc.y)).color = color;
                        hide();
                    }).padLeft(50f).height(24f).width(150f);
                }

                tt.add().growX();
                tt.add("    " + id).style(Styles.outlineLabel).color(msg.msgType.color).padRight(10);

                tt.button(Icon.copy, Styles.logici, () -> {
                    Core.app.setClipboardText(msg.message);
                    ui.announce("已导出本条聊天记录");
                }).size(24f).padRight(6);
                tt.button(Icon.cancel, Styles.logici, t::remove).size(24f);

            }).growX().height(30);

            t.row();

            t.table(tt -> {
                tt.left();
                tt.marginLeft(4);
                tt.setColor(msg.msgType.color);
                tt.labelWrap(msg.message).growX();
            }).pad(4).padTop(2).growX().grow();

            t.marginBottom(7);
        });
    }

    void exportMsg(){
        StringBuilder messageHis = new StringBuilder();
        messageHis.append("下面是[MDTX-").append(VarsX.version).append("] 导出的游戏内聊天记录").append("\n");
        messageHis.append("*** 当前地图名称: ").append(state.map.name()).append("（模式：").append(state.rules.modeName).append("）\n");
        messageHis.append("*** 当前波次: ").append(state.wave).append("\n");
        messageHis.append("成功选取共 ").append(msgList.size).append(" 条记录，如下：\n");
        for(var msg : msgList){
            messageHis.append(Strings.stripColors(msg.message)).append("\n");
        }
        Core.app.setClipboardText(Strings.stripGlyphs(Strings.stripColors(messageHis.toString())));
    }

    public static class Msg{
        public final Type msgType;
        public final String message;
        public final Date time = new Date();
        public final @Nullable String sender;
        public final @Nullable Vec2 msgLoc;

        public Msg(Type msgType, String message, @Nullable String sender, @Nullable Vec2 msgLoc){
            this.msgType = msgType;
            this.message = message;
            this.sender = sender;
            this.msgLoc = msgLoc;
        }

        public Msg(Type msgType, String message, @Nullable Player sender){
            this(msgType, message, sender != null ? sender.name() : null, sender != null ? new Vec2(sender.x, sender.y) : null);
        }

        public Msg(Type msgType, String message){
            this(msgType, message, null, null);
        }

        public void add(){
            UIExt.arcMessageDialog.addMsg(this);
        }
    }

    public enum Type{
        chat("聊天", Color.gray),
        serverMsg("服务器信息", Color.valueOf("#cefdce")),

        markLoc("标记~坐标", Color.valueOf("#7FFFD4")),
        markPlayer("标记~玩家", Color.valueOf("#7FFFD4")),

        console("指令", Color.gold),

        logicNotify("逻辑~通报", Color.valueOf("#ffccff")),
        logicAnnounce("逻辑~公告", Color.valueOf("#ffccff")),

        eventWorldLoad("事件~载入地图", Color.valueOf("#ff9999")),
        eventWave("事件~波次", Color.valueOf("#ffcc99"));

        public final String name;
        public final Color color;

        Type(String name, Color color){
            this.name = name;
            this.color = color;
        }

        public void log(String message){
            new Msg(this, message).add();
        }
    }
}