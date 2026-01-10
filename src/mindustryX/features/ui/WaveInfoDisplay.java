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

            buttons.button(Icon.waves, Styles.clearNonei, iconMed, waveInfoDialog::show).tooltip("波次信息");

            buttons.button("<", Styles.cleart, () -> shiftWaveOffset(-1));
            var i = buttons.button("", Styles.cleart, this::setWaveOffsetDialog).minHeight(48).maxWidth(160f).get();
            i.getLabel().setAlignment(Align.center);
            i.getLabel().setText(() -> "" + (state.wave + waveOffset));
            buttons.button(">", Styles.cleart, () -> shiftWaveOffset(1));

            buttons.button("R", Styles.cleart, () -> setWaveOffset(0)).tooltip("恢复当前波次");
            buttons.button("J", Styles.cleart, () -> ui.showConfirm("[red]这是一个作弊功能[]\n快速跳转到目标波次(不刷兵)", () -> {
                state.wave += waveOffset;
                setWaveOffset(0);
            })).tooltip("强制跳波").disabled((b) -> net.client());

            buttons.add().growX();
            buttons.add("♐>");
            buttons.button(Icon.wavesSmall, Styles.clearNonei, iconMed, () -> ShareFeature.shareWaveInfo(state.wave + waveOffset)).tooltip("分享波次信息");
            buttons.button(Icon.powerSmall, Styles.clearNonei, iconMed, () -> UIExt.coreItems.sharePowerInfo()).tooltip("分享电力情况");
            buttons.button(new TextureRegionDrawable(Items.copper.uiIcon), Styles.clearNonei, iconSmall, () -> UIExt.coreItems.shareItemInfo()).tooltip("分享库存情况");
            buttons.button(Icon.unitsSmall, Styles.clearNonei, iconMed, () -> UIExt.coreItems.shareUnitInfo()).tooltip("分享单位数量");
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
        Dialog lsSet = new BaseDialog("波次设定");
        lsSet.cont.add("设定查询波次").padRight(5f).left();
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
