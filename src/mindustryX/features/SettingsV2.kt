package mindustryX.features

import arc.Core
import arc.graphics.Color
import arc.input.KeyBind
import arc.input.KeyBind.Axis
import arc.input.KeyCode
import arc.scene.Element
import arc.scene.event.ClickListener
import arc.scene.event.InputEvent
import arc.scene.event.Touchable
import arc.scene.ui.*
import arc.scene.ui.ImageButton.ImageButtonStyle
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
import mindustry.ui.dialogs.BaseDialog
import mindustry.ui.dialogs.KeybindDialog
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
    private const val hiddenShortcutPrefix = "settingV2-bind-"
    private val shortcutPrefs = mutableListOf<CheckPref>()

    enum class ShortcutMode(val bundleKey: String) {
        toggle("settingV2.shortcut.mode.toggle"),
        hold("settingV2.shortcut.mode.hold")
    }

    open class ReactiveCore<T>(def: T) {
        private var _value: T = def
        private val listeners = mutableListOf<(T) -> Unit>()
        private val changedSet = mutableSetOf<String>()

        open val value: T get() = _value
        fun get(): T = value
        open fun set(value: T) {
            if (value == _value) return
            _value = value
            notifyChanged()
        }

        fun notifyChanged() {
            changedSet.clear()
            value?.let { v ->
                listeners.forEach { it.invoke(v) }
            }
        }

        fun addListener(listener: (T) -> Unit) {
            listeners.add(listener)
        }

        @JvmOverloads
        fun changed(name: String = "DEFAULT"): Boolean {
            return changedSet.add(name)
        }
    }

    interface UIBuilder {
        fun buildUI(): Table
    }

    open class Data<T>(val name: String, val def: T) : ReactiveCore<T>(def), UIBuilder {
        private var init = false
        var persistentProvider: PersistentProvider<T> = PersistentProvider.Arc(name)

        override val value: T
            get() {
                if (!init) {
                    persistentProvider.get()?.let { super.set(it) }
                    init = true
                }
                return super.value
            }

        init {
            addListener { (persistentProvider as? PersistentProvider.Savable)?.set(value) }
            if (name in ALL) Log.warn("Settings initialized!: $name")
            @Suppress("LeakingThis")
            ALL[name] = this
        }

        val modified get() = value != def

        fun resetDefault() {
            set(def)
            persistentProvider.reset()
        }

        fun addFallback(provider: PersistentProvider<T>) {
            mayLaterInit {
                if (!modified) {
                    set(provider.get() ?: def)
                }
                (provider as? PersistentProvider.Savable)?.reset()
            }
        }

        fun addFallbackName(name: String) {
            addFallback(PersistentProvider.Arc(name))
        }

        private inline fun mayLaterInit(crossinline action: () -> Unit) {
            if (Core.settings == null) {
                lateInit.add { action.invoke() }
            } else {
                action.invoke()
            }
        }

        val category: String get() = categoryOverride[name] ?: name.substringBefore('.', "")
        val title: String get() = Core.bundle.get("settingV2.${name}.name", name)
        val description: String? get() = Core.bundle.getOrNull("settingV2.${name}.description")

        override fun buildUI() = Table().apply {
            add(title).padRight(8f)
            label { value.toString() }.ellipsis(true).color(Color.gray).labelAlign(Align.left).growX()
            addTools()
        }

        protected open fun Table.addShortcutTool() {
            button(Icon.commandRallySmall, Styles.clearNonei) { }
                .tooltip("@settingV2.shortcut.unsupported")
                .fillY()
                .padLeft(8f)
                .disabled { true }
        }

        protected fun Table.addTools() {
            addShortcutTool()
            val help = description
            button(Icon.info, Styles.clearNonei) { Vars.ui.showInfo(help) }.tooltip(help ?: "@none")
                .fillY().padLeft(8f).disabled { help == null }
            button(Icon.undo, Styles.clearNonei) { resetDefault() }.tooltip("@settingV2.reset")
                .fillY().disabled { !modified }
        }
    }

    interface PersistentProvider<out T> {
        fun get(): T?
        fun reset()

        interface Savable<T> : PersistentProvider<T> {
            fun set(value: T)

            fun setOrReset(value: T?) {
                if (value == null) reset() else set(value)
            }
        }

        data object Noop : PersistentProvider<Nothing> {
            override fun get(): Nothing? = null
            override fun reset() {}
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

    fun <T, R> PersistentProvider<T>.map(mapper: (T) -> R): PersistentProvider<R> = object : PersistentProvider<R> {
        override fun get(): R? = this@map.get()?.let(mapper)
        override fun reset() = this@map.reset()
    }

    class CheckPref @JvmOverloads constructor(name: String, def: Boolean = false) : Data<Boolean>(name, def) {
        companion object {
            private val shortcutButtonStyle by lazy {
                ImageButtonStyle(Styles.clearNonei).apply {
                    imageUpColor = Color.white
                    imageCheckedColor = Pal.accent
                    imageDisabledColor = Color.darkGray
                }
            }
        }

        private val shortcutBind = KeyBind.add(hiddenShortcutName(name), KeyCode.unset)
        private val shortcutModeProvider = PersistentProvider.Arc<Int>("settingV2-shortcut-mode-$name")
        private var shortcutModeLoaded = false
        private var shortcutMode = ShortcutMode.toggle
        private var holdActive = false
        private var holdRestoreValue = false
        private var holdAppliedValue = false

        init {
            shortcutPrefs += this
        }

        fun toggle() {
            set(!value)
        }

        private fun ensureShortcutMode(): ShortcutMode {
            if (!shortcutModeLoaded) {
                shortcutMode = ShortcutMode.entries.getOrElse(shortcutModeProvider.get() ?: 0) { ShortcutMode.toggle }
                shortcutModeLoaded = true
            }
            return shortcutMode
        }

        private fun setShortcutMode(mode: ShortcutMode) {
            if (ensureShortcutMode() == mode) return
            if (holdActive) releaseHeldShortcut()
            shortcutMode = mode
            shortcutModeProvider.set(mode.ordinal)
        }

        private fun releaseHeldShortcut() {
            if (!holdActive) return
            holdActive = false
            if (value == holdAppliedValue) set(holdRestoreValue)
        }

        private fun hasShortcut(): Boolean = shortcutBind.value.key != KeyCode.unset

        private fun shortcutName(): String {
            return if (hasShortcut()) shortcutBind.value.keyName() else Core.bundle.get("settingV2.shortcut.unset")
        }

        private fun clearShortcut() {
            releaseHeldShortcut()
            shortcutBind.value = Axis(KeyCode.unset)
            shortcutBind.save()
        }

        private fun showShortcutPanel() {
            BaseDialog("$title / ${Core.bundle.get("settingV2.shortcut")}").apply {
                cont.defaults().minWidth(180f).fillX().pad(4f)
                cont.add(title).color(Pal.accent).wrap().growX().row()
                cont.label { shortcutName() }.wrap().left().growX().row()
                cont.button("@settings.rebind", Styles.grayt) {
                    KeybindDialog.showRebindDialog(shortcutBind)
                }.growX().row()
                cont.table { modes ->
                    modes.defaults().growX().height(48f).pad(2f)
                    ShortcutMode.entries.forEach { mode ->
                        modes.button(Core.bundle.get(mode.bundleKey), Styles.flatTogglet) {
                            setShortcutMode(mode)
                        }.checked { ensureShortcutMode() == mode }
                    }
                }.growX().row()
                cont.button("@settingV2.shortcut.unbind", Styles.grayt) {
                    clearShortcut()
                }.disabled { !hasShortcut() }.growX().row()
                addCloseButton()
                show()
            }
        }

        override fun Table.addShortcutTool() {
            val shortcutButton = button(Icon.commandRallySmall, shortcutButtonStyle, Vars.iconSmall) {
                KeybindDialog.showRebindDialog(shortcutBind)
            }.tooltip("@settingV2.shortcut").padLeft(4f).checked { hasShortcut() }.get()
            shortcutButton.addListener(object : ClickListener(KeyCode.mouseRight) {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    showShortcutPanel()
                }
            })
        }

        internal fun pollShortcut() {
            if (!hasShortcut()) {
                releaseHeldShortcut()
                return
            }

            when (ensureShortcutMode()) {
                ShortcutMode.toggle -> {
                    releaseHeldShortcut()
                    if (Core.input.keyTap(shortcutBind)) toggle()
                }

                ShortcutMode.hold -> {
                    val down = Core.input.keyDown(shortcutBind)
                    if (down && !holdActive) {
                        holdRestoreValue = value
                        holdAppliedValue = true
                        if (value != holdAppliedValue) {
                            holdActive = true
                            set(holdAppliedValue)
                        }
                    } else if (!down && holdActive) {
                        releaseHeldShortcut()
                    }
                }
            }
        }

        fun uiElement(): Element {
            val box = CheckBox(title)
            box.changed { set(box.isChecked) }
            box.update { box.isChecked = value }
            return box
        }

        override fun buildUI() = Table().apply {
            add(uiElement())
            add().expandX()
            addTools()
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

        override fun buildUI() = Table().apply {
            add(uiElement()).minWidth(220f).growX()
            addTools()
        }
    }

    class ChoosePref @JvmOverloads constructor(name: String, val values: List<String>, def: Int = 0) : SliderPref(name, def, 0, values.size - 1, labelMap = { values[it] }) {
        fun cycle() {
            set((value + 1) % values.size)
        }
    }

    class TextPref @JvmOverloads constructor(name: String, def: String = "", val prefRows: Int = 1) : Data<String>(name, def) {
        override fun set(value: String) {
            super.set(value.trim())
        }

        fun uiElement(): Element {
            val elem = if (prefRows <= 1) TextField("") else TextArea("").apply {
                setPrefRows(prefRows.toFloat())
            }
            elem.changed { set(elem.text) }
            elem.update { if (!elem.hasKeyboard()) elem.text = value }
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
                    category.children.add(setting)
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
                            add(it.buildUI()).growX().padBottom(4f).row()
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

    @JvmStatic
    fun pollShortcuts() {
        if (Vars.headless || Core.scene.hasField() || KeybindDialog.isCapturing()) return
        shortcutPrefs.forEach { it.pollShortcut() }
    }

    @JvmStatic
    fun isHiddenSettingKeybind(bind: KeyBind): Boolean = bind.name.startsWith(hiddenShortcutPrefix)

    private fun hiddenShortcutName(name: String): String {
        val safeName = buildString {
            name.forEach {
                append(if (it.isLetterOrDigit()) it else '_')
            }
        }
        return "$hiddenShortcutPrefix$safeName-${Integer.toUnsignedString(name.hashCode(), 16)}"
    }
}
