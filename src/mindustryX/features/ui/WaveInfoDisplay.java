package mindustryX.features.ui;

import arc.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.features.*;

import static mindustry.Vars.*;

public class WaveInfoDisplay extends Table{
    public static final float fontScl = 0.8f;
    private int waveOffset = 0;
    private final WaveInfoDialog waveInfoDialog = new WaveInfoDialog();
    private final Table waveInfo;

    public WaveInfoDisplay(){
        super(Tex.pane);
        Events.on(WorldLoadEvent.class, e -> {
            waveOffset = 0;
            rebuildWaveInfo();
        });
        Events.on(WaveEvent.class, e -> rebuildWaveInfo());

        margin(0, 4, 0, 4);
        table(buttons -> {
            buttons.defaults().size(32);
            buttons.add().growX();

            buttons.button(Icon.waves, Styles.clearNonei, iconMed, waveInfoDialog::show).tooltip(mindustryX.bundles.FuncX.ui("wave_info")); // 原文本:波次信息

            buttons.button("<", Styles.cleart, () -> shiftWaveOffset(-1));
            var i = buttons.button("", Styles.cleart, this::setWaveOffsetDialog).minHeight(48).maxWidth(160f).get();
            i.getLabel().setAlignment(Align.center);
            i.getLabel().setText(() -> "" + (state.wave + waveOffset));
            buttons.button(">", Styles.cleart, () -> shiftWaveOffset(1));

            buttons.button("R", Styles.cleart, () -> setWaveOffset(0)).tooltip(mindustryX.bundles.FuncX.ui("restore_current_wave")); // 原文本:恢复当前波次
            buttons.button("J", Styles.cleart, () -> ui.showConfirm(mindustryX.bundles.FuncX.ui("this_is_a_cheat_feature_njump_to_the"), () -> { // 原文本:[red]这是一个作弊功能[]\n快速跳转到目标波次(不刷兵)
                state.wave += waveOffset;
                setWaveOffset(0);
            })).tooltip(mindustryX.bundles.FuncX.ui("force_skip_waves")).disabled((b) -> net.client()); // 原文本:强制跳波

            buttons.add().growX();
            buttons.add("♐>");
            buttons.button(Icon.wavesSmall, Styles.clearNonei, iconMed, () -> ShareFeature.shareWaveInfo(state.wave + waveOffset)).tooltip(mindustryX.bundles.FuncX.ui("share_wave_information")); // 原文本:分享波次信息
            buttons.button(Icon.powerSmall, Styles.clearNonei, iconMed, ShareFeature::shareTeamPower).tooltip(mindustryX.bundles.FuncX.ui("share_power_status")); // 原文本:分享电力情况
            buttons.button(new TextureRegionDrawable(Items.copper.uiIcon), Styles.clearNonei, iconSmall, ShareFeature::openShareItemDialog).tooltip(mindustryX.bundles.FuncX.ui("share_inventory_status")); // 原文本:分享库存情况
            buttons.button(Icon.unitsSmall, Styles.clearNonei, iconMed, ShareFeature::openShareUnitDialog).tooltip(mindustryX.bundles.FuncX.ui("share_unit_count")); // 原文本:分享单位数量
        }).fillX().row();

        waveInfo = new Table().left().top();
        add(new ScrollPane(waveInfo, Styles.noBarPane){
            {
                setScrollingDisabledY(true);
                setForceScroll(true, false);
                // 自动失焦
                update(() -> {
                    if(hasScroll() && !hasMouse()){
                        Core.scene.setScrollFocus(null);
                    }
                });
            }

            @Override
            public float getPrefWidth(){
                return 0f;
            }

            @Override
            public float getMinHeight(){
                return getPrefHeight();
            }
        }).growX().fillY().row();

        add(new OverlayUI.PreferAnyWidth()).fillX().row();
    }

    private void setWaveOffsetDialog(){
        Dialog lsSet = new BaseDialog(mindustryX.bundles.FuncX.ui("wave_settings")); // 原文本:波次设定
        lsSet.cont.add(mindustryX.bundles.FuncX.ui("set_target_wave")).padRight(5f).left(); // 原文本:设定查询波次
        TextField field = lsSet.cont.field(state.wave + waveOffset + "", text -> waveOffset = Integer.parseInt(text) - state.wave).size(320f, 54f).valid(Strings::canParsePositiveInt).maxTextLength(100).get();
        lsSet.cont.row();
        lsSet.cont.slider(1, ArcWaveSpawner.calWinWaveClamped(), 1, res -> {
            waveOffset = (int)res - state.wave;
            field.setText((int)res + "");
        }).fillX().colspan(2);
        lsSet.addCloseButton();
        lsSet.show();
    }

    private void rebuildWaveInfo(){
        waveInfo.clearChildren();

        int curInfoWave = state.wave + waveOffset - 1;
        StringBuilder builder = new StringBuilder();
        for(SpawnGroup group : state.rules.spawns){
            int amount = group.getSpawned(curInfoWave);
            if(amount == 0) continue;

            waveInfo.table(groupT -> {
                groupT.center().image(group.type.uiIcon).scaling(Scaling.fit).size(iconSmall);
                if(amount > 1) groupT.add("x" + amount, fontScl);
                groupT.row();

                builder.setLength(0);
                if(group.effect != null && group.effect != StatusEffects.none) builder.append(group.effect.emoji());
                float shield = group.getShield(curInfoWave);
                if(shield > 0) builder.append(FormatDefault.format(shield));
                groupT.add(builder.toString()).colspan(groupT.getColumns());
            }).pad(0, 4, 0, 4).left().top();
        }
    }

    private void shiftWaveOffset(int shiftCount){
        int offset = Math.max(waveOffset + shiftCount, -state.wave + 1);
        setWaveOffset(offset);
    }

    private void setWaveOffset(int waveOffset){
        this.waveOffset = waveOffset;
        rebuildWaveInfo();
    }
}
