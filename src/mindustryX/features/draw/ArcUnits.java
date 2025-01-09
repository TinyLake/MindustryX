package mindustryX.features.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.payloads.*;
import mindustryX.features.*;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.*;
import static mindustry.Vars.*;

//move from mindustry.arcModule.draw.ARCUnits
public class ArcUnits{
    private static final int maxBuildPlans = 100;
    private static boolean alwaysShowUnitRTSAi, unitHealthBar, unitLogicMoveLine, unitLogicTimerBars, unithitbox, unitBuildPlan;
    private static float minHealthDrawStat;
    private static float unitWeaponRange, unitWeaponRangeAlpha;
    public static boolean selectedUnitsFlyer, selectedUnitsLand;

    private static float curStroke;
    private static int unitTargetType, superUnitEffect;
    private static boolean arcBuildInfo;

    static{
        // 减少性能开销
        Events.run(EventType.Trigger.update, () -> {
            alwaysShowUnitRTSAi = Core.settings.getBool("alwaysShowUnitRTSAi");
            unitHealthBar = Core.settings.getBool("unitHealthBar");
            unitLogicMoveLine = Core.settings.getBool("unitLogicMoveLine");
            unitLogicTimerBars = Core.settings.getBool("unitLogicTimerBars");
            unithitbox = Core.settings.getBool("unithitbox");
            unitBuildPlan = Core.settings.getBool("unitbuildplan");

            minHealthDrawStat = Core.settings.getInt("unitBarDrawMinHealth");

            unitWeaponRange = Core.settings.getInt("unitWeaponRange") * tilesize;
            unitWeaponRangeAlpha = Core.settings.getInt("unitWeaponRangeAlpha") / 100f;

            selectedUnitsFlyer = control.input.selectedUnits.contains(Flyingc::isFlying);
            selectedUnitsLand = control.input.selectedUnits.contains(unit -> !unit.isFlying());

            curStroke = (float)Core.settings.getInt("playerEffectCurStroke") / 10f;
            unitTargetType = Core.settings.getInt("unitTargetType");
            superUnitEffect = Core.settings.getInt("superUnitEffect");
            arcBuildInfo = Core.settings.getBool("arcBuildInfo");
        });
    }

    public static void draw(Unit unit){
        if(unit.isPlayer()){
            if(superUnitEffect > 0 && (unit.isLocal() || superUnitEffect == 2)) drawAimRange(unit);
            if(unitTargetType > 0) drawAimTarget(unit);
            if(arcBuildInfo && unit.isLocal()) drawBuildRange();
        }
        if((RenderExt.unitHideExcludePlayers && unit.isPlayer()) || (unit.maxHealth + unit.shield) >= minHealthDrawStat){
            Draw.z(Draw.z() + 0.1f);
            if(unit.team() == player.team() || RenderExt.showOtherInfo){
                if(unitWeaponRange > 0) drawWeaponRange(unit);
                if(alwaysShowUnitRTSAi) drawRTSAI(unit);
                if(unitHealthBar) drawHealthBar(unit);
                if(unit.controller() instanceof LogicAI ai){
                    if(unitLogicMoveLine) drawLogicMove(unit, ai);
                    if(unitLogicTimerBars) drawLogicTimer(unit, ai);
                }
                if(unitBuildPlan) drawBuildPlan(unit);
            }
            if(unithitbox) drawHitBox(unit);
        }
    }

    private static void drawAimRange(Unit unit){
        if(curStroke <= 0) return;
        Color effectColor = unit.controller() == player ? RenderExt.playerEffectColor : unit.team.color;

        float z = Draw.z();
        Draw.z(Layer.effect - 2f);
        Draw.color(effectColor);
        Lines.stroke(curStroke);

        for(int i = 0; i < 5; i++){
            float rot = unit.rotation + i * 360f / 5 + Time.time * 0.5f;
            Lines.arc(unit.x, unit.y, unit.type.maxRange, 0.14f, rot, (int)(50 + unit.type.maxRange / 10));
        }
        Draw.reset();
        Draw.z(z);
    }

