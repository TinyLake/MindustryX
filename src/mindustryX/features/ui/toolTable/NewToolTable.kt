package mindustryX.features.ui.toolTable

import arc.Core
import arc.Events
import arc.graphics.Colors
import arc.scene.style.Drawable
import arc.scene.ui.Dialog
import arc.scene.ui.TextField
import arc.scene.ui.layout.Scl
import arc.scene.ui.layout.Table
import arc.struct.ObjectIntMap
import arc.util.Strings
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.ctype.MappableContent
import mindustry.game.EventType.WorldLoadEvent
import mindustry.gen.Call
import mindustry.gen.Icon
import mindustry.gen.Iconc
import mindustry.graphics.Pal
import mindustry.ui.Fonts
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog
import mindustry.ui.dialogs.EffectsDialog
import mindustry.world.Block
import mindustry.world.blocks.environment.Floor
import mindustryX.features.*
import mindustryX.features.UIExt.i
import mindustryX.features.ui.comp.GridTable

object NewToolTable : Table() {
    val gridTable = GridTable()

    init {
        background = Styles.black6
        add(gridTable).growX().row()
        gridTable.defaults().size(Vars.iconLarge)

        button(i("信"), i("中央监控室")) { UIExt.arcMessageDialog.show() }
        button("S", i("同步一波")) { Call.sendChatMessage("/sync") }
        button(i("观"), i("观察者模式")) { Call.sendChatMessage("/ob") }
        button(i("版"), i("服务器信息版")) { Call.sendChatMessage("/broad") }
        toggle(i("雾"), i("战争迷雾"), { Vars.state.rules.fog }) {
            Vars.state.rules.fog = Vars.state.rules.fog xor true
        }.disabled { Vars.state.rules.pvp && Vars.player.team().id != 255 }
        button(i("[white]法"), i("法国军礼")) {
            Vars.ui.showConfirm(i("受不了，直接投降？")) { Call.sendChatMessage("/vote gameover") }
        }
        toggle(i("扫"), i("扫描模式"), { RenderExt.transportScan.value }) { RenderExt.transportScan.toggle() }

        toggle(i("块"), i("建筑显示"), { RenderExt.blockRenderLevel > 0 }) { RenderExt.blockRenderLevel0.cycle() }
        toggle(i("兵"), i("兵种显示"), { !RenderExt.unitHide.value }) { RenderExt.unitHide.toggle() }
        toggle(i("弹"), i("子弹显示"), { !RenderExt.noBulletShow.value }) { RenderExt.noBulletShow.toggle() }
        toggle(i("效"), i("特效显示"), { Vars.renderer.enableEffects }) { Settings.toggle("effects") }
        toggle(i("墙"), i("墙体阴影显示"), { Vars.enableDarkness }) { Vars.enableDarkness = !Vars.enableDarkness }
        toggle("${Iconc.map}", i("小地图显示"), { Core.settings.getBool("minimap") }) { Settings.toggle("minimap") }
        toggle(i("箱"), i("碰撞箱显示"), { RenderExt.unitHitbox.value }) { RenderExt.unitHitbox.toggle() }

        button("${Iconc.blockRadar}", i("雷达开关")) { ArcRadar.mobileRadar = !ArcRadar.mobileRadar }.get().also {
            SettingsV2.bindQuickSettings(it, ArcRadar.settings)
        }
        toggle("${Iconc.blockWorldProcessor}", i("移除逻辑锁定"), { Core.settings.getBool("removeLogicLock") }) {
            Settings.toggle("removeLogicLock")
            if (Core.settings.getBool("removeLogicLock")) {
                Vars.control.input.logicCutscene = false
                Vars.ui.announce(i("已移除逻辑视角锁定"))
            }
        }
        toggle(Blocks.worldMessage.emoji(), i("信息板全显示"), { RenderExt.displayAllMessage }) { Settings.toggle("displayallmessage") }
        button("${Iconc.itemCopper}", i("矿物信息")) { floorStatisticDialog() }

        button("${Iconc.fill}", i("特效大全")) { EffectsDialog.withAllEffects().show() }
        button("${Iconc.star}", i("ui大全")) { uiTableDialog().show() }


        add(GridTable()).update { t: Table ->
            if (customButtons.changed("ui")) t.clearChildren()
            if (t.hasChildren()) return@update
            t.defaults().size(Vars.iconLarge)
            for (it in customButtons.get()) {
                t.button(it.name, Styles.cleart) { it.run() }.tooltip(it.content)
            }
            t.button("${Iconc.settings}", Styles.cleart) {
                Dialog("@settings").apply {
                    cont.pane {
                        SettingsV2.buildSettingsTable(it)
                    }.grow().row()
                    cont.button("@close", Icon.left) { hide() }.fillX().row()
                    closeOnBack()

                    isCentered = true
                    isModal = false
                    margin(16f)
                    style = Dialog.DialogStyle().apply {
                        background = style.stageBackground //no stageBackground, only background
                    }
                    show()
                    width = parent.width.coerceAtMost(Scl.scl(600f))
                    height = parent.height.coerceAtMost(Scl.scl(800f))
                }
            }.tooltip(customButtons.title)
        }.fillX().row()

        Events.on(WorldLoadEvent::class.java) { Core.settings.put("removeLogicLock", false) }
    }

