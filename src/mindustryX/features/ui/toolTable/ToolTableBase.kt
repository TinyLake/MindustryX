package mindustryX.features.ui.toolTable

import arc.Core
import arc.graphics.Color
import arc.scene.ui.TextButton
import arc.scene.ui.layout.Table
import mindustry.Vars
import mindustry.ui.Styles

abstract class ToolTableBase(var icon: String) : Table() {
    var expand: Boolean = false

    @JvmField
    protected var maxHeight: Float = 0f

    init {
        background = Styles.black6
    }

    fun wrapped(): Table {
        return Table { t: Table ->
            var main: Table = this
            if (maxHeight > 0) {
                main = Table()
                val pane = main.pane(this).maxSize(800f, maxHeight).get()
                pane.update {
                    val e = Core.scene.hit(Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat(), true)
                    if (e != null && e.isDescendantOf(pane)) {
                        pane.requestScroll()
                    } else if (pane.hasScroll()) {
                        Core.scene.setScrollFocus(null)
                    }
                }
            }
            t.collapser(main) { expand }
            t.button(icon, Styles.flatBordert) { expand = !expand }.width(Vars.iconMed).minHeight(Vars.iconMed).fillY()
                .update { i: TextButton -> i.label.setColor(if (expand) Color.white else Color.lightGray) }
        }
    }
}
