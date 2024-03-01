package mindustryX.features.ui

import arc.scene.event.Touchable
import arc.scene.ui.layout.Table
import arc.scene.ui.layout.WidgetGroup

class SimpleCollapser(val table: Table = Table(), var collapsed: Boolean = true) : WidgetGroup(table) {
    init {
        this.touchable = if (collapsed) Touchable.disabled else Touchable.enabled
    }

    fun toggle() {
        collapsed = !collapsed
        this.touchable = if (collapsed) Touchable.disabled else Touchable.enabled
        this.visible = !collapsed
        invalidateHierarchy()
    }

    override fun draw() {
        if (collapsed) return
        super.draw()
    }

    override fun layout() {
        table.setBounds(0f, 0f, width, height)
    }

    override fun getPrefWidth(): Float = if (collapsed) 0f else table.prefWidth
    override fun getPrefHeight(): Float = if (collapsed) 0f else table.prefHeight
    override fun getMaxWidth(): Float = 0f
    override fun getMinHeight(): Float = 0f

    inline fun table(block: (Table).() -> Unit): SimpleCollapser {
        table.block()
        return this
    }
}