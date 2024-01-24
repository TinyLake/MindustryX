package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.struct.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.arcModule.ARCVars;
import mindustry.arcModule.RFuncs;
import mindustry.arcModule.toolpack.picToMindustry;
import mindustry.arcModule.ui.dialogs.MessageDialog;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.content.UnitTypes;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.StatUnit;

import java.util.regex.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.arcModule.RFuncs.getPrefix;
import static mindustry.content.Items.*;

public class SchematicsDialog extends BaseDialog{
    private static final float tagh = 42f;
    private SchematicInfoDialog info = new SchematicInfoDialog();
    private Schematic firstSchematic;
    private String search = "";
    private TextField searchField;
    private Runnable rebuildPane = () -> {}, rebuildTags = () -> {};
    private Pattern ignoreSymbols = Pattern.compile("[`~!@#$%^&*()\\-_=+{}|;:'\",<.>/?]");
    private Seq<String> tags, selectedTags = new Seq<>();
    private boolean checkedTags;
    private String blueprintlink = "https://docs.qq.com/sheet/DVHNoS3lIcm1NbFFS";

    private String surpuloTags = UnitTypes.gamma.emoji(), erekirTags = UnitTypes.emanate.emoji();
    private  Seq<String> planetTags = new Seq<String>().add(surpuloTags,erekirTags);
    public static final String ShareType = "[blue]<Schem>";

    private boolean clipbroad = true;
    private boolean fromShare = false;

    public SchematicsDialog(){
        super("@schematics");

        Vars.netClient.addPacketHandler("arcNetSchematic", url -> Http.get(url, r -> readShare(r.getResultAsString().replace(" ", "+"), null)));

        Events.on(EventType.WorldLoadEvent.class, event -> {
            if(state.rules.env == Planets.serpulo.defaultEnv){
                if (state.rules.hiddenBuildItems.equals(Planets.erekir.hiddenItems)){
                    if (selectedTags.contains(erekirTags)) selectedTags.remove(erekirTags);
                    if (!selectedTags.contains(surpuloTags)) selectedTags.add(surpuloTags);
                }else{
                    if (selectedTags.contains(erekirTags)) selectedTags.remove(erekirTags);
                    if (selectedTags.contains(surpuloTags)) selectedTags.remove(surpuloTags);
                }

            }
            else if(state.rules.env == Planets.erekir.defaultEnv){
                if (selectedTags.contains(surpuloTags)) selectedTags.remove(surpuloTags);
                if (!selectedTags.contains(erekirTags)) selectedTags.add(erekirTags);
            }
        });

        Core.assets.load("sprites/schematic-background.png", Texture.class).loaded = t -> t.setWrap(TextureWrap.repeat);

        tags = Core.settings.getJson("schematic-tags", Seq.class, String.class, Seq::new);

        shouldPause = true;
        addCloseButton();
        buttons.button("@schematic.import", Icon.download, this::showImport);
        if (mobile) buttons.row();
        buttons.button("[cyan]蓝图档案馆", Icon.link, () -> {
            if(!Core.app.openURI(blueprintlink)){
                ui.showErrorMessage("@linkfail");
                Core.app.setClipboardText(blueprintlink);
            }
        });
        buttons.button("[violet]转换器[white] " + Blocks.canvas.emoji() + Blocks.logicDisplay.emoji() + Blocks.sorter.emoji(), Icon.image, picToMindustry::show);
        makeButtonOverlay();
        shown(this::setup);
        onResize(this::setup);
    }

