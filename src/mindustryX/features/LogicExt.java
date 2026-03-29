package mindustryX.features;

import arc.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.Packets.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustryX.*;
import mindustryX.features.SettingsV2.*;

import static mindustry.Vars.net;
import static mindustryX.features.UIExt.i;

public class LogicExt{
    public enum BlockLayer{
        FLOOR, ORE, BLOCK
    }

    public static boolean worldCreator = false;
    public static boolean terrainSchematic = false;
    public static boolean floorLayerEnabled = true;
    public static boolean oreLayerEnabled = true;
    public static boolean blockLayerEnabled = true;
    public static boolean invertMapClick = false;
    /** Disable player control in InputHandler */
    public static boolean noUpdatePlayerMovement = false;
    /** protocol to mock, for compatible to force join servers. */
    public static int mockProtocol; /* = Version.build */
    /** Use contentsMapping from server, for compatibility when build version is not same. */
    public static boolean contentsCompatibleMode = false;
    public static boolean v146Mode = false;

    private static final CheckPref invertMapClick0 = new CheckPref("gameUI.invertMapClick");
    public static final CheckPref worldCreator0 = new CheckPref("worldCreator");
    public static final CheckPref terrainSchematic0 = new CheckPref("terrainSchematic");
    public static final CheckPref floorLayerEnabled0 = new CheckPref("advanceTool.floorLayerEnabled", true);
    public static final CheckPref oreLayerEnabled0 = new CheckPref("advanceTool.oreLayerEnabled", true);
    public static final CheckPref blockLayerEnabled0 = new CheckPref("advanceTool.blockLayerEnabled", true);
    public static final CheckPref reliableSync = new CheckPref("debug.reliableSync");
    public static final SliderPref limitUpdate = new SliderPref("debug.limitUpdate", 0, 0, 100, 1, (it) -> {
        if(it == 0) return i("关闭");
        return VarsX.bundle.tiles(it);
    });
    public static final CheckPref rotateCanvas = new CheckPref("block.rotateCanvas");
    public static final SettingsV2.CheckPref editOtherBlock0 = new CheckPref("block.editOtherBlock");

    public static int limitUpdateTimer = 10;
    public static boolean editOtherBlock;
    public static Building currentBuilding;

    public static void init(){
        invertMapClick0.addFallbackName("invertMapClick");
        reliableSync.addFallbackName("reliableSync");
        editOtherBlock0.addFallbackName("editOtherBlock");


        Events.run(Trigger.update, () -> {
            limitUpdateTimer = (limitUpdateTimer + 1) % 10;
            worldCreator = worldCreator0.get();
            terrainSchematic = terrainSchematic0.get();
            floorLayerEnabled = floorLayerEnabled0.get();
            oreLayerEnabled = oreLayerEnabled0.get();
            blockLayerEnabled = blockLayerEnabled0.get();
            invertMapClick = invertMapClick0.get();
            mockProtocol = ConnectPacket.clientVersion > 0 ? ConnectPacket.clientVersion : Version.build;
            v146Mode = mockProtocol == 146;
            contentsCompatibleMode = mockProtocol != Version.build;

            editOtherBlock = editOtherBlock0.get();
            editOtherBlock &= !net.client();
        });
    }

    public static boolean shouldIncludeFloorInTerrainSchematic(){
        return shouldIncludeInTerrainSchematic(BlockLayer.FLOOR);
    }

    public static boolean shouldIncludeOreInTerrainSchematic(){
        return shouldIncludeInTerrainSchematic(BlockLayer.ORE);
    }

    public static boolean shouldIncludeBlockInTerrainSchematic(){
        return shouldIncludeInTerrainSchematic(BlockLayer.BLOCK);
    }

    public static boolean isFloorLayer(Block block){
        return block instanceof Floor floor && !(floor instanceof OverlayFloor) && !floor.oreDefault && !floor.wallOre;
    }

    public static boolean isOreLayer(Block block){
        return block instanceof Floor floor && (floor instanceof OverlayFloor || floor.oreDefault || floor.wallOre);
    }

    public static boolean isBlockLayer(Block block){
        return block != null && !block.isAir() && !isFloorLayer(block) && !isOreLayer(block);
    }

    public static BlockLayer classifyBlockLayer(Block block){
        if(isFloorLayer(block)) return BlockLayer.FLOOR;
        if(isOreLayer(block)) return BlockLayer.ORE;
        return BlockLayer.BLOCK;
    }

    public static boolean isFloorLayerEnabled(){
        return floorLayerEnabled;
    }

    public static boolean isOreLayerEnabled(){
        return oreLayerEnabled;
    }

    public static boolean isBlockLayerEnabled(){
        return blockLayerEnabled;
    }

    public static boolean isLayerEnabled(BlockLayer layer){
        return switch(layer){
            case FLOOR -> isFloorLayerEnabled();
            case ORE -> isOreLayerEnabled();
            case BLOCK -> isBlockLayerEnabled();
        };
    }

    public static boolean isLayerEnabled(Block block){
        return block != null && !block.isAir() && isLayerEnabled(classifyBlockLayer(block));
    }

    public static boolean shouldIncludeInTerrainSchematic(BlockLayer layer){
        return terrainSchematic && isLayerEnabled(layer);
    }

}
