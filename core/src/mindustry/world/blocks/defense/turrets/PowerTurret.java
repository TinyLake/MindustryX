package mindustry.world.blocks.defense.turrets;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.Time;
import mindustry.entities.bullet.*;
import mindustry.gen.Icon;
import mindustry.logic.*;
import mindustry.world.meta.*;

import static mindustry.Vars.tilesize;

public class PowerTurret extends Turret{
    public BulletType shootType;

    public PowerTurret(String name){
        super(name);
        hasPower = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.ammo, StatValues.ammo(ObjectMap.of(this, shootType)));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);


        if(Core.settings.getBool("arcTurretPlacementItem")) {
            float iconSize = 6f + 2f * size;
            for (int i = 0; i < 4; i++) {
                float  rot = i / 4f * 360f + Time.time * 0.5f;
                Draw.rect(Icon.power.getRegion(),
                        x * tilesize + offset + (Mathf.sin((float) Math.toRadians(rot)) * (range + 4f)),
                        y * tilesize + offset + (Mathf.cos((float) Math.toRadians(rot)) * (range + 4f)),
                        iconSize, iconSize, -rot);
            }
        }
    }

    public void limitRange(float margin){
        limitRange(shootType, margin);
    }

    public class PowerTurretBuild extends TurretBuild{

        @Override
        public void updateTile(){
            unit.ammo(power.status * unit.type().ammoCapacity);

            super.updateTile();
        }

        @Override
        public double sense(LAccess sensor){
            return switch(sensor){
                case ammo -> power.status;
                case ammoCapacity -> 1;
                default -> super.sense(sensor);
            };
        }

        @Override
        public BulletType useAmmo(){
            //nothing used directly
            return shootType;
        }

        @Override
        public boolean hasAmmo(){
            //you can always rotate, but never shoot if there's no power
            return true;
        }

        @Override
        public BulletType peekAmmo(){
            return shootType;
        }
    }
}
