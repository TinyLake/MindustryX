package mindustryX.features.ui;

import arc.*;
import arc.flabel.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.mod.Mods.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;
import mindustryX.*;
import mindustryX.features.*;
import mindustryX.features.func.*;
import mindustryX.features.ui.comp.*;

import static mindustry.Vars.*;
import static mindustryX.features.UIExt.i;

public class UnitFactoryDialog extends BaseDialog{
    public static final int maxCount = 50;

    UnitStack selected = UnitStack.vanillaStack;

    private int unitCount = 1;
    private float unitRandDst = 4;
    /** 仅作为部分数据的载体 */
    private Unit spawnUnit = UnitTypes.emanate.create(Team.sharded);
    /** 载荷独立存储 */
    private final Seq<Payload> unitPayloads = new Seq<>();

    private boolean lookingLocation;
    private Table selection, infoTable, posTable, countTable, itemTable, propertiesTable, teamTable, effectTable, payloadTable;

    public UnitFactoryDialog(){
        super(i("单位工厂"));
        getCell(cont).setElement(new ScrollPane(cont));
        closeOnBack();
        addCloseButton();
        //Lazy build
        shown(() -> {
            if(cont.hasChildren()) return;
            setup();
        });
    }

    private void setup(){
        cont.top();

        selection = new Table();
        infoTable = new Table();
        posTable = new Table();
        countTable = new Table();
        itemTable = new Table();
        propertiesTable = new Table();
        teamTable = new Table();
        effectTable = new Table();
        payloadTable = new Table();

        setupCountTable();
        setupPosTable();
        rebuildTables();

        buttons.button(i("重置"), Icon.refresh, () -> {
            resetUnit(spawnUnit);
            rebuildTables();
        });
        buttons.button(i("[orange]生成！"), Icon.modeAttack, this::spawn);

        rebuild();

        resized(this::rebuild);

        Events.run(Trigger.draw, () -> {
            if(!lookingLocation){
                return;
            }

            Draw.z(Layer.overlayUI);
            Draw.color(spawnUnit.team.color);
            Lines.circle(spawnUnit.x, spawnUnit.y, 10);
            Draw.rect(spawnUnit.type.uiIcon, spawnUnit.x, spawnUnit.y, 10, 10, Time.time % 360);
            Draw.reset();

            Vec2 mouse = Core.input.mouseWorld();
            Tile tile = world.tileWorld(mouse.x, mouse.y);
            if(tile != null){
                Drawf.dashRect(Pal.accent, tile.drawx() - tilesize / 2f, tile.drawy() - tilesize / 2f, tilesize, tilesize);

                Vec2 tilePos = Tmp.v1.set(tile.x, tile.y);
                Vec2 textPos = Tmp.v2.set(tile).sub(0, tilesize);
                FuncX.drawText(textPos, "" + tilePos, 1f, Pal.accent, Align.top, Fonts.outline);
                Draw.reset();
            }
        });
    }

    private void spawn(){
        for(int n = 0; n < unitCount; n++){
            Unit unit = cloneUnit(spawnUnit);

            Vec2 pos = Tmp.v1.set(unit);
            Vec2 offset = Tmp.v2.rnd(Mathf.random(unitRandDst * tilesize));
            unit.set(pos.add(offset));

            unit.add();
        }
    }

