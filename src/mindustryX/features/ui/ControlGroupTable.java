package mindustryX.features.ui;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustryX.events.*;
import mindustryX.features.ui.OverlayUI.*;
import mindustryX.features.ui.comp.*;

import java.util.*;

public class ControlGroupTable extends Table{
    private static final Color hitColor = Color.white.cpy().lerp(Pal.remove, 0.5f);

    private ControlGroupModel[] models;

    public ControlGroupTable(){
        background(Styles.black3);

        Events.on(WorldLoadEvent.class, e -> {
            updateControlGroup();
        });

        Events.on(SaveLoadEvent.class, e -> {
            updateControlGroup();
        });

        Events.run(Trigger.unitCommandChange, () -> {
            if(models != null){
                for(ControlGroupModel model : models){
                    model.updateSelected(Vars.control.input.selectedUnits);
                }
            }
        });

        Events.on(HealthChangedEvent.class, e -> {
            if(models != null && e.amount > 0 && e.entity instanceof Unit unit && unit.team == Vars.player.team()){
                for(ControlGroupModel model : models){
                    if(model.units.contains(unit)){
                        model.hitUnit(unit.type);
                    }
                }
            }
        });

        update(() -> {
            if(models != null){
                for(ControlGroupModel model : models){
                    model.update();
                }
            }
        });
    }

    private void updateControlGroup(){
        IntSeq[] controlGroups = Vars.control.input.controlGroups;

        if(models == null){
            models = new ControlGroupModel[controlGroups.length];
            for(int i = 0; i < controlGroups.length; i++){
                models[i] = new ControlGroupModel();
            }
        }

        for(int i = 0; i < controlGroups.length; i++){
            // initialize is a must
            if(controlGroups[i] == null) controlGroups[i] = new IntSeq();
            models[i].setUnits(controlGroups[i]);
        }

        rebuild();
    }

    private void rebuild(){
        clearChildren();

        top();
        margin(8f);

        Table top = add(new GridTable()).growX().get();
        row();
        image().color(Pal.gray).growX().padBottom(4f);
        row();
        Table bottom = add(new GridTable()).growX().get();

        bottom.left();
        top.left();
        top.defaults().size(Vars.iconLarge);
        bottom.defaults().minWidth(320f).growX();

        int keyIndex = 1;
        for(ControlGroupModel model : models){
            int finalI = keyIndex++;

            top.button("" + finalI, Styles.cleart, () -> {
                for(Unit unit : Vars.control.input.selectedUnits){
                    model.appendUnit(unit);

                    // not append
                    for(ControlGroupModel otherModel : models){
                        if(otherModel != model) otherModel.removeUnit(unit);
                    }
                }
            }).get().getLabel().setStyle(Styles.outlineLabel);

            Table modelTable = bottom.table().grow().get();
            modelTable.visibility = () -> !model.isEmpty();

            modelTable.button(b -> {
                b.add("" + finalI).style(Styles.outlineLabel).labelAlign(Align.center).width(32f);
                Table unitTable = b.add(new GridTable()).padLeft(8f).padRight(16f).growX().get();
                setupUnitsTable(unitTable, model);
            }, Styles.clearNonei, () -> {
                setControlUnits(model.units.toSeq());

                if(Core.input.ctrl() && Vars.control.input instanceof DesktopInput desktopInput){
                    desktopInput.panning = true;
                    Core.camera.position.set(getCenter(model, Tmp.v1));
                }
            }).padTop(finalI != 1 ? 4f : 0f).grow();

            modelTable.table(buttons -> {
                buttons.right();
                buttons.defaults().growY().padLeft(8f);

                if(Vars.mobile){
                    buttons.button(Icon.eyeSmall, Styles.clearNonei, () -> {
                        Core.camera.position.set(getCenter(model, Tmp.v1));
                    });
                }

                buttons.button(Icon.cancelSmall, Styles.clearNonei, model::clear).size(Vars.iconMed);

                buttons.button(Icon.addSmall, Styles.clearNonei, () -> {
                    for(Unit unit : Vars.control.input.selectedUnits){
                        model.appendUnit(unit);
                    }
                }).size(Vars.iconMed);
            });
        }

        row();
        add(new PreferAnyWidth()).fillX();
    }

