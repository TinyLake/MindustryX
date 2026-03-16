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
import mindustryX.*;
import mindustryX.features.*;
import mindustryX.features.ui.toolTable.ai.*;

import static mindustry.Vars.*;
import static mindustryX.features.UIExt.i;

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
        aiButton(new ArcMinerAI(), UnitTypes.mono.region, i("ui.simple.miner-ai"));
        aiButton(new BuilderAI(), UnitTypes.poly.region, i("ui.simple.builder-ai"));
        aiButton(new RepairAI(), UnitTypes.mega.region, i("ui.simple.repair-ai"));
        aiButton(new DefenderAI(), UnitTypes.oct.region, i("ui.simple.defender-ai"));
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
        }).tooltip(i("ui.simple.add-destroyed-buildings-to-build-queue"));
        var t = button(new TextureRegionDrawable(Items.copper.uiIcon), Styles.clearNoneTogglei, () -> AutoFill.enable ^= true).tooltip(i("ui.simple.auto-fill")).checked((b) -> AutoFill.enable).get();
        SettingsV2.bindQuickSettings(t, AutoFill.INSTANCE.getSettings());
        toggleButton(Icon.modeAttack, "autotarget", i("ui.simple.auto-attack"));
        toggleButton(new TextureRegionDrawable(UnitTypes.vela.uiIcon), "forceBoost", i("ui.simple.force-boost"));
        toggleButton(Icon.eyeSmall, "detach-camera", i("ui.simple.detached-camera"));

        if(!mobile) return;
        row();
        toggleButton(Icon.unitsSmall, i("ui.simple.command-mode"), () -> control.input.commandMode = !control.input.commandMode).checked(b -> control.input.commandMode);
        toggleButton(Icon.pause, i("ui.simple.construction-suspended"), () -> control.input.isBuilding = !control.input.isBuilding).checked(b -> control.input.isBuilding);
        scriptButton(Icon.up, i("ui.simple.pick-up-payload"), () -> control.input.tryPickupPayload());
        scriptButton(Icon.down, i("ui.simple.drop-payload"), () -> control.input.tryDropPayload());
        scriptButton(new TextureRegionDrawable(Blocks.payloadConveyor.uiIcon), i("ui.simple.enter-the-conveyor-belt"), () -> {
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
            String state = setting ? i("ui.simple.off") : i("ui.simple.on");
            UIExt.announce(VarsX.bundle.toggleState(description, state));
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

        BaseDialog dialog = new BaseDialog(i("ui.simple.arc-ai-configurator"));

        dialog.cont.table(t -> {
            t.add(i("ui.simple.miner-ai-ore-filter")).color(Pal.accent).pad(cols / 2f).center().row();
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
