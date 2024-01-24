package mindustry.arcModule.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.arcModule.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.payloads.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.maxBuildPlans;
import static mindustry.arcModule.DrawUtilities.drawNSideRegion;

public class ARCUnits{
    private static boolean alwaysShowPlayerUnit, alwaysShowUnitRTSAi, unitHealthBar, unitLogicMoveLine, unitLogicTimerBars, unithitbox, unitBuildPlan;
    private static float defaultUnitTrans, unitDrawMinHealth, unitBarDrawMinHealth;
    private static float unitWeaponRange, unitWeaponRangeAlpha;
    public static boolean selectedUnitsFlyer, selectedUnitsLand;

    private static float curStroke;
    private static int unitTargetType, superUnitEffect;
    private static boolean arcBuildInfo;

    static{
        // 减少性能开销
        Events.run(EventType.Trigger.update, () -> {
            alwaysShowPlayerUnit = Core.settings.getBool("alwaysShowPlayerUnit");
            alwaysShowUnitRTSAi = Core.settings.getBool("alwaysShowUnitRTSAi");
            unitHealthBar = Core.settings.getBool("unitHealthBar");
            unitLogicMoveLine = Core.settings.getBool("unitLogicMoveLine");
            unitLogicTimerBars = Core.settings.getBool("unitLogicTimerBars");
            unithitbox = Core.settings.getBool("unithitbox");
            unitBuildPlan = Core.settings.getBool("unitbuildplan");

            defaultUnitTrans = ARCVars.unitHide ? 0 : Core.settings.getInt("unitTransparency") / 100f;
            unitDrawMinHealth = Core.settings.getInt("unitDrawMinHealth");
            unitBarDrawMinHealth = Core.settings.getInt("unitBarDrawMinHealth");

            unitWeaponRange = Core.settings.getInt("unitWeaponRange") * tilesize;
            unitWeaponRangeAlpha = Core.settings.getInt("unitWeaponRangeAlpha") / 100f;

            selectedUnitsFlyer = control.input.selectedUnits.contains(Flyingc::isFlying);
            selectedUnitsLand = control.input.selectedUnits.contains(unit -> !unit.isFlying());

            curStroke = (float) Core.settings.getInt("playerEffectCurStroke") / 10f;
            unitTargetType = Core.settings.getInt("unitTargetType");
            superUnitEffect = Core.settings.getInt("superUnitEffect");
            arcBuildInfo = Core.settings.getBool("arcBuildInfo");
        });
    }

    public static float drawARCUnits(Unit unit){
        float unitTrans = defaultUnitTrans;
        boolean drawUnit = (unit.maxHealth + unit.shield) > unitDrawMinHealth;
        boolean drawUnitBar = (unit.maxHealth + unit.shield) > unitBarDrawMinHealth;

        if (!drawUnit) {
            unitTrans = 0f;
            drawUnitBar = false;
        }
        if (unit.controller() instanceof Player) {
            drawPlayerEffect(unit);
            if (alwaysShowPlayerUnit) {
                unitTrans = 100f;
                drawUnitBar = true;
            }
        }
        if (drawUnitBar) {
            if (ARCVars.arcInfoControl(unit.team())) {
                drawWeaponRange(unit);
                drawRTSAI(unit);
                drawHealthBar(unit);
                drawLogic(unit);
                drawBuildPlan(unit);
            }
            drawHitBox(unit);
        }
        return unitTrans;
    }