    void setup(){
        if(!checkedTags){
            checkTags();
            checkedTags = true;
        }

        search = "";

        cont.top();
        cont.clear();

        cont.table(s -> {
            s.left();
            s.image(Icon.zoom);
            searchField = s.field(search, res -> {
                search = res;
                rebuildPane.run();
            }).growX().get();
            searchField.setMessageText("@schematic.search");
        }).fillX().padBottom(4);

        cont.row();

        cont.table(in -> {
            in.left();
            in.add("@schematic.tags").padRight(4);

            //tags (no scroll pane visible)
            in.pane(Styles.noBarPane, t -> {
                rebuildTags = () -> {
                    t.clearChildren();
                    t.left();

                    t.defaults().pad(2).height(tagh);
                    for(var tag : tags){
                        t.button(tag, Styles.togglet, () -> {
                            if(selectedTags.contains(tag)){
                                selectedTags.remove(tag);
                            }else{
                                selectedTags.add(tag);
                            }
                            rebuildPane.run();
                        }).checked(selectedTags.contains(tag)).with(c -> c.getLabel().setWrap(false));
                    }
                };
                rebuildTags.run();
            }).fillX().height(tagh).scrollY(false);

            in.button(Icon.pencilSmall, this::showAllTags).size(tagh).pad(2).tooltip("@schematic.edittags");
        }).height(tagh).fillX();

        cont.row();

        cont.table(in -> {
            in.left();
            in.add("科技树：").padRight(4);

            //tags (no scroll pane visible)
            in.pane(Styles.noBarPane, t -> {
                rebuildTags = () -> {
                    t.clearChildren();
                    t.left();

                    t.defaults().pad(2).height(tagh);
                    for(var tag : planetTags){
                        t.button(tag, Styles.togglet, () -> {
                            if(selectedTags.contains(tag)){
                                selectedTags.remove(tag);
                            }else{
                                selectedTags.add(tag);
                            }
                            rebuildPane.run();
                        }).checked(selectedTags.contains(tag)).with(c -> c.getLabel().setWrap(false));
                    }
                };
                rebuildTags.run();
            }).fillX().height(tagh).scrollY(false);

            in.button(Icon.refreshSmall, this::syncPlanetTags).size(tagh).pad(2).tooltip("刷新");
            in.add("辅助筛选：").padLeft(20f).padRight(4);
            in.button(copper.emoji(), Styles.togglet, () -> {
                        Core.settings.put("arcSchematicCanBuild", !Core.settings.getBool("arcSchematicCanBuild"));
                        rebuildPane.run();
                    }).size(tagh).pad(2).tooltip("可建造(核心有此类资源+地图未禁用)").checked(t->Core.settings.getBool("arcSchematicCanBuild"));
            if (Core.settings.getBool("autoSelSchematic")) {
                in.add("蓝图包含：").padLeft(20f).padRight(4);
                in.button(control.input.block == null ? "[red]\uE815" : control.input.block.emoji(), Styles.togglet, () -> {
                    control.input.block = null;
                    rebuildPane.run();
                }).size(tagh).pad(2).tooltip("蓝图需包含此建筑").checked(t -> control.input.block != null);
            }
        }).height(tagh).fillX();

        cont.row();

        cont.pane(t -> {
            t.top();

            t.update(() -> {
                if(Core.input.keyTap(Binding.chat) && Core.scene.getKeyboardFocus() == searchField && firstSchematic != null){
                    if(!state.rules.schematicsAllowed){
                        ui.showInfo("@schematic.disabled");
                    }else{
                        control.input.useSchematic(firstSchematic);
                        hide();
                    }
                }
            });

            rebuildPane = () -> {
                int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);

                t.clear();
                int i = 0;
                String searchString = ignoreSymbols.matcher(search.toLowerCase()).replaceAll("");

                firstSchematic = null;

                for(Schematic s : schematics.all()){
                    //make sure *tags* fit
                    if(selectedTags.any() && !s.labels.containsAll(selectedTags)) continue;
                    //make sure search fits
                    if(!search.isEmpty() && !ignoreSymbols.matcher(s.name().toLowerCase()).replaceAll("").contains(searchString)) continue;

                    if(Core.settings.getBool("autoSelSchematic") && control.input.block!=null && !s.containsBlock(control.input.block)) continue;
                    if (Core.settings.getBool("arcSchematicCanBuild") && !arcSchematicCanBuild(s)) continue;
                    if(firstSchematic == null) firstSchematic = s;

                    Button[] sel = {null};
                    sel[0] = t.button(b -> {
                        b.top();
                        b.margin(0f);
                        b.table(buttons -> {
                            buttons.left();
                            buttons.defaults().size(50f);

                            ImageButtonStyle style = Styles.emptyi;

                            buttons.button(Icon.info, style, () -> showInfo(s)).tooltip("@info.title");
                            buttons.button(Icon.upload, style, () -> showExport(s)).tooltip("@editor.export");
                            buttons.button(Icon.pencil, style, () -> showEdit(s)).tooltip("@schematic.edit");

                            if(s.hasSteamID()){
                                buttons.button(Icon.link, style, () -> platform.viewListing(s)).tooltip("@view.workshop");
                            }else{
                                buttons.button(Icon.trash, style, () -> {
                                    if(s.mod != null){
                                        ui.showInfo(Core.bundle.format("mod.item.remove", s.mod.meta.displayName));
                                    }else{
                                        ui.showConfirm("@confirm", "@schematic.delete.confirm", () -> {
                                            schematics.remove(s);
                                            rebuildPane.run();
                                        });
                                    }
                                }).tooltip("@save.delete");
                            }
                        }).growX().height(50f);
                        b.row();
                        b.stack(new SchematicImage(s).setScaling(Scaling.fit), new Table(n -> {
                            n.top();
                            n.table(Styles.black3, c -> {
                                Label label = c.add(s.name()).style(Styles.outlineLabel).color(Color.white).top().growX().maxWidth(200f - 8f).get();
                                label.setEllipsis(true);
                                label.setAlignment(Align.center);
                            }).growX().margin(1).pad(4).maxWidth(Scl.scl(200f - 8f)).padBottom(0);
                        })).size(200f);
                    }, () -> {
                        if(sel[0].childrenPressed()) return;
                        if(state.isMenu()){
                            showInfo(s);
                        }else{
                            if(!state.rules.schematicsAllowed){
                                ui.showInfo("@schematic.disabled");
                            }else{
                                control.input.useSchematic(s);
                                hide();
                            }
                        }
                    }).pad(4).style(Styles.flati).get();

                    sel[0].getStyle().up = Tex.pane;

                    if(++i % cols == 0){
                        t.row();
                    }
                }

                if(firstSchematic == null){
                    if(!searchString.isEmpty() || selectedTags.any()){
                        t.add("@none.found");
                    }else{
                        t.add("@none").color(Color.lightGray);
                    }
                }
            };

            rebuildPane.run();
        }).grow().scrollX(false);

