package mindustryX.features.ui;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustryX.features.*;

import java.util.*;

import static mindustry.Vars.*;
import static mindustry.ui.Styles.*;

public class ArcUnitFactoryDialog extends BaseDialog{
    private int unitCount = 1;
    private float unitRandDst = 1f;
    private final Vec2 unitLoc = new Vec2(0, 0);
    private final Unit spawnUnit = UnitTypes.emanate.create(Team.sharded);
    private final OrderedSet<StatusEntry> unitStatus = new OrderedSet<>();
    private final float[] statusTime = {10, 30f, 60f, 120f, 180f, 300f, 600f, 900f, 1200f, 1500f, 1800f, 2700f, 3600f, Float.MAX_VALUE};
    private float chatTime = 0;
    private boolean showUnitSelect, showUnitPro, showStatesEffect, showItems, showPayload, showSelectPayload, showPayloadBlock, elevation;
    private final boolean enableRTSCode = false;

    public ArcUnitFactoryDialog(){
        super("单位工厂-ARC");
        cont.table(t -> {
            t.add("目标单位：");
            t.image().update(it -> it.setDrawable(spawnUnit.type.uiIcon)).scaling(Scaling.fit).maxWidth(32);
            t.label(() -> spawnUnit.type.localizedName);
        }).row();

        cont.table((r) -> {
            r.add("数量：");
            r.field("" + unitCount, text -> unitCount = Integer.parseInt(text))
            .valid(Strings::canParsePositiveInt).maxTextLength(4);
        }).row();

        cont.table((r) -> {
            r.add("生成范围：");
            r.field(Strings.autoFixed(unitRandDst, 3), text -> unitRandDst = Float.parseFloat(text))
            .valid(Strings::canParsePositiveFloat).tooltip("在目标点附近的这个范围内随机生成").maxTextLength(8);
            r.add("格");
        }).row();

        cont.table(t -> {
            t.add("坐标: ");
            UIExt.buildPositionRow(t, unitLoc);
        }).row();

        var unitFabricator = new Collapser(t -> t.pane(this::buildUnitFabricator).minHeight(300), true);
        cont.button(Blocks.tetrativeReconstructor.emoji() + "[cyan]单位状态重构厂", unitFabricator::toggle).fillX().row();
        cont.add(unitFabricator).row();

        cont.button("[orange]生成！", Icon.modeAttack, () -> {
            for(var n = 0; n < unitCount; n++){
                Tmp.v1.rnd(Mathf.random(unitRandDst * tilesize));
                Unit unit = cloneUnit(spawnUnit);
                if(elevation) unit.elevation = 1f;
                unit.set(unitLoc.x * tilesize + Tmp.v1.x, unitLoc.y * tilesize + Tmp.v1.y);
                UnitExt.getStatuses(unit).addAll(unitStatus);
                unit.add();
            }
            if(control.input instanceof DesktopInput input){
                input.panning = true;
            }
            Core.camera.position.set(unitLoc.x * tilesize, unitLoc.y * tilesize);
        }).fillX().row();

        cont.button("[orange] 生成！(/js)", Icon.modeAttack, () -> {
            if(chatTime > 0f){
                ui.showInfoFade("为了防止因ddos被服务器ban，请勿太快操作", 5f);
                return;
            }
            chatTime = 1f;
            ui.showInfoFade("已生成单个单位。\n[gray]请不要短时多次使用本功能，否则容易因ddos被服务器ban", 5f);
            Tmp.v1.rnd(Mathf.random(unitRandDst)).add(unitLoc.x, unitLoc.y).scl(tilesize);
            sendFormatChat("/js u = UnitTypes.@.create(Team.get(@))",
            spawnUnit.type.name,
            spawnUnit.team.id
            );
            sendFormatChat("/js u.set(@,@)",
            unitLoc.x * tilesize,
            unitLoc.y * tilesize
            );
            if(spawnUnit.health != spawnUnit.type.health){
                sendFormatChat("/js u.health = @", spawnUnit.health);
                if(spawnUnit.health > spawnUnit.type.health){
                    sendFormatChat("/js u.maxHealth = @", spawnUnit.health);
                }
            }
            if(spawnUnit.shield != 0)
                sendFormatChat("/js u.shield = @", spawnUnit.shield);
            if(elevation)
                sendFormatChat("/js u.elevation = 1");
            if(!unitStatus.isEmpty()){
                sendFormatChat("/js gs=(t,o,n)=>{try{let f=t.getDeclaredField(n);f.setAccessible(true);return f.get(o)}catch(e){let s=t.getSuperclass();return s?gs(s,o,n):null}}");
                sendFormatChat("/js statuses = gs(u.class,u,\"statuses\")");
                unitStatus.each(entry -> {
                    if(!entry.effect.reactive){
                        sendFormatChat("/js {e = new StatusEntry().set(StatusEffects.@, @);statuses.add(e);statuses.size}", entry.effect.name, entry.time * 60f);
                    }else sendFormatChat("/js u.apply(StatusEffects.@)", entry.effect.name);
                });
                sendFormatChat("/js delete statuses");
            }
            if(spawnUnit.hasItem()){
                sendFormatChat("/js u.addItem(Items.@, @)", spawnUnit.stack.item.name, spawnUnit.stack.amount);
            }
            sendFormatChat("/js u.add()");
            sendFormatChat("/js delete u");
            Time.run(chatTime, () -> chatTime = 0f);
            if(control.input instanceof DesktopInput input){
                input.panning = true;
            }
            Core.camera.position.set(unitLoc.x * tilesize, unitLoc.y * tilesize);
        }).fillX().visible(() -> Core.settings.getBool("easyJS")).row();

        cont.button("RTS价格代码生成", Icon.logic, this::generateRTSCode).fillX().visible(() -> enableRTSCode);

        closeOnBack();
        addCloseButton();
    }

