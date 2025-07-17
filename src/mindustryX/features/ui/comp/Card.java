package mindustryX.features.ui.comp;

import arc.func.*;
import arc.graphics.*;
import arc.scene.actions.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/**
 * @author minri2
 * Create by 2024/4/13
 */
public class Card extends Table{
    public static CardStyle
    grayOuterDark = new CardStyle(){{
        shadowStyle = CardShadowStyle.outer;
        shadowDark = Pal.darkerGray;
    }},
    accentOutBack = new CardStyle(){{
        shadowStyle = CardShadowStyle.outer;
        shadowDark = Pal.accentBack;
    }};

    public CardStyle style;

    public Card(CardStyle style, Cons<Table> contCons){
        this(Pal.gray, style, contCons);
    }

    public Card(Color cardColor, CardStyle style, Cons<Table> contCons){
        this.style = style;

        if(cardColor != null){
            background(Tex.whiteui);
            setColor(cardColor);
        }

        setup(contCons);
    }

    private void setup(Cons<Table> contCons){
        Color topLeft, rightBottom;

        if(style.shadowStyle == CardShadowStyle.inner){
            topLeft = style.shadowDark;
            rightBottom = style.shadowLight;
        }else if(style.shadowStyle == CardShadowStyle.outer){
            topLeft = style.shadowLight;
            rightBottom = style.shadowDark;
        }else{
            throw new RuntimeException("Card got an unknown shadowStyle:" + style.shadowStyle);
        }

        float size = style.shadowSize;
        Cell<?> topShadow = null, bottomShadow = null;
        if(topLeft != null){
            topShadow = image().color(topLeft).height(size).padRight(-1).growX();
            row();
            image().color(topLeft).width(size).growY();
        }

        table(contCons).pad(size).grow();

        if(rightBottom != null){
            image().color(rightBottom).width(size).growY();
            row();
            bottomShadow = image().color(rightBottom).height(size).padRight(-1).growX();
        }

        int columns = getColumns();
        if(topShadow != null) topShadow.colspan(columns);
        if(bottomShadow != null) bottomShadow.colspan(columns);
    }

    public void setCardColor(Color cardColor){
        setCardColor(cardColor, 1.5f);
    }

    public void setCardColor(Color cardColor, float duration){
        if(color.equals(cardColor)) return;

        addAction(Actions.color(cardColor, duration));
    }

    public enum CardShadowStyle{
        /**
         * 内阴影 有内陷的效果
         */
        inner,
        /**
         * 外阴影 有外凸的效果
         */
        outer
    }

    public static class CardStyle{
        public float shadowSize = 6f;
        public CardShadowStyle shadowStyle = CardShadowStyle.inner;
        public @Nullable Color shadowLight, shadowDark = Pal.darkestGray;
    }

    /**
     * 为单行表格添加阴影
     * 由于表格没有rowspan所以只能为单行表格添加
     * @param table 添加阴影的表格
     * @param size 阴影大小
     * @param color 阴影的颜色
     */
    public static void cardShadow(Table table, float size, Color color){
        table.image().width(size).color(color).growY().right();
        table.row();
        table.image().height(size).color(color).growX().colspan(table.getColumns());
    }

    public static void cardShadow(Table table){
        cardShadow(table, 8f, Pal.darkerGray);
    }
}
