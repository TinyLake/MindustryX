package mindustryX.features.ui.toolTable;

import arc.*;
import arc.graphics.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustryX.features.Settings;
import mindustryX.features.*;
import mindustryX.features.ui.*;

import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.ui.Styles.flatTogglet;

//moved from mindustry.arcModule.ui.quickTool.HudSettingsTable
public class HudSettingsTable extends ToolTableBase{
    public HudSettingsTable(){
        super(String.valueOf(Iconc.settings));
        maxHeight = 240f;
        rebuild();
        Events.on(EventType.WorldLoadEvent.class, e -> Core.settings.put("removeLogicLock", false));
    }

    protected void rebuild(){
        table(t -> {
            t.defaults().size(30);
            t.button("[cyan]信", Styles.flatBordert, () -> UIExt.arcMessageDialog.show()).tooltip("中央监控室");
            t.button("[cyan]S", Styles.flatBordert, () -> Call.sendChatMessage("/sync")).tooltip("同步一波");
            t.button("[cyan]观", Styles.flatBordert, () -> Call.sendChatMessage("/ob")).tooltip("观察者模式");
            t.button("[cyan]技", Styles.flatBordert, () -> Call.sendChatMessage("/skill")).tooltip("技能！");
            t.button("[cyan]版", Styles.flatBordert, () -> Call.sendChatMessage("/broad")).tooltip("服务器信息版");
            t.button("[cyan]雾", Styles.flatTogglet, () -> state.rules.fog ^= true).checked(a -> state.rules.fog).tooltip("战争迷雾").disabled((_t) -> state.rules.pvp && player.team().id != 255);
            t.button("[red]版", Styles.flatTogglet, () -> Settings.toggle("ShowInfoPopup")).checked(a -> !Core.settings.getBool("ShowInfoPopup")).tooltip("关闭所有信息版");
            t.button("[white]法", Styles.flatBordert, () -> ui.showConfirm("受不了，直接投降？", () -> Call.sendChatMessage("/vote gameover"))).tooltip("法国军礼");
            t.row();
            t.button("[cyan]块", Styles.flatTogglet, () -> Settings.cycle("blockRenderLevel", 3))
            .checked((a) -> RenderExt.blockRenderLevel > 0).tooltip("建筑显示");
            t.button("[cyan]兵", Styles.flatTogglet, () -> RenderExt.unitHide = !RenderExt.unitHide)
            .checked(a -> !RenderExt.unitHide).tooltip("兵种显示");
            t.button("[cyan]弹", Styles.flatTogglet, () -> Settings.toggle("bulletShow"))
            .checked(a -> Core.settings.getBool("bulletShow")).tooltip("子弹显示");
            t.button("[cyan]灯", Styles.flatTogglet, () -> Settings.toggle("drawlight"))
            .checked(a -> renderer.drawLight).name("灯光").tooltip("[cyan]开灯啊！");
            t.button("[cyan]效", Styles.flatTogglet, () -> Settings.toggle("effects"))
            .checked(a -> Core.settings.getBool("effects")).tooltip("特效显示");
            t.button("[cyan]光", Styles.flatTogglet, () -> {
                Settings.toggle("bloom");
                renderer.toggleBloom(settings.getBool("bloom"));
            }).checked(a -> Core.settings.getBool("bloom")).tooltip("光效显示");
            t.button("[cyan]墙", Styles.flatTogglet, () -> enableDarkness ^= true)
            .checked(a -> enableDarkness).tooltip("墙体阴影显示");
            t.button("[cyan]天", Styles.flatTogglet, () -> Settings.toggle("showweather"))
            .checked(a -> Core.settings.getBool("showweather")).tooltip("天气显示");
            t.button("[cyan]" + Iconc.map, Styles.flatTogglet, () -> Settings.toggle("minimap"))
            .checked(a -> Core.settings.getBool("minimap")).tooltip("小地图显示");
            t.row();
            t.button("箱", Styles.flatTogglet, () -> Settings.toggle("unithitbox"))
            .checked(a -> Core.settings.getBool("unithitbox")).tooltip("碰撞箱显示");
            t.button("扫", Styles.flatTogglet, () -> ArcScanMode.enabled = !ArcScanMode.enabled).checked(a -> ArcScanMode.enabled).tooltip("扫描模式");
            var b = t.button("" + Iconc.blockRadar, Styles.flatBordert, () -> ArcRadar.mobileRadar = !ArcRadar.mobileRadar).tooltip("雷达开关").get();
            SettingsV2.bindQuickSettings(b, ArcRadar.settings);
            t.button("" + Iconc.blockWorldProcessor, Styles.flatTogglet, () -> {
                Settings.toggle("removeLogicLock");
                control.input.logicCutscene = false;
                ui.announce("已移除逻辑视角锁定");
            }).checked(a -> Core.settings.getBool("removeLogicLock")).tooltip("逻辑锁定");
            t.button(Blocks.worldMessage.emoji(), flatTogglet, () -> Settings.toggle("displayallmessage")).checked(a -> RenderExt.displayAllMessage).tooltip("开关信息板全显示");
            t.button("" + Iconc.itemCopper, Styles.flatBordert, this::floorStatisticDialog).tooltip("矿物信息");
            t.button(Icon.fillSmall, Styles.flati, () -> EffectsDialog.withAllEffects().show()).tooltip("特效大全");
            if(!mobile) t.button(Icon.starSmall, Styles.flati, this::uiTable).tooltip("ui大全");
            if(settings.getInt("arcQuickMsg", 0) == 0)
                t.button("\uE87C", Styles.flatBordert, this::arcQuickMsgTable).tooltip("快捷消息");
        }).left().row();

        if(settings.getInt("arcQuickMsg") > 0){
            table(t -> {
                t.defaults().size(30);
                for(int i = 0; i < settings.getInt("arcQuickMsg"); i++){
                    if(i % settings.getInt("arcQuickMsgKey", 8) == 0) t.row();
                    int finalI = i;
                    t.button(settings.getString(getArcQuickMsgShortName(i)), Styles.flatBordert, () -> {
                        if(settings.getBool(getArcQuickMsgJs(finalI))) mods.getScripts().runConsole(settings.getString(getArcQuickMsgName(finalI)));
                        else Call.sendChatMessage(settings.getString(getArcQuickMsgName(finalI)));
                    });
                }
                t.button("\uE87C", Styles.flatBordert, this::arcQuickMsgTable).tooltip("快捷消息");
            }).left().row();
        }

        sliderPref("turretShowRange", 0, 3, 1, s -> switch(s){
            case 0 -> "关闭";
            case 1 -> "仅对地";
            case 2 -> "仅对空";
            case 3 -> "全部";
            default -> s + "";
        });
        checkPref("unitHealthBar");
        sliderPref("unitTransparency", 0, 100, 5, i -> i > 0 ? i + "%" : "关闭");
        sliderPref("unitDrawMinHealth", 0, 2500, 50, i -> i + "[red]HP");
        sliderPref("unitBarDrawMinHealth", 0, 2500, 100, i -> i + "[red]HP");
        sliderPref("unitWeaponRange", 0, 100, 1, i -> i > 0 ? i + "%" : "关闭");

        checkPref("alwaysShowUnitRTSAi");
        checkPref("unitLogicMoveLine");
        checkPref("unitWeaponTargetLine");

        checkPref("blockWeaponTargetLine");
        checkPref("unitbuildplan");
        sliderPref("minimapSize", 40, 400, 10, i -> i + "");
        UIExt.quickToolOffset.buildUI(this);
    }

