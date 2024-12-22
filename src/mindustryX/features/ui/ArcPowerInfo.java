package mindustryX.features.ui;

import arc.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustryX.features.Settings;

//move from mindustry.arcModule.ui.PowerInfo
public class ArcPowerInfo{
    public static float balance, stored, capacity, produced, need;

    public static void update(){
        balance = 0;
        stored = 0;
        capacity = 0;
        produced = 0;
        need = 0;
        Groups.powerGraph.each(item -> {
            var graph = item.graph();
            if(graph.all.isEmpty() || graph.all.first().team != Vars.player.team()) return;
            balance += graph.getPowerBalance();
            stored += graph.getLastPowerStored();
            capacity += graph.getLastCapacity();
            produced += graph.getLastPowerProduced();
            need += graph.getLastPowerNeeded();
        });
    }

    public static int getPowerBalance(){
        return (int)(balance * 60);
    }

    public static float getSatisfaction(){
        if(Mathf.zero(produced)){
            return 0f;
        }else if(Mathf.zero(need)){
            return 1f;
        }
        return produced / need;
    }


    public static Element getBars(){
        Table power = new Table(Tex.wavepane).marginTop(6);

        Bar powerBar = new Bar(
        () -> Core.bundle.format("bar.powerbalance", (getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount(getPowerBalance())) +
        (getSatisfaction() >= 1 ? "" : " [gray]" + (int)(getSatisfaction() * 100) + "%"),
        () -> Pal.powerBar, ArcPowerInfo::getSatisfaction);
        Bar batteryBar = new Bar(
        () -> Core.bundle.format("bar.powerstored", UI.formatAmount((long)stored), UI.formatAmount((long)capacity)),
        () -> Pal.powerBar,
        () -> stored / capacity);

        power.margin(0);
        power.add(powerBar).height(18).growX().padBottom(1);
        power.row();
        power.add(batteryBar).height(18).growX().padBottom(1);

        power.update(ArcPowerInfo::update);
        return power;
    }
}