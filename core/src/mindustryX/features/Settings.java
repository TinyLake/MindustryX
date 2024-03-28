package mindustryX.features;

import arc.*;
import mindustry.*;
import mindustry.gen.*;

import static mindustry.Vars.maxSchematicSize;

public class Settings{
    public static void baseSettings(){
        int v = Core.settings.getInt("maxSchematicSize");
        maxSchematicSize = v == 257 ? Integer.MAX_VALUE : v;
    }
    public static void addSettings(){
        var setting = Vars.ui.settings;
        setting.addCategory("@settings.category.mindustryX", Icon.box, (c) -> {
            c.checkPref("showUpdateDialog", true);
            c.checkPref("githubMirror", false);

            c.addCategory("arcReWork");
            c.checkPref("replayRecord", false);
            c.checkPref("menuFloatText", true);
            c.checkPref("showAdvanceToolTable", false);
            c.checkPref("researchViewer", false);
            c.sliderPref("minimapSize", 140, 40, 400, 10, i -> i + "");
            c.sliderPref("maxSchematicSize", 32, 32, 257, 1, v -> {
                maxSchematicSize = v == 257 ? Integer.MAX_VALUE : v;
                return v == 257 ? "无限" : String.valueOf(v);
            });

            c.addCategory("blockSettings");
            c.checkPref("staticShieldsBorder", false);
            c.checkPref("arcTurretPlaceCheck", false);
            c.checkPref("arcchoiceuiIcon", false);
            c.sliderPref("HiddleItemTransparency", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.sliderPref("overdrive_zone", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.checkPref("arcPlacementEffect", false);
            c.sliderPref("blockbarminhealth", 0, 0, 4000, 50, i -> i + "[red]HP");

            c.addCategory("entitySettings");
            c.checkPref("bulletShow", true);
            c.checkPref("showMineBeam".toLowerCase(), true);
        });
    }
}
