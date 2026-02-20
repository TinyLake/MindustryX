package mindustryX.features.ui.toolTable;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.features.*;
import mindustryX.features.ui.toolTable.ai.*;

import static mindustry.Vars.*;

public class AuxiliaryTools extends Table{
    private AIController selectAI;

    public AuxiliaryTools(){
        background(Styles.black6);
        rebuild();
        Events.run(EventType.Trigger.update, () -> {
            if(selectAI != null && !player.dead()){
                if(selectAI instanceof BuilderAI builder){
                    builder.rebuildPeriod = 10f;
                }
                selectAI.unit(player.unit());
                selectAI.updateUnit();
                player.boosting = player.unit().isShooting;
            }
        });
    }

    protected void rebuild(){
        defaults().size(40);
        aiButton(new ArcMinerAI(), UnitTypes.mono.region, mindustryX.bundles.UiTextBundle.i("矿机AI")); // 原文本:矿机AI
        aiButton(new BuilderAI(), UnitTypes.poly.region, mindustryX.bundles.UiTextBundle.i("重建AI")); // 原文本:重建AI
        aiButton(new RepairAI(), UnitTypes.mega.region, mindustryX.bundles.UiTextBundle.i("修复AI")); // 原文本:修复AI
        aiButton(new DefenderAI(), UnitTypes.oct.region, mindustryX.bundles.UiTextBundle.i("保护AI")); // 原文本:保护AI
        button(Icon.settingsSmall, Styles.clearNonei, iconMed, this::showAiSettingDialog);

        row();
        button(new TextureRegionDrawable(Blocks.buildTower.uiIcon), Styles.clearNonei, iconMed, () -> {
            if(!player.isBuilder()) return;
            int count = 0;
            for(Teams.BlockPlan plan : player.team().data().plans){
                if(player.within(plan.x * tilesize, plan.y * tilesize, buildingRange)){
                    player.unit().addBuild(new BuildPlan(plan.x, plan.y, plan.rotation, plan.block, plan.config));
                    if(++count >= 255) break;
                }
            }
        }).tooltip(mindustryX.bundles.UiTextBundle.i("在建造列表加入被摧毁建筑")); // 原文本:在建造列表加入被摧毁建筑
        var t = button(new TextureRegionDrawable(Items.copper.uiIcon), Styles.clearNoneTogglei, () -> AutoFill.enable ^= true).tooltip(mindustryX.bundles.UiTextBundle.i("一键装填")).checked((b) -> AutoFill.enable).get(); // 原文本:一键装填
        SettingsV2.bindQuickSettings(t, AutoFill.INSTANCE.getSettings());
        toggleButton(Icon.modeAttack, "autotarget", mindustryX.bundles.UiTextBundle.i("自动攻击")); // 原文本:自动攻击
        toggleButton(new TextureRegionDrawable(UnitTypes.vela.uiIcon), "forceBoost", mindustryX.bundles.UiTextBundle.i("强制助推")); // 原文本:强制助推
        toggleButton(Icon.eyeSmall, "detach-camera", mindustryX.bundles.UiTextBundle.i("视角脱离玩家")); // 原文本:视角脱离玩家

        if(!mobile) return;
        row();
        toggleButton(Icon.unitsSmall, mindustryX.bundles.UiTextBundle.i("指挥模式"), () -> control.input.commandMode = !control.input.commandMode).checked(b -> control.input.commandMode); // 原文本:指挥模式
        toggleButton(Icon.pause, mindustryX.bundles.UiTextBundle.i("暂停建造"), () -> control.input.isBuilding = !control.input.isBuilding).checked(b -> control.input.isBuilding); // 原文本:暂停建造
        scriptButton(Icon.up, mindustryX.bundles.UiTextBundle.i("捡起载荷"), () -> control.input.tryPickupPayload()); // 原文本:捡起载荷
        scriptButton(Icon.down, mindustryX.bundles.UiTextBundle.i("丢下载荷"), () -> control.input.tryDropPayload()); // 原文本:丢下载荷
        scriptButton(new TextureRegionDrawable(Blocks.payloadConveyor.uiIcon), mindustryX.bundles.UiTextBundle.i("进入传送带"), () -> { // 原文本:进入传送带
            Building build = player.buildOn();
            if(build == null || player.dead()) return;
            Call.unitBuildingControlSelect(player.unit(), build);
        });
    }

    private void aiButton(AIController ai, TextureRegion textureRegion, String describe){
        button(new TextureRegionDrawable(textureRegion), Styles.clearNoneTogglei, iconMed, () -> selectAI = selectAI == ai ? null : ai).checked(b -> selectAI == ai).tooltip(describe);
    }


    protected void toggleButton(Drawable icon, String settingName, String description){
        button(icon, Styles.clearNoneTogglei, iconMed, () -> {
            boolean setting = Core.settings.getBool(settingName);

            Core.settings.put(settingName, !setting);
            String state = setting ? mindustryX.bundles.UiTextBundle.i("关闭") : mindustryX.bundles.UiTextBundle.i("开启"); // 原文本:关闭 | 开启
            UIExt.announce(mindustryX.bundles.UiTextBundle.uiToggleState(description, state));
        }).tooltip(description, true).checked(b -> Core.settings.getBool(settingName));
    }

    protected Cell<ImageButton> toggleButton(Drawable icon, String description, Runnable runnable){
        return button(icon, Styles.clearNonei, iconMed, runnable).tooltip(description, true);
    }

    protected void scriptButton(Drawable icon, String description, Runnable runnable){
        button(icon, Styles.clearNonei, iconMed, runnable).tooltip(description, true);
    }

    private void showAiSettingDialog(){
        int cols = (int)Math.max(Core.graphics.getWidth() / Scl.scl(480), 1);

        BaseDialog dialog = new BaseDialog(mindustryX.bundles.UiTextBundle.i("ARC-AI设定器")); // 原文本:ARC-AI设定器

        dialog.cont.table(t -> {
            t.add(mindustryX.bundles.UiTextBundle.i("minerAI-矿物筛选器")).color(Pal.accent).pad(cols / 2f).center().row(); // 原文本:minerAI-矿物筛选器
            t.image().color(Pal.accent).fillX().row();
            t.table(list -> {
                int i = 0;
                for(Item item : content.items()){
                    if(!indexer.hasOre(item) && !indexer.hasWallOre(item)) continue;
                    if(i++ % 3 == 0) list.row();
                    list.button(item.emoji() + "\n" + indexer.allOres.get(item) + "/" + indexer.allWallOres.get(item), Styles.flatToggleMenut, () -> {
                        if(ArcMinerAI.toMine.contains(item)) ArcMinerAI.toMine.remove(item);
                        else if(!ArcMinerAI.toMine.contains(item)) ArcMinerAI.toMine.add(item);
                    }).tooltip(item.localizedName).checked(k -> ArcMinerAI.toMine.contains(item)).width(100f).height(50f);
                }
            }).growX();
        }).growX().row();

        dialog.addCloseButton();
        dialog.show();
    }
}
