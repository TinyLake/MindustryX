package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;

import mindustry.ui.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustryX.features.*;

import static mindustry.Vars.*;

public class Junction extends Block{
    public float speed = 26; //frames taken to go through this junction
    public int capacity = 6;

    public Junction(String name){
        super(name);
        update = true;
        solid = false;
        underBullets = true;
        group = BlockGroup.transportation;
        unloadable = false;
        floating = true;
        noUpdateDisabled = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        //have to add a custom calculated speed, since the actual movement speed is apparently not linear
        stats.add(Stat.itemCapacity, capacity);
        stats.add(Stat.itemsMoved,Strings.autoFixed(60f / speed * capacity ,2) , StatUnit.itemsSecond);
    }


    @Override
    public boolean outputsItems(){
        return true;
    }

    public class JunctionBuild extends Building{
        public DirectionalItemBuffer buffer = new DirectionalItemBuffer(capacity);

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return 0;
        }

        @Override
        public void updateTile(){

            for(int i = 0; i < 4; i++){
                if(buffer.indexes[i] > 0){
                    if(buffer.indexes[i] > capacity) buffer.indexes[i] = capacity;
                    long l = buffer.buffers[i][0];
                    float time = BufferItem.time(l);

                    if(Time.time >= time + speed / timeScale || Time.time < time){

                        Item item = content.item(BufferItem.item(l));
                        Building dest = nearby(i);

                        //skip blocks that don't want the item, keep waiting until they do
                        if(item == null || dest == null || !dest.acceptItem(this, item) || dest.team != team){
                            continue;
                        }

                        dest.handleItem(this, item);
                        System.arraycopy(buffer.buffers[i], 1, buffer.buffers[i], 0, buffer.indexes[i] - 1);
                        buffer.indexes[i] --;
                    }
                }
            }
        }

        @Override
        public void handleItem(Building source, Item item){
            int relative = source.relativeTo(tile);
            buffer.accept(relative, item);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            int relative = source.relativeTo(tile);

            if(relative == -1 || !buffer.accepts(relative)) return false;
            Building to = nearby(relative);
            return to != null && to.team == team;
        }

        @Override
        public void draw(){
            super.draw();
            if(RenderExt.hiddenItemTransparency > 0){
                float begx, begy, endx, endy;
                for(int dir = 0; dir < 4; dir++){
                    endx = x + Geometry.d4(dir).x * tilesize / 2f + Geometry.d4(Math.floorMod(dir + 1, 4)).x * tilesize / 4f;
                    endy = y + Geometry.d4(dir).y * tilesize / 2f + Geometry.d4(Math.floorMod(dir + 1, 4)).y * tilesize / 4f;
                    begx = x - Geometry.d4(dir).x * tilesize / 4f + Geometry.d4(Math.floorMod(dir + 1, 4)).x * tilesize / 4f;
                    begy = y - Geometry.d4(dir).y * tilesize / 4f + Geometry.d4(Math.floorMod(dir + 1, 4)).y * tilesize / 4f;

                    Item item;
                    for(int i = 0; (item = buffer.getItem(dir, i)) != null; i++){
                        float time = buffer.getTime(dir, i);
                        float p = Math.min(((Time.time - time) * timeScale / speed), (float)(capacity - i) / capacity);
                        Draw.alpha(RenderExt.hiddenItemTransparency / 100f);
                        Draw.rect(item.uiIcon, Mathf.lerp(begx, endx, p), Mathf.lerp(begy, endy, p), 4f, 4f);
                    }
                }
            }
        }


        @Override
        public void write(Writes write){
            super.write(write);
            buffer.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            buffer.read(read);
        }
    }
}
