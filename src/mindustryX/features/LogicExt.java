package mindustryX.features;

import arc.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.net.Packets.*;

public class LogicExt{
    public static boolean limitUpdate = false;
    public static int limitDst = 0, limitTimer = 10;
    public static boolean worldCreator = false;
    public static boolean allUnlocked = false;
    public static boolean terrainSchematic = false;
    public static boolean invertMapClick = false;
    public static boolean reliableSync = false;
    public static boolean placeShiftReplacement = false;
    public static boolean v146Mode = false;
    public static boolean contentsCompatibleMode = false;

    public static void init(){
        Events.run(Trigger.update, () -> {
            limitUpdate = Core.settings.getBool("limitupdate");
            limitDst = Core.settings.getInt("limitdst") * Vars.tilesize;
            if(limitUpdate && limitTimer-- < 0){
                limitUpdate = false;
                limitTimer = 10;
            }
            worldCreator = Core.settings.getBool("worldCreator");
            allUnlocked = SettingsV2.allUnlocked.getValue();
            terrainSchematic = Core.settings.getBool("terrainSchematic");
            invertMapClick = Core.settings.getBool("invertMapClick");
            reliableSync = Core.settings.getBool("reliableSync");
            placeShiftReplacement = Core.settings.getBool("placeReplacement");
            v146Mode = ConnectPacket.clientVersion == 146;
            contentsCompatibleMode = ConnectPacket.clientVersion > 0 && ConnectPacket.clientVersion != Version.build;
        });
    }
}