    private void rebuild(){
        float rightWidth = Math.min(Core.scene.getWidth() * (Core.scene.getWidth() > 700 ? 0.6f : 0.9f) / Scl.scl(), 700);

        Table rightTable = new Table();
        rightTable.top();
        rightTable.defaults().growX();

        rightTable.add(infoTable).row();
        rightTable.add().height(8f).row();
        rightTable.table(Tex.pane, settings -> {
            settings.left().defaults().fillX().padTop(8f);
            settings.add(posTable).padTop(0f).row();
            settings.add(countTable).row();
            settings.table(randDstTable -> {
                randDstTable.add(i("生成范围："));
                randDstTable.field(Strings.autoFixed(unitRandDst, 3), text -> unitRandDst = Float.parseFloat(text)).valid(Strings::canParsePositiveFloat).tooltip(i("在目标点附近的这个范围内随机生成")).maxTextLength(6).padLeft(4f);
                randDstTable.add(i("格")).expandX().left();
            }).row();
            settings.add(itemTable).row();
            settings.add(propertiesTable).row();
            settings.add(teamTable).row();
        }).row();
        rightTable.add().height(8f).row();
        rightTable.add(effectTable).row();
        rightTable.add().height(8f).row();
        rightTable.add(payloadTable).row();


        Table main = cont;
        main.clearChildren();
        if(Core.graphics.isPortrait()){
            main.add(selection).growX().row();
            main.add(rightTable).padTop(8f).growX().row();
        }else{
            main.add(selection).width(rightWidth * 0.6f).growY();
            main.add(rightTable).padLeft(8f).width(rightWidth).growY().row();
        }

        Core.app.post(this::rebuildUnitSelection);
    }

    private void rebuildTables(){
        rebuildInfoTable(spawnUnit, infoTable);
        rebuildPropertiesTable(spawnUnit, propertiesTable);
        rebuildItemTable(spawnUnit, itemTable);
        rebuildTeamTable(spawnUnit, teamTable);
        rebuildEffectsTable(spawnUnit, effectTable);
        rebuildPayloadTable(spawnUnit, payloadTable);
    }

    private void setSpawnUnitType(UnitType unitType){
        spawnUnit.type = unitType;
        spawnUnit = cloneUnit(spawnUnit);
        spawnUnit.health = unitType.health;
        spawnUnit.elevation = Mathf.num(unitType.flying);

        if(spawnUnit instanceof PayloadUnit payloadUnit){
            payloadUnit.payloads = unitPayloads;
        }

        rebuildTables();
    }

    private void rebuildUnitSelection(){
        selection.top();
        selection.clearChildren();

        Table unitSelectTable = new Table(Tex.whiteui);
        unitSelectTable.top().setColor(Pal.darkerGray);

        selection.table(Tex.whiteui, stackSelectTable -> {
            stackSelectTable.left();

            final int[] i = {0};
            float width = selection.getWidth();
            int rows = Math.max(1, (int)(width / (184f + 8f) / Scl.scl()));
            UnitStack.getUnitStacks().each(stack -> {
                stackSelectTable.button(b -> {
                    TextureRegion region = stack.icon();
                    Drawable icon = region == null ? Tex.nomap : new TextureRegionDrawable(region);

                    b.image(icon).scaling(Scaling.fit).size(48f).pad(8f);

                    b.add(stack.name()).width(128f);
                }, Styles.clearTogglei, () -> {
                    selected = stack;
                    rebuildSelectTable(selected, unitSelectTable);
                }).height(64f).pad(8f).checked(b -> selected == stack);

                if(++i[0] % rows == 0){
                    stackSelectTable.row();
                }
            });
        }).color(Pal.gray).fillX().row();

        selection.add(unitSelectTable).padLeft(8).padRight(8).grow().row();
        Core.app.post(() -> rebuildSelectTable(selected, unitSelectTable));
    }

    private void rebuildSelectTable(UnitStack stack, Table table){
        final int[] i = {0};
        float width = selection.getWidth();
        int rows = Math.max(1, (int)((width - 4f * 2) / (64f + 4f) / Scl.scl()) - 1);

        table.clearChildren();

        stack.units.each(unit -> {
            // block单位只会造成崩溃
            if(unit == UnitTypes.block) return;

            table.button(new TextureRegionDrawable(unit.uiIcon), Styles.clearTogglei, 32f, () -> setSpawnUnitType(unit)).margin(3).size(64f).pad(4f).tooltip(unit.localizedName).checked(b -> spawnUnit.type == unit);

            if(++i[0] % rows == 0){
                table.row();
            }
        });
    }

