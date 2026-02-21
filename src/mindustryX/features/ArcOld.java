package mindustryX.features;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustryX.*;
import mindustryX.features.SettingsV2.*;

import static arc.Core.settings;
import static mindustry.Vars.*;

public class ArcOld{
    public static final CheckPref colorizedContent = new CheckPref("arcExtra.colorizedContent");
    public static final TextPref backgroundPath = new TextPref("arcExtra.backgroundPath");
    public static final CheckPref showPlacementEffect = new CheckPref("block.arcPlacementEffect");
    public static final CheckPref schematicShare = new CheckPref("arcExtra.schematicShare", true);

    static{
        colorizedContent.addFallbackName("colorizedContent");
        backgroundPath.addFallbackName("arcBackgroundPath");
        showPlacementEffect.addFallbackName("arcPlacementEffect");
    }

    private static Seq<Fi> backgrounds = Seq.with();

    public static void nextBackground(Image image){
        if(backgroundPath.changed()){
            if(backgroundPath.get().isEmpty()){
                backgrounds = Seq.with();
            }else{
                backgrounds = Core.files.absolute(backgroundPath.get()).findAll(f -> !f.isDirectory() && (f.extEquals("png") || f.extEquals("jpg") || f.extEquals("jpeg")));
            }
        }
        image.setDrawable((Drawable)null);
        if(backgrounds.size == 0) return;
        Fi file = backgrounds.random();
        mainExecutor.submit(() -> {
            try{
                var pixmap = new PixmapTextureData(new Pixmap(file), false, true);
                Core.app.post(() -> {
                    var texture = new TextureRegion(new Texture(pixmap));
                    if(image.getDrawable() != null) ((TextureRegion)image.getDrawable()).texture.dispose();
                    image.setDrawable(texture);
                });
            }catch(Exception e){
                Core.app.post(() -> ui.showException(VarsX.bundle.invalidBackgroundImage(file.path()), e));
            }
        });
    }

    public static void doOreAdsorption(){
        Unit unit = player.unit();
        if(Core.scene.hasMouse() || unit == null) return;
        Tile center = unit.tileOn();
        if(center == null) return;
        center.circle(Mathf.ceil(unit.type.mineRange / 8f), tile -> {
            Tile ptile = unit.mineTile;
            if((ptile == null || player.dst(ptile) > player.dst(tile) || ptile.drop() == Items.sand) &&
            unit.validMine(tile) && unit.acceptsItem(unit.getMineResult(tile)) && tile.drop() != Items.sand){
                unit.mineTile = tile;
            }
        });
    }

    public static void addSettings(){
        Vars.ui.settings.addCategory("@settings.arc", Icon.star, (c) -> {
            c.addCategory("arcCgameview");
            c.checkPref("hoveredTileInfo", false);
            c.checkPref("arcAlwaysTeamColor", false);

            c.addCategory("arcCDisplayBlock");
            c.checkPref("blockdisabled", false);
            c.checkPref("blockBars", false);
            c.checkPref("blockBars_mend", false);
            c.checkPref("arcdrillmode", false);

            c.checkPref("mass_driver_line", true);
            c.sliderPref("mass_driver_line_interval", 40, 8, 400, 4, i -> VarsX.bundle.tiles(i / 8f));
            {
                Cons<String> changed = (t) -> {
                    try{
                        RenderExt.massDriverLineColor = Color.valueOf(t);
                    }catch(Exception e){
                        RenderExt.massDriverLineColor = Color.valueOf("ff8c66");
                    }
                };
                c.textPref("mass_driver_line_color", "ff8c66", changed);
                changed.get(settings.getString("mass_driver_line_color"));
            }

            c.addCategory("arcAddTurretInfo");
            c.checkPref("showTurretAmmo", false);
            c.checkPref("showTurretAmmoAmount", false);
            c.sliderPref("turretShowRange", 0, 0, 3, 1, VarsX.bundle::turretShowRangeMode);
            c.checkPref("turretForceShowRange", false);
            c.sliderPref("turretAlertRange", 0, 0, 30, 1, VarsX.bundle::tilesOrOff);
            c.checkPref("blockWeaponTargetLine", false);
            c.checkPref("blockWeaponTargetLineWhenIdle", false);

            c.addCategory("arcAddUnitInfo");
            c.checkPref("unitHealthBar", false);
            c.sliderPref("unitWeaponRange", settings.getInt("unitAlertRange", 0), 0, 30, 1, VarsX.bundle::unitWeaponRangeMode);
            c.sliderPref("unitWeaponRangeAlpha", settings.getInt("unitweapon_range", 0), 0, 100, 1, VarsX.bundle::percentOrOff);
            c.checkPref("unitWeaponTargetLine", false);
            c.checkPref("unitItemCarried", true);
            c.checkPref("unitLogicMoveLine", false);
            c.checkPref("unitLogicTimerBars", false);
            c.checkPref("arcBuildInfo", false);
            c.checkPref("unitbuildplan", false);
            c.checkPref("alwaysShowUnitRTSAi", false);
            c.sliderPref("rtsWoundUnit", 0, 0, 100, 2, s -> s + "%");

            c.addCategory("arcPlayerEffect");
            {
                Cons<String> changed = (t) -> {
                    try{
                        RenderExt.playerEffectColor = Color.valueOf(t);
                    }catch(Exception e){
                        RenderExt.playerEffectColor = Pal.accent;
                    }
                };
                c.textPref("playerEffectColor", "ffd37f", changed);
                changed.get(settings.getString("playerEffectColor"));
            }
            c.sliderPref("unitTargetType", 0, 0, 5, 1, VarsX.bundle::unitTargetTypeMode);
            c.sliderPref("superUnitEffect", 0, 0, 2, 1, VarsX.bundle::superUnitEffectMode);
            c.sliderPref("playerEffectCurStroke", 0, 1, 30, 1, i -> (float)i / 10f + "Pixel(s)");

            c.addCategory("arcWeakCheat");
            c.checkPref("save_more_map", false);
            c.checkPref("overrideSkipWave", false);
            c.checkPref("playerNeedShooting", false);
        });
    }

