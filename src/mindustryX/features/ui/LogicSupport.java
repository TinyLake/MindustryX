package mindustryX.features.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
import mindustry.ui.*;
import mindustry.world.blocks.logic.LogicBlock.*;
import mindustryX.features.*;
import mindustryX.features.Settings;

import static mindustry.Vars.*;

public class LogicSupport{
    private static float refreshTime = 15f;
    private static final Table varsTable = new Table(), constTable = new Table();
    private static Table mainTable;

    private static boolean refresh;
    private static boolean changeSplash = true, autoRefresh = true;

    // work dialog
    private static LCanvas canvas;
    private static @Nullable LExecutor executor;
    private static Cons<String> consumer = s -> {
    };

    public static void init(){
        LogicDialog logic = ui.logic;

        changeSplash = Core.settings.getBool("logicSupportChangeSplash");

        logic.fill(t -> {
            t.left().name = "logicSupportX";

            t.visibility = () -> !Core.graphics.isPortrait();

            t.button(Icon.rightOpen, Styles.clearNonei, iconMed, () -> {
                Settings.toggle("logicSupport");
            }).height(150f).visible(() -> !mainTable.visible);

            t.fill(main -> {
                mainTable = main;

                main.marginLeft(16f);
                main.left().name = "logicSupportX";
                main.visible(() -> Core.settings.getBool("logicSupport"));

                main.table(LogicSupport::buildConfigTable).fillX().row();
                main.table(cont -> {
                    cont.top();

                    varsTable.background(Styles.grayPanel);
                    constTable.background(Styles.grayPanel);
                    TextButtonStyle style = new TextButtonStyle(Styles.defaultt){{
                        over = checked = Styles.grayPanel;
                        up = Styles.black;
                        down = Styles.black;
                    }};

                    ScrollPane pane = new ScrollPane(varsTable, Styles.noBarPane);
                    cont.table(buttons -> {
                        buttons.defaults().height(iconMed).growX();
                        buttons.button("变量表", style, () -> {
                            pane.setWidget(varsTable);
                            varsTable.clearChildren();
                        }).checked(b -> pane.getWidget() == varsTable);
                        buttons.button("常量表", style, () -> {
                            pane.setWidget(constTable); // 仅打开逻辑页面时重构
                        }).checked(b -> pane.getWidget() == constTable);
                    }).growX().row();
                    cont.add(pane).minHeight(450f).fillX().scrollX(false).get();
                }).width(400f).padTop(8f);

                Interval interval = new Interval();
                main.update(() -> {
                    if(varsTable.hasParent() && !varsTable.hasChildren()) rebuildVarsTable();
                    if(constTable.hasParent() && !constTable.hasChildren()) rebuildConstTable();
                    refresh = autoRefresh && interval.get(refreshTime);
                });
            });
        });

        logic.shown(() -> {
            canvas = logic.canvas;
            executor = Reflect.get(logic, "executor");
            consumer = Reflect.get(logic, "consumer");

            clearTables();
        });

//        logic.resized(() -> {
//            if(mainTable.visible){
//                rebuildVarsTable();
//                rebuildLinkTable();
//            }
//        });
    }

    private static void clearTables(){
        varsTable.clearChildren();
        constTable.clearChildren();
    }

    private static void buildConfigTable(Table table){
        table.background(Styles.black3);
        table.table(t -> {
            t.add("刷新间隔").padRight(5f).left();
            TextField field = t.field((int)refreshTime + "", text -> refreshTime = Integer.parseInt(text)).width(100f).valid(Strings::canParsePositiveInt).maxTextLength(5).get();
            t.slider(1, 60, 1, refreshTime, res -> {
                refreshTime = res;
                field.setText((int)res + "");
            });
        }).row();
        table.table(t -> {
            t.defaults().size(50f);
            t.button(Icon.downloadSmall, Styles.cleari, () -> {
                consumer.get(canvas.save());
                clearTables();
                UIExt.announce("[orange]已更新编辑的逻辑！");
            }).tooltip("更新编辑的逻辑");
            t.button(Icon.eyeSmall, Styles.clearTogglei, () -> {
                changeSplash ^= true;
                String text = "[orange]已" + (changeSplash ? "开启" : "关闭") + "变动闪烁";
                UIExt.announce(text);
                Core.settings.put("logicSupportChangeSplash", changeSplash);
            }).checked((b) -> changeSplash).tooltip("变量变动闪烁");
            t.button(Icon.refreshSmall, Styles.clearTogglei, () -> {
                autoRefresh = !autoRefresh;
                String text = "[orange]已" + (autoRefresh ? "开启" : "关闭") + "变量自动更新";
                UIExt.announce(text);
            }).checked((b) -> autoRefresh).tooltip("自动刷新变量");
            t.button(Icon.pause, Styles.clearTogglei, () -> {
                if(state.isPaused()) state.set(State.playing);
                else state.set(State.paused);
                String text = state.isPaused() ? "已暂停" : "已继续游戏";
                UIExt.announce(text);
            }).checked((b) -> state.isPaused()).tooltip("暂停逻辑(游戏)运行");
            t.button(Icon.eyeOffSmall, Styles.cleari, () -> Settings.toggle("logicSupport")).tooltip("隐藏逻辑辅助器");
        });
    }

