package mindustry.world.blocks.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustryX.features.*;

import static mindustry.Vars.*;

public class MendProjector extends Block{
    public final int timerUse = timers++;
    public Color baseColor = Color.valueOf("84f491");
    public Color phaseColor = Color.valueOf("ffd59e");
    public @Load("@-top") TextureRegion topRegion;
    public float reload = 250f;
    public float range = 60f;
    public float healPercent = 12f;
    public float phaseBoost = 12f;
    public float phaseRangeBoost = 50f;
    public float useTime = 400f;

    public MendProjector(String name){
        super(name);
        solid = true;
        update = true;
        group = BlockGroup.projectors;
        hasPower = true;
        hasItems = true;
        emitLight = true;
        lightRadius = 50f;
        suppressable = true;
        envEnabled |= Env.space;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setStats(){
        stats.timePeriod = useTime;
        super.setStats();

        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(StatExt.mend, healPercent, StatUnit.percent);
        stats.add(StatExt.mendReload, reload / 60f, StatUnit.seconds);
        stats.add(StatExt.mendSpeed, "@%/s", Strings.autoFixed(healPercent / (reload / 60f), 1));

        if(findConsumer(c -> c instanceof ConsumeItems) instanceof ConsumeItems cons){
            stats.remove(Stat.booster);
            stats.add(Stat.booster, StatValues.itemBoosters(
                "{0}x修复量",
                stats.timePeriod, (phaseBoost + healPercent) / healPercent, phaseRangeBoost,
                cons.items, this::consumesItem)
            );
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("charge", (MendBuild entity) -> new Bar(() -> ("充能: " + (int)entity.charge + " / " + reload), () -> Pal.items, () -> (entity.charge / reload)));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, baseColor);

        indexer.eachBlock(player.team(), x * tilesize + offset, y * tilesize + offset, range + phaseRangeBoost, other -> true, other -> Drawf.selected(other, Tmp.c1.set(phaseColor).a(Mathf.absin(4f, 1f))));

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range + phaseRangeBoost, phaseColor);

        indexer.eachBlock(player.team(), x * tilesize + offset, y * tilesize + offset, range, other -> true, other -> Drawf.selected(other, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f))));
    }

    public class MendBuild extends Building implements Ranged{
        public float heat, charge = Mathf.random(reload), phaseHeat, smoothEfficiency;

        @Override
        public float range(){
            return range;
        }

        @Override
        public void updateTile(){
            boolean canHeal = !checkSuppression();

            smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, efficiency, 0.08f);
            heat = Mathf.lerpDelta(heat, efficiency > 0 && canHeal ? 1f : 0f, 0.08f);
            charge += heat * delta();

            phaseHeat = Mathf.lerpDelta(phaseHeat, optionalEfficiency, 0.1f);

            if(optionalEfficiency > 0 && timer(timerUse, useTime) && canHeal){
                consume();
            }

            if(charge >= reload && canHeal){
                float realRange = range + phaseHeat * phaseRangeBoost;
                charge = 0f;

                indexer.eachBlock(this, realRange, b -> b.damaged() && !b.isHealSuppressed(), other -> {
                    other.heal(other.maxHealth() * (healPercent + phaseHeat * phaseBoost) / 100f * efficiency);
                    other.recentlyHealed();
                    Fx.healBlockFull.at(other.x, other.y, other.block.size, baseColor, other.block);
                });
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress) return Mathf.clamp(charge / reload);
            return super.sense(sensor);
        }

        @Override
        public void drawSelect(){
            float realRange = range + phaseHeat * phaseRangeBoost;

            indexer.eachBlock(this, realRange, other -> true, other -> Drawf.selected(other, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f))));

            Drawf.dashCircle(x, y, realRange, baseColor);
        }

        @Override
        public void draw(){
            super.draw();

            float realRange = range + phaseHeat * phaseRangeBoost;
            if(status() == BlockStatus.active && (float)Core.settings.getInt("mend_zone") > 2f){
                if (phaseHeat>0.2){Draw.color(Color.valueOf("00ff55"), (float)Core.settings.getInt("mend_zone") / 100f);}
                else {Draw.color(Color.valueOf("66ff99"),(float)Core.settings.getInt("mend_zone") / 100f);}

                Lines.dashCircle(x, y, realRange);
            }


            float f = 1f - (Time.time / 100f) % 1f;

            Draw.color(baseColor, phaseColor, phaseHeat);
            Draw.alpha(heat * Mathf.absin(Time.time, 50f / Mathf.PI2, 1f) * 0.5f);
            Draw.rect(topRegion, x, y);
            Draw.alpha(1f);
            Lines.stroke((2f * f + 0.2f) * heat);
            Lines.square(x, y, Math.min(1f + (1f - f) * size * tilesize / 2f, size * tilesize/2f));

            Draw.reset();
        }

        @Override
        public void drawBars() {
            super.drawBars();
            if (Core.settings.getBool("blockBars_mend")) {
                Draw.color(Color.black, 0.3f);
                Lines.stroke(4f);
                Lines.line(x - block.size * tilesize / 2f * 0.6f, y + block.size * tilesize / 2.5f,
                        x + block.size * tilesize / 2f * 0.6f, y + block.size * tilesize / 2.5f);
                Draw.color(Pal.heal, 1f);
                Lines.stroke(2f);
                Lines.line(x - block.size * tilesize / 2f * 0.6f, y + block.size * tilesize / 2.5f,
                        x + 0.6f * (charge / reload - 0.5f) * block.size * tilesize, y + block.size * tilesize / 2.5f);
                Draw.color();
            }
        }

        @Override
        public void drawLight(){
            Drawf.light(x, y, lightRadius * smoothEfficiency, baseColor, 0.7f * smoothEfficiency);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
            write.f(phaseHeat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
            phaseHeat = read.f();
        }
    }
}
