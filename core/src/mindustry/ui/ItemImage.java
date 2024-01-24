package mindustry.ui;

import arc.func.Intp;
import arc.func.Prov;
import arc.graphics.g2d.*;
import arc.scene.Element;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.type.*;
import static mindustry.Vars.*;

public class ItemImage extends Stack{

    public ItemImage(TextureRegion region, int amount){

        add(new Table(o -> {
            o.left();
            o.add(new Image(region)).size(32f).scaling(Scaling.fit);
        }));

        if(amount != 0){
            add(new Table(t -> {
                t.left().bottom();
                t.add(amount >= 1000 ? UI.formatAmount(amount) : amount + "").style(Styles.outlineLabel);
                t.pack();
            }));
        }
    }

    public ItemImage(TextureRegion region, int reqAmount, Intp curAmount){
        add(new Table(o -> {
            o.left();
            o.add(new Image(region)).size(32f).scaling(Scaling.fit);
        }));

        add(new Table(t -> {
            t.left().bottom();
            t.add(String.valueOf(reqAmount)).get().setFontScale(1f);
            t.pack();
        }));
        add(new Table(t -> {
            t.left().top();
            t.label(() -> String.valueOf(curAmount.get())).get().setFontScale(0.6f);
            t.pack();
        }));
    }

    public ItemImage(TextureRegion region, String text){
        add(new Table(o -> {
            o.left();
            o.add(new Image(region)).size(26f).scaling(Scaling.fit);
        }));

        add(new Table(t -> {
            t.left().bottom();
            t.add(text).get().setFontScale(1f);
            t.pack();
        }));
    }

    public ItemImage(ItemStack stack){
        this(stack.item.uiIcon, stack.amount);
    }

    public ItemImage(PayloadStack stack){
        this(stack.item.uiIcon, stack.amount);
    }
}
