package mindustryX.features;

import mindustry.content.StatusEffects;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ContinuousBulletType;
import mindustry.entities.bullet.PointLaserBulletType;
import mindustry.entities.pattern.ShootMulti;
import mindustry.entities.pattern.ShootPattern;
import mindustry.world.blocks.defense.turrets.Turret;

public class DPS {
    public static float estimateDPS(BulletType type, Turret turret) {
        float sum = -1;
        if(type instanceof ContinuousBulletType cbt && cbt.continuous) {
            sum = cbt.damage / cbt.damageInterval;
        } else if(type instanceof PointLaserBulletType plbt) {
            sum = plbt.damage / plbt.damageInterval;
        }

        if(sum <= 0) sum = estimateDPSSrc(type);
        return (type instanceof ContinuousBulletType || type instanceof PointLaserBulletType ? sum : (sum / turret.reload * type.reloadMultiplier)) * shots(type.shootPattern != null ? type.shootPattern : turret.shoot) * 60f;
    }

    private static float estimateDPSSrc(BulletType type) {
        if(type.spawnUnit != null){
            return type.spawnUnit.estimateDps();
        }
        if(type.despawnUnit != null){
            return type.despawnUnit.estimateDps();
        }

        float sum = type.damage + type.splashDamage;
        if(type.lightning > 0f){
            sum += type.lightning * type.lightningDamage;
        }
        if(type.fragBullet != null && type.fragBullet != type){
            sum += estimateDPSSrc(type.fragBullet) * type.fragBullets;
        }
        if(type.status != StatusEffects.none) {
            sum += (Math.max(0f, type.status.damage * Math.min(1f, type.statusDuration / 60f)) + (type.status.intervalDamageTime <= 0f ? 0f : type.status.intervalDamage / type.status.intervalDamageTime / 60f)) * Math.max(0f, Math.min(1f, type.statusChance));
        }
        for(var other : type.spawnBullets){
            sum += estimateDPSSrc(other);
        }
        if(type.intervalBullet != null){
            sum += Math.max(0f, estimateDPSSrc(type.intervalBullet) * type.intervalBullets / type.intervalDelay);
        }
        return sum;
    }

    public static int shots(ShootPattern pattern) {
        int cap = 0;
        if(pattern instanceof ShootMulti mul){
            for(ShootPattern dest : mul.dest){
                cap += shots(dest);
            }
            cap *= shots(mul.source);
        }
        cap += pattern.shots;
        return cap;
    }
}
