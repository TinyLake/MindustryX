package mindustryX.features

import arc.Events
import arc.scene.event.Touchable
import arc.scene.ui.layout.Table
import arc.util.Align
import mindustry.Vars
import mindustry.core.PerfCounter
import mindustry.game.EventType
import mindustry.gen.Tex
import mindustry.ui.Styles
import mindustryX.features.func.exportBlockData

object DebugUtil {
    @JvmField
    var renderDebug: Boolean = false

    @JvmField
    var lastDrawRequests: Int = 0

    @JvmField
    var lastVertices: Int = 0

    @JvmField
    var lastFlushCount: Int = 0

    @JvmField
    var lastSwitchTexture: Int = 0

    @JvmField
    var logicTime: Long = 0

    @JvmField
    var rendererTime: Long = 0

    @JvmField
    var uiTime: Long = 0 //nanos

    @JvmStatic
    fun init() {
        Events.run(EventType.Trigger.preDraw) {
            rendererTime = PerfCounter.render.rawValueNs()
            uiTime = PerfCounter.ui.rawValueNs()
        }
    }

    @JvmStatic
    fun metricTable(): Table = Table(Tex.pane).apply {
        left()
        label { "Draw: $lastDrawRequests" }.fillX().labelAlign(Align.left).touchable(Touchable.disabled).row()
        label { "Vertices: $lastVertices" }.fillX().labelAlign(Align.left).touchable(Touchable.disabled).row()
        label { "Texture: $lastSwitchTexture" }.fillX().labelAlign(Align.left).touchable(Touchable.disabled).row()
        label { "Flush: $lastFlushCount" }.fillX().labelAlign(Align.left).touchable(Touchable.disabled).row()
        image().update { DebugUtil.reset() }.row()
        table { t ->
            t.left().defaults().size(32f).pad(4f)
            t.button("D", Styles.logicTogglet) { renderDebug = !renderDebug }.checked { renderDebug }.tooltip("Render Debug")
            if (!Vars.mobile)
                t.button("M", Styles.logicTogglet) { Vars.mobile = !Vars.mobile }.checked { Vars.mobile }.tooltip("Mock Mobile")
            t.button("E", Styles.cleart) { exportBlockData() }.tooltip("Export Block Data")
        }.fillX()
    }

    @JvmStatic
    fun reset() {
        lastSwitchTexture = 0
        lastFlushCount = 0
        lastVertices = 0
        lastDrawRequests = 0
    }
}
