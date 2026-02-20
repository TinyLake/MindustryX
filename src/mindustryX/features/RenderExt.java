package mindustryX.features;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import kotlin.collections.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
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
import mindustryX.*;
import mindustryX.features.SettingsV2.*;
import mindustryX.features.draw.*;
import mindustryX.features.func.*;

import java.util.*;

import static mindustry.Vars.*;

public class RenderExt{
    public static boolean displayAllMessage;
    public static boolean arcChoiceUiIcon;
    public static boolean researchViewer;
    public static int hiddenItemTransparency;
    public static float overdriveZoneTransparency, mendZoneTransparency;
    public static boolean logicDisplayNoBorder, arcDrillMode;
    public static int blockRenderLevel;
    public static boolean renderSort;
    public static boolean massDriverLine;
    public static int massDriverLineInterval;
    public static boolean drawBars, drawBarsMend;
    public static boolean drawBlockDisabled;
    public static boolean showOtherInfo, editOtherBlock;
    public static boolean unitWeaponTargetLine, unitItemCarried;

    public static Color massDriverLineColor = Color.clear;
    public static Color playerEffectColor = Color.clear;

    public static final SettingsV2.CheckPref noBulletShow = new CheckPref("entityRender.noBulletShow");
    public static final SettingsV2.CheckPref unitHitbox = new CheckPref("entityRender.unitHitbox");
    public static final SettingsV2.CheckPref payloadPreview = new CheckPref("entityRender.payloadPreview");

    public static final SettingsV2.CheckPref unitHide = new CheckPref("entityRender.unitHide");
    public static final SettingsV2.CheckPref unitHideExcludePlayers = new CheckPref("entityRender.unitHideExcludePlayers", true);
    public static final SliderPref unitHideMinHealth = new SliderPref("entityRender.unitHideMinHealth", 0, 0, 4000, 50, v -> v + "[red]HP");

    public static final SettingsV2.CheckPref spawnerWaveDisplay = new CheckPref("gameUI.spawnerWaveDisplay", true);
    public static final SettingsV2.CheckPref transportScan = new CheckPref("gameUI.transportScan");
    public static final SettingsV2.CheckPref announceRtsTake = new CheckPref("gameUI.announceRtsTake", true);
    public static final SettingsV2.CheckPref deadOverlay = new CheckPref("gameUI.deadOverlay");

    public static final SettingsV2.CheckPref renderSort0 = new CheckPref("debug.renderSort");
    public static final SettingsV2.CheckPref arcChoiceUiIcon0 = new CheckPref("block.arcChoiceUiIcon");
    public static final SliderPref hiddenItemTransparency0 = new SliderPref("block.hiddenItemTransparency", 0, 0, 100, 2, v -> mindustryX.bundles.UiTextBundle.bundle().percentOrOff(v)); // 原文本:关闭
    public static final SliderPref overdriveZoneTransparency0 = new SliderPref("block.overdriveZoneTransparency", 0, 0, 100, 2, v -> mindustryX.bundles.UiTextBundle.bundle().percentOrOff(v)); // 原文本:关闭
    public static final SliderPref mendZoneTransparency0 = new SliderPref("block.mendZoneTransparency", 0, 0, 100, 2, v -> mindustryX.bundles.UiTextBundle.bundle().percentOrOff(v)); // 原文本:关闭
    public static final SliderPref healthBarMinHealth = new SliderPref("block.healthBarMinHealth", 0, 0, 4000, 50, v -> mindustryX.bundles.UiTextBundle.bundle().hpOrAll(v)); // 原文本:全部显示
    public static final ChoosePref blockRenderLevel0 = new ChoosePref("block.renderLevel", CollectionsKt.listOf(mindustryX.bundles.UiTextBundle.i("隐藏全部建筑"), mindustryX.bundles.UiTextBundle.i("只显示建筑状态"), mindustryX.bundles.UiTextBundle.i("全部显示")), 2); // 原文本:隐藏全部建筑 | 只显示建筑状态 | 全部显示
    public static final SettingsV2.CheckPref showOtherTeamState = new CheckPref("block.showOtherTeamState");
    public static final SettingsV2.CheckPref editOtherBlock0 = new CheckPref("block.editOtherBlock");
    public static final SettingsV2.CheckPref logicDisplayNoBorder0 = new CheckPref("block.logicDisplayNoBorder");


