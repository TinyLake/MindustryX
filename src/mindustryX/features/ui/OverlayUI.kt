package mindustryX.features.ui

import arc.Core
import arc.Graphics.Cursor.SystemCursor
import arc.func.Boolp
import arc.func.Prov
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Lines
import arc.input.KeyCode
import arc.math.geom.Rect
import arc.math.geom.Vec2
import arc.scene.Element
import arc.scene.event.InputEvent
import arc.scene.event.InputListener
import arc.scene.event.Touchable
import arc.scene.ui.ImageButton
import arc.scene.ui.ImageButton.ImageButtonStyle
import arc.scene.ui.TextButton
import arc.scene.ui.layout.Scl
import arc.scene.ui.layout.Table
import arc.scene.ui.layout.WidgetGroup
import arc.util.Align
import arc.util.Tmp
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.gen.Tex
import mindustry.ui.Styles
import mindustryX.features.SettingsV2
import mindustryX.features.SettingsV2.CheckPref
import mindustryX.features.SettingsV2.PersistentProvider
import mindustryX.features.UIExtKt
import mindustryX.features.ui.AdsorptionSystem.Axis
import kotlin.math.roundToInt

object OverlayUI {
    data class WindowData(
        val enabled: Boolean = false,
        val pinned: Boolean = false,
        @Deprecated("use center,size,constraintX,constraintY replaced")
        val rect: Rect? = null,
        val center: Vec2? = null,
        val size: Vec2? = null,
        val constraintX: AdsorptionSystem.Constraint? = null,
        val constraintY: AdsorptionSystem.Constraint? = null,
    )

    class WindowSetting(name: String) : SettingsV2.Data<WindowData>(name, WindowData()) {
        init {
            persistentProvider = PersistentProvider.AsUBJson(
                PersistentProvider.Arc(name),
                WindowData::class.java
            )
        }

        override fun buildUI(table: Table) {
            table.table().fillX().get().apply {
                image(Icon.list).padRight(4f)
                add(title).width(148f).padRight(8f)

                val myToggleI = ImageButtonStyle(Styles.clearNonei).apply {
                    imageUpColor = Color.darkGray
                    imageCheckedColor = Color.white
                }
                button(Icon.eyeSmall, myToggleI, Vars.iconSmall) {
                    set(value.copy(enabled = !value.enabled))
                }.padRight(4f).checked { value.enabled }
                button(Icon.lockSmall, myToggleI, Vars.iconSmall) {
                    set(value.copy(pinned = !value.pinned))
                }.padRight(4f).checked { value.pinned }
                val builder = StringBuilder()
                label {
                    builder.clear()
                    val center = value.center ?: return@label "[grey][UNUSED]"
                    builder.append("[${center.x.roundToInt()},${center.y.roundToInt()}]")
                    value.size?.let {
                        builder.append("[${it.x.roundToInt()}x${it.y.roundToInt()}]")
                    }
                    builder
                }

                add().growX()
                addTools()
            }
            table.row()
            value.constraintX?.let {
                table.label {
                    "X: ${it.type.name} to [${it.target}]"
                }.padLeft(64f).left().row()
            }
            value.constraintY?.let {
                table.label {
                    "Y: ${it.type.name} to [${it.target}]"
                }.padLeft(64f).left().row()
            }
        }

        var enabled: Boolean
            get() = value.enabled
            set(v) {
                set(value.copy(enabled = v))
            }

        var pinned: Boolean
            get() = value.pinned
            set(v) {
                set(value.copy(pinned = v))
            }
    }

    class Window(name: String, val table: Table) : Table() {
        inner class DragListener : InputListener() {
            private val offset = Vec2()
            override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean {
                if (Core.app.isMobile && pointer != 0) return false
                offset.set(event.stageX, event.stageY).sub(this@Window.x, this@Window.y)
                state = State.Dragging

                toFront()
                return true
            }

            override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
                if (Core.app.isMobile && pointer != 0) return
                setPosition(event.stageX - offset.x, event.stageY - offset.y)
                keepInStage()
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?) {
                if (Core.app.isMobile && pointer != 0) return
                state = State.EndDrag
            }
        }

