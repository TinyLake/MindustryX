package mindustryX.features;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.MessageBlock.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.tilesize;

public class RenderExt{
    public static boolean bulletShow, showMineBeam, displayAllMessage;
    public static boolean arcChoiceUiIcon;
    public static boolean researchViewer;
    public static boolean showPlacementEffect;
    public static int hiddenItemTransparency;
    public static int blockBarMinHealth;
    public static float overdriveZoneTransparency;

    public static boolean unitHide = false;

    private static Effect placementEffect;

    public static void init(){
        placementEffect = new Effect(0f, e -> {
            Draw.color(e.color);
            float range = e.rotation;
            Lines.stroke((1.5f - e.fin()) * (range / 100));
            if(e.fin() < 0.7f) Lines.circle(e.x, e.y, (float)((1 - Math.pow((0.7f - e.fin()) / 0.7f, 2f)) * range));
            else{
                Draw.alpha((1 - e.fin()) * 5f);
                Lines.circle(e.x, e.y, range);
            }
        });

        Events.run(Trigger.update, () -> {
            bulletShow = Core.settings.getBool("bulletShow");
            showMineBeam = Core.settings.getBool("showminebeam");
            displayAllMessage = Core.settings.getBool("displayallmessage");
            arcChoiceUiIcon = Core.settings.getBool("arcchoiceuiIcon");
            researchViewer = Core.settings.getBool("researchViewer");
            showPlacementEffect = Core.settings.getBool("arcPlacementEffect");
            hiddenItemTransparency = Core.settings.getInt("HiddleItemTransparency");
            blockBarMinHealth = Core.settings.getInt("blockbarminhealth");
            overdriveZoneTransparency = Core.settings.getInt("overdrive_zone") / 100f;
        });
        Events.run(Trigger.draw, RenderExt::draw);
        Events.on(TileChangeEvent.class, RenderExt::onSetBlock);
    }

    private static void draw(){

    }

    public static void onGroupDraw(Drawc t){
        if(unitHide && t instanceof Minerc) return;
        if(!bulletShow && t instanceof Bulletc) return;
        if(!showMineBeam && t instanceof Minerc) return;
        t.draw();
        if(displayAllMessage && t instanceof MessageBuild build){
            Draw.draw(Layer.overlayUI - 0.1f, build::drawSelect);
        }
    }

    private static void placementEffect(float x, float y, float lifetime, float range, Color color){
        placementEffect.lifetime = lifetime;
        placementEffect.at(x, y, range, color);
    }

    public static void onSetBlock(TileChangeEvent event){
        Building build = event.tile.build;
        if(build != null && showPlacementEffect){
            if(build.block instanceof BaseTurret t && build.health > blockBarMinHealth)
                placementEffect(build.x, build.y, 120f, t.range, build.team.color);
            else if(build.block instanceof Radar t)
                placementEffect(build.x, build.y, 120f, t.fogRadius * tilesize, build.team.color);
            else if(build.block instanceof CoreBlock t)
                placementEffect(build.x, build.y, 180f, t.fogRadius * tilesize, build.team.color);
            else if(build.block instanceof MendProjector t)
                placementEffect(build.x, build.y, 120f, t.range, Pal.heal);
            else if(build.block instanceof OverdriveProjector t)
                placementEffect(build.x, build.y, 120f, t.range, t.baseColor);
            else if(build.block instanceof LogicBlock t)
                placementEffect(build.x, build.y, 120f, t.range, t.mapColor);
        }
    }
}
