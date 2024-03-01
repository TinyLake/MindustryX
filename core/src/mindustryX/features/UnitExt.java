package mindustryX.features;

import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

public class UnitExt{
    public static Seq<StatusEntry> getStatuses(Unit unit){
        Class<?> cls = unit.getClass();
        if(cls.isAnonymousClass()) cls = cls.getSuperclass();
        while(true){
            try{
                return Reflect.get(cls, unit, "statuses");
            }catch(Exception e){
                cls = cls.getSuperclass();
                if(cls == Unit.class) return Seq.with();
            }
        }
    }
}
