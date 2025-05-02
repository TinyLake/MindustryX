package mindustryX.features.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
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
    private static float period = 15f;
    private static final Table varsTable = new Table(), linkTable = new Table();
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
                    linkTable.background(Styles.grayPanel);
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
                        buttons.button("链接表", style, () -> {
                            pane.setWidget(linkTable);
                            linkTable.clearChildren();
                        }).checked(b -> pane.getWidget() == linkTable);
                    }).growX().row();
                    cont.add(pane).minHeight(450f).fillX().scrollX(false).get();
                }).width(400f).padTop(8f);

                Interval interval = new Interval();
                main.update(() -> {
                    if(varsTable.hasParent() && !varsTable.hasChildren()) rebuildVarsTable();
                    if(linkTable.hasParent() && !linkTable.hasChildren()) rebuildLinkTable();
                    refresh = autoRefresh && interval.get(period);
                });
            });
        });

        logic.shown(() -> {
            canvas = logic.canvas;
            executor = Reflect.get(logic, "executor");
            consumer = Reflect.get(logic, "consumer");

            varsTable.clearChildren();
            linkTable.clearChildren();
        });

//        logic.resized(() -> {
//            if(mainTable.visible){
//                rebuildVarsTable();
//                rebuildLinkTable();
//            }
//        });
    }

    private static void buildConfigTable(Table table){
        table.background(Styles.black3);
        table.table(t -> {
            t.add("刷新间隔").padRight(5f).left();
            TextField field = t.field((int)period + "", text -> period = Integer.parseInt(text)).width(100f).valid(Strings::canParsePositiveInt).maxTextLength(5).get();
            t.slider(1, 60, 1, period, res -> {
                period = res;
                field.setText((int)res + "");
            });
        }).row();
        table.table(t -> {
            t.defaults().size(50f);
            t.button(Icon.downloadSmall, Styles.cleari, () -> {
                consumer.get(canvas.save());
                rebuildVarsTable();
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
                Label nameLabel = table.labelWrap(v.name).ellipsis(true).expand(2, 1).fill().get();
                Label valueLabel = table.labelWrap(arcVarsText(v)).ellipsis(true).padLeft(16f).expand(3, 1).fill().get();

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

                nameLabel.tapped(() -> {
                    Core.app.setClipboardText(v.name);
                    UIExt.announce("[cyan]复制变量名[white]\n " + v.name);
                });
                valueLabel.tapped(() -> {
                    Core.app.setClipboardText(valueLabel.getText().toString());
                    UIExt.announce("[cyan]复制变量属性[white]\n " + valueLabel.getText());
                });
            }).row();
        }

        varsTable.table(Tex.whitePane, table -> {
            Color color = Color.valueOf("#e600e6");

            table.setColor(color);
            table.add("@printbuffer").center().row();
            Label label = table.labelWrap("").labelAlign(Align.topLeft).minHeight(150).growX().get();

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

            table.touchable = Touchable.enabled;
            table.tapped(() -> {
                String text = executor.textBuffer.toString();
                Core.app.setClipboardText(text);
                UIExt.announce("[cyan]复制信息版[white]\n " + text);
            });
        }).fillX().row();
    }

    private static void rebuildLinkTable(){
        linkTable.top().clearChildren();

        Color color = Color.valueOf("#e600e6");

        int index = 0;
        for(LogicLink link : executor.build.links){
            if(link.active && link.valid){
                int finalIndex = index;
                linkTable.table(Tex.whitePane, table -> {
                    table.left();
                    Label label = table.labelWrap(link.name).ellipsis(true).expand(2, 1).fill().get();
                    Label indexLabel = table.labelWrap("[" + finalIndex + "]").padLeft(16f).expand(3, 1).fill().get();

                    final float[] heat = {1};
                    label.update(() -> {
                        if(refresh){
                            String text = link.name;
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

                    label.tapped(() -> {
                        String text = link.name;
                        Core.app.setClipboardText(text);
                        UIExt.announce("[cyan]复制链接建筑[white]\n " + text);
                    });
                    indexLabel.tapped(() -> {
                        String text = finalIndex + "";
                        Core.app.setClipboardText(text);
                        UIExt.announce("[cyan]复制链接建筑索引[white]\n " + text);
                    });
                }).padTop(10f).growX().row();

                index++;
            }
        }
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
