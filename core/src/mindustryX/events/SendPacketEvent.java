package mindustryX.events;

import arc.*;
import arc.util.*;
import mindustry.net.*;

public class SendPacketEvent{
    /** null for all, may emit again */
    @Nullable
    public NetConnection con;
    /** only when call in sendExcept */
    @Nullable
    public NetConnection except;
    public Object packet;
    public boolean isCancelled;

    private SendPacketEvent(){
    }

    private static final SendPacketEvent inst = new SendPacketEvent();

    /** @return isCancelled */
    public static boolean emit(@Nullable NetConnection con, @Nullable NetConnection except, Object packet){
        inst.isCancelled = false;
        inst.con = con;
        inst.except = except;
        inst.packet = packet;
        Events.fire(inst);
        return inst.isCancelled;
    }
}
