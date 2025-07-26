package mindustryX.features;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustryX.features.ui.*;
import mindustryX.features.ui.toolTable.*;

import static mindustry.Vars.*;

public class UIExt{
    public static TeamSelectDialog teamSelect;
    public static ModsRecommendDialog modsRecommend = new ModsRecommendDialog();
    public static TeamsStatDisplay teamsStatDisplay;
    public static ArcMessageDialog arcMessageDialog = new ArcMessageDialog();
    public static AdvanceToolTable advanceToolTable = new AdvanceToolTable();
    public static AdvanceBuildTool advanceBuildTool = new AdvanceBuildTool();
    public static AuxiliaryTools auxiliaryTools = new AuxiliaryTools();
    public static WaveInfoDisplay waveInfoDisplay = new WaveInfoDisplay();
    public static NewCoreItemsDisplay coreItems = new NewCoreItemsDisplay();

    public static void init(){
        teamSelect = new TeamSelectDialog();
        teamsStatDisplay = new TeamsStatDisplay();

        UIExtKt.init();
    }

    public static void buildPositionRow(Table tt, Vec2 vec){
        tt.add("x= ");
        TextField x = tt.field(Strings.autoFixed(vec.x, 2), text -> vec.x = Float.parseFloat(text)).valid(Strings::canParseFloat).maxTextLength(8).get();

        tt.add("y= ").marginLeft(32f);
        TextField y = tt.field(Strings.autoFixed(vec.y, 2), text -> vec.y = Float.parseFloat(text)).valid(Strings::canParseFloat).maxTextLength(8).get();

        tt.button(UnitTypes.gamma.emoji(), () -> {
            vec.set(player.tileX(), player.tileY());
            x.setText(String.valueOf(vec.x));
            y.setText(String.valueOf(vec.y));
        }).tooltip(b -> b.label(() -> Core.bundle.format("uiExt.selectPlayerPosition", player.tileX(), player.tileY()))).height(50f);

        tt.button(StatusEffects.blasted.emoji(), () -> {
            var last = MarkerType.getLastPos();
            if(last == null) return;
            vec.set(World.toTile(last.getX()), World.toTile(last.getY()));
            x.setText(String.valueOf(vec.x));
            y.setText(String.valueOf(vec.y));
        }).height(50f).tooltip((t) -> t.label(() -> {
            var last = MarkerType.getLastPos();
            if(last == null) return Core.bundle.get("uiExt.notMarked");
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
                int sp2 = message.lastIndexOf(']', i + add + 10);
                if(sp2 > sp && i + add - sp < 10) add = sp - i;

                sendChatMessage(message.substring(i, i + add));
                i += add;
            }
            sendChatMessage(message.substring(i));
            return;
        }
        Call.sendChatMessage(ui.chatfrag.mode.normalizedPrefix() + message);
    }

    public static void shareMessage(char icon, String message){
        sendChatMessage("<MDTX " + icon + ">" + message);
    }

    public static void openURI(String uri){
        if(!Core.app.openURI(uri)){
            ui.showErrorMessage("@linkfail");
            Core.app.setClipboardText(uri);
        }
    }

    public static void hitter(HitterCons cons){
        Element hitter;
        hitter = new Element(){
            @Override
            public void draw(){
                super.draw();

                Draw.color(Color.black, 0.25f);
                Fill.rect(x + width / 2, y + height / 2, width, height);
            }
        };
        hitter.setFillParent(true);
        hitter.update(hitter::toFront);
        hitter.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                super.clicked(event, x, y);

                if(cons.get(x, y)){
                    hitter.remove();
                }
            }
        });

        Core.scene.add(hitter);
    }

    public interface HitterCons{
        /**
         * @return whether hitter should be removed.
         */
        boolean get(float x, float y);
    }
}
