package mindustry.arcModule.ui.quickTool;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.arcModule.*;
import mindustry.arcModule.ui.dialogs.*;
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
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.arcModule.ElementUtils.*;
import static mindustry.arcModule.RFuncs.arcSetCamera;

public class AdvanceBuildTool extends ElementUtils.ToolTable{
    BuildRange placement = BuildRange.player;
    Rect selection = new Rect();
    private Block original = Blocks.conveyor, newBlock = Blocks.titaniumConveyor;
    private Block autoBuild = Blocks.turbineCondenser;
    private Block searchBlock = Blocks.itemSource;
    private Building searchBuild = null;
    private int searchBlockIndex = -1;
    private boolean shadowBuild = false;

    public Seq<Building> buildingSeq = new Seq<>();
    private final BuildTiles buildTiles = new BuildTiles();
    private final ObjectFloatMap<Tile> buildEff = new ObjectFloatMap<>();//default 0f


    public AdvanceBuildTool(){
        icon = Blocks.buildTower.emoji();
        Events.on(EventType.WorldLoadEvent.class, e -> rebuild());
        Events.run(EventType.Trigger.update, () -> {
            if(shadowBuild && player.unit() != null && player.unit().plans != null && player.unit().activelyBuilding()){
                if(player.unit().buildPlan().progress == 0) return;
                player.unit().plans.remove(player.unit().buildPlan());
                Call.deletePlans(player, new int[]{player.unit().plans.indexOf(player.unit().buildPlan(), true)});
            }
        });

    }

