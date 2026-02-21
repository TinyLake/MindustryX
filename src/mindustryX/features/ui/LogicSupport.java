package mindustryX.features.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.LogicBlock.*;
import mindustry.world.blocks.logic.MemoryBlock.*;
import mindustryX.*;
import mindustryX.features.SettingsV2.*;
import mindustryX.features.*;

import static mindustry.Vars.*;
import static mindustryX.features.UIExt.i;

public class LogicSupport{
    public static final CheckPref visible = new CheckPref("logicSupport.visible", true);
    public static final CheckPref changeSplash = new CheckPref("logicSupport.changeSplash", true);
    public static final SliderPref memoryColumns = new SliderPref("logicSupport.memoryColumns", 10, 2, 15);
    public static final SliderPref memoryDecimal = new SliderPref("logicSupport.memoryDecimal", 0, 0, 8);

    private static float refreshTime = 15f;
    private static final Table varsTable = new Table();

    private static boolean refresh;
    private static boolean autoRefresh = true;

    private static @Nullable Runnable refreshExecutor;
    private static @Nullable LExecutor executor;

    static{
        visible.addFallbackName("gameUI.logicSupport");
        visible.addFallbackName("logicSupport");
        changeSplash.addFallbackName("logicSupportChangeSplash");
    }

    public static void init(){
        LogicDialog logic = ui.logic;
        if(Core.graphics.isPortrait()) visible.set(false);//default hide in portrait mode

        Table main = new Table(Styles.grayPanel);
        main.margin(4f);

        main.add(i("逻辑辅助器[gold]X[]")).style(Styles.outlineLabel).pad(8f).padBottom(12f).row();
        main.fill(tt -> tt.top().right().button(Icon.cancel, Styles.clearNonei, iconMed, visible::toggle).tooltip(i("隐藏逻辑辅助器")));

        main.table(LogicSupport::buildConfigTable).fillX().row();
        main.pane(Styles.noBarPane, varsTable).growY().fillX().scrollX(false).width(400f).padTop(8f);

        Interval interval = new Interval();
        main.update(() -> refresh = autoRefresh && interval.get(refreshTime));


        logic.fill(t -> {
            t.name = "logicSupportX-toggle";
            t.visible(() -> !visible.get());
            t.left().button(Icon.rightOpen, Styles.clearNonei, iconMed, visible::toggle).height(150f);
        });

        logic.fill(t -> {
            t.name = "logicSupportX";
            t.visibility = visible::get;
            t.left().add(main).pad(4f);
        });
    }

    public static void build(LCanvas canvas, LExecutor executor, Cons<String> consumer){
        LogicSupport.executor = executor;

        rebuildVarsTable();

        refreshExecutor = () -> {
            consumer.get(canvas.save());
            rebuildVarsTable();
        };
    }

    private static void buildConfigTable(Table table){
        table.defaults().size(iconLarge);
        table.button(Icon.downloadSmall, Styles.cleari, () -> {
            if(refreshExecutor != null){
                refreshExecutor.run();
                UIExt.announce(i("[orange]已更新编辑的逻辑！"));
            }
        }).tooltip(i("更新编辑的逻辑")).disabled(b -> refreshExecutor == null);
        table.button(Icon.eyeSmall, Styles.clearTogglei, () -> {
            changeSplash.toggle();
            String state = changeSplash.get() ? i("开启") : i("关闭");
            String text = VarsX.bundle.toggleState(i("变动闪烁"), state);
            UIExt.announce(text);
        }).checked((b) -> changeSplash.get()).tooltip(i("变量变动闪烁"));
        table.button(Icon.refreshSmall, Styles.clearTogglei, () -> {
            autoRefresh = !autoRefresh;
            String state = autoRefresh ? i("开启") : i("关闭");
            String text = VarsX.bundle.toggleState(i("变量自动更新"), state);
            UIExt.announce(text);
        }).checked((b) -> autoRefresh).tooltip(i("自动刷新变量"));
        table.button(Icon.pause, Styles.clearTogglei, () -> {
            if(state.isPaused()) state.set(State.playing);
            else state.set(State.paused);
            String text = state.isPaused() ? i("已暂停") : i("已继续游戏");
            UIExt.announce(text);
        }).checked((b) -> state.isPaused()).tooltip(i("暂停逻辑(游戏)运行"));

        table.defaults().reset();
        var slider = new Slider(1, 60, 1, false);
        slider.setValue(refreshTime);
        slider.moved((res) -> refreshTime = res);
        var label = new Label(() -> VarsX.bundle.refreshInterval((int)refreshTime));
        label.touchable = Touchable.disabled;
        table.stack(slider, label).padLeft(8f).growX();
    }

