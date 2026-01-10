package mindustryX.features

import arc.Core
import arc.math.Mathf
import arc.math.geom.Vec2
import arc.util.Http
import arc.util.Log
import arc.util.Strings
import mindustry.Vars
import mindustry.content.StatusEffects
import mindustry.ctype.UnlockableContent
import mindustry.game.Schematic
import mindustry.gen.Iconc
import mindustry.gen.Player
import mindustryX.VarsX
import mindustryX.features.ui.ArcMessageDialog
import mindustryX.features.ui.FormatDefault.duration
import mindustryX.features.ui.FormatDefault.format

object ShareFeature {
    private fun tag(icon: Char) = "<MDTX $icon>"

    @JvmStatic
    fun send(icon: Char, message: String?) {
        UIExt.sendChatMessage("${tag(icon)}$message")
    }

    @JvmStatic
    fun at(playerName: String?) {
        send('@', "<AT>戳了$playerName[white]一下，并提醒他留意对话框")
    }

    @JvmStatic
    fun shareSchematic(s: Schematic?) {
        uploadPasteBin(Vars.schematics.writeBase64(s)) { url ->
            if (url == null) return@uploadPasteBin
            val code = url.substring(url.lastIndexOf('/') + 1)
            send(Iconc.paste, "<ARCxMDTX><Schem>[black]一坨兼容[] $code")
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
                Vars.ui.showException("上传失败，再重试一下？", it)
                Core.app.post { callback(null) }
            }
        }
    }

    @JvmStatic
    fun shareSchematicClipboard(schem: Schematic) {
        uploadPasteBin(Vars.schematics.writeBase64(schem)) { link: String? ->
            val msg = buildString {
                append("这是一条来自 MDTX-").append(VarsX.version).append("的分享记录\n")
                append("蓝图名：").append(schem.name()).append("\n")
                append("分享者：").append(Vars.player.name).append("\n")
                append("蓝图造价：")
                val arr = schem.requirements()
                for (stack in arr) {
                    append(stack.item.emoji()).append(stack.item.localizedName).append(stack.amount).append("|")
                }
                append("\n")
                append("电力：")
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
                append("蓝图代码链接：").append(link ?: "x").append("\n")
                if (Vars.schematics.writeBase64(schem).length > 3500) append("蓝图代码过长，请点击链接查看")
                else append("蓝图代码：\n").append(Vars.schematics.writeBase64(schem))
            }

            Core.app.setClipboardText(Strings.stripColors(msg))
            UIExt.announce("已保存至剪贴板")
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
        append("包含(地×").append(spawner.countGroundSpawns())
        append(",空x").append(spawner.countFlyerSpawns()).append("):")

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
            append("第").append(wave).append("波")

            if (wave >= Vars.state.wave) {
                val timer = (Vars.state.wavetime + (wave - Vars.state.wave) * Vars.state.rules.waveSpacing).toInt()
                append("(").append("还有").append(wave - Vars.state.wave).append("波, ")
                append(duration(timer.toFloat() / 60)).append(")")
            }

            append("：").append(waveInfo(wave))
        }
        send(Iconc.waves, msg)
    }


    private fun resolveAt(message: String, sender: Player?): Boolean {
        var message = message
        if (!message.contains("<AT>") && !message.contains(tag('@'))) return false

        //Remove prefix
        message = message.substringAfter("<AT>").substringAfter(tag('@'))
        if (message.contains(Vars.player.name)) {
            if (sender != null) Vars.ui.announce("[gold]你被[white] " + sender.name + " [gold]戳了一下，请注意查看信息框哦~", 10f)
            else Vars.ui.announce("[orange]你被戳了一下，请注意查看信息框哦~", 10f)
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

    private val posPattern = Regex("\\((?<x>\\d+),(?<y>\\d+)\\)")
    private fun resolvePositionShare(message: String): Vec2? {
        val match = posPattern.find(message) ?: return null
        return Vec2(match.groups["x"]!!.value.toFloat(), match.groups["y"]!!.value.toFloat())
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