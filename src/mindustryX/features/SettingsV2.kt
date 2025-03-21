package mindustryX.features

import arc.Core
import arc.input.KeyCode
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
import mindustry.gen.Icon
import mindustry.graphics.Pal
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog

/**
 * 新的设置类
 * 接口与Core.settings解耦，所有设置项将在实例化时读取
 * 所有读取修改应该通过value字段进行
 */

object SettingsV2 {
    data class Data<T>(val name: String, val def: T, val ext: SettingExt<T>, var persistentProvider: PersistentProvider = PersistentProvider.Arc) {
        private val changedSet = mutableSetOf<String>()
        var value: T = def
            set(value) {
                val v = ext.transformValue(value)
                if (v == field) return
                field = value
                persistentProvider.set(name, value)
                changedSet.clear()
            }

        init {
            if (name in ALL)
                Log.warn("Settings initialized!: $name")
            ALL[name] = this
            persistentProvider.run {
                value = get(name, def)
            }
        }

        @JvmOverloads
        fun changed(name: String = "DEFAULT"): Boolean {
            return changedSet.add(name)
        }

        //util
        val category: String get() = name.substringBefore('.', "")
        val title: String get() = Core.bundle.get("settingV2.${name}.name", name)
        fun resetDefault() {
            persistentProvider.reset(name)
            value = def
        }

        fun buildUI(table: Table) {
            Table().left().apply {
                button(Icon.undo, Styles.clearNonei) { resetDefault() }.tooltip("@settings.reset")
                    .fillY().disabled { value == def }
                ext.build(this@Data, this)

                Core.bundle.getOrNull("settingV2.${name}.description")?.let {
                    Vars.ui.addDescTooltip(this, it)
                }
                table.add(this).fillX().row()
            }
        }

        fun addFallbackName(name: String) {
            persistentProvider = PersistentProvider.WithFallback(name, persistentProvider)
        }
    }

    sealed interface PersistentProvider {
        fun <T> get(name: String, def: T): T
        fun <T> set(name: String, value: T)
        fun reset(name: String)

        data object Noop : PersistentProvider {
            override fun <T> get(name: String, def: T): T = def
            override fun <T> set(name: String, value: T) {}
            override fun reset(name: String) {}
        }

        data object Arc : PersistentProvider {
            override fun <T> get(name: String, def: T): T {
                @Suppress("UNCHECKED_CAST")
                return Core.settings.get(name, def) as T
            }

            override fun <T> set(name: String, value: T) {
                Core.settings.put(name, value)
            }

            override fun reset(name: String) {
                Core.settings.remove(name)
            }
        }

        class WithFallback(private val fallback: String, private val impl: PersistentProvider) : PersistentProvider {
            override fun <T> get(name: String, def: T): T {
                return impl.get(name, impl.get(fallback, def))
            }

            override fun <T> set(name: String, value: T) {
                impl.set(name, value)
            }

            override fun reset(name: String) {
                impl.reset(name)
                impl.reset(fallback)
            }
        }
    }

    sealed interface SettingExt<T> {
        fun transformValue(value: T): T = value
        fun build(s: Data<T>, table: Table)

        //util
        fun create(name: String, def: T) = Data(name, def, this)
        fun create(name: String, def: T, persistentProvider: PersistentProvider) = Data(name, def, this, persistentProvider)
    }

    data object CheckPref : SettingExt<Boolean> {
        override fun build(s: Data<Boolean>, table: Table) {
            val box = CheckBox(s.title)
            box.changed { s.value = box.isChecked }
            box.update { box.isChecked = s.value }
            table.add(box).left().padTop(3f)
        }

        fun create(name: String) = create(name, false)
    }

    data class SliderPref @JvmOverloads constructor(val min: Int, val max: Int, val step: Int = 1, val labelMap: (Int) -> String = { it.toString() }) : SettingExt<Int> {
        override fun transformValue(value: Int): Int = value.coerceIn(min, max)
        override fun build(s: Data<Int>, table: Table) {
            val elem = Slider(min.toFloat(), max.toFloat(), step.toFloat(), false)
            elem.changed { s.value = elem.value.toInt() }
            elem.update { elem.value = s.value.toFloat() }

            val content = Table().apply {
                touchable = Touchable.disabled
                add(s.title, Styles.outlineLabel).left().growX().wrap()
                label { labelMap(s.value) }.style(Styles.outlineLabel).padLeft(10f).right().get()
            }

            table.stack(elem, content).minWidth(220f).growX().padTop(4f)
        }
    }

