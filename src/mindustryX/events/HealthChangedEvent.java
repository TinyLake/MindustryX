package mindustryX.events;

import arc.*;
import arc.graphics.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustryX.*;

/**
 * @author minri2
 * Create by 2025/1/31
 */
@MindustryXApi
public class HealthChangedEvent{
    private static final HealthChangedEvent healthChangedEvent = new HealthChangedEvent();
    private static boolean autoReset = true;

    public Healthc entity;
    public @Nullable Sized source;
    public DamageType type;
    public float amount;

    private HealthChangedEvent(){
    }

    public static void setSource(Sized source){
        healthChangedEvent.source = source;
    }

    public static void setType(DamageType type){
        healthChangedEvent.type = type;
    }

    public static void startWrap(){
        autoReset = false;
    }

    public static void endWrap(){
        autoReset = true;
        healthChangedEvent.reset();
    }

    public static void fire(Healthc entity, float amount){
        if(healthChangedEvent.type == null){ // default normal
            healthChangedEvent.type = DamageType.normal;
        }

        healthChangedEvent.entity = entity;
        healthChangedEvent.amount = amount;
        Events.fire(healthChangedEvent);

        if(autoReset){
            reset();
        }
    }

    public static void reset(){
        setSource(null);
        setType(DamageType.normal);
    }

    public static class DamageType{
        public static DamageType
        normal = new DamageType(null, Pal.health),
        heal = new DamageType(null, Pal.heal),
        splash = new DamageType(StatusEffects.blasted.emoji(), StatusEffects.blasted.color);

        public final Color color;
        public @Nullable String icon;

        private DamageType(String icon, Color color){
            this.icon = icon;
            this.color = color.cpy();
        }
    }
}
