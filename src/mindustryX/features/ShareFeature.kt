package mindustryX.features

import arc.Core
import arc.math.Mathf
import arc.math.geom.Vec2
import arc.scene.style.TextureRegionDrawable
import arc.scene.ui.TextButton
import arc.scene.ui.layout.Table
import arc.util.Http
import arc.util.Log
import arc.util.Strings
import mindustry.Vars
import mindustry.content.Items
import mindustry.content.StatusEffects
import mindustry.ctype.UnlockableContent
import mindustry.entities.Units
import mindustry.game.Schematic
import mindustry.gen.Icon
import mindustry.gen.Iconc
import mindustry.gen.Player
import mindustry.gen.Tex
import mindustry.type.Item
import mindustry.type.UnitType
import mindustry.ui.Fonts
import mindustry.ui.Styles
import mindustry.ui.fragments.ChatFragment
import mindustryX.VarsX
import mindustryX.features.UIExtKt.showFloatSettingsPanel
import mindustryX.features.ui.ArcMessageDialog
import mindustryX.features.ui.FormatDefault.duration
import mindustryX.features.ui.FormatDefault.format
import mindustryX.features.ui.comp.GridTable

object ShareFeature {
    private fun tag(icon: Char) = "<MDTX $icon>"

    @JvmStatic
    fun send(icon: Char, message: String?) {
        UIExt.sendChatMessage("${tag(icon)}$message")
    }

    @JvmStatic
    fun at(playerName: String?) {
        send('@', mindustryX.bundles.UiTexts.uiTemplate("atPlayer", playerName)) // 原文本:<AT>戳了{0}[white]一下，并提醒他留意对话框
    }

    @JvmStatic
    fun shareSchematic(s: Schematic?) {
        uploadPasteBin(Vars.schematics.writeBase64(s)) { url ->
            if (url == null) return@uploadPasteBin
            val code = url.substring(url.lastIndexOf('/') + 1)
            send(Iconc.paste, mindustryX.bundles.UiTexts.uiTemplate("shareCode", code)) // 原文本:<ARCxMDTX><Schem>[black]一坨兼容[] {0}
        }
    }

    private fun uploadPasteBin(content: String, callback: (String?) -> Unit) {
        val req = Http.post("https://pastebin.com/api/api_post.php", "api_dev_key=sdBDjI5mWBnHl9vBEDMNiYQ3IZe0LFEk&api_option=paste&api_paste_expire_date=10M&api_paste_code=$content")
        req.submit { res ->
            val code = res!!.getResultAsString()
            Core.app.post { callback(code) }
        }
        req.error {
            Core.app.post {
                Vars.ui.showException(mindustryX.bundles.UiTexts.ui("upload_failed_try_again"), it) // 原文本:上传失败，再重试一下？
                Core.app.post { callback(null) }
            }
        }
    }

    @JvmStatic
    fun shareSchematicClipboard(schem: Schematic) {
        uploadPasteBin(Vars.schematics.writeBase64(schem)) { link: String? ->
            val msg = buildString {
                append(mindustryX.bundles.UiTexts.uiTemplate("shareHeader", VarsX.version)) // 原文本:这是一条来自 MDTX-{0} 的分享记录\n
                append(mindustryX.bundles.UiTexts.ui("blueprint_name")).append(schem.name()).append("\n") // 原文本:蓝图名：
                append(mindustryX.bundles.UiTexts.ui("shared_by")).append(Vars.player.name).append("\n") // 原文本:分享者：
                append(mindustryX.bundles.UiTexts.ui("blueprint_cost")) // 原文本:蓝图造价：
                val arr = schem.requirements()
                for (stack in arr) {
                    append(stack.item.emoji()).append(stack.item.localizedName).append(stack.amount).append("|")
                }
                append("\n")
                append(mindustryX.bundles.UiTexts.ui("power")) // 原文本:电力：
                val cons = schem.powerConsumption() * 60
                val prod = schem.powerProduction() * 60
                if (!Mathf.zero(prod)) {
                    append("+").append(Strings.autoFixed(prod, 2))
                    if (!Mathf.zero(cons)) {
                        append("|")
                    }
                }
                if (!Mathf.zero(cons)) {
                    append("-").append(Strings.autoFixed(cons, 2))
                }
                append("\n")
                append(mindustryX.bundles.UiTexts.ui("blueprint_code_link")).append(link ?: "x").append("\n") // 原文本:蓝图代码链接：
                if (Vars.schematics.writeBase64(schem).length > 3500) append(mindustryX.bundles.UiTexts.ui("the_blueprint_code_is_too_long_please_click_the_link_to_view_it")) // 原文本:蓝图代码过长，请点击链接查看
                else append(mindustryX.bundles.UiTexts.ui("blueprint_code_n")).append(Vars.schematics.writeBase64(schem)) // 原文本:蓝图代码：\n
            }

            Core.app.setClipboardText(Strings.stripColors(msg))
            UIExt.announce(mindustryX.bundles.UiTexts.ui("saved_to_clipboard")) // 原文本:已保存至剪贴板
        }
    }

