package mindustryX.features.ui;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import kotlin.collections.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.storage.*;
import mindustryX.features.*;
import mindustryX.features.SettingsV2.*;

import java.util.List;
import java.util.*;

import static mindustry.Vars.*;

//moved from mindustry.arcModule.ui.RCoreItemsDisplay
public class NewCoreItemsDisplay extends Table{
    public static final float MIN_WIDTH = 64f;

    private Table itemsTable, unitsTable, plansTable;

    private static final Interval timer = new Interval(2);

    private final int[] itemDelta;
    private final int[] lastItemAmount;
    private final ObjectSet<Item> usedItems = new ObjectSet<>();
    private final ObjectSet<UnitType> usedUnits = new ObjectSet<>();

    private final ItemSeq planItems = new ItemSeq();
    private final ObjectIntMap<Block> planCounter = new ObjectIntMap<>();

    private final SettingsV2.Data<Integer> columns = new SettingsV2.SliderPref(4, 15).create("coreItems.columns", 5);
    private final SettingsV2.Data<Boolean> showItem = CheckPref.INSTANCE.create("coreItems.showItem", true);
    private final SettingsV2.Data<Boolean> showUnit = CheckPref.INSTANCE.create("coreItems.showUnit", true);
    private final SettingsV2.Data<Boolean> showPlan = CheckPref.INSTANCE.create("coreItems.showPlan", true);
    final List<Data<?>> settings = CollectionsKt.listOf(columns, showItem, showUnit, showPlan);

    public NewCoreItemsDisplay(){
        itemDelta = new int[content.items().size];
        lastItemAmount = new int[content.items().size];
        Events.on(ResetEvent.class, e -> {
            usedItems.clear();
            usedUnits.clear();
            Arrays.fill(itemDelta, 0);
            Arrays.fill(lastItemAmount, 0);
            itemsTable.clearChildren();
            unitsTable.clearChildren();
            plansTable.clearChildren();
        });

        setup();
    }

    private void setup(){
        collapser(itemsTable = new Table(Styles.black3), showItem::getValue).growX().row();
        collapser(unitsTable = new Table(Styles.black3), showUnit::getValue).growX().row();

        var emptyLine = row().add();
        row().collapser(plansTable = new Table(Styles.black3), showPlan::getValue).growX().row();

        update(() -> {
            var newHeight = plansTable.hasChildren() ? 12f : 0f;
            if(emptyLine.maxHeight() != newHeight){
                emptyLine.height(newHeight);
                emptyLine.getTable().invalidate();
            }

            if(this.columns.changed()){
                rebuildItems();
                rebuildUnits();
                rebuildPlans();
            }
        });

        itemsTable.update(() -> {
            updateItemMeans();
            if(content.items().contains(item -> player.team().items().get(item) > 0 && usedItems.add(item))){
                rebuildItems();
            }
        });
        unitsTable.update(() -> {
            if(content.units().contains(unit -> player.team().data().countType(unit) > 0 && usedUnits.add(unit))){
                rebuildUnits();
            }
        });
        plansTable.update(() -> {
            if(timer.get(1, 10f)){
                rebuildPlans();
            }
        });
    }

    private void updateItemMeans(){
        if(!timer.get(0, 60f)) return;
        var items = player.team().items();
        for(Item item : usedItems){
            short id = item.id;
            int coreAmount = items.get(id);
            int lastAmount = lastItemAmount[id];
            itemDelta[id] = coreAmount - lastAmount;
            lastItemAmount[id] = coreAmount;
        }
    }

