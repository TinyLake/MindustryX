package mindustryX.features

import arc.Core
import arc.scene.ui.CheckBox
import arc.scene.ui.TextField
import arc.scene.ui.layout.Table
import arc.struct.Seq
import arc.util.Http
import arc.util.Log
import arc.util.serialization.Jval
import mindustry.gen.Icon
import mindustry.gen.Tex
import mindustry.graphics.Pal
import mindustry.ui.Styles
import mindustryX.features.SettingsV2.*
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * GitHub加速服务 - 支持多镜像站、重试机制和缓存
 * 
 * 功能：
 * 1. 多镜像站配置（源站、ghproxy、自定义）
 * 2. 失败自动重试，fallback到下一个可用镜像
 * 3. 静态资源缓存（Asset）
 * 4. 独立的Asset/API开关
 */
object GithubAcceleration {
    
    // ========== 代理配置 ==========
    data class ProxyConfig(
        var id: Int,
        var url: String,
        var name: String,
        var enabled: Boolean = true,
        var assetEnabled: Boolean = true,
        var apiEnabled: Boolean = true,
        val locked: Boolean = false
    ) {
        fun toJson() = Jval.newObject().apply {
            add("id", id)
            add("url", url)
            add("name", name)
            add("enabled", enabled)
            add("assetEnabled", assetEnabled)
            add("apiEnabled", apiEnabled)
            add("locked", locked)
        }
        
        companion object {
            fun fromJson(json: Jval) = ProxyConfig(
                id = json.getInt("id", 0),
                url = json.getString("url", ""),
                name = json.getString("name", ""),
                enabled = json.getBool("enabled", true),
                assetEnabled = json.getBool("assetEnabled", true),
                apiEnabled = json.getBool("apiEnabled", true),
                locked = json.getBool("locked", false)
            )
            
            fun defaults() = listOf(
                ProxyConfig(0, "https://github.com", "源站", true, true, true, true),
                ProxyConfig(1, "https://ghproxy.com", "ghproxy", true, true, false),
                ProxyConfig(2, "https://gh.tinylake.top", "WZ镜像", true, true, true)
            )
        }
    }
    
    // ========== 缓存数据 ==========
    private data class CachedResponse(
        val resultAsString: String,
        val timestamp: Long,
        val status: Int = 200
    )
    
    // ========== 设置项 ==========
    val enabled = CheckPref("githubAcceleration.enabled", true).apply {
        addFallbackName("githubMirror") // 兼容旧设置
    }
    
    val enableCache = CheckPref("githubAcceleration.cache", true)
    val cacheExpireMinutes = SliderPref("githubAcceleration.cacheExpire", 60, 10, 1440, 10) { 
        "${it}分钟" 
    }
    
    val maxRetries = SliderPref("githubAcceleration.maxRetries", 3, 1, 10) { 
        "${it}次" 
    }
    
    // 代理配置按钮（显示在QuickSettings中）
    private val configButton = object : Data<Boolean>("githubAcceleration.proxyConfig", false) {
        override fun buildUI() = Table().apply {
            button("配置代理列表", Styles.cleart) {
                showProxyConfigDialog()
            }.growX().height(40f)
        }
    }
    
    private val proxiesData = Data<String>("githubAcceleration.proxies", "").apply {
        persistentProvider = PersistentProvider.Arc("githubAcceleration.proxies")
    }
    
    // ========== 代理列表 ==========
    private var proxies = mutableListOf<ProxyConfig>()
    
    // QuickSettings使用的设置列表
    @JvmStatic
    val settings: List<Data<*>> get() = listOf(enabled, enableCache, cacheExpireMinutes, maxRetries, configButton)
    
    // ========== 缓存 ==========
    private val cache = ConcurrentHashMap<String, CachedResponse>()
    
    init {
        loadProxies()
        setupHttpHooks()
    }
    
