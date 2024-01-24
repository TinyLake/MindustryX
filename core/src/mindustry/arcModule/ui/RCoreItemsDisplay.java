package mindustry.arcModule.ui;

import arc.*;
import arc.func.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class RCoreItemsDisplay extends CoreItemsDisplay{
    private final Interval timer = new Interval(), timer2 = new Interval();
    private final ObjectSet<Item> usedItems = new ObjectSet<>();
    private final ObjectSet<UnitType> usedUnits = new ObjectSet<>();
    private final int[] updateItems = new int[content.items().size];
    private final int[] lastItems = new int[content.items().size];
    private ItemSeq planItems = new ItemSeq();
    private CoreBuild core;
    private int arccoreitems = -1;
    private final ObjectIntMap<Block> planBlock = new ObjectIntMap<>();
    private final Boolf<Block> planBlockFilter = b -> b.category != Category.distribution
    && b.category != Category.liquid
    && !(b instanceof PowerNode)
    && !(b instanceof BeamNode);

    public RCoreItemsDisplay(){
        arccoreitems = Core.settings.getInt("arccoreitems");
        rebuild();
    }

    @Override
    public void resetUsed(){
        usedItems.clear();
        usedUnits.clear();
        background(null);
    }

    public void updateItems(){
        if(core == null) return;
        for(Item item : Vars.content.items()){
            if(lastItems != null) updateItems[item.id] = core.items.get(item) - lastItems[item.id];
            lastItems[item.id] = core.items.get(item);
        }
    }

    private ItemSeq updatePlanItems(){
        planItems = new ItemSeq();
        control.input.allPlans().each(plan -> {
            if(plan.block instanceof CoreBlock) return;
            for(ItemStack stack : plan.block.requirements){
                int planAmount = (int)(plan.breaking ? -1 * state.rules.buildCostMultiplier * state.rules.deconstructRefundMultiplier * stack.amount * plan.progress : state.rules.buildCostMultiplier * stack.amount * (1 - plan.progress));
                planItems.add(stack.item, planAmount);
            }
        });
        return planItems;
    }

    void rebuild(){
        clear();
        if(usedItems.size > 0 || usedUnits.size > 0){
            background(Styles.black3);
            margin(4);
        }

        update(() -> {
            core = Vars.player.team().core();

            if(timer.get(60f)) updateItems();
            updatePlanItems();

            if(arccoreitems != Core.settings.getInt("arccoreitems")){
                arccoreitems = Core.settings.getInt("arccoreitems");
                rebuild();
            }

            if(content.items().contains(item -> core != null && core.items.get(item) > 0 && usedItems.add(item))){
                rebuild();
            }

            if(content.items().contains(item -> core != null && core.items.get(item) > 0 && usedItems.add(item)) || content.units().contains(unit -> Vars.player.team().data().countType(unit) > 0 && usedUnits.add(unit))){
                rebuild();
            }

            if(timer2.get(2f)) rebuild();
            touchable = Touchable.disabled;
        });

        int i = 0;
        if(arccoreitems == 1 || arccoreitems == 3){
            for(Item item : content.items()){
                if(usedItems.contains(item)){
                    if(mobile){
                        stack(
                        new Table(t -> t.image(item.uiIcon).size(iconSmall).padRight(3).tooltip(tooltip -> tooltip.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel))),

                        new Table(t -> t.label(() -> {
                            int update = updateItems[item.id];
                            if(update == 0) return "";
                            return (update < 0 ? "[red]" : "[green]+") + update;
                        }).get().setFontScale(0.85f)).top().left()
                        );
                    }else{
                        stack(
                        new Table(t -> t.image(item.uiIcon).size(iconSmall).padRight(3).tooltip(tooltip -> tooltip.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel))),

                        new Table(t -> t.label(() -> {
                            int update = updateItems[item.id];
                            if(update == 0) return "";
                            return (update < 0 ? "[red]" : "[green]+") + update;
                        }).get().setFontScale(0.85f)).top().left()
                        );
                    }


                    label(() -> {
                        if(core == null) return "";
                        int planAmount = planItems.get(item);
                        int amount = core.items.get(item);
                        if(planAmount == 0)
                            return (amount >= player.team().core().storageCapacity * 0.99 ? "[stat]" : "") + UI.formatAmount(amount);
                        String planColor = (planAmount > 0 ? "[scarlet]" : "[green]");
                        String amountColor = (amount < planAmount / 2 ? "[scarlet]" : amount < planAmount ? "[stat]" : "[green]");
                        return amountColor + UI.formatAmount(amount) + "[white]/" + planColor + UI.formatAmount(Math.abs(planAmount));
                    }).padRight(3).minWidth(52f).left();

                    if(++i % Core.settings.getInt("arcCoreItemsCol") == 0){
                        row();
                    }
                }
            }
        }

        if(arccoreitems == 2 || arccoreitems == 3){
            row();
            i = 0;
            for(UnitType unit : content.units()){
                if(usedUnits.contains(unit)){
                    image(unit.uiIcon).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(unit.localizedName).style(Styles.outlineLabel));
                    label(() -> {
                        int typeCount = player.team().data().countType(unit);
                        return (typeCount == Units.getCap(player.team()) ? "[stat]" : "") + typeCount;
                    }).padRight(3).minWidth(52f).left();

                    if(++i % Core.settings.getInt("arcCoreItemsCol") == 0){
                        row();
                    }
                }
            }
        }

        i = 0;
        row();
        planBlock.clear();
        control.input.allPlans().each(p -> planBlock.put(p.block, planBlock.get(p.block, 0) + (p.breaking ? -1 : 1)));
        for(Block block : content.blocks().select(planBlockFilter)){
            int count = planBlock.get(block, 0);
            if(count != 0){
                image(block.uiIcon).size(iconMed).padRight(3);
                label(() -> (count > 0 ? "[green]+" : "[red]") + count).padRight(3).minWidth(52f).left();
                if(++i % Core.settings.getInt("arcCoreItemsCol") == 0){
                    row();
                }
            }
        }
    }

    public boolean hadItem(Item item){
        return usedItems.contains(item);
    }
}