    private static void drawAimTarget(Unit unit){
        Color effectColor = unit.controller() == player ? RenderExt.playerEffectColor : unit.team.color;
        float z = Draw.z();
        Draw.z(Layer.effect);

        Draw.color(effectColor, 0.8f);
        Lines.line(unit.x, unit.y, unit.aimX, unit.aimY);
        switch(unitTargetType){
            case 1:
                Lines.dashCircle(unit.aimX, unit.aimY, 8);
                break;
            case 2:
                Drawf.target(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                break;
            case 3:
            case 4:
            case 5:
                Draw.color(effectColor, 0.7f);
                if(unitTargetType == 3) Lines.poly(unit.aimX, unit.aimY, 4, 6f, Time.time * 1.5f);
                if(unitTargetType == 4) Lines.circle(unit.aimX, unit.aimY, 6f);
                Lines.spikes(unit.aimX, unit.aimY, 3f / 7f * 6f, 6f / 7f * 6f, 4, Time.time * 1.5f);
                break;
        }

        Draw.color();
        Draw.z(z);
    }

    private static void drawWeaponRange(Unit unit){
        if(unitWeaponRange == 0 || unitWeaponRangeAlpha == 0) return;
        if(unitWeaponRange == 30){
            drawWeaponRange(unit, unitWeaponRangeAlpha);
        }else if(unit.team != player.team()){
            boolean canHitPlayer = !player.dead() && player.unit().hittable() && (player.unit().isFlying() ? unit.type.targetAir : unit.type.targetGround)
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

    private static void drawRTSAI(Unit unit){
        if(!control.input.commandMode && unit.isCommandable() && unit.command().command != null){
            Draw.z(Layer.effect);
            CommandAI ai = unit.command();
            if(ai.attackTarget != null){
                Draw.color(unit.team.color);
                if(ai.targetPos != null)
                    drawLimitLineColor(unit, ai.attackTarget, unit.hitSize / 2f, 3.5f, unit.team.color);
                Drawf.target(ai.attackTarget.getX(), ai.attackTarget.getY(), 6f, unit.team.color);
            }else if(ai.targetPos != null){
                Draw.color(unit.team.color);
                drawLimitLineColor(unit, ai.targetPos, unit.hitSize / 2f, 3.5f, unit.team.color);
                Draw.color(unit.team.color);
                Drawf.square(ai.targetPos.getX(), ai.targetPos.getY(), 3.5f, unit.team.color);
            }
            Draw.reset();
        }
    }

    private static void drawHealthBar(Unit unit){
        Draw.z(Layer.shields + 6f);
        float y_corr = 0f;
        if(!player.dead() && unit.hitSize < 30f && unit.hitSize > 20f && unit.controller().isBeingControlled(player.unit())) y_corr = 2f;
        if(unit.health < unit.maxHealth){
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
        if(unit.shield > 0 && unit.shield < 1e20){
            for(int didgt = 1; didgt <= Mathf.digits((int)(unit.shield / unit.maxHealth)) + 1; didgt++){
                Draw.color(Pal.shield, 0.8f);
                float shieldAmountScale = unit.shield / (unit.maxHealth * Mathf.pow(10f, (float)didgt - 1f));
                if(didgt > 1){
                    Lines.line(unit.x - unit.hitSize() * 0.6f,
                    unit.y + (unit.hitSize() / 2f) + (float)didgt * 2f + y_corr,
                    unit.x + unit.hitSize() * ((Mathf.ceil((shieldAmountScale - Mathf.floor(shieldAmountScale)) * 10f) - 1f + 0.0001f) * 1.2f * (1f / 9f) - 0.6f),
                    unit.y + (unit.hitSize() / 2f) + (float)didgt * 2f + y_corr);
                    //(s-1)*(1/9)because line(0) will draw length of 1
                }else{
                    Lines.line(unit.x - unit.hitSize() * 0.6f,
                    unit.y + (unit.hitSize() / 2f) + (float)didgt * 2f + y_corr,
                    unit.x + unit.hitSize() * ((shieldAmountScale - Mathf.floor(shieldAmountScale) - 0.001f) * 1.2f - 0.6f),
                    unit.y + (unit.hitSize() / 2f) + (float)didgt * 2f + y_corr);
                }
            }
        }
        Draw.reset();

        float oldZ = Draw.z();
        Draw.z(oldZ + 0.1f);//MDTX: There no replace for effect.uiIcon, so we offset the layer.
        float index = 0f;
        float iconSize = 4f;
        int iconColumns = Math.max((int)(unit.hitSize() / (iconSize + 1f)), 4);
        float iconWidth = Math.min(unit.hitSize() / iconColumns, iconSize + 1f);
        for(var entry : unit.statuses()){
            Draw.rect(entry.effect.uiIcon,
            unit.x - unit.hitSize() * 0.6f + iconWidth * (index % iconColumns),
            unit.y + (unit.hitSize() / 2f) + 3f + iconSize * Mathf.floor(index / iconColumns),
            iconSize, iconSize);
            index++;
        }
        Draw.z(oldZ);

        index = 0f;
        if(unit instanceof Payloadc payload && payload.payloads().any()){
            for(Payload p : payload.payloads()){
                Draw.rect(p.content().fullIcon,
                unit.x - unit.hitSize() * 0.6f + 0.5f * iconSize * index,
                unit.y + (unit.hitSize() / 2f) - 4f,
                4f, 4f);
                index++;
            }
        }
        Draw.reset();
    }

    private static void drawLogicMove(Unit unit, LogicAI ai){
        if(Mathf.len(ai.moveX - unit.x, ai.moveY - unit.y) > 1200f) return;

        Draw.color(0.2f, 0.2f, 1f, 0.9f);
        Lines.dashLine(unit.x, unit.y, ai.moveX, ai.moveY, (int)(Mathf.len(ai.moveX - unit.x, ai.moveY - unit.y) / 8));
        Lines.dashCircle(ai.moveX, ai.moveY, ai.moveRad);
        Draw.color();
    }

    private static void drawLogicTimer(Unit unit, LogicAI ai){
        Draw.color(Pal.heal);
        Lines.stroke(2f);
        Lines.line(unit.x - (unit.hitSize() / 2f), unit.y - (unit.hitSize() / 2f), unit.x - (unit.hitSize() / 2f), unit.y + unit.hitSize() * (ai.controlTimer / LogicAI.logicControlTimeout - 0.5f));
        Draw.reset();
    }

    private static void drawBuildPlan(Unit unit){
        if(unit.plans().isEmpty()) return;
        int counter = 0;
        if(unit != player.unit()){
            for(BuildPlan b : unit.plans()){
                unit.drawPlan(b, 0.5f);
                counter += 1;
                if(counter >= maxBuildPlans) break;
            }
        }
        counter = 0;
        Draw.color(Pal.gray);
        Lines.stroke(2f);
        float x = unit.x, y = unit.y, s = unit.hitSize / 2f;
        for(BuildPlan b : unit.plans()){
            Tmp.v2.trns(Angles.angle(x, y, b.drawx(), b.drawy()), s);
            Tmp.v3.trns(Angles.angle(x, y, b.drawx(), b.drawy()), b.block.size * 2f);
            Lines.circle(b.drawx(), b.drawy(), b.block.size * 2f);
            Lines.line(x + Tmp.v2.x, y + Tmp.v2.y, b.drawx() - Tmp.v3.x, b.drawy() - Tmp.v3.y);
            x = b.drawx();
            y = b.drawy();
            s = b.block.size * 2f;
            counter += 1;
            if(counter >= maxBuildPlans) break;
        }

        counter = 0;
        Draw.color(unit.team.color);
        Lines.stroke(0.75f);
        x = unit.x;
        y = unit.y;
        s = unit.hitSize / 2f;
        for(BuildPlan b : unit.plans()){
            Tmp.v2.trns(Angles.angle(x, y, b.drawx(), b.drawy()), s);
            Tmp.v3.trns(Angles.angle(x, y, b.drawx(), b.drawy()), b.block.size * 2f);
            Lines.circle(b.drawx(), b.drawy(), b.block.size * 2f);
            Draw.color(unit.team.color);
            Lines.line(x + Tmp.v2.x, y + Tmp.v2.y, b.drawx() - Tmp.v3.x, b.drawy() - Tmp.v3.y);
            x = b.drawx();
            y = b.drawy();
            s = b.block.size * 2f;
            counter += 1;
            if(counter >= maxBuildPlans) break;
        }
        Draw.reset();
    }

    private static void drawHitBox(Unit unit){
        Draw.color(unit.team.color, 0.5f);
        Lines.circle(unit.x, unit.y, unit.hitSize / 2f);
        Draw.color();
    }

    private static void drawBuildRange(){
        if(control.input.droppingItem){
            Color color = player.within(Core.input.mouseWorld(control.input.getMouseX(), control.input.getMouseY()), itemTransferRange) ? Color.gold : Color.red;
            drawNSideRegion(player.unit().x, player.unit().y, 3, player.unit().type.buildRange, player.unit().rotation, color, 0.25f, player.unit().stack.item.fullIcon, false);
        }else if(control.input.isBuilding || control.input.selectedBlock() || !player.unit().plans().isEmpty()){
            drawNSideRegion(player.unit().x, player.unit().y, 3, player.unit().type.buildRange, player.unit().rotation, Pal.heal, 0.25f, Icon.wrench.getRegion(), true);
        }
    }

    public static void drawNSideRegion(float x, float y, int n, float range, float rotation, Color color, float fraction, TextureRegion region, boolean regionColor){
        Draw.z(Layer.effect - 2f);
        color(color);

        stroke(2f);

        for(int i = 0; i < n; i++){
            float frac = 360f * (1 - fraction * n) / n / 2;
            float rot = rotation + i * 360f / n + frac;
            if(!regionColor){
                color(color);
                arc(x, y, range, 0.25f, rot, (int)(50 + range / 10));
                color();
            }else{
                arc(x, y, range, 0.25f, rot, (int)(50 + range / 10));
            }
            Draw.rect(region, x + range * Mathf.cos((float)Math.toRadians(rot - frac)), y + range * Mathf.sin((float)Math.toRadians(rot - frac)), 12f, 12f);
        }
        Draw.reset();
    }

    private static void drawLimitLineColor(Position start, Position dest, float len1, float len2, Color color){
        Tmp.v1.set(dest).sub(start).setLength(len1);
        Tmp.v2.set(Tmp.v1).scl(-1f).setLength(len2);

        Drawf.line(color, start.getX() + Tmp.v1.x, start.getY() + Tmp.v1.y, dest.getX() + Tmp.v2.x, dest.getY() + Tmp.v2.y);
    }
}
