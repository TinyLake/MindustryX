package mindustryX.features

import arc.Core
import arc.KeyBinds.KeyBind
import arc.KeyBinds.KeybindValue
import arc.input.InputDevice
import arc.input.KeyCode
import arc.util.Reflect
import mindustry.Vars

@Suppress("EnumEntryName")
enum class BindingExt(val default: KeybindValue = KeyCode.unset, val category: String? = null, val onTap: (() -> Unit)? = null) {
    //ARC
    superUnitEffect(KeyCode.o, "ARC", onTap = { Settings.cycle("superUnitEffect", 3) }),
    showRTSAi(KeyCode.l, onTap = { Settings.toggle("alwaysShowUnitRTSAi") }),
    arcDetail(KeyCode.unset),
    arcScanMode(KeyCode.unset, onTap = { ArcScanMode.enabled = !ArcScanMode.enabled }),
    oreAdsorption(KeyCode.unset),

    //MDTX
    toggle_unit(KeyCode.unset, "mindustryX", onTap = { RenderExt.unitHide = !RenderExt.unitHide }),
    point(KeyCode.j, onTap = MarkerType::showPanUI),
    lockonLastMark(KeyCode.unset, onTap = MarkerType::lockOnLastMark),
    toggle_block_render(KeyCode.unset, onTap = { Settings.cycle("blockRenderLevel", 3) }),
    focusLogicController(KeyCode.unset, onTap = { mindustryX.features.func.focusLogicController() }),
    ;

    //KT-14115 can't implement KeyBind directly
    private val bind: KeyBind = object : KeyBind {
        override fun name(): String = name
        override fun category(): String? = category
        override fun defaultValue(p0: InputDevice.DeviceType?): KeybindValue = default
    }

    fun keyTap() = Core.input.keyTap(bind)
    fun keyDown() = Core.input.keyDown(bind)

    companion object {
        @JvmStatic
        fun init() {
            Core.keybinds.apply {
                setDefaults(arrayOf<KeyBind>(*keybinds, *entries.map { it.bind }.toTypedArray()))
                Reflect.invoke(this,"load")
            }
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