package mindustry.arcModule;

import arc.*;
import arc.graphics.*;
import mindustry.*;
import mindustry.arcModule.ui.*;
import mindustry.core.*;
import mindustry.game.*;

import static arc.Core.settings;

public class ARCVars{
    public static ARCUI arcui = new ARCUI();
    public static boolean unitHide = false;
    public static boolean limitUpdate = false;
    public static int limitDst = 0;
    public static final int maxBuildPlans = 100;
    public static String arcVersionPrefix = "<ARC~" + Version.mdtXBuild + ">";

    private static Boolean arcInfoControl = false;

    static{
        // 减少性能开销
        Events.run(EventType.Trigger.update, () -> arcInfoControl = Core.settings.getBool("showOtherTeamState"));
    }

    public static String getThemeColorCode(){
        return "[#" + getThemeColor() + "]";
    }

    public static Color getThemeColor(){
        try{
            return Color.valueOf(settings.getString("themeColor"));
        }catch(Exception e){
            return Color.valueOf("ffd37f");
        }
    }

    public static Color getPlayerEffectColor(){
        try{
            return Color.valueOf(settings.getString("playerEffectColor"));
        }catch(Exception e){
            return Color.valueOf("ffd37f");
        }
    }

    public static Boolean arcInfoControl(Team team){
        return team == Vars.player.team() || arcInfoControl ||
        Vars.player.team().id == 255 || Vars.state.rules.mode() != Gamemode.pvp;
    }
}
