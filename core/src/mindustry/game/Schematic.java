package mindustry.game;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.mod.Mods.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.Fracker;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.Separator;
import mindustry.world.blocks.storage.*;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.consumers.ConsumeLiquids;

import java.util.concurrent.atomic.AtomicBoolean;

import static mindustry.Vars.*;

public class Schematic implements Publishable, Comparable<Schematic>{
    public final Seq<Stile> tiles;
    /** These are used for the schematic tag UI. */
    public Seq<String> labels = new Seq<>();
    /** Internal meta tags. */
    public StringMap tags;
    public int width, height;
    public @Nullable Fi file;
    /** Associated mod. If null, no mod is associated with this schematic. */
    public @Nullable LoadedMod mod;

    public ObjectFloatMap<Item> items;
    public ObjectFloatMap<Liquid> liquids;

    public Schematic(Seq<Stile> tiles, StringMap tags, int width, int height){
        this.tiles = tiles;
        this.tags = tags;
        this.width = width;
        this.height = height;
    }

    public boolean containsBlock(Block block){
        AtomicBoolean contains = new AtomicBoolean(false);
        tiles.each(t -> {
            if(t.block == block) contains.set(true);
        });
        return contains.get();

    }

    public float powerProduction(){
        return tiles.sumf(s -> s.block instanceof PowerGenerator p ? p.powerProduction : 0f);
    }

    public float powerConsumption(){
        return tiles.sumf(s -> s.block.consPower != null ? s.block.consPower.usage : 0f);
    }

    public void calProduction() {
        items = new ObjectFloatMap<>(content.items().copy().size << 1);
        liquids = new ObjectFloatMap<>(content.liquids().copy().size << 1);
        tiles.each(t -> {
            if(t.block== null)
                return;

            if (t.block instanceof GenericCrafter gc) {
                for(var c:gc.consumeBuilder){
                    if(c.optional) continue;
                    else if(c instanceof ConsumeItems consumeItems){
                        for (ItemStack stack : consumeItems.items) {
                            Item item = stack.item;
                            items.put(item, items.get(item, 0) - stack.amount * 60f / gc.craftTime);
                        }
                    }
                    else if(c instanceof ConsumeLiquid consumeLiquid){
                        Liquid liquid = consumeLiquid.liquid;
                        liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
                    }
                    else if(c instanceof ConsumeLiquids consumeLiquids){
                        for (LiquidStack stack : consumeLiquids.liquids) {
                            Liquid liquid = stack.liquid;
                            liquids.put(liquid, liquids.get(liquid, 0) - stack.amount * 60f);
                        }
                    }
                }
                if(gc.outputsItems()){
                    for (ItemStack stack : gc.outputItems) {
                        Item item = stack.item;
                        items.put(item, items.get(item, 0) + stack.amount * 60f / gc.craftTime);
                    }
                }/*
                if(gc.outputLiquid != null){
                    liquids.put(gc.outputLiquid.liquid, liquids.get(gc.outputLiquid.liquid, 0) + gc.outputLiquid.amount * 60f);
                }*/
                if(gc.outputLiquids !=null) {
                    for (LiquidStack stack : gc.outputLiquids) {
                        Liquid liquid = stack.liquid;
                        liquids.put(liquid, liquids.get(liquid, 0) + stack.amount * 60f);
                    }
                }
            }
            else if (t.block instanceof Separator s) {
                for(var c:s.consumeBuilder){
                    if(c.optional) continue;
                    else if(c instanceof ConsumeItems consumeItems){
                        for (ItemStack stack : consumeItems.items) {
                            Item item = stack.item;
                            items.put(item, items.get(item, 0) - stack.amount * 60f / s.craftTime);
                        }
                    }
                    else if (c instanceof ConsumeLiquid consumeLiquid) {
                        Liquid liquid = consumeLiquid.liquid;
                        liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
                    }
                }
            }
            else if (t.block instanceof Fracker f) {
                for(var c:f.consumeBuilder) {
                    if (c.optional) continue;
                    else if (c instanceof ConsumeItems consumeItems) {
                        for (ItemStack stack : consumeItems.items) {
                            Item item = stack.item;
                            items.put(item, items.get(item, 0) - stack.amount * f.itemUseTime / 60f);
                        }
                    }
                }
            }
            else if (t.block instanceof PowerGenerator) {
                if (t.block instanceof  ConsumeGenerator cg) {
                    for(var c:cg.consumeBuilder){
                        if(c.optional) continue;
                        else if(c instanceof ConsumeItems consumeItems){
                            for (ItemStack stack : consumeItems.items) {
                                Item item = stack.item;
                                items.put(item, items.get(item, 0) - stack.amount * 60f / cg.itemDuration);
                            }
                        }
                        else if(c instanceof ConsumeLiquid consumeLiquid){
                            Liquid liquid = consumeLiquid.liquid;
                            liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
                        }
                        else if(c instanceof ConsumeLiquids consumeLiquids){
                            for (LiquidStack stack : consumeLiquids.liquids) {
                                Liquid liquid = stack.liquid;
                                liquids.put(liquid, liquids.get(liquid, 0) - stack.amount * 60f);
                            }
                        }
                    }
                }
                else if (t.block instanceof NuclearReactor nr) {
                    for(var c:nr.consumeBuilder){
                        if(c.optional) continue;
                        else if(c instanceof ConsumeItems consumeItems){
                            for (ItemStack stack : consumeItems.items) {
                                Item item = stack.item;
                                items.put(item, items.get(item, 0) - stack.amount * 60f / nr.itemDuration);
                            }
                        }
                        else if(c instanceof ConsumeLiquid consumeLiquid){
                            Liquid liquid = consumeLiquid.liquid;
                            liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
                        }
                        else if(c instanceof ConsumeLiquids consumeLiquids){
                            for (LiquidStack stack : consumeLiquids.liquids) {
                                Liquid liquid = stack.liquid;
                                liquids.put(liquid, liquids.get(liquid, 0) - stack.amount * 60f);
                            }
                        }
                    }
                }
                else if (t.block instanceof ImpactReactor ir) {
                    for(var c:ir.consumeBuilder){
                        if(c.optional) continue;
                        else if(c instanceof ConsumeItems consumeItems){
                            for (ItemStack stack : consumeItems.items) {
                                Item item = stack.item;
                                items.put(item, items.get(item, 0) - stack.amount * 60f / ir.itemDuration);
                            }
                        }
                        else if(c instanceof ConsumeLiquid consumeLiquid){
                            Liquid liquid = consumeLiquid.liquid;
                            liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
                        }
                        else if(c instanceof ConsumeLiquids consumeLiquids){
                            for (LiquidStack stack : consumeLiquids.liquids) {
                                Liquid liquid = stack.liquid;
                                liquids.put(liquid, liquids.get(liquid, 0) - stack.amount * 60f);
                            }
                        }
                    }
                }
            }
        });
    }

