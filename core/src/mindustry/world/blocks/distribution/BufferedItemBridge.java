package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustryX.features.*;

import static mindustry.Vars.*;

public class BufferedItemBridge extends ItemBridge{
    public final int timerAccept = timers++;

    public float speed = 40f;
    public int bufferCapacity = 50;

    public BufferedItemBridge(String name){
        super(name);
        hasPower = false;
        hasItems = true;
        canOverdrive = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(StatExt.bufferCapacity, bufferCapacity);
        stats.add(Stat.itemsMoved, Strings.autoFixed(bufferCapacity * 60f / speed ,2) , StatUnit.itemsSecond);
    }

    public class BufferedItemBridgeBuild extends ItemBridgeBuild{
        ItemBuffer buffer = new ItemBuffer(bufferCapacity);

        @Override
        public void updateTransport(Building other){
            if(buffer.accepts() && items.total() > 0){
                buffer.accept(items.take());
            }

            Item item = buffer.poll(speed / timeScale);
            if(timer(timerAccept, 4 / timeScale) && item != null && other.acceptItem(this, item)){
                moved = true;
                other.handleItem(this, item);
                buffer.remove();
            }
        }

        @Override
        public void doDump(){
            dump();
        }

        @Override
        public void draw(){
            super.draw();

            if(RenderExt.hiddenItemTransparency > 0){
                Tile other = world.tile(link);

                float begx, begy, endx, endy;
                if(!linkValid(tile, other)){
                    begx = x - tilesize / 2f;
                    begy = y - tilesize / 2f;
                    endx = x + tilesize / 2f;
                    endy = y - tilesize / 2f;
                }else{
                    int i = tile.absoluteRelativeTo(other.x, other.y);
                    float ex = other.worldx() - x - Geometry.d4(i).x * tilesize / 2f,
                    ey = other.worldy() - y - Geometry.d4(i).y * tilesize / 2f;
                    float warmup = state.isEditor() ? 1f : this.warmup;
                    ex *= warmup;
                    ey *= warmup;

                    begx = x + Geometry.d4(i).x * tilesize / 2f;
                    begy = y + Geometry.d4(i).y * tilesize / 2f;
                    endx = x + ex;
                    endy = y + ey;
                }
                Item item;
                for(int i = 0; (item = buffer.getItem(i)) != null; i++){
                    float time = buffer.getTime(i);
                    float p = Math.min(((Time.time - time) * timeScale / speed), (float)(bufferCapacity - i) / bufferCapacity);

                    Draw.alpha(RenderExt.hiddenItemTransparency / 100f);
                    Draw.rect(item.uiIcon, Mathf.lerp(begx, endx, p), Mathf.lerp(begy, endy, p), 4f, 4f);
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
