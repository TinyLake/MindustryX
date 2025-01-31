package mindustryX.features;

import arc.graphics.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.graphics.*;

public class DamageType{

    public static DamageType
    normal = new DamageType(Pal.health),
    heal = new DamageType(Pal.heal),
    splash = new DamageType(StatusEffects.blasted.emoji(), StatusEffects.blasted.color);

    public final Color color;
    public @Nullable String icon;

    private DamageType(Color color){
        this(null, color);
    }

    private DamageType(String icon, Color color){
        this.icon = icon;
        this.color = color.cpy();
    }
}
