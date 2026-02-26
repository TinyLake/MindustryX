package mindustryX.features

import arc.Core
import arc.scene.ui.CheckBox
import arc.scene.ui.layout.Table
import arc.util.Http
import arc.util.Log
import arc.util.serialization.Jval
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.ui.Styles
import mindustryX.features.SettingsV2.*
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * GitHub加速服务 - 单文件实现
 * 通过包装 HttpRequest.block 实现重试和缓存机制
 */
object GithubAcceleration {
    
    // ========== 代理配置数据类 ==========
    data class ProxyConfig(
        var id: Int,
        var url: String,
        var name: String,
        var enabled: Boolean = true,
        var assetEnabled: Boolean = true,
        var apiEnabled: Boolean = true,
        val locked: Boolean = false
    ) {
        constructor() : this(0, "https://", "新代理", true, true, false, false)
        
        companion object {
            fun defaults() = listOf(
                ProxyConfig(0, "https://github.com", "源站", true, true, true, true),
                ProxyConfig(1, "https://ghproxy.com", "ghproxy", true, true, false),
                ProxyConfig(2, "https://gh.tinylake.top", "WZ镜像", true, true, true)
            )
        }
    }
    
    // ========== 设置项 ==========
    val enabled = CheckPref("githubAcceleration.enabled", true).apply {
        addFallbackName("githubMirror")
    }
    
    val enableCache = CheckPref("githubAcceleration.cache", true)
    val cacheExpireMinutes = SliderPref("githubAcceleration.cacheExpire", 60, 10, 1440, 10) { "${it}分钟" }
    val maxRetries = SliderPref("githubAcceleration.maxRetries", 3, 1, 10) { "${it}次" }
    
