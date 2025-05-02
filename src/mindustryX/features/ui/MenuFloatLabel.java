package mindustryX.features.ui;

import arc.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;

public class MenuFloatLabel extends WidgetGroup{
    private static final float period = 75f, varSize = 0.8f;
    private final Label textLabel;
    private final String[] labels;
    public float baseScale = 1f;
    private long lastVisible;

    public MenuFloatLabel(){
        super();
        setTransform(true);
        setRotation(20);
        addChild(textLabel = new Label(""));
        visible(() -> Core.settings.getBool("menuFloatText", true));
        textLabel.setAlignment(Align.center);
        update(() -> {
            textLabel.setFontScale(baseScale * Math.abs(Time.time % period / period - 0.5f) * varSize + 1);
            if(Core.graphics.getFrameId() - lastVisible > 1){
                randomLabel();
            }
            lastVisible = Core.graphics.getFrameId();
        });
        labels = Core.files.internal("labels").readString("UTF-8").replace("\r", "").replace("\\n", "\n").replace("/n", "\n").split("\n");
        randomLabel();
    }

    public void randomLabel(){
        Timer.schedule(() -> textLabel.setText("[yellow]" + labels[new Rand().random(0, labels.length - 1)]), 0.11f);
    }
}
