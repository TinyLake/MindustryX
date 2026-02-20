package mindustryX.features.ui

import arc.Core
import arc.Graphics.Cursor.SystemCursor
import arc.func.Boolp
import arc.func.Prov
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Lines
import arc.input.KeyCode
import arc.math.Mathf
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
import arc.scene.ui.layout.Stack
import arc.scene.ui.layout.Table
import arc.scene.ui.layout.WidgetGroup
import arc.util.Align
import arc.util.Log
import arc.util.Strings
import arc.util.Tmp
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.gen.Tex
import mindustry.graphics.Pal
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
        val scale: Float = 1f
    )

    class WindowSetting(name: String) : SettingsV2.Data<WindowData>(name, WindowData()) {
        init {
            persistentProvider = PersistentProvider.AsUBJson(
                PersistentProvider.Arc(name),
                WindowData::class.java
            )
        }

        override fun buildUI() = Table().apply {
            image(Icon.listSmall).color(Color.lightGray).padRight(4f)
            // Prevent long window names from drawing into the position column.
            add(title).width(148f).padRight(8f).ellipsis(true).left()

            val builder = StringBuilder()
            label {
                builder.clear()
                val center = value.center ?: return@label "[grey][UNUSED]"
                builder.append("[${center.x.roundToInt()},${center.y.roundToInt()}]")
                value.size?.let {
                    builder.append("[${it.x.roundToInt()}x${it.y.roundToInt()}]")
                }
                builder
            }.expandX().left()

            val myToggleI = ImageButtonStyle(Styles.clearNonei).apply {
                imageUpColor = Color.white
                imageCheckedColor = Pal.accent
                imageDisabledColor = Color.darkGray
            }
            button(Icon.eyeSmall, myToggleI, Vars.iconSmall) {
                set(value.copy(enabled = !value.enabled))
            }.tooltip(mindustryX.bundles.UiTexts.i("开关")).padRight(4f).checked { value.enabled } // 原文本:开关
            button(Icon.lockSmall, myToggleI, Vars.iconSmall) {
                set(value.copy(pinned = !value.pinned))
            }.tooltip(mindustryX.bundles.UiTexts.i("锁定")).padRight(4f).checked { value.pinned } // 原文本:锁定
            button(Icon.resizeSmall, myToggleI, Vars.iconSmall) {
                UIExtKt.showFloatSettingsPanel {
                    label { mindustryX.bundles.UiTexts.bundle().zoomScale(Strings.fixed(value.scale, 1)) }.center().row() // 原文本:缩放: x
                    slider(0.2f, 3f, 0.1f, value.scale) {
                        set(value.copy(scale = it))
                    }.update { it.value = value.scale }.width(200f)
                    button(Icon.undo, Styles.clearNonei) {
                        set(value.copy(scale = 1f))
                    }.disabled { Mathf.equal(value.scale, 1f) }.padTop(4f)
                    row()
                }
            }.tooltip(mindustryX.bundles.UiTexts.i("缩放")).padRight(4f).checked { !Mathf.equal(value.scale, 1f) } // 原文本:缩放
            addTools()

            row()
            value.constraintX?.let {
                add()
                label {
                    "X: ${it.type.name} to [${it.target}]"
                }.colspan(columns - 1).left().row()
            }
            value.constraintY?.let {
                add()
                label {
                    "Y: ${it.type.name} to [${it.target}]"
                }.colspan(columns - 1).left().row()
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
                applyScale()
                keepInStage()
                unapplyScale()
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

        var autoHeight = false
        var resizable = false
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

            if (state == State.Stable) {
                if (!resizable) setSize(prefWidth, prefHeight)
                else if (autoHeight && prefHeight != height) height = prefHeight
            }

            applyScale() //before any position/size operation

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
                val center = Vec2(getX(Align.center), getY(Align.center))
                val (constraintX, constraintY) = adsorption.findBestConstraints()
                data.set(data.value.copy(center = center, constraintX = constraintX, constraintY = constraintY))
            }

            unapplyScale()
        }

        private fun updateData() {
            @Suppress("DEPRECATION")
            data.value.rect?.let { old ->
                val center = old.getCenter(Vec2())
                val size = old.getSize(Vec2())
                data.set(data.value.copy(center = center, size = size, rect = null))
            }
            if (data.value.center == null)
                data.set(data.value.copy(center = Vec2(parent.width / 2, parent.height / 2)))
            if (!resizable && data.value.size != null) {
                data.set(data.value.copy(size = null))
            }
        }

        fun rebuild() {
            clear()
            if (open) {
                //编辑模式
                background = paneBg
                touchable = Touchable.enabled
                if (resizable)
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
                                add(setting.buildUI()).growX().padBottom(4f).row()
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
                    cell.maxSize(it.x / Scl.scl(), it.y / Scl.scl())
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

                if (resizable)
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

            //Repack table to fit content
            pack()

            data.set(data.value.copy(size = Vec2(table.width, table.height)))
        }

        //Apply scale to x,y,width,height, and reset scale to 1
        //Note: center position is preserved when applying/unapplying scale

        private fun applyScale() {
            val scale = data.value.scale
            x = (x + width / 2) - (width * scale) / 2
            y = (y + height / 2) - (height * scale) / 2
            width *= scale
            height *= scale
            setScale(1f)
        }

        private fun unapplyScale() {
            val scale = data.value.scale
            x = (x + width / 2) - (width / scale) / 2
            y = (y + height / 2) - (height / scale) / 2
            width /= scale
            height /= scale

            transform = scale != 1f
            setScale(scale)
            setOrigin(width / 2, height / 2)
        }
    }

    class PreferAnyWidth : Element() {
        override fun getMinWidth(): Float = 0f
        override fun getPrefWidth(): Float = width
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

        visible { Vars.state.isMenu || Vars.ui.hudfrag.shown }

        fill(Styles.black6) { t ->
            t.name = "overlayUI-bg"
            t.touchable = Touchable.enabled
            t.visibility = Boolp { open }
            t.bottom()
            t.defaults().size(Vars.iconLarge).width(Vars.iconLarge * 1.5f).pad(4f)
            t.button(Icon.add) {
                UIExtKt.showFloatSettingsPanel {
                    add(mindustryX.bundles.UiTexts.i("添加面板")).color(Color.gold).align(Align.center).row() // 原文本:添加面板
                    pane(Styles.smallPane, Table().apply {
                        defaults().minWidth(120f).fillX().pad(4f)
                        val notAvailable = mutableListOf<Window>()
                        windows.forEach {
                            if (!it.availability.get()) {
                                notAvailable.add(it)
                                return@forEach
                            }
                            add(TextButton(it.data.title).apply {
                                label.setWrap(true)
                                label.setAlignment(Align.left)
                                setDisabled { it.data.enabled }
                                changed { it.data.enabled = true }
                            }).row()
                        }
                        if (notAvailable.isNotEmpty()) {
                            add(mindustryX.bundles.UiTexts.i("当前不可用的面板:")).align(Align.center).row() // 原文本:当前不可用的面板:
                            notAvailable.forEach {
                                add(TextButton(it.data.title).apply {
                                    label.setWrap(true)
                                    label.setAlignment(Align.left)
                                    isDisabled = true
                                }).row()
                            }
                        }
                    }).grow().row()
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

        //LogicUpdate
        Vars.ui.hudGroup.find<Element?>("minimap")?.parent?.let { minimapParent ->
            AdsorptionSystem.addDynamic("minimapFrag") {
                if (UIExtKt.isVisible(minimapParent)) {
                    rectForElements(minimapParent.children)?.let {
                        reset(it.x, it.y, it.width, it.height)
                    }
                }
            }
        } ?: Log.warn("[OverlayUI] cannot find 'minimap' for adsorption")
        Vars.ui.hudGroup.find<Stack>("waves/editor")?.let { stack ->
            AdsorptionSystem.addDynamic("statusFrag") {
                val element = stack.children.firstOrNull { it.visible }
                if (element != null && UIExtKt.isVisible(element) && element is Table) {
                    rectForElements(element.children)?.let {
                        reset(it.x, it.y, it.width, it.height)
                    }
                }
            }
        } ?: Log.warn("[OverlayUI] cannot init 'statusFrag' for adsorption")
        update {
            AdsorptionSystem.update()
        }
    }

    private fun rectForElements(elements: Iterable<Element>): Rect? {
        val iter = elements.iterator()
        if (!iter.hasNext()) return null
        val r = iter.next().run {
            Tmp.r1.set(x, y, width, height)
        }
        while (iter.hasNext()) {
            val it = iter.next()
            r.merge(Tmp.r2.set(it.x, it.y, it.width, it.height))
        }
        return r
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
