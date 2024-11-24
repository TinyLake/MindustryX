package mindustryX.features

import arc.Core
import arc.input.KeyCode
import arc.scene.Element
import arc.scene.event.ClickListener
import arc.scene.event.InputEvent
import arc.scene.event.Touchable
import arc.scene.ui.*
import arc.scene.ui.layout.Table
import arc.util.Align
import arc.util.Log
import arc.util.Reflect
import arc.util.Time
import mindustry.Vars
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog

/**
 * 新的设置类
 * 接口与Core.settings解耦，所有设置项将在实例化时读取
 * 所有读取修改应该通过value字段进行
 */
object SettingsV2 {
    val ALL = mutableMapOf<String, ISetting>()

    interface ISetting {
        var changed: Boolean
        val name: String
        fun load()
        fun build(table: Table)
    }

    abstract class BaseSetting : ISetting {
        //所有子类都有 value, def字段，因为泛型涉及到包装，此处不声明
        override var changed: Boolean = true
            get() {
                if (!field) return false
                field = false
                return field
            }

        //util
        protected val title get() = Core.bundle.get("setting.${name}.name", name)
        protected fun Element.addDesc() {
            val desc = Core.bundle.getOrNull("setting.${name}.description") ?: return
            Vars.ui.addDescTooltip(this, desc)
        }
    }

    data class CheckPref(override val name: String, val def: Boolean = false) : BaseSetting() {
        var value = def
            set(value) {
                if (value == field) return
                field = value
                Core.settings.put(name, value)
                changed = true
            }

        override fun load() {
            value = Core.settings.getBool(name, def)
        }

        override fun build(table: Table) {
            val box = CheckBox(title)
            box.changed { value = box.isChecked }
            box.update { box.isChecked = value }
            box.addDesc()
            table.add(box).left().padTop(3f).row()
        }

        init {
            register()
        }
    }

    data class SliderPref(override val name: String, val def: Int, val min: Int, val max: Int, val step: Int = 1) : BaseSetting() {
        var value = def
            set(value) {
                field = value.coerceIn(min, max)
                Core.settings.put(name, field)
                changed = true
            }
        var labelMap: (Int) -> String = { it.toString() }

        override fun load() {
            value = Core.settings.getInt(name, def)
        }

        override fun build(table: Table) {
            val elem = Slider(min.toFloat(), max.toFloat(), step.toFloat(), false)
            elem.changed { value = elem.value.toInt() }
            elem.update { elem.value = value.toFloat() }

            val content = Table().apply {
                touchable = Touchable.disabled
                add(title, Styles.outlineLabel).left().growX().wrap()
                label { labelMap(value) }.style(Styles.outlineLabel).padLeft(10f).right().get()
            }

            table.stack(elem, content).minWidth(220f).growX().padTop(4f)
                .also { it.get().addDesc() }.row()
        }

        init {
            register()
        }
    }

    data class ChoosePref(
        override val name: String, val values: List<String>, val def: Int = 0,
        private val impl: SliderPref = SliderPref(name, def, 0, values.size - 1).apply {
            labelMap = { values[it] }
        }
    ) : ISetting by impl

    data class TextPref(override val name: String, val def: String = "", val area: Boolean = false) : BaseSetting() {
        var value = def
            set(value) {
                field = value
                Core.settings.put(name, value)
                changed = true
            }

        override fun load() {
            value = Core.settings.getString(name, def)
        }

        override fun build(table: Table) {
            if (area) {
                val elem = TextArea("")
                elem.setPrefRows(5f)
                elem.changed { value = elem.text }
                elem.update { elem.text = value }
                table.add(title).left().padTop(3f).get().addDesc()
                table.row().add(elem).fillX().row()
                return
            }
            val elem = TextField()
            elem.changed { value = elem.text }
            elem.update { elem.text = value }

            table.table().left().padTop(3f).fillX().get().apply {
                add(title).padRight(8f)
                add(elem).growX()
            }.addDesc()
            table.row()
        }

        init {
            register()
        }
    }

    fun ISetting.register() {
        if (name in ALL)
            Log.warn("Settings initialized!: $name")
        ALL[name] = this
        load()
    }

    class SettingDialog(val settings: List<ISetting>) : BaseDialog("@settings") {
        init {
            cont.add(Table().also {
                settings.forEach { s -> s.build(it) }
            }).fill().row()
            cont.button("@settings.reset") {
                settings.forEach {
                    Core.settings.remove(it.name)
                    it.load()
                }
            }
            addCloseButton()
            closeOnBack()
        }

        fun showFloatPanel(x: Float, y: Float) {
            val table = Table().apply {
                background(Styles.black8).margin(8f)
                settings.forEach { s -> s.build(this) }
                button("@close") { this.remove() }.fillX()
            }
            Core.scene.add(table)
            table.pack()
            table.setPosition(x, y, Align.center)
            table.keepInStage()
        }
    }


    @JvmStatic
    fun bindQuickSettings(button: Button, settings: List<ISetting>) {
        button.removeListener(button.clickListener)
        Reflect.set(Button::class.java, button, "clickListener", object : ClickListener() {
            private var startTime: Long = Long.MAX_VALUE
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean {
                if (super.touchDown(event, x, y, pointer, button)) {
                    startTime = Time.millis()
                    return true
                }
                return false
            }

            override fun clicked(event: InputEvent, x: Float, y: Float) {
                if (Core.input.keyDown(KeyCode.shiftLeft) || Time.timeSinceMillis(startTime) > 500) {
                    SettingDialog(settings).showFloatPanel(event.stageX, event.stageY)
                } else {
                    if (button.isDisabled) return
                    button.setProgrammaticChangeEvents(true)
                    button.toggle()
                }
            }
        })
        button.addListener(button.clickListener)
    }
}