package mindustryX.features;

import arc.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.net.Packets.*;
import mindustryX.features.SettingsV2.*;

public class LogicExt{
    public static boolean worldCreator = false;
    public static boolean terrainSchematic = false;
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
    public static final CheckPref reliableSync = new CheckPref("debug.reliableSync");
    public static final SliderPref limitUpdate = new SliderPref("debug.limitUpdate", 0, 0, 100, 1, (it) -> {
        if(it == 0) return mindustryX.bundles.FuncX.ui("off"); // 原文本:关闭
        return it + mindustryX.bundles.FuncX.ui("tiles"); // 原文本:格
    });
    public static final CheckPref rotateCanvas = new CheckPref("block.rotateCanvas");

    public static int limitUpdateTimer = 10;

    public static void init(){
        invertMapClick0.addFallbackName("invertMapClick");
        reliableSync.addFallbackName("reliableSync");


        Events.run(Trigger.update, () -> {
            limitUpdateTimer = (limitUpdateTimer + 1) % 10;
            worldCreator = worldCreator0.get();
            terrainSchematic = terrainSchematic0.get();
            invertMapClick = invertMapClick0.get();
            mockProtocol = ConnectPacket.clientVersion > 0 ? ConnectPacket.clientVersion : Version.build;
            v146Mode = mockProtocol == 146;
            contentsCompatibleMode = mockProtocol != Version.build;
        });
    }
}
