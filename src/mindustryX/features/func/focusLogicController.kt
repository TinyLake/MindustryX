@file:JvmName("FuncX")
@file:JvmMultifileClass

package mindustryX.features.func

import arc.util.Tmp
import mindustry.Vars
import mindustry.ai.types.LogicAI
import mindustry.gen.Building
import mindustry.gen.Unit
import mindustryX.features.MarkerType

fun focusLogicController() {
    if (Vars.world.height() == 0) return//empty
    val hovered = Vars.ui.hudfrag.blockfrag.hovered()
    val logic = (hovered as? Unit)?.let { (it.controller() as? LogicAI)?.controller }
        ?: (hovered as? Building)?.lastLogicController
        ?: return
    Vars.control.input.panCamera(Tmp.v1.set(logic))
    MarkerType.mark.at(logic)
}