    private void setupPosTable(){
        posTable.add(i("生成位置:"));

        posTable.label(() -> {
            int tileX = World.toTile(spawnUnit.x);
            int tileY = World.toTile(spawnUnit.y);
            return "" + Tmp.v1.set(tileX, tileY);
        }).left().expandX().padLeft(4);

        posTable.defaults().size(48);
        posTable.button(Icon.pickSmall, Styles.clearNonei, () -> {
            lookingLocation = true;
            hide();

            UIExt.hitter((cx, cy) -> {
                Vec2 v = Core.camera.unproject(cx, cy);
                spawnUnit.set(v);

                // 给个时间反应一下
                Timer.schedule(() -> {
                    show();
                    lookingLocation = false;
                }, 0.5f);
                return true;
            });

            UIExt.announce(i("[green]点击屏幕采集坐标"), 2f);
        });

        posTable.button(Icon.eyeSmall, Styles.clearNonei, () -> {
            lookingLocation = true;
            hide();

            Core.camera.position.set(spawnUnit);
            if(control.input instanceof DesktopInput input){
                input.panning = true;
            }

            UIExt.hitter((cx, cy) -> {
                show();

                lookingLocation = false;
                return true;
            });

            UIExt.announce(i("[green]点击屏幕返回"), 2f);
        }).padLeft(4);

        posTable.button(new TextureRegionDrawable(UnitTypes.gamma.uiIcon), Styles.clearNonei, 24, () -> spawnUnit.set(player)).padLeft(4);
    }

    private void setupCountTable(){
        countTable.add(i("生成数量:"));
        countTable.field("", text -> unitCount = Math.min(maxCount, Strings.parseInt(text)))
        .update(it -> it.setText("" + unitCount)).left().expandX().width(80).valid(Strings::canParseInt);

        countTable.button(b -> b.add("MAX"), Styles.clearNonei, () -> unitCount = maxCount).size(48f).padLeft(4);
        countTable.slider(1, maxCount, 1, 1, n -> unitCount = (int)n)
        .update(it -> it.setValue(unitCount)).width(128).padLeft(4).get();
    }

    private void rebuildInfoTable(Unit unit, Table infoTable){
        infoTable.clearChildren();

        infoTable.table(Tex.pane, imageTable -> imageTable.image(unit.type.uiIcon).size(112).scaling(Scaling.fit)).top();

        infoTable.table(null, rightTable -> {
            rightTable.table(Tex.pane, labelTable -> {
                String name = unit.type.name;
                String localizedName = unit.type.localizedName;
                boolean hasLocalized = !name.equals(localizedName);
                String text = localizedName +
                (hasLocalized ? "(" + name + ")" : "") +
                "[" + unit.type.id + "]";

                Label label = labelTable.add(text).labelAlign(Align.left).left().expandX().get();

                labelTable.table(null, buttons -> {
                    buttons.button(Icon.copySmall, Styles.clearNonei, 16, () -> {
                        Core.app.setClipboardText("" + label.getText());

                        ui.announce(VarsX.bundle.copiedSuccessfully(String.valueOf(label.getText())), 3);
                    }).size(28);

                    buttons.button("i", () -> ui.content.show(unit.type)).size(28).padLeft(4);
                }).right();
            }).growX().row();

            rightTable.pane(Styles.noBarPane, describeTable -> {
                describeTable.background(Tex.pane);

                describeTable.label(() -> unit.type.description).labelAlign(Align.left).grow().wrap();
            }).grow().maxHeight(80).padTop(4).scrollX(false);
        }).grow().padLeft(4);
    }