    @Override
    protected void buildTable(){
        table(t -> {
            t.setBackground(Styles.black6);
            t.table(tt -> {
                tt.button((placement == BuildRange.global ? "[cyan]" : "[gray]") + "", NCtextStyle, () -> {
                    placement = BuildRange.global;
                    rebuild();
                }).tooltip("[cyan]全局检查").size(30f);
                tt.button((placement == BuildRange.zone ? "[cyan]" : "[gray]") + "\uE818", NCtextStyle, () -> {
                    selection = control.input.lastSelection;
                    if(selection.area() < 10f) return;
                    placement = BuildRange.zone;
                    rebuild();
                }).tooltip("[cyan]选择范围").size(30f);
                tt.button((placement == BuildRange.team ? "" : "[gray]") + Blocks.coreShard.emoji(), NCtextStyle, () -> {
                    placement = BuildRange.team;
                    rebuild();
                }).tooltip("[cyan]队伍区域").size(30f);
                tt.button((placement == BuildRange.player ? "" : "[gray]") + UnitTypes.gamma.emoji(), NCtextStyle, () -> {
                    placement = BuildRange.player;
                    rebuild();
                }).tooltip("[cyan]玩家建造区").size(30f);
                tt.update(() -> {
                    if(placement != BuildRange.zone) return;
                    DrawUtilities.drawText("建造区域", 0.2f, selection.x * tilesize + selection.width * tilesize * 0.5f, selection.y * tilesize + selection.height * tilesize, Align.bottom);
                    Draw.color(Pal.stat, 0.7f);
                    Draw.z(Layer.effect - 1f);
                    Lines.stroke(Math.min(Math.abs(width), Math.abs(height)) / tilesize / 10f);
                    Lines.rect(selection.x * tilesize - tilesize / 2f, selection.y * tilesize - tilesize / 2f, selection.width * tilesize + tilesize, selection.height * tilesize + tilesize);
                    Draw.reset();
                });
            }).fillX().row();
            t.table(tt -> {
                tt.button("R", NCtextStyle, this::replaceBlock).tooltip("[cyan]替换方块").size(30f);
                tt.button(replaceBlockName(), NCtextStyle, this::replaceBlockSetting).tooltip("[acid]设置替换").width(100f).height(30f);
            }).fillX().row();
            t.table(tt -> {
                tt.button(autoBuild.emoji(), NCtextStyle, () -> blockAutoPlacer(autoBuild)).size(30f);
                tt.button("\uE87C", NCtextStyle, () -> {
                    new BlockSelectDialog(Block::isPlaceable, block -> autoBuild = block, block -> autoBuild == block).show();
                    rebuild();
                }).size(30f);
                tt.update(() -> {
                    if(control.input.selectedBlock()){
                        autoBuild = control.input.block;
                        rebuild();
                    }
                });
            }).fillX().row();
            t.table(tt -> {
                tt.button("S", NCtextStyle, this::searchBlock).update(button -> {

                    buildingSeq = player.team().data().buildings.select(building1 -> building1.block == searchBlock);
                    if(searchBlock.privileged){
                        for(Team team : Team.all){
                            if(team == player.team()) continue;
                            buildingSeq.add(team.data().buildings.select(building1 -> building1.block == searchBlock));
                        }
                    }

                    if(buildingSeq.contains(searchBuild)){
                        searchBlockIndex = buildingSeq.indexOf(searchBuild);
                    }else{
                        searchBuild = null;
                        searchBlockIndex = -1;
                    }

                    if(buildingSeq.isEmpty() || searchBlockIndex == -1) button.setText("[lightgray]\uE88A");
                    else button.setText("\uE88A" + (searchBlockIndex + 1) + "/" + buildingSeq.size);
                }).tooltip("[cyan]搜索方块").growX().height(30f);

                tt.button(searchBlock.emoji(), NCtextStyle, () -> {
                    new BlockSelectDialog(Block::isPlaceable, block -> searchBlock = block, block -> searchBlock == block).show().hidden(this::rebuild);
                    searchBlockIndex = 0;
                }).tooltip("[acid]搜索替换").width(30f).height(30f);

                tt.update(() -> {
                    if(control.input.selectedBlock()){
                        searchBlock = control.input.block;
                        rebuild();
                    }
                });
            }).fillX().row();
            t.table(tt -> {
                tt.button(Blocks.worldMessage.emoji(), textStyle, () -> Core.settings.put("displayallmessage", !Core.settings.getBool("displayallmessage", false))).checked(a -> Core.settings.getBool("displayallmessage")).size(30, 30).tooltip("开关信息板全显示");
                tt.button(Blocks.worldProcessor.emoji(), NCtextStyle, () -> {
                    RFuncs.worldProcessor();
                    searchBlock = Blocks.worldProcessor;
                    rebuild();
                }).size(30).tooltip("地图世处信息");
            }).fillX().row();
            t.table(tt -> tt.button("\uE817", textStyle, () -> shadowBuild = !shadowBuild).checked(a -> shadowBuild).size(30, 30).tooltip("虚影建造模式\n[red]有些服限制发包数较低，建筑较多时会被踢出。请酌情使用")).fillX().row();
            if(!net.client()){
                t.table(tt -> {
                    tt.button("\uF8C9", textStyle, () -> {
                        AdvanceToolTable.forcePlacement ^= true;
                        if(mobile)
                            arcui.arcInfo("允许蓝图建造地形");
                    }).checked(a -> AdvanceToolTable.forcePlacement).size(30, 30).tooltip("允许蓝图建造地形");
                    tt.button("\uE800", NCtextStyle, () -> {
                        instantBuild();
                        if(mobile)
                            arcui.arcInfo("瞬间建造\n[cyan]强制瞬间建造[acid]选择范围内[cyan]内规划中的所有建筑\n[orange]可能出现bug");
                    }).size(30, 30).tooltip("瞬间建造\n[cyan]强制瞬间建造[acid]选择范围内[cyan]规划中的所有建筑\n[orange]可能出现bug");
                    tt.button("\uF8D2", NCtextStyle, () -> {
                        if(!AdvanceToolTable.forcePlacement) arcui.arcInfo("请开启允许蓝图建造地形 \uF8C9");
                        else saveTerrain(true);
                    }).size(30, 30).tooltip("复制所选范围内的地板作为蓝图");
                    tt.button("\uF8C4", NCtextStyle, () -> {
                        if(!AdvanceToolTable.forcePlacement) arcui.arcInfo("请开启允许蓝图建造地形 \uF8C9");
                        else saveTerrain(false);
                    }).size(30, 30).tooltip("复制所选范围内的修饰作为蓝图");

                }).fillX().row();
            }
        });
    }

