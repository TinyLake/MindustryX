package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.struct.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.dialogs.SettingsMenuDialog.*;

import static arc.Core.settings;
import static mindustry.Vars.*;

public class Settings{
    public static class LazySettingsCategory extends SettingsCategory{
        private final Prov<Drawable> iconProv;

        public LazySettingsCategory(String name, Prov<Drawable> icon, Cons<SettingsTable> builder){
            super(name, null, builder);
            iconProv = icon;
        }

        public void init(){
            icon = iconProv.get();
        }
    }

    public static final Seq<LazySettingsCategory> categories = new Seq<>();

    public static void addSettings(){
        categories.add(new LazySettingsCategory("@settings.category.mindustryX", () -> Icon.box, (c) -> {
            c.checkPref("showUpdateDialog", true);
            c.checkPref("githubMirror", true);
            c.checkPref("replayRecord", false);

            c.addCategory("gameUI");
            c.checkPref("menuFloatText", true);
            c.checkPref("deadOverlay", false);
            c.checkPref("invertMapClick", false);
            c.checkPref("arcSpecificTable", true);
            c.checkPref("logicSupport", true);
            c.checkPref("powerStatistic", true);
            c.checkPref("showOtherTeamResource", false);
            c.checkPref("showQuickToolTable", true);
            c.sliderPref("arccoreitems", 3, 0, 3, 1, s -> switch(s){
                case 0 -> "不显示";
                case 1 -> "资源状态";
                case 2 -> "兵种状态";
                default -> "显示资源和兵种";
            });
            c.sliderPref("arcCoreItemsCol", 5, 4, 15, 1, i -> i + "列");
            c.sliderPref("itemSelectionHeight", 4, 4, 12, i -> i + "行");
            c.sliderPref("itemSelectionWidth", 4, 4, 12, i -> i + "列");
            c.sliderPref("blockInventoryWidth", 3, 3, 16, i -> i + "");
            c.sliderPref("editorBrush", 6, 3, 12, i -> i + "");
            c.checkPref("researchViewer", false);
            c.sliderPref("minimapSize", 140, 40, 400, 10, i -> i + "");
            c.sliderPref("maxSchematicSize", 64, 64, 257, 1, v -> {
                maxSchematicSize = v == 257 ? Integer.MAX_VALUE : v;
                return v == 257 ? "无限" : String.valueOf(v);
            });
            {
                var v = Core.settings.getInt("maxSchematicSize");
                maxSchematicSize = v == 257 ? Integer.MAX_VALUE : v;
            }
            c.checkPref("colorizedContent", false);
            c.textPref("arcBackgroundPath", "");
            c.checkPref("autoSelSchematic", false);
            c.checkPref("arcCommandTable", true);

            c.addCategory("blockSettings");
            c.checkPref("rotateCanvas", false);
            c.checkPref("staticShieldsBorder", false);
            c.checkPref("arcTurretPlaceCheck", false);
            c.checkPref("arcchoiceuiIcon", false);
            c.sliderPref("HiddleItemTransparency", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.sliderPref("overdrive_zone", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.checkPref("arcPlacementEffect", false);
            c.sliderPref("blockbarminhealth", 0, 0, 4000, 50, i -> i + "[red]HP");
            c.sliderPref("blockRenderLevel", 2, 0, 2, 1, s -> switch(s){
                case 0 -> "隐藏全部建筑";
                case 1 -> "只显示建筑状态";
                default -> "全部显示";
            });
            c.checkPref("showOtherTeamState", false);
            c.checkPref("editOtherBlock", false);
            c.checkPref("logicDisplayNoBorder", false);

            c.addCategory("entitySettings");
            c.checkPref("bulletShow", true);
            c.checkPref("showMineBeam".toLowerCase(), true);
            c.checkPref("noPlayerHitBox", false);
            c.checkPref("payloadpreview", true);
            c.checkPref("unithitbox", false);

            c.addCategory("developerMode");
            c.checkPref("renderSort", false);
            c.checkPref("reliableSync", false);
            c.checkPref("renderDebug", false);
            c.checkPref("limitupdate", false, v -> {
                if(!v) return;
                settings.put("limitupdate", false);
                ui.showConfirm("确认开启限制更新", "此功能可以大幅减少LG开销，但会导致视角外的一切停止更新\n强烈不建议在单人开启，在服务器里会造成不同步", () -> {
                    settings.put("limitupdate", true);
                });
            });
            c.sliderPref("limitdst", 10, 0, 100, 1, s -> s + "格");
        }));
        ArcOld.init(categories);
        Events.on(ClientLoadEvent.class, e -> {
            categories.each(LazySettingsCategory::init);
            Vars.ui.settings.getCategories().addAll(categories);
        });
    }

    public static void toggle(String name){
        Core.settings.put(name, !Core.settings.getBool(name));
    }

    public static void cycle(String name, int max){
        Core.settings.put(name, (Core.settings.getInt(name) + 1) % max);
    }
}