    private void rebuildPropertiesTable(Unit unit, Table propertiesTable){
        propertiesTable.clearChildren();
        propertiesTable.defaults().expandX().left();

        propertiesTable.table(t -> t.check(i("飞行模式"), unit.elevation > 0, a -> unit.elevation = a ? 1 : 0).padBottom(5f).padRight(10f).tooltip(i("[orange]生成的单位会飞起来"), true)
        .checked(b -> unit.elevation > 0));

        propertiesTable.row();

        propertiesTable.table(healthTable -> {
            healthTable.add(i("[red]血量："));
            healthTable.field(Strings.autoFixed(unit.health, 1), text -> unit.health = Float.parseFloat(text)).valid(Strings::canParsePositiveFloat).padLeft(4f);
        });

        propertiesTable.table(shieldTable -> {
            shieldTable.add(i("[yellow]护盾："));
            shieldTable.field(Strings.autoFixed(unit.shield, 1), text -> unit.shield = Float.parseFloat(text)).valid(Strings::canParsePositiveFloat).padLeft(4f);
        });
    }

    private void rebuildItemTable(Unit unit, Table itemTable){
        itemTable.clearChildren();

        int itemCapacity = unit.itemCapacity();
        if(itemCapacity == 0) return;

        itemTable.add(i("携带物品:"));
        itemTable.button(b -> b.image(() -> unit.hasItem() ? unit.item().uiIcon : Icon.noneSmall.getRegion()).size(48f).scaling(Scaling.fit).padLeft(8f), Styles.clearNonei, () -> ContentSelectDialog.once(content.items(), unit.item(), item -> unit.stack.item = item)).size(48f).padLeft(8f);

        itemTable.field("" + unit.stack.amount, text -> unit.stack.amount = Mathf.clamp(Strings.parseInt(text), 0, itemCapacity))
        .update((it) -> it.setText("" + unit.stack.amount)).valid(Strings::canParsePositiveInt).padLeft(8f).expandX().left().width(80);
        itemTable.button(b -> b.add("MAX"), Styles.clearNonei, () -> unit.stack.amount = unit.itemCapacity()).size(48f).padLeft(4);
        itemTable.button(Icon.none, Styles.clearNonei, unit::clearItem).size(48f).padLeft(4);

        itemTable.slider(0, itemCapacity, 1, unit.stack.amount, n -> unit.stack.amount = (int)n)
        .update((s) -> s.setValue(unit.stack.amount)).width(128f).padLeft(4);
    }

    private void rebuildTeamTable(Unit unit, Table teamTable){
        teamTable.clearChildren();

        teamTable.add(i("生成队伍:"));
        teamTable.label(() -> {
            Team team = unit.team;
            return "[#" + team.color + "]" + team.localized();
        }).minWidth(88f).padLeft(8f);

        Image image = teamTable.image().color(unit.team.color).size(48).left().expandX().get();
        Cons<Team> changeTeam = team -> {
            unit.team = team;
            image.addAction(Actions.color(team.color, 0.25f));
        };

        for(Team team : Team.baseTeams){
            teamTable.button(b -> b.image().grow().color(team.color), Styles.clearNonei, () -> changeTeam.get(team)).size(36).pad(8);
        }

        teamTable.add(i("队伍ID:")).padLeft(4);
        teamTable.field("" + unit.team.id, text -> {
            int id = Mathf.clamp(Strings.parseInt(text), 0, Team.all.length - 1);
            changeTeam.get(Team.all[id]);
        }).update(it -> it.setText("" + unit.team.id)).width(72).valid(Strings::canParseInt).get();
    }