    public ItemSeq requirements(){
        ItemSeq requirements = new ItemSeq();

        tiles.each(t -> {
            for(ItemStack stack : t.block.requirements){
                requirements.add(stack.item, stack.amount);
            }
        });

        return requirements;
    }

    public boolean hasCore(){
        return tiles.contains(s -> s.block instanceof CoreBlock);
    }

    public CoreBlock findCore(){
        Stile tile = tiles.find(s -> s.block instanceof CoreBlock);
        if(tile == null) throw new IllegalArgumentException("Schematic is missing a core!");
        return (CoreBlock)tile.block;
    }

    public String name(){
        return tags.get("name", "unknown");
    }

    public String description(){
        return tags.get("description", "");
    }

    public void save(){
        schematics.saveChanges(this);
    }

    @Override
    public String getSteamID(){
        return tags.get("steamid");
    }

    @Override
    public void addSteamID(String id){
        tags.put("steamid", id);
        save();
    }

    @Override
    public void removeSteamID(){
        tags.remove("steamid");
        save();
    }

    @Override
    public String steamTitle(){
        return name();
    }

    @Override
    public String steamDescription(){
        return description();
    }

    @Override
    public String steamTag(){
        return "schematic";
    }

    @Override
    public Fi createSteamFolder(String id){
        Fi directory = tmpDirectory.child("schematic_" + id).child("schematic." + schematicExtension);
        file.copyTo(directory);
        return directory;
    }

    @Override
    public Fi createSteamPreview(String id){
        Fi preview = tmpDirectory.child("schematic_preview_" + id + ".png");
        schematics.savePreview(this, preview);
        return preview;
    }

    @Override
    public int compareTo(Schematic schematic){
        return name().compareTo(schematic.name());
    }

    public static class Stile{
        public Block block;
        public short x, y;
        public Object config;
        public byte rotation;

        public Stile(Block block, int x, int y, Object config, byte rotation){
            this.block = block;
            this.x = (short)x;
            this.y = (short)y;
            this.config = config;
            this.rotation = rotation;
        }

        //pooling only
        public Stile(){
            block = Blocks.air;
        }

        public Stile(Block block, int x, int y){
            this.block = block;
            this.x = (short)x;
            this.y = (short)y;
        }

        public Stile set(Stile other){
            block = other.block;
            x = other.x;
            y = other.y;
            config = other.config;
            rotation = other.rotation;
            return this;
        }

        public Stile copy(){
            return new Stile(block, x, y, config, rotation);
        }
    }
}
