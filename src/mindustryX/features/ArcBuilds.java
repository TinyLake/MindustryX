package mindustryX.features;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustryX.features.draw.*;

import static mindustry.Vars.*;

//move from mindustry.arcModule.draw.ARCBuilds
public class ArcBuilds{
    private static boolean targetAir = false, targetGround = false, canShoot = false;
    private static boolean turretForceShowRange = false;
    private static int turretShowRange = 0, turretAlertRange;
    private static boolean showTurretAmmo = false, showTurretAmmoAmount = false;
    public static boolean blockWeaponTargetLine = false;
    private static boolean blockWeaponTargetLineWhenIdle = false;

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
        Draw.color(build.team.color, 0.6f);
        Lines.circle(build.x, build.y, build.range());
        Draw.color();
    }

    public static void turretDraw(BaseTurret.BaseTurretBuild build){
        if(build == null || !(build.team == player.team() || RenderExt.showOtherInfo)) return;

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
                boolean canHitPlayer = !player.dead() && player.unit().hittable() && (player.unit().isFlying() ? targetAir : targetGround)
                && build.within(player.unit().x, player.unit().y, build.range() + turretAlertRange);
                boolean canHitMouse = build.within(Core.input.mouseWorldX(), Core.input.mouseWorldY(), build.range() + turretAlertRange);
                boolean canHitCommand = control.input.commandMode && ((ArcUnits.selectedUnitsFlyer && targetAir) || (ArcUnits.selectedUnitsLand && targetGround));
                boolean canHitPlans = (control.input.block != null || control.input.selectPlans.size > 0) && targetGround;
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

                Draw.rect(lastAmmo.fullIcon, ammoX, ammoY, size, size);

                float leftAmmo = Mathf.lerp(0, 1, Math.min(1f, (float)entry.amount / ((ItemTurret)it.block).maxAmmo));
                if(leftAmmo < 0.75f && showTurretAmmoAmount){
                    Draw.color(lastAmmo.color, 0.5f);
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

    public static void turretPlaceDraw(float x, float y, Turret block){
        float iconSize = 6f + 2f * block.size, range = block.range;
        ObjectMap<? extends UnlockableContent, BulletType> ammoTypes;
        if(block instanceof ContinuousLiquidTurret t){
            ammoTypes = t.ammoTypes;
        }else if(block instanceof LiquidTurret t){
            ammoTypes = t.ammoTypes;
        }else if(block instanceof ItemTurret t){
            ammoTypes = t.ammoTypes;
        }else if(block instanceof PowerTurret){
            float oldZ = Draw.z();
            Draw.z(oldZ + 0.01f);//MDTX: There no replace for Icon.power, so we offset the layer.
            turretBulletDraw(x, y, Icon.power.getRegion(), iconSize, range, 0f);
            Draw.z(oldZ);
            return;
        }else return;

        int drawIndex = 0;
        for(var e : ammoTypes.entries()){
            var item = e.key;
            var bulletType = e.value;
            drawIndex += 1;
            if(!item.unlockedNow()) return;
            Drawf.dashCircle(x, y, range + bulletType.rangeChange, Pal.placing);
            turretBulletDraw(x, y, item.fullIcon, iconSize, range + bulletType.rangeChange, (float)drawIndex / ammoTypes.size);
        }
    }

    public static void turretSelectDraw(TurretBuild build){
        TextureRegion currentAmmo = null;
        if(build.block instanceof ContinuousLiquidTurret || build.block instanceof LiquidTurret){
            if(build.liquids.currentAmount() > 0){
                currentAmmo = build.liquids.current().fullIcon;
            }
        }else if(build.block instanceof ItemTurret){
            if(build.hasAmmo()){
                currentAmmo = ((ItemTurret.ItemEntry)build.ammo.peek()).item.fullIcon;
            }
        }else if(build.block instanceof PowerTurret){
            currentAmmo = Icon.power.getRegion();
        }else return;
        if(currentAmmo == null){
            turretPlaceDraw(build.x, build.y, (Turret)build.block);
            return;
        }
        float iconSize = 6f + 2f * build.block.size;
        turretBulletDraw(build.x, build.y, currentAmmo, iconSize, build.range(), 0);
    }

    private static void turretBulletDraw(float x, float y, TextureRegion icon, float iconSize, float range, float rotOffset){
        for(int i = 0; i < 4; i++){
            float rot = (i + rotOffset) * 90f + Time.time * 0.3f;
            Draw.rect(icon,
            x + (Mathf.sin((float)Math.toRadians(rot)) * (range)),
            y + (Mathf.cos((float)Math.toRadians(rot)) * (range)),
            iconSize, iconSize, -rot);
        }
    }
}
