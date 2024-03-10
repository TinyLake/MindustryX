package mindustryX.features.graphics;

import arc.Core;
import arc.Events;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Dialog;
import mindustry.game.EventType;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import static mindustry.gen.Tex.windowEmpty;
import static mindustryX.features.graphics.NewUI.BLUR_BACK;

public class NewUI {

    public static Dialog.DialogStyle blurBack;

    public static Blur uiBlur = new Blur(Blur.DEf_B);

    public static Drawable BLUR_BACK;
    public static void init(){

        ScreenSampler.setup();

        BLUR_BACK = new TextureRegionDrawable(Core.atlas.white()) {
            @Override
            public void draw(float x, float y, float width, float height) {
                uiBlur.directDraw(() -> super.draw(x, y, width, height));

                Styles.black3.draw(x, y, width, height);
            }

            @Override
            public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
                uiBlur.directDraw(() -> super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation));

                Styles.black3.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
            }
        };

        blurBack = new Dialog.DialogStyle(){{
            stageBackground = BLUR_BACK;
            titleFont = Fonts.def;
            background = windowEmpty;
            titleFontColor = Pal.accent;
        }};

        UpdatePool.receive("syncUIBlurCfg", () -> {
            uiBlur.blurScl = Core.settings.getInt("blurLevel", 1);
            uiBlur.blurSpace = Core.settings.getInt("backBlurLen", 120) * 0.01f;

            Styles.defaultDialog.stageBackground = Core.settings.getBool("enableBlur", true)? BLUR_BACK: Styles.black9;
        });
    }
}