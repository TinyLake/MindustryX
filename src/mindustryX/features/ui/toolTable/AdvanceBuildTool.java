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
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustryX.features.*;
import mindustryX.features.func.*;

import static mindustry.Vars.*;

//moved from mindustry.arcModule.ui.quickTool.AdvanceBuildTool
public class AdvanceBuildTool extends Table{
    BuildRange placement = BuildRange.player;
    Rect selection = new Rect();
    Block find = Blocks.worldProcessor;

    public Seq<Building> buildingSeq = new Seq<>();
    private final BuildTiles buildTiles = new BuildTiles();
    private int searchIndex = 0;


    public AdvanceBuildTool(){
        background(Styles.black6);
        Events.on(EventType.WorldLoadEvent.class, e -> rebuild());
        Events.run(Trigger.draw, () -> {
            if(placement == BuildRange.zone){
                Draw.z(Layer.overlayUI - 1f);
                Draw.color(Pal.stat, 0.7f);
                Lines.stroke(Math.min(Math.abs(width), Math.abs(height)) / tilesize / 10f);
                Lines.rect(selection.x * tilesize - tilesize / 2f, selection.y * tilesize - tilesize / 2f, selection.width * tilesize + tilesize, selection.height * tilesize + tilesize);
                Draw.color();
                FuncX.drawText(selection.getCenter(Tmp.v1).scl(tilesize), mindustryX.bundles.UiTexts.i("建造区域"), Scl.scl(1.25f), Color.white); // 原文本:建造区域
            }
            if(placement == BuildRange.find && find != null){
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
        button("", Styles.clearTogglet, () -> placement = BuildRange.global).checked((b) -> placement == BuildRange.global).tooltip(mindustryX.bundles.UiTexts.i("全局检查")).size(30f); // 原文本:全局检查
        button("\uE818", Styles.clearTogglet, () -> {
            selection = control.input.lastSelection;
            if(selection.area() < 10f){
                UIExt.announce(mindustryX.bundles.UiTexts.i("当前选定区域为空，请通过F规划区域")); // 原文本:当前选定区域为空，请通过F规划区域
                return;
            }
            placement = BuildRange.zone;
        }).checked((b) -> placement == BuildRange.zone).tooltip(mindustryX.bundles.UiTexts.i("选择范围")).size(30f); // 原文本:选择范围
        button(Blocks.coreShard.emoji(), Styles.clearTogglet, () -> {
            placement = BuildRange.team;
            rebuild();
        }).checked((b) -> placement == BuildRange.team).tooltip(mindustryX.bundles.UiTexts.i("队伍区域")).size(30f); // 原文本:队伍区域
        button(UnitTypes.gamma.emoji(), Styles.clearTogglet, () -> placement = BuildRange.player).checked((b) -> placement == BuildRange.player).tooltip(mindustryX.bundles.UiTexts.i("玩家建造区")).size(30f); // 原文本:玩家建造区

        var findButton = add(new TextButton("", Styles.clearTogglet)).update((b) -> {
            buildingSeq.clear();
            if(find.privileged){
                for(Team team : Team.all){
                    buildingSeq.add(team.data().getBuildings(find));
                }
            }else{
                buildingSeq.add(player.team().data().getBuildings(find));
            }
            b.setText(find.emoji() + " " + buildingSeq.size);
            b.setChecked(placement == BuildRange.find);
        }).height(30f).tooltip(mindustryX.bundles.UiTexts.i("查找方块")).wrapLabel(false).get(); // 原文本:查找方块
        findButton.clicked(() -> {
            if(findButton.childrenPressed()) return;
            if(placement != BuildRange.find){
                placement = BuildRange.find;
                if(find == Blocks.worldProcessor) showWorldProcessorInfo();
            }else{
                if(buildingSeq.isEmpty()) return;
                searchIndex = searchIndex % buildingSeq.size;
                control.input.panCamera(Tmp.v1.set(buildingSeq.get(searchIndex)));
                searchIndex++;
                UIExt.announce(Strings.format("@[grey]/[]@ @@", searchIndex, buildingSeq.size, find.emoji(), find.localizedName));
            }
        });
        findButton.getLabelCell().padLeft(2f);
        findButton.button(Icon.settingsSmall, Styles.clearTogglei, iconSmall, () -> {
            if(target == null){
                UIExt.announce(mindustryX.bundles.UiTexts.i("[yellow]当前选中物品为空，请在物品栏选中建筑")); // 原文本:[yellow]当前选中物品为空，请在物品栏选中建筑
                return;
            }
            find = target;
            searchIndex = 0;
            placement = BuildRange.find;
            rebuild();
        }).tooltip(mindustryX.bundles.UiTexts.i("设置目标")).padRight(2f); // 原文本:设置目标

        add().width(16);
        button("P", Styles.cleart, () -> {
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
                UIExt.announce(mindustryX.bundles.UiTexts.i("[yellow]建筑过多，避免卡顿，仅保留前1000个规划")); // 原文本:[yellow]建筑过多，避免卡顿，仅保留前1000个规划
            }
        }).tooltip(mindustryX.bundles.UiTexts.i("放置/替换")).size(30f); // 原文本:放置/替换
    }

    public static void showWorldProcessorInfo(){
        Log.info(mindustryX.bundles.UiTexts.uiCurrentMap(state.map.name()));
        int[] data = new int[3];
        Groups.build.each(b -> {
            if(b instanceof LogicBlock.LogicBuild lb && lb.block.privileged){
                data[0] += 1;
                data[1] += lb.code.split("\n").length + 1;
                data[2] += lb.code.length();
            }
        });
        String text = mindustryX.bundles.UiTexts.uiWorldProcessorSummary(data[0], data[1], data[2]);
        Log.info(text);
        ui.announce(text, 10);
    }

    void replaceBlock(Block ori, Block re){
        if(player.dead()) return;
        player.team().data().buildings.each(building -> building.block == ori && contain(building.tile),
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
