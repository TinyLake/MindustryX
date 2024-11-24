package mindustryX.features.ui.toolTable;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustryX.features.func.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

//moved from mindustry.arcModule.ui.quickTool.AdvanceBuildTool
public class AdvanceBuildTool extends ToolTableBase{
    BuildRange placement = BuildRange.player;
    Rect selection = new Rect();
    private Block find = Blocks.turbineCondenser, target = Blocks.titaniumConveyor;
    private Building searchBuild = null;
    private int searchBlockIndex = -1;

    public Seq<Building> buildingSeq = new Seq<>();
    private final BuildTiles buildTiles = new BuildTiles();
    private final ObjectFloatMap<Tile> buildEff = new ObjectFloatMap<>();//default 0f


    public AdvanceBuildTool(){
        super(Blocks.buildTower.emoji());
        Events.on(EventType.WorldLoadEvent.class, e -> rebuild());
    }

    protected void rebuild(){
        clear();
        row().add("区域：");
        table(tt -> {
            tt.button((placement == BuildRange.global ? "[cyan]" : "[gray]") + "", Styles.flatBordert, () -> {
                placement = BuildRange.global;
                rebuild();
            }).tooltip("[cyan]全局检查").size(30f);
            tt.button((placement == BuildRange.zone ? "[cyan]" : "[gray]") + "\uE818", Styles.flatBordert, () -> {
                selection = control.input.lastSelection;
                if(selection.area() < 10f){
                    ui.announce("当前选定区域为空，请通过F规划区域");
                    return;
                }
                placement = BuildRange.zone;
                rebuild();
            }).tooltip("[cyan]选择范围").size(30f);
            tt.button((placement == BuildRange.team ? "" : "[gray]") + Blocks.coreShard.emoji(), Styles.flatBordert, () -> {
                placement = BuildRange.team;
                rebuild();
            }).tooltip("[cyan]队伍区域").size(30f);
            tt.button((placement == BuildRange.player ? "" : "[gray]") + UnitTypes.gamma.emoji(), Styles.flatBordert, () -> {
                placement = BuildRange.player;
                rebuild();
            }).tooltip("[cyan]玩家建造区").size(30f);
            tt.update(() -> {
                if(placement != BuildRange.zone) return;
                FuncX.drawText(selection.getCenter(Tmp.v1).scl(tilesize), "建造区域", Scl.scl(1.25f), Color.white);
                Draw.color(Pal.stat, 0.7f);
                Draw.z(Layer.effect - 1f);
                Lines.stroke(Math.min(Math.abs(width), Math.abs(height)) / tilesize / 10f);
                Lines.rect(selection.x * tilesize - tilesize / 2f, selection.y * tilesize - tilesize / 2f, selection.width * tilesize + tilesize, selection.height * tilesize + tilesize);
                Draw.reset();
            });
        }).fillX().row();
        row().add("设定：");
        table(tt -> {
            tt.update(() -> {
                if(control.input.selectedBlock() && target != control.input.block){
                    target = control.input.block;
                    rebuild();
                }
            });
            tt.button(find.emoji(), Styles.flatBordert, () -> {
                new BlockSelectDialog(Block::isPlaceable, block -> this.find = block, block -> this.find == block).show();
                rebuild();
            }).size(30f).tooltip("源方块");
            tt.button("", Styles.flatBordert, this::searchBlock).update(button -> {
                buildingSeq.clear();
                if(find.privileged){
                    for(Team team : Team.all){
                        buildingSeq.add(team.data().getBuildings(find));
                    }
                }else{
                    buildingSeq.add(player.team().data().getBuildings(find));
                }

                if(!buildingSeq.contains(searchBuild)){
                    searchBuild = null;
                    searchBlockIndex = -1;
                }

                if(buildingSeq.isEmpty()) button.setText("0/0");
                else button.setText((searchBlockIndex + 1) + "/" + buildingSeq.size);
            }).tooltip("搜索方块").growX().width(90f).height(30f);
            tt.button("\uE803", Styles.flatBordert, this::replaceBlockSetting).tooltip("快速设置").size(30f);
            tt.button(target.emoji(), Styles.flatBordert, () -> {
                new BlockSelectDialog(Block::isPlaceable, block -> target = block, block -> target == block).show();
                rebuild();
            }).size(30f).tooltip("目标方块");
            tt.add().width(16);
        }).fillX().row();
        row().add("操作：");
        table(tt -> {
            tt.button("\uE88A" + Blocks.worldProcessor.emoji(), Styles.flatBordert, () -> {
                showWorldProcessorInfo();
                find = Blocks.worldProcessor;
                rebuild();
            }).tooltip("地图世处信息").width(60f).height(30f);
            tt.button("P", Styles.flatBordert, () -> buildTiles.buildBlock(target, tile -> {
                if(target instanceof ThermalGenerator g){
                    if(g.attribute == null || target.floating) return 0;
                    float[] res = {0f};
                    tile.getLinkedTilesAs(target, other -> res[0] += other.floor().isDeep() ? 0f : other.floor().attributes.get(g.attribute));
                    return res[0];
                }
                if(target instanceof Drill) return ((Drill)target).countOreArc(tile);
                return 1f;
            })).tooltip("自动放置").size(30f);
            tt.button("R", Styles.flatBordert, () -> replaceBlock(find, target)).tooltip("一键替换").size(30f);
            if(!net.client()){
                tt.button("\uE800", Styles.flatBordert, () -> {
                    instantBuild();
                    if(mobile)
                        ui.announce("瞬间建造\n[cyan]强制瞬间建造[acid]选择范围内[cyan]内规划中的所有建筑\n[orange]可能出现bug");
                }).size(30, 30).tooltip("瞬间建造\n[cyan]强制瞬间建造[acid]选择范围内[cyan]规划中的所有建筑\n[orange]可能出现bug");
            }
        }).fillX().row();
    }

