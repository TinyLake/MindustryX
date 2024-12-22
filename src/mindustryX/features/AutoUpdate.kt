package mindustryX.features

import arc.Core
import arc.Events
import arc.files.Fi
import arc.util.Align
import arc.util.Http
import arc.util.Log
import arc.util.OS
import arc.util.io.Streams
import arc.util.serialization.Jval
import mindustry.Vars
import mindustry.core.Version
import mindustry.game.EventType
import mindustry.gen.Icon
import mindustry.graphics.Pal
import mindustry.net.BeControl
import mindustry.ui.Bar
import mindustry.ui.dialogs.BaseDialog
import mindustryX.VarsX
import mindustryX.features.ui.Format
import java.time.Duration
import java.time.Instant

object AutoUpdate {
    data class Release(val tag: String, val version: String, val json: Jval) {
        data class Asset(val name: String, val url: String)

        fun matchCurrent(): Boolean {
            return tag == "$currentBranch-build" || json.getString("body", "").contains("REPLACE $currentBranch")
        }

        fun findAsset(): Asset? {
            val assets = json.get("assets").asArray().asIterable()
                .map { Asset(it.getString("name"), it.getString("browser_download_url", "")) }
                .sortedByDescending { it.name }
            return assets.firstOrNull {
                when {
                    VarsX.isLoader -> it.name.endsWith("loader.dex.jar")
                    OS.isAndroid -> it.name.endsWith(".apk")
                    else -> it.name.endsWith("Desktop.jar")
                }
            }
        }
    }

    val active get() = !VarsX.devVersion

    val repo = "TinyLake/MindustryX-work"
    var versions = emptyList<Release>()
    val currentBranch get() = VarsX.version.split('-', limit = 2).getOrNull(1)
    var latest: Release? = null
    val newVersion: Release? get() = latest?.takeIf { it.version > VarsX.version }

    val ignoreOnce = SettingsV2.TextPref.create("AutoUpdate.ignoreOnce", "")
    val ignoreUntil = SettingsV2.TextPref.create("AutoUpdate.ignoreUntil", "")

    fun checkUpdate() {
        if (versions.isNotEmpty()) return
        Http.get("https://api.github.com/repos/$repo/releases")
            .timeout(10000)
            .error { Log.warn("Fetch releases fail: $it") }
            .submit { res ->
                val json = Jval.read(res.resultAsString)
                versions = json.asArray().map {
                    Release(it.getString("tag_name"), it.getString("name"), it)
                }.toList()
                Core.app.post(::fetchSuccess)
            }
    }

    private fun fetchSuccess() {
        val available = versions.filter { it.matchCurrent() }
        latest = available.maxByOrNull { it.version } ?: return

        val newVersion = newVersion ?: return
        if (!Core.settings.getBool("showUpdateDialog", true) || ignoreOnce.value == newVersion.version
            || kotlin.runCatching { Instant.parse(ignoreUntil.value) > Instant.now() }.getOrNull() == true
        ) return

        if (Vars.clientLoaded) return showDialog(newVersion)
        Events.on(EventType.ClientLoadEvent::class.java) {
            Vars.ui.showConfirm("检测到新版MindustryX!\n打开更新列表?", ::showDialog)
        }
    }

    @JvmOverloads
    fun showDialog(version: Release? = latest) {
        checkUpdate()
        val dialog = BaseDialog("自动更新")
        dialog.cont.apply {
            add("当前版本号: ${VarsX.version}").labelAlign(Align.center).width(500f).row()
            newVersion?.let {
                add("新版本: ${it.version}").labelAlign(Align.center).width(500f).row()
            }
            if (versions.isEmpty()) {
                add("检查更新失败，请稍后再试").row()
                return@apply
            }
            versions.forEach {
                check(it.version, version == it) { _ ->
                    dialog.hide()
                    showDialog(it)
                }.left().row()
            }
            if (version == null) {
                add("你已是最新版本，不需要更新！")
                return@apply
            }

            val asset = version.findAsset()
            var url = asset?.url.orEmpty()
            field(url) { url = it }.fillX()
            button("♐") {
                if (!Core.app.openURI(url)) {
                    Vars.ui.showErrorMessage("打开失败，网址已复制到粘贴板\n请自行在浏览器打开")
                    Core.app.clipboardText = url
                }
            }.width(50f)

            if (version == newVersion) {
                row().check("跳过当前版本") {
                    if (it) ignoreOnce.value = version.version
                    else ignoreOnce.resetDefault()
                }.checked { ignoreOnce.value == version.version }
                row().check("7天不再提示") {
                    if (it) ignoreOnce.value = (Instant.now() + Duration.ofDays(7)).toString()
                    else ignoreUntil.resetDefault()
                }.checked { kotlin.runCatching { Instant.parse(ignoreUntil.value) > Instant.now() }.getOrNull() == true }
            }

            row().button("自动下载更新") {
                if (asset == null) return@button
                if (!VarsX.isLoader && OS.isAndroid) {
                    Vars.ui.showErrorMessage("目前不支持Apk自动安卓，请在浏览器打开后手动安装")
                    return@button
                }
                startDownload(asset.copy(url = url))
            }.fillX()
        }
        dialog.addCloseButton()
        dialog.show()
    }

    private fun startDownload(asset: Release.Asset) {
        val file = Vars.bebuildDirectory.child(asset.name)

        var progress = 0f
        var length = 0f
        var canceled = false
        val dialog = BaseDialog("@be.updating").apply {
            cont.add(Bar({
                if (length == 0f) return@Bar Core.bundle["be.updating"]
                with(Format(fixDecimals = true)) { "${format(progress * length)}/${format(length)}MB" }
            }, { Pal.accent }, { progress })).width(400f).height(70f)
            buttons.button("@cancel", Icon.cancel) {
                canceled = true
                hide()
            }.size(210f, 64f)
            setFillParent(false)
            show()
        }
        Http.get(asset.url, { res ->
            length = res.contentLength.toFloat() / 1024 / 1024
            file.write(false, 4096).use { out ->
                Streams.copyProgress(res.resultAsStream, out, res.contentLength, 4096) { progress = it }
            }
            if (canceled) return@get
            endDownload(file)
        }) {
            dialog.hide()
            Vars.ui.showException(it)
        }
    }

    private fun endDownload(file: Fi) {
        if (VarsX.isLoader) {
            Vars.mods.importMod(file)
            file.delete()
            Vars.ui.mods.show()
            return
        }
        val fileDest = if (OS.hasProp("becopy")) Fi.get(OS.prop("becopy"))
        else Fi.get(BeControl::class.java.protectionDomain.codeSource.location.toURI().path)
        val args = if (OS.isMac) arrayOf<String>(Vars.javaPath, "-XstartOnFirstThread", "-DlastBuild=" + Version.build, "-Dberestart", "-Dbecopy=" + fileDest.absolutePath(), "-jar", file.absolutePath())
        else arrayOf<String>(Vars.javaPath, "-DlastBuild=" + Version.build, "-Dberestart", "-Dbecopy=" + fileDest.absolutePath(), "-jar", file.absolutePath())
        Runtime.getRuntime().exec(args)
        Core.app.exit()
    }
}