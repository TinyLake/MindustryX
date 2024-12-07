package mindustryX.features.ui.toolTable;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustryX.features.*;
import mindustryX.features.func.*;

import static mindustry.Vars.*;

//moved from mindustry.arcModule.ui.quickTool.AdvanceBuildTool
public class AdvanceBuildTool extends ToolTableBase{
    BuildRange placement = BuildRange.player;
    Rect selection = new Rect();
    Block find = Blocks.worldProcessor;
    boolean highlight = false;

    public Seq<Building> buildingSeq = new Seq<>();
    private final BuildTiles buildTiles = new BuildTiles();


    public AdvanceBuildTool(){
        super(Blocks.buildTower.emoji());
        Events.on(EventType.WorldLoadEvent.class, e -> rebuild());
        Events.run(Trigger.draw, () -> {
            if(placement == BuildRange.zone){
                Draw.z(Layer.overlayUI - 1f);
                Draw.color(Pal.stat, 0.7f);
                Lines.stroke(Math.min(Math.abs(width), Math.abs(height)) / tilesize / 10f);
                Lines.rect(selection.x * tilesize - tilesize / 2f, selection.y * tilesize - tilesize / 2f, selection.width * tilesize + tilesize, selection.height * tilesize + tilesize);
                Draw.color();
                FuncX.drawText(selection.getCenter(Tmp.v1).scl(tilesize), "建造区域", Scl.scl(1.25f), Color.white);
            }
            if(highlight && find != null){
                Draw.z(Layer.blockBuilding + 1f);
                Draw.color(Pal.negativeStat);
                for(var it : buildingSeq){
                    Lines.stroke(it.block.size);
                    it.hitbox(Tmp.r1);
                    Lines.rect(Tmp.r1);
                }
                Draw.color();
            }
        });
    }

    protected void rebuild(){
        clear();
        center();
        final Block target = control.input.block;
        update(() -> {
            if(control.input.selectedBlock() && target != control.input.block){
                rebuild();
            }
        });
        add().height(40);
        button("", Styles.flatTogglet, () -> placement = BuildRange.global).checked((b) -> placement == BuildRange.global).tooltip("[cyan]全局检查").size(30f);
        button("\uE818", Styles.flatTogglet, () -> {
            selection = control.input.lastSelection;
            if(selection.area() < 10f){
                UIExt.announce("当前选定区域为空，请通过F规划区域");
                return;
            }
            placement = BuildRange.zone;
        }).checked((b) -> placement == BuildRange.zone).tooltip("[cyan]选择范围").size(30f);
        button(Blocks.coreShard.emoji(), Styles.flatTogglet, () -> {
            placement = BuildRange.team;
            rebuild();
        }).checked((b) -> placement == BuildRange.team).tooltip("[cyan]队伍区域").size(30f);
        button(UnitTypes.gamma.emoji(), Styles.flatTogglet, () -> placement = BuildRange.player).checked((b) -> placement == BuildRange.player).tooltip("[cyan]玩家建造区").size(30f);

        add().width(16);
        button("", Styles.flatTogglet, () -> placement = BuildRange.find).update((b) -> {
            buildingSeq.clear();
            if(find.privileged){
                for(Team team : Team.all){
                    buildingSeq.add(team.data().getBuildings(find));
                }
            }else{
                buildingSeq.add(player.team().data().getBuildings(find));
            }
            b.getLabel().setWrap(false);
            b.setText(find.emoji() + " " + buildingSeq.size);
            b.setChecked(placement == BuildRange.find);
        }).height(30f).tooltip("查找方块");
        button(String.valueOf(Iconc.settings), Styles.flatBordert, () -> {
            if(target == null){
                UIExt.announce("[yellow]当前选中物品为空，请在物品栏选中建筑");
                return;
            }
            find = target;
            placement = BuildRange.find;
            rebuild();
        }).size(30f).tooltip("设置目标");
        button(String.valueOf(Iconc.eye), Styles.flatTogglet, () -> {
            highlight = !highlight;
            if(highlight && find == Blocks.worldProcessor) showWorldProcessorInfo();
        }).checked((b) -> highlight).size(30).tooltip("高亮目标");

        add().width(16);
        button("P", Styles.flatBordert, () -> {
            if(target == null || player.dead()) return;
            if(placement == BuildRange.find){
                replaceBlock(find, target);
                return;
            }
            buildTiles.buildBlock(target, tile -> {
                if(target instanceof ThermalGenerator g){
                    if(g.attribute == null || target.floating) return 0;
                    float[] res = {0f};
                    tile.getLinkedTilesAs(target, other -> res[0] += other.floor().isDeep() ? 0f : other.floor().attributes.get(g.attribute));
                    return res[0];
                }
                if(target instanceof Drill) return ((Drill)target).countOreArc(tile);
                return 1f;
            });
            var plans = player.unit().plans();
            if(plans.size > 1000){
                while(plans.size > 1000) plans.removeLast();
                UIExt.announce("[yellow]建筑过多，避免卡顿，仅保留前1000个规划");
            }
        }).tooltip("一键放置").size(30f);
        if(!net.client()){
            button("\uE800", Styles.flatBordert, () -> {
                instantBuild();
                if(mobile)
                    ui.announce("瞬间建造\n[cyan]强制瞬间建造[acid]选择范围内[cyan]内规划中的所有建筑\n[orange]可能出现bug");
            }).size(30, 30).tooltip("瞬间建造\n[cyan]强制瞬间建造[acid]选择范围内[cyan]规划中的所有建筑\n[orange]可能出现bug");
        }
    }

