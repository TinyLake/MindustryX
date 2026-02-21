package mindustryX.features

import arc.files.Fi
import arc.scene.ui.CheckBox
import arc.scene.ui.layout.Table
import arc.util.Http
import arc.util.Log
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.ui.Styles
import mindustryX.features.SettingsV2.CheckPref
import mindustryX.features.SettingsV2.Data
import mindustryX.features.SettingsV2.PersistentProvider
import mindustryX.features.SettingsV2.SliderPref
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object GithubAcceleration {
    private const val headerAttempt = "X-MDTX-GH-Attempt"
    private const val headerOriginal = "X-MDTX-GH-Original"
    private const val maxCacheBytes = 2 * 1024 * 1024

    data class ProxyConfig(
        var id: Int,
        var name: String,
        var url: String,
        var enabled: Boolean,
        var assetEnabled: Boolean,
        var apiEnabled: Boolean,
        var locked: Boolean = false
    ) {
        constructor() : this(0, "", "", true, true, true, false)

        fun isDirect(): Boolean = locked || url.isBlank()

        companion object {
            fun defaults() = listOf(
                ProxyConfig(0, "源站 (github.com)", "", true, true, true, true),
                ProxyConfig(1, "ghproxy", "https://ghproxy.com", true, true, false),
                ProxyConfig(2, "kgithub", "https://kgithub.com", false, true, true)
            )
        }
    }

    val enabled = CheckPref("githubAcceleration.enabled", true).apply {
        addFallbackName("githubMirror")
    }
    val enableCache = CheckPref("githubAcceleration.cache", true)
    val cacheExpireMinutes = SliderPref("githubAcceleration.cacheExpire", 120, 10, 1440, 10) { "${it} min" }
    val maxRetries = SliderPref("githubAcceleration.maxRetries", 3, 1, 8) { it.toString() }

    @JvmField
    val proxyList = object : Data<List<ProxyConfig>>("githubAcceleration.proxies", ProxyConfig.defaults()) {
        init {
            persistentProvider = PersistentProvider.AsUBJson(PersistentProvider.Arc(name), List::class.java, ProxyConfig::class.java)
        }

        override fun set(value: List<ProxyConfig>) {
            super.set(normalize(value))
        }

        override fun buildUI() = Table().let { root ->
            var shown = false
            root.button(title) { shown = !shown }.growX().height(55f).padBottom(2f).get().apply {
                imageDraw { if (shown) Icon.downOpen else Icon.upOpen }.size(Vars.iconMed)
                cells.reverse()
                update { isChecked = shown }
            }
            root.row()
            root.collapser(Table().apply {
                defaults().pad(2f)
                update {
                    if (changed()) clearChildren()
                    if (hasChildren()) return@update

                    add("#").width(26f)
                    add("启用").width(36f)
                    add("镜像地址/备注").growX()
                    add("Asset").width(50f)
                    add("API").width(44f)
                    add("操作").width(78f)
                    row()

                    value.forEachIndexed { index, proxy ->
                        var edited = proxy
                        add(index.toString()).width(26f)

                        if (proxy.locked) {
                            add("[gray]✓[]").width(36f)
                        } else {
                            val enabledBox = CheckBox("")
                            enabledBox.isChecked = proxy.enabled
                            enabledBox.changed { edited = edited.copy(enabled = enabledBox.isChecked) }
                            add(enabledBox).width(36f)
                        }

                        table {
                            it.defaults().growX().left()
                            it.field(proxy.name) { text -> edited = edited.copy(name = text) }.maxTextLength(24).row()
                            it.field(proxy.url) { text -> edited = edited.copy(url = text) }.maxTextLength(240)
                        }.growX()

                        val assetBox = CheckBox("")
                        assetBox.isChecked = proxy.assetEnabled
                        assetBox.isDisabled = proxy.locked
                        assetBox.changed { edited = edited.copy(assetEnabled = assetBox.isChecked) }
                        add(assetBox).width(50f)

                        val apiBox = CheckBox("")
                        apiBox.isChecked = proxy.apiEnabled
                        apiBox.isDisabled = proxy.locked
                        apiBox.changed { edited = edited.copy(apiEnabled = apiBox.isChecked) }
                        add(apiBox).width(44f)

                        table { ops ->
                            if (proxy.locked) {
                                ops.image(Icon.lock).size(Vars.iconSmall)
                            } else {
                                ops.button(Icon.trashSmall, Styles.clearNonei, Vars.iconMed) {
                                    set(value.filterNot { it === proxy })
                                }
                                ops.button(Icon.saveSmall, Styles.clearNonei, Vars.iconMed) {
                                    set(value.map { if (it === proxy) edited else it })
                                }.disabled { edited == proxy }
                            }
                        }.width(78f)
                        row()
                    }

                    button("@add", Icon.addSmall) {
                        val nextId = (value.maxOfOrNull { it.id } ?: 0) + 1
                        set(value + ProxyConfig(nextId, "自建代理", "https://", true, true, true, false))
                    }.colspan(columns).fillX().row()
                    add("[yellow]修改配置后，请点击保存图标生效").colspan(columns).center().padTop(-4f).row()
                    button("清空缓存", Icon.trash) { clearCache() }.colspan(columns).fillX()
                }
            }) { shown }.growX()
            root.row()
        }
    }

    @JvmStatic
    val settings: List<Data<*>> get() = listOf(enabled, enableCache, cacheExpireMinutes, maxRetries, proxyList)

    private val wrappedUrlPattern = Regex("^https?://[^/]+/(https?://.+)$")
    private val cacheRoot: Fi by lazy {
        Vars.dataDirectory.child("cache").child("gh-acceleration").also { it.mkdirs() }
    }

    init {
        settings.forEach { SettingsV2.categoryOverride[it.name] = "githubAcceleration" }
        Http.onBeforeRequest = arc.func.Cons { onBeforeRequest(it) }
    }

    @JvmStatic
    fun init() {
        loadProxies()
    }

    @JvmStatic
    fun loadProxies() {
        proxyList.set(proxyList.value)
    }

    @JvmStatic
    fun clearCache() {
        cacheRoot.list().forEach { it.delete() }
        Log.info("GitHub acceleration cache cleared")
    }

    private fun normalize(raw: List<ProxyConfig>): List<ProxyConfig> {
        val source = raw.firstOrNull { it.isDirect() || it.id == 0 }?.copy(
            id = 0,
            name = "源站 (github.com)",
            url = "",
            enabled = true,
            assetEnabled = true,
            apiEnabled = true,
            locked = true
        ) ?: ProxyConfig.defaults().first()

        val result = mutableListOf(source)
        raw.filterNot { it.id == 0 || it.locked }.forEachIndexed { index, proxy ->
            val normalizedUrl = normalizeProxyUrl(proxy.url)
            if (normalizedUrl.isBlank()) return@forEachIndexed
            result += proxy.copy(
                id = index + 1,
                name = proxy.name.trim().ifBlank { "代理${index + 1}" },
                url = normalizedUrl,
                locked = false
            )
        }
        return result
    }

    private fun normalizeProxyUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank() || trimmed == "https://") return ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return "https://$trimmed"
    }

    private fun onBeforeRequest(req: Http.HttpRequest) {
        if (!enabled.value) return

        val requestUrl = unwrapProxyUrl(req.url)
        if (!isGithubUrl(requestUrl)) return

        val originalUrl = req.headers.get(headerOriginal) ?: requestUrl
        req.headers.put(headerOriginal, originalUrl)

        val isApi = isApiUrl(originalUrl)
        val canRetry = req.method == Http.HttpMethod.GET
        val attempt = req.headers.get(headerAttempt)?.toIntOrNull() ?: 0

        if (enableCache.value && !isApi && req.method == Http.HttpMethod.GET) {
            readCache(originalUrl)?.let {
                req.directResponse = Http.HttpResponse.ofBytes(200, it)
                return
            }
        }

        val proxies = selectEnabledProxies(isApi)
        if (proxies.isEmpty()) return

        val totalAttempts = minOf(maxRetries.value.coerceAtLeast(1), proxies.size)
        val currentAttempt = attempt.coerceIn(0, totalAttempts - 1)
        val proxy = proxies[currentAttempt]
        req.url = toProxyUrl(proxy, originalUrl)

        if (!canRetry) return
        val success = req.success ?: return
        val originalErrorHandler = req.errorHandler

        req.success = arc.func.ConsT { response ->
            if (enableCache.value && !isApi) {
                val length = response.contentLength
                if (length in 1..maxCacheBytes) {
                    writeCache(originalUrl, response.result)
                }
            }
            success.get(response)
        }

        req.error { error ->
            val nextAttempt = currentAttempt + 1
            if (nextAttempt >= totalAttempts) {
                originalErrorHandler.get(error)
                return@error
            }

            val next = cloneRequest(req, originalUrl)
            next.headers.put(headerAttempt, nextAttempt.toString())
            next.error(originalErrorHandler)
            next.submit(success)
        }
    }

    private fun cloneRequest(req: Http.HttpRequest, originalUrl: String): Http.HttpRequest {
        val next = Http.request(req.method, originalUrl)
        next.timeout = req.timeout
        next.followRedirects = req.followRedirects
        next.includeCredentials = req.includeCredentials
        next.content = req.content
        next.contentStream = req.contentStream
        req.headers.each { key, value ->
            if (key != headerAttempt) next.header(key, value)
        }
        return next
    }

    private fun selectEnabledProxies(isApi: Boolean): List<ProxyConfig> {
        val enabledProxies = proxyList.value.filter {
            it.enabled && if (isApi) it.apiEnabled else it.assetEnabled
        }
        if (enabledProxies.isEmpty()) return emptyList()
        if (isApi) return enabledProxies

        val direct = enabledProxies.filter { it.isDirect() }
        val mirrors = enabledProxies.filterNot { it.isDirect() }
        return mirrors + direct
    }

    private fun toProxyUrl(proxy: ProxyConfig, originalUrl: String): String {
        if (proxy.isDirect()) return originalUrl
        return proxy.url.trimEnd('/') + "/" + originalUrl
    }

    private fun isGithubUrl(url: String): Boolean = runCatching {
        val host = URL(url).host.lowercase()
        host == "github.com"
                || host.endsWith(".github.com")
                || host == "githubusercontent.com"
                || host.endsWith(".githubusercontent.com")
    }.getOrDefault(false)

    private fun isApiUrl(url: String): Boolean = runCatching {
        URL(url).host.equals("api.github.com", ignoreCase = true)
    }.getOrDefault(false)

    private fun unwrapProxyUrl(url: String): String {
        val match = wrappedUrlPattern.matchEntire(url) ?: return url
        val candidate = match.groupValues[1]
        return if (isGithubUrl(candidate)) candidate else url
    }

    private fun readCache(url: String): ByteArray? {
        val file = cacheFile(url)
        if (!file.exists()) return null
        val expireAt = file.lastModified() + cacheExpireMinutes.value * 60_000L
        if (System.currentTimeMillis() > expireAt) {
            file.delete()
            return null
        }
        return runCatching { file.readBytes() }.getOrNull()
    }

    private fun writeCache(url: String, data: ByteArray) {
        if (data.isEmpty() || data.size > maxCacheBytes) return
        val file = cacheFile(url)
        runCatching { file.writeBytes(data) }
            .onFailure { Log.warn("GitHub cache write failed: @", it.toString()) }
    }

    private fun cacheFile(url: String): Fi {
        val digest = MessageDigest.getInstance("SHA-1").digest(url.toByteArray(StandardCharsets.UTF_8))
        val name = digest.joinToString("") { "%02x".format(it) }
        return cacheRoot.child("$name.bin")
    }
}
