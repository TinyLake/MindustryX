package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustryX.features.func.*;
import mindustryX.features.ui.*;

import java.util.regex.*;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.stroke;
import static mindustry.Vars.*;

public class MarkerType{
    private static final Pattern posPattern = Pattern.compile("(?<type><[A-Za-z]+>)?\\((?<x>\\d+),(?<y>\\d+)\\)");
    /** 冷却时间 */
    public static final float heatTime = 60f;
    /** 滞留时间 */
    public static final float retainTime = 1800f;
    public static MarkerType mark, gatherMark, attackMark, defenseMark, quesMark;
    public static Seq<MarkerType> allTypes;
    private static MarkElement last;
    private static final Vec2 panCenter = new Vec2();
    private static final Element markHitter = new Table(){{
        fillParent = true;
        touchable = Touchable.enabled;
        background(((TextureRegionDrawable)Tex.whiteui).tint(0, 0, 0, 0.1f));
        center().add("[cyan]标记模式,点击屏幕标记.", Styles.outlineLabel);
        tapped(() -> {
            MarkerType.showPanUI();
            Core.app.post(this::remove);
        });
    }};

    public static void init(){
        mark = new MarkerType("Mark", new Effect(1800, e -> {
            color(e.color);
            stroke(2f);
            Lines.circle(e.x, e.y, 1f * tilesize);
            stroke(e.fout() * 1.5f + 0.5f);
            Lines.circle(e.x, e.y, 8f + e.finpow() * 92f);
            //Drawf.arrow(player.x, player.y, e.x, e.y, 5f * tilesize, 4f, Pal.command);
        }), Color.valueOf("eab678"));
        gatherMark = new MarkerType("Gather", new Effect(1800, e -> {
            color(e.color, 0.8f);
            stroke(2f);
            Lines.circle(e.x, e.y, 4f * tilesize);
            stroke(1f);
            Lines.circle(e.x, e.y, 4f * tilesize * (e.finpow() * 8f - (int)(e.finpow() * 8f)));
            for(int j = 0; j < 4; j++){
                Draw.alpha(Math.min(1f, j - e.fout() * 3));
                if(e.fout() * 3 < j){
                    for(int i = 0; i < 8; i++){
                        float rot = i * 45f;
                        float radius = 4f * tilesize + j * 6f + 4f;
                        drawSimpleArrow(e.x + Angles.trnsx(rot, radius), e.y + Angles.trnsy(rot, radius), e.x, e.y, 4f, 2f);
                    }
                }
            }
            Draw.color();
        }), Color.cyan);
        attackMark = new MarkerType("Attack", new Effect(1800, e -> {
            color(e.color);
            stroke(2f);
            Lines.circle(e.x, e.y, 1f * tilesize);
            float radius = 20f + e.finpow() * 80f;
            Lines.circle(e.x, e.y, radius);
            for(int i = 0; i < 4; i++){
                float rot = i * 90f + 45f + (-Time.time) % 360f;
                drawSimpleArrow(e.x + Angles.trnsx(rot, radius), e.y + Angles.trnsy(rot, radius), e.x, e.y, 6f + 4 * e.finpow(), 2f + 4 * e.finpow());
            }
        }), Color.valueOf("#DC143C"));
        defenseMark = new MarkerType("Defend", new Effect(1800, e -> {
            color(Pal.heal);
            if(e.fin() < 0.2f){
                Lines.circle(e.x, e.y, 20f + e.fin() * 400f);
                return;
            }
            Lines.circle(e.x, e.y, 101f);
            Lines.circle(e.x, e.y, 93f);
            for(int i = 0; i < 16; i++){
                float rot = i * 22.5f;
                if((e.fin() - 0.2f) * 50 > i)
                    drawSimpleArrow(e.x, e.y, e.x + Angles.trnsx(rot, 120f), e.y + Angles.trnsy(rot, 120f), 96f, 4f);
            }
        }), Color.acid);
        quesMark = new MarkerType("What", new Effect(1200, e -> {
            color(Color.violet);
            stroke(2f);
            Draw.alpha(Math.min(e.fin() * 5, 1));
            Lines.arc(e.x, e.y + 25f, 10, 0.75f, 270);
            Lines.line(e.x, e.y + 15f, e.x, e.y + 7f);
            Lines.circle(e.x, e.y, 3f);
            Lines.circle(e.x, e.y + 18.5f, 27f);
        }), Color.pink);
        allTypes = Seq.with(mark, gatherMark, attackMark, defenseMark, quesMark);
    }

