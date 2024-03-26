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
import kotlin.system.exitProcess

object AutoUpdate {
    data class Release(val tag: String, val version: String, val json: Jval) {
        data class Asset(val name: String, val url: String)

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

    val active get() = !Version.mdtXBuild.endsWith("-dev")

    val repo = "TinyLake/MindustryX-work"
    var versions = emptyList<Release>()
    val currentBranch get() = Version.mdtXBuild.split('-', limit = 2).getOrNull(1)
    var newVersion: Release? = null

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
        val new = versions.find { it.tag == "$currentBranch-build" && it.version > Version.mdtXBuild } ?: return
        newVersion = new
        if (Core.settings.getBool("showUpdateDialog", true)) {
            if (Vars.clientLoaded) return showDialog()
            Events.on(EventType.ClientLoadEvent::class.java) {
                Vars.ui.showConfirm("检测到新版MindustryX!\n打开更新列表?", ::showDialog)
            }
        }
    }

    @JvmOverloads
    fun showDialog(version: Release? = newVersion) {
        checkUpdate()
        val dialog = BaseDialog("自动更新")
        dialog.cont.apply {
            add("当前版本号: ${Version.mdtXBuild}").labelAlign(Align.center).width(500f).row()
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
        var length = 0L
        var canceled = false
        val dialog = BaseDialog("@be.updating").apply {
            cont.add(Bar({
                if (length == 0L) return@Bar Core.bundle["be.updating"]
                "${progress * length / 1024 / 1024}/${length / 1024 / 1024}MB"
            }, { Pal.accent }, { progress })).width(400f).height(70f)
            buttons.button("@cancel", Icon.cancel) {
                canceled = true
                hide()
            }.size(210f, 64f)
            setFillParent(false)
            show()
        }
        Http.get(asset.url, { res ->
            length = res.contentLength
            file.write(false, 4096).use { out ->
                Streams.copyProgress(res.resultAsStream, out, length, 4096) { progress = it }
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
        exitProcess(0)
    }
}