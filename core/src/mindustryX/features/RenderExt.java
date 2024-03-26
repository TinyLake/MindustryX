package mindustryX.features;

import arc.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

public class RenderExt{
    public static void init(){
        Events.run(Trigger.update, () -> {
        });
        Events.run(Trigger.draw, RenderExt::draw);
    }

    private static void draw(){

    }

    public static void onGroupDraw(Drawc t){
        t.draw();
    }
}
