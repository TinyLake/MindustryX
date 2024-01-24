package mindustry.arcModule.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.arcModule.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.draw.ARCUnits.*;

public class ARCBuilds{
    static boolean targetAir = false, targetGround = false, canShoot = false;
    static boolean turretForceShowRange = false;

    static int turretShowRange = 0, turretAlertRange;

    static boolean canHitPlayer = false, canHitCommand = false, canHitPlans = false, canHitMouse = false;

    static boolean showTurretAmmo = false, showTurretAmmoAmount = false;

    static boolean blockWeaponTargetLine = false, blockWeaponTargetLineWhenIdle = false;

    static{
        // 减少性能开销
        Events.run(EventType.Trigger.update, () -> {
            turretForceShowRange = Core.settings.getBool("turretForceShowRange");
            turretShowRange = Core.settings.getInt("turretShowRange");

            turretAlertRange = Core.settings.getInt("turretAlertRange") * tilesize;

            showTurretAmmo = Core.settings.getBool("showTurretAmmo");
            showTurretAmmoAmount = Core.settings.getBool("showTurretAmmoAmount");

            blockWeaponTargetLine = Core.settings.getBool("blockWeaponTargetLine");
            blockWeaponTargetLineWhenIdle = Core.settings.getBool("blockWeaponTargetLineWhenIdle");
        });
    }

    private static void drawRange(BaseTurret.BaseTurretBuild build){
        Draw.z(Layer.turret - 0.8f);
        //Draw.color(build.team.color, 0.05f);
        //Fill.circle(build.x, build.y, build.range());
        Draw.color(build.team.color, 0.6f);
        Lines.circle(build.x, build.y, build.range());
        Draw.reset();
    }

    public static void arcTurret(BaseTurret.BaseTurretBuild build){
        if(build == null || !ARCVars.arcInfoControl(build.team)) return;
        Draw.z(Layer.turret);

        Vec2 targetPos = Vec2.ZERO;
        if(build.block instanceof Turret t){
            targetAir = t.targetAir;
            targetGround = t.targetGround;
            targetPos = ((Turret.TurretBuild)build).targetPos;
            canShoot = ((Turret.TurretBuild)build).hasAmmo();
        }else if(build.block instanceof TractorBeamTurret t){
            targetAir = t.targetAir;
            targetGround = t.targetGround;
            Unit target = ((TractorBeamTurret.TractorBeamBuild)build).target;
            if(target != null){
                targetPos = Tmp.v1.set(target.x, target.y);
            }
            canShoot = build.potentialEfficiency > 0;
        }
        if(build instanceof PowerTurret.PowerTurretBuild){
            canShoot = build.efficiency > 0;
        }

        if(turretForceShowRange || canShoot){
            if((turretShowRange == 3 || (turretShowRange == 2 && targetAir) || (turretShowRange == 1 && targetGround)))
                drawRange(build);
            else if(turretAlertRange > 0 && build.team != player.team()){
                canHitPlayer = !player.unit().isNull() && player.unit().hittable() && (player.unit().isFlying() ? targetAir : targetGround)
                && build.within(player.unit().x, player.unit().y, build.range() + turretAlertRange);
                canHitMouse = build.within(Core.input.mouseWorldX(), Core.input.mouseWorldY(), build.range() + turretAlertRange);
                canHitCommand = control.input.commandMode && ((selectedUnitsFlyer && targetAir) || (selectedUnitsLand && targetGround));
                canHitPlans = (control.input.block != null || control.input.selectPlans.size > 0) && targetGround;
                if(canHitPlayer || (canHitMouse && (canHitCommand || canHitPlans))) drawRange(build);
            }

            if(showTurretAmmo && build instanceof ItemTurret.ItemTurretBuild it && it.ammo.any()){
                //lc参考miner代码
                ItemTurret.ItemEntry entry = (ItemTurret.ItemEntry)it.ammo.peek();
                Item lastAmmo = entry.item;

                Draw.z(Layer.turret + 0.1f);

                float size = Math.max(4f, build.block.size * tilesize / 2.5f);
                float ammoX = build.x - (build.block.size * tilesize / 2.0F) + (size / 2);
                float ammoY = build.y - (build.block.size * tilesize / 2.0F) + (size / 2);

                Draw.rect(lastAmmo.uiIcon, ammoX, ammoY, size, size);

                float leftAmmo = Mathf.lerp(0, 1, Math.min(1f, (float)entry.amount / ((ItemTurret)it.block).maxAmmo));
                if(leftAmmo < 0.75f && showTurretAmmoAmount){
                    Draw.alpha(0.5f);
                    Draw.color(lastAmmo.color);
                    Lines.stroke(Lines.getStroke() * build.block.size * 0.5f);
                    Lines.arc(ammoX, ammoY, size * 0.5f, leftAmmo);
                }

                Draw.reset();
            }
            if(targetPos.x != 0 && targetPos.y != 0 && blockWeaponTargetLine && Mathf.len(targetPos.x - build.x, targetPos.y - build.y) <= 1500f){
                if(!(build instanceof Turret.TurretBuild) || ((Turret.TurretBuild)build).isShooting() || ((Turret.TurretBuild)build).isControlled()){
                    Draw.color(1f, 0.2f, 0.2f, 0.8f);
                    Lines.stroke(1.5f);
                    Lines.line(build.x, build.y, targetPos.x, targetPos.y);
                    Lines.dashCircle(targetPos.x, targetPos.y, 8);
                }else if(blockWeaponTargetLineWhenIdle){
                    Draw.color(1f, 1f, 1f, 0.3f);
                    Lines.stroke(1.5f);
                    Lines.line(build.x, build.y, targetPos.x, targetPos.y);
                    Lines.dashCircle(targetPos.x, targetPos.y, 8);
                }
            }
        }
    }
}
