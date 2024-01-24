package mindustry.logic;

import arc.Core;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.actions.Actions;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Time;
import mindustry.core.GameState.State;
import mindustry.ctype.Content;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.logic.LExecutor.PrintI;
import mindustry.logic.LExecutor.Var;
import mindustry.logic.LStatements.InvalidStatement;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.logic.LCanvas.tooltip;

public class LogicDialog extends BaseDialog{
    public LCanvas canvas;
    Cons<String> consumer = s -> {};
    boolean privileged;
    public static float period = 15f;
    float counter = 0f;
    Table varTable = new Table();
    Table mainTable = new Table();
    public static boolean refreshing = true;

    public static String transText = "";

    @Nullable LExecutor executor;

    private boolean dispose = false;

    public LogicDialog(){
        super("logic");

        clearChildren();

        canvas = new LCanvas();
        shouldPause = true;

        addCloseListener();

        shown(this::setup);
        hidden(() -> {
            if(!dispose){
                consumer.get(canvas.save());
            } else {
                dispose = false;
            }});
        onResize(() -> {
            setup();
            canvas.rebuild();
            varsTable();
        });


        add(mainTable).grow().name("canvas");
        rebuildMain();

        row();

        add(buttons).growX().name("canvas");
    }

    private void rebuildMain(){
        mainTable.clear();
        canvas.rebuild();
        if(!Core.settings.getBool("logicSupport"))  {
            mainTable.add(canvas).grow();
        }else{
            varsTable();
            mainTable.add(varTable);
            mainTable.add(canvas).grow();
            counter=0;
            varTable.update(()->{
                counter+=Time.delta;
                if(counter>period && refreshing){
                    counter=0;
                }
            });
        }
    }
    private void varsTable(){
        varTable.clear();
        varTable.table(t->{
            t.table(tt->{
                tt.add("刷新间隔").padRight(5f).left();
                TextField field = tt.field((int)period + "", text -> {
                    period = Integer.parseInt(text);
                }).width(100f).valid(Strings::canParsePositiveInt).maxTextLength(5).get();
                tt.slider(1, 60,1, period, res -> {
                    period = res;
                    field.setText((int)res + "");
                });
            });
            t.row();
            t.table(tt -> {
                tt.button(Icon.cancelSmall, Styles.cleari, () -> {
                    Core.settings.put("logicSupport", !Core.settings.getBool("logicSupport"));
                    arcui.arcInfo("[orange]已关闭逻辑辅助器！");
                    rebuildMain();
                }).size(50f);
                tt.button(Icon.refreshSmall, Styles.cleari, () -> {
                    executor.build.updateCode(executor.build.code);
                    varsTable();
                    arcui.arcInfo("[orange]已更新逻辑显示！");
                }).size(50f);
                tt.button(Icon.pauseSmall, Styles.cleari, () -> {
                    refreshing = !refreshing;
                    arcui.arcInfo("[orange]已" + (refreshing ? "开启" : "关闭") + "逻辑刷新");
                }).checked(refreshing).size(50f);
                tt.button(Icon.rightOpenOutSmall, Styles.cleari, () -> {
                    Core.settings.put("rectJumpLine", !Core.settings.getBool("rectJumpLine"));
                    arcui.arcInfo("[orange]已" + (refreshing ? "开启" : "关闭") + "方形跳转线");
                    this.canvas.rebuild();
                }).checked(refreshing).size(50f);

                tt.button(Icon.playSmall, Styles.cleari, () -> {
                    if (state.isPaused()) state.set(State.playing);
                    else state.set(State.paused);
                    arcui.arcInfo(state.isPaused() ? "已暂停" : "已继续游戏");
                }).checked(state.isPaused()).size(50f);
            });
        });
        varTable.row();
            varTable.pane(t->{
                if(executor==null) return;
                for(var s : executor.vars){
                    if(s.name.startsWith("___")) continue;
                    String text = arcVarsText(s);
                    t.table(tt->{
                        tt.background(Tex.whitePane);

                        tt.table(tv->{
                            tv.labelWrap(s.name).width(100f);
                            tv.touchable = Touchable.enabled;
                            tv.tapped(()->{
                                Core.app.setClipboardText(s.name);
                                arcui.arcInfo("[cyan]复制变量名[white]\n " + s.name);
                            });
                        });
                        tt.table(tv->{
                            Label varPro = tv.labelWrap(text).width(200f).get();
                            tv.touchable = Touchable.enabled;
                            tv.tapped(()->{
                                Core.app.setClipboardText(varPro.getText().toString());
                                arcui.arcInfo("[cyan]复制变量属性[white]\n " + varPro.getText());
                            });
                            tv.update(()->{
                                if(counter + Time.delta>period && refreshing){
                                    varPro.setText(arcVarsText(s));
                                }
                            });
                        }).padLeft(20f);

                        tt.update(()->{
                            if(counter + Time.delta>period && refreshing){
                                tt.setColor(arcVarsColor(s));
                            }
                        });

                    }).padTop(10f).row();
                }
                t.table(tt->{
                    tt.background(Tex.whitePane);

                    tt.table(tv->{
                        Label varPro = tv.labelWrap(executor.textBuffer.toString()).width(300f).get();
                        tv.touchable = Touchable.enabled;
                        tv.tapped(()->{
                            Core.app.setClipboardText(varPro.getText().toString());
                            arcui.arcInfo("[cyan]复制信息版[white]\n " + varPro.getText());
                        });
                        tv.update(()->{
                            if(counter + Time.delta>period && refreshing){
                                varPro.setText(executor.textBuffer.toString());
                            }
                        });
                    }).padLeft(20f);

                    tt.update(()->{
                        if(counter + Time.delta>period && refreshing){
                            tt.setColor(Color.valueOf("#e600e6"));
                        }
                    });

                }).padTop(10f).row();
            }).width(400f).padLeft(20f);
    }

