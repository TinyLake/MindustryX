package mindustryX.features

import arc.Core
import arc.func.Cons
import arc.func.Prov
import arc.scene.Element
import arc.scene.event.InputEvent
import arc.scene.event.InputListener
import arc.scene.event.Touchable
import arc.scene.ui.layout.Table
import arc.util.Align
import mindustry.Vars
import mindustry.gen.Tex
import mindustryX.features.ui.LogicSupport
import mindustryX.features.ui.OverlayUI
import mindustryX.features.ui.TeamsStatDisplay
import mindustryX.features.ui.WaveInfoDisplay
import mindustryX.features.ui.toolTable.AdvanceToolTable
import mindustryX.features.ui.toolTable.AuxiliaryTools
import mindustryX.features.ui.toolTable.NewToolTable

object UIExtKt {

    @JvmStatic
    fun init() {
        LogicSupport.init()
        OverlayUI.init()

        val inGameOnly = Prov { Vars.state.isGame }
        OverlayUI.registerWindow("debug", DebugUtil.metricTable())
        OverlayUI.registerWindow("auxiliaryTools", AuxiliaryTools()).availability = inGameOnly
        OverlayUI.registerWindow("quickTool", NewToolTable).apply {
            resizable = true
            availability = inGameOnly
            settings.add(NewToolTable.customButtons)
        }
        OverlayUI.registerWindow("mappingTool", AdvanceToolTable()).availability = inGameOnly
        OverlayUI.registerWindow("advanceBuildTool", UIExt.advanceBuildTool).availability = inGameOnly
        OverlayUI.registerWindow("teamsStats", TeamsStatDisplay().wrapped()).apply {
            availability = inGameOnly
        }
        OverlayUI.registerWindow("coreItems", UIExt.coreItems).apply {
            autoHeight = true
            resizable = true
            availability = inGameOnly
            settings.addAll(UIExt.coreItems.settings)
        }
        OverlayUI.registerWindow("waveInfo", WaveInfoDisplay()).apply {
            resizable = true
            availability = inGameOnly
        }
        OverlayUI.registerWindow("controlGroup", UIExt.controlGroup).apply {
            autoHeight = true
            resizable = true
            availability = inGameOnly
        }
    }

    @JvmStatic
    fun showFloatSettingsPanel(builder: Cons<Table>) = showFloatSettingsPanel { builder.get(this) }

    @JvmStatic
    fun showFloatSettingsPanel(builder: Table.() -> Unit) {
        val mouse = Core.input.mouse().cpy()
        val table = Table(Tex.pane).apply {
            builder.invoke(this)
            button("@close") { this.remove() }.fillX()

            touchable = Touchable.enabled
            addListener(object : InputListener() {
                override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Element?) {
                    if (hit(x, y, false) == null) remove()
                }
            })
        }
        Core.scene.add(table)
        table.update {
            table.pack()
            if (table.width > Core.scene.width * 0.8) table.width = Core.scene.width * 0.8f
            if (table.height > Core.scene.height * 0.8) table.height = Core.scene.height * 0.8f
            table.setPosition(mouse.x, mouse.y, Align.center)
            table.keepInStage()
            Core.scene.setScrollFocus(table)
        }
    }

    fun isVisible(element: Element): Boolean {
        if (element.scene == null) return false
        var current: Element? = element
        while (current != null) {
            if (!current.visible) return false
            current = current.parent
        }
        return true
    }
}