    @JvmStatic
    fun shareContent(content: UnlockableContent, description: Boolean) {
        val msg = buildString {
            append(content.localizedName).append(content.emoji())
            append("(").append(content.name).append(")")
            if (content.description != null && description) {
                append(": ").append(content.description)
            }
        }
        send(Iconc.info, msg)
    }

    //因为ArcMessageDialog共用了，所以单独提取出来
    fun waveInfo(wave: Int) = buildString {
        val spawner = Vars.spawner
        append(mindustryX.bundles.UiTexts.uiTemplate("waveContains", spawner.countGroundSpawns(), spawner.countFlyerSpawns())) // 原文本:包含(地×{0},空x{1}):

        for (group in Vars.state.rules.spawns) {
            val count = group.getSpawned(wave - 1)
            if (count == 0) continue
            append(group.type.emoji()).append("(")
            if (group.effect !== StatusEffects.none && group.effect != null) {
                append(group.effect.emoji()).append("|")
            }
            group.getShield(wave - 1).takeIf { it > 0 }?.let { shield ->
                append(format(shield)).append("|")
            }
            append(count).append(")")
        }
    }

    @JvmStatic
    fun shareWaveInfo(wave: Int) {
        if (!Vars.state.rules.waves) return

        val msg = buildString {
            append(mindustryX.bundles.UiTexts.uiTemplate("waveTitle", wave)) // 原文本:第{0}波

            if (wave >= Vars.state.wave) {
                val timer = (Vars.state.wavetime + (wave - Vars.state.wave) * Vars.state.rules.waveSpacing).toInt()
                append(mindustryX.bundles.UiTexts.uiTemplate("waveEta", wave - Vars.state.wave, duration(timer.toFloat() / 60))) // 原文本:(还有{0}波, {1})
            }

            append("：").append(waveInfo(wave))
        }
        send(Iconc.waves, msg)
    }

    data class PowerInfo(val balance: Float, val stored: Float, val capacity: Float, val produced: Float, val need: Float, val satisfaction: Float)
    data class TeamItemInfo(val amount: Int, val delta: Int)

    @JvmStatic
    fun shareTeamPower() {
        val info = UIExt.coreItems.powerInfo()
        //电力: +xxx K/s 电力储存: xxx M/ xxx M
        val msg = buildString {
            append(info.balance.let { Core.bundle.format("bar.powerbalance", (if (it >= 0) "[accent]+" else "[red]") + format(it.toLong()) + "[]") })
            if (info.satisfaction < 1) append(" [gray]").append((info.satisfaction * 100).toInt()).append("%[]")
            append("  ")
            append(Core.bundle.format("bar.powerstored", format(info.stored.toLong()), format(info.capacity.toLong())))
        }
        send(Iconc.power, msg)
    }

    @JvmStatic
    fun openShareItemDialog() {
        showFloatSettingsPanel { table ->
            val grid = GridTable().apply {
                defaults().size(Vars.iconMed).pad(4f)
                for (item in Vars.content.items()) {
                    if (!UIExt.coreItems.usedItems.contains(item)) continue
                    button(TextureRegionDrawable(item.uiIcon), Styles.clearNonei, Vars.iconMed) { shareTeamItem(item) }
                }
            }
            table.add(grid).growX().maxWidth(320f).row()
        }
    }

    fun shareTeamItem(item: Item) {
        if (Vars.player.dead() || Vars.player.team().core() == null) return
        val (amount, delta) = UIExt.coreItems.itemInfo(item)
        send(
            item.emoji().firstOrNull() ?: Iconc.itemCopper,
            Core.bundle.format(
                "mdtx.share.item", item.localizedName,
                (if (amount > 100) format(amount.toLong()) else "[red]$amount[]"),
                (if (delta > 0) "[accent]+" else "[red]") + format(delta.toLong()) + "[]"
            )
        )
    }

    @JvmStatic
    fun openShareUnitDialog() {
        showFloatSettingsPanel { table ->
            val grid = GridTable().apply {
                defaults().size(Vars.iconMed).pad(4f)
                for (unit in Vars.content.units()) {
                    if (!UIExt.coreItems.usedUnits.contains(unit)) continue
                    button(TextureRegionDrawable(unit.uiIcon), Styles.clearNonei, Vars.iconMed) { shareTeamUnit(unit) }
                }
            }
            table.add(grid).growX().maxWidth(320f).row()
        }
    }

    fun shareTeamUnit(unit: UnitType) {
        if (Vars.player.dead() || Vars.player.team().core() == null) return
        val count = Vars.player.team().data().countType(unit)
        val limit = Units.getCap(Vars.player.team())
        val color = (if (count == limit) "orange" else if (count < 10) "red" else "accent")
        send(
            unit.emoji().firstOrNull() ?: Iconc.units,
            Core.bundle.format("mdtx.share.unit", unit.localizedName, "[$color]$count[]", limit)
        )
    }

