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
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog
import mindustryX.VarsX
import mindustryX.features.ui.Format
import java.time.Duration
import java.time.Instant

object AutoUpdate {
    data class Release(val repo: String, val url: String, val tag: String, val version: String, val json: Jval) {
        data class Asset(val name: String, val url: String)

        fun matchCurrent(): Boolean {
            if (repo == AutoUpdate.repo) return currentBranch == null
            return tag == "$currentBranch-build" || json.getString("body", "").contains("REPLACE $currentBranch")
        }

        fun findAsset(): Asset? {
            val assets = json.get("assets").asArray().asIterable()
                .map { Asset(it.getString("name"), it.getString("browser_download_url", "")) }
                .sortedByDescending { it.name }
            return assets.firstOrNull {
                when {
                    VarsX.isLoader -> it.name.contains("loader") && it.name.endsWith(".jar")
                    OS.isAndroid -> it.name.endsWith(".apk")
                    else -> it.name.endsWith("Desktop.jar")
                }
            }
        }
    }

    val active get() = !VarsX.devVersion

    const val repo = "TinyLake/MindustryX"
    const val devRepo = "TinyLake/MindustryX-work"
    var versions = emptyList<Release>()
    val currentBranch get() = VarsX.version.split('-', limit = 2).getOrNull(1)
    var latest: Release? = null
    val newVersion: Release? get() = latest?.takeIf { it.version > VarsX.version }

    val showUpdateDialog = SettingsV2.CheckPref("AutoUpdate.showUpdateDialog", true).apply { addFallbackName("showUpdateDialog") }
    val ignoreOnce = SettingsV2.Data("AutoUpdate.ignoreOnce", "")
    val ignoreUntil = SettingsV2.Data("AutoUpdate.ignoreUntil", "")

    fun getReleases(repo: String, result: (List<Release>) -> Unit) {
        Http.get("https://api.github.com/repos/$repo/releases")
            .timeout(10000)
            .error { Log.warn("Fetch releases fail from $repo: $it");result(emptyList()) }
            .submit { res ->
                val json = Jval.read(res.resultAsString)
                val releases = json.asArray().map {
                    Release(repo, it.getString("html_url"), it.getString("tag_name"), it.getString("name"), it)
                }.sortedByDescending { it.version }
                result(releases)
            }
    }

    fun checkUpdate() {
        if (versions.isNotEmpty()) return
        getReleases(repo) { versions0 ->
            val versions = versions0.filter { it.tag == "v${it.version}" }//filter old release
            getReleases(devRepo) { devVersions ->
                this.versions = versions + devVersions
                Core.app.post { fetchSuccess() }
            }
        }
    }

    private fun fetchSuccess() {
        val available = versions.filter { it.matchCurrent() }
        latest = available.maxByOrNull { it.version } ?: return

        val newVersion = newVersion ?: return
        if (!showUpdateDialog.value || ignoreOnce.value == newVersion.version
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
            fun buildVersionList(versions: List<Release>) {
                table().fillX().get().apply {
                    versions.forEach {
                        check(it.version, version == it) { _ ->
                            dialog.hide()
                            showDialog(it)
                        }.left().expandX()
                        button(Icon.infoCircle, Styles.clearNonei, Vars.iconSmall) {
                            UIExt.openURI(it.url)
                        }.tooltip("打开发布页面").padRight(16f).row()
                    }
                }
                row()
            }

            //width为整个Table最小宽度
            add("当前版本号: ${VarsX.version}").labelAlign(Align.center).width(500f).row()
            newVersion?.let {
                add("新版本: ${it.version}").row()
            }
            if (versions.isEmpty()) {
                add("检查更新失败，请稍后再试").row()
                return@apply
            }

            image().fillX().height(2f).row()
            add("正式版").row()
            buildVersionList(versions.filter { it.repo == repo })

            image().fillX().height(2f).row()
            add("测试版本(更新更快,BUG修复更及时)").row()
            buildVersionList(versions.filter { it.repo == devRepo })

            image().fillX().height(2f).row()
            if (version == null) {
                add("你已是最新版本，不需要更新！")
                return@apply
            }

            val asset = version.findAsset()
            var url = asset?.url.orEmpty()
            table().fillX().get().apply {
                field(url) { url = it }.growX()
                button("♐") {
                    UIExt.openURI(url)
                }.width(50f)
            }
            row()

            button("自动下载更新") {
                if (asset == null) return@button
                if (!VarsX.isLoader && OS.isAndroid) {
                    Vars.ui.showErrorMessage("目前不支持Apk自动安卓，请在浏览器打开后手动安装")
                    return@button
                }
                startDownload(asset.copy(url = url))
            }.fillX().row()

            if (version == newVersion) {
                table().fillX().get().apply {
                    button(ignoreOnce.title) {
                        ignoreOnce.set(version.version)
                        dialog.hide()
                    }.growX()
                    button(ignoreUntil.title) {
                        ignoreOnce.set((Instant.now() + Duration.ofDays(7)).toString())
                        dialog.hide()
                    }.growX()
                }.row()
            }
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
            val buffer = 1024 * 1024
            file.write(false, buffer).use { out ->
                Streams.copyProgress(res.resultAsStream, out, res.contentLength, buffer) { progress = it }
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