    static{
        init();
        Events.run(WorldLoadEvent.class, () -> last = null);
        ui.hudGroup.addChildAt(0, new Element(){
            private final Circle outer = new Circle(0, 0, Scl.scl(120));
            private final Circle inner = new Circle(0, 0, Scl.scl(60));

            {
                name = "markPanUI";
                addListener(new InputListener(){
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                        if(inner.contains(x, y)){
                            mark.markWithMessage(panCenter);
                        }else if(outer.contains(x, y)){
                            int i = Mathf.round(Mathf.angle(x, y) / 360f * (allTypes.size - 1)) % (allTypes.size - 1);
                            allTypes.get(1 + i).markWithMessage(panCenter);
                        }
                        panCenter.setZero();
                        event.stop();
                        return true;
                    }
                });
            }

            @Override
            public void updateVisibility(){
                visible = !panCenter.isZero();
            }

            @Override
            public void act(float delta){
                super.act(delta);
                var pos = Core.camera.project(Tmp.v1.set(panCenter));
                setPosition(pos.x, pos.y);
            }

            @Override
            public void draw(){
                float oR = outer.radius, iR = inner.radius;
                Draw.color(Color.black, 0.6f);
                Fill.circle(x, y, oR);
                Draw.color(Pal.accent);
                Lines.circle(x, y, oR);
                Lines.circle(x, y, iR);
                int sp = allTypes.size - 1;
                for(int i = 0; i < sp; i++){//first in center,so skip
                    Lines.lineAngle(x, y, 360f * (i - 0.5f) / sp, oR - iR, iR);
                }

                FuncX.drawText(Tmp.v1.set(x, y), mark.localizedName + "\n[white]" + FormatDefault.formatTile(panCenter), Scl.scl(4f), mark.color);
                for(int i = 0; i < sp; i++){
                    var mark = allTypes.get(1 + i);
                    Vec2 tCenter = Tmp.v1.trns(360f * i / sp, (oR + iR) / 2).add(x, y);
                    FuncX.drawText(tCenter, mark.localizedName, Scl.scl(4f), mark.color);
                }
                Draw.reset();
            }

            @Override
            public Element hit(float x, float y, boolean touchable){
                return this;
            }
        });
    }

    public static void showPanUI(){
        panCenter.set(Core.input.mouseWorld());
    }

    public static void toggleMarkHitterUI(){
        if(markHitter.parent == null){
            ui.hudGroup.addChildAt(0, markHitter);
        }else{
            markHitter.remove();
        }
    }


    public final Color color;
    private final String name;
    private final Effect effect;
    public String localizedName;

    public MarkerType(String name, Effect effect, Color color){
        this.name = name;
        this.effect = effect;
        this.color = color;

        localizedName = Core.bundle.get("marker." + name + ".name", "unknown");
    }

    public MarkElement at(Position pos){
        var element = new MarkElement(this, pos);
        element.show();
        return element;
    }

    public void markWithMessage(Vec2 pos){
        if(last != null && last.time < heatTime){
            Vars.ui.announce("请不要频繁标记!");
            return;
        }
        last = at(pos);
        UIExt.sendChatMessage(Strings.format("<ARCxMDTX>[#@]<@>[]@", color, name, FormatDefault.formatTile(pos)));
    }

    public static void newMarkFromChat(String text, Vec2 pos){
        MarkerType type = mark;
        if(text.contains("集合")){
            type = gatherMark;
        }
        for(MarkerType it : allTypes){
            if(text.contains("<" + it.name + ">")){
                type = it;
                break;
            }
        }

        var exists = (MarkElement)Groups.draw.find(it -> it instanceof MarkElement e && e.message == null && e.within(pos.scl(tilesize), 2 * tilesize));
        last = exists != null ? exists : type.at(pos.scl(tilesize));
        last.message = text;
    }

    public static void eachActive(Cons<MarkElement> cons){
        if(last == null) return;
        Groups.draw.each(d -> {
            if(d instanceof MarkElement e) cons.get(e);
        });
    }

    public static void lockOnLastMark(){
        if(last == null) return;
        control.input.panCamera(Tmp.v1.set(last));
        last.show();
    }

    public static @Nullable Position getLastPos(){
        return last;
    }

    /** 在x,y附近length范围，绘制一个指向x2,y2的三角箭头，箭头大小radius */
    private static void drawSimpleArrow(float x, float y, float x2, float y2, float length, float radius){
        float angle = Angles.angle(x, y, x2, y2);
        Tmp.v1.set(x2, y2).sub(x, y).limit(length);
        float vx = Tmp.v1.x + x, vy = Tmp.v1.y + y;
        Fill.poly(vx, vy, 3, radius, angle);
    }


    public static class MarkElement extends EffectState{
        public final MarkerType type;
        @Nullable
        public String message;

        public MarkElement(MarkerType MarkerType, Position markPos){
            this.type = MarkerType;

            set(markPos);
            this.effect = MarkerType.effect;
            this.lifetime = effect.lifetime;
            this.color.set(MarkerType.color);
        }

        public void show(){
            time = 0;
            add();
        }

        @Override
        public float clipSize(){
            return Float.MAX_VALUE;
        }

        @Override
        public void draw(){
            super.draw();
            Draw.z(Layer.overlayUI);
            showArrow();
            if(message != null)
                FuncX.drawText(this, message);
        }

        private void showArrow(){
            Draw.reset();
            Drawf.arrow(player.x, player.y, x, y, 5f * tilesize, 4f, color);

            var p = Tmp.v1.set(this).sub(player).limit(4.5f * tilesize).add(player);
            FuncX.drawText(p, (int)(dst(player) / 8) + "", Scl.scl(1.25f), color);
        }
    }
}
