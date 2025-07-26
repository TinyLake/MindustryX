package mindustryX.features.ui.toolTable

import arc.Core
import arc.Events
import arc.graphics.Colors
import arc.scene.style.Drawable
import arc.scene.ui.Dialog
import arc.scene.ui.layout.Scl
import arc.scene.ui.layout.Table
import arc.struct.ObjectIntMap
import mindustry.Vars
import mindustry.content.Blocks
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
import mindustryX.features.ui.comp.GridTable

object NewToolTable : Table() {
    val gridTable = GridTable()

    init {
        background = Styles.black6
        add(gridTable).growX().row()
        gridTable.defaults().size(Vars.iconLarge)

        button("[cyan]信", Core.bundle.get("newToolTable.centralMonitoringRoom")) { UIExt.arcMessageDialog.show() }
        button("[cyan]S", Core.bundle.get("newToolTable.syncWave")) { Call.sendChatMessage("/sync") }
        button("[cyan]观", Core.bundle.get("newToolTable.observerMode")) { Call.sendChatMessage("/ob") }
        button("[cyan]版", Core.bundle.get("newToolTable.serverInfoBoard")) { Call.sendChatMessage("/broad") }
        toggle("[cyan]雾", Core.bundle.get("newToolTable.fogOfWar"), { Vars.state.rules.fog }) {
            Vars.state.rules.fog = Vars.state.rules.fog xor true
        }.disabled { Vars.state.rules.pvp && Vars.player.team().id != 255 }
        button("[white]法", Core.bundle.get("newToolTable.frenchSalute")) {
            Vars.ui.showConfirm(Core.bundle.get("newToolTable.surrender")) { Call.sendChatMessage("/vote gameover") }
        }
        toggle("扫", "扫描模式", { RenderExt.transportScan.value }) { RenderExt.transportScan.toggle() }

        toggle("[cyan]块", "@newToolTable.buildingDisplay", { RenderExt.blockRenderLevel > 0 }) { RenderExt.blockRenderLevel0.cycle() }
        toggle("[cyan]兵", "@newToolTable.unitDisplay", { !RenderExt.unitHide.value }) { RenderExt.unitHide.toggle() }
        toggle("[cyan]弹", "@newToolTable.bulletDisplay", { !RenderExt.noBulletShow.value }) { RenderExt.noBulletShow.toggle() }
        toggle("[cyan]效", "@newToolTable.effectDisplay", { Vars.renderer.enableEffects }) { Settings.toggle("effects") }
        toggle("[cyan]墙", "@newToolTable.wallShadowDisplay", { Vars.enableDarkness }) { Vars.enableDarkness = !Vars.enableDarkness }
        toggle("[cyan]${Iconc.map}", "@newToolTable.minimapDisplay", { Core.settings.getBool("minimap") }) { Settings.toggle("minimap") }
        toggle("箱", "@newToolTable.hitboxDisplay", { RenderExt.unitHitbox.value }) { RenderExt.unitHitbox.toggle() }

        button("${Iconc.blockRadar}", "@newToolTable.radarToggle") { ArcRadar.mobileRadar = !ArcRadar.mobileRadar }.get().also {
            SettingsV2.bindQuickSettings(it, ArcRadar.settings)
        }
        toggle("${Iconc.blockWorldProcessor}", "@newToolTable.removeLogicLock", { Core.settings.getBool("removeLogicLock") }) {
            Settings.toggle("removeLogicLock")
            if (Core.settings.getBool("removeLogicLock")) {
                Vars.control.input.logicCutscene = false
                Vars.ui.announce(Core.bundle.get("newToolTable.logicLockRemoved"))
            }
        }
        toggle(Blocks.worldMessage.emoji(), "@newToolTable.messageDisplayAll", { RenderExt.displayAllMessage }) { Settings.toggle("displayallmessage") }
        button("${Iconc.itemCopper}", "@newToolTable.mineralInfo") { floorStatisticDialog() }

        button("${Iconc.fill}", "@newToolTable.effectsCollection") { EffectsDialog.withAllEffects().show() }
        button("${Iconc.star}", "@newToolTable.uiCollection") { uiTableDialog().show() }


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
        constructor() : this("?", "@newToolTable.noCommand")

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

        override fun buildUI(table: Table) {
            var shown = false
            table.button(title) { shown = !shown }.fillX().height(55f).padBottom(2f).get().apply {
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
                    add("@newToolTable.serialNumber");add("@newToolTable.displayName");add("@newToolTable.messageScript");row()
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
                    add("@newToolTable.saveBeforeAdd").colspan(columns).center().padTop(-4f).row()
                }
            }) { shown }.fillX()
            table.row()
        }
    }


    private fun floorStatisticDialog() {
        val dialog = BaseDialog("@newToolTable.mineralStatistics")
        val table = dialog.cont
        table.clear()

        table.table { c: Table ->
            c.add("@newToolTable.mineralOre").color(Pal.accent).center().fillX().row()
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

            c.add("液体").color(Pal.accent).center().fillX().row()
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


    private fun uiTableDialog() = BaseDialog("@newToolTable.uiIconCollection").apply {
        cont.defaults().maxWidth(800f)
        val sField = cont.field("") { }.fillX().get()
        cont.row()
        Table().apply {
            defaults().minWidth(1f)
            add("@newToolTable.color").color(Pal.accent).center().row()
            image().color(Pal.accent).fillX().row()
            GridTable().apply {
                defaults().height(32f).width(80f).pad(4f)
                for (colorEntry in Colors.getColors()) {
                    val value = colorEntry.value
                    val key = colorEntry.key
                    button("[#$value]$key", Styles.cleart) {
                        Core.app.clipboardText = "[#$value]"
                        sField.appendText("[#$value]")
                    }.tooltip(key)
                }
            }.also { add(it).growX().row() }

            add("物品").color(Pal.accent).center().row()
            image().color(Pal.accent).fillX().row()
            GridTable().apply {
                defaults().size(Vars.iconLarge)
                for (it in Fonts.stringIcons) {
                    val icon = it.value
                    button(icon, Styles.cleart) {
                        Core.app.clipboardText = icon
                        sField.appendText(icon)
                    }.tooltip(it.key)
                }
            }.also { add(it).growX().row() }

            add("图标").color(Pal.accent).center().row()
            image().color(Pal.accent).fillX().row()
            GridTable().apply {
                defaults().size(Vars.iconLarge)
                for (it in Iconc.codes) {
                    val icon = it.value.toChar().toString()
                    button(icon, Styles.cleart) {
                        Core.app.clipboardText = icon
                        sField.appendText(icon)
                    }.tooltip(it.key)
                }
            }.also { add(it).growX().row() }
        }.also { cont.pane(it).apply { get().isScrollingDisabledX = true }.growX().row() }
        addCloseButton()
    }
}