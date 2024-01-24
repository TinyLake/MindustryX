package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.arcModule.*;
import mindustry.arcModule.ai.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.getThemeColor;
import static mindustry.arcModule.ai.ArcMinerAI.*;
import static mindustry.arcModule.ui.RStyles.clearLineNoneTogglei;
import static mindustry.content.UnitTypes.*;
import static mindustry.ui.Styles.flatToggleMenut;

public class AITools extends BaseToolsTable{
    private AIController selectAI;

    public AITools(){
        super(Icon.android);

        Events.run(EventType.Trigger.update, () -> {
            if(selectAI != null){
                selectAI.unit(player.unit());
                selectAI.updateUnit();
            }
        });
    }

    @Override
    public void setup(){
        button(Icon.settingsSmall, clearLineNoneTogglei, 30, this::showSettingDialog);

        if(false) aiButton(new ATRIAI(), Blocks.worldProcessor.region, "ATRI AI");
        aiButton(new ArcMinerAI(), mono.region, "矿机AI");
        aiButton(new ArcBuilderAI(), poly.region, "重建AI");
        aiButton(new ArcRepairAI(), mega.region, "修复AI");
        aiButton(new DefenderAI(), oct.region, "保护AI");
    }

    private void aiButton(AIController ai, TextureRegion textureRegion, String describe){
        button(new TextureRegionDrawable(textureRegion), clearLineNoneTogglei, 30, () -> selectAI(ai))
        .checked(b -> selectAI == ai).size(40).with(b -> ElementUtils.tooltip(b, describe));
    }

    private void selectAI(AIController ai){
        selectAI = selectAI == ai ? null : ai;
    }

    private void showSettingDialog(){
        int cols = (int)Math.max(Core.graphics.getWidth() / Scl.scl(480), 1);

        BaseDialog dialog = new BaseDialog("ARC-AI设定器");

        dialog.cont.table(t -> {
            t.add("minerAI-矿物筛选器").color(getThemeColor()).pad(cols / 2f).center().row();

            t.image().color(getThemeColor()).fillX().row();

            t.table(c -> {
                c.add("地表矿").row();

                c.table(list -> {
                    int i = 0;
                    for(Block block : ArcMinerAI.oreAllList){
                        if(indexer.floorOresCount[block.id] == 0) continue;
                        if(i++ % 3 == 0) list.row();
                        list.button(block.emoji() + "\n" + indexer.floorOresCount[block.id], flatToggleMenut, () -> {
                            if(oreList.contains(block)) oreList.remove(block);
                            else if(!oreList.contains(block)) oreList.add(block);
                        }).tooltip(block.localizedName).checked(k -> oreList.contains(block)).width(100f).height(50f);
                    }
                }).row();

                c.add("墙矿").row();

                c.table(list -> {
                    int i = 0;
                    for(Block block : oreAllWallList){
                        if(indexer.wallOresCount[block.id] == 0) continue;
                        if(i++ % 3 == 0) list.row();
                        list.button(block.emoji() + "\n" + indexer.wallOresCount[block.id], flatToggleMenut, () -> {
                            if(oreWallList.contains(block)) oreWallList.remove(block);
                            else if(!oreWallList.contains(block)) oreWallList.add(block);
                        }).tooltip(block.localizedName).checked(k -> oreWallList.contains(block)).width(100f).height(50f);
                    }
                }).row();

            }).growX();
        }).growX();

        dialog.cont.row();

        dialog.cont.table(t -> {
            t.add("builderAI").color(getThemeColor()).pad(cols / 2f).center().row();

            t.image().color(getThemeColor()).fillX().row();

            t.table(tt -> {
                tt.add("重建冷却时间: ");

                TextField sField = tt.field(ArcBuilderAI.rebuildTime + "", text -> ArcBuilderAI.rebuildTime = Math.max(5f, Float.parseFloat(text))).valid(Strings::canParsePositiveFloat).width(200f).get();

                tt.slider(5, 200, 5, i -> {
                    ArcBuilderAI.rebuildTime = i;
                    sField.setText(ArcBuilderAI.rebuildTime + "");
                }).width(200f);
            }).growX();
        }).growX();

        dialog.addCloseButton();
        dialog.show();
    }

}