    void replaceBlockGroup(Dialog dialog, Table t, Block ori, Block re){
        t.button(ori.emoji() + "\uE803" + re.emoji(), () -> {
            find = ori;
            target = re;
            dialog.hide();
        }).width(100f).height(30f);
    }

    void replaceBlockSetting(){
        BaseDialog dialog = new BaseDialog("方块替换器");
        dialog.cont.table(t -> {
            t.table(tt -> tt.label(() -> "当前选择：" + find.emoji() + "\uE803" + target.emoji())).row();
            t.image().color(Pal.accent).fillX().row();
            t.table(tt -> {
                replaceBlockGroup(dialog, tt, Blocks.conveyor, Blocks.titaniumConveyor);
                replaceBlockGroup(dialog, tt, Blocks.conveyor, Blocks.duct);
                replaceBlockGroup(dialog, tt, Blocks.conduit, Blocks.pulseConduit);
                replaceBlockGroup(dialog, tt, Blocks.conduit, Blocks.reinforcedConduit);
            }).padTop(5f).row();
            t.image().color(Pal.accent).padTop(5f).fillX().row();
        });
        dialog.hidden(this::rebuild);
        dialog.addCloseButton();
        dialog.show();
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

    void searchBlock(){
        if(buildingSeq.size == 0){
            ui.announce("[violet]方块搜索\n[acid]未找到此方块");
            return;
        }
        searchBlockIndex = (searchBlockIndex + 1) % buildingSeq.size;
        searchBuild = buildingSeq.get(searchBlockIndex);

        control.input.panCamera(Tmp.v1.set(searchBuild));
        ui.announce("[violet]方块搜索\n[white]找到方块[cyan]" + (searchBlockIndex + 1) + "[]/[cyan]" + buildingSeq.size + "[]" + find.emoji());
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
        global, zone, team, player
    }

    class BuildTiles{
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
