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
    class TraceObject(val samples: List<TraceSample>) {
        val type = samples[0].type
        val obj = samples[0].obj
        val tree: List<String> = when (obj) {
            is Building -> listOf(
                type, "Building", obj.block.category.toString(), obj.block.localizedName,
                "${obj.block.localizedName} ${Format.default.formatTile(obj)}",
            )

            is Unitc -> listOf(
                type, "Unit", obj.type().toString(), obj.type().localizedName,
                "${obj.type().localizedName} ${Format.default.formatTile(obj)}",
            )

            //Bullet可能已经被reset了，无法获取更多信息了
            is Bulletc -> listOf(type, "Bullet", obj.toString())
            is PowerGraphUpdaterc -> listOf(type, "PowerGraphUpdater", obj.toString())
            else -> listOf(type, "Unknown", obj.toString())
        }
        val name get() = tree.last()
        val avg get() = samples.sumOf { it.time } / samples.size
        val min get() = samples.minOf { it.time }
        val max get() = samples.maxOf { it.time }

        override fun toString(): String {
            return "TraceObject(tree=$tree, avg=$avg)"
        }
    }

    class TraceNode(val prefix: List<String>, objects: List<TraceObject>) {
        val name = prefix.lastOrNull() ?: "Root"
        val objectsCount = objects.size
        val timeSum = objects.sumOf { it.avg }
        val isLeaf = objects[0].tree.size <= prefix.size + 1
        val children: List<TraceNode> = if (!isLeaf) objects.groupBy { it.tree[prefix.size] }.map { (name, list) ->
            TraceNode(prefix + name, list)
        } else emptyList()
        val objects: List<TraceObject> = if (isLeaf) objects else emptyList()

        override fun toString(): String {
            return "TraceNode(name='$name', objectsCount=$objectsCount, timeSum=$timeSum)"
        }

        fun buildChildren(): Table = Table().apply {
            left().defaults().left().pad(4f)
            this@TraceNode.children.forEach { c ->
                var expanded = false
                button({
                    it.label { if (expanded) "-" else "+" }
                }) {
                    expanded = !expanded
                }.size(Vars.iconMed)
                add(c.name).expandX()
                add("x${c.objectsCount}").minWidth(60f)
                add("${c.timeSum / 1000} μs").labelAlign(Align.right).minWidth(100f)
                add("${"%.2f".format(c.timeSum * 100f / timeSum)}%").labelAlign(Align.right).width(120f)
                row()

                add().width(24f)//pad
                collapser(c.buildChildren()) { expanded }.colspan(columns - 1).growX()
                row()
            }
            objects.forEach { obj ->
                add(obj.name).left().minWidth(200f).expandX()
                add("MIN ${obj.min}ns").labelAlign(Align.right).minWidth(100f)
                add("AVG ${obj.avg}ns").labelAlign(Align.right).minWidth(100f)
                add("MAX ${obj.max}ns").labelAlign(Align.right).minWidth(100f)
                add("${"%.2f".format(obj.avg * 100f / timeSum)}%").labelAlign(Align.right).width(120f)
                row()
            }
        }
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
        val objects = traceSamples.groupBy { it.obj }.map { TraceObject(it.value) }
        val root = TraceNode(emptyList(), objects)
        BaseDialog("Trace Result").apply {
            cont.add("Total: ${root.objectsCount} objects, ${Format.default.format(root.timeSum / 1e6f)} ms").pad(10f).row()
            cont.pane(root.buildChildren()).expandY()
            addCloseButton()
        }.show()
    }
}