    inline fun button(icon: String, tooltip: String, crossinline action: () -> Unit) = gridTable.button(icon, Styles.cleart) {
        action()
    }.tooltip(tooltip)!!

    inline fun toggle(icon: String, tooltip: String, crossinline checked: () -> Boolean, crossinline action: () -> Unit) = gridTable.button(icon, Styles.flatToggleMenut) {
        action()
    }.checked { checked() }.tooltip(tooltip)!!

    data class Button(val icon: Drawable, val tooltip: String, val action: () -> Unit)

    data class CustomButton(val name: String, val content: String) {
        constructor() : this("?", i("未输入指令"))

        fun run() {
            if (content.startsWith("@js ")) {
                Vars.mods.scripts.runConsole(Vars.ui.consolefrag.injectConsoleVariables() + content.substring(4))
            } else {
                Call.sendChatMessage(content)
            }
        }
    }

    private class OldCustomButtonSettings : SettingsV2.PersistentProvider<List<CustomButton>> {
        val num = SettingsV2.PersistentProvider.Arc<Int>("arcQuickMsg")
        override fun get(): List<CustomButton>? {
            val num = num.get() ?: return null
            return List(num) { i ->
                val name = Core.settings.getString("arcQuickMsgShort$i", "")
                val isJs = Core.settings.getBool("arcQuickMsgJs$i", false)
                val content = Core.settings.getString("arcQuickMsg$i", "")
                CustomButton(name, if (isJs) "@js $content" else content)
            }
        }

        override fun reset() {
            val num = num.get() ?: return
            for (i in 0 until num) {
                Core.settings.remove("arcQuickMsgShort$i")
                Core.settings.remove("arcQuickMsgJs$i")
                Core.settings.remove("arcQuickMsg$i")
            }
            this.num.reset()
        }
    }

    @JvmField
    val customButtons = object : SettingsV2.Data<List<CustomButton>>("quickButtons.customButtons", emptyList()) {
        init {
            persistentProvider = SettingsV2.PersistentProvider.AsUBJson(SettingsV2.PersistentProvider.Arc(name), List::class.java, CustomButton::class.java)
            addFallback(OldCustomButtonSettings())
        }

        override fun buildUI() = Table().let { table ->
            var shown = false
            table.button(title) { shown = !shown }.growX().height(55f).padBottom(2f).get().apply {
                imageDraw { if (shown) Icon.downOpen else Icon.upOpen }.size(Vars.iconMed)
                cells.reverse()
                update { isChecked = shown }
            }
            table.row()
            table.collapser(Table().apply {
                defaults().pad(2f)
                update {
                    if (changed()) clearChildren()
                    if (hasChildren()) return@update
                    add(i("序号")); add(i("显示名")); add(i("消息(@js 开头为脚本)")); row()
                    value.forEachIndexed { i, d ->
                        var tmp = d
                        add(i.toString()).padRight(4f)
                        field(d.name) { v -> tmp = tmp.copy(name = v) }.maxTextLength(10)
                        field(d.content) { v -> tmp = tmp.copy(content = v) }.maxTextLength(300).growX()
                        button(Icon.trashSmall, Styles.clearNonei, Vars.iconMed) {
                            set(value.filterNot { it === d })
                        }
                        button(Icon.saveSmall, Styles.clearNonei, Vars.iconMed) {
                            set(value.map { if (it === d) tmp else it })
                        }.disabled { tmp === d }
                        row()
                    }
                    button("@add", Icon.addSmall) {
                        set(value + CustomButton())
                    }.colspan(columns).fillX().row()
                    add(i("[yellow]添加新指令前，请先保存编辑的指令")).colspan(columns).center().padTop(-4f).row()
                }
            }) { shown }.growX()
            table.row()
        }
    }