    private void rebuildEffectsTable(Unit unit, Table effectTable){
        effectTable.clearChildren();

        Seq<StatusEntry> unitStatus = unit.statuses();
        Table settingTable = new Table(Tex.whiteui), effectInfo = new Table(Styles.grayPanel);
        settingTable.setColor(Pal.gray);

        Runnable rebuildInfo = () -> {
            float[] status = {1f, 1f, 1f, 1f, 1f, 1f, -1f};

            for(StatusEntry entry : unitStatus){
                status[0] *= entry.damageMultiplier;
                status[1] *= entry.healthMultiplier;
                status[2] *= entry.speedMultiplier;
                status[3] *= entry.reloadMultiplier;
                status[4] *= entry.buildSpeedMultiplier;
                status[5] *= entry.dragMultiplier;
                if(entry.effect.dynamic){
                    status[6] = entry.armorOverride;
                }
            }

            effectInfo.clearChildren();
            effectInfo.defaults().pad(4f);

            effectInfo.add(i("[red]伤害"));
            effectInfo.add(FormatDefault.format(status[0])).expandX().right();

            effectInfo.add(i("[acid]血量")).padLeft(12f);
            effectInfo.add(FormatDefault.format(status[1])).expandX().right();
            effectInfo.row();

            effectInfo.add(i("[cyan]移速"));
            effectInfo.add(FormatDefault.format(status[2])).expandX().right();

            effectInfo.add(i("[violet]攻速")).padLeft(12f);
            effectInfo.add(FormatDefault.format(status[3])).expandX().right();
            effectInfo.row();

            effectInfo.add(i("[accent]建速"));
            effectInfo.add(FormatDefault.format(status[4])).expandX().right();

            effectInfo.add(i("[purple]阻力")).padLeft(12f);
            effectInfo.add(FormatDefault.format(status[5])).expandX().right();
            effectInfo.row();

            if(status[6] >= 0){
                effectInfo.add(i("[teal]装甲"));
                effectInfo.add(FormatDefault.format(status[6])).expandX().right();
            }
        };

        effectTable.table(Tex.whiteui, leftTable -> {
            leftTable.top();
            leftTable.setColor(Pal.gray);
            leftTable.defaults().fillY();

            leftTable.table(topTable -> {
                topTable.top();

                topTable.table(statusTable -> {
                    statusTable.image(StatusEffects.burning.uiIcon).size(64).scaling(Scaling.fit).expandX().left();

                    statusTable.button(Icon.refresh, Styles.cleari, 48f, () -> {
                        unitStatus.clear();
                        rebuildInfo.run();
                        rebuildEffectSettingTable(unitStatus, settingTable, rebuildInfo);
                    }).size(64f).pad(8f);
                }).growX();

                topTable.row();

                topTable.add(effectInfo).fill().pad(8f);
            });

            leftTable.row();

            leftTable.table(selection -> {
                int i = 0;
                for(StatusEffect effect : content.statusEffects()){
                    Cell<ImageButton> cell = selection.button(new TextureRegionDrawable(effect.uiIcon), Styles.cleari, 32f, () -> {
                        float time = unitStatus.isEmpty() ? 600f * 60 : unitStatus.peek().time;
                        unitStatus.add(new StatusEntry().set(effect, time));

                        rebuildInfo.run();
                        rebuildEffectSettingTable(unitStatus, settingTable, rebuildInfo);
                    }).size(48f).pad(4f).tooltip(effect.localizedName);

                    if(effect.dynamic){
                        cell.disabled(b -> unit.statuses().contains(e -> e.effect == effect));
                    }

                    if(++i % 4 == 0){
                        selection.row();
                    }
                }
            }).pad(8f).fill().right();
        }).growY();

        effectTable.add(settingTable).grow();

        rebuildInfo.run();
        rebuildEffectSettingTable(unitStatus, settingTable, rebuildInfo);
    }

