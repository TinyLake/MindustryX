package mindustry.arcModule;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.arcModule.ui.dialogs.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.DrawUtilities.drawText;
import static mindustry.arcModule.RFuncs.getPrefix;

public class Marker{
    /**
     * 冷却时间
     */
    public static final float heatTime = 60f;
    /**
     * 滞留时间
     */
    public static final float retainTime = 1800f;
    public static final Seq<MarkElement> markList = new Seq<>();
    public static MarkType mark, gatherMark, attackMark, defenseMark, quesMark;
    public static Seq<MarkType> markTypes = Seq.with(
    mark = new MarkType("Mark", ArcFx.marker, Color.valueOf("eab678")),
    gatherMark = new MarkType("Gather", ArcFx.markerGather, Color.cyan),
    attackMark = new MarkType("Attack", ArcFx.markerAttack, Color.red),
    defenseMark = new MarkType("Defend", ArcFx.markerDefense, Color.acid),
    quesMark = new MarkType("What", ArcFx.markerQuestion, Color.pink)
    );
    public static boolean isLocal;

    static{
        Events.run(WorldLoadEvent.class, markList::clear);
    }

    public static void mark(MarkType type, float x, float y){
        mark(type, Tmp.v1.set(x, y), true);
    }

    public static void mark(MarkType type, float x, float y, boolean sendMessage){
        mark(type, Tmp.v1.set(x, y), sendMessage);
    }

    public static void mark(MarkType type, Vec2 pos){
        mark(type, pos, true);
    }

    public static void mark(MarkType type, Vec2 pos, boolean sendMessage){
        if(markList.size > 0 && (Time.time - markList.peek().time) < heatTime){
            Vars.ui.announce("请不要频繁标记!");
            return;
        }

        markList.add(new MarkElement(type, pos));

        type.showEffect(pos);

        if(sendMessage){
            isLocal = true;
            type.sendMessage(pos);
        }
    }

    public static boolean resolveMessage(String text){
        if(isLocal){
            isLocal = false;
            return true;
        }

        MarkType markType = null;
        int Indexer = -1;

        for(MarkType markType1 : markTypes){
            if(text.contains("<" + markType1.name + ">") || text.contains("<" + markType1.localizedName + ">")){
                markType = markType1;
                Indexer = text.indexOf("<" + markType1.name + ">");
            }
        }

        if(Indexer > 0){
            /* Parse position */
            String posStr = text.substring(text.indexOf('(', Indexer + 1), text.indexOf(')', Indexer + 1) + 1);

            Vec2 pos = Tmp.v1;

            try{
                pos.fromString(posStr);
            }catch(Throwable e){
                Log.err("Cannot resolve position from " + posStr);
                return false;
            }

            mark(markType, pos.scl(tilesize), false);
            MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.markLoc, text, pos));
            return true;
        }

        if(text.contains("[YELLOW][集合]") && text.contains("[WHITE]\"[WHITE]\",输入\"[gold]go[WHITE]\"前往")){

            int typeStart = text.indexOf("[WHITE]发起集合([RED]");
            int typeEnd = text.indexOf("[WHITE])");
            if(typeStart == -1 || typeEnd == -1){
                return false;
            }

            /* Parse position */
            String posStr = text.substring(typeStart + 17, typeEnd);

            Vec2 pos = Tmp.v1;

            try{
                pos.fromString("(" + posStr + ")");
            }catch(Throwable e){
                Log.err("Cannot resolve position from " + posStr);
                return false;
            }

            mark(findLocalizedName("集合"), pos.scl(tilesize), false);
            MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.markLoc, text, pos));
            return true;
        }
        return false;

    }

    public static MarkType findLocalizedName(String localizedName){
        return markTypes.find(maskType -> maskType.localizedName.equals(localizedName));
    }

    public static void lockonLastMark(){
        if(markList.size == 0) return;

        if(control.input instanceof DesktopInput input){
            input.panning = true;
        }

        Core.camera.position.set(Marker.markList.peek().markPos);

        markList.peek().showEffect();
    }

    public static void drawMarker(){
        if(markList.size == 0) return;

        markList.each(t -> (Time.time - t.time) < t.markType.effect.lifetime, t -> t.markType.showArrow(t.markPos));
    }

    public static class MarkType{
        public final Color color;
        private final String name;
        private final Effect effect;
        public String localizedName;
        public String describe;

        public MarkType(String name, Effect effect){
            this(name, effect, Color.white);
        }

        public MarkType(String name, Effect effect, Color color){
            this.name = name;
            this.effect = effect;
            this.color = color;

            localizedName = Core.bundle.get("marker." + name + ".name", "unknown");
            describe = Core.bundle.get("marker." + name + ".name", "unknown");
        }

        public String shortName(){
            return "[#" + color + "]" + localizedName;
        }

        public String tinyName(){
            return "[#" + color + "]" + localizedName.charAt(0);
        }

        public void showEffect(Vec2 pos){
            effect.arcCreate(pos.x, pos.y, 0, color, null);
        }

        public void sendMessage(Vec2 pos){
            String text = getPrefix(color, name) +
            "(" + World.toTile(pos.x) + "," + World.toTile(pos.y) + ")";
            Call.sendChatMessage(text);
            MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.markLoc, text, pos));
        }

        public void showArrow(Vec2 pos){
            Draw.reset();
            Drawf.arrow(player.x, player.y, pos.x, pos.y, 5f * tilesize, 4f, color);
            float dst = Mathf.dst(player.x, player.y, pos.x, pos.y) / 8;
            Tmp.v1.set(pos.x, pos.y).sub(player.x, player.y).limit(4.5f * tilesize);
            drawText((int)dst + "", 0.2f,
            Tmp.v1.x + player.x,
            Tmp.v1.y + player.y,
            color, Align.center);
        }
    }

    public static class MarkElement{
        public MarkType markType;
        public float time;
        public String player;
        public Vec2 markPos;

        public MarkElement(MarkType markType, Vec2 markPos){
            this(markType, "", markPos);
        }

        public MarkElement(MarkType markType, String player, Vec2 markPos){
            this(markType, Time.time, player, markPos);
        }

        public MarkElement(MarkType markType, float time, String player, Vec2 markPos){
            this.markType = markType;
            this.time = time;
            this.player = player;
            this.markPos = new Vec2().set(markPos);
        }

        public void showEffect(){
            markType.showEffect(markPos);
        }

    }
}