    static{
        noBulletShow.addFallback(SettingsV2.INSTANCE.map(new PersistentProvider.Arc<Boolean>("bulletShow"), it -> !it));
        unitHitbox.addFallbackName("unithitbox");
        unitHitbox.addFallbackName("payloadpreview");

        //noinspection unchecked
        unitHide.setPersistentProvider(PersistentProvider.Noop.INSTANCE);
        unitHideExcludePlayers.addFallbackName("unitHideExcludePlayers");
        unitHideMinHealth.addFallbackName("unitDrawMinHealth");

        deadOverlay.addFallbackName("deadOverlay");
        arcChoiceUiIcon0.addFallbackName("arcchoiceuiIcon");
        hiddenItemTransparency0.addFallbackName("HiddleItemTransparency");
        overdriveZoneTransparency0.addFallbackName("overdrive_zone");
        mendZoneTransparency0.addFallbackName("mend_zone");
        healthBarMinHealth.addFallbackName("blockbarminhealth");
        blockRenderLevel0.addFallbackName("blockRenderLevel");
        showOtherTeamState.addFallbackName("showOtherTeamState");
        editOtherBlock0.addFallbackName("editOtherBlock");
        logicDisplayNoBorder0.addFallbackName("logicDisplayNoBorder");
    }

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
            displayAllMessage = Core.settings.getBool("displayallmessage");
            arcChoiceUiIcon = arcChoiceUiIcon0.get();
            researchViewer = VarsX.researchViewer.get();
            hiddenItemTransparency = hiddenItemTransparency0.get();
            overdriveZoneTransparency = overdriveZoneTransparency0.get() / 100f;
            mendZoneTransparency = mendZoneTransparency0.get() / 100f;
            logicDisplayNoBorder = logicDisplayNoBorder0.get();
            arcDrillMode = Core.settings.getBool("arcdrillmode");
            blockRenderLevel = blockRenderLevel0.get();
            renderSort = renderSort0.get();
            massDriverLine = Core.settings.getBool("mass_driver_line");
            massDriverLineInterval = Core.settings.getInt("mass_driver_line_interval");
            drawBars = Core.settings.getBool("blockBars");
            drawBarsMend = Core.settings.getBool("blockBars_mend");
            drawBlockDisabled = Core.settings.getBool("blockdisabled");
            showOtherInfo = showOtherTeamState.get();
            editOtherBlock = editOtherBlock0.get();
            editOtherBlock &= !net.client();

            unitWeaponTargetLine = Core.settings.getBool("unitWeaponTargetLine");
            unitItemCarried = Core.settings.getBool("unitItemCarried");
        });
        Events.run(Trigger.draw, RenderExt::draw);
        Events.on(TileChangeEvent.class, RenderExt::onSetBlock);
        Events.on(ResetEvent.class, (e) -> {
            removePool.clear();
        });

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
        ArcSpawnerShow.update(player != null && !player.dead() && spawnerWaveDisplay.get());
        if(player == null || player.dead()) return;
        ArcRadar.draw();
        if(payloadPreview.get()) PayloadDropHint.draw(player);
        if(transportScan.get()) NewTransferScanMode.INSTANCE.draw();
    }

    public static void onGroupDraw(Drawc t){
        if(noBulletShow.get() && t instanceof Bulletc) return;
        if(!renderer.enableEffects && t instanceof EffectState) return;
        if(t instanceof Unitc u) hide:{
            if(u.isPlayer() && (u.isLocal() || unitHideExcludePlayers.get())) break hide;
            if(unitHide.get() || u.maxHealth() + u.shield() < unitHideMinHealth.get()) return;
        }
        t.draw();
        if(t instanceof Unit u){
            ArcUnits.draw(u);
            if(unitHitbox.get()) drawHitBox(u);
        }
    }

    public static void onBlockDraw(Tile tile, Block block, @Nullable Building build){
        if(blockRenderLevel < 2) return;
        block.drawBase(tile);
        if(displayAllMessage && build instanceof MessageBuild){
            Draw.z(Layer.overlayUI - 0.1f);
            build.drawSelect();
        }
        if(arcDrillMode && build instanceof DrillBuild drill){
            Draw.z(Layer.blockOver);
            arcDrillModeDraw(block, drill);
        }
        if(massDriverLine && build instanceof MassDriverBuild b){
            Draw.z(Layer.effect);
            drawMassDriverLine(b);
        }
        if(build != null && drawBars){
            Draw.z(Layer.turret + 4f);
            drawBars(build);
        }
        if(build instanceof BaseTurretBuild turretBuild){
            Draw.z(Layer.turret);
            ArcBuilds.arcTurret(turretBuild);
        }
    }

    private static void placementEffect(float x, float y, float lifetime, float range, Color color){
        placementEffect.lifetime = lifetime;
        placementEffect.at(x, y, range, color);
    }

    public static void onSetBlock(TileChangeEvent event){
        Building build = event.tile.build;
        if(build != null && ArcOld.showPlacementEffect.get()){
            if(build.block instanceof BaseTurret t)
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
        if(build.health / build.maxHealth < 0.9f && build.maxHealth > healthBarMinHealth.get())
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

    private static void drawHitBox(Unit unit){
        Draw.color(unit.team.color, 0.5f);
        Lines.circle(unit.x, unit.y, unit.hitSize / 2f);
        Draw.color();
    }

    static ObjectMap<String, HashSet<Unit>> removePool = new ObjectMap<>();

    public static void onRtsRemoveUnit(Player player, Unit unit){
        if(!announceRtsTake.get()) return;
        if(removePool.containsKey(player.name)){
            removePool.get(player.name).add(unit);
            return;
        }
        var set = new HashSet<Unit>();
        set.add(unit);
        removePool.put(player.name, set);
        Time.run(60f, () -> {
            var count = new ObjectIntMap<UnitType>();
            for(Unit u : set){
                count.increment(u.type);
            }
            StringBuilder builder = new StringBuilder();
            builder.append("[gold][MDTX][]").append(player.name).append(mindustryX.bundles.UiTextBundle.i("\n[white]分走了单位:")).append(" "); // 原文本:\n[white]分走了单位: {0}
            for(UnitType type : count.keys()){
                builder.append(type.emoji()).append("x").append(count.get(type)).append(" ");
            }
            UIExt.announce(builder.toString());
        });
    }
}