    // ========== 核心逻辑：通过包装block实现重试和缓存 ==========
    private fun setupHttpHooks() {
        // 请求前：修改URL并包装block实现重试和缓存
        Http.onBeforeRequest = { req ->
            if (enabled.value && isGithubUrl(req.url)) {
                val originalUrl = req.url
                val isApi = isApiUrl(originalUrl)
                
                // 检查缓存
                if (enableCache.value && !isApi) {
                    val cached = cache[originalUrl]
                    if (cached != null) {
                        val age = (System.currentTimeMillis() - cached.timestamp) / 60000
                        if (age <= cacheExpireMinutes.value) {
                            Log.debug("使用缓存: @ (age: @ min)", originalUrl, age)
                            // 直接返回缓存内容
                            val originalBlock = req.block
                            req.block = { res ->
                                // 包装响应对象，使用缓存数据
                                val cachedRes = object : Http.HttpResponse(res.connection) {
                                    override fun getResultAsString() = cached.resultAsString
                                    override fun getStatus() = cached.status
                                }
                                originalBlock?.get(cachedRes)
                            }
                            return@onBeforeRequest
                        } else {
                            cache.remove(originalUrl)
                        }
                    }
                }
                
                // 获取可用代理列表
                val available = proxies.filter { 
                    it.enabled && when {
                        isApi -> it.apiEnabled
                        else -> it.assetEnabled
                    }
                }
                
                if (available.isEmpty()) return@onBeforeRequest
                
                // 应用第一个代理
                val cleanUrl = cleanProxyUrl(originalUrl)
                val firstProxy = available.first()
                req.url = if (firstProxy.locked && firstProxy.id == 0) cleanUrl
                          else "${firstProxy.url.trimEnd('/')}/$cleanUrl"
                
                Log.debug("GitHub加速: @ -> @", firstProxy.name, req.url)
                
                // 包装block实现重试和缓存
                val originalBlock = req.block
                var attemptIndex = 0
                
                req.block = object : arc.func.Cons<Http.HttpResponse> {
                    override fun get(response: Http.HttpResponse) {
                        try {
                            // 调用原始处理器
                            originalBlock?.get(response)
                            
                            // 成功：缓存响应
                            if (enableCache.value && !isApi && response.status == 200) {
                                try {
                                    val content = response.resultAsString
                                    if (content != null) {
                                        cache[originalUrl] = CachedResponse(content, System.currentTimeMillis(), 200)
                                        Log.debug("缓存GitHub资源: @", originalUrl)
                                    }
                                } catch (e: Exception) {
                                    // 缓存失败不影响请求
                                }
                            }
                        } catch (e: Exception) {
                            // 请求失败：尝试重试
                            handleError(e)
                        }
                    }
                    
                    private fun handleError(error: Throwable) {
                        attemptIndex++
                        
                        if (attemptIndex < available.size && attemptIndex < maxRetries.value) {
                            val nextProxy = available[attemptIndex]
                            val retryUrl = if (nextProxy.locked && nextProxy.id == 0) cleanUrl
                                          else "${nextProxy.url.trimEnd('/')}/$cleanUrl"
                            
                            Log.warn("GitHub加速重试 [@/@]: @ -> @", attemptIndex + 1, available.size, nextProxy.name, retryUrl)
                            
                            // 创建新请求重试
                            val retryReq = Http.HttpRequest()
                            retryReq.method = req.method
                            retryReq.url = retryUrl
                            retryReq.content = req.content
                            retryReq.contentType = req.contentType
                            retryReq.followRedirects = req.followRedirects
                            retryReq.includeCredentials = req.includeCredentials
                            retryReq.timeout = req.timeout
                            retryReq.headers.putAll(req.headers)
                            retryReq.block = this // 使用同一个处理器继续重试
                            retryReq.error = req.error
                            
                            retryReq.submit()
                        } else {
                            // 所有代理都失败了
                            Log.err("GitHub加速失败: 已尝试 @ 个代理", attemptIndex)
                            req.error?.get(error)
                        }
                    }
                }
            }
        }
    }
    
    // ========== 工具方法 ==========
    private fun isGithubUrl(url: String): Boolean = try {
        val host = URL(url).host.lowercase()
        host.contains("github.com") || host.contains("githubusercontent.com")
    } catch (e: Exception) { false }
    
    private fun isApiUrl(url: String) = url.contains("api.github.com")
    
    private fun cleanProxyUrl(url: String): String {
        // 移除已有代理前缀：https://proxy.com/https://github.com/...
        val pattern = Regex("^https?://[^/]+/(https?://(?:github\\.com|raw\\.githubusercontent\\.com)/)")
        return pattern.find(url)?.groupValues?.get(1) ?: url
    }
    
    // ========== 代理管理 ==========
    fun loadProxies() {
        try {
            val json = proxiesData.value
            if (json.isNotEmpty()) {
                proxies.clear()
                Jval.read(json).asArray().forEach {
                    proxies.add(ProxyConfig.fromJson(it))
                }
            }
            if (proxies.isEmpty()) {
                proxies.addAll(ProxyConfig.defaults())
                saveProxies()
            }
        } catch (e: Exception) {
            Log.err("加载GitHub代理配置失败", e)
            proxies.clear()
            proxies.addAll(ProxyConfig.defaults())
        }
    }
    
    fun saveProxies() {
        try {
            val array = Jval.newArray()
            proxies.forEach { array.add(it.toJson()) }
            proxiesData.set(array.toString(Jval.Jformat.formatted))
            Log.info("保存GitHub代理配置: @ 个", proxies.size)
        } catch (e: Exception) {
            Log.err("保存GitHub代理配置失败", e)
        }
    }
    