    private static void drawPlayerEffect(Unit unit) {
        Color effectColor = unit.controller() == player ? ARCVars.getPlayerEffectColor() : unit.team.color;

        boolean drawCircle = (unit.controller() == player && superUnitEffect != 0) || (unit.controller() instanceof Player && superUnitEffect == 2);
        if (drawCircle) {
            // 射程圈
            Lines.stroke(Lines.getStroke() * curStroke);

            Draw.z(Layer.effect - 2f);
            Draw.color(effectColor);

            Tmp.v1.trns(unit.rotation - 90, unit.x, unit.y).add(unit.x, unit.y);

            if (curStroke > 0) {
                for (int i = 0; i < 5; i++) {
                    float rot = unit.rotation + i * 360f / 5 + Time.time * 0.5f;
                    Lines.arc(unit.x, unit.y, unit.type.maxRange, 0.14f, rot, (int) (50 + unit.type.maxRange / 10));
                }
            }
        }
        // 武器圈
        if (unitTargetType > 0) {
            Draw.z(Layer.effect);
            Draw.color(effectColor, 0.8f);
            Lines.stroke(1f);
            Lines.line(unit.x, unit.y, unit.aimX, unit.aimY);
            switch (unitTargetType) {
                case 1:
                    Lines.dashCircle(unit.aimX, unit.aimY, 8);
                    break;
                case 2:
                    Drawf.target(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                    break;
                case 3:
                    Drawf.target2(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                    break;
                case 4:
                    Drawf.targetc(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                    break;
                case 5:
                    Drawf.targetd(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                    break;
            }
        }

        //玩家专属特效
        if (unit.controller() == player) {
            detailBuildMode();
        }
    }

    private static void drawWeaponRange(Unit unit){
        if(unitWeaponRange == 0 || unitWeaponRangeAlpha == 0) return;
        if(unitWeaponRange == 30){
            drawWeaponRange(unit, unitWeaponRangeAlpha);
        }else if(unit.team != player.team()){
            boolean canHitPlayer = !player.unit().isNull() && player.unit().hittable() && (player.unit().isFlying() ? unit.type.targetAir : unit.type.targetGround)
            && unit.within(player.unit().x, player.unit().y, unit.type.maxRange + unitWeaponRange);
            boolean canHitCommand = control.input.commandMode && ((selectedUnitsFlyer && unit.type.targetAir) || (selectedUnitsLand && unit.type.targetGround));
            boolean canHitPlans = (control.input.block != null || control.input.selectPlans.size > 0) && unit.type.targetGround;
            boolean canHitMouse = unit.within(Core.input.mouseWorldX(), Core.input.mouseWorldY(), unit.type.maxRange + unitWeaponRange);
            if(canHitPlayer || (canHitMouse && (canHitCommand || canHitPlans)))
                drawWeaponRange(unit, unitWeaponRangeAlpha);
        }
    }

    private static void drawWeaponRange(Unit unit, float alpha){
        Draw.color(unit.team.color);
        Draw.alpha(alpha);
        Lines.dashCircle(unit.x, unit.y, unit.type.maxRange);
        Draw.reset();
    }

    private static void drawRTSAI(Unit unit) {
        if (!control.input.commandMode && alwaysShowUnitRTSAi && unit.isCommandable() && unit.command().command != null && unit.command().command.name.equals("move")) {
            Draw.z(Layer.effect);
            CommandAI ai = unit.command();
            //draw target line
            if (ai.targetPos != null) {
                Position lineDest = ai.attackTarget != null ? ai.attackTarget : ai.targetPos;
                Draw.color(unit.team.color);
                Drawf.limitLineColor(unit, lineDest, unit.hitSize / 2f, 3.5f, unit.team.color);

                if (ai.attackTarget == null) {
                    Draw.color(unit.team.color);
                    Drawf.square(lineDest.getX(), lineDest.getY(), 3.5f, unit.team.color);
                }
            }

            if (ai.attackTarget != null) {
                Draw.color(unit.team.color);
                Drawf.target(ai.attackTarget.getX(), ai.attackTarget.getY(), 6f, unit.team.color);
            }
            Draw.color();
        }
        Draw.reset();
    }

    private static void drawHealthBar(Unit unit) {
        if(!unitHealthBar) return;
        Draw.z(Layer.shields + 6f);
        float y_corr = 0f;
        if (unit.hitSize < 30f && unit.hitSize > 20f && unit.controller().isBeingControlled(player.unit())) y_corr = 2f;
        if (unit.health < unit.maxHealth) {
            Draw.reset();
            Lines.stroke(4f);
            Draw.color(unit.team.color, 0.5f);
            Lines.line(unit.x - unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f) + y_corr, unit.x + unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f) + y_corr);
            Lines.stroke(2f);
            Draw.color(Pal.health, 0.8f);
            Lines.line(
                    unit.x - unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f) + y_corr,
                    unit.x + unit.hitSize() * (Math.min(Mathf.maxZero(unit.health), unit.maxHealth) * 1.2f / unit.maxHealth - 0.6f), unit.y + (unit.hitSize() / 2f) + y_corr);
            Lines.stroke(2f);
        }
        if (unit.shield > 0 && unit.shield < 1e20) {
            for (int didgt = 1; didgt <= Mathf.digits((int) (unit.shield / unit.maxHealth)) + 1; didgt++) {
                Draw.color(Pal.shield, 0.8f);
                float shieldAmountScale = unit.shield / (unit.maxHealth * Mathf.pow(10f, (float) didgt - 1f));
                if (didgt > 1) {
                    Lines.line(unit.x - unit.hitSize() * 0.6f,
                            unit.y + (unit.hitSize() / 2f) + (float) didgt * 2f + y_corr,
                            unit.x + unit.hitSize() * ((Mathf.ceil((shieldAmountScale - Mathf.floor(shieldAmountScale)) * 10f) - 1f + 0.0001f) * 1.2f * (1f / 9f) - 0.6f),
                            unit.y + (unit.hitSize() / 2f) + (float) didgt * 2f + y_corr);
                    //(s-1)*(1/9)because line(0) will draw length of 1
                } else {
                    Lines.line(unit.x - unit.hitSize() * 0.6f,
                            unit.y + (unit.hitSize() / 2f) + (float) didgt * 2f + y_corr,
                            unit.x + unit.hitSize() * ((shieldAmountScale - Mathf.floor(shieldAmountScale) - 0.001f) * 1.2f - 0.6f),
                            unit.y + (unit.hitSize() / 2f) + (float) didgt * 2f + y_corr);
                }
            }
        }
        Draw.reset();

        float index = 0f;
        float iconSize = 4f;
        int iconColumns = Math.max((int) (unit.hitSize() / (iconSize + 1f)), 4);
        float iconWidth = Math.min(unit.hitSize() / iconColumns, iconSize + 1f);
        for (var entry : unit.statuses()) {
            Draw.rect(entry.effect.uiIcon,
                    unit.x - unit.hitSize() * 0.6f + iconWidth * (index % iconColumns),
                    unit.y + (unit.hitSize() / 2f) + 3f + iconSize * Mathf.floor(index / iconColumns),
            iconSize, iconSize);
            index++;
        }

        index = 0f;
        if (unit instanceof Payloadc payload && payload.payloads().any()) {
            for (Payload p : payload.payloads()) {
                Draw.rect(p.icon(),
                        unit.x - unit.hitSize() * 0.6f + 0.5f * iconSize * index,
                        unit.y + (unit.hitSize() / 2f) - 4f,
                        4f, 4f);
                index++;
            }
        }
        Draw.reset();
    }

    private static void drawLogic(Unit unit) {
        if (unit.controller() instanceof LogicAI logicai) {
            if (unitLogicMoveLine && Mathf.len(logicai.moveX - unit.x, logicai.moveY - unit.y) <= 1200f) {
                Lines.stroke(1f);
                Draw.color(0.2f, 0.2f, 1f, 0.9f);
                Lines.dashLine(unit.x, unit.y, logicai.moveX, logicai.moveY, (int) (Mathf.len(logicai.moveX - unit.x, logicai.moveY - unit.y) / 8));
                Lines.dashCircle(logicai.moveX, logicai.moveY, logicai.moveRad);
                Draw.reset();
            }
            if (unitLogicTimerBars) {
                Lines.stroke(2f);
                Draw.color(Pal.heal);
                Lines.line(unit.x - (unit.hitSize() / 2f), unit.y - (unit.hitSize() / 2f), unit.x - (unit.hitSize() / 2f), unit.y + unit.hitSize() * (logicai.controlTimer / LogicAI.logicControlTimeout - 0.5f));
                Draw.reset();
            }
        }
    }

    private static void drawBuildPlan(Unit unit) {
        if (unitBuildPlan && !unit.plans().isEmpty()) {
            int counter = 0;
            if (unit != player.unit()) {
                for (BuildPlan b : unit.plans()) {
                    unit.drawPlan(b, 0.5f);
                    counter += 1;
                    if (counter >= maxBuildPlans) break;
                }
            }
            counter = 0;
            Draw.color(Pal.gray);
            Lines.stroke(2f);
            float x = unit.x, y = unit.y, s = unit.hitSize / 2f;
            for (BuildPlan b : unit.plans()) {
                Tmp.v2.trns(Angles.angle(x, y, b.drawx(), b.drawy()), s);
                Tmp.v3.trns(Angles.angle(x, y, b.drawx(), b.drawy()), b.block.size * 2f);
                Lines.circle(b.drawx(), b.drawy(), b.block.size * 2f);
                Lines.line(x + Tmp.v2.x, y + Tmp.v2.y, b.drawx() - Tmp.v3.x, b.drawy() - Tmp.v3.y);
                x = b.drawx();
                y = b.drawy();
                s = b.block.size * 2f;
                counter += 1;
                if (counter >= maxBuildPlans) break;
            }

            counter = 0;
            Draw.color(unit.team.color);
            Lines.stroke(0.75f);
            x = unit.x;
            y = unit.y;
            s = unit.hitSize / 2f;
            for (BuildPlan b : unit.plans()) {
                Tmp.v2.trns(Angles.angle(x, y, b.drawx(), b.drawy()), s);
                Tmp.v3.trns(Angles.angle(x, y, b.drawx(), b.drawy()), b.block.size * 2f);
                Lines.circle(b.drawx(), b.drawy(), b.block.size * 2f);
                Draw.color(unit.team.color);
                Lines.line(x + Tmp.v2.x, y + Tmp.v2.y, b.drawx() - Tmp.v3.x, b.drawy() - Tmp.v3.y);
                x = b.drawx();
                y = b.drawy();
                s = b.block.size * 2f;
                counter += 1;
                if (counter >= maxBuildPlans) break;
            }
            Draw.reset();
        }
    }

    private static void drawHitBox(Unit unit) {
        if (unithitbox) {
            Draw.color(unit.team.color, 0.5f);
            Lines.circle(unit.x, unit.y, unit.hitSize / 2f);
            Draw.reset();
        }
    }

    private static void detailBuildMode() {
        if (!arcBuildInfo) return;
        if (control.input.droppingItem) {
            Color color = player.within(Core.input.mouseWorld(control.input.getMouseX(), control.input.getMouseY()), itemTransferRange) ? Color.gold : Color.red;
            drawNSideRegion(player.unit().x, player.unit().y, 3, player.unit().type.buildRange, player.unit().rotation, color, 0.25f, player.unit().stack.item.uiIcon, false);
        } else if (control.input.isBuilding || control.input.selectedBlock() || !player.unit().plans().isEmpty()) {
            drawNSideRegion(player.unit().x, player.unit().y, 3, player.unit().type.buildRange, player.unit().rotation, Pal.heal, 0.25f, Icon.wrench.getRegion(), true);
        }
    }

    public static void drawControlTurret() {
        if (player.unit() instanceof BlockUnitc unitc) {
            unitc.tile().drawSelect();
        }
    }
}