    private void rebuildEffectSettingTable(Seq<StatusEntry> unitStatus, Table table, Runnable onChanged){
        table.clearChildren();
        table.top().background(Tex.whiteui).setColor(Pal.darkerGray);
        table.defaults().growX();

        unitStatus.each(entry -> {
            StatusEffect effect = entry.effect;

            table.table(Tex.whiteui, t -> {
                t.setColor(Pal.lightishGray);

                t.image(effect.uiIcon).pad(4f).size(40).scaling(Scaling.fit);
                t.add(effect.localizedName).ellipsis(true).width(64f).padLeft(6);

                if(entry.effect.permanent){
                    t.add(i("<永久状态>")).expandX();
                }else if(entry.effect.reactive){
                    t.add(i("<瞬间状态>")).expandX();
                }else{
                    t.table(bottom -> {
                        bottom.field("", text -> entry.time = Strings.parseFloat(text) * 60f)
                        .valid(text -> Strings.canParsePositiveFloat(text.replaceAll("∞", "Infinity")))
                        .update(it -> {
                            if(!it.hasKeyboard()){
                                it.setText(Float.isInfinite(entry.time) ? "∞" : "" + entry.time / 60f);
                            }
                        })
                        .width(100f);

                        bottom.add(i("秒"));
                        bottom.button(b -> b.add(new FLabel("{rainbow}∞")), Styles.clearNonei, () -> entry.time = Float.POSITIVE_INFINITY)
                        .size(32f).padLeft(8).expandX().right();
                    }).padTop(8f).expandX().left();
                }

                t.button(Icon.copySmall, Styles.clearNonei, 32, () -> {
                    unitStatus.add(cloneStatus(entry));

                    onChanged.run();
                    rebuildEffectSettingTable(unitStatus, table, onChanged);
                }).size(32).disabled(effect.dynamic);

                t.button(Icon.cancelSmall, Styles.clearNonei, 32, () -> {
                    unitStatus.remove(entry, true);

                    onChanged.run();
                    rebuildEffectSettingTable(unitStatus, table, onChanged);
                }).size(32);
            }).padLeft(8f).padRight(8f).padTop(12f);

            table.row();

            if(effect.dynamic) table.add(new Card(Pal.darkestGray, Card.grayOuterDark, t -> {
                t.defaults().pad(4f).padLeft(8f).left();

                t.add(i("[red]伤害")).style(Styles.outlineLabel);
                t.field("" + entry.damageMultiplier, text -> entry.damageMultiplier = Strings.parseFloat(text)).valid(Strings::canParsePositiveFloat).width(88f);

                t.add().expandX();

                t.add(i("[acid]血量")).style(Styles.outlineLabel);
                t.field("" + entry.healthMultiplier, text -> entry.healthMultiplier = Strings.parseFloat(text)).valid(Strings::canParsePositiveFloat).width(88f);
                t.add().expandX().row();

                t.add(i("[cyan]移速")).style(Styles.outlineLabel);
                t.field("" + entry.speedMultiplier, text -> entry.speedMultiplier = Strings.parseFloat(text)).valid(Strings::canParsePositiveFloat).width(88f);

                t.add().expandX();

                t.add(i("[violet]攻速")).style(Styles.outlineLabel);
                t.field("" + entry.reloadMultiplier, text -> entry.reloadMultiplier = Strings.parseFloat(text)).valid(Strings::canParsePositiveFloat).width(88f);
                t.add().expandX().row();

                t.add(i("[accent]建速")).style(Styles.outlineLabel);
                t.field("" + entry.buildSpeedMultiplier, text -> entry.buildSpeedMultiplier = Strings.parseFloat(text)).valid(Strings::canParsePositiveFloat).width(88f);

                t.add().expandX();

                t.add(i("[purple]阻力")).style(Styles.outlineLabel);
                t.field("" + entry.dragMultiplier, text -> entry.dragMultiplier = Strings.parseFloat(text)).valid(Strings::canParsePositiveFloat).width(88f);
                t.add().expandX().row();

                t.add(i("[teal]装甲")).style(Styles.outlineLabel);
                t.field("" + entry.armorOverride, text -> entry.armorOverride = Strings.parseFloat(text)).valid(Strings::canParseFloat).width(88f);

                for(Element child : t.getChildren()){
                    if(child instanceof TextField field){
                        field.changed(onChanged);
                    }
                }
            })).padLeft(8f).padRight(8f).fillX();

            table.row();
        });
    }

