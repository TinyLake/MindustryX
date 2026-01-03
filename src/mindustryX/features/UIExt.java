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
    public static ArcMessageDialog arcMessageDialog = new ArcMessageDialog();
    public static AdvanceBuildTool advanceBuildTool = new AdvanceBuildTool();
    public static NewCoreItemsDisplay coreItems = new NewCoreItemsDisplay();
    public static ControlGroupTable controlGroup = new ControlGroupTable();

    public static void init(){
        teamSelect = new TeamSelectDialog();

        UIExtKt.init();
        Settings.addSettings();
        AutoUpdate.INSTANCE.initUI();
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
