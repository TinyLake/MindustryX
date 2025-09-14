package mindustryX.features

import arc.Core
import arc.graphics.Color
import arc.input.KeyCode
import arc.scene.Element
import arc.scene.event.ClickListener
import arc.scene.event.InputEvent
import arc.scene.event.Touchable
import arc.scene.ui.*
import arc.scene.ui.layout.Stack
import arc.scene.ui.layout.Table
import arc.util.Align
import arc.util.Log
import arc.util.Reflect
import arc.util.Time
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.graphics.Pal
import mindustry.io.JsonIO
import mindustry.ui.Styles
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * 新的设置类
 * 接口与Core.settings解耦，所有设置项将在实例化时读取
 * 所有读取修改应该通过value字段进行
 */

object SettingsV2 {
    //headless Data
    open class DataCore<T>(val name: String, val def: T) {
        var persistentProvider: PersistentProvider<T> = PersistentProvider.Arc(name)
        var value: T = def
            get() {
                if (!init) {
                    persistentProvider.get()?.let { field = it }
                    init = true
                }
                return field
            }
            private set

        private var init = false
        private val changedSet = mutableSetOf<String>()

        fun get(): T = value //for java usage
        open fun set(value: T) {
            if (value == this.value) return
            this.value = value
            (persistentProvider as? PersistentProvider.Savable)?.set(value)
            changedSet.clear()
        }

        init {
            if (name in ALL)
                Log.warn("Settings initialized!: $name")
            @Suppress("LeakingThis")
            ALL[name] = this
        }

        @JvmOverloads
        fun changed(name: String = "DEFAULT"): Boolean {
            return changedSet.add(name)
        }

        fun resetDefault() {
            value = def
            changedSet.clear()
            persistentProvider.reset()
        }

        fun addFallback(provider: PersistentProvider<T>) {
            if (value == def) {
                set(provider.get() ?: def)
            }
            provider.reset()
        }

        fun addFallbackName(name: String) {
            if (Core.settings == null) {
                lateInit.add { addFallbackName(name) }
                return
            }
            addFallback(PersistentProvider.Arc(name))
        }
    }

    interface WithUI {
        fun buildUI(table: Table)
    }

    open class Data<T>(name: String, def: T) : DataCore<T>(name, def), WithUI {
        val category: String get() = categoryOverride[name] ?: name.substringBefore('.', "")
        val title: String get() = Core.bundle.get("settingV2.${name}.name", name)
        val description: String? get() = Core.bundle.getOrNull("settingV2.${name}.description")

        override fun buildUI(table: Table) {
            table.table().fillX().padTop(3f).get().apply {
                add(title).padRight(8f)
                label { value.toString() }.ellipsis(true).color(Color.gray).labelAlign(Align.left).growX()
                addTools()
            }
            table.row()
        }

        protected fun Table.addTools() {
            val help = description
            button(Icon.info, Styles.clearNonei) { Vars.ui.showInfo(help) }.tooltip(help ?: "@none")
                .fillY().padLeft(8f).disabled { help == null }
            button(Icon.undo, Styles.clearNonei) { resetDefault() }.tooltip("@settingV2.reset")
                .fillY().disabled { value == def }
        }
    }

    interface PersistentProvider<out T> {
        fun get(): T?
        fun reset()
        interface Savable<T> : PersistentProvider<T> {
            fun set(value: T)
        }

        data object Noop : PersistentProvider<Nothing> {
            override fun get(): Nothing? = null
            override fun reset() = Unit
        }

        class Arc<T>(val name: String) : PersistentProvider<T>, Savable<T> {
            @Suppress("UNCHECKED_CAST")
            override fun get(): T? = Core.settings.get(name, null) as T?
            override fun set(value: T) {
                Core.settings.put(name, value)
            }

            override fun reset() {
                Core.settings.remove(name)
            }
        }

        class AsUBJson<T>(private val base: Savable<ByteArray>, val cls: Class<*>, val elementClass: Class<*>? = null) : Savable<T> {
            override fun get(): T? {
                val bs = base.get() ?: return null
                @Suppress("UNCHECKED_CAST")
                return JsonIO.readBytes(cls as Class<T>, elementClass, DataInputStream(ByteArrayInputStream(bs)))
            }

            override fun set(value: T) {
                val bs = ByteArrayOutputStream().use {
                    JsonIO.writeBytes(value, elementClass, DataOutputStream(it))
                    it.toByteArray()
                }
                base.set(bs)
            }

            override fun reset() {
                base.reset()
            }
        }
    }

    class CheckPref @JvmOverloads constructor(name: String, def: Boolean = false) : Data<Boolean>(name, def) {
        fun toggle() {
            set(!value)
        }