    private void setupUnitsTable(Table table, ControlGroupModel model){
        table.left();
        table.defaults().minSize(Vars.iconMed).padLeft(8f);

        for(UnitType type : Vars.content.units()){
            int amount = model.count(type);
            if(amount == 0) continue;

            Image image = new Image(type.uiIcon);
            Button btn = table.button(unitTable -> {
                unitTable.add(image).scaling(Scaling.fit).size(Vars.iconMed);

                unitTable.fill(t -> {
                    t.right().bottom();
                    t.label(() -> model.countSelect(type) + "/" + amount).style(Styles.outlineLabel).fontScale(0.75f);
                });
            }, Styles.clearNoneTogglei, () -> {})
            .checked(b -> model.countSelect(type) > 0).tooltip(type.localizedName).get();

            btn.addListener(new ClickListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    event.stop(); // as nested button
                    return super.touchDown(event, x, y, pointer, button);
                }

                @Override
                public void clicked(InputEvent event, float x, float y){
                    super.clicked(event, x, y);

                    if(getTapCount() >= 2){
                        model.removeType(type);
                    }else {
                        setControlUnits(model.units.toSeq().retainAll(u -> u.type == type));
                    }
                }
            });

            // hit feedback
            image.setColor(Color.white);
            image.update(() -> {
                if(model.isHit(type)){
                    image.clearActions();
                    image.actions(Actions.color(hitColor, 0.1f), Actions.color(Color.white, 0.3f));
                    model.consumeHit(type);
                }
            });
        }

        table.update(() -> {
            if(model.changed()){
                model.consumeChanged();
                model.updateSelected(Vars.control.input.selectedUnits);

                table.clearChildren();
                setupUnitsTable(table, model);
            }
        });
    }

    private static void setControlUnits(Seq<Unit> units){
        Vars.control.input.selectedUnits.set(units);
        Events.fire(Trigger.unitCommandChange);
    }

    private static Vec2 getCenter(ControlGroupModel model, Vec2 out){
        out.setZero();
        int size = model.units.size;
        if(size == 0) return out;
        for(Unit unit : model.units){
            out.add(unit.x / size, unit.y / size);
        }
        return out;
    }

    public static class ControlGroupModel{
        public final ObjectSet<Unit> units = new ObjectSet<>();
        private final ObjectIntMap<UnitType> counter = new ObjectIntMap<>();
        private final ObjectIntMap<UnitType> selectCounter = new ObjectIntMap<>();
        private final Bits hitMap = new Bits(Vars.content.units().size);

        private IntSeq groupID;

        private int lastGroupSize = -1;
        private boolean dirty;

        public void clear(){
            units.clear();
            counter.clear();
            selectCounter.clear();
            hitMap.clear();
            if(groupID != null) groupID.clear();

            dirty = false;
        }

        public boolean changed(){
            return dirty;
        }

        public void consumeChanged(){
            dirty = false;
        }

        public boolean isEmpty(){
            return units.isEmpty();
        }

        public void setUnits(IntSeq groupID){
            this.groupID = groupID;

            units.clear();
            counter.clear();
            lastGroupSize = groupID.size;
            dirty = true;

            for(int i = 0; i < groupID.size; i++){
                Unit unit = Groups.unit.getByID(groupID.items[i]);
                if(unit != null && units.add(unit)){
                    counter.increment(unit.type);
                }
            }
        }

        public void appendUnit(Unit unit){
            if(units.add(unit)){
                groupID.add(unit.id);
                counter.increment(unit.type);
                dirty = true;
            }
        }

        public void removeUnit(Unit unit){
            if(units.remove(unit)){
                groupID.removeValue(unit.id);
                counter.increment(unit.type, -1);
                dirty = true;
            }
        }

        public void removeType(UnitType type){
            Iterator<Unit> iterator = units.iterator();
            while(iterator.hasNext()){
                Unit unit = iterator.next();
                if(unit.type == type){
                    groupID.removeValue(unit.id);
                    iterator.remove();
                    dirty = true;
                }
            }

            counter.put(type, 0);
        }

        public int count(UnitType type){
            return counter.get(type);
        }

        public int countSelect(UnitType type){
            return selectCounter.get(type);
        }

        public boolean isHit(UnitType type){
            return hitMap.get(type.id);
        }

        public void hitUnit(UnitType type){
            hitMap.set(type.id);
        }

        public void consumeHit(UnitType type){
            hitMap.clear(type.id);
        }

        public void update(){
            // check size changed
            if(lastGroupSize != groupID.size){
                setUnits(groupID);
                return;
            }

            // check invalid
            Iterator<Unit> iterator = units.iterator();
            while(iterator.hasNext()){
                Unit unit = iterator.next();
                if(!unit.isCommandable() || !unit.isValid()){
                    iterator.remove();
                    groupID.removeValue(unit.id);
                    counter.increment(unit.type, -1);
                    dirty = true;
                }
            }
        }

        public void updateSelected(Seq<Unit> selected){
            selectCounter.clear();
            for(Unit unit : selected){
                if(units.contains(unit)){
                    selectCounter.increment(unit.type);
                }
            }
        }
    }
}