    void buildUnitFabricator(Table table){
        table.clear();
        table.button((b) -> {
            b.image(showUnitSelect ? Icon.upOpen : Icon.downOpen);
            b.table(t -> {
                t.add("加工单位：");
                t.image(spawnUnit.type.uiIcon).scaling(Scaling.fit).maxWidth(32);
            }).grow();
        }, Styles.togglet, () -> showUnitSelect = !showUnitSelect).fillX().minWidth(400f).row();
        table.collapser(list -> {
            int i = 0;
            for(UnitType unit : content.units()){
                if(i++ % 8 == 0) list.row();
                list.button((b) -> {
                    b.image(unit.uiIcon).scaling(Scaling.fit);
                    if(enableRTSCode) b.add("" + getPrice(unit));
                }, cleart, () -> {
                    if(spawnUnit.type != unit){
                        changeUnitType(spawnUnit, unit);
                        buildUnitFabricator(table);
                    }
                    showUnitSelect = !showUnitSelect;
                    buildUnitFabricator(table);
                }).tooltip(unit.localizedName).width(enableRTSCode ? 100f : 50f).height(50f);
            }
        }, () -> showUnitSelect).row();

        table.button("[#" + spawnUnit.team.color + "]单位属性", showUnitPro ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> showUnitPro = !showUnitPro).fillX().row();
        table.collapser(t -> {
            t.table(tt -> {
                tt.add("[red]血：");
                tt.field(Strings.autoFixed(spawnUnit.health, 1), text -> spawnUnit.health = Float.parseFloat(text)).valid(Strings::canParsePositiveFloat);
                tt.add("[yellow]盾：");
                tt.field(Strings.autoFixed(spawnUnit.shield, 1), text -> spawnUnit.shield = Float.parseFloat(text)).valid(Strings::canParsePositiveFloat);
            }).row();
            t.table(tt -> {
                tt.add("队伍：");
                var f = tt.field(String.valueOf(spawnUnit.team.id), text -> spawnUnit.team = Team.get(Integer.parseInt(text)))
                .valid(text -> Strings.canParsePositiveInt(text) && Integer.parseInt(text) < Team.all.length).maxTextLength(4).get();
                for(Team team : Team.baseTeams){
                    tt.button("[#" + team.color + "]" + team.localized(), flatToggleMenut, () -> {
                        spawnUnit.team = team;
                        f.setText(String.valueOf(team.id));
                    }).checked(b -> spawnUnit.team == team).size(30, 30);
                }
                tt.button("[violet]+", flatToggleMenut,
                () -> UIExt.teamSelect.pickOne(team -> {
                    spawnUnit.team = team;
                    f.setText(String.valueOf(team.id));
                }, spawnUnit.team)
                ).checked(b -> !Seq.with(Team.baseTeams).contains(spawnUnit.team)).tooltip("[acid]更多队伍选择").center().width(50f).row();
            }).row();
            t.check("飞行模式    [orange]生成的单位会飞起来", elevation, a -> elevation = !elevation).center().padBottom(5f).padRight(10f);
        }, () -> showUnitPro).row();

        table.button((b) -> {
            b.image(showStatesEffect ? Icon.upOpen : Icon.downOpen);
            b.table((t) -> {
                t.add("单位状态 ").fill();
                for(var entry : unitStatus){
                    t.image(entry.effect.uiIcon).scaling(Scaling.fit);
                }
            }).grow();
        }, Styles.togglet, () -> showStatesEffect = !showStatesEffect).fillX().row();

        table.collapser(t -> {
            t.table(list -> {
                int i = 0;
                for(StatusEffect effect : content.statusEffects()){
                    if(effect == StatusEffects.none) continue;
                    if(i++ % 8 == 0) list.row();
                    list.button((b) -> b.image(effect.uiIcon).scaling(Scaling.fit), squareTogglet, () -> {
                        unitStatus.add(new StatusEntry().set(effect, unitStatus.isEmpty() ? 600f : unitStatus.orderedItems().peek().time));
                        buildUnitFabricator(table);
                    }).size(50f).color(unitStatus.select(e -> e.effect == effect).isEmpty() ? Color.gray : Color.white).tooltip(effect.localizedName);
                }
            }).top().center();

            t.row();

            float[] status = {1f, 1f, 1f, 1f};
            unitStatus.each(s -> {
                status[0] *= s.effect.healthMultiplier;
                status[1] *= s.effect.damageMultiplier;
                status[2] *= s.effect.reloadMultiplier;
                status[3] *= s.effect.speedMultiplier;
            });
            t.table(tt -> {
                tt.add("[acid]血量");
                tt.add("[red]伤害");
                tt.add("[violet]攻速");
                tt.add("[cyan]移速");
                tt.row();
                tt.add(FormatDefault.format(status[0]));
                tt.add(FormatDefault.format(status[1]));
                tt.add(FormatDefault.format(status[2]));
                tt.add(FormatDefault.format(status[3]));
            }).row();
            t.table(list -> {
                for(var entry : unitStatus){
                    list.image(entry.effect.uiIcon).scaling(Scaling.fit);
                    list.add(entry.effect.localizedName).padRight(4f);

                    if(entry.effect.permanent){
                        list.add("<永久状态>");
                    }else if(entry.effect.reactive){
                        list.add("<瞬间状态>");
                    }else{
                        list.table(et -> {
                            TextField sField = et.field(checkInf(entry.time), text -> entry.time = Objects.equals(text, "Inf") ? Float.MAX_VALUE : Float.parseFloat(text)).valid(text -> Objects.equals(text, "Inf") || Strings.canParsePositiveFloat(text)).tooltip("buff持续时间(单位：秒)").maxTextLength(10).get();

                            et.add("秒");

                            Slider sSlider = et.slider(0f, statusTime.length - 1f, 1f, statusTimeIndex(entry.time), n -> {
                                if(statusTimeIndex(entry.time) != n){
                                    sField.setText(checkInf(statusTime[(int)n]));
                                }
                                entry.time = statusTime[(int)n];
                            }).get();

                            sField.update(() -> sSlider.setValue(statusTimeIndex(entry.time)));
                        });
                    }

                    list.button(Icon.cancel, () -> {
                        unitStatus.remove(entry);
                        buildUnitFabricator(table);
                    });
                    list.row();
                }
            });
        }, () -> showStatesEffect).row();

        table.button((b) -> {
            b.image(showItems ? Icon.upOpen : Icon.downOpen);
            b.table(t -> {
                t.add("携带物品");
                if(spawnUnit.stack.amount > 0){
                    t.image(spawnUnit.stack.item.uiIcon).scaling(Scaling.fit);
                    t.add("" + spawnUnit.stack.amount).padRight(4f);
                }
            }).grow();
        }, Styles.togglet, () -> showItems = !showItems).fillX().row();
        table.collapser(pt -> {
            pt.table(ptt -> {
                int i = 0;
                for(Item item : content.items()){
                    ptt.button(b -> b.image(item.uiIcon).scaling(Scaling.fit), cleart, () -> {
                        spawnUnit.stack.item = item;
                        if(spawnUnit.stack.amount == 0){
                            spawnUnit.stack.amount = spawnUnit.itemCapacity();
                        }
                        buildUnitFabricator(table);
                    }).size(50f).left().tooltip(item.localizedName);
                    if(++i % 6 == 0) ptt.row();
                }
            });
            if(spawnUnit.stack.amount > 0){
                pt.row();
                pt.table(ptt -> {
                    ptt.image(spawnUnit.stack.item.uiIcon).scaling(Scaling.fit);
                    ptt.add(" 数量：");
                    ptt.field(String.valueOf(spawnUnit.stack.amount), text -> spawnUnit.stack.amount = Integer.parseInt(text)).valid(value -> {
                        if(!Strings.canParsePositiveInt(value)) return false;
                        int val = Integer.parseInt(value);
                        return 0 < val && val <= spawnUnit.type.itemCapacity;
                    }).maxTextLength(4);
                    ptt.add("/ " + spawnUnit.type.itemCapacity + " ");
                    ptt.button(Icon.up, cleari, () -> {
                        spawnUnit.stack.amount = spawnUnit.type.itemCapacity;
                        buildUnitFabricator(table);
                    }).tooltip("设置物品数量为单位最大容量");
                    ptt.button(Icon.down, cleari, () -> {
                        spawnUnit.stack.amount = 0;
                        buildUnitFabricator(table);
                    }).tooltip("清空单位物品");
                });
            }
        }, () -> showItems).row();

        if(spawnUnit instanceof Payloadc pay){
            table.button((b) -> {
                b.image(showPayload ? Icon.upOpen : Icon.downOpen);
                b.table(t -> {
                    t.add("携带负载");
                    for(Payload payload : pay.payloads()){
                        t.image(payload.content().uiIcon).scaling(Scaling.fit).maxWidth(32);
                    }
                }).grow();
            }, Styles.togglet, () -> showPayload = !showPayload).fillX().checked(showPayload).row();
            table.collapser(p -> {
                p.table(pt -> pay.payloads().each(payload -> {
                    if(payload instanceof Payloadc payloadUnit){
                        pt.button(b -> b.image(payload.content().uiIcon).scaling(Scaling.fit).getTable().add("[red]*"), squareTogglet, () -> {
                            pay.payloads().remove(payload);
                            buildUnitFabricator(table);
                        }).color(payloadUnit.team().color).size(50f).left();
                    }else{
                        pt.button(b -> b.image(payload.content().uiIcon).scaling(Scaling.fit), squareTogglet, () -> {
                            pay.payloads().remove(payload);
                            buildUnitFabricator(table);
                        }).size(50f).left();
                    }
                    if(pay.payloads().indexOf(payload) % 8 == 7) pt.row();
                })).row();

                p.button("载入单位 " + UnitTypes.mono.emoji(), showSelectPayload ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> showSelectPayload = !showSelectPayload).width(300f).row();
                p.collapser((c) -> {
                    c.table(list -> {
                        int i = 0;
                        for(UnitType units : content.units()){
                            list.button(b -> b.image(units.uiIcon).scaling(Scaling.fit), () -> {
                                pay.addPayload(new UnitPayload(units.create(spawnUnit.team)));
                                buildUnitFabricator(table);
                            }).size(50f).tooltip(units.localizedName);
                            if(++i % 8 == 0) list.row();
                        }
                    });
                    c.row();
                    c.table(pt -> {
                        pt.button("[cyan]自递归", () -> {
                            pay.pickup(cloneUnit(spawnUnit));
                            buildUnitFabricator(table);
                        }).width(200f);
                        pt.button("?", () -> ui.showInfo("""
                        使用说明：携带的单位存在一个序列，每个单位可以具备特定的属性。
                        [cyan]自递归[white]是指根据当前的配置生成一个单位，并储存到载荷序列上
                        这一单位具备所有目前设置的属性，包括buff、物品和载荷。
                        合理使用自递归可以发掘无限的可能性
                        [orange][警告]可能导致地图损坏！请备份地图后再使用！""")).size(50f);
                    }).row();
                }, () -> showSelectPayload).row();

                p.button("载入建筑 " + Blocks.surgeWallLarge.emoji(), showPayloadBlock ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> showPayloadBlock = !showPayloadBlock).width(300f).row();
                p.collapser(list -> {
                    int i = 0;
                    for(Block payBlock : content.blocks()){
                        if(!payBlock.isVisible() || !payBlock.isAccessible() || payBlock.isFloor())
                            continue;
                        list.button(b -> b.image(payBlock.uiIcon).scaling(Scaling.fit), () -> {
                            pay.addPayload(new BuildPayload(payBlock, spawnUnit.team));
                            buildUnitFabricator(table);
                        }).size(50f).tooltip(payBlock.localizedName);
                        if(++i % 8 == 0) list.row();
                    }
                }, () -> showPayloadBlock);
            }, () -> showPayload).fillX().row();
        }

        table.button("[red]重置出厂状态", () -> {
            resetUnitType(spawnUnit, spawnUnit.type);
            buildUnitFabricator(table);
        }).fillX().row();
        //table.add("[orange]单位加工车间。 [white]Made by [violet]Lucky Clover\n").width(400f);
    }