        inner class ResizeListener : InputListener() {
            private val last = Vec2()
            private var resizeSide: Int = 0

            override fun mouseMoved(event: InputEvent, x: Float, y: Float): Boolean {
                resizeSide = when {
                    event.targetActor != this@Window -> 0
                    x < table.getX(Align.left) -> Align.left
                    x > table.getX(Align.right) -> Align.right
                    y < table.getY(Align.bottom) -> Align.bottom
                    y > table.getY(Align.top) -> Align.top
                    else -> 0
                }
                if (Align.isLeft(resizeSide) || Align.isRight(resizeSide)) {
                    Core.graphics.cursor(SystemCursor.horizontalResize)
                } else if (Align.isTop(resizeSide) || Align.isBottom(resizeSide)) {
                    Core.graphics.cursor(SystemCursor.verticalResize)
                } else {
                    Core.graphics.restoreCursor()
                    return false
                }
                return true
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Element?) {
                if (Core.app.isMobile && pointer != 0) return
                Core.graphics.restoreCursor()
            }

            override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: KeyCode): Boolean {
                if (Core.app.isMobile && pointer != 0) return false
                mouseMoved(event, x, y)
                if (event.targetActor != this@Window || resizeSide == 0) return false
                last.set(event.stageX, event.stageY)
                toFront()
                return true
            }

