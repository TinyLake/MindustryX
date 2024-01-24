package mindustry.ui.fragments;

import arc.*;
import arc.func.Boolf;
import arc.graphics.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.arcModule.ARCVars;
import mindustry.arcModule.ui.dialogs.BlockSelectDialog;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.ConstructBlock.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;

public class PlacementFragment{
    private int rowWidth = Math.max(Core.settings.getInt("itemSelectionWidth"),4);
    private int maxRow = Math.max(Core.settings.getInt("itemSelectionHeight"),4);

    public Category currentCategory = Category.distribution;

    Seq<Block> returnArray = new Seq<>(), returnArray2 = new Seq<>();
    Seq<Category> returnCatArray = new Seq<>();
    boolean[] categoryEmpty = new boolean[Category.all.length];
    public ObjectMap<Category,Block> selectedBlocks = new ObjectMap<>();
    ObjectFloatMap<Category> scrollPositions = new ObjectFloatMap<>();
    @Nullable Block menuHoverBlock;
    @Nullable Displayable hover,hover2;
    @Nullable Tile hoverTile;
    @Nullable Object lastDisplayTile;
    @Nullable Building lastFlowBuild, nextFlowBuild;
    @Nullable Object lastDisplayState;
    @Nullable Team lastTeam;
    boolean wasHovered;
    Table blockTable, toggler, topTable, blockCatTable, commandTable;
    Stack mainStack;
    ScrollPane blockPane;
    Runnable rebuildCommand;
    boolean blockSelectEnd, wasCommandMode;
    int blockSelectSeq;
    long blockSelectSeqMillis;
    Binding[] blockSelect = {
        Binding.block_select_01,
        Binding.block_select_02,
        Binding.block_select_03,
        Binding.block_select_04,
        Binding.block_select_05,
        Binding.block_select_06,
        Binding.block_select_07,
        Binding.block_select_08,
        Binding.block_select_09,
        Binding.block_select_10,
        Binding.block_select_left,
        Binding.block_select_right,
        Binding.block_select_up,
        Binding.block_select_down
    };

    public PlacementFragment(){
        Events.on(WorldLoadEvent.class, event -> {
            Core.app.post(() -> {
                currentCategory = Category.distribution;
                control.input.block = null;
                rebuild();
            });
        });

        Events.run(Trigger.unitCommandChange, () -> {
            if(rebuildCommand != null){
                rebuildCommand.run();
            }
        });

        Events.on(UnlockEvent.class, event -> {
            if(event.content instanceof Block){
                rebuild();
            }
        });

        Events.on(ResetEvent.class, event -> {
            selectedBlocks.clear();
        });

        Events.run(Trigger.update, () -> {
            //disable flow updating on previous building so it doesn't waste CPU
            if(lastFlowBuild != null && lastFlowBuild != nextFlowBuild){
                if(lastFlowBuild.flowItems() != null) lastFlowBuild.flowItems().stopFlow();
                if(lastFlowBuild.liquids != null) lastFlowBuild.liquids.stopFlow();
            }

            lastFlowBuild = nextFlowBuild;

            if(nextFlowBuild != null){
                if(nextFlowBuild.flowItems() != null) nextFlowBuild.flowItems().updateFlow();
                if(nextFlowBuild.liquids != null) nextFlowBuild.liquids.updateFlow();
            }

            if (rowWidth != Core.settings.getInt("itemSelectionWidth", 4)) {
                rowWidth = Core.settings.getInt("itemSelectionWidth", 4);
                rebuild();
            }

            if (maxRow != Core.settings.getInt("itemSelectionHeight", 4)) {
                maxRow = Core.settings.getInt("itemSelectionHeight", 4);
                rebuild();
            }
        });
    }

    public Displayable hover(){
        return hover;
    }

    public void rebuild(){
        //category does not change on rebuild anymore, only on new world load
        Group group = toggler.parent;
        int index = toggler.getZIndex();
        toggler.remove();
        build(group);
        toggler.setZIndex(index);
    }

