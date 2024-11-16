package mindustryX.features.ui.auxiliary;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustryX.features.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

public class ScriptButtons extends AuxiliaryTools.Table{
    public ScriptButtons(){
        super(UnitTypes.gamma.uiIcon);
        defaults().size(40);

        button(new TextureRegionDrawable(Blocks.buildTower.uiIcon), RStyles.clearLineNonei, iconMed, () -> {
            if(!player.isBuilder()) return;
            int count = 0;
            for(Teams.BlockPlan plan : player.team().data().plans){
                if(player.within(plan.x * tilesize, plan.y * tilesize, buildingRange)){
                    player.unit().addBuild(new BuildPlan(plan.x, plan.y, plan.rotation, content.block(plan.block), plan.config));
                    if(++count >= 255) break;
                }
            }
        }).tooltip("在建造列表加入被摧毁建筑");
        button(Items.copper.emoji(), RStyles.clearLineNoneTogglet, () -> AutoFill.enable ^= true).tooltip("一键装弹").checked((b) -> AutoFill.enable);
        addSettingButton(Icon.modeAttack, "autotarget", "自动攻击", null);
        addSettingButton(new TextureRegionDrawable(UnitTypes.vela.uiIcon), "forceBoost", "强制助推", null);
        addSettingButton(Icon.eyeSmall, "viewMode", "视角脱离玩家", s -> {
            if(s){
                if(control.input instanceof DesktopInput desktopInput){
                    desktopInput.panning = true;
                }
            }else{
                Core.camera.position.set(player);
            }
        });
    }

    protected void addSettingButton(Drawable icon, String settingName, String description, Boolc onClick){
        button(icon, RStyles.clearLineNoneTogglei, iconMed, () -> {
            boolean setting = Core.settings.getBool(settingName);

            Core.settings.put(settingName, !setting);
            UIExt.announce("已" + (setting ? "取消" : "开启") + description);

            if(onClick != null) onClick.get(!setting);
        }).tooltip(description, true).checked(b -> Core.settings.getBool(settingName));
    }
}