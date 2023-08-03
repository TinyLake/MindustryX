package mindustryX.events;

import mindustry.logic.*;
import mindustry.world.blocks.logic.LogicBlock.*;
import mindustryX.*;

/**
 * Fired after LAssembler finish assemble and about to load to LExecutor
 * Allow mods to modify LAssembler, try parse InvalidStatement again.
 * Not fired when LogicBlock load empty code.
 */
@MindustryXApi
public class LogicAssembledEvent{
    public LogicBuild build;
    public LAssembler assembler;

    public LogicAssembledEvent(LogicBuild build, LAssembler assembler){
        this.build = build;
        this.assembler = assembler;
    }
}
