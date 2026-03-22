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
    interface IData<T> {
        val default: T
        val value: T get() = get()
        fun get(): T
        fun set(value: T)
        fun reset()
        fun changed(key: Any): Boolean
        fun changed(): Boolean = changed(Unit)
    }

    class MemoryStore<T>(override val default: T) : IData<T> {
        private var _value: T = default
        private val changedSet = mutableSetOf<Any>()

        override val value: T get() = _value
        override fun get(): T = value //for java usage
        override fun set(value: T) {
            if (value == _value) return
            _value = value
            notifyChanged()
        }

        fun notifyChanged() = changedSet.clear()
        override fun reset() = set(default)
        override fun changed(key: Any): Boolean = changedSet.add(key)
    }

    abstract class Computed<T, R>(val core: IData<T>, final override val default: R) : IData<R> {
        private var _value: R = default
        abstract fun compute(t: T): R
        abstract fun computeR(r: R): T
        override fun get(): R {
            if (core.changed(this)) {
                _value = compute(core.get())
            }
            return _value
        }

        override fun set(value: R) {
            if (get() == value) return
            core.set(computeR(value))
            core.changed(this)//consume, not dirty
            this._value = value
        }

        override fun reset() {
            core.reset()
            core.changed(this)//consume, not dirty
            _value = default
        }

        override fun changed(key: Any): Boolean = core.changed(key)
    }

    data class ArcSetting<T>(val key: String, override val default: T) : IData<T> {
        private val core = MemoryStore(default)
        private var loaded = false

        @Suppress("UNCHECKED_CAST")
        override fun get(): T {
            if (!loaded) {
                core.set(Core.settings.get(key, default) as T)
                loaded = true
            }
            return core.get()
        }

        override fun set(value: T) {
            if (core.get() == value) return
            core.set(value)
            Core.settings.put(key, value)
        }

        override fun reset() {
            if (core.get() == default) return
            set(default)
            Core.settings.remove(key)
        }

        override fun changed(key: Any): Boolean = core.changed(key)
    }

    class UBJsonMapped<T>(core: IData<ByteArray?>, default: T, val cls: Class<T>, val elementClass: Class<*>? = null) : Computed<ByteArray?, T>(core, default) {
        constructor(key: String, default: T, cls: Class<T>, elementClass: Class<*>? = null) : this(ArcSetting(key, null), default, cls, elementClass)

        override fun compute(t: ByteArray?): T {
            if (t == null) return default
            return JsonIO.readBytes(cls, elementClass, DataInputStream(ByteArrayInputStream(t)))
        }

        override fun computeR(r: T): ByteArray? {
            return ByteArrayOutputStream().use {
                JsonIO.writeBytes(r, elementClass, DataOutputStream(it))
                it.toByteArray()
            }
        }
    }

    fun interface UIBuilder {
        fun buildUI(): Table
    }


    interface PersistentProvider<out T> {
        fun get(): T?
        fun reset()
        class Arc<T>(val name: String) : PersistentProvider<T> {
            @Suppress("UNCHECKED_CAST")
            override fun get(): T? = Core.settings.get(name, null) as T?
            override fun reset() {
                Core.settings.remove(name)
            }
        }
    }

    fun Table.addHelpButton(help: String) {
        button(Icon.info, Styles.clearNonei) { Vars.ui.showInfo(help) }.tooltip(help)
            .fillY().padLeft(8f)
    }

    fun <T> Table.addResetButton(data: IData<T>) {
        button(Icon.undo, Styles.clearNonei) { data.set(data.default) }.tooltip("@settingV2.reset")
            .fillY().disabled { data.get() == data.default }
    }

    open class Data<T>(val name: String, val core: IData<T>) : IData<T> by core {
        constructor(name: String, def: T) : this(name, ArcSetting<T>(name, def))
        constructor(name: String, cls: Class<T>, def: T) : this(name, UBJsonMapped(name, def, cls))

        val category: String get() = categoryOverride[name] ?: name.substringBefore('.', "")
        val title: String get() = Core.bundle.get("settingV2.${name}.name", name)
        val description: String? get() = Core.bundle.getOrNull("settingV2.${name}.description")
        open var ui: UIBuilder = UIBuilder {
            Table().apply {
                add(title).padRight(8f)
                label { get().toString() }.ellipsis(true).color(Color.gray).labelAlign(Align.left).growX()
                addTools()
            }
        }


        init {
            if (name in ALL)
                Log.warn("Settings initialized!: $name")
            @Suppress("LeakingThis")
            ALL[name] = this
        }

        val modified get() = get() != core.default

        fun addFallback(provider: PersistentProvider<T>) {
            mayLaterInit {
                if (!modified) {
                    set(provider.get() ?: core.default)
                }
                provider.reset()
            }
        }

        fun addFallbackName(name: String) {
            addFallback(PersistentProvider.Arc(name))
        }

        fun <O> addFallback(name: String, map: (O) -> T) {
            val arc = PersistentProvider.Arc<O>(name)
            addFallback(object : PersistentProvider<T> {
                override fun get(): T? = arc.get()?.let<O, T> { it: O -> map(it) }
                override fun reset() = arc.reset()
            })
        }

        private inline fun mayLaterInit(crossinline action: () -> Unit) {
            if (Core.settings == null) {
                lateInit.add { action.invoke() }
            } else {
                action.invoke()
            }
        }

        protected fun Table.addTools() {
            description?.let { addHelpButton(it) }
            addResetButton(this@Data)
        }
    }

    @Suppress("UNCHECKED_CAST")
    open class ListData<T>(name: String, cls: Class<T>, def: List<T> = emptyList()) :
        Data<List<T>>(name, UBJsonMapped(name, def, List::class.java as Class<List<T>>, cls))

    class CheckPref(name: String, core: IData<Boolean>) : Data<Boolean>(name, core) {
        @JvmOverloads
        constructor(name: String, def: Boolean = false) : this(name, ArcSetting(name, def))

        fun toggle() {
            set(!get())
        }

        override var ui: UIBuilder = UI()

        inner class UI : UIBuilder {
            fun uiElement(): Element {
                val box = CheckBox(title)
                box.changed { core.set(box.isChecked) }
                box.update { box.isChecked = core.get() }

                return box
            }

            override fun buildUI() = Table().apply {
                add(uiElement())
                add().expandX()
                description?.let { addHelpButton(it) }
                addResetButton(core)
            }
        }
    }

    open class SliderPref @JvmOverloads constructor(
        name: String, def: Int, val min: Int, val max: Int, val step: Int = 1,
        val labelMap: (Int) -> String = { it.toString() }
    ) : Data<Int>(name, def) {
        override fun set(value: Int) {
            core.set(value.coerceIn(min, max))
        }

        override var ui: UIBuilder = UI()

        //old usage
        fun uiElement() = UI().uiElement()

        inner class UI : UIBuilder {
            fun uiElement(): Element {
                val elem = Slider(min.toFloat(), max.toFloat(), step.toFloat(), false)
                elem.changed { core.set(elem.value.toInt()) }
                elem.update { elem.value = core.get().toFloat() }

                val content = Table().apply {
                    touchable = Touchable.disabled
                    add(title, Styles.outlineLabel).left().growX().wrap()
                    label { labelMap(core.get()) }.style(Styles.outlineLabel).padLeft(10f).right().get()
                }

                return Stack(elem, content)
            }

            override fun buildUI() = Table().apply {
                add(uiElement()).minWidth(220f).growX()
                addTools()
            }
        }
    }


    class ChoosePref @JvmOverloads constructor(name: String, val values: List<String>, def: Int = 0) : SliderPref(name, def, 0, values.size - 1, labelMap = { values[it] }) {
        fun cycle() {
            set((get() + 1) % values.size)
        }
    }

    class TextPref @JvmOverloads constructor(name: String, def: String = "", val prefRows: Int = 1) : Data<String>(name, def) {
        override fun set(value: String) {
            super.set(value.trim())
        }

        override var ui: UIBuilder = UI()

        inner class UI : UIBuilder {
            fun uiElement(): Element {
                val elem = if (prefRows <= 1) TextField("") else TextArea("").apply {
                    setPrefRows(prefRows.toFloat())
                }
                elem.changed { set(elem.text) }
                elem.update { if (!elem.hasKeyboard()) elem.text = get() }
                return elem
            }

            override fun buildUI() = Table().apply {
                if (prefRows > 1) {
                    add(title).left().expandX()
                    addTools()
                    row().add(uiElement()).colspan(columns).growX()
                } else {
                    add(title).padRight(8f)
                    add(uiElement()).growX()
                    addTools()
                }
            }
        }
    }

    val ALL = LinkedHashMap<String, Data<*>>()
    val categoryOverride = mutableMapOf<String, String>()
    private val lateInit = mutableListOf<() -> Unit>()

    fun init() {
        lateInit.forEach { it.invoke() }
        lateInit.clear()
    }

    class CategoryUI(val key: String) : UIBuilder {
        val title: String = Core.bundle.get("settingV2.${key}.category", key)
        val children = mutableListOf<UIBuilder>()

        override fun buildUI() = Table().apply {
            if (key.isNotEmpty() && this@CategoryUI.children.isNotEmpty()) {
                add(title).color(Pal.accent).padTop(10f).padBottom(5f).center().row()
                image().color(Pal.accent).growX().height(3f).padBottom(10f).row()
            }
            this@CategoryUI.children.forEach {
                add(it.buildUI()).growX().padBottom(4f).row()
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun buildSettingsTable(table: Table, settings: List<Data<*>> = ALL.values.toList()) {
        table.clearChildren()
        val searchTable = table.table().growX().get()
        table.row()
        val contentTable = table.table().growX().get()
        table.row()

        var settingSearch = ""
        var onlyModified = false
        fun rebuildContent() {
            contentTable.clearChildren()
            val categories = mutableMapOf<String, CategoryUI>()
            settings.forEach { setting ->
                val category = categories.getOrPut(setting.category) { CategoryUI(setting.category) }
                val match = (!onlyModified || setting.modified) && (
                        category.key.contains(settingSearch, ignoreCase = true) || category.title.contains(settingSearch, ignoreCase = true)
                                || setting.name.contains(settingSearch, true) || setting.title.contains(settingSearch, true)
                        )
                if (match) {
                    category.children.add(setting.ui)
                }
            }
            categories.entries.sortedBy { it.key }.forEach {
                contentTable.add(it.value.buildUI()).growX().padBottom(8f).row()
            }
        }
        searchTable.apply {
            image(Icon.zoom)
            field(settingSearch) {
                settingSearch = it
                rebuildContent()
            }.growX()
            button(Icon.filter, Styles.squareTogglei, Vars.iconMed) {
                onlyModified = !onlyModified
                rebuildContent()
            }.checked { onlyModified }.tooltip("@settingV2.onlyModified")
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
                        settings.forEach {
                            add(it.ui.buildUI()).growX().padBottom(4f).row()
                        }
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