    private static void rebuildVarsTable(){
        varsTable.top().clearChildren();
        if(executor == null) return;
        varsTable.defaults().padTop(10f).growX();

        for(var v : executor.allVars){
            if(v.name.startsWith("___")) continue;
            varsTable.table(Tex.paneSolid, table -> {
                Label nameLabel = createCopyableLabel(v.name, null, VarsX.bundle::copiedVariableNameHint);
                Label valueLabel = createCopyableLabel(arcVarsText(v), null, VarsX.bundle::copiedVariableAttributesHint);

                table.add(nameLabel).color(arcVarsColor(v)).ellipsis(true).wrap().expand(3, 1).fill().get();
                table.add(valueLabel).ellipsis(true).wrap().padLeft(16f).expand(2, 1).fill().get();

                final float[] heat = {1};
                valueLabel.update(() -> {
                    if(refresh){
                        String text = arcVarsText(v);
                        if(!valueLabel.textEquals(text)){
                            heat[0] = 1;
                            nameLabel.setColor(arcVarsColor(v));
                            valueLabel.setText(text);
                        }
                    }

                    table.color.set(splashColor(heat));
                });
            }).row();
        }

        varsTable.table(Tex.paneSolid, table -> {
            Label label = createCopyableLabel("", table, VarsX.bundle::copiedPrintBufferHint);

            table.add("@printbuffer").color(Color.goldenrod).center().row();
            table.add(label).labelAlign(Align.topLeft).wrap().minHeight(150).growX();

            final float[] heat = {1};
            label.update(() -> {
                if(refresh){
                    StringBuilder text = executor.textBuffer;
                    if(!label.textEquals(text)){
                        label.setText(text);
                        heat[0] = 1;
                    }
                }

                table.color.set(splashColor(heat));
            });
        }).fillX().row();
    }

    private static Color splashColor(float[] heat){
        if(!changeSplash.get()) return Color.white;
        heat[0] = Mathf.lerpDelta(heat[0], 0, 0.1f);
        return Tmp.c1.set(Color.white).lerp(Color.yellow, heat[0]);
    }

    private static Label createCopyableLabel(String text, @Nullable Element hitter, Func<String, String> hintBuilder){
        Label label = new Label(text);
        hitter = hitter == null ? label : hitter;
        hitter.touchable = Touchable.enabled;
        hitter.tapped(() -> {
            String t = label.getText().toString();
            Core.app.setClipboardText(t);
            UIExt.announce(hintBuilder.get(t));
        });
        return label;
    }

    public static String arcVarsText(LVar s){
        return s.isobj ? PrintI.toString(s.objval) : Math.abs(s.numval - (long)s.numval) < 0.00001 ? (long)s.numval + "" : s.numval + "";
    }

    public static Color arcVarsColor(LVar s){
        if(s.constant && s.name.startsWith("@")) return Color.goldenrod;
        if(s.constant) return Pal.accent;
        //More light as foreground
        return LogicDialog.typeColor(s, Tmp.c1).lerp(Color.white, 0.5f);
    }

    public static void buildMemoryTools(Table table, MemoryBuild build){
        Table vars = new Table();

        table.background(Styles.black3);
        table.table(t -> {
            t.add(LogicSupport.memoryColumns.uiElement()).minWidth(200f).padLeft(4f);
            t.add(LogicSupport.memoryDecimal.uiElement()).minWidth(200f).padLeft(4f);
            t.button(Icon.refresh, Styles.clearNonei, () -> {
                vars.clearChildren();
                buildMemoryPane(vars, build.memory);
            });
        }).row();
        buildMemoryPane(vars, build.memory);
        table.pane(Styles.noBarPane, vars).touchable(Touchable.disabled).maxHeight(500f).fillX().pad(4).get().setScrollingDisabledX(true);
        vars.update(() -> {
            vars.getCells().each(cell -> {
                if(cell.prefWidth() > cell.maxWidth()){
                    cell.width(cell.prefWidth());
                    vars.invalidateHierarchy();
                }
            });
            if(vars.needsLayout()) table.pack();
        });
    }