    private void rebuildPayloadTable(Unit unit, Table payloadTable){
        payloadTable.clearChildren();

        if(!(unit instanceof PayloadUnit payloadUnit)) return;
        Seq<Payload> payloads = payloadUnit.payloads;

        Table settingTable = new Table(Tex.whiteui);
        settingTable.setColor(Pal.gray);

        payloadTable.table(Tex.whiteui, table -> {
            table.top();
            table.setColor(Pal.gray);

            table.image(Blocks.payloadLoader.uiIcon).size(64).scaling(Scaling.fit).expandX().left();

            table.button(Icon.refresh, Styles.cleari, 48f, () -> {
                payloads.clear();
                rebuildPayloadSettingTable(payloads, settingTable);
            }).size(64f).pad(8f);

            table.row();

            table.table(Styles.grayPanel, buttons -> {
                buttons.defaults().height(48f).pad(4f).growX();

                buttons.button(i("装载建筑"), new TextureRegionDrawable(Blocks.siliconSmelter.uiIcon), Styles.flatt, 32,
                () -> ContentSelectDialog.once(content.blocks().select(block -> !block.isFloor() && block.buildVisibility != BuildVisibility.hidden),
                null,
                block -> {
                    BuildPayload payload = new BuildPayload(block, payloadUnit.team);
                    payloads.add(payload);
                    rebuildPayloadSettingTable(payloads, settingTable);
                })).row();

                buttons.button(i("装载单位"), new TextureRegionDrawable(UnitTypes.alpha.uiIcon), Styles.flatt, 32f,
                () -> ContentSelectDialog.once(content.units(), null, unitType -> {
                    UnitPayload payload = new UnitPayload(unitType.create(payloadUnit.team));
                    payloads.add(payload);
                    rebuildPayloadSettingTable(payloads, settingTable);
                })).row();

                buttons.button(i("装载自己"), Icon.add, Styles.flatt, () -> {
                    payloads.add(new UnitPayload(cloneUnit(payloadUnit)));
                    rebuildPayloadSettingTable(payloads, settingTable);
                }).row();
            }).pad(8f).colspan(2).fill();
        }).fillY();

        payloadTable.pane(Styles.noBarPane, settingTable).grow();

        rebuildPayloadSettingTable(payloads, settingTable);
    }

    private void rebuildPayloadSettingTable(Seq<Payload> payloads, Table table){
        table.clearChildren();
        table.top().background(Tex.whiteui).setColor(Pal.darkerGray);
        table.defaults().pad(8).growX().uniformX();

        int i = 0;
        for(Payload payload : payloads){
            table.table(Tex.whiteui, t -> {
                t.setColor(Pal.lightishGray);

                t.image(payload.content().uiIcon).pad(4f).size(48f).scaling(Scaling.fit);
                t.add(payload.content().localizedName).ellipsis(true).width(64f).padLeft(6).expandX().left();

                t.defaults().size(32).pad(4f);

                if(payload instanceof UnitPayload unitPayload){
                    t.button(Icon.editSmall, Styles.clearNonei, 24f, () -> simpleFactory(unitPayload.unit));
                }

                t.button(Icon.copySmall, Styles.clearNonei, 24f, () -> {
                    Payload copied = clonePayload(payload);
                    payloads.add(copied);

                    rebuildPayloadSettingTable(payloads, table);
                });

                t.button(Icon.cancelSmall, Styles.clearNonei, 24f, () -> {
                    payloads.remove(payload, true);

                    rebuildPayloadSettingTable(payloads, table);
                });
            });

            if(++i % 2 == 0){
                table.row();
            }
        }

        if(i == 1){
            // 占位
            table.add();
        }
    }

    private void simpleFactory(Unit unit){
        BaseDialog dialog = new BaseDialog(i("单位工厂"));

        Table main = new Table();

        float width = Math.min(Core.scene.getWidth() * (Core.scene.getWidth() > 700 ? 0.6f : 0.9f) / Scl.scl(), 700);
        dialog.cont.add(main).width(width).growY();

        main.top();
        main.defaults().growX();

        main.table(infoTable -> rebuildInfoTable(unit, infoTable)).row();

        main.defaults().padTop(12f);

        main.table(itemTable -> rebuildItemTable(unit, itemTable)).row();
        main.table(propertiesTable -> rebuildPropertiesTable(unit, propertiesTable)).row();

        main.table(teamTable -> rebuildTeamTable(unit, teamTable)).row();

        main.table(bottomTable -> {
            bottomTable.left();
            bottomTable.defaults().grow().uniformY();

            bottomTable.table(effectTable -> rebuildEffectsTable(unit, effectTable)).fillX().row();

            bottomTable.defaults().padTop(16f);
            bottomTable.table(payloadTable -> rebuildPayloadTable(unit, payloadTable)).fillX();
        }).padTop(8f).growY();

        dialog.addCloseButton();
        dialog.buttons.button(i("重置"), Icon.refresh, () -> {
            resetUnit(unit);
            rebuildTables();
        });

        dialog.show();
    }

