package mindustry.world.blocks.defense.turrets;

import arc.Core;
import arc.math.*;
import arc.scene.ui.layout.Table;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ReloadTurret extends BaseTurret{
    public float reload = 10f;

    public ReloadTurret(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();

        if(coolant != null){
            stats.remove(Stat.booster);
            stats.add(Stat.booster, StatValues.boosters(reload, coolant.amount, coolantMultiplier, true, l -> l.coolant && consumesLiquid(l)));
        }
    }

    public class ReloadTurretBuild extends BaseTurretBuild{
        public float reloadCounter;

        protected void updateCooling(){
            if(reloadCounter < reload && coolant != null && coolant.efficiency(this) > 0 && efficiency > 0){
                float capacity = coolant instanceof ConsumeLiquidFilter filter ? filter.getConsumed(this).heatCapacity : 1f;
                float amount = coolant.amount * coolant.efficiency(this);
                coolant.update(this);
                reloadCounter += amount * edelta() * capacity * coolantMultiplier;

                if(Mathf.chance(0.06 * amount)){
                    coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                }
            }
        }

        protected float baseReloadSpeed(){
            return efficiency;
        }

        @Override
        public void displayBars(Table bars){
            super.displayBars(bars);
            //bar for shoot cd
            bars.add(new Bar(() -> Core.bundle.format("stat.reloadDetail", (int)(reloadCounter * 100 / reload)), () -> Pal.ammo, () -> (float)(reloadCounter / reload)));
            bars.row();
        }

    }
}