    private String checkInf(float value){
        if(value == Float.MAX_VALUE){
            return "Inf";
        }
        return Strings.autoFixed(value, 1);
    }

    private int statusTimeIndex(float time){
        for(int i = statusTime.length - 1; i >= 0; i--){
            if(statusTime[i] <= time){
                return i;
            }
        }
        return 0;
    }

    private Unit cloneUnit(Unit unit){
        Unit reUnit = unit.type.create(unit.team);
        reUnit.health = unit.health;
        reUnit.shield = unit.shield;
        reUnit.stack = unit.stack;

        if(unit instanceof Payloadc pay && reUnit instanceof Payloadc rePay){
            pay.payloads().each(rePay::addPayload);
        }
        return reUnit;
    }


    private void resetUnitType(Unit unit, UnitType unitType){
        elevation = false;
        unit.type = unitType;
        unit.health = unitType.health;
        unit.shield = 0;
        unit.stack.amount = 0;
        if(unit instanceof Payloadc pay){
            pay.payloads().clear();
        }
        unitStatus.clear();
    }

    private void changeUnitType(Unit unit, UnitType unitType){
        unit.type = unitType;
        unit.health = unitType.health;
        unit.shield = 0;
        if(unit.stack.amount > unit.itemCapacity()){
            unit.stack.amount = unit.itemCapacity();
        }
        unitStatus.clear();
    }