    private static void rebuildVarsTable(){
        varsTable.top().clearChildren();
        if(executor == null) return;
        varsTable.defaults().padTop(10f).growX();

        for(var v : executor.vars){
            if(v.name.startsWith("___")) continue;
            varsTable.table(Tex.whitePane, table -> {
                Label valueLabel = createCopiedLabel(arcVarsText(v), null, "[cyan]复制变量属性[white]\n@");
                Label nameLabel = createCopiedLabel(v.name, null, "[cyan]复制变量名[white]\n@");

                table.add(nameLabel).ellipsis(true).wrap().expand(3, 1).fill().get();
                table.add(valueLabel).ellipsis(true).wrap().padLeft(16f).expand(2, 1).fill().get();

                Color typeColor = arcVarsColor(v);
                final float[] heat = {1};
                valueLabel.update(() -> {
                    if(refresh){
                        String text = arcVarsText(v);
                        if(!valueLabel.textEquals(text)){
                            heat[0] = 1;
                            typeColor.set(arcVarsColor(v));
                            valueLabel.setText(text);
                        }
                    }

                    if(changeSplash){
                        heat[0] = Mathf.lerpDelta(heat[0], 0, 0.1f);
                        table.color.set(Tmp.c1.set(typeColor).lerp(Color.white, heat[0]));
                    }else{
                        table.color.set(typeColor);
                    }
                });
            }).row();
        }

        varsTable.table(Tex.whitePane, table -> {
            Color color = Color.valueOf("#e600e6");
            Label label = createCopiedLabel("", table, "[cyan]复制信息版[white]\n@");

            table.setColor(color);
            table.add("@printbuffer").center().row();
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

                if(changeSplash){
                    heat[0] = Mathf.lerpDelta(heat[0], 0, 0.1f);
                    table.color.set(Tmp.c1.set(color).lerp(Color.white, heat[0]));
                }else{
                    table.color.set(color);
                }
            });
        }).fillX().row();
    }

    private static void rebuildConstTable(){
        constTable.top().clearChildren();

        Seq<LVar> constVars = new Seq<>();
        executor.build.updateCode(executor.build.code, true, assembler -> {
            constVars.set(assembler.vars.values().toSeq().select(v -> v.constant));
        });

        Color color = Color.valueOf("#e600e6");
        constTable.defaults().padTop(10f).growX();

        for(LVar v : constVars){
            if(v.name.startsWith("___")) continue;
            if(v.isobj && v.obj() instanceof Building building && Structs.contains(executor.links, building)) continue;

            constTable.table(Tex.whitePane, table -> {
                table.setColor(color);

                table.add(createCopiedLabel(v.name, null, "[cyan]复制常量名[white]\n@")).ellipsis(true).wrap().expand(3, 1).fill();
                table.add(createCopiedLabel(arcVarsText(v), null, "[cyan]复制常量[white]\n@")).ellipsis(true).wrap().padLeft(16f).expand(2, 1).fill();
            }).row();
        }

        int index = 0;
        for(LogicLink link : executor.build.links){
            if(link.active && link.valid){
                int finalIndex = index;
                constTable.table(Tex.whitePane, table -> {
                    table.left().setColor(color);

                    table.add(createCopiedLabel(link.name, null, "[cyan]复制链接建筑[white]\n@")).ellipsis(true).wrap().expand(3, 1).fill();
                    table.table(indexTable -> {
                        indexTable.left();

                        indexTable.add("[");
                        indexTable.add(createCopiedLabel("" + finalIndex, indexTable, "[cyan]复制链接索引[white]\n@")).labelAlign(Align.center).minWidth(24f);
                        indexTable.add("]");
                    }).padLeft(16f).expand(2, 1).fill();
                }).row();

                index++;
            }
        }
    }

    private static Label createCopiedLabel(String text, @Nullable  Element hitter, String hint){
        Label label = new Label(text);
        hitter = hitter == null ? label : hitter;
        hitter.touchable = Touchable.enabled;
        hitter.tapped(() -> {
            String t = label.getText().toString();
            Core.app.setClipboardText(t);
            UIExt.announce(Strings.format(hint, t));
        });
        return label;
    }

    public static String arcVarsText(LVar s){
        return s.isobj ? PrintI.toString(s.objval) : Math.abs(s.numval - (long)s.numval) < 0.00001 ? (long)s.numval + "" : s.numval + "";
    }

    public static Color arcVarsColor(LVar s){
        if(s.constant && s.name.startsWith("@")) return Color.goldenrod;
        else if(s.constant) return Color.valueOf("00cc7e");
        else return LogicDialog.typeColor(s, new Color());
    }
}