    public static void showWorldProcessorInfo(){
        Log.info("当前地图:@", state.map.name());
        int[] data = new int[3];
        Groups.build.each(b -> {
            if(b instanceof LogicBlock.LogicBuild lb && lb.block.privileged){
                data[0] += 1;
                data[1] += lb.code.split("\n").length + 1;
                data[2] += lb.code.length();
            }
        });
        Log.info("地图共有@个世处，总共@行指令，@个字符", data[0], data[1], data[2]);
        ui.announce(Strings.format("地图共有@个世处，总共@行指令，@个字符", data[0], data[1], data[2]), 10);
    }

    void replaceBlock(Block ori, Block re){
        if(player.dead()) return;
        player.team().data().buildings.each(building -> building.block() == ori && contain(building.tile),
        building -> player.unit().addBuild(new BuildPlan(building.tile.x, building.tile.y, building.rotation, re, building.config())));
    }

    boolean contain(Tile tile){
        if(placement == BuildRange.global) return true;
        if(placement == BuildRange.zone) return selection.contains(tile.x, tile.y);
        if(placement == BuildRange.player) return tile.within(player.x, player.y, buildingRange);
        if(placement == BuildRange.team){
            if(state.rules.polygonCoreProtection){
                float mindst = Float.MAX_VALUE;
                CoreBlock.CoreBuild closest = null;
                for(Teams.TeamData data : state.teams.active){
                    for(CoreBlock.CoreBuild tiles : data.cores){
                        float dst = tiles.dst2(tile.x * tilesize, tile.y * tilesize);
                        if(dst < mindst){
                            closest = tiles;
                            mindst = dst;
                        }
                    }
                }
                return closest == null || closest.team == player.team();
            }else return !state.teams.anyEnemyCoresWithin(player.team(), tile.x * tilesize, tile.y * tilesize, state.rules.enemyCoreBuildRadius + tilesize);
        }
        return true;
    }

    void instantBuild(){
        if(player.dead()) return;
        player.unit().plans.each(buildPlan -> {
            if(!contain(buildPlan.tile())) return;
            forceBuildBlock(buildPlan.block, buildPlan.tile(), player.team(), buildPlan.rotation, buildPlan.config);
        });
    }

    void forceBuildBlock(Block block, Tile tile, Team team, int rotation, Object config){
        if(block == Blocks.cliff) buildCliff(tile);
        else if(block instanceof OverlayFloor){
            tile.setOverlay(block);
        }else if(block instanceof Floor floor){
            tile.setFloor(floor);
        }else{
            tile.setBlock(block, team, rotation);
            tile.build.configure(config);
        }
        pathfinder.updateTile(tile);
    }

    void buildCliff(Tile tile){
        int rotation = 0;
        for(int i = 0; i < 8; i++){
            Tile other = world.tiles.get(tile.x + Geometry.d8[i].x, tile.y + Geometry.d8[i].y);
            if(other != null && !other.floor().hasSurface()){
                rotation |= (1 << i);
            }
        }

        if(rotation != 0){
            tile.setBlock(Blocks.cliff);
        }

        tile.data = (byte)rotation;
    }

    enum BuildRange{
        global, zone, team, player, find
    }

