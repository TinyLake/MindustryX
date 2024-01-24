package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.scene.style.*;
import mindustry.arcModule.toolpack.*;
import mindustry.content.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/**
 * 专为手机制备的脚本按钮
 */
public class MobileScriptButtons extends ScriptButtons{

    public MobileScriptButtons(){
        icon = new TextureRegionDrawable(UnitTypes.emanate.uiIcon);

        if(mobile){
            toggle();
        }
    }

    @Override
    protected void setup(){
        defaults().size(40);

        scriptButton(Icon.unitsSmall, "指挥模式", () -> control.input.commandMode = !control.input.commandMode).checked(b -> control.input.commandMode);

        scriptButton(Icon.pause, "暂停建造", () -> control.input.isBuilding = !control.input.isBuilding).checked(b -> control.input.isBuilding);

        scriptButton(Icon.up, "捡起载荷", () -> control.input.tryPickupPayload());

        scriptButton(Icon.down, "丢下载荷", () -> control.input.tryDropPayload());

        scriptButton(Blocks.payloadConveyor.uiIcon, "进入传送带", () -> {
            Building build = player.buildOn();

            if(build == null) return;

            Unit unit = player.unit();
            Call.unitBuildingControlSelect(unit, build);
        });

        scriptButton(Blocks.radar.uiIcon, "雷达扫描", () -> arcScanner.mobileRadar = !arcScanner.mobileRadar);

        addSettingButton(StatusEffects.unmoving.uiIcon, "viewMode", "原地静止", s -> {
            if(s) Core.camera.position.set(player);
        });
    }

}