    public static void colorizeContent(){
        if(!colorizedContent.get()) return;
        content.items().each(c -> colorizeContent(c, c.color));
        content.liquids().each(c -> colorizeContent(c, c.color));
        content.statusEffects().each(c -> colorizeContent(c, c.color));
        content.planets().each(c -> colorizeContent(c, c.atmosphereColor));
        content.blocks().each(c -> {
            if(c.hasColor) colorizeContent(c, c.mapColor.cpy().mul(1.2f));
            else if(c.itemDrop != null) colorizeContent(c, c.itemDrop.color);
        });
    }

    private static void colorizeContent(UnlockableContent c, Color color){
        c.localizedName = "[#" + color + "]" + c.localizedName + "[]";
    }

    private static @Nullable Teamc autoTarget = null;

    public static void updatePlayer(){
        Unit unit = player.unit();
        if(unit == null) return;
        if(Core.settings.getBool("forceBoost")){
            player.boosting = true;
        }

        //Add Auto Targeting for Desktop
        if(control.input instanceof DesktopInput && !Core.input.keyDown(Binding.select) && Core.settings.getBool("autotarget")){
            UnitType type = unit.type;
            //validate
            if(autoTarget != null){
                boolean validHealTarget = type.canHeal && autoTarget instanceof Building b && b.isValid() && autoTarget.team() == unit.team && b.damaged() && autoTarget.within(unit, type.range);
                if((Units.invalidateTarget(autoTarget, unit, type.range) && !validHealTarget) || state.isEditor()){
                    autoTarget = null;
                }
            }
            //retarget
            if(autoTarget == null){
                float range = unit.hasWeapons() ? unit.range() : 0f;
                player.shooting = false;
                if(!(player.unit() instanceof BlockUnitUnit u && u.tile() instanceof ControlBlock c && !c.shouldAutoTarget())){
                    boolean targetBuilding = type.targetGround && type.hasWeapons() && type.weapons.first().bullet.buildingDamageMultiplier > 0.05f;
                    autoTarget = Units.closestTarget(unit.team, unit.x, unit.y, range, u -> u.checkTarget(type.targetAir, type.targetGround), u -> targetBuilding);

                    if(type.canHeal && autoTarget == null){
                        autoTarget = Geometry.findClosest(unit.x, unit.y, indexer.getDamaged(unit.team));
                        if(autoTarget != null && !unit.within(autoTarget, range)){
                            autoTarget = null;
                        }
                    }
                }
            }
            //aim
            if(autoTarget != null){
                Vec2 intercept = Predict.intercept(unit, autoTarget, unit.hasWeapons() ? type.weapons.first().bullet.speed : 0f);

                player.shooting = true;
                unit.aim(intercept);
            }
        }else{
            autoTarget = null;
        }
    }
}