    private void arcQuickMsgTable(){
        BaseDialog dialog = new BaseDialog("快捷信息");
        dialog.hidden(() -> {
            clear();
            rebuild();
        });
        dialog.cont.table(t -> {
            t.add("""
            在此编辑快速消息，可在快捷设置面板显示。如设置：
            [white]法 /vote gameover
            这一指令会添加一个“[white]法的按钮，点击会自动输入/vote gameover。""").center().fillX().row();
            t.table(tt -> {
                tt.add("快捷消息个数： ");
                Label label = tt.add(String.valueOf(settings.getInt("arcQuickMsg", 0))).get();
                tt.slider(0, 50, 1, settings.getInt("arcQuickMsg", 0), i -> {
                    settings.put("arcQuickMsg", (int)i);
                    label.setText(String.valueOf(settings.getInt("arcQuickMsg")));
                }).width(200f).row();
                tt.add("每行多少个按键： ");
                Label label2 = tt.add(String.valueOf(settings.getInt("arcQuickMsgKey", 0))).get();
                tt.slider(3, 10, 1, settings.getInt("arcQuickMsgKey", 0), i -> {
                    settings.put("arcQuickMsgKey", (int)i);
                    label2.setText(String.valueOf(settings.getInt("arcQuickMsgKey")));
                }).width(200f);
            }).row();
            t.pane(tt -> {
                tt.add("第i个").width(50f);
                tt.add("JS").width(50f);
                tt.add("按钮显示\n(建议单个字符)").width(100f);
                tt.add("              输入信息").width(400f).center().row();

                for(int i = 0; i < settings.getInt("arcQuickMsg", 0); i++){
                    tt.add(i + "  ");
                    int finalI = i;
                    tt.check("", settings.getBool(getArcQuickMsgJs(finalI)), js -> settings.put(getArcQuickMsgJs(finalI), js));
                    tt.field(settings.getString(getArcQuickMsgShortName(finalI), "?"), text -> settings.put(getArcQuickMsgShortName(finalI), text)).maxTextLength(10);
                    tt.field(settings.getString(getArcQuickMsgName(finalI), "未输入指令"), text -> settings.put(getArcQuickMsgName(finalI), text)).maxTextLength(300).width(350f);
                    tt.row();
                }
            });
        });
        dialog.addCloseButton();
        dialog.show();
    }

