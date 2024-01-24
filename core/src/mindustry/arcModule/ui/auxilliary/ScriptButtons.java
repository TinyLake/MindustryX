package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import mindustry.arcModule.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.arcModule.ui.RStyles.clearLineNonei;
import static mindustry.content.UnitTypes.vela;

public class ScriptButtons extends BaseToolsTable{

    public ScriptButtons(){
        super(UnitTypes.gamma.uiIcon);
    }

    @Override
    protected void setup(){
        defaults().size(40);

        scriptButton(Blocks.buildTower.uiIcon, "在建造列表加入被摧毁建筑", () -> player.buildDestroyedBlocks());

        scriptButton(Blocks.message.uiIcon, "锁定上个标记点", Marker::lockonLastMark);

        scriptButton(Items.copper.uiIcon, "一键放置", () -> player.dropItems());

        scriptButton(Icon.pencilSmall, "特效显示", () -> EffectsDialog.withAllEffects().show());

        addSettingButton(Icon.modeAttack, "autotarget", "自动攻击", s -> {
        });

        addSettingButton(vela.uiIcon, "forceBoost", "强制助推", s -> {
        });

        if(!mobile){
            addSettingButton(Icon.eyeSmall, "removePan", "视角脱离玩家", s -> {
                if(control.input instanceof DesktopInput desktopInput){
                    desktopInput.panning = true;
                }
            });
        }
    }

    protected void addSettingButton(TextureRegion region, String settingName, String description, Boolc onClick){
        addSettingButton(new TextureRegionDrawable(region), settingName, description, onClick);
    }

    protected void addSettingButton(Drawable icon, String settingName, String description, Boolc onClick){
        scriptButton(icon, description, () -> {
            boolean setting = Core.settings.getBool(settingName);

            Core.settings.put("removePan", !setting);
            arcui.arcInfo("已" + (setting ? "取消" : "开启") + description);

            onClick.get(!setting);
        }).checked(b -> Core.settings.getBool(settingName));
    }

    protected Cell<ImageButton> scriptButton(TextureRegion region, String description, Runnable runnable){
        return scriptButton(new TextureRegionDrawable(region), description, runnable);
    }

    protected Cell<ImageButton> scriptButton(Drawable icon, String description, Runnable runnable){
        return scriptButton(icon, clearLineNonei, description, runnable);
    }

    protected Cell<ImageButton> scriptButton(Drawable icon, ImageButtonStyle style, String description, Runnable runnable){
        Cell<ImageButton> cell = button(icon, style, 30, runnable);

        ElementUtils.tooltip(cell.get(), description);

        return cell;
    }

}
