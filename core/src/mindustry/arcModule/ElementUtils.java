package mindustry.arcModule;

import arc.graphics.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.Tooltip.*;
import arc.scene.ui.layout.*;
import mindustry.ui.*;

import static mindustry.gen.Tex.*;
import static mindustry.ui.Styles.*;

public class ElementUtils{
    public static TextButton.TextButtonStyle
    textStyle = new TextButton.TextButtonStyle(){{
        down = flatOver;
        up = pane;
        over = flatDownBase;
        font = Fonts.def;
        fontColor = Color.white;
        disabledFontColor = Color.gray;
        checked = flatDown;
    }},
    NCtextStyle = new TextButton.TextButtonStyle(){{
        down = flatOver;
        up = pane;
        over = flatDownBase;
        font = Fonts.def;
        fontColor = Color.white;
        disabledFontColor = Color.gray;
    }};

    public static <T extends Element> T tooltip(T element, String text){
        return tooltip(element, text, true);
    }

    public static <T extends Element> T tooltip(T element, String text, boolean allowMobile){
        Tooltip tooltip = Tooltips.getInstance().create(text);
        tooltip.allowMobile = allowMobile;

        element.addListener(tooltip);
        return element;
    }

    public static abstract class ToolTable extends Table{
        public String icon = "";
        public boolean expand = false;


        public void rebuild(){
            clear();
            table().growX().left();
            if(expand){
                buildTable();
            }
            button((expand ? "" : "[lightgray]") + icon, textStyle, () -> {
                expand = !expand;
                rebuild();
            }).right().width(40f).minHeight(40f).fillY();
        }

        protected abstract void buildTable();
    }
}
