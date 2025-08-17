package mindustryX.features.ui.comp

import arc.math.Mathf
import arc.scene.Element
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import arc.util.Reflect
import kotlin.math.min

/**自动换行的Table布局
 * - 所有元素使用相同的[cell]，最好设置[cell.minWidth()]
 * - 支持检测子元素[visible]，进行动态增减
 * */
class GridTable : Table() {
    private val elementsTmp = mutableListOf<Element>()
    private val cell: Cell<Element> = defaults()!! //readonly

    override fun act(delta: Float) {
        super.act(delta)
        computeColumns()
    }

    private fun computeColumns() {
        if (!hasChildren()) return
        val children = this.children.asIterable()
        val cellMinWidth = cell.minWidth().takeIf { it > 0 }
            ?: children.firstOrNull { it.visible }?.minWidth
            ?: Float.MAX_VALUE
        val cellWidth = cellMinWidth + Reflect.get<Float>(cell, "padLeft")
        val newColumns = Mathf.floor(width / cellWidth).coerceAtLeast(1)
        val columnsChanged = columns != min(newColumns, children.count { it.visible })
        val visibleChanged = children.count { it.visible } != cells.size || cells.any { it.get()?.visible != true }
        if (!columnsChanged && !visibleChanged) return

        elementsTmp += children //Can't use children.begin, as clearChildren() use it internally.
        clearChildren()
        var i = 0
        elementsTmp.forEach {
            if (!it.visible) addChild(it)
            else {
                add(it).set(cell)
                i++
                if (i % newColumns == 0) row()
            }
        }
        elementsTmp.clear()
    }

    override fun layout() {
        computeColumns()
        super.layout()
    }

    fun firstElement(): Element? {
        return cells.firstOrNull()?.get()
    }

    override fun getMinWidth(): Float {
        return cell.minWidth()
    }

    override fun getPrefWidth(): Float {
        return super.getMaxWidth().takeUnless { it == 0f }
            ?: super.getPrefWidth().takeIf { it >= minWidth }
            ?: (cell.minWidth() * Mathf.ceil(Mathf.sqrt(cells.size * 4 / 3f)))
    }
}