    private void rebuildItems(){
        itemsTable.clearChildren();
        if(player.team().core() == null) return;

        int i = 0;
        for(Item item : content.items()){
            if(!usedItems.contains(item)){
                continue;
            }

            itemsTable.stack(
            new Table(t ->
            t.image(item.uiIcon).size(iconMed - 4).scaling(Scaling.fit).pad(2f)
            .tooltip(tooltip -> tooltip.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel))
            ),
            new Table(t -> t.label(() -> {
                int update = itemDelta[item.id];
                if(update == 0) return "";
                return (update < 0 ? "[red]" : "[green]+") + UI.formatAmount(update);
            }).fontScale(0.85f)).top().left()
            );

            itemsTable.table(amountTable -> {
                amountTable.defaults().expand().left();

                Label amountLabel = amountTable.add("").growY().get();
                amountTable.row();
                var planLabel = amountTable.add("").fontScale(0.6f).height(0.01f);

                amountTable.update(() -> {
                    int planAmount = planItems.get(item);
                    int amount = player.team().items().get(item);

                    float newFontScale = 1f;
                    Color amountColor = Color.white;
                    if(planAmount == 0){
                        var core = player.team().core();
                        if(core != null && amount >= core.storageCapacity * 0.99){
                            amountColor = Pal.accent;
                        }
                        planLabel.height(0.01f);//can't use 0 as maxHeight;
                        planLabel.get().setText("");
                    }else{
                        amountColor = (amount > planAmount ? Color.green
                        : amount > planAmount / 2 ? Pal.stat
                        : Color.scarlet);
                        planLabel.height(Float.NEGATIVE_INFINITY);
                        planLabel.color(planAmount > 0 ? Color.scarlet : Color.green);
                        planLabel.get().setText(UI.formatAmount(planAmount));
                        newFontScale = 0.7f;
                    }

                    if(amountLabel.getFontScaleX() != newFontScale)
                        amountLabel.setFontScale(newFontScale);
                    amountLabel.setColor(amountColor);
                    amountLabel.setText(UI.formatAmount(amount));
                });
            }).minWidth(MIN_WIDTH).left();

            if(++i % columns.getValue() == 0){
                itemsTable.row();
            }
        }
    }

    private void rebuildUnits(){
        unitsTable.clearChildren();

        int i = 0;
        for(UnitType unit : content.units()){
            if(usedUnits.contains(unit)){
                unitsTable.image(unit.uiIcon).size(iconSmall).scaling(Scaling.fit).pad(2f)
                .tooltip(t -> t.background(Styles.black6).margin(4f).add(unit.localizedName).style(Styles.outlineLabel));
                unitsTable.label(() -> {
                    int typeCount = player.team().data().countType(unit);
                    return (typeCount == Units.getCap(player.team()) ? "[stat]" : "") + typeCount;
                }).minWidth(MIN_WIDTH).left();

                if(++i % columns.getValue() == 0){
                    unitsTable.row();
                }
            }
        }
    }

    private void rebuildPlans(){
        planItems.clear();
        planCounter.clear();

        control.input.allPlans().each(plan -> {
            Block block = plan.block;

            if(block instanceof CoreBlock) return;

            if(plan.build() instanceof ConstructBuild build){
                block = build.current;
            }

            planCounter.increment(block, plan.breaking ? -1 : 1);

            for(ItemStack stack : block.requirements){
                int planAmount = (int)(plan.breaking ? -state.rules.buildCostMultiplier * state.rules.deconstructRefundMultiplier * stack.amount * plan.progress
                : state.rules.buildCostMultiplier * stack.amount * (1 - plan.progress));
                planItems.add(stack.item, planAmount);
            }
        });

        plansTable.clearChildren();
        if(planCounter.isEmpty()) return;
        int i = 0;
        for(Block block : content.blocks()){
            int count = planCounter.get(block, 0);
            if(count == 0 || block.category == Category.distribution && block.size < 3
            || block.category == Category.liquid && block.size < 3
            || block instanceof PowerNode
            || block instanceof BeamNode) continue;

            plansTable.image(block.uiIcon).size(iconSmall).scaling(Scaling.fit).pad(2f);
            plansTable.label(() -> (count > 0 ? "[green]+" : "[red]") + count).minWidth(MIN_WIDTH).left();

            if(++i % columns.getValue() == 0){
                plansTable.row();
            }
        }
    }

    public boolean hadItem(Item item){
        return usedItems.contains(item);
    }
}