    private void sendFormatChat(String format, Object... args){
        for(int i = 0; i < args.length; i++){
            if(args[i] instanceof Float f){
                args[i] = Strings.autoFixed(f, 1);
            }
        }
        Time.run(chatTime, () -> Call.sendChatMessage(Strings.format(format, args)));
        chatTime = chatTime + 10f;
    }

    private int getPrice(UnitType unitType){
        return (int)(unitType.health * (1 + unitType.range / 8 / 50) / 20);
    }


    private void generateRTSCode(){
        StringBuilder code = new StringBuilder();
        Vars.content.units().each(unitType -> {
            code.append("set 单位 @").append(unitType.name).append("\n");
            code.append("set 价格 ").append(getPrice(unitType)).append("\n");
            code.append("set 工厂 ");
            if(unitType.flying){
                if(unitType.health < 400) code.append("空1");
                else code.append("空2");
            }else{
                if(unitType.allowLegStep || unitType.naval) code.append("海爬");
                else code.append("陆");
                if(unitType.health < 720) code.append("1");
                else code.append("2");
            }
            code.append("\n");
            code.append("set 名称 \"").append(unitType.emoji()).append(" ").append(unitType.localizedName).append(" ").append(unitType.name).append("\"\n");
            code.append("set @counter c返回").append("\n");

        });
        Core.app.setClipboardText(code.toString());
    }
}
