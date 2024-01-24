package mindustry.editor;

import arc.Core;
import arc.scene.ui.*;
import arc.struct.*;
import mindustry.*;
import mindustry.arcModule.ui.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.filters.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapInfoDialog extends BaseDialog{
    private final ArcWaveInfoDialog waveInfo;
    private final MapGenerateDialog generate;
    private final CustomRulesDialog ruleInfo = new CustomRulesDialog();
    private final MapObjectivesDialog objectives = new MapObjectivesDialog();

    public MapInfoDialog(){
        super("@editor.mapinfo");
        this.waveInfo = new ArcWaveInfoDialog();
        this.generate = new MapGenerateDialog(false);

        addCloseButton();

        shown(this::setup);
    }

    private void setup(){
        cont.clear();

        ObjectMap<String, String> tags = editor.tags;

        cont.pane(t -> {
            t.add("@editor.mapname").padRight(8).left();
            t.defaults().padTop(15);

            TextField name = t.field(state.isPlaying() ? state.map.name() : tags.get("name", ""), text -> {
                tags.put("name", text);
            }).size(400, 55f).maxTextLength(50).get();
            name.setMessageText("@unknown");

            t.row();
            t.add("@editor.description").padRight(8).left();

            TextArea description = t.area(state.isPlaying() ? state.map.description() : tags.get("description", ""), Styles.areaField, text -> {
                tags.put("description", text);
            }).size(400f, 140f).maxTextLength(1000).get();

            t.row();
            t.add("[acid]地图模式标签").padRight(8).left();

            TextField modeName = t.field(state.isPlaying() ? state.rules.modeName : tags.get("modeName", ""), Styles.areaField, text -> {
                tags.put("modeName", text);
            }).size(400f, 55f).maxTextLength(1000).get();

            if (Core.settings.getBool("arcPlayerList")) {
                t.row();
                t.add("[cyan]服务器标签").padRight(8).left();
                t.button("编辑...", () -> {
                    BaseDialog dialog = new BaseDialog("地图标签");

                    dialog.cont.pane(td -> {
                        td.add("[cyan]微泽系服务器标签编辑器 \n[white]BY [violet]Lucky Clover"
                                + "\n\n[white]用于国内新版微泽系插件标签。"
                                + "\n[orange]如果您发现标签无效，可能是您所在的服务器插件版本过低。请联系服主更新插件"
                                + "\n所有标签与主服同步"
                                + "\n\n[red]需要退出地图界面后重新打开才会更新"
                                + "\n\n[white]如果有bug欢迎提出"
                        );
                        td.row();
                        td.row();

                        td.add("PVP保护时间(s)").left();
                        td.field(arcReadStringLabel("@pvpProtect"), pama -> {
                            arcAddStringLabel(pama, "@pvpProtect");
                        }).maxTextLength(5).left();
                        td.row();
                        td.add("禁用队伍(如1,2,3...)").left();
                        td.field(arcReadStringLabel("@banTeam"), pama -> {
                            arcAddStringLabel(pama, "@banTeam");
                        }).maxTextLength(5).left();
                        td.row();
                        td.add("插件选择").left();
                        td.field(arcReadStringLabel("@mapScript"), pama -> {
                            arcAddStringLabel(pama, "@mapScript");
                        }).maxTextLength(6).left();
                        td.row();
                        td.check("空域管制 [acid]敌方核心保护区内禁止空军", arcreadBoolLable("[@limitAir]"), islimit -> {
                            arcAddBoolLable(islimit, "[@limitAir]");
                        }).left();
                        td.row();
                        td.check("塔防模式 [acid]怪物仅会在出生点地板移动，不会攻击", arcreadBoolLable("[@towerDefend]"), islimit -> {
                            arcAddBoolLable(islimit, "[@towerDefend]");
                        }).left();
                        td.row();
                        td.check("敌人掉落 [acid]非塔防模式下，打怪掉落资源", arcreadBoolLable("[@TDDrop]"), islimit -> {
                            arcAddBoolLable(islimit, "[@TDDrop]");
                        }).left();
                        td.row();
                        td.check("水漫金山 [acid]蓝队核心会释放洪水淹没你的核心", arcreadBoolLable("[@floodV2]"), islimit -> {
                            arcAddBoolLable(islimit, "[@flood]");
                        }).left();
                    }).left();
                    dialog.row();
                    dialog.addCloseButton();
                    dialog.margin(16f);

                    dialog.show();
                }).left().width(200f);
            }


            t.row();
            t.add("@editor.author").padRight(8).left();

            TextField author = t.field(state.isPlaying() ? state.map.author() : tags.get("author", ""), text -> {
                tags.put("author", text);
            }).size(400, 55f).maxTextLength(50).get();
            author.setMessageText("@unknown");

            t.row();

            t.table(Tex.button, r -> {
                r.defaults().width(230f).height(60f);

                var style = Styles.flatt;

                r.button("@editor.rules", Icon.list, style, () -> {
                    ruleInfo.show(Vars.state.rules, () -> Vars.state.rules = new Rules());
                    hide();
                }).marginLeft(10f);

                r.button("@editor.waves", Icon.units, style, () -> {
                    waveInfo.show();
                    hide();
                }).marginLeft(10f);

                r.row();

                r.button("@editor.objectives", Icon.info, style, () -> {
                    objectives.show(state.rules.objectives.all, state.rules.objectives.all::set);
                    hide();
                }).marginLeft(10f);

                r.button("@editor.generation", Icon.terrain, style, () -> {
                    //randomize so they're not all the same seed
                    var res = maps.readFilters(editor.tags.get("genfilters", ""));
                    res.each(GenerateFilter::randomize);

                    generate.show(res,
                    filters -> {
                        //reset seed to 0 so it is not written
                        filters.each(f -> f.seed = 0);
                        editor.tags.put("genfilters", JsonIO.write(filters));
                    });
                    hide();
                }).marginLeft(10f);
            }).colspan(2).center();

            name.change();
            description.change();
            modeName.change();
            author.change();

            t.margin(16f);
        });

    }

    private String arcReadStringLabel(String labelName) {
        ObjectMap<String, String> tags = editor.tags;
        String des = tags.get("description", "");
        try {
            int strLength = des.length();
            int stopindex = 0;
            for (int i = 0; i < strLength; i++) {
                if (des.substring(i, i + 2 + labelName.length()).equals("[" + labelName + "=")) {
                    stopindex = i;
                    break;
                }
            }
            for (int i = stopindex; i < strLength; i++) {
                if (des.charAt(i) == ']') {
                    return des.substring(stopindex + 2 + labelName.length(), i);
                }
            }
        } catch (Exception e) {
        }
        return "<未设定参数>";
    }

    private void arcAddStringLabel(String parameter, String labelName) {
        ObjectMap<String, String> tags = editor.tags;
        String des = tags.get("description", "");
        if (des.contains(labelName)) {
            try {
                int strLength = des.length();
                int stopindex = 0;
                for (int i = 0; i < strLength; i++) {
                    if (des.substring(i, i + 2 + labelName.length()).equals("[" + labelName + "=")) {
                        stopindex = i;
                        break;
                    }
                }
                for (int i = stopindex; i < strLength; i++) {
                    if (des.charAt(i) == ']') {
                        des = des.substring(0, stopindex + 2 + labelName.length()) + parameter + des.substring(i, strLength + 1);
                        tags.put("description", des);
                        break;
                    }
                }
            } catch (Exception e) {
            }
        } else {
            tags.put("description", des.concat("[" + labelName + "=" + parameter + "]"));
        }
    }

    private boolean arcreadBoolLable(String labelname) {
        ObjectMap<String, String> tags = editor.tags;
        if (tags.get("description", "").contains(labelname)) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    private void arcAddBoolLable(Boolean isadd, String labelname) {
        ObjectMap<String, String> tags = editor.tags;
        String des = tags.get("description", "");
        if (des.contains(labelname) && !isadd) {
            tags.put("description", des.replace(labelname, ""));
        } else if (!des.contains(labelname) && isadd) {
            tags.put("description", des.concat(labelname));
        }
    }
}
