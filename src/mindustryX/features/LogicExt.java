package mindustryX.features;

import arc.*;
import mindustry.*;
import mindustry.game.EventType.*;

public class LogicExt{
    public static boolean limitUpdate = false;
    public static int limitDst = 0, limitTimer = 10;
    public static boolean worldCreator = false;
    public static boolean allUnlocked = false;
    public static boolean terrainSchematic = false;
    public static boolean invertMapClick = false;
    public static boolean reliableSync = false;
    public static boolean placeShiftReplacement = false;

    public static void init(){
        Events.run(Trigger.update, () -> {
            limitUpdate = Core.settings.getBool("limitupdate");
            limitDst = Core.settings.getInt("limitdst") * Vars.tilesize;
            if(limitUpdate && limitTimer-- < 0){
                limitUpdate = false;
                limitTimer = 10;
            }
            worldCreator = Core.settings.getBool("worldCreator");
            allUnlocked = Core.settings.getBool("allUnlocked");
            terrainSchematic = Core.settings.getBool("terrainSchematic");
            invertMapClick = Core.settings.getBool("invertMapClick");
            reliableSync = Core.settings.getBool("reliableSync");
            placeShiftReplacement = Core.settings.getBool("placeReplacement");
        });
    }
}