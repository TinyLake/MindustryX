package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
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
            c.checkPref("githubMirror", true);
            c.checkPref("replayRecord", false);

            c.addCategory("gameUI");
            c.checkPref("menuFloatText", true);
            c.checkPref("deadOverlay", false);
            c.checkPref("invertMapClick", false);
            c.checkPref("arcSpecificTable", true);
            c.checkPref("logicSupport", true);
            c.checkPref("showOtherTeamResource", false);
            c.checkPref("showQuickToolTable", true);
            c.sliderPref("itemSelectionHeight", 4, 4, 12, i -> i + "行");
            c.sliderPref("itemSelectionWidth", 4, 4, 12, i -> i + "列");
            c.checkPref("researchViewer", false);
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
            c.checkPref("payloadpreview", true);
            c.checkPref("unithitbox", false);
            c.checkPref("unitHideExcludePlayers", true);
            c.sliderPref("unitDrawMinHealth", 0, 0, 2500, 50, i -> i + "[red]HP");
            c.checkPref("damagePopup", false);
            c.checkPref("healPopup", true);
            c.checkPref("playerPopupOnly", true);
            c.sliderPref("popupMinHealth", 600, 0, 4000, 50, i -> i + "[red]HP");

            c.addCategory("developerMode");
            c.checkPref("renderSort", false);
            c.checkPref("reliableSync", false);
            c.checkPref("renderDebug", false);
            c.checkPref("limitupdate", false, v -> {
                if(!v) return;
                settings.put("limitupdate", false);
                ui.showConfirm("确认开启限制更新", "此功能可以大幅减少LG开销，但会导致视角外的一切停止更新\n强烈不建议在单人开启，在服务器里会造成不同步", () -> settings.put("limitupdate", true));
            });
            c.sliderPref("limitdst", 10, 0, 100, 1, s -> s + "格");
        }));
        categories.add(new LazySettingsCategory("@settings.category.settingV2", () -> Icon.box, (c) -> {
        }){
            @SuppressWarnings({"deprecation"})
            @Override
            public void init(){
                super.init();
                table = new SettingsTable(){
                    @Override
                    public Table build(){
                        SettingsV2.buildSettingsTable(this);
                        add().width(500).row();
                        return this;
                    }
                };
            }
        });
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
