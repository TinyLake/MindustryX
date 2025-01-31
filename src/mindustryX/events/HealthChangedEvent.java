package mindustryX.events;

import arc.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustryX.features.*;

/**
 * @author minri2
 * Create by 2025/1/31
 */
public class HealthChangedEvent{
    public static final HealthChangedEvent healthChangedEvent = new HealthChangedEvent();
    private static boolean autoReset = true;

    public Healthc entity;
    public @Nullable Sized source;
    public DamageType type;
    public float amount;

    private HealthChangedEvent(){
    }

    public HealthChangedEvent setSource(Sized source){
        this.source = source;
        return this;
    }

    public HealthChangedEvent setType(DamageType type){
        this.type = type;
        return this;
    }

    public HealthChangedEvent startWrap(){
        autoReset = false;
        return this;
    }

    public HealthChangedEvent endWrap(){
        autoReset = true;
        reset();
        return this;
    }

    public HealthChangedEvent fire(Healthc entity, float amount){
        if(type == null){ // default normal
            type = DamageType.normal;
        }

        this.entity = entity;
        this.amount = amount;
        Events.fire(this);

        if(autoReset){
            reset();
        }
        return this;
    }

    public HealthChangedEvent reset(){
        setSource(null);
        setType(DamageType.normal);
        return this;
    }
}