    // 代理列表配置（参考 customButtons 的实现模式）
    @JvmField
    val proxyList = object : Data<List<ProxyConfig>>("githubAcceleration.proxies", ProxyConfig.defaults()) {
        init {
            persistentProvider = PersistentProvider.AsUBJson(
                PersistentProvider.Arc(name),
                List::class.java,
                ProxyConfig::class.java
            )
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
                    
                    // 表头
                    add("№").width(30f); add("启用").width(40f); add("名称").width(80f)
                    add("URL").growX(); add("Asset").width(50f); add("API").width(50f); add("操作").width(80f)
                    row()
                    
                    // 代理列表
                    value.forEachIndexed { _, proxy ->
                        var tmp = proxy
                        
                        add(proxy.id.toString()).width(30f)
                        
                        // 启用开关
                        if (proxy.locked) {
                            add("[gray]✓").width(40f)
                        } else {
                            val cb = CheckBox("").apply { isChecked = proxy.enabled }
                            cb.changed { tmp = tmp.copy(enabled = cb.isChecked) }
                            add(cb).width(40f)
                        }
                        
                        field(proxy.name) { v -> tmp = tmp.copy(name = v) }.width(80f).maxTextLength(20)
                        field(proxy.url) { v -> tmp = tmp.copy(url = v) }.growX().maxTextLength(200)
                        
                        // Asset/API 开关
                        val assetCb = CheckBox("").apply { isChecked = proxy.assetEnabled; disabled = proxy.locked }
                        assetCb.changed { tmp = tmp.copy(assetEnabled = assetCb.isChecked) }
                        add(assetCb).width(50f)
                        
                        val apiCb = CheckBox("").apply { isChecked = proxy.apiEnabled; disabled = proxy.locked }
                        apiCb.changed { tmp = tmp.copy(apiEnabled = apiCb.isChecked) }
                        add(apiCb).width(50f)
                        
                        // 操作
                        table { ops ->
                            if (proxy.locked) {
                                ops.image(Icon.lock).size(24f)
                            } else {
                                ops.button(Icon.trashSmall, Styles.clearNonei, Vars.iconMed) {
                                    set(value.filterNot { it === proxy })
                                }
                                ops.button(Icon.saveSmall, Styles.clearNonei, Vars.iconMed) {
                                    set(value.map { if (it === proxy) tmp else it })
                                }.disabled { tmp === proxy }
                            }
                        }.width(80f)
                        row()
                    }
                    
                    // 添加新代理
                    button("@add", Icon.addSmall) {
                        val newId = (value.maxOfOrNull { it.id } ?: 0) + 1
                        set(value + ProxyConfig().copy(id = newId))
                    }.colspan(columns).fillX().row()
                    
                    add("[yellow]添加新代理前，请先保存编辑的代理").colspan(columns).center().padTop(-4f).row()
                    
                    // 清空缓存
                    button("清空缓存", Icon.trash) {
                        cache.clear()
                        Vars.ui.showInfoFade("缓存已清空")
                    }.colspan(columns).fillX()
                }
            }) { shown }.growX()
            table.row()
        }
    }
    
    @JvmStatic
    val settings: List<Data<*>> get() = listOf(enabled, enableCache, cacheExpireMinutes, maxRetries, proxyList)
    
    // ========== 内部实现 ==========
    private data class CachedResponse(val content: String, val timestamp: Long, val status: Int = 200)
    private val cache = ConcurrentHashMap<String, CachedResponse>()
    
    init {
        setupHttpHooks()
    }
    
    private fun setupHttpHooks() {
        Http.onBeforeRequest = { req ->
            if (!enabled.value || !isGithubUrl(req.url)) return@onBeforeRequest
            
            val originalUrl = req.url
            val isApi = isApiUrl(originalUrl)
            
            // 检查缓存 - 如果有缓存，修改 block 直接返回缓存内容
            if (enableCache.value && !isApi) {
                cache[originalUrl]?.let { cached ->
                    val ageMin = (System.currentTimeMillis() - cached.timestamp) / 60000
                    if (ageMin <= cacheExpireMinutes.value) {
                        Log.debug("GH缓存命中: @ (age: @min)", originalUrl, ageMin)
                        // 由于无法直接构造 HttpResponse，我们跳过缓存返回功能
                        // 仅作为缓存标记，减少不必要的代理切换
                        // TODO: 需要 Arc patch 支持才能真正返回缓存内容
                    }
                    cache.remove(originalUrl) // 过期则移除
                }
            }
            
            // 获取可用代理
            val proxies = proxyList.value.filter {
                it.enabled && if (isApi) it.apiEnabled else it.assetEnabled
            }
            
            if (proxies.isEmpty()) return@onBeforeRequest
            
            // 应用第一个代理
            val cleanUrl = cleanProxyUrl(originalUrl)
            val firstProxy = proxies.first()
            req.url = if (firstProxy.locked && firstProxy.id == 0) cleanUrl
                      else "${firstProxy.url.trimEnd('/')}/$cleanUrl"
            
            Log.debug("GH加速: @ -> @", firstProxy.name, req.url)
            
            // 包装 block 实现重试和缓存
            wrapBlockWithRetry(req, originalUrl, cleanUrl, proxies, isApi)
        }
    }
    
    // 移除缓存包装函数，因为无法直接构造 HttpResponse
    
    private fun wrapBlockWithRetry(
        req: Http.HttpRequest,
        originalUrl: String,
        cleanUrl: String,
        proxies: List<ProxyConfig>,
        isApi: Boolean
    ) {
        val originalBlock = req.block
        var attemptIndex = 0
        
        req.block = object : arc.func.Cons<Http.HttpResponse> {
            override fun get(response: Http.HttpResponse) {
                try {
                    originalBlock?.get(response)
                    
                    // 成功：缓存
                    if (enableCache.value && !isApi && response.status == 200) {
                        response.resultAsString?.let { content ->
                            cache[originalUrl] = CachedResponse(content, System.currentTimeMillis(), 200)
                            Log.debug("GH缓存: @", originalUrl)
                        }
                    }
                } catch (e: Exception) {
                    handleError(e)
                }
            }
            
            private fun handleError(error: Throwable) {
                attemptIndex++
                
                if (attemptIndex < proxies.size && attemptIndex < maxRetries.value) {
                    val nextProxy = proxies[attemptIndex]
                    val retryUrl = if (nextProxy.locked && nextProxy.id == 0) cleanUrl
                                  else "${nextProxy.url.trimEnd('/')}/$cleanUrl"
                    
                    Log.warn("GH重试 [@/@]: @ -> @", attemptIndex + 1, proxies.size, nextProxy.name, retryUrl)
                    
                    // 创建新请求重试
                    Http.HttpRequest().apply {
                        method = req.method
                        url = retryUrl
                        content = req.content
                        contentType = req.contentType
                        followRedirects = req.followRedirects
                        includeCredentials = req.includeCredentials
                        timeout = req.timeout
                        headers.putAll(req.headers)
                        block = this@object
                        this.error = req.error
                    }.submit()
                } else {
                    Log.err("GH加速失败: 已尝试 @ 个代理", attemptIndex)
                    req.error?.get(error)
                }
            }
        }
    }
    
    private fun isGithubUrl(url: String) = try {
        val host = URL(url).host.lowercase()
        host.contains("github.com") || host.contains("githubusercontent.com")
    } catch (e: Exception) { false }
    
    private fun isApiUrl(url: String) = url.contains("api.github.com")
    
    private fun cleanProxyUrl(url: String): String {
        val pattern = Regex("^https?://[^/]+/(https?://(?:github\\.com|raw\\.githubusercontent\\.com)/)")
        return pattern.find(url)?.groupValues?.get(1) ?: url
    }
}