    public static String arcVarsText(Var s){
        return s.isobj ? PrintI.toString(s.objval) : Math.abs(s.numval - (long)s.numval) < 0.00001 ? (long)s.numval + "" : s.numval + "";
    }

    public static Color arcVarsColor(Var s){
        if(s.constant && s.name.startsWith("@")) return Color.goldenrod;
        else if (s.constant) return Color.valueOf("00cc7e");
        else return typeColor(s,new Color());
    }

    private static Color typeColor(Var s, Color color){
        return color.set(
            !s.isobj ? Pal.place :
            s.objval == null ? Color.darkGray :
            s.objval instanceof String ? Pal.ammo :
            s.objval instanceof Content ? Pal.logicOperations :
            s.objval instanceof Building ? Pal.logicBlocks :
            s.objval instanceof Unit ? Pal.logicUnits :
            s.objval instanceof Team ? Pal.logicUnits :
            s.objval instanceof Enum<?> ? Pal.logicIo :
            Color.white
        );
    }

    private String typeName(Var s){
        return
            !s.isobj ? "number" :
            s.objval == null ? "null" :
            s.objval instanceof String ? "string" :
            s.objval instanceof Content ? "content" :
            s.objval instanceof Building ? "building" :
            s.objval instanceof Team ? "team" :
            s.objval instanceof Unit ? "unit" :
            s.objval instanceof Enum<?> ? "enum" :
            "unknown";
    }