    fun addProxy(url: String, name: String) {
        val id = (proxies.maxOfOrNull { it.id } ?: 0) + 1
        proxies.add(ProxyConfig(id, url.trimEnd('/'), name))
        saveProxies()
    }
    
    fun removeProxy(id: Int) {
        proxies.removeIf { it.id == id && !it.locked }
        saveProxies()
    }
    
    fun clearCache() {
        cache.clear()
        Log.info("清空GitHub缓存")
    }
    
    // ========== QuickSettings: 代理配置对话框 ==========
    private fun showProxyConfigDialog() {
        val dialog = mindustry.ui.dialogs.BaseDialog("GH 加速代理配置")
        
        dialog.cont.pane { pane ->
            pane.table(Tex.button) { t ->
                t.defaults().pad(4f).left()
                
                // 标题
                t.add("GH 加速配置").color(Pal.accent).colspan(6).center().row()
                t.image().color(Pal.accent).fillX().colspan(6).height(3f).pad(4f).row()
                
                // 表头
                t.add("№").width(40f)
                t.add("启用").width(50f)
                t.add("镜像地址").minWidth(150f).growX()
                t.add("Asset").width(60f)
                t.add("API").width(60f)
                t.add("操作").width(80f).row()
                
                t.image().fillX().colspan(6).height(2f).row()
                
                // 代理列表
                proxies.forEach { proxy ->
                    // 序号
                    t.add("${proxy.id}").width(40f)
                    
                    // 启用开关
                    if (proxy.locked) {
                        t.add("[gray]✓[]").width(50f)
                    } else {
                        val cb = CheckBox("").apply { isChecked = proxy.enabled }
                        cb.changed { 
                            proxy.enabled = cb.isChecked
                            saveProxies()
                        }
                        t.add(cb).width(50f)
                    }
                    
                    // 名称和URL
                    t.table { inner ->
                        inner.add(proxy.name).left().row()
                        inner.add("[gray]${proxy.url}[]").left().labelAlign(arc.util.Align.left).row()
                    }.minWidth(150f).growX()
                    
                    // Asset开关
                    val assetCb = CheckBox("").apply { isChecked = proxy.assetEnabled }
                    assetCb.changed { 
                        proxy.assetEnabled = assetCb.isChecked
                        saveProxies()
                    }
                    assetCb.disabled = proxy.locked
                    t.add(assetCb).width(60f)
                    
                    // API开关
                    val apiCb = CheckBox("").apply { isChecked = proxy.apiEnabled }
                    apiCb.changed { 
                        proxy.apiEnabled = apiCb.isChecked
                        saveProxies()
                    }
                    apiCb.disabled = proxy.locked
                    t.add(apiCb).width(60f)
                    
                    // 操作按钮
                    t.table { ops ->
                        if (proxy.locked) {
                            ops.image(Icon.lock).size(24f)
                        } else {
                            ops.button(Icon.trash, Styles.cleari, 24f) {
                                removeProxy(proxy.id)
                                dialog.hide()
                                Core.app.post { showProxyConfigDialog() }
                            }.size(32f)
                        }
                    }.width(80f).row()
                }
                
                t.image().fillX().colspan(6).height(2f).row()
                
                // 底部按钮
                t.table { bottom ->
                    bottom.button("+ 添加代理", Styles.cleart) {
                        dialog.hide()
                        showAddDialog()
                    }.fillX()
                    
                    bottom.button("清空缓存", Styles.cleart) {
                        clearCache()
                        mindustry.Vars.ui.showInfoFade("缓存已清空")
                    }.fillX()
                }.colspan(6).fillX().row()
                
                // 提示
                t.add("[yellow]修改后自动保存[]").colspan(6).center().pad(8f).row()
            }.grow()
        }.grow()
        
        dialog.addCloseButton()
        dialog.show()
    }
    
    private fun showAddDialog() {
        val dialog = mindustry.ui.dialogs.BaseDialog("添加GitHub代理")
        
        var urlField: TextField? = null
        var nameField: TextField? = null
        
        dialog.cont.table { t ->
            t.add("镜像URL:").left().row()
            urlField = t.field("https://") { }.growX().get()
            t.row()
            
            t.add("备注名称:").left().row()
            nameField = t.field("") { }.growX().get()
        }
        
        dialog.buttons.defaults().size(120f, 50f)
        dialog.buttons.button("取消") { dialog.hide() }
        dialog.buttons.button("添加") {
            val url = urlField?.text?.trim() ?: ""
            val name = nameField?.text?.trim()?.ifEmpty { url } ?: url
            
            if (url.isNotEmpty()) {
                addProxy(url, name)
                dialog.hide()
            }
        }
        
        dialog.show()
    }
}
