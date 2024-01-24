package mindustry.arcModule;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.*;

public class DrawUtilities{
    private static final Vec2 vector = new Vec2();

    public static float drawText(String text, float scl, float dx, float dy, int align){
        return drawText(text, scl, dx, dy, Color.white, align);
    }

    public static float drawText(String text, float scl, float dx, float dy, Color color, int align){
        return drawText(Fonts.outline, text, scl, dx, dy, color, align);
    }

    /**
     * 绘制文本
     * <p>
     * 文本框在'给定方位'对齐'给定坐标'
     * <p>
     * 注意：当对齐方位没有指定横向方位时，默认居中对齐
     * <p>
     * 例如：{@code drawText(Fonts.outline, "Test", 1f, 3f, 3f, Color.white, Align.bottom)}
     * "Test"文本框的 '正下方' 将对齐坐标(3, 3)
     * @param align 文本对齐方位
     * @return 返回绘制文本的高度
     */
    public static float drawText(Font font, String text, float scl, float x, float y, Color color, int align){
        if(!Align.isLeft(align) && !Align.isRight(align)){
            align |= Align.center;
        }
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(scl);
        layout.setText(font, text);

        float height = layout.height;

        /* draw 的原点在文本左上角 */
        if(Align.isBottom(align)){
            y += height;
        }else if((align & Align.center) == 0){
            y += height / 2f;
        }

        font.setColor(color);
        font.draw(text, x, y, align);

        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);

        return height;
    }

    public static void arcDashCircling(float x, float y, float radius, float speed){
        arcDashCircle(x, y, radius, Time.time * speed);
    }

    public static void arcDashCircle(float x, float y, float radius, float rotation){
        float scaleFactor = 0.6f;
        int sides = 10 + (int)(radius * scaleFactor);
        if(sides % 2 == 1) sides++;

        vector.set(0, 0);

        for(int i = 0; i < sides; i += 2){
            vector.set(radius, 0).rotate(360f / sides * i + 90 + rotation);
            float x1 = vector.x;
            float y1 = vector.y;

            vector.set(radius, 0).rotate(360f / sides * (i + 1) + 90 + rotation);

            line(x1 + x, y1 + y, vector.x + x, vector.y + y);
        }
    }

    public static void drawNSideRegion(float x, float y, int n, float range, float rotation, Color color, float fraction, TextureRegion region, boolean regionColor){
        Draw.z(Layer.effect - 2f);
        color(color);

        stroke(2f);

        for(int i = 0; i < n; i++){
            float frac = 360f * (1 - fraction * n) / n / 2;
            float rot = rotation + i * 360f / n + frac;
            if(!regionColor){
                color(color);
                arc(x, y, range, 0.25f, rot, (int)(50 + range / 10));
                color();
            }else{
                arc(x, y, range, 0.25f, rot, (int)(50 + range / 10));
            }
            Draw.rect(region, x + range * Mathf.cos((float)Math.toRadians(rot - frac)), y + range * Mathf.sin((float)Math.toRadians(rot - frac)), 12f, 12f);
        }
        Draw.reset();
    }
}
