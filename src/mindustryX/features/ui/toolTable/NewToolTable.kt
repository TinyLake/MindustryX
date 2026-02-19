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
import mindustryX.features.ui.comp.GridTable

object NewToolTable : Table() {
    val gridTable = GridTable()

    init {
        background = Styles.black6
        add(gridTable).growX().row()
        gridTable.defaults().size(Vars.iconLarge)

        button(mindustryX.bundles.ui("shortcut_message_center"), mindustryX.bundles.ui("message_center")) { UIExt.arcMessageDialog.show() } // 原文本:信 | 中央监控室
        button("S", mindustryX.bundles.ui("sync_a_wave")) { Call.sendChatMessage("/sync") } // 原文本:同步一波
        button(mindustryX.bundles.ui("shortcut_observer_mode"), mindustryX.bundles.ui("observer_mode")) { Call.sendChatMessage("/ob") } // 原文本:观 | 观察者模式
        button(mindustryX.bundles.ui("shortcut_server_info"), mindustryX.bundles.ui("server_info_build")) { Call.sendChatMessage("/broad") } // 原文本:版 | 服务器信息版
        toggle(mindustryX.bundles.ui("shortcut_fog"), mindustryX.bundles.ui("fog_of_war"), { Vars.state.rules.fog }) { // 原文本:雾 | 战争迷雾
            Vars.state.rules.fog = Vars.state.rules.fog xor true
        }.disabled { Vars.state.rules.pvp && Vars.player.team().id != 255 }
        button(mindustryX.bundles.ui("shortcut_surrender_vote"), mindustryX.bundles.ui("surrender_vote")) { // 原文本:[white]法 | 法国军礼
            Vars.ui.showConfirm(mindustryX.bundles.ui("surrender_vote_confirm")) { Call.sendChatMessage("/vote gameover") } // 原文本:受不了，直接投降？
        }
        toggle(mindustryX.bundles.ui("shortcut_scan_mode"), mindustryX.bundles.ui("scan_mode"), { RenderExt.transportScan.value }) { RenderExt.transportScan.toggle() } // 原文本:扫 | 扫描模式

        toggle(mindustryX.bundles.ui("shortcut_block_render"), mindustryX.bundles.ui("block_rendering"), { RenderExt.blockRenderLevel > 0 }) { RenderExt.blockRenderLevel0.cycle() } // 原文本:块 | 建筑显示
        toggle(mindustryX.bundles.ui("shortcut_unit_render"), mindustryX.bundles.ui("unit_rendering"), { !RenderExt.unitHide.value }) { RenderExt.unitHide.toggle() } // 原文本:兵 | 兵种显示
        toggle(mindustryX.bundles.ui("shortcut_bullet_render"), mindustryX.bundles.ui("bullet_rendering"), { !RenderExt.noBulletShow.value }) { RenderExt.noBulletShow.toggle() } // 原文本:弹 | 子弹显示
        toggle(mindustryX.bundles.ui("fx"), mindustryX.bundles.ui("effects_rendering"), { Vars.renderer.enableEffects }) { Settings.toggle("effects") } // 原文本:效 | 特效显示
        toggle(mindustryX.bundles.ui("shortcut_wall_shadow"), mindustryX.bundles.ui("wall_shadow_rendering"), { Vars.enableDarkness }) { Vars.enableDarkness = !Vars.enableDarkness } // 原文本:墙 | 墙体阴影显示
        toggle("${Iconc.map}", mindustryX.bundles.ui("minimap"), { Core.settings.getBool("minimap") }) { Settings.toggle("minimap") } // 原文本:小地图显示
        toggle(mindustryX.bundles.ui("shortcut_hitbox"), mindustryX.bundles.ui("hitbox_overlay"), { RenderExt.unitHitbox.value }) { RenderExt.unitHitbox.toggle() } // 原文本:箱 | 碰撞箱显示

        button("${Iconc.blockRadar}", mindustryX.bundles.ui("radar_toggle")) { ArcRadar.mobileRadar = !ArcRadar.mobileRadar }.get().also { // 原文本:雷达开关
            SettingsV2.bindQuickSettings(it, ArcRadar.settings)
        }
        toggle("${Iconc.blockWorldProcessor}", mindustryX.bundles.ui("remove_logic_lock"), { Core.settings.getBool("removeLogicLock") }) { // 原文本:移除逻辑锁定
            Settings.toggle("removeLogicLock")
            if (Core.settings.getBool("removeLogicLock")) {
                Vars.control.input.logicCutscene = false
                Vars.ui.announce(mindustryX.bundles.ui("logic_camera_lock_removed")) // 原文本:已移除逻辑视角锁定
            }
        }
        toggle(Blocks.worldMessage.emoji(), mindustryX.bundles.ui("show_all_message_blocks"), { RenderExt.displayAllMessage }) { Settings.toggle("displayallmessage") } // 原文本:信息板全显示
        button("${Iconc.itemCopper}", mindustryX.bundles.ui("ore_info")) { floorStatisticDialog() } // 原文本:矿物信息

        button("${Iconc.fill}", mindustryX.bundles.ui("effects_library")) { EffectsDialog.withAllEffects().show() } // 原文本:特效大全
        button("${Iconc.star}", mindustryX.bundles.ui("ui_toolkit")) { uiTableDialog().show() } // 原文本:ui大全


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
        constructor() : this("?", mindustryX.bundles.ui("no_command_entered")) // 原文本:未输入指令

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
                    add(mindustryX.bundles.ui("index")); add(mindustryX.bundles.ui("display_name")); add(mindustryX.bundles.ui("message_arg_js_starts_with_script")); row() // 原文本:序号 | 显示名 | 消息(@js 开头为脚本)
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
                    add(mindustryX.bundles.ui("before_adding_new_instructions_please_save_the_edited_instructions_first")).colspan(columns).center().padTop(-4f).row() // 原文本:[yellow]添加新指令前，请先保存编辑的指令
                }
            }) { shown }.growX()
            table.row()
        }
    }


    private fun floorStatisticDialog() {
        val dialog = BaseDialog(mindustryX.bundles.ui("arc_ore_statistics")) // 原文本:ARC-矿物统计
        val table = dialog.cont
        table.clear()

        table.table { c: Table ->
            c.add(mindustryX.bundles.ui("ore_count_surface_wall")).color(Pal.accent).center().fillX().row() // 原文本:矿物矿(地表/墙矿)
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

            c.add(mindustryX.bundles.ui("liquids")).color(Pal.accent).center().fillX().row() // 原文本:液体
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


    private fun uiTableDialog() = BaseDialog(mindustryX.bundles.ui("ui_icon_library")).apply { // 原文本:UI图标大全
        cont.defaults().maxWidth(800f)
        var query = ""
        val sField = TextField()
        if (!Vars.mobile) sField.requestKeyboard()
        cont.table().growX().get().apply {
            image(Icon.zoom).size(48f)
            field(query) { query = it }.pad(8f).grow().colspan(2).update { if (!it.hasKeyboard()) it.text = query }
            button(Icon.cancelSmall, Styles.cleari) { query = "" }.padLeft(16f).size(32f)
            row()
            add(mindustryX.bundles.ui("staging_area")).color(Pal.lightishGray).padRight(16f) // 原文本:暂存区
            add(sField).growX().get()
            button(Icon.copySmall, Styles.cleari) {
                Core.app.clipboardText = sField.text
            }.padLeft(16f).size(32f)
            button(Icon.cancelSmall, Styles.cleari) { sField.clearText() }.padLeft(16f).size(32f)
        }
        cont.row()
        Table().apply {
            defaults().minWidth(1f)
            add(mindustryX.bundles.ui("color")).color(Pal.accent).center().row() // 原文本:颜色
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

            add(mindustryX.bundles.ui("item")).color(Pal.accent).center().row() // 原文本:物品
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

            add(mindustryX.bundles.ui("icon")).color(Pal.accent).center().row() // 原文本:图标
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