    boolean gridUpdate(InputHandler input){
        scrollPositions.put(currentCategory, blockPane.getScrollY());

        if(Core.input.keyTap(Binding.pick) && player.isBuilder() && !Core.scene.hasDialog()){ //mouse eyedropper select
            var build = world.buildWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);

            if (build == null && AdvanceToolTable.worldCreator) {
                var tile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
                if (tile != null) {
                    Block target;
                    if (tile.block() != Blocks.air) {
                        target = tile.block();
                    }
                    else if (tile.overlay() != Blocks.air) {
                        target = tile.overlay();
                    }
                    else {
                        target = tile.floor();
                    }
                    if (target != Blocks.air && (target.isVisible() || AdvanceToolTable.allBlocksReveal)) {
                        input.block = target;
                        currentCategory = input.block.category;
                        return true;
                    }

                }
            }
            //can't middle click buildings in fog
            if(build != null && build.inFogTo(player.team())){
                build = null;
            }

            Block tryRecipe = build == null ? null : build instanceof ConstructBuild c ? c.current : build.block;
            Object tryConfig = build == null || !build.block.copyConfig ? null : build.config();

            for(BuildPlan req : player.unit().plans()){
                if(!req.breaking && req.block.bounds(req.x, req.y, Tmp.r1).contains(Core.input.mouseWorld())){
                    tryRecipe = req.block;
                    tryConfig = req.config;
                    break;
                }
            }

            if(tryRecipe != null && tryRecipe.isVisible() && unlocked(tryRecipe)){
                input.block = tryRecipe;
                tryRecipe.lastConfig = tryConfig;
                currentCategory = input.block.category;
                return true;
            }
        }

        if(ui.chatfrag.shown() || ui.consolefrag.shown() || Core.scene.hasKeyboard()) return false;

        for(int i = 0; i < blockSelect.length; i++){
            if(Core.input.keyTap(blockSelect[i])){
                if(i > 9){ //select block directionally
                    Seq<Block> blocks = getUnlockedByCategory(currentCategory);
                    Block currentBlock = getSelectedBlock(currentCategory);
                    for(int j = 0; j < blocks.size; j++){
                        if(blocks.get(j) == currentBlock){
                            switch(i){
                                //left
                                case 10 -> j = (j - 1 + blocks.size) % blocks.size;
                                //right
                                case 11 -> j = (j + 1) % blocks.size;
                                //up
                                case 12 -> {
                                    j = (j > 3 ? j - 4 : blocks.size - blocks.size % 4 + j);
                                    j -= (j < blocks.size ? 0 : 4);
                                }
                                //down
                                case 13 -> j = (j < blocks.size - 4 ? j + 4 : j % 4);
                            }
                            input.block = blocks.get(j);
                            selectedBlocks.put(currentCategory, input.block);
                            break;
                        }
                    }
                }else if(blockSelectEnd || Time.timeSinceMillis(blockSelectSeqMillis) > 400){ //1st number of combo, select category
                    //select only visible categories
                    if(!getUnlockedByCategory(Category.all[i]).isEmpty()){
                        currentCategory = Category.all[i];
                        if(input.block != null){
                            input.block = getSelectedBlock(currentCategory);
                        }
                        blockSelectEnd = false;
                        blockSelectSeq = 0;
                        blockSelectSeqMillis = Time.millis();
                    }
                }else{ //select block
                    if(blockSelectSeq == 0){ //2nd number of combo
                        blockSelectSeq = i + 1;
                    }else{ //3rd number of combo
                        //entering "X,1,0" selects the same block as "X,0"
                        i += (blockSelectSeq - (i != 9 ? 0 : 1)) * 10;
                        blockSelectEnd = true;
                    }
                    Seq<Block> blocks = getByCategory(currentCategory);
                    if(i >= blocks.size || !unlocked(blocks.get(i))) return true;
                    input.block = (i < blocks.size) ? blocks.get(i) : null;
                    selectedBlocks.put(currentCategory, input.block);
                    blockSelectSeqMillis = Time.millis();
                }
                return true;
            }
        }

        if(Core.input.keyTap(Binding.category_prev)){
            int i = 0;
            do{
                currentCategory = currentCategory.prev();
                i ++;
            }while(categoryEmpty[currentCategory.ordinal()] && i < categoryEmpty.length);
            input.block = getSelectedBlock(currentCategory);
            return true;
        }

        if(Core.input.keyTap(Binding.category_next)){
            int i = 0;
            do{
                currentCategory = currentCategory.next();
                i ++;
            }while(categoryEmpty[currentCategory.ordinal()] && i < categoryEmpty.length);
            input.block = getSelectedBlock(currentCategory);
            return true;
        }

        if(Core.input.keyTap(Binding.block_info)){
            var build = world.buildWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
            Block hovering = build == null ? null : build instanceof ConstructBuild c ? c.current : build.block;
            Block displayBlock = menuHoverBlock != null ? menuHoverBlock : input.block != null ? input.block : hovering;
            if(displayBlock != null && displayBlock.unlockedNow()){
                ui.content.show(displayBlock);
                Events.fire(new BlockInfoEvent());
            }
        }