    private static Unit cloneUnit(Unit unit){
        Unit cloned = unit.type.create(unit.team);
        cloned.health = unit.health;
        cloned.shield = unit.shield;
        cloned.stack.set(unit.stack.item, unit.stack.amount);
        cloned.elevation = unit.elevation;
        cloned.set(unit);

        if(unit instanceof Payloadc payloadUnit && cloned instanceof Payloadc clonedPayloadUnit){
            for(Payload payload : payloadUnit.payloads()){
                Payload copied = clonePayload(payload);
                clonedPayloadUnit.addPayload(copied);
            }
        }

        Seq<StatusEntry> statusEntries = cloned.statuses();
        statusEntries.set(unit.statuses());
        statusEntries.replace(UnitFactoryDialog::cloneStatus);
        return cloned;
    }

    private static void resetUnit(Unit unit){
        unit.setType(unit.type);
        unit.elevation = Mathf.num(unit.type.flying);
        unit.statuses().clear();
        if(unit instanceof Payloadc pay){
            pay.payloads().clear();
        }
    }

    private static StatusEntry cloneStatus(StatusEntry entry){
        StatusEntry cloned = new StatusEntry().set(entry.effect, entry.time);

        if(entry.effect.dynamic){
            cloned.damageMultiplier = entry.damageMultiplier;
            cloned.healthMultiplier = entry.healthMultiplier;
            cloned.speedMultiplier = entry.speedMultiplier;
            cloned.reloadMultiplier = entry.reloadMultiplier;
            cloned.buildSpeedMultiplier = entry.buildSpeedMultiplier;
            cloned.dragMultiplier = entry.dragMultiplier;
            cloned.armorOverride = entry.armorOverride;
        }

        return cloned;
    }

    private static Payload clonePayload(Payload payload){
        if(payload instanceof BuildPayload buildPayload){
            Building build = buildPayload.build;
            return new BuildPayload(build.block, build.team);
        }
        if(payload instanceof UnitPayload unitPayload){
            Unit unit = cloneUnit(unitPayload.unit);
            return new UnitPayload(unit);
        }
        throw new IllegalArgumentException("Unknown payload type: " + payload);
    }

    private static class UnitStack{
        private static Seq<UnitStack> classedUnits;
        public static UnitStack vanillaStack = new UnitStack(null);

        public @Nullable LoadedMod mod;
        public Seq<UnitType> units = new Seq<>();

        public UnitStack(@Nullable LoadedMod mod){
            this.mod = mod;
        }

        public String name(){
            return mod == null ? Core.bundle.get("vanilla", i("原版")) : mod.meta.displayName;
        }

        public TextureRegion icon(){
            return mod == null ? Blocks.duo.uiIcon :
            mod.iconTexture != null ? new TextureRegion(mod.iconTexture) : null;
        }

        private static Seq<UnitStack> getUnitStacks(){
            if(classedUnits == null){
                initClassedUnits();
            }
            return classedUnits;
        }

        private static void initClassedUnits(){
            classedUnits = new Seq<>();
            classedUnits.add(vanillaStack);
            content.units().each(unit -> {
                if(unit.isVanilla()){
                    vanillaStack.units.add(unit);
                }else{
                    LoadedMod mod = unit.minfo.mod;
                    UnitStack stack = classedUnits.find(s -> s.mod == mod);

                    if(stack == null){
                        stack = new UnitStack(mod);
                        classedUnits.add(stack);
                    }

                    stack.units.add(unit);
                }
            });
        }
    }
}
