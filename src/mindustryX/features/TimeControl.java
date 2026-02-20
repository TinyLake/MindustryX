package mindustryX.features;

import arc.Events;
import arc.func.Floatp;
import arc.math.WindowedMean;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.ui.*;

//move from mindustry.arcModule.TimeControl
public class TimeControl{
    public static float gameSpeed = 1f;
    public static int targetFps = 60;
    public static boolean fpsLock = false;

    private static final WindowedMean gameSpeedBalance = new WindowedMean(120);
    private static Floatp origin;
    private static final Floatp deltaProvider = () -> {
        float delta = origin.get();
        if(fpsLock){
            gameSpeedBalance.add(60f / (delta * targetFps));
            return 60f / targetFps;
        }else{
            return delta * gameSpeed;
        }
    };

    public static void init(){
        origin = Reflect.get(Time.class, "deltaimpl");
        Events.on(EventType.ResetEvent.class, e -> {
            gameSpeed = 1f;
            targetFps = 60;
            fpsLock = false;
        });
    }


    public static void setGameSpeed(float speed){
        gameSpeed = speed;
        if(fpsLock){
            fpsLock = false;
            Vars.ui.announce(mindustryX.bundles.UiTexts.uiFpsLockOff(gameSpeed));
        }else{
            Vars.ui.announce(mindustryX.bundles.UiTexts.uiCurrentGameSpeed(gameSpeed));
        }
        Time.setDeltaProvider(gameSpeed == 1f ? origin : deltaProvider);
    }

    public static void setFpsLock(){
        gameSpeedBalance.clear();
        fpsLock = true;
        Vars.ui.announce(mindustryX.bundles.UiTexts.uiFpsLockEnabled(targetFps));
        Time.setDeltaProvider(deltaProvider);
    }

    public static float getGameSpeed(){
        if(fpsLock) return gameSpeedBalance.rawMean();
        return gameSpeed;
    }

    public static void draw(Table table){
        table.label(() -> "x" + Strings.autoFixed(getGameSpeed(), 2)).width(18f * 3);

        table.button("/2", Styles.cleart, () -> setGameSpeed(gameSpeed * 0.5f)).tooltip(mindustryX.bundles.UiTexts.bundle().i("将时间流速放慢到一半")).size(40f, 30f); // 原文本:将时间流速放慢到一半
        table.button("×2", Styles.cleart, () -> setGameSpeed(gameSpeed * 2f)).tooltip(mindustryX.bundles.UiTexts.bundle().i("将时间流速加快到两倍")).size(40f, 30f); // 原文本:将时间流速加快到两倍
        table.button("[red]S", Styles.cleart, () -> setGameSpeed(0f)).tooltip(mindustryX.bundles.UiTexts.bundle().i("暂停时间")).size(30f, 30f); // 原文本:暂停时间
        table.button("[green]N", Styles.cleart, () -> setGameSpeed(1f)).tooltip(mindustryX.bundles.UiTexts.bundle().i("恢复原速")).size(30f, 30f); // 原文本:恢复原速
        table.button("[white]F", Styles.cleart, TimeControl::setFpsLock).tooltip(mindustryX.bundles.UiTexts.bundle().i("帧率模拟")).size(30f, 30f); // 原文本:帧率模拟

        table.field(Integer.toString(targetFps), s -> {
            int num = Integer.parseInt(s);
            if(num < 2 || num > 10000) return;
            targetFps = num;
            if(fpsLock){
                Vars.ui.announce(mindustryX.bundles.UiTexts.uiFpsLockCurrent(targetFps));
            }
        }).valid(s -> {
            if(!Strings.canParsePositiveInt(s)) return false;
            int num = Integer.parseInt(s);
            return 2 <= num && num < 10000;
        }).tooltip(mindustryX.bundles.UiTexts.bundle().i("允许的范围：2~9999")).size(80f, 30f); // 原文本:允许的范围：2~9999
    }
}