    class BuildTiles{
        private final ObjectFloatMap<Tile> buildEff = new ObjectFloatMap<>();//default 0f
        public int minx, miny, maxx, maxy, width, height;
        Seq<Tile> validTile = new Seq<>();
        Seq<Float> eff = new Seq<>();
        float efficiency = 0;
        Block block;
        boolean canBuild = true;

        public BuildTiles(){
        }

        void buildBlock(Block buildBlock, Floatf<Tile> tilef){
            block = buildBlock;
            updateTiles();
            checkValid();
            calBlockEff(tilef);
            eff.sort().reverse().remove(0f);
            eff.each(this::buildEff);
        }

        public void updateTiles(){
            minx = 9999;
            miny = 9999;
            maxx = -999;
            maxy = -999;
            validTile.clear();
            eff.clear();
            world.tiles.eachTile(tile -> {
                if(tile == null) return;
                if(!contain(tile)) return;
                validTile.add(tile);
                minx = Math.min(minx, tile.x);
                miny = Math.min(miny, tile.y);
                maxx = Math.max(maxx, tile.x);
                maxy = Math.max(maxy, tile.y);
            });
            buildEff.clear();
            width = maxx - minx;
            height = maxy - miny;
        }

        void checkValid(){
            validTile.each(tile -> {
                if(
                (block.size == 2 && world.getDarkness(tile.x, tile.y) >= 3) ||
                (state.rules.staticFog && state.rules.fog && !fogControl.isDiscovered(player.team(), tile.x, tile.y)) ||
                (tile.floor().isDeep() && !block.floating && !block.requiresWater && !block.placeableLiquid) || //deep water
                (block == tile.block() && tile.build != null && rotation == tile.build.rotation && block.rotate) || //same block, same rotation
                !tile.interactable(player.team()) || //cannot interact
                !tile.floor().placeableOn || //solid wall
                //replacing a block that should be replaced (e.g. payload placement)
                !((block.canReplace(tile.block()) || //can replace type
                (tile.build instanceof ConstructBlock.ConstructBuild build && build.current == block && tile.centerX() == tile.x && tile.centerY() == tile.y)) && //same type in construction
                block.bounds(tile.x, tile.y, Tmp.r1).grow(0.01f).contains(tile.block().bounds(tile.centerX(), tile.centerY(), Tmp.r2))) || //no replacement
                (block.requiresWater && tile.floor().liquidDrop != Liquids.water) //requires water but none found
                ) buildEff.put(tile, -1); // cannot build
            });
        }

        void calBlockEff(Floatf<Tile> tilef){
            validTile.each(tile -> {
                canBuild = true;
                getLinkedTiles(tile, tile1 -> canBuild = buildEff.get(tile, 0f) != -1 && canBuild);   //不可能建造
                if(canBuild){
                    efficiency = tilef.get(tile);
                    buildEff.put(tile, efficiency);
                    if(!eff.contains(efficiency)) eff.add(efficiency);
                }else{
                    buildEff.remove(tile, 0);
                }
            });
        }

        void buildEff(float e){
            if(e == 0 || player.dead()) return;
            validTile.each(tile -> {
                if(buildEff.get(tile, 0f) != e) return;
                if(!block.canPlaceOn(tile, player.team(), 0)) return;
                player.unit().addBuild(new BuildPlan(tile.x, tile.y, 0, block));
                getFullLinkedTiles(tile, tile1 -> buildEff.remove(tile1, 0f));
            });
        }

        private void getLinkedTiles(Tile tile, Cons<Tile> cons){
            if(block.isMultiblock()){
                int size = block.size, o = block.sizeOffset;
                for(int dx = 0; dx < size; dx++){
                    for(int dy = 0; dy < size; dy++){
                        Tile other = world.tile(tile.x + dx + o, tile.y + dy + o);
                        if(other != null) cons.get(other);
                    }
                }
            }else{
                cons.get(tile);
            }
        }

        private void getFullLinkedTiles(Tile tile, Cons<Tile> cons){
            if(block.isMultiblock()){
                int size = block.size, o = 0;
                for(int dx = -size + 1; dx < size; dx++){
                    for(int dy = -size + 1; dy < size; dy++){
                        Tile other = world.tile(tile.x + dx + o, tile.y + dy + o);
                        if(other != null) cons.get(other);
                    }
                }
            }else{
                cons.get(tile);
            }
        }

    }
}
