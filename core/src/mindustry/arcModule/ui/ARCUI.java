package mindustry.arcModule.ui;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.arcModule.ui.dialogs.*;
import mindustry.ui.*;

import static mindustry.arcModule.toolpack.arcWaveSpawner.initArcWave;

public class ARCUI{
    public static Color attackMark = Color.valueOf("#DC143C");

    public AchievementsDialog achievements;
    //public MindustryWikiDialog mindustrywiki;
    public mindustry.arcModule.ui.dialogs.MessageDialog MessageDialog;
    public mindustry.arcModule.ui.dialogs.MusicDialog MusicDialog;


    /** Display text in the upper of the screen, then fade out. */
    public void arcInfo(String text, float duration){
        Table t = new Table(Styles.black3);
        t.touchable = Touchable.disabled;
        t.margin(8f).add(text).style(Styles.outlineLabel).labelAlign(Align.center);
        t.update(() -> t.setPosition(Core.graphics.getWidth() / 2f, Core.graphics.getHeight() / 4f, Align.center));
        t.actions(Actions.fadeOut(duration, Interp.pow4In), Actions.remove());
        t.pack();
        t.act(0.1f);
        Core.scene.add(t);
    }

    public void arcInfo(String text){
        arcInfo(text, 3);
    }

    public void init(){
        achievements = new AchievementsDialog();
        //mindustrywiki = new MindustryWikiDialog();
        MessageDialog = new MessageDialog();
        MusicDialog = new MusicDialog();

        initArcWave();
    }
}
