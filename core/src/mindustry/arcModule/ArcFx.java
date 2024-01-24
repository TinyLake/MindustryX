package mindustry.arcModule;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.arcModule.ui.*;
import mindustry.entities.*;
import mindustry.graphics.*;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.stroke;
import static mindustry.Vars.tilesize;

public class ArcFx{
    public static final Effect
    marker = new Effect(1800, e -> {
        color(Pal.command);
        stroke(2f);
        Lines.circle(e.x, e.y, 1f * tilesize);
        stroke(e.fout() * 1.5f + 0.5f);
        Lines.circle(e.x, e.y, 8f + e.finpow() * 92f);
        //Drawf.arrow(player.x, player.y, e.x, e.y, 5f * tilesize, 4f, Pal.command);
    }),

    markerGather = new Effect(1800, e -> {
        color(Color.cyan, 0.8f);
        stroke(2f);
        Lines.circle(e.x, e.y, 4f * tilesize);
        stroke(1f);
        Lines.circle(e.x, e.y, 4f * tilesize * (e.finpow() * 8f - (int)(e.finpow() * 8f)));
        for(int j = 0; j < 4; j++){
            if(e.fout() * 3 < j){
                for(int i = 0; i < 8; i++){
                    float rot = i * 45f;
                    float radius = 4f * tilesize + j * 6f + 4f;
                    Drawf.simpleArrow(e.x + Angles.trnsx(rot, radius), e.y + Angles.trnsy(rot, radius), e.x, e.y, 4f, 2f, Color.cyan, Math.min(1f, j - e.fout() * 3));
                }
            }
        }
    }),

    markerAttack = new Effect(1800, e -> {
        color(ARCUI.attackMark);
        stroke(2f);
        Lines.circle(e.x, e.y, 1f * tilesize);
        float radius = 20f + e.finpow() * 80f;
        Lines.circle(e.x, e.y, radius);
        for(int i = 0; i < 4; i++){
            float rot = i * 90f + 45f + (-Time.time) % 360f;
            Drawf.simpleArrow(e.x + Angles.trnsx(rot, radius), e.y + Angles.trnsy(rot, radius), e.x, e.y, 6f + 4 * e.finpow(), 2f + 4 * e.finpow());
        }
    }),

    markerDefense = new Effect(1800, e -> {
        color(Pal.heal);
        if(e.fin() < 0.2f){
            Lines.circle(e.x, e.y, 20f + e.fin() * 400f);
            return;
        }
        Lines.circle(e.x, e.y, 101f);
        Lines.circle(e.x, e.y, 93f);
        for(int i = 0; i < 16; i++){
            float rot = i * 22.5f;
            if((e.fin() - 0.2f) * 50 > i)
                Drawf.simpleArrow(e.x, e.y, e.x + Angles.trnsx(rot, 120f), e.y + Angles.trnsy(rot, 120f), 96f, 4f);
        }
    }),

    markerQuestion = new Effect(1200, e -> {
        color(Color.violet);
        stroke(2f);
        Draw.alpha(Math.min(e.fin() * 5, 1));
        Lines.arc(e.x, e.y + 25f, 10, 0.75f, 270);
        Lines.line(e.x, e.y + 15f, e.x, e.y + 7f);
        Lines.circle(e.x, e.y, 3f);
        Lines.circle(e.x, e.y + 18.5f, 27f);
    }),

    arcIndexer = new Effect(120f, e -> {
        color(ARCVars.getThemeColor());
        Lines.circle(e.x, e.y, 8f);
        for(int i = 0; i < 3; i++){
            float rot = i * 120f + 90f;
            Drawf.simpleArrow(e.x, e.y, e.x + Angles.trnsx(rot, 120f), e.y + Angles.trnsy(rot, 120f), 100f - 80f * e.fin(), -4f, ARCVars.getThemeColor());
        }
    });
}