        fun uiElement(): Element {
            val box = CheckBox(title)
            box.changed { set(box.isChecked) }
            box.update { box.isChecked = value }

            return box
        }

        override fun buildUI(table: Table) {
            table.table().fillX().get().apply {
                add(uiElement()).left().expandX().padTop(3f)
                addTools()
            }
            table.row()
        }
    }

    open class SliderPref @JvmOverloads constructor(name: String, def: Int, val min: Int, val max: Int, val step: Int = 1, val labelMap: (Int) -> String = { it.toString() }) : Data<Int>(name, def) {
        override fun set(value: Int) {
            super.set(value.coerceIn(min, max))
        }

        fun uiElement(): Element {
            val elem = Slider(min.toFloat(), max.toFloat(), step.toFloat(), false)
            elem.changed { set(elem.value.toInt()) }
            elem.update { elem.value = value.toFloat() }

            val content = Table().apply {
                touchable = Touchable.disabled
                add(title, Styles.outlineLabel).left().growX().wrap()
                label { labelMap(value) }.style(Styles.outlineLabel).padLeft(10f).right().get()
            }

            return Stack(elem, content)
        }

        override fun buildUI(table: Table) {
            table.table().fillX().padTop(4f).get().apply {
                add(uiElement()).minWidth(220f).growX()
                addTools()
            }
            table.row()
        }
    }

    class ChoosePref @JvmOverloads constructor(name: String, val values: List<String>, def: Int = 0) : SliderPref(name, def, 0, values.size - 1, labelMap = { values[it] }) {
        fun cycle() {
            set((value + 1) % values.size)
        }
    }

    open class TextPref @JvmOverloads constructor(name: String, def: String = "") : Data<String>(name, def) {
        override fun set(value: String) {
            super.set(value.trim())
        }

        override fun buildUI(table: Table) {
            val elem = TextField(value)
            elem.changed { set(elem.text) }
            elem.update { if (!elem.hasKeyboard()) elem.text = value }

            table.table().fillX().padTop(3f).get().apply {
                add(title).padRight(8f)
                add(elem).growX()
                addTools()
            }
            table.row()
        }
    }

    class TextAreaPref @JvmOverloads constructor(name: String, def: String = "") : TextPref(name, def) {
        override fun buildUI(table: Table) {
            val elem = TextArea("")
            elem.setPrefRows(5f)
            elem.changed { set(elem.text) }
            elem.update { if (!elem.hasKeyboard()) elem.text = value }

            table.table().fillX().padTop(3f).get().apply {
                add(title).left().expandX()
                addTools()
                row().add(elem).colspan(columns).growX()
            }
            table.row()
        }
    }

    val ALL = LinkedHashMap<String, DataCore<*>>()
    val categoryOverride = mutableMapOf<String, String>()
    private val lateInit = mutableListOf<() -> Unit>()

    fun init() {
        lateInit.forEach { it.invoke() }
        lateInit.clear()
    }

    class CategoryUI(val key: String) : WithUI {
        val title: String = Core.bundle.get("settingV2.${key}.category", key)
        val children = mutableListOf<WithUI>()

        override fun buildUI(table: Table) {
            if (key.isNotEmpty() && children.isNotEmpty()) {
                table.add(title).color(Pal.accent).padTop(10f).padBottom(5f).center().row()
                table.image().color(Pal.accent).growX().height(3f).padBottom(10f).row()
            }
            children.forEach { it.buildUI(table) }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun buildSettingsTable(table: Table, settings: List<Data<*>> = ALL.values.filterIsInstance<Data<*>>()) {
        table.clearChildren()
        val searchTable = table.table().growX().get()
        table.row()
        val contentTable = table.table().growX().get()
        table.row()

        var settingSearch = ""
        fun rebuildContent() {
            contentTable.clearChildren()
            val categories = mutableMapOf<String, CategoryUI>()
            settings.forEach { setting ->
                val category = categories.getOrPut(setting.category) { CategoryUI(setting.category) }
                val match = category.key.contains(settingSearch, ignoreCase = true) || category.title.contains(settingSearch, ignoreCase = true)
                        || ("@modified" in settingSearch && setting.value != setting.def)
                        || setting.name.contains(settingSearch, true) || setting.title.contains(settingSearch, true)
                if (match) {
                    category.children.add(setting)
                }
            }
            categories.entries.sortedBy { it.key }.forEach {
                it.value.buildUI(contentTable)
            }
        }
        searchTable.apply {
            image(Icon.zoom)
            field(settingSearch) {
                settingSearch = it
                rebuildContent()
            }.growX()
        }
        rebuildContent()
    }

    @JvmStatic
    fun bindQuickSettings(button: Button, settings: Iterable<Data<*>>) {
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
                    UIExtKt.showFloatSettingsPanel {
                        settings.forEach { it.buildUI(this) }
                    }
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