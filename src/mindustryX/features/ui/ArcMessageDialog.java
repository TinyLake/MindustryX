package mindustryX.features.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.Queue;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.storage.*;
import mindustryX.*;
import mindustryX.features.*;

import java.text.*;
import java.util.*;

import static mindustry.Vars.*;

//move from mindustry.arcModule.ui.dialogs.MessageDialog
public class ArcMessageDialog extends BaseDialog{
    public static final Queue<Msg> msgList = new Queue<>();//队头为新添加的
    private static int maxMsgRecorded = Math.max(Core.settings.getInt("maxMsgRecorded"), 20);
    private Table historyTable;
    private boolean fieldMode = false;

    public ArcMessageDialog(){
        super("ARC-中央监控室");

        //voiceControl.voiceControlDialog();
        cont.pane(t -> historyTable = t).maxWidth(1000).scrollX(false);

        addCloseButton();
        buttons.button("设置", Icon.settings, this::arcMsgSettingTable);
        buttons.button("导出", Icon.upload, this::exportMsg).name("导出聊天记录");

        buttons.row();
        buttons.button("清空", Icon.trash, () -> {
            msgList.clear();
            build();
        });

        shown(this::build);
        onResize(this::build);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            addMsg(new Msg(Type.eventWorldLoad, "载入地图： " + state.map.name()));
            addMsg(new Msg(Type.eventWorldLoad, "简介： " + state.map.description()));
            while(msgList.size >= maxMsgRecorded) msgList.removeLast();
        });

        Events.on(EventType.WaveEvent.class, e -> {
            if(state.wavetime < 60f) return;
            addMsg(new Msg(Type.eventWave, "波次： " + state.wave + " | " + getWaveInfo(state.wave - 1)));
        });

        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if(e.tile.build instanceof CoreBlock.CoreBuild)
                addMsg(new Msg(Type.eventCoreDestory, "核心摧毁： " + "(" + (int)e.tile.x + "," + (int)e.tile.y + ")", new Vec2(e.tile.x * 8, e.tile.y * 8)));
        });
    }

    public static void share(String type, String content){
        UIExt.sendChatMessage("<ARCxMDTX><" + type + ">" + content);
    }

    public static void shareWaveInfo(int waves){
        if(!state.rules.waves) return;
        StringBuilder builder = new StringBuilder();
        builder.append("标记了第").append(waves).append("波");
        if(waves < state.wave){
            builder.append("。");
        }else{
            if(waves > state.wave){
                builder.append("，还有").append(waves - state.wave).append("波");
            }
            int timer = (int)(state.wavetime + (waves - state.wave) * state.rules.waveSpacing);
            builder.append("[[").append(FormatDefault.duration((float)timer / 60)).append("]。");
        }

        builder.append(getWaveInfo(waves));
        share("Wave", builder.toString());
    }

    public static void shareContent(UnlockableContent content, boolean description){
        StringBuilder builder = new StringBuilder();
        builder.append("标记了").append(content.localizedName).append(content.emoji());
        builder.append("(").append(content.name).append(")");
        if(content.description != null && description){
            builder.append("。介绍: ").append(content.description);
        }
        ArcMessageDialog.share("Content", builder.toString());
    }

    public static void uploadPasteBin(String content, Cons<String> callback){
        Http.HttpRequest req = Http.post("https://pastebin.com/api/api_post.php", "api_dev_key=sdBDjI5mWBnHl9vBEDMNiYQ3IZe0LFEk&api_option=paste&api_paste_expire_date=10M&api_paste_code=" + content);
        req.submit(r -> {
            String code = r.getResultAsString();
            Core.app.post(() -> callback.get(code));
        });
        req.error(e -> Core.app.post(() -> {
            ui.showException("上传失败，再重试一下？", e);
            Core.app.post(() -> callback.get(null));
        }));
    }

    public static String getWaveInfo(int waves){
        StringBuilder builder = new StringBuilder();
        if(state.rules.attackMode){
            int sum = Math.max(state.teams.present.sum(t -> t.team != player.team() ? t.cores.size : 0), 1) + Vars.spawner.countSpawns();
            builder.append("包含(×").append(sum).append(")");
        }else{
            builder.append("包含(×").append(Vars.spawner.countSpawns()).append("):");
        }
        for(SpawnGroup group : state.rules.spawns){
            if(group.getSpawned(waves - 1) > 0){
                builder.append((char)Fonts.getUnicode(group.type.name)).append("(");
                if(group.effect != StatusEffects.invincible && group.effect != StatusEffects.none && group.effect != null){
                    builder.append((char)Fonts.getUnicode(group.effect.name)).append("|");
                }
                if(group.getShield(waves - 1) > 0){
                    builder.append(FormatDefault.format(group.getShield(waves - 1))).append("|");
                }
                builder.append(group.getSpawned(waves - 1)).append(")");
            }
        }
        return builder.toString();
    }

    void build(){
        historyTable.clear();
        historyTable.setWidth(800f);
        int i = 0;
        for(var msg : msgList){
            i++;
            int id = i;
            if(!msg.msgType.show) continue;
            historyTable.table(Tex.whitePane, t -> {
                t.setColor(msg.msgType.color);
                t.marginTop(5);

                t.table(Tex.whiteui, tt -> {
                    tt.color.set(msg.msgType.color);

                    if(msg.msgType == Type.chat)
                        tt.add(getPlayerName(msg)).style(Styles.outlineLabel).left().width(300f);
                    else
                        tt.add(msg.msgType.name).style(Styles.outlineLabel).color(msg.msgType.color).left().width(300f);

                    tt.add(formatTime(msg.time)).style(Styles.outlineLabel).color(msg.msgType.color).left().padLeft(20f).width(100f);

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
                    tt.button(Icon.cancel, Styles.logici, () -> {
                        msgList.remove(msg);
                        build();
                    }).size(24f);

                }).growX().height(30);

                t.row();

                t.table(tt -> {
                    tt.left();
                    tt.marginLeft(4);
                    tt.setColor(msg.msgType.color);
                    if(fieldMode) tt.field(msg.message, Styles.nodeArea, text -> {
                    }).growX();
                    else tt.labelWrap(getPlayerMsg(msg)).growX();
                }).pad(4).padTop(2).growX().grow();

                t.marginBottom(7);
            }).growX().padBottom(15f).row();
        }
    }

    private String getPlayerName(Msg msgElement){
        int typeStart = msgElement.message.indexOf("[coral][");
        int typeEnd = msgElement.message.indexOf("[coral]]");
        if(typeStart == -1 || typeEnd == -1 || typeEnd <= typeStart){
            return msgElement.msgType.name;
        }

        return msgElement.message.substring(typeStart + 20, typeEnd);
    }

    private String getPlayerMsg(Msg msgElement){
        if(msgElement.msgType != Type.normal) return msgElement.message;
        int typeStart = msgElement.message.indexOf("[coral][");
        int typeEnd = msgElement.message.indexOf("[coral]]");
        if(typeStart == -1 || typeEnd == -1 || typeEnd <= typeStart){
            return msgElement.message;
        }
        return msgElement.message.substring(typeEnd + 9);
    }

    private void arcMsgSettingTable(){
        BaseDialog setDialog = new BaseDialog("中央监控室-设置");
        if(Core.settings.getInt("maxMsgRecorded") == 0) Core.settings.put("maxMsgRecorded", 500);

        setDialog.cont.table(t -> {
            t.check("信息编辑模式", fieldMode, a -> {
                fieldMode = a;
                build();
            }).left().width(200f).row();

            t.add("调整显示的信息").height(50f).row();
            t.table(tt -> {
                tt.button("关闭全部", Styles.cleart, () -> {
                    for(Type type : Type.values()) type.show = false;
                }).width(200f).height(50f);
                tt.button("默认", Styles.cleart, () -> {
                    for(Type type : Type.values()) type.show = true;
                    Type.serverTips.show = false;
                }).width(200f).height(50f);
            }).row();
            t.table(Tex.button, tt -> tt.pane(tp -> {
                for(Type type : Type.values()){

                    CheckBox box = new CheckBox("[#" + type.color.toString() + "]" + type.name);

                    box.update(() -> box.setChecked(type.show));
                    box.changed(() -> {
                        type.show = !type.show;
                        build();
                    });

                    box.left();
                    tp.add(box).left().padTop(3f).row();
                }
            }).maxHeight(500).width(400f));
        });

        setDialog.cont.row();

        setDialog.cont.table(t -> {
            t.add("最大储存聊天记录(过高可能导致卡顿)：");
            t.field(maxMsgRecorded + "", text -> {
                int record = Math.min(Math.max(Integer.parseInt(text), 1), 9999);
                maxMsgRecorded = record;
                Core.settings.put("maxMsgRecorded", record);
            }).valid(Strings::canParsePositiveInt).width(200f).get();
            t.row();
            t.add("超出限制的聊天记录将在载入地图时清除");
        });

        setDialog.addCloseButton();
        setDialog.button("刷新", Icon.refresh, this::build);

        setDialog.show();
    }

    public String formatTime(Date time){
        return new SimpleDateFormat("HH:mm:ss", Locale.US).format(time);
    }

    public static void resolveMsg(String message, @Nullable Player sender){
        Type type = resolveMarkType(message);
        if(type == null) type = resolveServerType(message);
        if(type == null) type = sender != null ? Type.chat : Type.normal;

        addMsg(new Msg(type, message, sender != null ? sender.name() : null, sender != null ? new Vec2(sender.x, sender.y) : null));
        if(!type.show) return;
        switch(type){
            case schematic -> {
                String id = message.split("<Schem>")[1];
                id = id.substring(id.indexOf(' ') + 1);
                Http.get("https://pastebin.com/raw/" + id, r -> {
                    String content = r.getResultAsString().replace(" ", "+");
                    Core.app.post(() -> ui.schematics.readShare(content, sender));
                });
            }
            case markPlayer -> {
                if(!message.split("AT")[1].contains(player.name)) return;
                if(sender != null)
                    ui.announce("[gold]你被[white] " + sender.name + " [gold]戳了一下，请注意查看信息框哦~", 10);
                else ui.announce("[orange]你被戳了一下，请注意查看信息框哦~", 10);
            }
        }
    }

    public static Type resolveMarkType(String message){
        if(!message.contains("<ARC")) return null;
        if(message.contains("标记了") && message.contains("Wave")) return Type.markWave;
        if(message.contains("标记了") && message.contains("Content")) return Type.markContent;
        if(message.contains("<AT>")) return Type.markPlayer;
        if(message.contains("<Schem>")) return Type.schematic;
        return null;
    }

    private static final Seq<String> serverMsg = Seq.with("加入了服务器", "离开了服务器", "自动存档完成", "登录成功", "经验+", "[YELLOW]本局游戏时长:", "[YELLOW]单人快速投票", "[GREEN]回档成功",
    "[YELLOW]PVP保护时间, 全力进攻吧", "[YELLOW]发起", "[YELLOW]你可以在投票结束前使用", "[GREEN]投票成功", "[GREEN]换图成功,当前地图",
    "[RED]本地图禁用单位", "[RED]该地图限制空军,禁止进入敌方领空", "[yellow]本地图限制空军", "[YELLOW]火焰过多造成服务器卡顿,自动关闭火焰",
    " [GREEN]====", "[RED]无效指令", "[RED]该技能", "切换成功",
    "[violet][投票系统][]", "[coral][-]野生的", "[CYAN][+]野生的"   // xem相关
    );

    public static Type resolveServerType(String message){
        if(message.contains("小贴士")) return Type.serverTips;
        if(message.contains("[YELLOW][技能]")) return Type.serverSkill;
        for(int i = 0; i < serverMsg.size; i++){
            if(message.contains(serverMsg.get(i))){
                return Type.serverMsg;
            }
        }
        return null;
    }

    public static void addMsg(Msg msg){
        msgList.addFirst(msg);
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
        public final Date time;
        public final @Nullable String sender;
        public boolean selected;
        public final @Nullable Vec2 msgLoc;

        public Msg(Type msgType, String message, Date time, @Nullable String sender, @Nullable Vec2 msgLoc){
            this.msgType = msgType;
            this.message = message;
            this.time = time;
            this.sender = sender;
            this.msgLoc = msgLoc;
        }

        public Msg(Type msgType, String message, String sender, Vec2 msgLoc){
            this(msgType, message, new Date(), sender, msgLoc);
        }

        public Msg(Type msgType, String message, Vec2 msgLoc){
            this(msgType, message, null, msgLoc);
        }

        public Msg(Type msgType, String message){
            this(msgType, message, null);
        }

        public Msg add(){
            ArcMessageDialog.addMsg(this);
            return this;
        }
    }

    public enum Type{
        normal("消息", Color.gray),

        chat("聊天", Color.valueOf("#778899")),
        console("指令", Color.gold),

        markLoc("标记", "坐标", Color.valueOf("#7FFFD4")),
        markWave("标记", "波次", Color.valueOf("#7FFFD4")),
        markContent("标记", "内容", Color.valueOf("#7FFFD4")),
        markPlayer("标记", "玩家", Color.valueOf("#7FFFD4")),
        arcChatPicture("分享", "图片", Color.yellow),
        music("分享", "音乐", Color.pink),
        schematic("分享", "蓝图", Color.blue),
        district("规划区", "", Color.violet),

        serverTips("服务器", "小贴士", Color.valueOf("#98FB98"), false),
        serverMsg("服务器", "信息", Color.valueOf("#cefdce")),
        serverToast("服务器", "通报", Color.valueOf("#00FA9A")),
        serverSkill("服务器", "技能", Color.valueOf("#e6ffcc")),

        logicNotify("逻辑", "通报", Color.valueOf("#ffccff")),
        logicAnnounce("逻辑", "公告", Color.valueOf("#ffccff")),

        eventWorldLoad("事件", "载入地图", Color.valueOf("#ff9999")),
        eventCoreDestory("事件", "核心摧毁", Color.valueOf("#ffcccc")),
        eventWave("事件", "波次", Color.valueOf("#ffcc99"));

        public final String name;
        public final String type;
        public final String subClass;
        public final Color color;
        public Boolean show;

        Type(String type, String subClass, Color color, Boolean show){
            this.name = subClass.isEmpty() ? type : (type + "~" + subClass);
            this.type = type;
            this.subClass = subClass;
            this.color = color;
            this.show = show;
        }

        Type(String type, String subClass, Color color){
            this(type, subClass, color, true);
        }

        Type(String type, Color color){
            this(type, "", color);
        }
    }
}