    fun newShareTable() = Table(Styles.black3).apply {
        defaults().size(Vars.iconMed)
        val underlineToggleT = TextButton.TextButtonStyle().apply {
            font = Fonts.def
            up = Styles.none
            over = Tex.underline
            checked = Tex.underlineOver //Over是黄色的
        }
        button("T", underlineToggleT) { Vars.ui.chatfrag.nextMode() }
            .checked { _ -> Vars.ui.chatfrag.mode == ChatFragment.ChatMode.team }.tooltip(mindustryX.bundles.UiTexts.ui("add_prefix_t")) // 原文本:前缀添加/t
        button(Icon.zoomSmall, Styles.clearNonei) { MarkerType.lockOnLastMark() }
            .tooltip(mindustryX.bundles.UiTexts.ui("lock_the_last_marked_point")) // 原文本:锁定上个标记点

        add("♐>").padRight(18f)
        button(Icon.mapSmall, Styles.clearNonei, Vars.iconMed) { MarkerType.toggleMarkHitterUI() }.tooltip(mindustryX.bundles.UiTexts.ui("mark_map_location")) // 原文本:标记地图位置
        button(Icon.wavesSmall, Styles.clearNonei, Vars.iconMed) { shareWaveInfo(Vars.state.wave) }.tooltip(mindustryX.bundles.UiTexts.ui("share_wave_information")) // 原文本:分享波次信息
        button(Icon.powerSmall, Styles.clearNonei, Vars.iconMed) { shareTeamPower() }.tooltip(mindustryX.bundles.UiTexts.ui("share_power_status")) // 原文本:分享电力情况
        button(TextureRegionDrawable(Items.copper.uiIcon), Styles.clearNonei, Vars.iconSmall) { openShareItemDialog() }.tooltip(mindustryX.bundles.UiTexts.ui("share_inventory_status")) // 原文本:分享库存情况
        button(Icon.unitsSmall, Styles.clearNonei, Vars.iconMed) { openShareUnitDialog() }.tooltip(mindustryX.bundles.UiTexts.ui("share_unit_count")) // 原文本:分享单位数量
    }

    private fun resolveAt(message: String, sender: Player?): Boolean {
        var message = message
        if (!message.contains("<AT>") && !message.contains(tag('@'))) return false

        //Remove prefix
        message = message.substringAfter("<AT>").substringAfter(tag('@'))
        if (message.contains(Vars.player.name)) {
            if (sender != null) Vars.ui.announce(mindustryX.bundles.UiTexts.uiTemplate("atNoticeFrom", sender.name), 10f) // 原文本:[gold]你被[white]{0}[gold]戳了一下，请注意查看信息框哦~
            else Vars.ui.announce(mindustryX.bundles.UiTexts.ui("you_were_poked_please_pay_attention_to_the_information_box"), 10f) // 原文本:[orange]你被戳了一下，请注意查看信息框哦~
        }

        return true
    }

    private fun resolveSchematicShare(message: String, sender: Player?): Boolean {
        if (!ArcOld.schematicShare.get()) return false

        var code = if (message.contains(tag(Iconc.paste))) {
            message.substringAfter(tag(Iconc.paste))
        } else if (message.contains("<Schem>")) {
            message.substringAfter("<Schem>")
        } else return false

        code = code.substringAfterLast(' ')
        if (code.isEmpty()) return false

        Http.get("https://pastebin.com/raw/$code") { res ->
            val content = res.getResultAsString().replace(" ", "+")
            Core.app.post {
                runCatching { Vars.ui.schematics.readShare(content, sender) }
                    .onFailure { Log.err("Fail read schematic share: ", it) }
            }
        }
        return true
    }

    private val posPattern = Regex("\\((\\d+),(\\d+)\\)")
    private fun resolvePositionShare(message: String): Vec2? {
        val match = posPattern.find(message) ?: return null
        return Vec2(match.groupValues[1].toFloat(), match.groupValues[2].toFloat())
    }

    @JvmStatic
    fun resolve(message: String, sender: Player?) {
        when {
            resolvePositionShare(message)?.also {
                ArcMessageDialog.Msg(ArcMessageDialog.Type.markLoc, message, sender?.name, it).add()
                MarkerType.newMarkFromChat(message, it)
            } != null -> Unit

            resolveAt(message, sender) -> {
                ArcMessageDialog.Msg(ArcMessageDialog.Type.markPlayer, message, sender).add()
            }

            resolveSchematicShare(message, sender) -> {}
            else -> {
                if (sender == null) {
                    ArcMessageDialog.Msg(ArcMessageDialog.Type.serverMsg, message).add()
                } else {
                    ArcMessageDialog.Msg(ArcMessageDialog.Type.chat, message, sender).add()
                }
            }
        }
    }
}
