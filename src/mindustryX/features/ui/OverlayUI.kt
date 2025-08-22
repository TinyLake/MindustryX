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
import kotlin.math.roundToInt

object OverlayUI {
    data class WindowData(
        val enabled: Boolean = false,
        val pinned: Boolean = false,
        val rect: Rect? = null,
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
                label {
                    val rect = value.rect ?: return@label "[grey][UNUSED]"
                    "[${rect.x.roundToInt()},${rect.y.roundToInt()} - ${rect.width.roundToInt()}x${rect.height.roundToInt()}]"
                }

                add().growX()
                addTools()
            }
            table.row()
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
                dragging = true

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
                dragging = false
                saveTableRect()
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
                saveTableRect()
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
                saveTableRect()
            }
        }

        val data = WindowSetting("overlayUI.$name")
        private val paneBg = Tex.pane
        private var dragging = false

        var availability: Prov<Boolean> = Prov { true }
        val settings = mutableListOf<SettingsV2.Data<*>>(data)

        init {
            this.name = name
            visible { data.enabled && availability.get() && (open || data.value.pinned) }
            update {
                if (!dragging) keepInStage()
                if (data.changed()) {
                    rebuild()
                    data.changed()//ignore change by rebuild
                }
            }
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

                val rect = data.value.rect
                if (rect == null) {
                    add(table).grow()
                    pack()
                    setPosition(parent.getX(Align.center), parent.getY(Align.center), Align.center)
                    saveTableRect()
                } else {
                    //Set window position and size
                    val cell = add(table).maxSize(rect.width / Scl.scl(), rect.height / Scl.scl())
                    pack()
                    setPosition(rect.x - table.x, rect.y - table.y)

                    //allow for 'grow', 'grow' may update table. So keep window size, and layout again
                    cell.grow().maxSize(Float.NEGATIVE_INFINITY)
                    layout()

                    saveTableRect()
                }

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
                data.value.rect?.let { rect ->
                    setBounds(rect.x, rect.y, rect.width, rect.height)
                }
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

        fun saveTableRect() {
            if (parent == null) return
            keepInStage()
            validate()

            val pos = localToParentCoordinates(Tmp.v1.set(table.x, table.y))
            val rect = Rect(pos.x, pos.y, table.width, table.height)
            //自动贴边处理
            if (getX(Align.left) == 0f) rect.x = 0f
            if (getX(Align.right) == parent.width) rect.x = parent.width - rect.width
            if (getY(Align.bottom) == 0f) rect.y = 0f
            if (getY(Align.top) == parent.height) rect.y = parent.height - rect.height
            data.set(data.value.copy(rect = rect))
        }
    }

    private val showOverlayButton: CheckPref = CheckPref("gameUI.overlayButton", true)
    var open = false
        private set
    val windows: List<Window>
        get() = group.children.filterIsInstance<Window>()

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