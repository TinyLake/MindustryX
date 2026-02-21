package mindustryX.features.ui.comp

import arc.Core
import arc.input.KeyCode
import arc.math.geom.Vec2
import arc.scene.Element
import arc.scene.event.InputEvent
import arc.scene.event.InputListener
import arc.scene.event.Touchable
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import arc.util.Tmp

class DragSortListener(val swap: (Element, Element) -> Unit = ::swapCell) : InputListener() {
    private val begin = Vec2()
    override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean {
        if (button != KeyCode.mouseLeft || pointer != 0) return false
        event.listenerActor.apply {
            begin.set(x, y).also { localToParentCoordinates(it) }
            toFront()
            touchable = Touchable.disabled
        }
        return true
    }

    override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
        event.listenerActor.apply {
            Tmp.v1.set(x, y).also { localToParentCoordinates(it) }
            translation.set(Tmp.v1.sub(begin))
        }
    }

    override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: KeyCode?) {
        if (button != KeyCode.mouseLeft || pointer != 0) return
        event.listenerActor.apply {
            translation.setZero()
            touchable = Touchable.enabled

            hitAnother(event.stageX, event.stageY)?.let { swap(this, it) }
        }
    }

    companion object {
        fun hitAnother(x: Float, y: Float): Element? {
            var element = Core.scene.hit(x, y, true)
            while (element != null) {
                if (element.listeners.any { it is DragSortListener }) return element
                element = element.parent
            }
            return null
        }

        private fun getCell(elem: Element): Cell<*>? {
            return (elem.parent as? Table)?.getCell(elem)
        }

        fun swapCell(a: Element, b: Element) {
            if (a == b) return
            val cellA = getCell(a) ?: return
            val cellB = getCell(b) ?: return

            a.remove(); b.remove()
            cellA.setElement(b); cellB.setElement(a)
        }
    }
}