        if(Core.settings.getBool("autoSelSchematic") && control.input.block!=null){
            arcui.arcInfo("[orange]蓝图筛选模式[white]:蓝图必须包含 "+control.input.block.emoji(),5f);
        }
    }

    public void showInfo(Schematic schematic){
        info.show(schematic);
    }

    public void showImport(){
        BaseDialog dialog = new BaseDialog("@editor.import");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButtonStyle style = Styles.flatt;
                t.defaults().size(280f, 60f).left();
                t.row();
                t.button("@schematic.copy.import", Icon.copy, style, () -> {
                    dialog.hide();
                    try{
                        Schematic s = Schematics.readBase64(Core.app.getClipboardText());
                        s.removeSteamID();
                        schematics.add(s);
                        setup();
                        ui.showInfoFade("@schematic.saved");
                        checkTags(s);
                        showInfo(s);
                    }catch(Throwable e){
                        ui.showException(e);
                    }
                }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null || !Core.app.getClipboardText().startsWith(schematicBaseStart));
                t.row();
                t.button("@schematic.importfile", Icon.download, style, () -> platform.showFileChooser(true, schematicExtension, file -> {
                    dialog.hide();

                    try{
                        Schematic s = Schematics.read(file);
                        s.removeSteamID();
                        schematics.add(s);
                        setup();
                        showInfo(s);
                        checkTags(s);
                    }catch(Exception e){
                        ui.showException(e);
                    }
                })).marginLeft(12f);
                t.row();
                if(steam){
                    t.button("@schematic.browseworkshop", Icon.book, style, () -> {
                        dialog.hide();
                        platform.openWorkshop();
                    }).marginLeft(12f);
                }
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }

    public void showExport(Schematic s){
        BaseDialog dialog = new BaseDialog("@editor.export");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButtonStyle style = Styles.flatt;
                t.defaults().size(280f, 60f).left();
                if(steam && !s.hasSteamID()){
                    t.button("@schematic.shareworkshop", Icon.book, style,
                        () -> platform.publish(s)).marginLeft(12f);
                    t.row();
                    dialog.hide();
                }
                t.button("@schematic.exportfile", Icon.export, style, () -> {
                    dialog.hide();
                    platform.export(s.name(), schematicExtension, file -> Schematics.write(s, file));
                }).marginLeft(12f);
                t.row();
                t.button("[cyan]剪贴板[white]/[gray]消息框", Icon.copy, style, () -> {
                    clipbroad = !clipbroad;
                }).marginLeft(12f).update(button -> button.setText(clipbroad? "[cyan]剪贴板[white]/[gray]消息框" : "[gray]剪贴板[white]/[cyan]消息框"));
                t.row();

                t.button("蓝图代码", Icon.copy, style, () -> {
                    dialog.hide();
                    arcSendBlueprintMsg(schematics.writeBase64(s));
                }).marginLeft(12f);
                t.row();
                t.button("记录蓝图[cyan][简]", Icon.export, style, () -> {
                    dialog.hide();
                    arcSendBlueprintMsg(arcSchematicsInfo(s,false));
                }).marginLeft(12f);
                t.row();
                t.button("记录蓝图[cyan][详]", Icon.export, style, () -> {
                    dialog.hide();
                    arcSendBlueprintMsg(arcSchematicsInfo(s,true));
                }).marginLeft(12f);
                t.row();
                t.button("分享蓝图", Icon.export, style, () -> {
                    try {
                        Http.HttpRequest req = Http.post("https://pastebin.com/api/api_post.php", "api_dev_key=sdBDjI5mWBnHl9vBEDMNiYQ3IZe0LFEk&api_option=paste&api_paste_expire_date=10M&api_paste_code=" + schematics.writeBase64(s));
                        req.submit(r -> {
                            String code = r.getResultAsString();
                            if (clipbroad) arcSendClipBroadMsg(s, code);
                            else RFuncs.sendChatMsg(getPrefix("blue", "Schem") + " " + code.substring(code.lastIndexOf('/') + 1));
                        });
                        req.error(e -> Core.app.post(() -> {ui.showException("分享失败", e);if (clipbroad) arcSendClipBroadMsg(s, "x");}));
                    } catch (Exception e) {
                        ui.showException("分享失败", e);
                    }
                    dialog.hide();
                }).marginLeft(12f);
            });
        });
        dialog.addCloseButton();
        dialog.show();
    }

    private void arcSendBlueprintMsg(String msg) {
        if (clipbroad) Core.app.setClipboardText(msg);
        else RFuncs.sendChatMsg(msg);
        arcui.arcInfo(clipbroad ? "已保存至剪贴板" : "已发送到聊天框");
    }

    private void arcSendClipBroadMsg(Schematic schem, String msg){
        StringBuilder s = new StringBuilder();
        s.append("这是一条来自").append(ARCVars.arcVersionPrefix).append("的分享记录\n");
        s.append("分享者：").append(player.name).append("\n");
        s.append("蓝图代码链接：").append(msg).append("\n");
        s.append("蓝图名：").append(schem.name()).append("\n");
        s.append("蓝图造价：");
        ItemSeq arr = schem.requirements();
        for(ItemStack stack : arr){
            s.append(stack.item.localizedName).append(stack.amount).append("|");
        }
        s.append("\n").append("电力：");
        float cons = schem.powerConsumption() * 60, prod = schem.powerProduction() * 60;
        if(!Mathf.zero(prod)){
            s.append("+").append(Strings.autoFixed(prod, 2));
            if(!Mathf.zero(cons)){
                s.append("|");
            }
        }
        if(!Mathf.zero(cons)){
            s.append("-").append(Strings.autoFixed(cons, 2));
        }
        if (schematics.writeBase64(schem).length() > 3500) s.append("\n").append("蓝图代码过长，请点击链接查看");
        else s.append("\n").append("蓝图代码：\n").append(schematics.writeBase64(schem));
        Core.app.setClipboardText(Strings.stripColors(s.toString()));
        arcui.arcInfo("已保存至剪贴板");
    }

    public boolean resolveSchematic(String msg, @Nullable Player sender) {
        if ((!msg.contains(ShareType)) || (!MessageDialog.arcMsgType.schematic.show)) {
            return false;
        }
        int start = msg.indexOf(' ', msg.indexOf(ShareType) + ShareType.length());
        Http.get("https://pastebin.com/raw/" + msg.substring(start + 1), r -> readShare(r.getResultAsString().replace(" ", "+"), sender));
        return true;
    }

    private void readShare(String base64, @Nullable Player sender) {
        Core.app.post(() -> {
            try {
                Schematic s = Schematics.readBase64(base64);
                s.removeSteamID();
                s.tags.put("name", sender == null ? "来自服务器的蓝图" : "来自" + sender.plainName() + "的蓝图");
                fromShare = true;
                SchematicsDialog.this.showInfo(s);
            } catch(Throwable e) {
                ui.showException(e);
            }
        });
    }

    private String arcSchematicsInfo(Schematic schem, boolean description){
        String builder = ARCVars.arcVersionPrefix;
        builder+="标记了蓝图["+schem.name()+"]";
        builder+="。属性："+schem.width+"x"+schem.height+"，"+schem.tiles.size+"个建筑。";
        if(description){
            builder+="耗材：";
            ItemSeq arr = schem.requirements();
            for(ItemStack s : arr){
                builder+=s.item.emoji()+ s.amount+"|";
            }

            builder+="。电力：";
            cont.row();
            float cons = schem.powerConsumption() * 60, prod = schem.powerProduction() * 60;
            if(!Mathf.zero(prod)){
                builder+="+"+ Strings.autoFixed(prod, 2);
                if(!Mathf.zero(cons)){
                    builder+="|";
                }
            }
            if(!Mathf.zero(cons)){
                builder+="-"+ Strings.autoFixed(cons, 2);
            }
        }
        return builder;
    }


    public void showEdit(Schematic s){
        new BaseDialog("@schematic.edit"){{
            setFillParent(true);
            addCloseListener();

            cont.margin(30);

            cont.add("@schematic.tags").padRight(6f);
            cont.table(tags -> buildTags(s, tags, false)).maxWidth(400f).fillX().left().row();

            cont.margin(30).add("@name").padRight(6f);
            TextField nameField = cont.field(s.name(), null).size(400f, 55f).left().get();

            cont.row();

            cont.margin(30).add("@editor.description").padRight(6f);
            TextField descField = cont.area(s.description(), Styles.areaField, t -> {}).size(400f, 140f).left().get();

            Runnable accept = () -> {
                s.tags.put("name", nameField.getText());
                s.tags.put("description", descField.getText());
                s.save();
                hide();
                rebuildPane.run();
            };

            buttons.defaults().size(210f, 64f).pad(4);
            buttons.button("@ok", Icon.ok, accept).disabled(b -> nameField.getText().isEmpty());
            buttons.button("@cancel", Icon.cancel, this::hide);

            keyDown(KeyCode.enter, () -> {
                if(!nameField.getText().isEmpty() && Core.scene.getKeyboardFocus() != descField){
                    accept.run();
                }
            });
        }}.show();
    }

    //adds all new tags to the global list of tags
    //alternatively, unknown tags could be discarded on import?
    void checkTags(){
        ObjectSet<String> encountered = new ObjectSet<>();
        encountered.addAll(tags);
        for(Schematic s : schematics.all()){
            for(var tag : s.labels){
                if(encountered.add(tag)){
                    tags.add(tag);
                }
            }
        }
    }

    //adds any new tags found to the global tag list
    //TODO remove tags from it instead?
    void checkTags(Schematic s){
        boolean any = false;
        for(var tag : s.labels){
            if(!tags.contains(tag)){
                tags.add(tag);
                any = true;
            }
        }
        if(any){
            rebuildTags.run();
        }
    }

    void tagsChanged(){
        rebuildTags.run();
        if(selectedTags.any()){
            rebuildPane.run();
        }

        Core.settings.putJson("schematic-tags", String.class, tags);
    }

    void addTag(Schematic s, String tag){
        s.labels.add(tag);
        s.save();
        tagsChanged();
    }

    void removeTag(Schematic s, String tag){
        s.labels.remove(tag);
        s.save();
        tagsChanged();
    }

    //shows a dialog for creating a new tag
    void showNewTag(Cons<String> result){
        ui.showTextInput("@schematic.addtag", "", "", out -> {
            if(tags.contains(out)){
                ui.showInfo("@schematic.tagexists");
            }else{
                tags.add(out);
                tagsChanged();
                result.get(out);
            }
        });
    }

    void showNewIconTag(Cons<String> cons){
        new Dialog(){{
            closeOnBack();
            setFillParent(true);

            cont.pane(t -> {
                resized(true, () -> {
                    t.clearChildren();
                    t.marginRight(19f).marginLeft(12f);
                    t.defaults().size(48f);

                    int cols = (int)Math.min(20, Core.graphics.getWidth() / Scl.scl(52f));

                    int i = 0;
                    for(String icon : PlanetDialog.defaultIcons){
                        String out = (char)Iconc.codes.get(icon) + "";
                        if(tags.contains(out)) continue;

                        t.button(Icon.icons.get(icon), Styles.flati, iconMed, () -> {
                            tags.add(out);
                            tagsChanged();
                            cons.get(out);
                            hide();
                        });

                        if(++i % cols == 0) t.row();
                    }

                    for(ContentType ctype : defaultContentIcons){
                        var all = content.getBy(ctype).<UnlockableContent>as().select(u -> !u.isHidden() && u.unlockedNow() && u.hasEmoji());

                        t.row();
                        if(all.count(u -> !tags.contains(u.emoji())) > 0) t.image().colspan(cols).growX().width(Float.NEGATIVE_INFINITY).height(3f).color(Pal.accent);
                        t.row();

                        i = 0;
                        for(UnlockableContent u : all){
                            if(tags.contains(u.emoji())) continue;
                            t.button(new TextureRegionDrawable(u.uiIcon), Styles.flati, iconMed, () -> {
                                String out = u.emoji() + "";

                                tags.add(out);
                                tagsChanged();
                                cons.get(out);

                                hide();
                            }).tooltip(u.localizedName);

                            if(++i % cols == 0) t.row();
                        }
                    }
                });
            }).scrollX(false);
            buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
        }}.show();
    }

    void showAllTags(){
        var dialog = new BaseDialog("@schematic.edittags");
        dialog.addCloseButton();
        Runnable[] rebuild = {null};
        dialog.cont.pane(p -> {
            rebuild[0] = () -> {
                p.clearChildren();
                p.margin(12f).defaults().fillX().left();

                float sum = 0f;
                Table current = new Table().left();

                for(var tag : tags){

                    var next = new Table(Tex.whiteui, n -> {
                        n.setColor(Pal.gray);
                        n.margin(5f);

                        n.table(move -> {

                            //move up
                            move.button(Icon.upOpen, Styles.emptyi, () -> {
                                int idx = tags.indexOf(tag);
                                if(idx > 0){
                                    tags.swap(idx, idx - 1);
                                    tagsChanged();
                                    rebuild[0].run();
                                }
                            }).tooltip("@editor.moveup").row();
                            //move down
                            move.button(Icon.downOpen, Styles.emptyi, () -> {
                                int idx = tags.indexOf(tag);
                                if(idx < tags.size - 1){
                                    tags.swap(idx, idx + 1);
                                    tagsChanged();
                                    rebuild[0].run();
                                }
                            }).tooltip("@editor.movedown");
                        }).fillY();

                        n.table(t -> {
                            t.add(tag).left().row();
                            t.add(Core.bundle.format("schematic.tagged", schematics.all().count(s -> s.labels.contains(tag)))).left()
                            .update(b -> b.setColor(b.hasMouse() ? Pal.accent : Color.lightGray)).get().clicked(() -> {
                                dialog.hide();
                                selectedTags.clear().add(tag);
                                rebuildTags.run();
                                rebuildPane.run();
                            });
                        }).growX().fillY();

                        n.table(b -> {
                            b.margin(2);

                            //rename tag
                            b.button(Icon.pencil, Styles.emptyi, () -> {
                                ui.showTextInput("@schematic.renametag", "@name", tag, result -> {
                                    //same tag, nothing was renamed
                                    if(result.equals(tag)) return;

                                    if(tags.contains(result)){
                                        ui.showInfo("@schematic.tagexists");
                                    }else{
                                        for(Schematic s : schematics.all()){
                                            if(s.labels.any()){
                                                s.labels.replace(tag, result);
                                                s.save();
                                            }
                                        }
                                        selectedTags.replace(tag, result);
                                        tags.replace(tag, result);
                                        tagsChanged();
                                        rebuild[0].run();
                                    }
                                });
                            }).tooltip("@schematic.renametag").row();
                            //delete tag
                            b.button(Icon.trash, Styles.emptyi, () -> {
                                ui.showConfirm("@schematic.tagdelconfirm", () -> {
                                    for(Schematic s : schematics.all()){
                                        if(s.labels.any()){
                                            s.labels.remove(tag);
                                            s.save();
                                        }
                                    }
                                    selectedTags.remove(tag);
                                    tags.remove(tag);
                                    tagsChanged();
                                    rebuildPane.run();
                                    rebuild[0].run();
                                });
                            }).tooltip("@save.delete");
                        }).fillY();
                    });

                    next.pack();
                    float w = next.getWidth() + Scl.scl(6f);

                    if(w*2f + sum >= Core.graphics.getWidth() * 0.8f){
                        p.add(current).row();
                        current = new Table();
                        current.left();
                        current.add(next).minWidth(240).pad(4);
                        sum = 0;
                    }else{
                        current.add(next).minWidth(240).pad(4);
                    }

                    sum += w;
                }

                if(sum > 0){
                    p.add(current).row();
                }

                p.table(t -> {
                    t.left().defaults().fillX().height(tagh).pad(2);

                    t.button("@schematic.texttag", Icon.add, () -> showNewTag(res -> rebuild[0].run())).wrapLabel(false).get().getLabelCell().padLeft(5);
                    t.button("@schematic.icontag", Icon.add, () -> showNewIconTag(res -> rebuild[0].run())).wrapLabel(false).get().getLabelCell().padLeft(5);
                });
                p.row();
                p.table(t ->{
                    t.left().defaults().fillX().height(tagh).pad(2);
                    t.button("自动标签", Icon.add, () -> arcAutoTags(res -> rebuild[0].run())).wrapLabel(false).get().getLabelCell().padLeft(5);
                });

            };

            resized(true, rebuild[0]);
        }).scrollX(false);
        dialog.show();
    }

    void arcAutoTags(Cons<String> cons){
        new Dialog(){{
            closeOnBack();
            setFillParent(true);

            cont.pane(t -> {
                resized(true, () -> {
                    t.clearChildren();
                    t.marginRight(19f);
                    t.defaults().size(48f);

                    int cols = (int)Math.min(20, Core.graphics.getWidth() / Scl.scl(52f));

                    for(ContentType ctype : defaultContentIcons){
                        t.row();
                        t.image().colspan(cols).growX().width(Float.NEGATIVE_INFINITY).height(3f).color(Pal.accent);
                        t.row();

                        int i = 0;
                        for(UnlockableContent u : content.getBy(ctype).<UnlockableContent>as()){
                            if(!u.isHidden() && u.unlockedNow() && u.hasEmoji() && !tags.contains(u.emoji())){
                                t.button(new TextureRegionDrawable(u.uiIcon), Styles.flati, iconMed, () -> {
                                    String out = u.emoji() + "";

                                    tags.add(out);
                                    tagsChanged();

                                    if(u instanceof Block block){
                                        for(Schematic s : schematics.all()){
                                            s.tiles.each(sBlock -> {
                                                if(sBlock.block == block){
                                                    addTag(s,out);
                                                    cons.get(out);
                                                    hide();
                                                }
                                            });
                                        }
                                    }
                                    else if(u instanceof Item item){
                                        Seq<Block> blocklist = new Seq<>();
                                        for (Block factory : content.blocks()) {
                                            if(factory instanceof GenericCrafter crafter){
                                                if(crafter.outputItems == null) continue;
                                                for(ItemStack stack:crafter.outputItems){
                                                if (stack.item == item) blocklist.add(factory);
                                                }
                                            }
                                        }
                                        for(Schematic s : schematics.all()){
                                            s.tiles.each(sBlock -> {
                                                if(blocklist.contains(sBlock.block)){
                                                    addTag(s,out);
                                                    cons.get(out);
                                                    hide();
                                                }
                                            });
                                        }
                                    }
                                    else if(u instanceof Liquid liquid){
                                        Seq<Block> blocklist = new Seq<>();
                                        for (Block factory : content.blocks()) {
                                            if(factory instanceof GenericCrafter crafter){
                                                if(crafter.outputLiquids==null) continue;
                                                for(LiquidStack stack: crafter.outputLiquids){
                                                    if (stack.liquid == liquid) blocklist.add(factory);
                                                }
                                            }
                                        }
                                        for(Schematic s : schematics.all()){
                                            s.tiles.each(sBlock -> {
                                                if(blocklist.contains(sBlock.block)){
                                                    addTag(s,out);
                                                    cons.get(out);
                                                    hide();
                                                }
                                            });
                                        }
                                    }


                                    cons.get(out);

                                    hide();
                                });

                                if(++i % cols == 0) t.row();
                            }
                        }
                    }
                });
            });
            buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
        }}.show();
    }

    void syncPlanetTags(){
        arcui.arcInfo("标签自动分类中...请稍后");
        for(Schematic s : schematics.all()){
            Boolean surpulo = true;
            Boolean erekir = true;
            for (Item item:erekirOnlyItems){
                if(s.requirements().has(item)) {
                    surpulo = false;
                    break;
                }
            }
            for(Item item : serpuloItems.copy().removeAll(erekirItems::contains)){
                if(s.requirements().has(item)) {
                    erekir = false;
                    break;
                }
            }

            if(surpulo && !s.labels.contains(surpuloTags)) addTag(s,surpuloTags);
            if(erekir && !s.labels.contains(erekirTags)) addTag(s,erekirTags);
        }
        arcui.arcInfo("标签分类完成");
    }

    boolean arcSchematicCanBuild(Schematic s){
        for (ItemStack item : s.requirements()){
            if (!ui.hudfrag.coreItems.hadItem(item.item)) return false;
        }
        for (Block block: state.rules.bannedBlocks){
            if (s.containsBlock(block)) return false;
        }
        return true;
    }

    void buildTags(Schematic schem, Table t){
        buildTags(schem, t, true);
    }

    void buildTags(Schematic schem, Table t, boolean name){
        t.clearChildren();
        t.left();

        //sort by order in the main target array. the complexity of this is probably awful
        schem.labels.sort(s -> tags.indexOf(s));

        if(name) t.add("@schematic.tags").padRight(4);
        t.pane(s -> {
            s.left();
            s.defaults().pad(3).height(tagh);
            for(var tag : schem.labels){
                s.table(Tex.button, i -> {
                    i.add(tag).padRight(4).height(tagh).labelAlign(Align.center);
                    i.button(Icon.cancelSmall, Styles.emptyi, () -> {
                        removeTag(schem, tag);
                        buildTags(schem, t, name);
                    }).size(tagh).padRight(-9f).padLeft(-9f);
                });
            }

        }).fillX().left().height(tagh).scrollY(false);

        t.button(Icon.addSmall, () -> {
            var dialog = new BaseDialog("@schematic.addtag");
            dialog.addCloseButton();
            dialog.cont.pane(p -> resized(true, () -> {
                p.clearChildren();
                p.defaults().fillX().left();

                float sum = 0f;
                Table current = new Table().left();
                for(var tag : tags){
                    if(schem.labels.contains(tag)) continue;

                    var next = Elem.newButton(tag, () -> {
                        addTag(schem, tag);
                        buildTags(schem, t, name);
                        dialog.hide();
                    });
                    next.getLabel().setWrap(false);

                    next.pack();
                    float w = next.getPrefWidth() + Scl.scl(6f);

                    if(w + sum >= Core.graphics.getWidth() * (Core.graphics.isPortrait() ? 1f : 0.8f)){
                        p.add(current).row();
                        current = new Table();
                        current.left();
                        current.add(next).height(tagh).pad(2);
                        sum = 0;
                    }else{
                        current.add(next).height(tagh).pad(2);
                    }

                    sum += w;
                }

                if(sum > 0){
                    p.add(current).row();
                }

                Cons<String> handleTag = res -> {
                    dialog.hide();
                    addTag(schem, res);
                    buildTags(schem, t, name);
                };

                p.row();

                p.table(v -> {
                    v.left().defaults().fillX().height(tagh).pad(2);
                    v.button("@schematic.texttag", Icon.add, () -> showNewTag(handleTag)).wrapLabel(false).get().getLabelCell().padLeft(4);
                    v.button("@schematic.icontag", Icon.add, () -> showNewIconTag(handleTag)).wrapLabel(false).get().getLabelCell().padLeft(4);
                });
            }));
            dialog.show();
        }).size(tagh).tooltip("@schematic.addtag");
    }

    @Override
    public Dialog show(){
        super.show();

        if(Core.app.isDesktop() && searchField != null){
            Core.scene.setKeyboardFocus(searchField);
        }

        return this;
    }

    public static class SchematicImage extends Image{
        public float scaling = 16f;
        public float thickness = 4f;
        public Color borderColor = Pal.gray;

        private Schematic schematic;
        private Texture lastTexture;
        boolean set;

        public SchematicImage(Schematic s){
            super(Tex.clear);
            setScaling(Scaling.fit);
            schematic = s;

            if(schematics.hasPreview(s)){
                setPreview();
                set = true;
            }
        }

        @Override
        public void draw(){
            boolean checked = parent.parent instanceof Button
                && ((Button)parent.parent).isOver();

            boolean wasSet = set;
            if(!set){
                Core.app.post(this::setPreview);
                set = true;
            }else if(lastTexture != null && lastTexture.isDisposed()){
                set = wasSet = false;
            }

            Texture background = Core.assets.get("sprites/schematic-background.png", Texture.class);
            TextureRegion region = Draw.wrap(background);
            float xr = width / scaling;
            float yr = height / scaling;
            region.setU2(xr);
            region.setV2(yr);
            Draw.color();
            Draw.alpha(parentAlpha);
            Draw.rect(region, x + width/2f, y + height/2f, width, height);

            if(wasSet){
                super.draw();
            }else{
                Draw.rect(Icon.refresh.getRegion(), x + width/2f, y + height/2f, width/4f, height/4f);
            }

            Draw.color(checked ? Pal.accent : borderColor);
            Draw.alpha(parentAlpha);
            Lines.stroke(Scl.scl(thickness));
            Lines.rect(x, y, width, height);
            Draw.reset();
        }

        private void setPreview(){
            TextureRegionDrawable draw = new TextureRegionDrawable(new TextureRegion(lastTexture = schematics.getPreview(schematic)));
            setDrawable(draw);
            setScaling(Scaling.fit);
        }
    }

    public class SchematicInfoDialog extends BaseDialog{

        SchematicInfoDialog(){
            super("");
            setFillParent(true);
            addCloseListener();
        }

        public void show(Schematic schem){
            cont.clear();
            title.setText("[[" + Core.bundle.get("schematic") + "] " +schem.name());

            cont.add(Core.bundle.format("schematic.info", schem.width, schem.height, schem.tiles.size)).color(Color.lightGray).row();
            cont.table(tags -> buildTags(schem, tags)).fillX().left().row();
            cont.add(new SchematicImage(schem)).maxSize(800f).row();

            ItemSeq arr = schem.requirements();
            cont.table(r -> {
                int i = 0;
                for(ItemStack s : arr){
                    r.image(s.item.uiIcon).left().size(iconMed);
                    r.label(() -> {
                        Building core = player.core();
                        if(core == null || state.isMenu() || state.rules.infiniteResources || core.items.has(s.item, s.amount)) return "[lightgray]" + s.amount + "";
                        return (core.items.has(s.item, s.amount) ? "[lightgray]" : "[scarlet]") + Math.min(core.items.get(s.item), s.amount) + "[lightgray]/" + s.amount;
                    }).padLeft(2).left().padRight(4);

                    if(++i % 4 == 0){
                        r.row();
                    }
                }
            });
            cont.row();
            float cons = schem.powerConsumption() * 60, prod = schem.powerProduction() * 60;
            if(!Mathf.zero(cons) || !Mathf.zero(prod)){
                cont.table(t -> {

                    if(!Mathf.zero(prod)){
                        t.image(Icon.powerSmall).color(Pal.powerLight).padRight(3);
                        t.add("+" + Strings.autoFixed(prod, 2)).color(Pal.powerLight).left();

                        if(!Mathf.zero(cons)){
                            t.add().width(15);
                        }
                    }

                    if(!Mathf.zero(cons)){
                        t.image(Icon.powerSmall).color(Pal.remove).padRight(3);
                        t.add("-" + Strings.autoFixed(cons, 2)).color(Pal.remove).left();
                    }
                });
            }
            cont.row();

            schem.calProduction();
            cont.table(r -> {
                int i = 0;
                for(Item item : schem.items.keys()){
                    r.image(item.uiIcon).left().size(iconMed);
                    r.label(
                            () -> (schem.items.get(item, 0) > 0 ? "+" : "") + Strings.autoFixed(schem.items.get(item, 0), 2) + StatUnit.perSecond.localized()
                    ).padLeft(2).left().padRight(5).color(Color.lightGray);
                    if(++i % 4 == 0){
                        r.row();
                    }
                }
                for (Liquid liquid : schem.liquids.keys()) {
                    r.image(liquid.uiIcon).left().size(iconMed);
                    r.label(
                            () -> (schem.liquids.get(liquid, 0) > 0 ? "+" : "") + Strings.autoFixed(schem.liquids.get(liquid, 0), 2) + StatUnit.perSecond.localized()
                    ).padLeft(2).left().padRight(5).color(Color.lightGray);
                    if(++i % 4 == 0){
                        r.row();
                    }
                }
            });
            buttons.clearChildren();
            buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
            buttons.button("@back", Icon.left, this::hide);
            buttons.button("@editor.export", Icon.upload, () -> showExport(schem));
            buttons.button("@edit", Icon.edit, () -> showEdit(schem));
            if (fromShare) {
                fromShare = false;
                buttons.button("@save", Icon.save, () -> {
                    schematics.add(schem);
                    setup();
                    ui.showInfoFade("@schematic.saved");
                    checkTags(schem);
                });
            }
            show();
        }
    }
}