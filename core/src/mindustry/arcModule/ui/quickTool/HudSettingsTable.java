package mindustry.arcModule.ui.quickTool;

import arc.*;
import arc.func.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.arcModule.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.arcModule.ElementUtils.*;

public class HudSettingsTable extends ElementUtils.ToolTable{
    protected Seq<Setting> list = new Seq<>();

    public HudSettingsTable(){
        icon = String.valueOf(Iconc.settings);
        rebuild();
        Events.on(EventType.WorldLoadEvent.class, e -> Core.settings.put("removeLogicLock", false));
    }

    @Override
    protected void buildTable(){
        list.clear();
        Table sets = new Table();
        sliderPref("turretShowRange", 0, 0, 3, 1, s -> {
            if(s == 0){
                return "关闭";
            }else if(s == 1){
                return "仅对地";
            }else if(s == 2){
                return "仅对空";
            }else if(s == 3){
                return "全部";
            }else{
                return s + "";
            }
        });
        sliderPref("chatValidType", 0, 0, 3, 1, s -> {
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
        checkPref("unitHealthBar", false);
        sliderPref("unitTransparency", 100, 0, 100, 5, i -> i > 0 ? i + "%" : "关闭");
        sliderPref("unitDrawMinHealth", 0, 0, 2500, 50, i -> i + "[red]HP");
        sliderPref("unitBarDrawMinHealth", 0, 0, 2500, 100, i -> i + "[red]HP");
        sliderPref("unitWeaponRange", 0, 0, 100, 1, i -> i > 0 ? i + "%" : "关闭");

        checkPref("alwaysShowUnitRTSAi", false);
        checkPref("unitLogicMoveLine", true);
        checkPref("unitWeaponTargetLine", true);

        checkPref("blockWeaponTargetLine", true);
        checkPref("unitbuildplan", false);


        for(Setting setting : list){
            setting.add(sets);
        }

        ScrollPane pane = pane(sp -> {
            sp.background(Styles.black6);
            sp.table(t -> {
                t.button("[cyan]S", NCtextStyle, () -> Call.sendChatMessage("/sync")).size(30).tooltip("同步一波");
                t.button("[cyan]观", NCtextStyle, () -> Call.sendChatMessage("/ob")).size(30).tooltip("观察者模式");
                t.button("[cyan]技", NCtextStyle, () -> Call.sendChatMessage("/skill")).size(30).tooltip("技能！");
                t.button("[cyan]版", NCtextStyle, () -> Call.sendChatMessage("/broad")).size(30).tooltip("服务器信息版");
                t.button("[red]版", textStyle, () -> settings.put("ShowInfoPopup", !Core.settings.getBool("ShowInfoPopup"))).checked(a -> Core.settings.getBool("ShowInfoPopup")).size(30, 30).tooltip("关闭所有信息版");
                t.button("[white]法", NCtextStyle, () -> ui.showConfirm("受不了，直接投降？", () -> Call.sendChatMessage("/vote gameover"))).size(30, 30).tooltip("法国军礼");
                if(settings.getInt("arcQuickMsg", 0) == 0)
                    t.button("\uE87C", NCtextStyle, this::arcQuickMsgTable).size(30, 30).tooltip("快捷消息");
            }).left();
            sp.row();
            if(settings.getInt("arcQuickMsg") > 0){
                sp.table(t -> {
                    for(int i = 0; i < settings.getInt("arcQuickMsg"); i++){
                        if(i % settings.getInt("arcQuickMsgKey", 8) == 0) t.row();
                        int finalI = i;
                        t.button(settings.getString(getArcQuickMsgShortName(i)), NCtextStyle, () -> {
                            if(settings.getBool(getArcQuickMsgJs(finalI))) mods.getScripts().runConsole(settings.getString(getArcQuickMsgName(finalI)));
                            else Call.sendChatMessage(settings.getString(getArcQuickMsgName(finalI)));
                        }
                        ).size(30);
                    }
                    t.button("\uE87C", NCtextStyle, this::arcQuickMsgTable).size(30, 30).tooltip("快捷消息");
                }).left();
                sp.row();
            }
            sp.table(t -> {
                t.button("[cyan]块", textStyle, () -> {
                    int blockRenderLevel = Core.settings.getInt("blockRenderLevel");
                    Core.settings.put("blockRenderLevel", (blockRenderLevel + 1) % 3);
                }).size(30, 30).tooltip("建筑显示");
                t.button("[cyan]兵", textStyle, () -> ARCVars.unitHide = !ARCVars.unitHide).checked(a -> !ARCVars.unitHide).size(30, 30).tooltip("兵种显示");
                t.button("[cyan]箱", textStyle, () -> Core.settings.put("unithitbox", !Core.settings.getBool("unithitbox"))).checked(a -> Core.settings.getBool("unithitbox")).size(30, 30).tooltip("碰撞箱显示");
                t.button("[cyan]弹", textStyle, () -> Core.settings.put("bulletShow", !Core.settings.getBool("bulletShow"))).checked(a -> Core.settings.getBool("bulletShow")).size(30, 30).tooltip("子弹显示");
                t.button("[violet]锁", textStyle, () -> {
                    Core.settings.put("removeLogicLock", !Core.settings.getBool("removeLogicLock"));
                    control.input.logicCutscene = false;
                    arcui.arcInfo("已移除逻辑视角锁定");
                }).checked(a -> Core.settings.getBool("removeLogicLock")).size(30, 30).tooltip("逻辑锁定");
                t.button("[violet]丢", textStyle, () -> {
                    boolean block = Core.settings.getBool("blockDrop");
                    Core.settings.put("blockDrop", !block);
                    arcui.arcInfo("已" + (block ? "开启" : "关闭") + "丢弃物品");
                }).checked(a -> !Core.settings.getBool("blockDrop")).size(30, 30).tooltip("允许丢弃物品");
                t.button("[cyan]雾", textStyle, () -> {
                    if(!state.rules.pvp || player.team().id == 255) renderer.fogEnabled = !renderer.fogEnabled;
                }).checked(a -> renderer.fogEnabled).size(30, 30).tooltip("战争迷雾").visible(() -> !state.rules.pvp || player.team().id == 255);
            }).left();
            sp.row();
            sp.table(t -> {
                t.button("[red]灯", textStyle, () -> settings.put("drawlight", !settings.getBool("drawlight"))).checked(a -> state.rules.lighting).size(30, 30).name("灯光").tooltip("[cyan]开灯啊！");
                t.button("[acid]效", textStyle, () -> Core.settings.put("effects", !Core.settings.getBool("effects"))).checked(a -> Core.settings.getBool("effects")).size(30, 30).tooltip("特效显示");
                t.button("[acid]光", textStyle, () -> {
                    Core.settings.put("bloom", !Core.settings.getBool("bloom"));
                    renderer.toggleBloom(settings.getBool("bloom"));
                }).checked(a -> Core.settings.getBool("bloom")).size(30, 30).tooltip("光效显示");
                t.button("[acid]墙", textStyle, () -> Core.settings.put("forceEnableDarkness", !Core.settings.getBool("forceEnableDarkness"))).checked(a -> Core.settings.getBool("forceEnableDarkness")).size(30, 30).tooltip("墙体阴影显示");
                t.button("[acid]天", textStyle, () -> Core.settings.put("showweather", !Core.settings.getBool("showweather"))).checked(a -> Core.settings.getBool("showweather")).size(30, 30).tooltip("天气显示");
                if(settings.getBool("developMode"))
                    t.button(StatusEffects.burning.emoji(), textStyle, () -> state.rules.fire = !state.rules.fire).checked(a -> state.rules.fire).size(30, 30).tooltip("太燃了");
                t.button("[cyan]扫", textStyle, () -> control.input.arcScanMode = !control.input.arcScanMode).checked(a -> control.input.arcScanMode).size(30, 30).tooltip("扫描模式");

            }).left();
            sp.row();
            sp.add(sets);
        }).maxSize(800f, 300f).get();

        pane.update(() -> {
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(pane)){
                pane.requestScroll();
            }else if(pane.hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
    }

    private void arcQuickMsgTable(){
        BaseDialog dialog = new BaseDialog("快捷信息");
        dialog.cont.table(t -> {
            t.add("在此编辑快速消息，可在快捷设置面板显示。如设置：\n[white]法 /vote gameover\n" +
            "这一指令会添加一个“[white]法的按钮，点击会自动输入/vote gameover。\n" +
            "由于懒得写更新，请修改滑块后[orange]关闭此窗口后再打开一次[white]\n" +
            "快捷设置面板同样需要[orange]关闭后再打开一次[white]才能生效").center().fillX().row();
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

    public interface StringProcessor{
        String get(int i);
    }

    public Seq<Setting> getSettings(){
        return list;
    }

    public void pref(Setting setting){
        list.add(setting);
        rebuild();
    }

    public SliderSettingF sliderPref(String name, String title, int def, int min, int max, StringProcessor s){
        return sliderPref(name, title, def, min, max, 1, s);
    }

    public SliderSettingF sliderPref(String name, String title, int def, int min, int max, int step, StringProcessor s){
        SliderSettingF res;
        list.add(res = new SliderSettingF(name, title, def, min, max, step, s));
        settings.defaults(name, def);
        return res;
    }

    public SliderSettingF sliderPref(String name, int def, int min, int max, StringProcessor s){
        return sliderPref(name, def, min, max, 1, s);
    }

    public SliderSettingF sliderPref(String name, int def, int min, int max, int step, StringProcessor s){
        SliderSettingF res;
        list.add(res = new SliderSettingF(name, bundle.get("setting." + name + ".name"), def, min, max, step, s));
        settings.defaults(name, def);
        return res;
    }

    public void checkPref(String name, String title, boolean def){
        list.add(new CheckSettingF(name, title, def, null));
        settings.defaults(name, def);
    }

    public void checkPref(String name, String title, boolean def, Boolc changed){
        list.add(new CheckSettingF(name, title, def, changed));
        settings.defaults(name, def);
    }

    /**
     * Localized title.
     */
    public void checkPref(String name, boolean def){
        list.add(new CheckSettingF(name, bundle.get("setting." + name + ".name"), def, null));
        settings.defaults(name, def);
    }

    /**
     * Localized title.
     */
    public void checkPref(String name, boolean def, Boolc changed){
        list.add(new CheckSettingF(name, bundle.get("setting." + name + ".name"), def, changed));
        settings.defaults(name, def);
    }

    public abstract static class Setting{
        public String name;
        public String title;

        public abstract void add(Table table);
    }

    public static class CheckSettingF extends Setting{
        boolean def;
        Boolc changed;

        CheckSettingF(String name, String title, boolean def, Boolc changed){
            this.name = name;
            this.title = title;
            this.def = def;
            this.changed = changed;
        }

        @Override
        public void add(Table table){
            CheckBox box = new CheckBox(title);
            box.update(() -> box.setChecked(settings.getBool(name)));

            box.changed(() -> {
                settings.put(name, box.isChecked());
                if(changed != null){
                    changed.get(box.isChecked());
                }
            });

            box.left();
            table.add(box).left().padTop(0.5f);
            table.row();
        }
    }

    public static class SliderSettingF extends Setting{
        int def;
        int min;
        int max;
        int step;
        StringProcessor sp;
        float[] values = null;

        SliderSettingF(String name, String title, int def, int min, int max, int step, StringProcessor s){
            this.name = name;
            this.title = title;
            this.def = def;
            this.min = min;
            this.max = max;
            this.step = step;
            this.sp = s;
        }

        @Override
        public void add(Table table){
            Slider slider = new Slider(min, max, step, false);

            slider.setValue(settings.getInt(name));

            Label value = new Label("", Styles.outlineLabel);
            Table content = new Table();
            content.add(title, Styles.outlineLabel).left().growX().wrap();
            content.add(value).padLeft(10f).right();
            content.margin(3f, 33f, 3f, 33f);
            content.touchable = Touchable.disabled;

            slider.changed(() -> {
                settings.put(name, (int)slider.getValue());
                value.setText(sp.get((int)slider.getValue()));
            });

            slider.change();

            table.stack(slider, content).width(Math.min(Core.graphics.getWidth() / 1.2f, 300f)).left().padTop(4f).get();
            table.row();
        }
    }

}
