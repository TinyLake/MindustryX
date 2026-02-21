package mindustryX.features;

import arc.util.*;
import mindustry.entities.abilities.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustryX.VarsX;

import static mindustry.Vars.*;

public class StatExt{
    public static Stat
    canOverdrive = new Stat("can_overdrive"),
    cost = new Stat("cost"),
    healthScaling = new Stat("health_scaling"),
    hardness = new Stat("hardness"),
    buildable = new Stat("buildable"),
    boilPoint = new Stat("boil_point"),
    dragMultiplier = new Stat("drag_multiplier"),//移动阻力倍率

    bufferCapacity = new Stat("buffer_capacity", StatCat.items),
    regenSpeed = new Stat("regen_speed", StatCat.function),//力墙 回复速度
    mend = new Stat("mend", StatCat.function),//治疗 修复量
    mendReload = new Stat("mend_reload", StatCat.function),//治疗 修复间隔
    mendSpeed = new Stat("mend_speed", StatCat.function),//治疗 修复速度
    warmupPartial = new Stat("warmup_partial", StatCat.power),//冲击 启动时间
    warmupTime = new Stat("warmup_time", StatCat.power),//冲击 完全启动时间
    warmupPower = new Stat("warmup_power", StatCat.power),//冲击 启动总耗电

    rotateSpeed = new Stat("rotate_speed", StatCat.movement),
    boostMultiplier = new Stat("boost_multiplier", StatCat.movement),
    drownTimeMultiplier = new Stat("drown_time_multiplier", StatCat.movement),
    mineLevel = new Stat("mine_level", StatCat.support),

    estimateDPS = new Stat("estimate_dps", StatCat.combat),
    aiController = new Stat("ai_controller", StatCat.combat),
    targets = new Stat("targets", StatCat.combat),
    ammoType = new Stat("ammo_type", StatCat.combat),
    ammoCapacity = new Stat("ammo_capacity", StatCat.combat);

    private static String statValue(Object value){
        if(value instanceof Number n){
            return "[stat]" + Strings.autoFixed(n.floatValue(), 1) + "[]";
        }
        return "[white]" + value + "[]";
    }

    public static @Nullable String description(Ability ability, UnitType unit){
        if(ability instanceof ForceFieldAbility a){
            return VarsX.bundle.shieldCapacity(
            statValue(a.max),
            statValue(a.radius / tilesize),
            statValue(a.regen * 60f),
            statValue(a.cooldown / 60f)
            );
        }else if(ability instanceof LiquidExplodeAbility a){
            float rad = Math.max(unit.hitSize / tilesize * a.radScale, 1);
            return VarsX.bundle.liquidExplode(
            statValue(1f / 3f * Math.PI * rad * rad * a.amount * a.radAmountScale),
            statValue(a.liquid.localizedName),
            statValue(a.liquid.emoji()),
            statValue(rad)
            );
        }else if(ability instanceof LiquidRegenAbility a){
            return VarsX.bundle.liquidRegen(
            statValue(a.slurpSpeed),
            statValue(a.liquid.localizedName),
            statValue(a.liquid.emoji()),
            statValue(a.slurpSpeed * a.regenPerSlurp),
            statValue(Math.PI * Math.pow(Math.max(unit.hitSize / tilesize * 0.6f, 1), 2) * a.slurpSpeed * a.regenPerSlurp)
            );
        }else if(ability instanceof MoveLightningAbility a){
            return VarsX.bundle.lightning(
            statValue(a.chance * 100),
            statValue(a.damage),
            statValue(a.length),
            statValue(a.maxSpeed)
            );
        }else if(ability instanceof SuppressionFieldAbility a){
            return VarsX.bundle.durationTiles(
            statValue(a.reload / 60f),
            statValue(a.range / tilesize)
            );
        }
        return null;
    }

    public static StatValue targets(BlockFlag[] targetFlags){
        return table -> {
            table.row();
            table.table(t -> {
                t.background(Styles.grayPanel);
                for(BlockFlag flag : targetFlags){
                    if(flag == null) continue;
                    t.add(flag.name()).width(150f).padBottom(5f);
                    int count = 0;
                    for(Block block : content.blocks()){
                        if(block.flags.contains(flag)){
                            if(count >= 3){
                                t.add("\uE813").width(30f);
                                break;
                            }else t.add(block.emoji()).width(30f);
                            count += 1;
                        }
                    }
                    t.row();
                }
            }).padLeft(12f);
        };
    }
}
