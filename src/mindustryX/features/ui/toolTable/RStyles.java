package mindustryX.features.ui.toolTable;

import arc.scene.ui.ImageButton.*;

import static mindustry.gen.Tex.underlineWhite;
import static mindustry.ui.Styles.*;

//move from mindustry.arcModule.ui.RStyles
public class RStyles{
    public static ImageButtonStyle
    clearLineNonei,
    clearLineNoneTogglei;

    public static void load(){
        clearLineNonei = new ImageButtonStyle(clearNonei){{
            up = none;
            over = accentDrawable;
            down = none;
        }};

        clearLineNoneTogglei = new ImageButtonStyle(clearLineNonei){{
            checked = underlineWhite;
        }};
    }

}
