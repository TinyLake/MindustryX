package mindustry.world.blocks.power;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import mindustryX.features.*;

public class ImpactReactor extends PowerGenerator{
    public final int timerUse = timers++;
    public float warmupSpeed = 0.001f;
    public float itemDuration = 60f;

    public ImpactReactor(String name){
        super(name);
        hasPower = true;
        hasLiquids = true;
        liquidCapacity = 30f;
        hasItems = true;
        outputsPower = consumesPower = true;
        flags = EnumSet.of(BlockFlag.reactor, BlockFlag.generator);
        lightRadius = 115f;
        emitLight = true;
        envEnabled = Env.any;

        drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawPlasma(), new DrawDefault());

        explosionShake = 6f;
        explosionShakeDuration = 16f;
        explosionDamage = 1900 * 4;
        explosionMinWarmup = 0.3f;
        explodeEffect = Fx.impactReactorExplosion;
        explodeSound = Sounds.explosionbig;
    }

    private float warmupToTime(float warmup, float timeScale) {
        return Mathf.log(1f - warmupSpeed * timeScale, 1f - warmup);
    }

    private float timeToWarmup(float time, float timeScale) {
        return 1f - Mathf.pow(1 - warmupSpeed * timeScale, time);
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("power", (GeneratorBuild entity) -> new Bar(
                () -> entity.warmup() > 0.999f
                        ? Strings.format(Iconc.power+"@[lightgray](@%)[]",
                                Strings.autoFixed((entity.getPowerProduction() - consPower.usage) * 60 * entity.timeScale(), 1),
                                Strings.autoFixed(entity.productionEfficiency * 100, 1)
                        )
                        : Strings.format(Iconc.power+"@[lightgray](@%)[]|@s",
                                Strings.autoFixed((entity.getPowerProduction() - consPower.usage) * 60 * entity.timeScale(), 1),
                                Strings.autoFixed(entity.productionEfficiency * 100, 1),
                                Strings.autoFixed((warmupToTime(0.999f, entity.timeScale()) - warmupToTime(entity.warmup(), entity.timeScale())) / 60f, 1)
                        ),
                () -> Pal.powerBar,
                () -> entity.productionEfficiency)
        );
    }

    @Override
    public void setStats(){
        super.setStats();

        if(hasItems){
            stats.add(Stat.productionTime, itemDuration / 60f, StatUnit.seconds);
        }
        float startTime = warmupToTime(Mathf.pow(consPower.usage / powerProduction, 1f / 5f), 1f);
        stats.add(StatExt.warmupPartial, startTime / 60f, StatUnit.seconds);
        stats.add(StatExt.warmupTime, warmupToTime(0.999f, 1f) / 60f, StatUnit.seconds);
        float startConsPower = 0;
        for (int tick = 1;tick < startTime;tick++) {
            startConsPower += consPower.usage - Mathf.pow(timeToWarmup(tick, 1f), 5f) * powerProduction;
        }
        stats.add(StatExt.warmupPower, Mathf.ceil(startConsPower / 50f) * 50f, StatUnit.none);
    }

    public class ImpactReactorBuild extends GeneratorBuild{
        public float warmup, totalProgress;

        @Override
        public void updateTile(){
            if(efficiency >= 0.9999f && power.status >= 0.99f){
                boolean prevOut = getPowerProduction() <= consPower.requestedPower(this);

                warmup = Mathf.lerpDelta(warmup, 1f, warmupSpeed * timeScale);
                if(Mathf.equal(warmup, 1f, 0.001f)){
                    warmup = 1f;
                }

                if(!prevOut && (getPowerProduction() > consPower.requestedPower(this))){
                    Events.fire(Trigger.impactPower);
                }

                if(timer(timerUse, itemDuration / timeScale)){
                    consume();
                }
            }else{
                warmup = Mathf.lerpDelta(warmup, 0f, 0.01f);
            }

            totalProgress += warmup * Time.delta;

            productionEfficiency = Mathf.pow(warmup, 5f);
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public float totalProgress(){
            return totalProgress;
        }

        @Override
        public float ambientVolume(){
            return warmup;
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.heat) return warmup;
            return super.sense(sensor);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            warmup = read.f();
        }
    }
}
