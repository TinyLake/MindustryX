package mindustryX.features.ui.toolTable;

import arc.graphics.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.TextButton.*;

import static mindustry.gen.Tex.underlineWhite;
import static mindustry.ui.Styles.*;

//move from mindustry.arcModule.ui.RStyles
public class RStyles{
    public static TextButtonStyle clearLineNoneTogglet;

    public static ImageButtonStyle
    clearAccentNonei,
    clearAccentNoneTogglei,
    clearLineNonei,
    clearLineNoneTogglei;

    public static void load(){
        clearLineNoneTogglet = new TextButtonStyle(fullTogglet){{
            up = none;
            over = accentDrawable;
            down = underlineWhite;
            checked = underlineWhite;
            disabledFontColor = Color.white;
        }};

        clearAccentNonei = new ImageButtonStyle(clearNonei){{
            up = none;
            over = black3;
            down = none;
        }};

        clearAccentNoneTogglei = new ImageButtonStyle(clearAccentNonei){{
            checked = accentDrawable;
        }};

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
