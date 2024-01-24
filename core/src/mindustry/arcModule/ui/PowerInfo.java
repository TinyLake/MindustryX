package mindustry.arcModule.ui;

import arc.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class PowerInfo{

    private static final PowerInfo info = new PowerInfo();
    public float powerbal;
    public float stored;
    public float capacity;
    public float produced;
    public float need;

    public void add(float powerbal, float stored, float cap, float produced, float need){
        this.powerbal += powerbal;
        this.stored += stored;
        this.capacity += cap;
        this.produced += produced;
        this.need += need;
    }

    private void clear(){
        this.powerbal = 0;
        this.stored = 0;
        this.capacity = 0;
        this.produced = 0;
        this.need = 0;
    }

    public int getPowerBalance(){
        return (int)(powerbal * 60);
    }

    public float getStored(){
        return stored;
    }

    public float getCapacity(){
        return capacity;
    }

    public float getSatisfaction(){
        if(Mathf.zero(produced)){
            return 0f;
        }else if(Mathf.zero(need)){
            return 1f;
        }
        return produced / need;
    }


    public static void update(){
        info.clear();
        Groups.powerGraph.each(item -> {
            if(item.graph().team == Vars.player.team())
                info.add(item.graph().getPowerBalance(), item.graph().getLastPowerStored(), item.graph().getLastCapacity(), item.graph().getLastPowerProduced(), item.graph().getLastPowerNeeded());
        });
    }

    public static Element getBars(){
        Table power = new Table(Tex.wavepane).marginTop(6);

        Bar powerBar = new Bar(
        () -> Core.bundle.format("bar.powerbalance", (info.getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount(info.getPowerBalance())) +
        (info.getSatisfaction() >= 1 ? "" : " [gray]" + (int)(info.getSatisfaction() * 100) + "%"),
        () -> Pal.powerBar,
        info::getSatisfaction);
        Bar batteryBar = new Bar(
        () -> Core.bundle.format("bar.powerstored", UI.formatAmount((long)info.getStored()), UI.formatAmount((long)info.getCapacity())),
        () -> Pal.powerBar,
        () -> info.getStored() / info.getCapacity());

        power.clicked(() -> {
            int arccoreitems = Core.settings.getInt("arccoreitems");
            Core.settings.put("arccoreitems", (arccoreitems + 1) % 4);
        });
        power.margin(0);
        power.add(powerBar).height(18).growX().padBottom(1);
        power.row();
        power.add(batteryBar).height(18).growX().padBottom(1);

        power.update(PowerInfo::update);
        return power;
    }
}