package mindustryX.features

import arc.files.Fi
import arc.func.Cons
import arc.func.ConsT
import arc.scene.ui.CheckBox
import arc.scene.ui.layout.Table
import arc.util.Http
import arc.util.Log
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.ui.Styles
import mindustryX.features.SettingsV2.CheckPref
import mindustryX.features.SettingsV2.ListData
import mindustryX.features.SettingsV2.SliderPref
import mindustryX.features.SettingsV2.UIBuilder
import java.net.URL
import java.security.MessageDigest

private const val bypassHeader = "X-MDTX-GH-Bypass"
private const val maxCacheBytes = 2 * 1024 * 1024

data class ProxyConfig(
    var url: String = "https://",
    var assetEnabled: Boolean = false,
    var apiEnabled: Boolean = false,
)

/** 代理列表设置，UI 是带增删/保存的表格。 */
class ProxyListData(name: String, default: List<ProxyConfig>) : ListData<ProxyConfig>(name, ProxyConfig::class.java, default) {
    override var ui: UIBuilder = UI()

    inner class UI : UIBuilder {
        override fun buildUI(): Table = Table().apply {
            var shown = false
            button(title) { shown = !shown }.growX().height(55f).padBottom(2f).row()
            Table().apply {
                defaults().pad(2f)
                update {
                    if (changed()) clearChildren()
                    if (hasChildren()) return@update

                    add("#").width(26f)
                    add("镜像地址").growX()
                    add("Asset").width(56f)
                    add("API").width(50f)
                    add("操作").width(84f)
                    row()

                    get().forEachIndexed { index, proxy ->
                        var edited = proxy
                        add(index.toString()).width(26f)

                        field(proxy.url) { edited = edited.copy(url = it.trim()) }.maxTextLength(240).growX()

                        val assetBox = CheckBox("")
                        assetBox.isChecked = proxy.assetEnabled
                        assetBox.changed { edited = edited.copy(assetEnabled = assetBox.isChecked) }
                        add(assetBox).width(56f)

                        val apiBox = CheckBox("")
                        apiBox.isChecked = proxy.apiEnabled
                        apiBox.changed { edited = edited.copy(apiEnabled = apiBox.isChecked) }
                        add(apiBox).width(50f)

                        table { ops ->
                            ops.button(Icon.trashSmall, Styles.clearNonei, Vars.iconMed) {
                                set(get().filterNot { it === proxy })
                            }
                            ops.button(Icon.saveSmall, Styles.clearNonei, Vars.iconMed) {
                                set(get().map { if (it === proxy) edited else it })
                            }.disabled { edited == proxy }
                        }.width(84f)
                        row()
                    }

                    add("[gray]源站 (github.com) 始终作为最后兜底[]").colspan(5).left().padTop(2f).row()
                    button("@add", Icon.addSmall) {
                        set(get() + ProxyConfig())
                    }.colspan(5).fillX().row()
                    add("[yellow]修改后点击保存图标生效").colspan(5).center().padTop(-4f).row()
                    button("清空缓存", Icon.trash) { GithubAcceleration.clearCache() }.colspan(5).fillX()
                }
            }.also {
                collapser(it) { shown }.growX().row()
            }
        }
    }
}

object GithubAcceleration {
    val enabled = CheckPref("githubAcceleration.enabled", true).apply { addFallbackName("githubMirror") }
    val enableCache = CheckPref("githubAcceleration.cache", true)
    val cacheExpireMinutes = SliderPref("githubAcceleration.cacheExpire", 120, 10, 1440, 10) { "$it min" }
    val maxRetries = SliderPref("githubAcceleration.maxRetries", 3, 1, 8) { it.toString() }

    val proxyList = ProxyListData(
        "githubAcceleration.proxies", listOf(
            ProxyConfig("https://gh.tinylake.top", assetEnabled = true, apiEnabled = true),
            ProxyConfig("https://ghproxy.com", assetEnabled = true, apiEnabled = false)
        )
    )

    private val cacheRoot: Fi by lazy {
        Vars.dataDirectory.child("cache").child("gh-acceleration").also { it.mkdirs() }
    }

    private val canSynthesize: Boolean by lazy {
        runCatching { Http.HttpResponse::class.java.getMethod("ofBytes", Int::class.javaPrimitiveType, ByteArray::class.java) }.isSuccess
    }

    private val cacheEnabled get() = enableCache.get() && canSynthesize

    @JvmStatic
    fun clearCache() {
        runCatching { cacheRoot.list().forEach { it.delete() } }
        Log.info("GitHub acceleration cache cleared")
    }

    private fun isGithubUrl(url: String): Boolean = runCatching {
        val host = URL(url).host.lowercase()
        host == "github.com" || host.endsWith(".github.com")
                || host == "githubusercontent.com" || host.endsWith(".githubusercontent.com")
    }.getOrDefault(false)

