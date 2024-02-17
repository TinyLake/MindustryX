package mindustryX.features;

import arc.util.*;
import mindustry.entities.abilities.*;
import mindustry.entities.pattern.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.tilesize;

public class StatExt{
    public static Stat
    unitrange = new Stat("unit_range"),
    canOverdrive = new Stat("can_overdrive"),
    cost = new Stat("cost"),
    healthScaling = new Stat("health_scaling"),
    hardness = new Stat("hardness"),
    buildable = new Stat("buildable"),
    boilPoint = new Stat("boil_point"),
    dragMultiplier = new Stat("drag_multiplier"),//移动阻力倍率

    bufferCapacity = new Stat("buffer_capacity", StatCat.items),
    sepOutput = new Stat("sep_output", StatCat.crafting),
    regenSpeed = new Stat("regen_speed", StatCat.function),//力墙 回复速度
    regenSpeedBroken = new Stat("regen_speed_broken", StatCat.function),//力墙 过热时回复速度
    mend = new Stat("mend", StatCat.function),//治疗 修复量
    mendReload = new Stat("mend_reload", StatCat.function),//治疗 修复间隔
    mendSpeed = new Stat("mend_speed", StatCat.function),//治疗 修复速度
    warmupPartial = new Stat("warmup_partial", StatCat.power),//冲击 启动时间
    warmupTime = new Stat("warmup_time", StatCat.power),//冲击 完全启动时间
    warmupPower = new Stat("warmup_power", StatCat.power),//冲击 启动总耗电

    rotateSpeed = new Stat("rotate_speed", StatCat.movement),
    boostMultiplier = new Stat("boost_multiplier", StatCat.movement),
    drownTimeMultiplier = new Stat("drownTime_multiplier", StatCat.movement),
    mineLevel = new Stat("mine_level", StatCat.support),
    unitItemCapacity = new Stat("unitItem_capacity", StatCat.support),

    crushDamage = new Stat("crush_damage", StatCat.combat),//碾压伤害(每格)
    estimateDPS = new Stat("estimate_dps", StatCat.combat),
    aiController = new Stat("ai_controller", StatCat.combat),
    targets = new Stat("targets", StatCat.combat),
    ammoType = new Stat("ammo_type", StatCat.combat),
    ammoCapacity = new Stat("ammo_capacity", StatCat.combat);

    public static String abilityFormat(String format, Object... values){
        for(int i = 0; i < values.length; i++){
            if(values[i] instanceof Number n)
                values[i] = "[stat]" + Strings.autoFixed(n.floatValue(), 1) + "[]";
            else
                values[i] = "[white]" + values[i] + "[]";
        }
        return Strings.format("[lightgray]" + format.replace("~", "[accent]~[]"), values);
    }

    public static @Nullable String description(Ability ability, UnitType unit){
        if(ability instanceof ForceFieldAbility a){
            return abilityFormat("@盾容~@格~@恢复~@s冷却",
            a.max, a.radius / tilesize, a.regen * 60f, a.cooldown / 60f
            );
        }else if(ability instanceof LiquidExplodeAbility a){
            float rad = Math.max(unit.hitSize / tilesize * a.radScale, 1);
            return abilityFormat("总计@@@~@格半径",
            1f / 3f * Math.PI * rad * rad * a.amount * a.radAmountScale,// 1/3πr²h
            a.liquid.localizedName, a.liquid.emoji(), rad
            );
        }else if(ability instanceof LiquidRegenAbility a){
            return abilityFormat("每格吸收@/s@@~@/s回血~最大@/s",
            a.slurpSpeed, a.liquid.localizedName, a.liquid.emoji(), a.slurpSpeed * a.regenPerSlurp,
            Math.PI * Math.pow(Math.max(unit.hitSize / tilesize * 0.6f, 1), 2) * a.slurpSpeed * a.regenPerSlurp
            );
        }else if(ability instanceof MoveLightningAbility a){
            return abilityFormat("闪电@概率~@伤害~@长度 @x速度",
            a.chance * 100, a.damage, a.length, a.maxSpeed
            );
        }else if(ability instanceof SuppressionFieldAbility a){
            return abilityFormat("@s~@格",
            a.reload / 60f, a.range / tilesize
            );
        }
        return null;
    }

    public static int totalShots(ShootPattern pattern){
        if(pattern instanceof ShootHelix){
            return pattern.shots * 2;
        }else if(pattern instanceof ShootMulti s){
            int total = 0;
            for(var p : s.dest){
                total += totalShots(p);
            }
            return pattern.shots * total;
        }
        return pattern.shots;
    }
}
