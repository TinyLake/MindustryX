package mindustryX.features.ui.toolTable;

import arc.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.ui.*;

public abstract class ToolTableBase extends Table{
    public String icon = "";
    public boolean expand = false;
    protected float maxHeight = 0;

    public ToolTableBase(String icon){
        this.icon = icon;
        setBackground(Styles.black6);
    }

    public Table wrapped(){
        return new Table(t -> {
            t.add().growX();
            Table main = this;
            if(maxHeight > 0){
                main = new Table();
                ScrollPane pane = main.pane(this).maxSize(800f, maxHeight).get();
                pane.update(() -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if(e != null && e.isDescendantOf(pane)){
                        pane.requestScroll();
                    }else if(pane.hasScroll()){
                        Core.scene.setScrollFocus(null);
                    }
                });
            }
            t.collapser(main, () -> expand);
            t.button(icon, Styles.flatBordert, () -> expand = !expand).width(40f).minHeight(40f).fillY()
            .update(i -> i.getLabel().setColor(expand ? Color.white : Color.lightGray));
        });
    }
}