    /** Hook：只接管 GitHub 的 GET。fetch 同步执行，响应仍在连接关闭前交付。 */
    @JvmStatic
    fun onRequest(req: Http.HttpRequest): Boolean {
        if (!enabled.get()) return false
        if (req.method != Http.HttpMethod.GET) return false
        if (req.headers.containsKey(bypassHeader)) return false
        val original = req.url ?: return false
        if (!isGithubUrl(original)) return false
        val callback = req.success ?: return false
        val headers = req.headers
        return try {
            fetch(original, { inner ->
                inner.timeout(req.timeout)
                headers.each { k, v -> inner.header(k, v) }
            }) {
                callback.get(it)
            }
            true
        } catch (e: Throwable) {
            req.errorHandler.get(e)
            true
        }
    }

    /** MindustryX 自己的 GitHub GET 入口，loader fallback 下也走同一套 pipeline。 */
    @JvmStatic
    fun get(url: String, configure: Cons<Http.HttpRequest>? = null, callback: (Result<Http.HttpResponse>) -> Unit) {
        Vars.mainExecutor.execute {
            try {
                fetch(url, configure) { res ->
                    runCatching { callback(Result.success(res)) }
                        .onFailure { Log.err("Exception when handle response: $url", it) }
                }
            } catch (e: Throwable) {
                callback(Result.failure(e))
            }
        }
    }

    /** Java 调用入口：从上面的 [get] 派生，把 Result 映射成 success/error。 */
    @JvmStatic
    fun get(url: String, configure: Cons<Http.HttpRequest>?, success: ConsT<Http.HttpResponse, Exception>, error: Cons<Throwable>) {
        get(url, configure) { result ->
            result.onSuccess { success.get(it) }
                .onFailure { error.get(it) }
        }
    }

    /** 同步执行：按列表顺序逐个尝试，成功回调在连接关闭前交付；全部失败抛异常。
     *  [success] 不允许抛异常，调用方需自行 try/catch。 */
    @Throws(Exception::class)
    private fun fetch(original: String, configure: Cons<Http.HttpRequest>?, success: (Http.HttpResponse) -> Unit) {
        if (cacheEnabled && !isApiUrl(original)) {
            readCache(original)?.let { bytes ->
                success(Http.HttpResponse.ofBytes(200, bytes))
                return
            }
        }

        // 镜像按列表顺序尝试，源站(github.com)始终作为最后兜底
        val urls = buildList {
            proxyList.takeIf { enabled.get() }?.run {
                val isApi = isApiUrl(original)
                get().filter { if (isApi) it.apiEnabled else it.assetEnabled }
                    .take(maxRetries.get())
            }?.forEach {
                val base = it.url.trim().trimEnd('/')
                val prefix = if (base.startsWith("http://") || base.startsWith("https://")) base else "https://$base"
                add("$prefix/$original")
            }
            add(original)
        }

        var last: Throwable? = null
        for (url in urls) {
            val req = Http.get(url).header(bypassHeader, "1")
            configure?.get(req)

            var delivered = false
            req.error { e -> last = e }
            req.block { res ->
                val out = if (cacheEnabled && !isApiUrl(original) && res.contentLength in 1..maxCacheBytes.toLong()) {
                    val bytes = res.result
                    writeCache(original, bytes)
                    Http.HttpResponse.ofBytes(200, bytes)
                } else {
                    res
                }
                delivered = true
                last = null
                success(out)
            }
            if (delivered) {
                throw last ?: return
            }
        }
        throw last ?: IllegalStateException("All GitHub proxies failed for $original")
    }

    private fun isApiUrl(url: String): Boolean = runCatching {
        URL(url).host.equals("api.github.com", ignoreCase = true)
    }.getOrDefault(false)

    private fun cacheFile(url: String): Fi {
        val digest = MessageDigest.getInstance("SHA-1").digest(url.toByteArray(Charsets.UTF_8))
        return cacheRoot.child(digest.joinToString("") { "%02x".format(it) } + ".bin")
    }

    private fun readCache(url: String): ByteArray? {
        val file = cacheFile(url)
        if (!file.exists()) return null
        if (System.currentTimeMillis() > file.lastModified() + cacheExpireMinutes.get() * 60_000L) {
            file.delete()
            return null
        }
        return runCatching { file.readBytes() }.getOrNull()
    }

    private fun writeCache(url: String, data: ByteArray) {
        if (data.isEmpty() || data.size > maxCacheBytes) return
        runCatching { cacheFile(url).writeBytes(data) }
            .onFailure { Log.warn("GitHub cache write failed: @", it.toString()) }
    }
}