    public static void buildMemoryPane(Table t, double[] memory){
        Format format = new Format(LogicSupport.memoryDecimal.get());
        for(int i = 0; i < memory.length; i++){
            int finalI = i;
            t.add("[" + i + "]").color(Color.lightGray).align(Align.left);
            t.add().width(8);
            t.label(() -> format.format((float)memory[finalI])).growX().align(Align.right).labelAlign(Align.right)
            .touchable(Touchable.enabled).get().tapped(() -> {
                Core.app.setClipboardText(memory[finalI] + "");
                UIExt.announce(VarsX.bundle.copiedMemory(memory[finalI]));
            });
            if((i + 1) % LogicSupport.memoryColumns.get() == 0) t.row();
            else t.add("|").color(((i % LogicSupport.memoryColumns.get()) % 2 == 0) ? Color.cyan : Color.acid)
            .padLeft(12).padRight(12);
        }
    }

    //region LogicBlock

    private static boolean showVars = false;

    public static void buildLogicTools(Table table, LogicBuild build){
        var block = (LogicBlock)build.block;

        table.setBackground(Styles.black3);
        Table vars = new Table();
        table.table(t -> {
            t.defaults().size(40);
            t.button(Icon.pencil, Styles.cleari, () -> {
                if(!block.accessible())
                    UIExt.announce(i("[yellow]当前无权编辑，仅供查阅"));
                build.showEditDialog();
            });
            t.button(Icon.info, Styles.cleari, () -> {
                showVars = !showVars;
                vars.clear();
                if(showVars) buildLogicVarTable(vars, build.executor);
            });
            t.button(Icon.trash, Styles.cleari, () -> {
                build.links.clear();
                build.updateCode(build.code, true, null);
            }).disabled(b -> net.client()).tooltip(i("重置所有链接"));
            t.button(Icon.paste, Styles.cleari, () -> showLogicCodePickDialog(block, build)).tooltip(i("从蓝图中选择代码"));
        });
        table.row().pane(Styles.noBarPane, vars).pad(4).maxHeight(400f).touchable(Touchable.disabled).get().setScrollingDisabledX(true);
        if(showVars) buildLogicVarTable(vars, build.executor);
    }

    private static void showLogicCodePickDialog(LogicBlock block, LogicBuild build){
        var all = schematics.all().select(it -> it.tiles.contains(s -> s.block instanceof LogicBlock));
        new BaseDialog(i("选择代码")){{
            addCloseButton();
            closeOnBack();
            cont.add(i("TIP: 所有包含处理器的蓝图")).row();
            cont.pane(tt -> {
                for(var schem : all){
                    tt.button(schem.name(), () -> {
                        var blocks = schem.tiles.select(s -> s.block instanceof LogicBlock);
                        cont.clear();
                        LogicBuild tmp = block.new LogicBuild();
                        for(var block : blocks){
                            tmp.readCompressed((byte[])block.config, false);
                            String code = tmp.code;
                            cont.button(block.block.emoji(), () -> {
                                build.configure(block.config);
                                hide();
                            }).tooltip(Strings.truncate(code, 300, "\n...")).size(iconMed);
                        }
                        if(blocks.size == 1){
                            cont.getChildren().pop().change();
                        }
                    }).growX().row();
                }
            }).growX().maxWidth(600f);
        }}.show();
    }

    private static void buildLogicVarTable(Table table, LExecutor executor){
        final var vars = executor.vars;
        table.update(() -> {
            if(vars != executor.vars){
                table.clear();
                buildLogicVarTable(table, executor);
                return;
            }
            table.getCells().each(cell -> {
                if(cell.prefWidth() > cell.maxWidth()){
                    cell.width(cell.prefWidth());
                    table.invalidateHierarchy();
                }
            });
            if(table.needsLayout()) table.parent.parent.pack();
        });

        table.setColor(Color.lightGray);
        for(var s : vars){
            if(s.name.startsWith("___")) continue;
            table.add(s.name).color(LogicSupport.arcVarsColor(s)).align(Align.left);
            table.label(() -> LogicSupport.arcVarsText(s)).align(Align.right).labelAlign(Align.right);
            table.row();
        }
    }
}
