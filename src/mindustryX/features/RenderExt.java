package mindustryX.features;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.BaseTurret.*;
import mindustry.world.blocks.distribution.MassDriver.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.MessageBlock.*;
import mindustry.world.blocks.production.Drill.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.*;
import mindustryX.features.draw.*;
import mindustryX.features.func.*;

import static mindustry.Vars.*;

public class RenderExt{
    public static boolean bulletShow, showMineBeam, displayAllMessage;
    public static boolean arcChoiceUiIcon;
    public static boolean researchViewer;
    public static boolean showPlacementEffect;
    public static int hiddenItemTransparency;
    public static int blockBarMinHealth;
    public static float overdriveZoneTransparency, mendZoneTransparency;
    public static boolean logicDisplayNoBorder, arcDrillMode;
    public static int blockRenderLevel;
    public static boolean renderSort;
    public static boolean massDriverLine;
    public static int massDriverLineInterval;
    public static boolean drawBars, drawBarsMend;
    public static float healthBarMinHealth;
    public static boolean payloadPreview;
    public static boolean deadOverlay;
    public static boolean drawBlockDisabled;
    public static boolean showOtherInfo, editOtherBlock;

    public static boolean unitHide = false;
    public static Color massDriverLineColor = Color.clear;
    public static Color playerEffectColor = Color.clear;

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
            showMineBeam = !unitHide && Core.settings.getBool("showminebeam");
            displayAllMessage = Core.settings.getBool("displayallmessage");
            arcChoiceUiIcon = Core.settings.getBool("arcchoiceuiIcon");
            researchViewer = Core.settings.getBool("researchViewer");
            showPlacementEffect = Core.settings.getBool("arcPlacementEffect");
            hiddenItemTransparency = Core.settings.getInt("HiddleItemTransparency");
            blockBarMinHealth = Core.settings.getInt("blockbarminhealth");
            overdriveZoneTransparency = Core.settings.getInt("overdrive_zone") / 100f;
            mendZoneTransparency = Core.settings.getInt("mend_zone") / 100f;
            logicDisplayNoBorder = Core.settings.getBool("logicDisplayNoBorder");
            arcDrillMode = Core.settings.getBool("arcdrillmode");
            blockRenderLevel = Core.settings.getInt("blockRenderLevel");
            renderSort = Core.settings.getBool("renderSort");
            massDriverLine = Core.settings.getBool("mass_driver_line");
            massDriverLineInterval = Core.settings.getInt("mass_driver_line_interval");
            drawBars = Core.settings.getBool("blockBars");
            drawBarsMend = Core.settings.getBool("blockBars_mend");
            healthBarMinHealth = Core.settings.getInt("blockbarminhealth");
            payloadPreview = Core.settings.getBool("payloadpreview");
            deadOverlay = Core.settings.getBool("deadOverlay");
            drawBlockDisabled = Core.settings.getBool("blockdisabled");
            showOtherInfo = Core.settings.getBool("showOtherTeamState");
            editOtherBlock = Core.settings.getBool("editOtherBlock");
            editOtherBlock &= !net.client();
        });
        Events.run(Trigger.draw, RenderExt::draw);
        Events.on(TileChangeEvent.class, RenderExt::onSetBlock);

        //Optimize white() for ui
        AtlasRegion white = Core.atlas.white(),
        whiteUI = Core.atlas.find("whiteui"),
        whiteSet = new AtlasRegion(white){
            @Override
            public void set(TextureRegion region0){
                super.set(region0);
                if(region0 instanceof AtlasRegion region){
                    name = region.name;
                    offsetX = region.offsetX;
                    offsetY = region.offsetY;
                    packedWidth = region.packedWidth;
                    packedHeight = region.packedHeight;
                    originalWidth = region.originalWidth;
                    originalHeight = region.originalHeight;
                    rotate = region.rotate;
                    splits = region.splits;
                }
            }
        };
        Reflect.set(TextureAtlas.class, Core.atlas, "white", whiteSet);
        Events.run(Trigger.uiDrawBegin, () -> whiteSet.set(whiteUI));
        Events.run(Trigger.uiDrawEnd, () -> whiteSet.set(white));
    }

    private static void draw(){
        if(RenderExt.payloadPreview) PayloadDropHint.draw(player);
    }

    public static void onGroupDraw(Drawc t){
        if(!bulletShow && t instanceof Bulletc) return;
        t.draw();
    }

    public static void onBlockDraw(Tile tile, Block block, @Nullable Building build){
        if(blockRenderLevel < 2) return;
        block.drawBase(tile);
        if(displayAllMessage && build instanceof MessageBuild)
            Draw.draw(Layer.overlayUI - 0.1f, build::drawSelect);
        if(arcDrillMode && build instanceof DrillBuild drill)
            arcDrillModeDraw(block, drill);
        if(massDriverLine && build instanceof MassDriverBuild b)
            drawMassDriverLine(b);
        if(build != null && drawBars)
            drawBars(build);
        if(build instanceof BaseTurretBuild turretBuild)
            ArcBuilds.arcTurret(turretBuild);
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

    /** 在转头旁边显示矿物类型 */
    private static void arcDrillModeDraw(Block block, DrillBuild build){
        Item dominantItem = build.dominantItem;
        if(dominantItem == null) return;
        int size = block.size;
        float dx = build.x - size * tilesize / 2f + 5, dy = build.y - size * tilesize / 2f + 5;
        float iconSize = 5f;
        Draw.rect(dominantItem.fullIcon, dx, dy, iconSize, iconSize);
        Draw.reset();

        float eff = Mathf.lerp(0, 1, Math.min(1f, (float)build.dominantItems / (size * size)));
        if(eff < 0.9f){
            Draw.alpha(0.5f);
            Draw.color(dominantItem.color);
            Lines.stroke(1f);
            Lines.arc(dx, dy, iconSize * 0.75f, eff);
        }
    }

    private static void drawMassDriverLine(MassDriverBuild build){
        if(build.waitingShooters.isEmpty()) return;
        Draw.z(Layer.effect);
        float x = build.x, y = build.y, size = build.block.size;
        float sin = Mathf.absin(Time.time, 6f, 1f);
        for(var shooter : build.waitingShooters){
            Lines.stroke(2f, Pal.placing);
            Drawf.dashLine(RenderExt.massDriverLineColor, shooter.x, shooter.y, x, y);
            int slice = Mathf.floorPositive(build.dst(shooter) / RenderExt.massDriverLineInterval);
            Vec2 interval = Tmp.v1.set(build).sub(shooter).setLength(RenderExt.massDriverLineInterval);
            float dx = interval.x, dy = interval.y;
            for(int i = 0; i < slice; i++){
                Drawf.arrow(shooter.x + dx * i, shooter.y + dy * i, x, y, size * tilesize + sin, 4f + sin, RenderExt.massDriverLineColor);
            }
        }
    }

    private static void drawBars(Building build){
        if(build.health / build.maxHealth < 0.9f && build.maxHealth > healthBarMinHealth)
            drawBar(build, build.team.color, Pal.health, build.health / build.maxHealth);
        if(drawBarsMend){
            if(build instanceof MendProjector.MendBuild b){
                var block = (MendProjector)build.block;
                drawBar(build, Color.black, Pal.heal, b.charge / block.reload);
            }else if(build instanceof ForceProjector.ForceBuild b && b.buildup > 0){
                var block = (ForceProjector)build.block;
                float ratio = 1 - b.buildup / (block.shieldHealth + block.phaseShieldBoost * b.phaseHeat);
                drawBar(build, Color.black, b.broken ? Pal.remove : Pal.stat, ratio);
            }
        }
        float buildRatio = -1, leftTime = 0;
        if(build instanceof Reconstructor.ReconstructorBuild b){
            buildRatio = b.fraction();
            leftTime = ((Reconstructor)build.block).constructTime - b.progress;
        }else if(build instanceof UnitAssembler.UnitAssemblerBuild b){
            buildRatio = b.progress;
            leftTime = b.plan().time * (1 - b.progress);
        }else if(build instanceof UnitFactory.UnitFactoryBuild b){
            buildRatio = b.fraction();
            leftTime = b.currentPlan == -1 ? -1 : (((UnitFactory)build.block).plans.get(b.currentPlan).time - b.progress);
        }
        if(buildRatio >= 0){
            drawBar(build, Color.black, Pal.accent, buildRatio);
            String progressT = Strings.format("[stat]@% | @s", (int)(Mathf.clamp(buildRatio, 0f, 1f) * 100), leftTime < 0 ? Iconc.cancel : Strings.fixed(leftTime / (60f * Vars.state.rules.unitBuildSpeed(build.team) * build.timeScale()), 0));
            FuncX.drawText(Tmp.v1.set(build).add(0, build.block.offset * 0.8f - 5f), progressT, 0.9f);
        }
    }

    private static void drawBar(Building build, Color bg, Color fg, Float ratio){
        Draw.z(Layer.turret + 4f);
        float x = build.x, size = build.block.size * tilesize * 0.5f;
        float x1 = x - size * 0.6f, x2 = x + size * 0.6f, y = build.y + size * 0.8f;
        Draw.color(bg, 0.3f);
        Lines.stroke(4f);
        Lines.line(x1, y, x2, y);

        Draw.color(fg, 0.6f);
        Lines.stroke(2f);
        Lines.line(x1, y, Mathf.lerp(x1, x2, Mathf.clamp(ratio, 0f, 1f)), y);

        Draw.reset();
    }
}