            override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
                if (Core.app.isMobile && pointer != 0) return
                val delta = Tmp.v1.set(event.stageX, event.stageY).sub(last)
                last.set(event.stageX, event.stageY)
                dragResize(resizeSide, delta)
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?) {
                if (Core.app.isMobile && pointer != 0) return
                endResize()
            }
        }

        inner class FixedResizeListener(val align: Int) : InputListener() {
            private val last = Vec2()

            override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: KeyCode): Boolean {
                if (Core.app.isMobile && pointer != 0) return false
                mouseMoved(event, x, y)
                if (event.targetActor != event.listenerActor) return false
                last.set(event.stageX, event.stageY)
                toFront()
                return true
            }

            override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
                if (Core.app.isMobile && pointer != 0) return
                val delta = Tmp.v1.set(event.stageX, event.stageY).sub(last)
                last.set(event.stageX, event.stageY)
                dragResize(align, delta)
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?) {
                if (Core.app.isMobile && pointer != 0) return
                endResize()
            }
        }

        enum class State {
            Stable, Dragging, EndDrag
        }

        val data = WindowSetting("overlayUI.$name")
        private val paneBg = Tex.pane
        private var state = State.Stable

        var availability: Prov<Boolean> = Prov { true }
        val settings = mutableListOf<SettingsV2.Data<*>>(data)
        private val adsorption = AdsorptionSystem.Element(name)

        init {
            this.name = name
        }

        override fun updateVisibility() {
            visible = data.enabled && availability.get() && (open || data.value.pinned)
            if (!visible) adsorption.remove()
        }

        override fun act(delta: Float) {
            super.act(delta)
            if (!data.enabled) return

            updateData()
            if (data.changed()) {
                rebuild()
                data.changed()//ignore change by rebuild
            }

            width = width.coerceAtMost(Core.scene.width)
            height = height.coerceAtMost(Core.scene.height)
            if (state == State.Stable) {
                data.value.center?.let { setPosition(it.x, it.y, Align.center) }
            }
            keepInStage()

            //Sync AdsorptionSystem
            adsorption.apply {
                reset(x, y, width, height)
                if (state == State.Stable) {
                    data.value.constraintX?.let { applyConstraint(it) }
                    data.value.constraintY?.let { applyConstraint(it) }
                } else {
                    val (constraintX, constraintY) = findBestConstraints()
                    constraintX?.let {
                        applyConstraint(it)
                        constraintDrawTask.add(it)
                    }
                    constraintY?.let {
                        applyConstraint(it)
                        constraintDrawTask.add(it)
                    }
                }
                setPosition(rect.x, rect.y)
            }

            //Save Drag Result
            if (state == State.EndDrag) {
                state = State.Stable
                val center = Vec2(table.getX(Align.center), table.getY(Align.center))
                localToParentCoordinates(center)
                val (constraintX, constraintY) = adsorption.findBestConstraints()
                data.set(data.value.copy(center = center, constraintX = constraintX, constraintY = constraintY))
            }
        }

        private fun updateData() {
            @Suppress("DEPRECATION")
            data.value.rect?.let { old ->
                val center = old.getCenter(Vec2())
                val size = old.getSize(Vec2())
                data.set(data.value.copy(center = center, size = size, rect = null))
                Align.left
            }
            if (data.value.center == null)
                data.set(data.value.copy(center = Vec2(parent.width / 2, parent.height / 2)))
        }

        fun rebuild() {
            clear()
            if (open) {
                //编辑模式
                background = paneBg
                touchable = Touchable.enabled
                addListener(ResizeListener())

                table { header ->
                    //Later set text, so preferredSize is not affected
                    header.add("").update { it.setText(data.title); it.update(null) }
                        .ellipsis(true).minWidth(0f).growX().labelAlign(Align.left)

                    header.touchable = Touchable.enabled
                    header.addListener(DragListener())

                    header.defaults().size(Vars.iconMed).pad(2f)
                    header.button(Icon.settingsSmall, Styles.cleari) {
                        UIExtKt.showFloatSettingsPanel {
                            defaults().minWidth(120f).pad(4f)
                            settings.forEach { setting ->
                                setting.buildUI(this)
                            }
                        }
                    }
                    header.button(Icon.lockOpenSmall, ImageButtonStyle(Styles.cleari).apply {
                        up = null
                        imageChecked = Icon.lockSmall
                    }) { data.pinned = !data.pinned }.checked { data.pinned }
                    header.button(Icon.cancelSmall, Styles.cleari) {
                        data.enabled = false
                    }
                }.fillX().row()

                //Set window position and size
                val cell = add(table)
                data.value.size?.let {
                    cell.maxSize(it.x, it.y)
                }
                pack()

                //allow for 'grow', 'grow' may update table. So keep window size, and layout again
                cell.grow().maxSize(Float.NEGATIVE_INFINITY)
                layout()

                addChild(object : Element() {
                    override fun act(delta: Float) {
                        touchable = Touchable.disabled
                        setBounds(table.x, table.y, table.width, table.height)
                    }

                    override fun draw() {
                        Draw.color()
                        Lines.rect(x, y, width, height)
                    }
                })

                addChild(ImageButton(Icon.resize).apply {
                    setSize(Vars.iconMed)
                    addListener(FixedResizeListener(Align.left or Align.bottom))
                })
            } else {
                //预览模式, 作为Group使用
                background = null
                touchable = Touchable.childrenOnly
                add(table).grow()
                data.value.size?.let { setSize(it.x, it.y) }
            }
        }

        fun dragResize(side: Int, delta: Vec2) {
            //消除不相关方向偏置
            if (Align.isCenterHorizontal(side)) delta.x = 0f
            if (Align.isCenterVertical(side)) delta.y = 0f
            //delta 将delta转换为尺寸增量
            if (Align.isLeft(side)) delta.x = -delta.x
            if (Align.isBottom(side)) delta.y = -delta.y
            //clamp delta变化
            if (width + delta.x < minWidth) delta.x = minWidth - width
            if (maxWidth > 0 && width + delta.x > maxWidth) delta.x = maxWidth - width
            if (height + delta.y < minHeight) delta.y = minHeight - height
            if (maxHeight > 0 && height + delta.y > maxHeight) delta.y = maxHeight - height
            //应用delta
            if (Align.isLeft(side)) this@Window.x -= delta.x
            if (Align.isBottom(side)) this@Window.y -= delta.y
            setSize(width + delta.x, height + delta.y)
        }

        fun endResize() {
            if (parent == null) return
            validate()
            data.set(data.value.copy(size = Vec2(table.width / Scl.scl(), table.height / Scl.scl())))
        }
    }

    private val showOverlayButton: CheckPref = CheckPref("gameUI.overlayButton", true)
    var open = false
        private set
    val windows: List<Window>
        get() = group.children.filterIsInstance<Window>()
    private val constraintDrawTask = mutableListOf<AdsorptionSystem.Constraint>()

    private val group = WidgetGroup().apply {
        name = "overlayUI"
        setFillParent(true)
        touchable = Touchable.childrenOnly
        zIndex = 99

        fill(Styles.black6) { t ->
            t.name = "overlayUI-bg"
            t.touchable = Touchable.enabled
            t.visibility = Boolp { open }
            t.bottom()
            t.defaults().size(Vars.iconLarge).width(Vars.iconLarge * 1.5f).pad(4f)
            t.button(Icon.add) {
                UIExtKt.showFloatSettingsPanel {
                    add("添加面板").color(Color.gold).align(Align.center).row()
                    defaults().minWidth(120f).fillX().pad(4f)
                    val notAvailable = mutableListOf<Window>()
                    windows.forEach {
                        if (!it.availability.get()) {
                            notAvailable.add(it)
                            return@forEach
                        }
                        add(TextButton(it.data.title).apply {
                            label.setWrap(false)
                            setDisabled { it.data.enabled }
                            changed { it.data.enabled = true }
                        }).row()
                    }
                    if (notAvailable.isNotEmpty()) {
                        add("当前不可用的面板:").align(Align.center).row()
                        notAvailable.forEach {
                            add(TextButton(it.data.title).apply {
                                label.setWrap(false)
                                isDisabled = true
                            }).row()
                        }
                    }
                }
            }
            t.button(Icon.exit) { toggle() }
        }
        fill { t ->
            t.name = "overlayUI-tips"
            t.touchable = Touchable.disabled
            t.visibility = Boolp { open }
            t.left().top()
            t.add("@overlayUI.tips").pad(8f)
        }

        fill { t ->
            t.left().name = "toggle"
            t.button(Icon.settings, Vars.iconMed) { toggle() }
            t.visible { showOverlayButton.value }
        }

        fill { _, _, _, _ ->
            Draw.color(Color.red)
            Lines.stroke(4f * Scl.scl())
            constraintDrawTask.forEach { c ->
                c.targetPoint?.rect?.let { Lines.rect(it) }
            }

            Draw.color(Color.yellow)
            Lines.stroke(2f * Scl.scl())
            constraintDrawTask.forEach { c ->
                val target = c.targetPoint ?: return@forEach
                val tar = target.computeAnchor(c.axis, c.type.targetAnchor)
                if (c.axis == Axis.X) {
                    Lines.dashLine(tar, 0f, tar, Core.scene.height, 64)
                } else {
                    Lines.dashLine(0f, tar, Core.scene.width, tar, 64)
                }
            }
            Draw.reset()

            constraintDrawTask.clear()
        }.apply {
            name = "draw-Constant"
            update { toFront() }
        }
    }

    fun registerWindow(name: String, table: Table): Window {
        val window = Window(name, table)
        group.addChild(window)
        return window
    }

    fun init() {
        Core.scene.add(group)
    }

    fun toggle() {
        open = !open
        group.children.filterIsInstance<Window>().forEach {
            it.updateVisibility()
            if (it.visible) it.rebuild()
        }
    }
}