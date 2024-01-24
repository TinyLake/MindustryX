package mindustry.arcModule.ai;

import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class ATRIAI extends AIController{
    //builderAI

    public static float buildRadius = 1500, retreatDst = 110f, retreatDelay = Time.toSeconds * 2f;
    public static float rebuildTime = 120f;

    public @Nullable Unit following;
    public @Nullable Teamc enemy;
    public @Nullable Teams.BlockPlan lastPlan;

    public float fleeRange = 370f;

    boolean found = false;
    float retreatTimer;

    //minerAI
    public static final Seq<Block> oreAllList = content.blocks().select(b -> b instanceof Floor f && !f.wallOre && f.itemDrop != null);
    public static final Seq<Block> oreAllWallList = content.blocks().select(b -> ((b instanceof Floor f && f.wallOre) || b instanceof StaticWall) && b.itemDrop != null);
    public static Seq<Block> oreList = content.blocks().select(b -> b instanceof Floor f && !f.wallOre && f.itemDrop != null);
    public static Seq<Block> oreWallList = content.blocks().select(b -> ((b instanceof Floor f && f.wallOre) || b instanceof StaticWall) && b.itemDrop != null);

    public Seq<Item> canMineList;
    public boolean mining = true;
    public Item targetItem;
    public Tile ore;


    public ATRIAI(float fleeRange){
        this.fleeRange = fleeRange;
    }

    public ATRIAI(){
    }

    @Override
    public void init(){
        if(!unit.canMine()) return;

        if(unit.type.mineFloor){
            canMineList = oreList.map(b -> b.itemDrop).select(i -> unit.canMine(i));
        }else if(unit.type.mineWalls){
            canMineList = oreWallList.map(b -> b.itemDrop).select(i -> unit.canMine(i));
        }
    }

    @Override
    public void updateMovement(){
        builderMode();
    }

    private void builderMode(){

        if(target != null && shouldShoot()){
            unit.lookAt(target);
        }

        unit.updateBuilding = true;

        if(following != null){
            retreatTimer = 0f;
            //try to follow and mimic someone

            //validate follower
            if(!following.isValid() || !following.activelyBuilding()){
                following = null;
                unit.plans.clear();
                return;
            }

            //set to follower's first build plan, whatever that is
            unit.plans.clear();
            unit.plans.addFirst(following.buildPlan());
            lastPlan = null;
        }else if(unit.buildPlan() == null){
            //not following anyone or building
            if(timer.get(timerTarget4, 40)){
                enemy = target(unit.x, unit.y, fleeRange, true, true);
            }

            //fly away from enemy when not doing anything, but only after a delay
            if((retreatTimer += Time.delta) >= retreatDelay){
                if(enemy != null){
                    unit.clearBuilding();
                    var core = unit.closestCore();
                    if(core != null && !unit.within(core, retreatDst)){
                        moveTo(core, retreatDst);
                    }
                }
            }
        }

        if(unit.buildPlan() != null){
            if(unit.controller() == Vars.player && control.input instanceof DesktopInput di) di.isBuilding = true;
            //approach plan if building
            BuildPlan req = unit.buildPlan();

            //clear break plan if another player is breaking something
            if(!req.breaking && timer.get(timerTarget2, 40f)){
                for(Player player : Groups.player){
                    if(player.isBuilder() && player.unit().activelyBuilding() && player.unit().buildPlan().samePos(req) && player.unit().buildPlan().breaking){
                        unit.plans.removeFirst();
                        //remove from list of plans
                        unit.team.data().plans.remove(p -> p.x == req.x && p.y == req.y);
                        return;
                    }
                }
            }

            boolean valid =
            !(lastPlan != null && lastPlan.removed) &&
            ((req.tile() != null && req.tile().build instanceof ConstructBlock.ConstructBuild cons && cons.current == req.block) ||
            (req.breaking ?
            Build.validBreak(unit.team(), req.x, req.y) :
            Build.validPlace(req.block, unit.team(), req.x, req.y, req.rotation)));

            if(valid){
                //move toward the plan
                moveTo(req.tile(), unit.type.buildRange - 20f);
            }else{
                //discard invalid plan
                unit.plans.removeFirst();
                lastPlan = null;
            }
        }else{
            //follow someone and help them build
            if(timer.get(timerTarget2, 60f)){
                found = false;

                Units.nearby(unit.team, unit.x, unit.y, buildRadius, u -> {
                    if(found) return;

                    if(u.canBuild() && u != unit && u.activelyBuilding()){
                        BuildPlan plan = u.buildPlan();

                        Building build = world.build(plan.x, plan.y);
                        if(build instanceof ConstructBlock.ConstructBuild cons){
                            float dist = Math.min(cons.dst(unit) - unit.type.buildRange, 0);

                            //make sure you can reach the plan in time
                            if(dist / unit.speed() < cons.buildCost * 0.9f){
                                following = u;
                                found = true;
                            }
                        }
                    }
                });
            }

            //find new plan
            if(!unit.team.data().plans.isEmpty() && following == null && timer.get(timerTarget3, rebuildTime)){
                Queue<Teams.BlockPlan> blocks = unit.team.data().plans;
                Teams.BlockPlan block = blocks.first();

                //check if it's already been placed
                if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block().id == block.block){
                    blocks.removeFirst();
                }else if(Build.validPlace(content.block(block.block), unit.team(), block.x, block.y, block.rotation) && (!nearEnemy(block.x, block.y))){ //it's valid
                    lastPlan = block;
                    //add build plan
                    unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
                    //shift build plan to tail so next unit builds something else
                    blocks.addLast(blocks.removeFirst());
                }else{
                    //shift head of queue to tail, try something else next time
                    blocks.addLast(blocks.removeFirst());
                }
            }
        }
    }


    protected boolean nearEnemy(int x, int y){
        return Units.nearEnemy(unit.team, x * tilesize - fleeRange / 2f, y * tilesize - fleeRange / 2f, fleeRange, fleeRange);
    }

    @Override
    public AIController fallback(){
        return unit.type.flying ? new FlyingAI() : new GroundAI();
    }

    @Override
    public boolean useFallback(){
        return state.rules.waves && unit.team == state.rules.waveTeam && !unit.team.rules().rtsAi;
    }

    @Override
    public boolean shouldShoot(){
        return !unit.isBuilding() && unit.type.canAttack;
    }
}
