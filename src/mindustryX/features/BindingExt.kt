package mindustryX.features

import arc.Core
import arc.input.KeyBind
import arc.input.KeyBind.KeybindValue
import arc.input.KeyCode
import mindustry.Vars
import mindustry.input.Binding
import mindustryX.features.ui.OverlayUI

@Suppress("EnumEntryName")
enum class BindingExt(val default: KeybindValue = KeyCode.unset, val category: String? = null, val onTap: (() -> Unit)? = null) {
    //ARC
    superUnitEffect(KeyCode.o, "ARC", onTap = { Settings.cycle("superUnitEffect", 3) }),
    showRTSAi(KeyCode.l, onTap = { Settings.toggle("alwaysShowUnitRTSAi") }),
    arcDetail(KeyCode.unset),
    arcScanMode(KeyCode.unset, onTap = { RenderExt.transportScan.toggle() }),
    oreAdsorption(KeyCode.unset),

    //MDTX
    toggle_unit(KeyCode.unset, "mindustryX", onTap = { RenderExt.unitHide.toggle() }),
    point(KeyCode.j, onTap = MarkerType::showPanUI),
    lockonLastMark(KeyCode.unset, onTap = MarkerType::lockOnLastMark),
    toggle_block_render(KeyCode.unset, onTap = { RenderExt.blockRenderLevel0.cycle() }),
    focusLogicController(KeyCode.unset, onTap = { mindustryX.features.func.focusLogicController() }),
    placeRouterReplacement(KeyCode.shiftLeft),
    overlayUI(KeyCode.z, onTap = onTap@{
        if (Core.input.keyTap(Binding.schematicFlipX) && !Vars.control.input.selectPlans.isEmpty)
            return@onTap // avoid conflict with schematic flip
        if (!Core.input.ctrl()) OverlayUI.toggle()
    }),
    ;

    private val bind: KeyBind = KeyBind.add(name, default, category)

    fun keyTap() = Core.input.keyTap(bind)
    fun keyDown() = Core.input.keyDown(bind)

    companion object {
        @JvmStatic
        fun init() {
        }

        @JvmStatic
        fun pollKeys() {
            if (Vars.headless || Core.scene.hasField()) return
            BindingExt.entries.forEach {
                val onTap = it.onTap ?: return@forEach
                if (it.keyTap()) onTap()
            }
        }
    }
}