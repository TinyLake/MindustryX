package mindustryX.features;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.editor.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

public class UIExt{
    public static TeamSelectDialog teamSelect;
    public static ModsRecommendDialog modsRecommend = new ModsRecommendDialog();
    public static TeamsStatDisplay teamsStatDisplay;
    public static ArcMessageDialog arcMessageDialog = new ArcMessageDialog();
    public static HudSettingsTable hudSettingsTable = new HudSettingsTable();
    public static AdvanceToolTable advanceToolTable = new AdvanceToolTable();
    public static AdvanceBuildTool advanceBuildTool = new AdvanceBuildTool();
    public static AuxiliaryTools auxiliaryTools = new AuxiliaryTools();
    public static WaveInfoDisplay waveInfoDisplay = new WaveInfoDisplay();
    public static NewCoreItemsDisplay coreItems = new NewCoreItemsDisplay();

    public static void init(){
        teamSelect = new TeamSelectDialog();

        teamsStatDisplay = new TeamsStatDisplay();
        ui.hudGroup.fill(t -> {
            t.name = "otherCore";
            t.left().add(teamsStatDisplay);
            t.visible(() -> ui.hudfrag.shown && Core.settings.getBool("showOtherTeamResource"));
        });

        ui.hudGroup.fill(t -> {
            t.right().name = "quickTool";
            t.update(() -> t.y = Core.settings.getInt("quickToolOffset"));
            t.add(hudSettingsTable).growX().row();
            t.add(advanceToolTable).growX().row();
            t.add(advanceBuildTool).growX().row();
            t.visible(() -> ui.hudfrag.shown && Core.settings.getBool("showQuickToolTable"));
        });
    }

    public static void buildPositionRow(Table tt, Vec2 vec){
        tt.add("x= ");
        TextField x = tt.field(Strings.autoFixed(vec.x, 2), text -> {
            vec.x = Float.parseFloat(text);
        }).valid(Strings::canParseFloat).maxTextLength(8).get();

        tt.add("y= ").marginLeft(32f);
        TextField y = tt.field(Strings.autoFixed(vec.y, 2), text -> {
            vec.y = Float.parseFloat(text);
        }).valid(Strings::canParseFloat).maxTextLength(8).get();

        tt.button(UnitTypes.gamma.emoji(), () -> {
            vec.set(player.tileX(), player.tileY());
            x.setText(String.valueOf(vec.x));
            y.setText(String.valueOf(vec.y));
        }).tooltip(b -> b.label(() -> "选择玩家当前位置：" + player.tileX() + "," + player.tileY())).height(50f);

        tt.button(StatusEffects.blasted.emoji(), () -> {
            var last = MarkerType.getLastPos();
            if(last == null) return;
            vec.set(World.toTile(last.getX()), World.toTile(last.getY()));
            x.setText(String.valueOf(vec.x));
            y.setText(String.valueOf(vec.y));
        }).height(50f).tooltip((t) -> t.label(() -> {
            var last = MarkerType.getLastPos();
            if(last == null) return "[red]未标记";
            return "选择上个标记点：" + FormatDefault.formatTile(last);
        }));
    }

    public static void announce(String text){
        announce(text, 3);
    }

    public static void announce(String text, float duration){
        //Copy from UI.announce, no set lastAnnouncement and add offset to y
        Table t = new Table(Styles.black3);
        t.touchable = Touchable.disabled;
        t.margin(8f).add(text).style(Styles.outlineLabel).labelAlign(Align.center);
        t.update(() -> t.setPosition(Core.graphics.getWidth() / 2f, Core.graphics.getHeight() / 2f + 30f, Align.center));
        t.actions(Actions.fadeOut(Math.min(duration, 30f), Interp.pow4In), Actions.remove());
        t.pack();
        t.act(0.1f);
        Core.scene.add(t);
    }

    public static void sendChatMessage(String message){
        int maxSize = 140;
        if(message.length() > maxSize){
            int i = 0;
            while(i < message.length() - maxSize){
                int add = maxSize;
                //避免分割颜色
                int sp = message.lastIndexOf('[', i + add);
                int sp2 = message.lastIndexOf(']', i + add);
                if(sp2 > sp && i + add - sp < 10) add = sp - i;

                sendChatMessage(message.substring(i, i + add));
                i += add;
            }
            sendChatMessage(message.substring(i));
            return;
        }
        Call.sendChatMessage(ui.chatfrag.mode.normalizedPrefix() + message);
    }
}