    void replaceBlockSetting(){
        BaseDialog dialog = new BaseDialog("方块替换器");
        dialog.cont.table(t -> {
            t.table(tt -> tt.label(() -> "当前选择：" + replaceBlockName())).row();
            t.image().color(ARCVars.getThemeColor()).fillX().row();
            t.table(tt -> {
                replaceBlockGroup(dialog, tt, Blocks.conveyor, Blocks.titaniumConveyor);
                replaceBlockGroup(dialog, tt, Blocks.conveyor, Blocks.duct);
                replaceBlockGroup(dialog, tt, Blocks.conduit, Blocks.pulseConduit);
                replaceBlockGroup(dialog, tt, Blocks.conduit, Blocks.reinforcedConduit);
            }).padTop(5f).row();
            t.image().color(ARCVars.getThemeColor()).padTop(5f).fillX().row();
            t.table(tt -> {
                tt.button("源方块", () -> new BlockSelectDialog(block -> block.replaceable, block -> original = block, block -> original == block).show()).width(100f).height(30f).row();
                tt.button("新方块", () -> new BlockSelectDialog(block -> original.canReplace(block), block -> newBlock = block, block -> newBlock == block).show()).width(100f).height(30f).row();
            }).padTop(5f).row();
        });
        dialog.hidden(this::rebuild);
        dialog.addCloseButton();
        dialog.show();
    }

    void replaceBlockGroup(Dialog dialog, Table t, Block ori, Block re){
        t.button(replaceBlockName(ori, re), () -> {
            original = ori;
            newBlock = re;
            dialog.hide();
        }).width(100f).height(30f);
    }

    String replaceBlockName(){
        return replaceBlockName(original, newBlock);
    }

    String replaceBlockName(Block ori, Block re){
        return ori.emoji() + "\uE803" + re.emoji();
    }

    void replaceBlock(){
        replaceBlock(original, newBlock);
    }

    void replaceBlock(Block ori, Block re){
        player.team().data().buildings.each(building -> building.block() == ori && contain(building.tile),
        building -> player.unit().addBuild(new BuildPlan(building.tile.x, building.tile.y, building.rotation, re, building.config())));
    }

    void blockAutoPlacer(Block block){
        buildTiles.buildBlock(block, tile -> getBlockEff(block, tile));
    }

    float getBlockEff(Block block, Tile tile){
        if(block instanceof ThermalGenerator) return block.sumAttribute(((ThermalGenerator)block).attribute, tile);
        if(block instanceof Drill) return ((Drill)block).countOreArc(tile);
        return 1f;
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
            arcui.arcInfo("[violet]方块搜索\n[acid]未找到此方块");
            return;
        }
        searchBlockIndex = (searchBlockIndex + 1) % buildingSeq.size;
        searchBuild = buildingSeq.get(searchBlockIndex);

        arcSetCamera(searchBuild);
        arcui.arcInfo("[violet]方块搜索\n[acid]找到方块[cyan]" + (searchBlockIndex + 1) + "[acid]/[cyan]" + buildingSeq.size + "[white]" + searchBlock.emoji());
    }

    void instantBuild(){
        player.unit().plans.each(buildPlan -> {
            if(!contain(buildPlan.tile())) return;
            forceBuildBlock(buildPlan.block, buildPlan.tile(), player.team(), buildPlan.rotation, buildPlan.config);
        });
    }

    void saveTerrain(boolean floor){
        buildTiles.updateTiles();
        Seq<Schematic.Stile> tiles = new Seq<>();
        buildTiles.validTile.each(tile -> {
            if(!floor && tile.overlay() == Blocks.air) return;
            tiles.add(new Schematic.Stile(floor ? tile.floor() : tile.overlay(), tile.x - buildTiles.minx, tile.y - buildTiles.miny));
        });
        control.input.lastSchematic = new Schematic(tiles, new StringMap(), buildTiles.width, buildTiles.height);
        control.input.useSchematic(control.input.lastSchematic);
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
            if(e == 0) return;
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