    private String getArcQuickMsgShortName(int i){
        return "arcQuickMsgShort" + i;
    }

    private String getArcQuickMsgName(int i){
        return "arcQuickMsg" + i;
    }

    private String getArcQuickMsgJs(int i){
        return "arcQuickMsgJs" + i;
    }


    private void floorStatisticDialog(){
        BaseDialog dialog = new BaseDialog("ARC-矿物统计");
        Table table = dialog.cont;
        table.clear();

        table.table(c -> {
            c.add("地表矿").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(list -> {
                int i = 0;
                for(Block block : content.blocks().select(b -> b instanceof Floor f && !f.wallOre && f.itemDrop != null)){
                    if(indexer.floorOresCount[block.id] == 0) continue;
                    if(i++ % 4 == 0) list.row();
                    list.add(block.emoji() + " " + block.localizedName + "\n" + indexer.floorOresCount[block.id]).width(100f).height(50f);
                }
            }).row();

            c.add("墙矿").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(list -> {
                int i = 0;
                for(Block block : content.blocks().select(b -> ((b instanceof Floor f && f.wallOre) || b instanceof StaticWall) && b.itemDrop != null)){
                    if(indexer.wallOresCount[block.id] == 0) continue;
                    if(i++ % 4 == 0) list.row();
                    list.add(block.emoji() + " " + block.localizedName + "\n" + indexer.wallOresCount[block.id]).width(100f).height(50f);
                }
            }).row();

            c.add("液体").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(list -> {
                int i = 0;
                for(Block block : content.blocks().select(b -> ((b instanceof Floor f && f.liquidDrop != null)))){
                    if(indexer.floorOresCount[block.id] == 0) continue;
                    if(i++ % 4 == 0) list.row();
                    list.add(block.emoji() + " " + block.localizedName + "\n" + indexer.floorOresCount[block.id]).width(100f).height(50f);
                }
            }).row();
        });
        dialog.addCloseButton();
        dialog.show();
    }

    private void uiTable(){
        BaseDialog dialog = new BaseDialog("ARC-ui大全");
        TextField sField = dialog.cont.field("", text -> {
        }).fillX().get();
        dialog.cont.row();

        dialog.cont.pane(c -> {
            c.add("颜色").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(ct -> {
                int i = 0;
                for(var colorEntry : Colors.getColors()){
                    Color value = colorEntry.value;
                    String key = colorEntry.key;
                    ct.button("[#" + value + "]" + key, Styles.cleart, () -> {
                        Core.app.setClipboardText("[#" + value + "]");
                        sField.appendText("[#" + value + "]");
                    }).size(50f).tooltip(key);
                    i += 1;
                    if(i % 15 == 0) ct.row();
                }
            }).row();
            c.add("物品").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(ct -> {
                int i = 0;
                for(var it : Fonts.stringIcons){
                    final String icon = it.value;
                    ct.button(icon, Styles.cleart, () -> {
                        Core.app.setClipboardText(icon);
                        sField.appendText(icon);
                    }).size(50f).tooltip(it.key);
                    i += 1;
                    if(i % 15 == 0) ct.row();
                }
            }).row();
            c.add("图标").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(ct -> {
                int i = 0;
                for(var it : Iconc.codes){
                    String icon = String.valueOf((char)it.value), internal = it.key;
                    ct.button(icon, Styles.cleart, () -> {
                        Core.app.setClipboardText(icon);
                        sField.appendText(icon);
                    }).size(50f).tooltip(internal);
                    i += 1;
                    if(i % 15 == 0) ct.row();
                }
            }).row();
        }).row();

        dialog.addCloseButton();
        dialog.show();
    }

    public interface StringProcessor{
        String get(int i);
    }

    public void sliderPref(String name, int min, int max, int step, StringProcessor s){
        Slider slider = new Slider(min, max, step, false);
        Label value = new Label("", Styles.outlineLabel);
        slider.update(() -> {
            slider.setValue(settings.getInt(name));
            value.setText(s.get((int)slider.getValue()));
        });
        slider.changed(() -> settings.put(name, (int)slider.getValue()));

        Table content = new Table();
        content.add(bundle.get("setting." + name + ".name"), Styles.outlineLabel).left().growX().wrap();
        content.add(value).padLeft(10f).right();
        content.margin(3f, 33f, 3f, 33f);
        content.touchable = Touchable.disabled;

        stack(slider, content).width(270f).left().padTop(4f).row();

        if(settings.getDefault(name) == null)
            Log.warn("no default value for " + name);
    }

    public void checkPref(String name){
        CheckBox box = new CheckBox(bundle.get("setting." + name + ".name"));
        box.update(() -> box.setChecked(settings.getBool(name)));
        box.changed(() -> settings.put(name, box.isChecked()));

        box.left();
        add(box).left().padTop(0.5f).row();

        if(settings.getDefault(name) == null)
            Log.warn("no default value for " + name);
    }
}
