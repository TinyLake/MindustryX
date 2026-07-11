package mindustryX.features.ui.toolTable.ai;

import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class ArcMinerAI extends AIController{
    public static final Seq<Item> toMine = content.items().copy();

    public boolean mining = true;
    public Item targetItem;
    public Tile ore;

    private Item updateTargetItem(boolean canMineNonBuildable){
        return toMine.select(i -> unit.canMine(i) && (unit.type.mineFloor ? indexer.hasOre(i) : indexer.hasWallOre(i))
        && (canMineNonBuildable || i.buildable)
        && unit.core().acceptItem(null, i)
        ).reverse().min(i -> unit.core().items.get(i));
    }

    private Tile findClosetOre(Building build){
        if(unit.type.mineFloor){
            return indexer.findClosestOre(build.x, build.y, targetItem);
        }
        return indexer.findClosestWallOre(build.x, build.y, targetItem);
    }

    @Override
    public void updateMovement(){
        if(!unit.canMine() || unit.core() == null) return;

        CoreBlock.CoreBuild core = unit.closestCore();
        //变量命名不知道叫啥了
        //最近的可以塞入非建筑物品的核心
        CoreBlock.CoreBuild core2 = unit.team.data().cores.select(c -> !((CoreBlock)c.block).incinerateNonBuildable).min(c -> unit.dst(c));

        CoreBlock.CoreBuild targetCore = targetItem == null || targetItem.buildable || core2 == null ? core : core2;

        if(unit.type.canBoost){
            player.boosting = true;
        }
        if(mining){

            if(targetItem != null && (!core.acceptItem(null, targetItem) || (core2 == null && !targetItem.buildable))){
                unit.mineTile = null;
                targetItem = null;
            }

            if(targetItem == null || timer.get(timerTarget2, 300f)){
                targetItem = updateTargetItem(core2 != null);
                if(targetItem == null) return;
            }

            if(!unit.acceptsItem(targetItem) || unit.stack.amount >= unit.type.itemCapacity){
                mining = false;
                return;
            }

            if(ore == null || !unit.validMine(ore, false) || ore.drop() != targetItem || timer.get(timerTarget3, 120f)){
                ore = findClosetOre(targetCore);
                if(ore == null) return;
            }


            Tmp.v1.setLength(unit.type.mineRange * 0.9f).limit(ore.dst(targetCore) - 0.5f).setAngle(ore.angleTo(targetCore)).add(ore);
            moveTo(Tmp.v1, 0.1f);
            if(unit.validMine(ore)){
                unit.mineTile = ore;
            }

        }else{
            unit.mineTile = null;

            if(unit.stack.amount == 0){
                mining = true;
                return;
            }
            if(!core.acceptItem(null, unit.stack.item)){
                unit.clearItem();
            }

            moveTo(targetCore, core.hitSize());
            if(unit.within(targetCore, itemTransferRange) && targetCore.acceptItem(null, targetItem)){
                Call.transferInventory(player, core);
                targetItem = updateTargetItem(core2 != null);
            }
        }
    }

    @Override
    public void updateVisuals(){
    }
}
