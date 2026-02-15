package mindustryX.features

import arc.Events
import arc.scene.event.Touchable
import arc.scene.ui.layout.Table
import arc.util.Align
import mindustry.Vars
import mindustry.core.PerfCounter
import mindustry.game.EventType
import mindustry.gen.*
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog
import mindustryX.features.func.exportBlockData
import mindustryX.features.ui.Format

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

    @JvmField
    var traceUpdate = false

    @JvmStatic
    fun init() {
        Events.run(EventType.Trigger.preDraw) {
            rendererTime = PerfCounter.render.rawValueNs()
            uiTime = PerfCounter.ui.rawValueNs()
        }
    }

    @JvmStatic
    fun metricTable(): Table = Table(Tex.pane).apply {
        add(object : Table() {
            init {
                label { "Draw: $lastDrawRequests" }.fillX().labelAlign(Align.left).touchable(Touchable.disabled).row()
                label { "Vertices: $lastVertices" }.fillX().labelAlign(Align.left).touchable(Touchable.disabled).row()
                label { "Texture: $lastSwitchTexture" }.fillX().labelAlign(Align.left).touchable(Touchable.disabled).row()
                label { "Flush: $lastFlushCount" }.fillX().labelAlign(Align.left).touchable(Touchable.disabled).row()
            }

            var timer = 0f
            override fun act(delta: Float) {
                timer += delta
                //减少刷新频率
                if (timer > 0.5f) {
                    super.act(delta)
                    timer = 0f
                }
                DebugUtil.reset()
            }
        }).row()
        table { t ->
            t.left().defaults().size(32f).pad(4f)
            t.button("D", Styles.logicTogglet) { renderDebug = !renderDebug }.checked { renderDebug }.tooltip("Render Debug")
            if (!Vars.mobile)
                t.button("M", Styles.logicTogglet) { Vars.mobile = !Vars.mobile }.checked { Vars.mobile }.tooltip("Mock Mobile")
            t.button("E", Styles.cleart) { exportBlockData() }.tooltip("Export Block Data")
            t.button("T", Styles.cleart) {
                traceSamples.clear()
                traceCount = traceTimes
                traceUpdate = true
            }.tooltip("Trace Update")
        }.fillX()
    }

    @JvmStatic
    fun reset() {
        lastSwitchTexture = 0
        lastFlushCount = 0
        lastVertices = 0
        lastDrawRequests = 0
    }

    data class TraceSample(val type: String, val obj: Any?, val time: Long)
    data class TraceSummary(val type: String, val obj: Any?, val count: Int, val sum: Long, val min: Long, val max: Long) {
        constructor(type: String, obj: Any?, samples: List<Long>) : this(type, obj, samples.size, samples.sum(), samples.minOrNull() ?: 0L, samples.maxOrNull() ?: 0L)
    }

    private const val traceTimes = 30
    private var traceCount = 0
    private val traceSamples = mutableListOf<TraceSample>()

    @JvmStatic
    fun logTrace(type: String, obj: Any?, time: Long) {
        traceSamples.add(TraceSample(type, obj, time))
    }

    @JvmStatic
    fun traceEnd() {
        if (traceCount > 0) {
            traceCount--
            return
        }
        traceUpdate = false
        val summary = traceSamples.groupBy { it.obj }.map { (_, list) ->
            TraceSummary(list[0].type, list[0].obj, list.map { it.time })
        }.sortedByDescending { it.sum }
        BaseDialog("Trace Result").apply {
            val sum = summary.sumOf { it.sum }
            val knownTypes = summary.groupBy {
                when (it.obj) {
                    is Building -> "Building"
                    is Unitc -> "Unit"
                    is Bulletc -> "Bullet"
                    is PowerGraphUpdaterc -> "PowerGraphUpdater"
                    else -> "Unknown"
                }
            }.map { (type, list) ->
                TraceSummary(list[0].type, type, list.map { it.sum })
            }.sortedByDescending { it.sum }
            cont.add("Total: ${summary.size} objects, costs: ${Format.default.format(sum / 1e6f / traceTimes)}ms").pad(4f).row()
            cont.table {
                it.left().defaults().pad(4f)
                knownTypes.forEach { info ->
                    it.add(info.obj.toString()).left().minWidth(100f)
                    it.add("x${info.count}").left().minWidth(50f)
                    it.add("${info.sum / traceTimes / 1000} μs/tick").right().minWidth(100f)
                    it.add("${"%.2f".format(info.sum * 100f / sum)}%").right().minWidth(80f)
                    it.row()
                }
            }.row()
            //详情
            cont.image().fillX().row()
            cont.pane { t ->
                t.left().defaults().pad(4f)
                summary.slice(0..(summary.size.coerceAtMost(50))).forEach { info ->
                    val name = when (val obj = info.obj) {
                        is Building -> "${obj.block.localizedName} ${Format.default.formatTile(obj)}"
                        is Unitc -> "${obj.type().localizedName} ${Format.default.formatTile(obj)}"
                        else -> obj.toString()
                    }
                    t.add(info.type).left().minWidth(100f)
                    t.add(name).left().minWidth(200f)
                    t.add("MIN ${info.min / 1000} μs").right().minWidth(100f)
                    t.add("AVG ${info.sum / info.count} μs").right().minWidth(100f)
                    t.add("MAX ${info.max} μs").right().minWidth(100f)
                    t.add("${"%.2f".format(info.sum * 100f / sum)}%").right().minWidth(80f)
                    t.row()
                }
            }
            addCloseButton()
        }.show()
    }
}