    data class ChoosePref(
        val values: List<String>,
        private val impl: SliderPref = SliderPref(0, values.size, labelMap = { values[it] })
    ) : SettingExt<Int> by impl

    data object TextPref : SettingExt<String> {
        override fun transformValue(value: String): String = value.trim()
        override fun build(s: Data<String>, table: Table) {
            val elem = TextField()
            elem.changed { s.value = elem.text }
            elem.update { elem.text = s.value }

            table.table().left().padTop(3f).fillX().get().apply {
                add(s.title).padRight(8f)
                add(elem).growX()
            }
        }
    }

    data object TextAreaPref : SettingExt<String> {
        override fun transformValue(value: String): String = value.trim()
        override fun build(s: Data<String>, table: Table) {
            val elem = TextArea("")
            elem.setPrefRows(5f)
            elem.changed { s.value = elem.text }
            elem.update { elem.text = s.value }
            table.add(s.title).left().padTop(3f)
            table.row().add(elem).colspan(2).fillX()
        }
    }

    val ALL = LinkedHashMap<String, Data<*>>()

    class SettingDialog(val settings: Iterable<Data<*>>) : BaseDialog("@settings") {
        init {
            cont.add(Table().also { t ->
                settings.forEach { it.buildUI(t) }
            }).fill().row()
            cont.button("@settingV2.reset") {
                settings.forEach { it.resetDefault() }
            }
            addCloseButton()
            closeOnBack()
        }

        fun showFloatPanel(x: Float, y: Float) {
            val table = Table().apply {
                background(Styles.black8).margin(8f)
                settings.forEach { it.buildUI(this) }
                button("@close") { this.remove() }.fillX()
            }
            Core.scene.add(table)
            table.pack()
            table.setPosition(x, y, Align.center)
            table.keepInStage()
        }
    }

    private var settingSearch: String = ""

    @JvmStatic
    fun buildSettingsTable(table: Table) {
        table.clearChildren()
        val searchTable = table.table().fillX().get()
        table.row()
        val contentTable = table.table().fillX().get()
        table.row()

        fun rebuildContent() {
            contentTable.clearChildren()
            ALL.values.groupBy { it.category }.toSortedMap().forEach { (c, settings0) ->
                val category = Core.bundle.get("settingV2.$c.category")
                val categoryMatch = c.contains(settingSearch, ignoreCase = true) || category.contains(settingSearch, ignoreCase = true)
                val settings = if (categoryMatch) settings0 else settings0.filter {
                    if ("@modified" in settingSearch) return@filter it.changed()
                    it.name.contains(settingSearch, true) || it.title.contains(settingSearch, true)
                }
                if (c.isNotEmpty() && settings.isNotEmpty()) {
                    contentTable.add(category).color(Pal.accent).padTop(10f).padBottom(5f).center().row()
                    contentTable.image().color(Pal.accent).fillX().height(3f).padBottom(10f).row()
                }
                settings.forEach { it.buildUI(contentTable) }
            }
        }
        searchTable.apply {
            image(Icon.zoom)
            field(settingSearch, {
                settingSearch = it
                rebuildContent()
            }).growX()
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

    //零散的设置，放在下方

    @JvmField
    val blockInventoryWidth = SliderPref(3, 16).create("blockInventoryWidth", 3)

    @JvmField
    val minimapSize = SliderPref(40, 400, 10).create("minimapSize", 140)

    @JvmField
    val arcTurretShowPlaceRange = CheckPref.create("arcTurretPlaceCheck")

    @JvmField
    val arcTurretShowAmmoRange = CheckPref.create("arcTurretPlacementItem")

    @JvmField
    val staticShieldsBorder = CheckPref.create("staticShieldsBorder")

    @JvmField
    val allUnlocked = CheckPref.create("allUnlocked")

    @JvmField
    val editorBrush = SliderPref(3, 13).create("editorBrush", 6)

    @JvmField
    val noPlayerHitBox = CheckPref.create("noPlayerHitBox")
}