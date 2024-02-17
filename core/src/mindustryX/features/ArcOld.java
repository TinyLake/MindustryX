package mindustryX.features;

import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustryX.features.ui.*;

import static arc.Core.settings;
import static mindustry.Vars.*;

public class ArcOld{
    public static void addSettings(){
        var setting = Vars.ui.settings;
        setting.addCategory("@settings.arc", Icon.star, (c) -> {
            c.addCategory("arcHudToolbox");
            c.sliderPref("AuxiliaryTable", 0, 0, 3, 1, s -> {
                if(s == 0){
                    return "关闭";
                }else if(s == 1){
                    return "左上-右";
                }else if(s == 2){
                    return "左上-下";
                }else if(s == 3){
                    return "右上-下";
                }else{
                    return "";
                }
            });
            c.checkPref("showAdvanceToolTable", false);
            c.checkPref("minimapTools", !mobile);
            c.checkPref("arcSpecificTable", true);
            c.checkPref("logicSupport", true);
            c.checkPref("powerStatistic", true);
            c.sliderPref("arccoreitems", 3, 0, 3, 1, s -> {
                if(s == 0){
                    return "不显示";
                }else if(s == 1){
                    return "资源状态";
                }else if(s == 2){
                    return "兵种状态";
                }else{
                    return "显示资源和兵种";
                }
            });
            c.sliderPref("arcCoreItemsCol", 5, 4, 15, 1, i -> i + "列");
            c.checkPref("showQuickToolTable", settings.getBool("showFloatingSettings"));
            c.sliderPref("arcDetailInfo", 1, 0, 1, 1, s -> {
                if(s == 0){
                    return "详细模式";
                }else if(s == 1){
                    return "简略模式";
                }else{
                    return s + "";
                }
            });

            c.addCategory("arcCgameview");
            c.checkPref("hoveredTileInfo", false);
            c.checkPref("alwaysshowdropzone", false);
            c.checkPref("showFlyerSpawn", false);
            c.checkPref("showFlyerSpawnLine", false);
            c.checkPref("bulletShow", true);
            if(Shaders.shield != null){
                c.checkPref("staticShieldsBorder", false);
            }

            c.addCategory("arcCDisplayBlock");
            c.sliderPref("blockRenderLevel", 2, 0, 2, 1, s -> {
                if(s == 0){
                    return "隐藏全部建筑";
                }else if(s == 1){
                    return "只显示建筑状态";
                }else if(s == 2){
                    return "全部显示";
                }else{
                    return s + "";
                }
            });
            c.checkPref("displayblock", true);
            c.checkPref("forceEnableDarkness", true);
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
            c.checkPref("arclogicbordershow", true);
            c.checkPref("arcPlacementEffect", false);

            c.sliderPref("mass_driver_line_alpha", 100, 0, 100, 1, i -> i > 0 ? i + "%" : "关闭");
            c.sliderPref("mass_driver_line_interval", 40, 8, 400, 4, i -> i / 8f + "格");
            c.stringInput("mass_driver_line_color", "ff8c66");

            c.addCategory("arcAddTurretInfo");
            c.checkPref("showTurretAmmo", false);
            c.checkPref("showTurretAmmoAmount", false);
            c.checkPref("arcTurretPlacementItem", false);
            c.checkPref("arcTurretPlaceCheck", false);
            c.sliderPref("turretShowRange", 0, 0, 3, 1, s -> {
                if(s == 0){
                    return "关闭";
                }else if(s == 1){
                    return "仅对地";
                }else if(s == 2){
                    return "仅对空";
                }else if(s == 3){
                    return "全部";
                }else{
                    return "";
                }
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


            c.sliderPref("unitWeaponRange", settings.getInt("unitAlertRange", 0), 0, 30, 1, s -> {
                if(s == 0){
                    return "关闭";
                }else if(s == 30){
                    return "一直开启";
                }else{
                    return s + "格";
                }
            });
            c.sliderPref("unitWeaponRangeAlpha", settings.getInt("unitweapon_range", 0), 0, 100, 1, i -> i > 0 ? i + "%" : "关闭");

            c.checkPref("unitWeaponTargetLine", false);
            c.checkPref("showminebeam", true);
            c.checkPref("unitItemCarried", false);
            c.checkPref("unithitbox", false);
            c.checkPref("unitLogicMoveLine", false);
            c.checkPref("unitLogicTimerBars", false);
            c.checkPref("arcBuildInfo", false);
            c.checkPref("unitbuildplan", false);
            c.checkPref("arcCommandTable", true);
            c.checkPref("alwaysShowUnitRTSAi", false);
            c.sliderPref("rtsWoundUnit", 0, 0, 100, 2, s -> s + "%");

            c.addCategory("arcPlayerEffect");
            c.stringInput("playerEffectColor", "ffd37f");
            c.sliderPref("unitTargetType", 0, 0, 5, 1, s -> {
                if(s == 0){
                    return "关闭";
                }else if(s == 1){
                    return "虚圆";
                }else if(s == 2){
                    return "攻击";
                }else if(s == 3){
                    return "攻击去边框";
                }else if(s == 4){
                    return "圆十字";
                }else if(s == 5){
                    return "十字";
                }else{
                    return s + "";
                }
            });
            c.sliderPref("superUnitEffect", 0, 0, 2, 1, s -> {
                if(s == 0){
                    return "关闭";
                }else if(s == 1){
                    return "独一无二";
                }else if(s == 2){
                    return "全部玩家";
                }else{
                    return s + "";
                }
            });
            c.sliderPref("playerEffectCurStroke", 0, 1, 30, 1, i -> (float)i / 10f + "Pixel(s)");


            c.addCategory("arcShareinfo");
            c.sliderPref("chatValidType", 0, 0, 3, 1, s -> {
                if(s == 0){
                    return "原版模式";
                }else if(s == 1){
                    return "纯净聊天";
                }else if(s == 2){
                    return "服务器记录";
                }else if(s == 3){
                    return "全部记录";
                }else{
                    return s + "";
                }
            });
            c.checkPref("arcPlayerList", true);
            c.checkPref("ShowInfoPopup", true);
            c.checkPref("arcShareWaveInfo", false);
            c.checkPref("arcAlwaysTeamColor", false);
            c.checkPref("arcSelfName", false);

            c.addCategory("arcWeakCheat");
            c.checkPref("save_more_map", false);
            c.checkPref("forceIgnoreAttack", false);
            c.checkPref("allBlocksReveal", false, b -> AdvanceToolTable.allBlocksReveal = b);
            c.checkPref("worldCreator", false, b -> AdvanceToolTable.worldCreator = b);
            c.checkPref("overrideSkipWave", false);
            c.checkPref("forceConfigInventory", false);
            c.addCategory("arcStrongCheat");
            c.checkPref("showOtherTeamResource", false);
            c.checkPref("showOtherTeamState", false);
            c.checkPref("selectTeam", false);
            c.checkPref("playerNeedShooting", false);
            c.checkPref("otherCheat", false);
        });
        setting.addCategory("@settings.specmode", Icon.info, (c) -> {
            c.addCategory("moreContent");
            c.checkPref("override_boss_shown", false);
            c.checkPref("shiftCopyIcon", true);
            c.checkPref("githubMirror", false);
            c.checkPref("modMode", false);
            c.sliderPref("minimapSize", 140, 40, 400, 10, i -> i + "");
            c.sliderPref("maxSchematicSize", 32, 32, 257, 1, v -> {
                maxSchematicSize = v == 257 ? Integer.MAX_VALUE : v;
                return v == 257 ? "无限" : String.valueOf(v);
            });
            c.sliderPref("itemSelectionHeight", 4, 4, 12, i -> i + "行");
            c.sliderPref("itemSelectionWidth", 4, 4, 12, i -> i + "列");
            c.sliderPref("blockInventoryWidth", 3, 3, 16, i -> i + "");
            c.sliderPref("editorBrush", 4, 3, 12, i -> i + "");
            c.checkPref("autoSelSchematic", false);
            c.checkPref("researchViewer", false);
            c.checkPref("arcShareMedia", true);


            c.addCategory("arcRadar");
            c.sliderPref("radarMode", 0, 0, 30, 1, s -> {
                if(s == 0) return "关闭";
                else if(s == 30) return "一键开关";
                else{
                    return "[lightgray]x[white]" + Strings.autoFixed(s * 0.2f, 1) + "倍搜索";
                }
            });
            c.sliderPref("radarSize", 0, 0, 50, 1, s -> {
                if(s == 0) return "固定大小";
                else{
                    return "[lightgray]x[white]" + Strings.autoFixed(s * 0.1f, 1) + "倍";
                }
            });

            c.addCategory("personalized");
            c.checkPref("menuFloatText", true);
            c.sliderPref("menuFlyersCount", 0, -15, 50, 5, i -> i + "");
            c.checkPref("menuFlyersRange", false);
            c.checkPref("menuFlyersFollower", false);
            c.checkPref("colorizedContent", false);
            c.sliderPref("fontSize", 10, 5, 25, 1, i -> "x " + Strings.fixed(i * 0.1f, 1));
            c.stringInput("themeColor", "ffd37f");
            c.stringInput("arcBackgroundPath", "");

            c.addCategory("developerMode");
            c.checkPref("arcInfSchem", false);
            c.checkPref("rotateCanvas", false);
            c.checkPref("limitupdate", false, v -> {
                if(!v) return;
                settings.put("limitupdate", false);
                ui.showConfirm("确认开启限制更新", "此功能可以大幅提升fps，但会导致视角外的一切停止更新\n在服务器里会造成不同步\n强烈不建议在单人开启\n\n[darkgray]在帧数和体验里二选一", () -> {
                    settings.put("limitupdate", true);
                });
            });
            c.sliderPref("limitdst", 10, 0, 100, 1, s -> s + "格");
            c.checkPref("developMode", false);
        });
    }
}
