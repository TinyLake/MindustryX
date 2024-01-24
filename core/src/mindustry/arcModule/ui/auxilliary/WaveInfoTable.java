package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.arcModule.*;
import mindustry.arcModule.ui.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.state;
import static mindustry.arcModule.toolpack.arcWaveSpawner.calWinWave;
import static mindustry.arcModule.ui.RStyles.*;

public class WaveInfoTable extends BaseToolsTable{
    public static float fontScl = 0.8f;

    private int waveOffset = 0;

    private final Table waveInfo;

    private final ArcWaveInfoDialog waveInfoDialog = new ArcWaveInfoDialog();

    public WaveInfoTable(){
        super(Icon.waves);

        Events.on(WorldLoadEvent.class, e -> {
            waveOffset = 0;
            rebuildWaveInfo();
        });

        Events.on(WaveEvent.class, e -> rebuildWaveInfo());

        waveInfo = new Table(Tex.pane);
    }

    @Override
    protected void setup(){
        left().top();
        waveInfo.left().top();

        button(Icon.waves, clearAccentNonei, waveInfoDialog::show).size(40).tooltip("波次信息");

        table(buttons -> {
            buttons.defaults().size(40);

            buttons.button("<", clearLineNonet, () -> shiftWaveOffset(-1));

            buttons.button("O", clearLineNonet, () -> setWaveOffset(0));

            buttons.button(">", clearLineNonet, () -> shiftWaveOffset(1));

            buttons.button("Go", clearLineNonet, () -> {
                state.wave += waveOffset;
                setWaveOffset(0);
            });

            buttons.button("♐", clearLineNonet, () -> {
                String message = RFuncs.arcShareWaveInfo(state.wave + waveOffset);
                int seperator = 145;
                for(int i = 0; i < message.length() / (float)seperator; i++){
                    Call.sendChatMessage(message.substring(i * seperator, Math.min(message.length(), (i + 1) * seperator)));
                }
            }).get().setDisabled(() -> !state.rules.waves && !Core.settings.getBool("arcShareWaveInfo"));

        }).left().row();

        table(setWave -> {
            setWave.label(() -> "" + getDisplayWaves()).get().setFontScale(fontScl);

            setWave.row();

            setWave.button(Icon.settingsSmall, clearAccentNonei, 30, () -> {
                Dialog lsSet = new BaseDialog("波次设定");
                lsSet.cont.add("设定查询波次").padRight(5f).left();
                TextField field = lsSet.cont.field(state.wave + waveOffset + "", text -> waveOffset = Integer.parseInt(text) - state.wave).size(320f, 54f).valid(Strings::canParsePositiveInt).maxTextLength(100).get();
                lsSet.cont.row();
                lsSet.cont.slider(1, calWinWave(), 1, res -> {
                    waveOffset = (int)res - state.wave;
                    field.setText((int)res + "");
                });
                lsSet.addCloseButton();
                lsSet.show();
            });
        });

        pane(Styles.noBarPane, waveInfo).scrollY(false).pad(8f).maxWidth(300f).left();
    }

    private void rebuildWaveInfo(){
        waveInfo.clearChildren();

        int curInfoWave = getDisplayWaves();
        for(SpawnGroup group : state.rules.spawns){
            int amount = group.getSpawned(curInfoWave);

            if(amount == 0) continue;

            float shield = group.getShield(curInfoWave);
            StatusEffect effect = group.effect;

            waveInfo.table(groupT -> {
                groupT.image(group.type.uiIcon).scaling(Scaling.fit).size(20).row();

                groupT.add("" + amount, fontScl).row();

                groupT.add((shield > 0 ? UI.formatAmount((long)shield) : ""), fontScl).row();

                if(effect != null && effect != StatusEffects.none){
                    groupT.image(effect.uiIcon).size(20);
                }
            }).pad(8).left().top();
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

    private int getDisplayWaves(){
        return state.wave - 1 + waveOffset;
    }

}