    private fun floorStatisticDialog() {
        val dialog = BaseDialog(i("ARC-矿物统计"))
        val table = dialog.cont
        table.clear()

        table.table { c: Table ->
            c.add(i("矿物矿(地表/墙矿)")).color(Pal.accent).center().fillX().row()
            c.image().color(Pal.accent).fillX().row()
            c.table { list: Table ->
                var i = 0
                for (item in Vars.content.items()) {
                    if (!Vars.indexer.hasOre(item) && !Vars.indexer.hasWallOre(item)) continue
                    if (i++ % 4 == 0) list.row()
                    list.add(
                        """${item.emoji()} ${item.localizedName}
                            |${Vars.indexer.allOres[item]}/${Vars.indexer.allWallOres[item]}""".trimMargin()
                    ).width(100f).height(50f)
                }
            }.row()

            c.add(i("液体")).color(Pal.accent).center().fillX().row()
            c.image().color(Pal.accent).fillX().row()
            c.table { list: Table ->
                var i = 0
                val counts = ObjectIntMap<Floor>()
                for (tile in Vars.world.tiles) {
                    counts.increment(tile.floor())
                }
                for (block in Vars.content.blocks().select { b: Block -> ((b is Floor && b.liquidDrop != null)) }) {
                    if ((block !is Floor) || block.liquidDrop == null || counts[block.asFloor()] == 0) continue
                    if (i++ % 4 == 0) list.row()
                    list.add(
                        """${block.emoji()} ${block.localizedName}
                            |${counts[block.asFloor()]}""".trimMargin()
                    ).width(100f).height(50f)
                }
            }.row()
        }
        dialog.addCloseButton()
        dialog.show()
    }


    private fun uiTableDialog() = BaseDialog(i("UI图标大全")).apply {
        cont.defaults().maxWidth(800f)
        var query = ""
        val sField = TextField()
        if (!Vars.mobile) sField.requestKeyboard()
        cont.table().growX().get().apply {
            image(Icon.zoom).size(48f)
            field(query) { query = it }.pad(8f).grow().colspan(2).update { if (!it.hasKeyboard()) it.text = query }
            button(Icon.cancelSmall, Styles.cleari) { query = "" }.padLeft(16f).size(32f)
            row()
            add(i("暂存区")).color(Pal.lightishGray).padRight(16f)
            add(sField).growX().get()
            button(Icon.copySmall, Styles.cleari) {
                Core.app.clipboardText = sField.text
            }.padLeft(16f).size(32f)
            button(Icon.cancelSmall, Styles.cleari) { sField.clearText() }.padLeft(16f).size(32f)
        }
        cont.row()
        Table().apply {
            defaults().minWidth(1f)
            add(i("颜色")).color(Pal.accent).center().row()
            image().color(Pal.accent).fillX().row()
            GridTable().apply {
                defaults().height(32f).width(80f).pad(4f)
                for (colorEntry in Colors.getColors()) {
                    val value = colorEntry.value
                    val key = colorEntry.key
                    button("[#$value]$key", Styles.cleart) {
                        Core.app.clipboardText = "[#$value]"
                        sField.appendText("[#$value]")
                    }.tooltip(key).visible { Strings.matches(query, key) }
                }
            }.also { add(it).growX().row() }

            add(i("物品")).color(Pal.accent).center().row()
            image().color(Pal.accent).fillX().row()
            GridTable().apply {
                defaults().size(Vars.iconLarge)

                val keys = mutableSetOf<String>()
                Vars.content.each { if (it is MappableContent && Fonts.stringIcons.containsKey(it.name)) keys.add(it.name) }
                keys.addAll(Fonts.stringIcons.keys())
                for (key in keys) {
                    val icon = Fonts.stringIcons[key]
                    button(icon, Styles.cleart) {
                        Core.app.clipboardText = icon
                        sField.appendText(icon)
                    }.tooltip(key).visible { Strings.matches(query, key) }.get().label.setFontScale(1.75f)
                }
            }.also { add(it).growX().row() }

            add(i("图标")).color(Pal.accent).center().row()
            image().color(Pal.accent).fillX().row()
            GridTable().apply {
                defaults().size(Vars.iconLarge)
                for (it in Iconc.codes) {
                    val icon = it.value.toChar().toString()
                    val key = it.key
                    button(icon, Styles.cleart) {
                        Core.app.clipboardText = icon
                        sField.appendText(icon)
                    }.tooltip(key).visible { Strings.matches(query, key) }.get().label.setFontScale(1.75f)
                }
            }.also { add(it).growX().row() }
        }.also { cont.pane(it).apply { get().isScrollingDisabledX = true }.growX().row() }
        addCloseButton()
    }
}
