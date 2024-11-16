package mindustryX.features.ui.auxiliary;

import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustryX.features.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

/**
 * 专为手机制备的脚本按钮
 */
public class MobileScriptButtons extends AuxiliaryTools.Table{

    public MobileScriptButtons(){
        super(UnitTypes.emanate.uiIcon);
        defaults().size(40);
        if(mobile) shown = true;

        scriptButton(Icon.unitsSmall, "指挥模式", () -> control.input.commandMode = !control.input.commandMode).checked(b -> control.input.commandMode);
        scriptButton(Icon.pause, "暂停建造", () -> control.input.isBuilding = !control.input.isBuilding).checked(b -> control.input.isBuilding);
        scriptButton(Icon.up, "捡起载荷", () -> control.input.tryPickupPayload());
        scriptButton(Icon.down, "丢下载荷", () -> control.input.tryDropPayload());
        scriptButton(Blocks.payloadConveyor.uiIcon, "进入传送带", () -> {
            Building build = player.buildOn();
            if(build == null || player.dead()) return;
            Call.unitBuildingControlSelect(player.unit(), build);
        });
    }

    protected void scriptButton(TextureRegion region, String description, Runnable runnable){
        scriptButton(new TextureRegionDrawable(region), description, runnable);
    }

    protected Cell<ImageButton> scriptButton(Drawable icon, String description, Runnable runnable){
        return button(icon, RStyles.clearLineNonei, iconMed, runnable).tooltip(description, true);
    }
}