        return false;
    }

    public void build(Group parent){
        parent.fill(full -> {
            toggler = full;
            full.bottom().right().visible(() -> ui.hudfrag.shown);

            full.table(frame -> {

                //rebuilds the category table with the correct recipes
                Runnable rebuildCategory = () -> {
                    blockTable.clear();
                    blockTable.top().margin(5);

                    int index = 0;

                    ButtonGroup<ImageButton> group = new ButtonGroup<>();
                    group.setMinCheckCount(0);

                    for(Block block : getUnlockedByCategory(currentCategory)){
                        if(!unlocked(block) && !AdvanceToolTable.allBlocksReveal) continue;
                        if (block == Blocks.air || block instanceof ConstructBlock) continue;
                        if(index++ % rowWidth == 0){
                            blockTable.row();
                        }

                        ImageButton button = blockTable.button(new TextureRegionDrawable(block.uiIcon), Styles.selecti, () -> {
                            if(unlocked(block) || AdvanceToolTable.allBlocksReveal){
                                if((Core.input.keyDown(KeyCode.shiftLeft) || Core.input.keyDown(KeyCode.controlLeft)) && Fonts.getUnicode(block.name) != 0){
                                    Core.app.setClipboardText((char)Fonts.getUnicode(block.name) + "");
                                    ui.showInfoFade("@copied");
                                }else{
                                    control.input.block = control.input.block == block ? null : block;
                                    selectedBlocks.put(currentCategory, control.input.block);
                                }
                            }
                        }).size(46f).group(group).name("block-" + block.name).get();
                        button.resizeImage(iconMed);

                        button.update(() -> { //color unplacable things gray
                            Building core = player.core();
                            Color color = (state.rules.infiniteResources || (core != null && (core.items.has(block.requirements, state.rules.buildCostMultiplier) || state.rules.infiniteResources))) && player.isBuilder() ? Color.white : Color.gray;
                            button.forEach(elem -> elem.setColor(color));
                            button.setChecked(control.input.block == block);

                            if(!block.isPlaceable()){
                                button.forEach(elem -> elem.setColor(Color.darkGray));
                            }
                        });

                        button.hovered(() -> menuHoverBlock = block);
                        button.exited(() -> {
                            if(menuHoverBlock == block){
                                menuHoverBlock = null;
                            }
                        });
                    }
                    //add missing elements to even out table size
                    if(index < 4){
                        for(int i = 0; i < 4-index; i++){
                            blockTable.add().size(46f);
                        }
                    }
                    blockTable.act(0f);
                    blockPane.setScrollYForce(scrollPositions.get(currentCategory, 0));
                    Core.app.post(() -> {
                        blockPane.setScrollYForce(scrollPositions.get(currentCategory, 0));
                        blockPane.act(0f);
                        blockPane.layout();
                    });
                };

                //top table with hover info
                frame.table(Tex.buttonEdge2,top -> {
                    topTable = top;
                    top.add(new Table()).growX().update(topTable -> {

                        //find current hovered thing
                        Displayable hovered = hover;
                        Displayable hovered2 = hover2;
                        Displayable hoveredTile = hoverTile;
                        Block displayBlock = menuHoverBlock != null ? menuHoverBlock : control.input.block;
                        Object displayState = displayBlock != null ? displayBlock : hovered;
                        boolean isHovered = displayBlock == null; //use hovered thing if displayblock is null

                        //don't refresh unnecessarily
                        //refresh only when the hover state changes, or the displayed block changes
                        if(wasHovered == isHovered && lastDisplayState == displayState && lastDisplayTile == hoveredTile) return;

                        topTable.clear();
                        topTable.top().left().margin(5);

                        lastDisplayState = displayState;
                        wasHovered = isHovered;
                        lastDisplayTile = hoveredTile;
                        lastTeam = player.team();

                        //show details of selected block, with costs
                        if(displayBlock != null){

                            topTable.table(header -> {
                                String keyCombo = "";
                                if(!mobile){
                                    Seq<Block> blocks = getByCategory(currentCategory);
                                    for(int i = 0; i < blocks.size; i++){
                                        if(blocks.get(i) == displayBlock && (i + 1) / 10 - 1 < blockSelect.length){
                                            keyCombo = Core.bundle.format("placement.blockselectkeys", Core.keybinds.get(blockSelect[currentCategory.ordinal()]).key.toString())
                                                + (i < 10 ? "" : Core.keybinds.get(blockSelect[(i + 1) / 10 - 1]).key.toString() + ",")
                                                + Core.keybinds.get(blockSelect[i % 10]).key.toString() + "]";
                                            break;
                                        }
                                    }
                                }
                                final String keyComboFinal = keyCombo;
                                header.left();
                                header.add(new Image(displayBlock.uiIcon)).size(8 * 4);
                                header.labelWrap(() -> !unlocked(displayBlock) && !AdvanceToolTable.allBlocksReveal ? Core.bundle.get("block.unknown") : displayBlock.localizedName + keyComboFinal)
                                .left().width(190f).padLeft(5);
                                header.add().growX();
                                if(unlocked(displayBlock) || AdvanceToolTable.allBlocksReveal){
                                    header.button("?", Styles.flatBordert, () -> {
                                        ui.content.show(displayBlock);
                                        Events.fire(new BlockInfoEvent());
                                    }).size(8 * 5).padTop(-5).padRight(-5).right().grow().name("blockinfo");
                                }
                            }).growX().left();
                            topTable.row();
                            //add requirement table
                            topTable.table(req -> {
                                req.top().left();

                                for(ItemStack stack : displayBlock.requirements){
                                    req.table(line -> {
                                        line.left();
                                        line.image(stack.item.uiIcon).size(8 * 2);
                                        line.add((Core.settings.getBool("colorizedContent") ? "": "[lightgray]") + stack.item.localizedName).maxWidth(140f).fillX().padLeft(2).left().get().setEllipsis(true);
                                        line.labelWrap(() -> {
                                            Building core = player.core();
                                            int stackamount = Math.round(stack.amount * state.rules.buildCostMultiplier);
                                            if(core == null || state.rules.infiniteResources) return "*/" + stackamount;

                                            int amount = core.items.get(stack.item);
                                            String color = (amount < stackamount / 2f ? "[scarlet]" : amount < stackamount ? "[accent]" : "[white]");

                                            return color + UI.formatAmount(amount) + "[white]/" + stackamount;
                                        }).padLeft(5);
                                    }).left();
                                    req.row();
                                }
                            }).growX().left().margin(3);

                            if(!displayBlock.isPlaceable() || !player.isBuilder()){
                                topTable.row();
                                topTable.table(b -> {
                                    b.image(Icon.cancel).padRight(2).color(Color.scarlet);
                                    b.add(!player.isBuilder() ? "@unit.nobuild" : !displayBlock.supportsEnv(state.rules.env) ? "@unsupported.environment" : "@banned").width(190f).wrap();
                                    b.left();
                                }).padTop(2).left();
                            }

                        }else if(hovered != null){
                            //show hovered item, whatever that may be
                            hovered.display(topTable);
                            if(hovered2 != hovered && hovered2 != null){
                                topTable.row();
                                hovered2.display(topTable);
                            }}

                        //只要可行便绘制地板|建筑，移除了其他重复绘制
                        if (Core.settings.getBool("hoveredTileInfo") && hoveredTile != null) {
                            topTable.row();
                            topTable.row();
                            topTable.table(t -> {
                                t.left();
                                t.add(new Image(hoverTile.floor().uiIcon)).size(20f).left();
                                t.add(" " + hoverTile.floor().localizedName).left();
                                if (hoverTile.block() != Blocks.air)
                                    t.add(" | " + hoverTile.block().emoji() + (hoverTile.build != null ? "[#" + hoverTile.build.team.color + "]" : "") + hoverTile.block().localizedName).left();
                                if (hoverTile.overlay() != Blocks.air)
                                    t.add(" | " + hoverTile.overlay().emoji() + hoverTile.overlay().localizedName).left();
                            }).growX().left();
                        }

                    });
                }).colspan(3).fillX().visible(this::hasInfoBox).touchable(Touchable.enabled).row();

                frame.image().color(Pal.gray).colspan(3).height(4).growX().row();

                blockCatTable = new Table();
                commandTable = new Table(Tex.pane2);
                mainStack = new Stack();

                mainStack.update(() -> {
                    if(control.input.commandMode != wasCommandMode){
                        mainStack.clearChildren();
                        mainStack.addChild(control.input.commandMode ? commandTable : blockCatTable);

                        //hacky, but forces command table to be same width as blocks
                        if(control.input.commandMode){
                            commandTable.getCells().peek().width(blockCatTable.getWidth() / Scl.scl(1f));
                        }

                        wasCommandMode = control.input.commandMode;
                    }
                });

                frame.add(mainStack).colspan(3).fill();

                frame.row();

                //for better inset visuals at the bottom
                frame.rect((x, y, w, h) -> {
                    if(Core.scene.marginBottom > 0){
                        Tex.paneLeft.draw(x, 0, w, y);
                    }
                }).colspan(3).fillX().row();

                //commandTable: commanded units
                if (Core.settings.getBool("arcCommandTable")){
                    commandTable.touchable = Touchable.enabled;
                    commandTable.add(Core.bundle.get("commandmode.name")).fill().center().labelAlign(Align.center).row();
                    commandTable.image().color(Pal.accent).growX().pad(20f).padTop(0f).padBottom(4f).row();
                    commandTable.table(u -> {
                        u.left();
                        int[] curCount = {0};
                        UnitCommand[] currentCommand = {null};
                        var commands = new Seq<UnitCommand>();

                        rebuildCommand = () -> {
                            u.clearChildren();
                            var units = control.input.selectedUnits;
                            if(units.size > 0){
                                int[] counts = new int[content.units().size];
                                for(var unit : units){
                                    counts[unit.type.id] ++;
                                }
                                commands.clear();
                                boolean firstCommand = false;
                                Table unitlist = u.table().growX().left().get();
                                unitlist.left();

                                int col = 0;
                                for(int i = 0; i < counts.length; i++){
                                    if(counts[i] > 0){
                                        var type = content.unit(i);
                                        unitlist.add(new ItemImage(type.uiIcon, counts[i])).tooltip(type.localizedName).pad(4).with(b -> {
                                            var listener = new ClickListener();

                                            //left click -> select
                                            b.clicked(KeyCode.mouseLeft, () -> {
                                                control.input.selectedUnits.removeAll(unit -> unit.type != type);
                                                Events.fire(Trigger.unitCommandChange);
                                            });
                                            //right click -> remove
                                            b.clicked(KeyCode.mouseRight, () -> {
                                                control.input.selectedUnits.removeAll(unit -> unit.type == type);
                                                Events.fire(Trigger.unitCommandChange);
                                            });

                                            b.addListener(listener);
                                            b.addListener(new HandCursorListener());
                                            //gray on hover
                                            b.update(() -> ((Group)b.getChildren().first()).getChildren().first().setColor(listener.isOver() ? Color.lightGray : Color.white));
                                        });

                                        if(++col % 7 == 0){
                                            unitlist.row();
                                        }

                                        if(!firstCommand){
                                            commands.add(type.commands);
                                            firstCommand = true;
                                        }else{
                                            //remove commands that this next unit type doesn't have
                                            commands.removeAll(com -> !Structs.contains(type.commands, com));
                                        }
                                    }
                                }

                                if(commands.size > 1){
                                    u.row();

                                    u.table(coms -> {
                                        coms.add(Blocks.worldProcessor.emoji()).width(20f).padRight(20f);
                                        for(var command : commands){
                                            coms.button(Icon.icons.get(command.icon, Icon.cancel), Styles.clearNoneTogglei, () -> {
                                                IntSeq ids = new IntSeq();
                                                for(var unit : units){
                                                    ids.add(unit.id);
                                                }

                                                Call.setUnitCommand(player, ids.toArray(), command);
                                            }).checked(i -> currentCommand[0] == command).size(50f).tooltip(command.localized());
                                        }
                                    }).fillX().padTop(4f).left();
                                }
                                if(control.input.selectedUnits.size > 0){
                                    u.row();
                                    u.table(sp -> {
                                        float wound = (float) Core.settings.getInt("rtsWoundUnit") / 100f;
                                        if (control.input.selectedUnits.contains(unit -> unit.health >= unit.maxHealth * wound) && control.input.selectedUnits.contains(unit -> unit.health < unit.maxHealth * wound)){
                                            sp.table(spp->{
                                                arcSelectUnits(spp,"[green]\uE813","高血量单位", unit -> unit.health >= unit.maxHealth * wound);
                                                arcSelectUnits(spp,"[red]\uE80F","低血量单位", unit -> unit.health < unit.maxHealth * wound);
                                            });
                                        }

                                        if (control.input.selectedUnits.contains(unit -> unit.type.commands.length > 1) && control.input.selectedUnits.contains(unit -> unit.type.commands.length <= 1)){
                                            sp.table(spp->{
                                                arcSelectUnits(spp,"\uE86E","进攻性单位", unit -> unit.type.commands.length <= 1);
                                                arcSelectUnits(spp,"\uE86B","辅助性单位", unit -> unit.type.commands.length > 1);
                                            });
                                        }

                                        int hasFlyer = control.input.selectedUnits.contains(unit -> unit.isFlying()) ? 1 : 0;
                                        int hasLand = control.input.selectedUnits.contains(unit -> !unit.isFlying() && !unit.type.naval) ? 1 : 0;
                                        int hasNaval = control.input.selectedUnits.contains(unit -> unit.type.naval) ? 1 : 0;
                                        if (hasFlyer + hasLand + hasNaval >=2 ){
                                            sp.table(spp->{
                                                if (hasFlyer == 1) arcSelectUnits(spp,UnitTypes.flare.emoji(),"飞行单位", unit -> unit.isFlying());
                                                if (hasLand == 1) arcSelectUnits(spp,UnitTypes.crawler.emoji(),"陆军单位", unit -> !unit.isFlying() && !unit.type.naval);
                                                if (hasNaval == 1) arcSelectUnits(spp,UnitTypes.retusa.emoji(),"海军单位", unit -> unit.type.naval);
                                            });
                                        }
                                    }).fillX().padTop(4f).left();
                                }
                            }else{
                                u.add(Core.bundle.get("commandmode.nounits")).color(Color.lightGray).growX().center().labelAlign(Align.center).pad(6);
                            }
                            if (mobile) {
                                u.row();
                                u.table(sp->{
                                    sp.button("\uE86D", Styles.cleart, () -> {
                                        control.input.selectedUnits.clear();
                                        control.input.commandBuildings.clear();
                                        for(var unit : player.team().data().units){
                                            if(unit.isCommandable()){
                                                control.input.selectedUnits.add(unit);
                                            }
                                        }
                                    }).size(40f);
                                    sp.button(Blocks.additiveReconstructor.emoji(), Styles.cleart, () -> {
                                        control.input.selectedUnits.clear();
                                        control.input.commandBuildings.clear();
                                        for(var build : player.team().data().buildings){
                                            if(build.block.commandable){
                                                control.input.commandBuildings.add(build);
                                            }
                                        }
                                    }).size(40f);
                                }).fillX().padTop(4f).left().row();
                            }
                        };

                        u.update(() -> {
                            boolean hadCommand = false;
                            UnitCommand shareCommand = null;

                            //find the command that all units have, or null if they do not share one
                            for(var unit : control.input.selectedUnits){
                                if(unit.isCommandable()){
                                    var nextCommand = unit.command().command;

                                    if(hadCommand){
                                        if(shareCommand != nextCommand){
                                            shareCommand = null;
                                        }
                                    }else{
                                        shareCommand = nextCommand;
                                        hadCommand = true;
                                    }
                                }
                            }

                            currentCommand[0] = shareCommand;

                            int size = control.input.selectedUnits.size;
                            if(curCount[0] != size){
                                curCount[0] = size;
                                rebuildCommand.run();
                            }

                            for(UnitCommand command : commands){
                                //first stance must always be the stop stance
                                if(command.keybind != null && Core.input.keyTap(command.keybind)){
                                    Call.setUnitCommand(player, control.input.selectedUnits.mapInt(un -> un.id).toArray(), command);
                                }
                            }
                        });
                        rebuildCommand.run();
                    }).grow();

                } else{//commandTable: commanded units
                        commandTable.touchable = Touchable.enabled;
                        commandTable.add("[accent]指挥模式").fill().center().labelAlign(Align.center).row();
                        commandTable.image().color(Pal.accent).growX().pad(20f).padTop(0f).padBottom(4f).row();
                        commandTable.table(u -> {
                            u.left();
                            int[] curCount = {0};
                            UnitCommand[] currentCommand = {null};
                            var commands = new Seq<UnitCommand>();

                            rebuildCommand = () -> {
                                u.clearChildren();
                                var units = control.input.selectedUnits;
                                if(units.size > 0){
                                    int[] counts = new int[content.units().size];
                                    for(var unit : units){
                                        counts[unit.type.id] ++;
                                    }
                                    commands.clear();
                                    boolean firstCommand = false;
                                    Table unitlist = u.table().growX().left().get();
                                    unitlist.left();

                                    int col = 0;
                                    for(int i = 0; i < counts.length; i++){
                                        if(counts[i] > 0){
                                            var type = content.unit(i);
                                            unitlist.add(new ItemImage(type.uiIcon, counts[i])).tooltip(type.localizedName).pad(4).with(b -> {
                                                var listener = new ClickListener();

                                                //left click -> select
                                                b.clicked(KeyCode.mouseLeft, () -> control.input.selectedUnits.removeAll(unit -> unit.type != type));
                                                //right click -> remove
                                                b.clicked(KeyCode.mouseRight, () -> control.input.selectedUnits.removeAll(unit -> unit.type == type));

                                                b.addListener(listener);
                                                b.addListener(new HandCursorListener());
                                                //gray on hover
                                                b.update(() -> ((Group)b.getChildren().first()).getChildren().first().setColor(listener.isOver() ? Color.lightGray : Color.white));
                                            });

                                            if(++col % 7 == 0){
                                                unitlist.row();
                                            }

                                            if(!firstCommand){
                                                commands.add(type.commands);
                                                firstCommand = true;
                                            }else{
                                                //remove commands that this next unit type doesn't have
                                                commands.removeAll(com -> !Structs.contains(type.commands, com));
                                            }
                                        }
                                    }

                                    if(commands.size > 1){
                                        u.row();

                                        u.table(coms -> {
                                            for(var command : commands){
                                                coms.button(Icon.icons.get(command.icon, Icon.cancel), Styles.clearNoneTogglei, () -> {
                                                    IntSeq ids = new IntSeq();
                                                    for(var unit : units){
                                                        ids.add(unit.id);
                                                    }

                                                    Call.setUnitCommand(player, ids.toArray(), command);
                                                }).checked(i -> currentCommand[0] == command).size(50f).tooltip(command.localized());
                                            }
                                        }).fillX().padTop(4f).left();
                                    }
                                }else{
                                    u.add("[未选择单位]").color(ARCVars.getThemeColor()).growX().center().labelAlign(Align.center).pad(6);
                                }
                            };

                            u.update(() -> {
                                boolean hadCommand = false;
                                UnitCommand shareCommand = null;

                                //find the command that all units have, or null if they do not share one
                                for(var unit : control.input.selectedUnits){
                                    if(unit.isCommandable()){
                                        var nextCommand = unit.command().currentCommand();

                                        if(hadCommand){
                                            if(shareCommand != nextCommand){
                                                shareCommand = null;
                                            }
                                        }else{
                                            shareCommand = nextCommand;
                                            hadCommand = true;
                                        }
                                    }
                                }

                                currentCommand[0] = shareCommand;

                                int size = control.input.selectedUnits.size;
                                if(curCount[0] != size){
                                    curCount[0] = size;
                                    rebuildCommand.run();
                                }
                            });
                            rebuildCommand.run();
                        }).grow();

                }

                //blockCatTable: all blocks | all categories
                {
                    blockCatTable.table(Tex.pane2, blocksSelect -> {
                        blocksSelect.margin(4).marginTop(0);
                        blockPane = blocksSelect.pane(blocks -> blockTable = blocks).height(maxRow * 46f + 10f).update(pane -> {
                            if(pane.hasScroll()){
                                Element result = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                                if(result == null || !result.isDescendantOf(pane)){
                                    Core.scene.setScrollFocus(null);
                                }
                            }
                        }).grow().get();
                        blockPane.setStyle(Styles.smallPane);
                        blocksSelect.row();
                        blocksSelect.table(control.input::buildPlacementUI).name("inputTable").growX();
                    }).fillY().bottom().touchable(Touchable.enabled);
                    blockCatTable.table(categories -> {
                        categories.bottom();
                        categories.add(new Image(Styles.black6){
                            @Override
                            public void draw(){
                                if(height <= Scl.scl(3f)) return;
                                getDrawable().draw(x, y, width, height - Scl.scl(3f));
                            }
                        }).colspan(2).growX().growY().padTop(-3f).row();
                        categories.defaults().size(50f);

                        ButtonGroup<ImageButton> group = new ButtonGroup<>();

                        //update category empty values
                        for(Category cat : Category.all){
                            Seq<Block> blocks = getUnlockedByCategory(cat);
                            categoryEmpty[cat.ordinal()] = blocks.isEmpty();
                        }

                        boolean needsAssign = categoryEmpty[currentCategory.ordinal()];

                        int f = 0;
                        if (maxRow > 5){
                            categories.button(Icon.zoom, Styles.clearTogglei, () -> {
                                new BlockSelectDialog(block -> block.replaceable, block -> {
                                    control.input.block = block;
                                    currentCategory = block.category;
                                    rebuildCategory.run();
                                    }, block -> control.input.block == block).show();
                            }).group(group);
                            categories.image(Styles.black6);
                            categories.row();
                        }
                        for(Category cat : getCategories()){
                            if(f++ % 2 == 0) categories.row();

                            if(categoryEmpty[cat.ordinal()]){
                                categories.image(Styles.black6);
                                continue;
                            }

                            if(needsAssign){
                                currentCategory = cat;
                                needsAssign = false;
                            }

                            categories.button(ui.getIcon(cat.name()), Styles.clearTogglei, () -> {
                                currentCategory = cat;
                                if(control.input.block != null){
                                    control.input.block = getSelectedBlock(currentCategory);
                                }
                                rebuildCategory.run();
                            }).group(group).update(i -> i.setChecked(currentCategory == cat)).name("category-" + cat.name());
                        }
                    }).fillY().bottom().touchable(Touchable.enabled);
                }

                mainStack.add(blockCatTable);

                rebuildCategory.run();
                frame.update(() -> {
                    if(!control.input.commandMode && gridUpdate(control.input)){
                        rebuildCategory.run();
                    }
                });
            });
        });
    }

    Seq<Category> getCategories(){
        return returnCatArray.clear().addAll(Category.all).sort((c1, c2) -> Boolean.compare(categoryEmpty[c1.ordinal()], categoryEmpty[c2.ordinal()]));
    }

    Seq<Block> getByCategory(Category cat){
        return returnArray.selectFrom(content.blocks(), block -> block.category == cat && block.isVisible() && block.environmentBuildable());
    }

    Seq<Block> getUnlockedByCategory(Category cat){
        return returnArray2.selectFrom(content.blocks(), block -> block.category == cat &&(AdvanceToolTable.allBlocksReveal||block.isVisible() && unlocked(block)) ).sort((b1, b2) -> Boolean.compare(!b1.isPlaceable(), !b2.isPlaceable()));
    }

    Block getSelectedBlock(Category cat){
        return selectedBlocks.get(cat, () -> getByCategory(cat).find(this::unlocked));
    }

    boolean unlocked(Block block){
        return block.unlockedNow() && block.placeablePlayer && block.environmentBuildable() &&
            block.supportsEnv(state.rules.env); //TODO this hides env unsupported blocks, not always a good thing
    }

    boolean hasInfoBox(){
        hover = hovered();
        hover2 = hoveredblock();
        hoverTile = hoveredTile();
        return control.input.block != null || menuHoverBlock != null || hover != null || hoverTile != null;
    }

    /** Returns the thing being hovered over. */
    @Nullable
    Displayable hovered(){
        Vec2 v = topTable.stageToLocalCoordinates(Core.input.mouse());

        //if the mouse intersects the table or the UI has the mouse, no hovering can occur
        if(Core.scene.hasMouse() || topTable.hit(v.x, v.y, false) != null) return null;

        //check for a unit
        Unit unit = Units.closestOverlap(player.team(), Core.input.mouseWorldX(), Core.input.mouseWorldY(), 5f, u -> true);
        //if cursor has a unit, display it
        if(unit != null) return unit;
        unit = Units.closestEnemy(player.team(), Core.input.mouseWorldX(), Core.input.mouseWorldY(), 5f, u -> true);
        if(unit != null) return unit;
        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile != null){
            //if the tile has a building, display it
            if(hoverTile.build != null && hoverTile.build.displayable()  && !hoverTile.build.inFogTo(player.team())){
                return nextFlowBuild = hoverTile.build;
            }
        }

        return null;


    }
    @Nullable
    Displayable hoveredblock(){
        Vec2 v = topTable.stageToLocalCoordinates(Core.input.mouse());

        //if the mouse intersects the table or the UI has the mouse, no hovering can occur
        if(Core.scene.hasMouse() || topTable.hit(v.x, v.y, false) != null) return null;

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile != null){
            //if the tile has a building, display it
            if(hoverTile.build != null && hoverTile.build.displayable() && !hoverTile.build.inFogTo(player.team())){
                return nextFlowBuild = hoverTile.build;
            }
        }

        return null;
    }

    @Nullable
    Tile hoveredTile(){
        Vec2 v = topTable.stageToLocalCoordinates(Core.input.mouse());

        //if the mouse intersects the table or the UI has the mouse, no hovering can occur
        if(Core.scene.hasMouse() || topTable.hit(v.x, v.y, false) != null) return null;

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile != null && !(hoverTile.build!=null && hoverTile.build.inFogTo(player.team()))) return hoverTile;
        return null;
    }

    void arcSelectUnits(Table table, String icon, String info, Boolf<Unit> cons){
        float size = 40f;

        table.button(icon, Styles.cleart, () -> {}).tooltip(info).size(size).with(b->{
            var listener = new ClickListener();
            b.clicked(KeyCode.mouseLeft, () -> {
                control.input.selectedUnits = control.input.selectedUnits.select(cons::get);
                Events.fire(Trigger.unitCommandChange);
                arcui.arcInfo("[cyan]arc控制器\n[acid]选择" + info + "！");
            });
            //right click -> remove
            b.clicked(KeyCode.mouseRight, () -> {
                control.input.selectedUnits.removeAll(cons::get);
                Events.fire(Trigger.unitCommandChange);
                arcui.arcInfo("[cyan]arc控制器\n[orange]移除" + info + "！");
            });

            b.addListener(listener);
            b.addListener(new HandCursorListener());
        });
    }
}
