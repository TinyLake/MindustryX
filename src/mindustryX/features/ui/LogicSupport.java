package mindustryX.features.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.GameState.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
import mindustry.ui.*;
import mindustryX.features.*;
import mindustryX.features.Settings;

import static mindustry.Vars.ui;

public class LogicSupport {
    private static float period = 15f;
    private static final Table varTable = new Table();
    private static boolean refreshing = true, doRefresh;

    // work dialog
    private static LCanvas canvas;
    private static @Nullable LExecutor executor;
    private static Cons<String> consumer = s -> {
    };

    public static void init(){
        LogicDialog logic = ui.logic;

        logic.fill(t -> {
            t.left().name = "logicSupportX";

            t.setFillParent(true);
            t.visible(() -> Core.settings.getBool("logicSupport"));

            t.center().left();
            t.table(LogicSupport::buildLogicSupport).growY();
            Interval interval = new Interval();
            t.update(() -> {
                if(!varTable.hasChildren()) buildVarsTable();
                doRefresh = refreshing && interval.get(period);
            });
        });
        logic.fill(t -> {
            t.name = "open_logicSupport";
            t.setFillParent(true);
            t.visible(() -> !Core.settings.getBool("logicSupport"));

            t.center().left().button(Icon.rightOpen, Styles.clearNonei, Vars.iconMed, () -> Settings.toggle("logicSupport"));
        });

        logic.shown(() -> {
            canvas = logic.canvas;
            executor = Reflect.get(logic, "executor");
            consumer = Reflect.get(logic, "consumer");
        });
    }

    private static void buildLogicSupport(Table table){
        table.background(Styles.black3);
        table.table(t -> {
            t.table(tt -> {
                tt.add("刷新间隔").padRight(5f).left();
                TextField field = tt.field((int)period + "", text -> period = Integer.parseInt(text)).width(100f).valid(Strings::canParsePositiveInt).maxTextLength(5).get();
                tt.slider(1, 60, 1, period, res -> {
                    period = res;
                    field.setText((int)res + "");
                });
            });
            t.row();
            t.table(tt -> {
                tt.defaults().size(50f);
                tt.button(Icon.downloadSmall, Styles.cleari, () -> {
                    executor.build.updateCode(executor.build.code);
                    buildVarsTable();
                    UIExt.announce("[orange]已重新加载逻辑！");
                }).tooltip("加载逻辑代码");
                tt.button(Icon.refreshSmall, Styles.clearTogglei, () -> {
                    refreshing = !refreshing;
                    String text = "[orange]已" + (refreshing ? "开启" : "关闭") + "逻辑刷新";
                    UIExt.announce(text);
                }).checked((b) -> refreshing).tooltip("辅助器自动刷新");
                tt.button(Icon.pause, Styles.clearTogglei, () -> {
                    if(Vars.state.isPaused()) Vars.state.set(State.playing);
                    else Vars.state.set(State.paused);
                    String text = Vars.state.isPaused() ? "已暂停" : "已继续游戏";
                    UIExt.announce(text);
                }).checked((b) -> Vars.state.isPaused()).tooltip("暂停逻辑(游戏)运行");
                tt.button(Icon.eyeOffSmall, Styles.cleari, () -> Settings.toggle("logicSupport")).tooltip("隐藏逻辑辅助器");
            });
        }).row();
        table.pane(varTable).width(400f).padLeft(20f);
    }

    private static void buildVarsTable(){
        varTable.clearChildren();
        if(executor == null) return;
        varTable.defaults().padTop(10f);
        for(var s : executor.vars){
            if(s.name.startsWith("___")) continue;
            varTable.table(Tex.whitePane, tt -> {
                tt.table(tv -> {
                    tv.labelWrap(s.name).width(100f);
                    tv.touchable = Touchable.enabled;
                    tv.tapped(() -> {
                        Core.app.setClipboardText(s.name);
                        UIExt.announce("[cyan]复制变量名[white]\n " + s.name);
                    });
                });
                tt.table(tv -> {
                    Label varPro = tv.labelWrap(arcVarsText(s)).width(200f).get();
                    tv.touchable = Touchable.enabled;
                    tv.tapped(() -> {
                        Core.app.setClipboardText(varPro.getText().toString());
                        UIExt.announce("[cyan]复制变量属性[white]\n " + varPro.getText());
                    });
                    tv.update(() -> {
                        if(doRefresh){
                            tt.setColor(arcVarsColor(s));
                            varPro.setText(arcVarsText(s));
                        }
                    });
                }).padLeft(20f);
            }).row();
        }
        varTable.table(Tex.whitePane, tt -> {
            tt.setColor(Color.valueOf("#e600e6"));
            tt.add("@printbuffer").center().row();
            var labelC = tt.labelWrap(() -> executor.textBuffer).labelAlign(Align.topLeft).minHeight(1).growX();
            tt.update(() -> {
                if(labelC.prefHeight() > labelC.minHeight())
                    labelC.height(labelC.prefHeight());
            });
            tt.touchable = Touchable.enabled;
            tt.tapped(() -> {
                String text = executor.textBuffer.toString();
                Core.app.setClipboardText(text);
                UIExt.announce("[cyan]复制信息版[white]\n " + text);
            });
        }).padLeft(4f).fillX().row();
    }

    public static String arcVarsText(LVar s){
        return s.isobj ? PrintI.toString(s.objval) : Math.abs(s.numval - (long)s.numval) < 0.00001 ? (long)s.numval + "" : s.numval + "";
    }

    public static Color arcVarsColor(LVar s){
        if(s.constant && s.name.startsWith("@")) return Color.goldenrod;
        else if (s.constant) return Color.valueOf("00cc7e");
        else return LogicDialog.typeColor(s,new Color());
    }
}
