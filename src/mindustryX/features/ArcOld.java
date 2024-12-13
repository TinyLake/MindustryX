package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustryX.features.Settings.*;

import static arc.Core.settings;
import static arc.graphics.Color.RGBtoHSV;
import static mindustry.Vars.*;

public class ArcOld{
    public static void init(Seq<LazySettingsCategory> categories){
        categories.add(new LazySettingsCategory("@settings.arc", () -> Icon.star, (c) -> {
            c.addCategory("arcCgameview");
            c.checkPref("hoveredTileInfo", false);
            c.checkPref("showFlyerSpawn", false);
            c.checkPref("showFlyerSpawnLine", false);
            c.checkPref("bulletShow", true);
            if(Shaders.shield != null){
                c.checkPref("staticShieldsBorder", false);
            }

            c.addCategory("arcCDisplayBlock");
            c.checkPref("forceEnableDarkness", true, (b) -> enableDarkness = b);
            enableDarkness = settings.getBool("forceEnableDarkness");
            c.sliderPref("HiddleItemTransparency", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.sliderPref("overdrive_zone", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.sliderPref("mend_zone", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.checkPref("blockdisabled", false);
            c.checkPref("blockBars", false);
            c.sliderPref("blockbarminhealth", 0, 0, 4000, 50, i -> i + "[red]HP");
            c.checkPref("blockBars_mend", false);
            c.checkPref("arcdrillmode", false);
            c.checkPref("arcDrillProgress", false);
            c.checkPref("arcchoiceuiIcon", false);
            c.checkPref("arcPlacementEffect", false);

            c.checkPref("mass_driver_line", true);
            c.sliderPref("mass_driver_line_interval", 40, 8, 400, 4, i -> i / 8f + "格");
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
            c.checkPref("arcTurretPlacementItem", false);
            c.checkPref("arcTurretPlaceCheck", false);
            c.sliderPref("turretShowRange", 0, 0, 3, 1, s -> switch(s){
                case 0 -> "关闭";
                case 1 -> "仅对地";
                case 2 -> "仅对空";
                case 3 -> "全部";
                default -> "";
            });
            c.checkPref("turretForceShowRange", false);
            c.sliderPref("turretAlertRange", 0, 0, 30, 1, i -> i > 0 ? i + "格" : "关闭");
            c.checkPref("blockWeaponTargetLine", false);
            c.checkPref("blockWeaponTargetLineWhenIdle", false);

            c.addCategory("arcAddUnitInfo");
            c.checkPref("alwaysShowPlayerUnit", false);

            c.sliderPref("unitTransparency", 100, 0, 100, 5, i -> i > 0 ? i + "%" : "关闭");
            c.sliderPref("unitDrawMinHealth", settings.getInt("minhealth_unitshown", 0), 0, 2500, 50, i -> i + "[red]HP");

            c.checkPref("unitHealthBar", false);
            c.sliderPref("unitBarDrawMinHealth", settings.getInt("minhealth_unithealthbarshown", 0), 0, 2500, 100, i -> i + "[red]HP");


            c.sliderPref("unitWeaponRange", settings.getInt("unitAlertRange", 0), 0, 30, 1, s -> switch(s){
                case 0 -> "关闭";
                case 30 -> "一直开启";
                default -> s + "格";
            });
            c.sliderPref("unitWeaponRangeAlpha", settings.getInt("unitweapon_range", 0), 0, 100, 1, i -> i > 0 ? i + "%" : "关闭");

            c.checkPref("unitWeaponTargetLine", false);
            c.checkPref("showminebeam", true);
            c.checkPref("unitItemCarried", true);
            c.checkPref("unithitbox", false);
            c.checkPref("unitLogicMoveLine", false);
            c.checkPref("unitLogicTimerBars", false);
            c.checkPref("arcBuildInfo", false);
            c.checkPref("unitbuildplan", false);
            c.checkPref("arcCommandTable", true);
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
            c.sliderPref("unitTargetType", 0, 0, 5, 1, s -> switch(s){
                case 0 -> "关闭";
                case 1 -> "虚圆";
                case 2 -> "攻击";
                case 3 -> "攻击去边框";
                case 4 -> "圆十字";
                case 5 -> "十字";
                default -> s + "";
            });
            c.sliderPref("superUnitEffect", 0, 0, 2, 1, s -> switch(s){
                case 0 -> "关闭";
                case 1 -> "独一无二";
                case 2 -> "全部玩家";
                default -> s + "";
            });
            c.sliderPref("playerEffectCurStroke", 0, 1, 30, 1, i -> (float)i / 10f + "Pixel(s)");


            c.addCategory("arcShareinfo");
            c.checkPref("ShowInfoPopup", true);
            c.checkPref("arcAlwaysTeamColor", false);

            c.addCategory("arcWeakCheat");
            c.checkPref("save_more_map", false);
            c.checkPref("forceIgnoreAttack", false);
            c.checkPref("allUnlocked", false);
            c.checkPref("worldCreator", false);
            c.checkPref("overrideSkipWave", false);
            c.addCategory("arcStrongCheat");
            c.checkPref("playerNeedShooting", false);
        }));
    }

    public static void colorizeContent(){
        if(!settings.getBool("colorizedContent")) return;
        content.items().each(c -> colorizeContent(c, c.color));
        content.liquids().each(c -> colorizeContent(c, c.color));
        content.statusEffects().each(c -> colorizeContent(c, c.color));
        content.planets().each(c -> colorizeContent(c, c.atmosphereColor));
        content.blocks().each(c -> {
            if(c.hasColor) colorizeContent(c, blockColor(c));
            else if(c.itemDrop != null) colorizeContent(c, c.itemDrop.color);
        });
    }

    private static void colorizeContent(UnlockableContent c, Color color){
        c.localizedName = "[#" + color + "]" + c.localizedName + "[]";
    }

    private static Color blockColor(Block block){
        Color bc = new Color(0, 0, 0, 1);
        Color bestColor = new Color(0, 0, 0, 1);
        int highestS = 0;
        if(!block.synthetic()){
            PixmapRegion image = Core.atlas.getPixmap(block.fullIcon);
            for(int x = 0; x < image.width; x++)
                for(int y = 0; y < image.height; y++){
                    bc.set(image.get(x, y));
                    int s = RGBtoHSV(bc)[1] * RGBtoHSV(bc)[1] + RGBtoHSV(bc)[2] + RGBtoHSV(bc)[2];
                    if(s > highestS){
                        highestS = s;
                        bestColor = bc;
                    }
                }
        }else{
            return block.mapColor.cpy().mul(1.2f);
        }
        return bestColor;
    }
}
