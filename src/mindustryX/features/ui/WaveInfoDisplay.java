package mindustryX.features.ui;

import arc.*;
import arc.scene.*;
import arc.scene.event.*;
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
import mindustryX.features.SettingsV2.*;

import static mindustry.Vars.*;

public class WaveInfoDisplay extends Table{
    public static SettingsV2.Data<Boolean> enable = new CheckPref("gameUI.newWaveInfoDisplay", true);
    public static final float fontScl = 0.8f;
    private int waveOffset = 0;
    private final WaveInfoDialog waveInfoDialog = new WaveInfoDialog();
    private final Table waveInfo;

    static{
        enable.addFallbackName("newWaveInfoDisplay");
    }

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

            buttons.button(Icon.waves, Styles.clearNonei, iconMed, waveInfoDialog::show).tooltip("@waveInfoDisplay.waveInfo");

            buttons.button("<", Styles.cleart, () -> shiftWaveOffset(-1));
            var i = buttons.button("", Styles.cleart, this::setWaveOffsetDialog).minHeight(48).maxWidth(160f).get();
            i.getLabel().setAlignment(Align.center);
            i.getLabel().setText(() -> "" + (state.wave + waveOffset));
            buttons.button(">", Styles.cleart, () -> shiftWaveOffset(1));

            buttons.button("R", Styles.cleart, () -> setWaveOffset(0)).tooltip("@waveInfoDisplay.restoreCurrent");
            buttons.button("J", Styles.cleart, () -> ui.showConfirm(Core.bundle.get("waveInfoDisplay.cheatFunction"), () -> {
                state.wave += waveOffset;
                setWaveOffset(0);
            })).tooltip("@waveInfoDisplay.forceSkip").disabled((b) -> net.client());

            buttons.button(Icon.settingsSmall, Styles.clearNonei, iconMed, () -> UIExtKt.showFloatSettingsPanel(table -> {
                for(var it : UIExt.coreItems.settings){
                    it.buildUI(table);
                }
            })).tooltip("@waveInfoDisplay.configResources");
            buttons.button(Icon.eyeOffSmall, Styles.clearNonei, iconMed, () -> enable.set(false)).tooltip("@waveInfoDisplay.hideWaveDisplay");

            buttons.add().growX();
            buttons.add("♐>");
            buttons.button(Icon.wavesSmall, Styles.clearNonei, iconMed, () -> shareWaveInfo(state.wave + waveOffset)).tooltip("@waveInfoDisplay.shareWaveInfo");
            buttons.button(Icon.powerSmall, Styles.clearNonei, iconMed, () -> UIExt.coreItems.sharePowerInfo()).tooltip("@waveInfoDisplay.sharePowerInfo");
            buttons.button(new TextureRegionDrawable(Items.copper.uiIcon), Styles.clearNonei, iconSmall, () -> UIExt.coreItems.shareItemInfo()).tooltip("@waveInfoDisplay.shareItemInfo");
            buttons.button(Icon.unitsSmall, Styles.clearNonei, iconMed, () -> UIExt.coreItems.shareUnitInfo()).tooltip("@waveInfoDisplay.shareUnitInfo");
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
        }).growX();
    }

    public void shareWaveInfo(int wave){
        if(!state.rules.waves) return;
        StringBuilder builder = new StringBuilder();
        builder.append(Core.bundle.format("waveInfoDisplay.wave", wave));
        if(wave >= state.wave){
            builder.append("(");
            if(wave > state.wave){
                builder.append(Core.bundle.format("waveInfoDisplay.remaining", wave - state.wave));
            }
            int timer = (int)(state.wavetime + (wave - state.wave) * state.rules.waveSpacing);
            builder.append(FormatDefault.duration((float)timer / 60)).append(")");
        }
        builder.append(Core.bundle.get("waveInfoDisplay.waveColon"));

        builder.append(ArcMessageDialog.getWaveInfo(wave));
        UIExt.shareMessage(Iconc.waves, builder.toString());
    }

    public Element wrapped(){
        var ret = new Table();
        ret.collapser(UIExt.coreItems, () -> UIExt.coreItems.enable.get()).touchable(Touchable.disabled).growX().row();
        ret.add().height(4).row();
        ret.collapser(this, () -> enable.getValue()).growX().row();
        ret.collapser(tt -> tt.button(Icon.downOpen, Styles.emptyi, () -> enable.set(true)), () -> !enable.getValue()).center().row();
        return ret;
    }

    private void setWaveOffsetDialog(){
        Dialog lsSet = new BaseDialog("@waveInfoDisplay.waveSettingTitle");
        lsSet.cont.add("@waveInfoDisplay.setQueryWave").padRight(5f).left();
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
