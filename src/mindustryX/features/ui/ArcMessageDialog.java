package mindustryX.features.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.struct.Queue;
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
        super("@arcMessageDialog.title");

        //voiceControl.voiceControlDialog();
        cont.pane(t -> historyTable = t).maxWidth(1000).scrollX(false);

        addCloseButton();
        buttons.button("@arcMessageDialog.settings", Icon.settings, this::arcMsgSettingTable);
        buttons.button("@arcMessageDialog.export", Icon.upload, this::exportMsg).name("@arcMessageDialog.exportTooltip");

        buttons.row();
        buttons.button("@arcMessageDialog.clear", Icon.trash, () -> {
            msgList.clear();
            build();
        });

        shown(this::build);
        onResize(this::build);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            addMsg(new Msg(Type.eventWorldLoad, Core.bundle.format("arcMessageDialog.loadMap", state.map.name())));
            addMsg(new Msg(Type.eventWorldLoad, Core.bundle.format("arcMessageDialog.mapDescription", state.map.description())));
            while(msgList.size >= maxMsgRecorded) msgList.removeLast();
        });

        Events.on(EventType.WaveEvent.class, e -> {
            if(state.wavetime < 60f) return;
            addMsg(new Msg(Type.eventWave, Core.bundle.format("arcMessageDialog.wave", state.wave, getWaveInfo(state.wave - 1))));
        });

        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if(e.tile.build instanceof CoreBlock.CoreBuild)
                addMsg(new Msg(Type.eventCoreDestory, Core.bundle.format("arcMessageDialog.coreDestroy", "(" + (int)e.tile.x + "," + (int)e.tile.y + ")"), new Vec2(e.tile.x * 8, e.tile.y * 8)));
        });
    }

    public static void share(String type, String content){
        UIExt.sendChatMessage("<ARCxMDTX><" + type + ">" + content);
    }

    public static void shareContent(UnlockableContent content, boolean description){
        StringBuilder builder = new StringBuilder();
        builder.append(Core.bundle.format("arcMessageDialog.markContent", content.localizedName, content.emoji(), content.name));
        if(content.description != null && description){
            builder.append(Core.bundle.format("arcMessageDialog.markDescription", content.description));
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
            ui.showException(Core.bundle.get("arcMessageDialog.uploadFailed"), e);
            Core.app.post(() -> callback.get(null));
        }));
    }

    public static String getWaveInfo(int waves){
        StringBuilder builder = new StringBuilder();
        if(state.rules.attackMode){
            int sum = Math.max(state.teams.present.sum(t -> t.team != player.team() ? t.cores.size : 0), 1) + Vars.spawner.countSpawns();
            builder.append(Core.bundle.format("arcMessageDialog.contains", sum));
        }else{
            builder.append(Core.bundle.format("arcMessageDialog.containsColon", Vars.spawner.countSpawns()));
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
                        tt.add(msg.msgType.getLocalizedName()).style(Styles.outlineLabel).color(msg.msgType.color).left().width(300f);

                    tt.add(formatTime(msg.time)).style(Styles.outlineLabel).color(msg.msgType.color).left().padLeft(20f).width(100f);

                    if(msg.msgLoc != null){
                        tt.button(Core.bundle.format("arcMessageDialog.location", (int)(msg.msgLoc.x / tilesize), (int)(msg.msgLoc.y / tilesize)), Styles.logict, () -> {
                            control.input.panCamera(msg.msgLoc);
                            MarkerType.mark.at(Tmp.v1.scl(msg.msgLoc.x, msg.msgLoc.y)).color = color;
                            hide();
                        }).padLeft(50f).height(24f).width(150f);
                    }

                    tt.add().growX();
                    tt.add("    " + id).style(Styles.outlineLabel).color(msg.msgType.color).padRight(10);

                    tt.button(Icon.copy, Styles.logici, () -> {
                        Core.app.setClipboardText(msg.message);
                        ui.announce(Core.bundle.get("arcMessageDialog.exported"));
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
            return msgElement.msgType.getLocalizedName();
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
        BaseDialog setDialog = new BaseDialog("@arcMessageDialog.settingsTitle");
        if(Core.settings.getInt("maxMsgRecorded") == 0) Core.settings.put("maxMsgRecorded", 500);

        setDialog.cont.table(t -> {
            t.check("@arcMessageDialog.editMode", fieldMode, a -> {
                fieldMode = a;
                build();
            }).left().width(200f).row();

            t.add("@arcMessageDialog.adjustDisplay").height(50f).row();
            t.table(tt -> {
                tt.button("@arcMessageDialog.closeAll", Styles.cleart, () -> {
                    for(Type type : Type.values()) type.show = false;
                }).width(200f).height(50f);
                tt.button("@arcMessageDialog.default", Styles.cleart, () -> {
                    for(Type type : Type.values()) type.show = true;
                    Type.serverTips.show = false;
                }).width(200f).height(50f);
            }).row();
            t.table(Tex.button, tt -> tt.pane(tp -> {
                for(Type type : Type.values()){

                    CheckBox box = new CheckBox("[#" + type.color.toString() + "]" + type.getLocalizedName());

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
            t.add("@arcMessageDialog.maxRecords");
            t.field(maxMsgRecorded + "", text -> {
                int record = Math.min(Math.max(Integer.parseInt(text), 1), 9999);
                maxMsgRecorded = record;
                Core.settings.put("maxMsgRecorded", record);
            }).valid(Strings::canParsePositiveInt).width(200f).get();
            t.row();
            t.add("@arcMessageDialog.maxRecordsWarning");
        });

        setDialog.addCloseButton();
        setDialog.button("@arcMessageDialog.refresh", Icon.refresh, this::build);

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
                    ui.announce(Core.bundle.format("arcMessageDialog.playerPoke", sender.name), 10);
                else ui.announce(Core.bundle.get("arcMessageDialog.pokeTip"), 10);
            }
        }
    }

    public static Type resolveMarkType(String message){
        if(!message.contains("<ARC")) return null;
        if(message.contains(Core.bundle.get("arcMessageDialog.serverMsg.marked")) && message.contains("Wave")) return Type.markWave;
        if(message.contains(Core.bundle.get("arcMessageDialog.serverMsg.marked")) && message.contains("Content")) return Type.markContent;
        if(message.contains("<AT>")) return Type.markPlayer;
        if(message.contains("<Schem>")) return Type.schematic;
        return null;
    }

    private static Seq<String> getServerMsgPatterns(){
        return Seq.with(
            Core.bundle.get("arcMessageDialog.serverMsg.joined"),
            Core.bundle.get("arcMessageDialog.serverMsg.left"),
            Core.bundle.get("arcMessageDialog.serverMsg.autoSave"),
            Core.bundle.get("arcMessageDialog.serverMsg.loginSuccess"),
            Core.bundle.get("arcMessageDialog.serverMsg.experience"),
            Core.bundle.get("arcMessageDialog.serverMsg.gameTime"),
            Core.bundle.get("arcMessageDialog.serverMsg.quickVote"),
            Core.bundle.get("arcMessageDialog.serverMsg.rollback"),
            Core.bundle.get("arcMessageDialog.serverMsg.pvpProtection"),
            Core.bundle.get("arcMessageDialog.serverMsg.initiated"),
            Core.bundle.get("arcMessageDialog.serverMsg.voteUsage"),
            Core.bundle.get("arcMessageDialog.serverMsg.voteSuccess"),
            Core.bundle.get("arcMessageDialog.serverMsg.mapChange"),
            Core.bundle.get("arcMessageDialog.serverMsg.unitDisabled"),
            Core.bundle.get("arcMessageDialog.serverMsg.airRestriction"),
            Core.bundle.get("arcMessageDialog.serverMsg.airLimit"),
            Core.bundle.get("arcMessageDialog.serverMsg.flameDisabled"),
            Core.bundle.get("arcMessageDialog.serverMsg.separator"),
            Core.bundle.get("arcMessageDialog.serverMsg.invalidCommand"),
            Core.bundle.get("arcMessageDialog.serverMsg.skill"),
            Core.bundle.get("arcMessageDialog.serverMsg.switchSuccess"),
            Core.bundle.get("arcMessageDialog.serverMsg.voteSystem"),
            Core.bundle.get("arcMessageDialog.serverMsg.wildMinus"),
            Core.bundle.get("arcMessageDialog.serverMsg.wildPlus")   // xem相关
        );
    }

    public static Type resolveServerType(String message){
        if(message.contains(Core.bundle.get("arcMessageDialog.serverMsg.tips"))) return Type.serverTips;
        if(message.contains(Core.bundle.get("arcMessageDialog.serverMsg.skillTag"))) return Type.serverSkill;
        Seq<String> serverMsg = getServerMsgPatterns();
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
        messageHis.append(Core.bundle.format("arcMessageDialog.exportHeader", VarsX.version)).append("\n");
        messageHis.append(Core.bundle.format("arcMessageDialog.exportMapInfo", state.map.name(), state.rules.modeName)).append("\n");
        messageHis.append(Core.bundle.format("arcMessageDialog.exportWaveInfo", state.wave)).append("\n");
        messageHis.append(Core.bundle.format("arcMessageDialog.exportSummary", msgList.size)).append("\n");
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
        normal("normal", Color.gray),

        chat("chat", Color.valueOf("#778899")),
        console("console", Color.gold),

        markLoc("markLoc", "markLocSub", Color.valueOf("#7FFFD4")),
        markWave("markWave", "markWaveSub", Color.valueOf("#7FFFD4")),
        markContent("markContent", "markContentSub", Color.valueOf("#7FFFD4")),
        markPlayer("markPlayer", "markPlayerSub", Color.valueOf("#7FFFD4")),
        arcChatPicture("arcChatPicture", "arcChatPictureSub", Color.yellow),
        music("music", "musicSub", Color.pink),
        schematic("schematic", "schematicSub", Color.blue),
        district("district", "", Color.violet),

        serverTips("serverTips", "serverTipsSub", Color.valueOf("#98FB98"), false),
        serverMsg("serverMsg", "serverMsgSub", Color.valueOf("#cefdce")),
        serverToast("serverToast", "serverToastSub", Color.valueOf("#00FA9A")),
        serverSkill("serverSkill", "serverSkillSub", Color.valueOf("#e6ffcc")),

        logicNotify("logicNotify", "logicNotifySub", Color.valueOf("#ffccff")),
        logicAnnounce("logicAnnounce", "logicAnnounceSub", Color.valueOf("#ffccff")),

        eventWorldLoad("eventWorldLoad", "eventWorldLoadSub", Color.valueOf("#ff9999")),
        eventCoreDestory("eventCoreDestory", "eventCoreDestorySub", Color.valueOf("#ffcccc")),
        eventWave("eventWave", "eventWaveSub", Color.valueOf("#ffcc99"));

        public final String type;
        public final String subClass;
        public final Color color;
        public Boolean show;

        Type(String type, String subClass, Color color, Boolean show){
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
        
        public String getLocalizedName(){
            String typeLocalized = Core.bundle.get("arcMessageDialog.type." + type, type);
            if(subClass.isEmpty()) return typeLocalized;
            String subLocalized = Core.bundle.get("arcMessageDialog.type." + subClass, subClass);
            return typeLocalized + "~" + subLocalized;
        }
        
        // For compatibility with existing code that uses .name
        public String name(){
            return getLocalizedName();
        }
    }
}