    private void setup(){
        buttons.clearChildren();
        buttons.defaults().size(160f, 64f);
        buttons.button("@back", Icon.left, this::hide).name("back");

        buttons.button("@edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@editor.export");
            dialog.cont.pane(p -> {
                p.margin(10f);
                p.table(Tex.button, t -> {
                    TextButtonStyle style = Styles.flatt;
                    t.defaults().size(280f, 60f).left();

                    t.button("@schematic.copy", Icon.copy, style, () -> {
                        dialog.hide();
                        Core.app.setClipboardText(canvas.save());
                    }).marginLeft(12f);
                    t.row();
                    t.button("@schematic.copy.import", Icon.download, style, () -> {
                        dialog.hide();
                        try{
                            canvas.load(Core.app.getClipboardText().replace("\r\n", "\n"));
                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null);
                    t.row();
                    t.button("[orange]清空",Icon.trash,style,() -> canvas.clearAll()).marginLeft(12f);
                    t.row();
                    t.button("[orange]丢弃更改", Icon.cancel,style, () -> ui.showConfirm("确认丢弃?", () -> {
                        dispose = true;
                        dialog.hide();
                        hide();
                    })).marginLeft(12f);
                    t.row();
                    t.button("[orange]逻辑辅助器",Icon.settings,style,()-> {
                        Core.settings.put("logicSupport",!Core.settings.getBool("logicSupport"));
                        rebuildMain();
                    }).marginLeft(12f);
                });
            });

            dialog.addCloseButton();
            dialog.show();
        }).name("edit");

        if(Core.graphics.isPortrait()) buttons.row();

        buttons.button("@variables", Icon.menu, () -> {
            BaseDialog dialog = new BaseDialog("@variables");
            dialog.hidden(() -> {
                if(!wasPaused && !net.active()){
                    state.set(State.paused);
                }
            });

            dialog.shown(() -> {
                if(!wasPaused && !net.active()){
                    state.set(State.playing);
                }
            });

            dialog.cont.pane(p -> {
                p.margin(10f).marginRight(16f);
                p.table(Tex.button, t -> {
                    t.defaults().fillX().height(45f);
                    for(var s : executor.vars){
                        if(s.constant) continue;

                        Color varColor = Pal.gray;
                        float stub = 8f, mul = 0.5f, pad = 4;

                        t.add(new Image(Tex.whiteui, varColor.cpy().mul(mul))).width(stub);
                        t.stack(new Image(Tex.whiteui, varColor), new Label(" " + s.name + " ", Styles.outlineLabel){{
                            setColor(Pal.accent);
                        }}).padRight(pad);

                        t.add(new Image(Tex.whiteui, Pal.gray.cpy().mul(mul))).width(stub);
                        t.table(Tex.pane, out -> {
                            float[] counter = {-1f};
                            Label label = out.add("").style(Styles.outlineLabel).padLeft(4).padRight(4).width(140f).wrap().get();
                            label.update(() -> {
                                if(counter[0] < 0 || (counter[0] += Time.delta) >= period){
                                    String text = s.isobj ? PrintI.toString(s.objval) : Math.abs(s.numval - (long)s.numval) < 0.00001 ? (long)s.numval + "" : s.numval + "";
                                    if(!label.textEquals(text)){
                                        label.setText(text);
                                        if(counter[0] >= 0f){
                                            label.actions(Actions.color(Pal.accent), Actions.color(Color.white, 0.2f));
                                        }
                                    }
                                    counter[0] = 0f;
                                }
                            });
                            label.act(1f);
                        }).padRight(pad);

                        t.add(new Image(Tex.whiteui, typeColor(s, new Color()).mul(mul))).update(i -> i.setColor(typeColor(s, i.color).mul(mul))).width(stub);

                        t.stack(new Image(Tex.whiteui, typeColor(s, new Color())){{
                            update(() -> setColor(typeColor(s, color)));
                        }}, new Label(() -> " " + typeName(s) + " "){{
                            setStyle(Styles.outlineLabel);
                        }});

                        t.row();

                        t.add().growX().colspan(6).height(4).row();
                    }
                });
            });

            dialog.addCloseButton();
            dialog.show();
        }).name("variables").disabled(b -> executor == null || executor.vars.length == 0);

        buttons.button("@add", Icon.add, () -> {
            BaseDialog dialog = new BaseDialog("@add");
            dialog.cont.table(table -> {
                table.background(Tex.button);
                table.pane(t -> {
                    for(Prov<LStatement> prov : LogicIO.allStatements){
                        LStatement example = prov.get();
                        if(example instanceof InvalidStatement || example.hidden() || (example.privileged() && !privileged) || (example.nonPrivileged() && privileged)) continue;

                        LCategory category = example.category();
                        Table cat = t.find(category.name);
                        if(cat == null){
                            t.table(s -> {
                                if(category.icon != null){
                                    s.image(category.icon, Pal.darkishGray).left().size(15f).padRight(10f);
                                }
                                s.add(category.localized()).color(Pal.darkishGray).left().tooltip(category.description());
                                s.image(Tex.whiteui, Pal.darkishGray).left().height(5f).growX().padLeft(10f);
                            }).growX().pad(5f).padTop(10f);

                            t.row();

                            cat = t.table(c -> {
                                c.top().left();
                            }).name(category.name).top().left().growX().fillY().get();
                            t.row();
                        }

                        TextButtonStyle style = new TextButtonStyle(Styles.flatt);
                        style.fontColor = category.color;
                        style.font = Fonts.outline;

                        cat.button(example.name(), style, () -> {
                            canvas.add(prov.get());
                            dialog.hide();
                        }).size(130f, 50f).self(c -> tooltip(c, "lst." + example.name())).top().left();

                        if(cat.getChildren().size % 3 == 0) cat.row();
                    }
                }).grow();
            }).fill().maxHeight(Core.graphics.getHeight() * 0.8f);
            dialog.addCloseButton();
            dialog.show();
        }).disabled(t -> canvas.statements.getChildren().size >= LExecutor.maxInstructions);
    }

    public void show(String code, LExecutor executor, boolean privileged, Cons<String> modified){
        this.executor = executor;
        this.privileged = privileged;
        canvas.statements.clearChildren();
        canvas.rebuild();
        canvas.privileged = privileged;
        try{
            canvas.load(code);
        }catch(Throwable t){
            Log.err(t);
            canvas.load("");
        }
        this.consumer = result -> {
            if(!result.equals(code)){
                modified.get(result);
            }
        };
        varsTable();
        show();
    }
}
