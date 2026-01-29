package mindustryX.features

import arc.Core
import arc.scene.ui.layout.Table
import arc.util.Http
import arc.util.Log
import arc.util.serialization.Jval
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.ui.Styles
import mindustryX.features.SettingsV2.Data
import mindustryX.features.ui.GithubAccelerationDialog
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing GitHub acceleration proxies with retry logic and caching
 */
object GithubAccelerationService {
    
    private val cache = ConcurrentHashMap<String, CachedResponse>()
    
    // Settings
    val enabled = SettingsV2.CheckPref("githubAcceleration.enabled", true).apply {
        addFallbackName("githubMirror") // Migrate from old setting
    }
    
    val enableCache = SettingsV2.CheckPref("githubAcceleration.enableCache", true)
    val cacheExpireMinutes = SettingsV2.SliderPref(
        "githubAcceleration.cacheExpireMinutes", 
        60, 10, 1440, 10
    ) { "${it}分钟" }
    
    val maxRetries = SettingsV2.SliderPref(
        "githubAcceleration.maxRetries",
        3, 1, 10
    ) { "$it 次" }
    
    private val proxiesData = Data<String>("githubAcceleration.proxies", "").apply {
        persistentProvider = SettingsV2.PersistentProvider.Arc("githubAcceleration.proxies")
    }
    
    private var _proxies = mutableListOf<GithubProxyConfig>()
    
    val proxies: List<GithubProxyConfig>
        get() = _proxies.toList()
    
    private val dialog by lazy { GithubAccelerationDialog() }
    
    // Custom settings entry that acts as a button
    val configSetting = object : Data<Boolean>("githubAcceleration.config", false) {
        override fun buildUI() = Table().apply {
            button("⚙ 配置 GH 加速代理", Icon.settings, Styles.cleart) {
                dialog.show()
            }.growX().height(50f)
        }
    }
    
    init {
        loadProxies()
    }
    
    /**
     * Load proxies from settings
     */
    private fun loadProxies() {
        try {
            val json = proxiesData.value
            if (json.isNotEmpty()) {
                val array = Jval.read(json).asArray()
                _proxies.clear()
                array.forEach {
                    _proxies.add(GithubProxyConfig.fromJson(it))
                }
            }
            
            // If empty, use defaults
            if (_proxies.isEmpty()) {
                _proxies.addAll(GithubProxyConfig.getDefaults())
                saveProxies()
            }
        } catch (e: Exception) {
            Log.err("Failed to load GitHub proxies, using defaults", e)
            _proxies.clear()
            _proxies.addAll(GithubProxyConfig.getDefaults())
        }
    }
    
    /**
     * Save proxies to settings
     */
    fun saveProxies() {
        try {
            val array = Jval.newArray()
            _proxies.forEach {
                array.add(it.toJson())
            }
            proxiesData.set(array.toString(Jval.Jformat.formatted))
        } catch (e: Exception) {
            Log.err("Failed to save GitHub proxies", e)
        }
    }
    
    /**
     * Add a new proxy
     */
    fun addProxy(url: String, name: String, assetEnabled: Boolean = true, apiEnabled: Boolean = true) {
        val newId = (_proxies.maxOfOrNull { it.id } ?: 0) + 1
        _proxies.add(GithubProxyConfig(
            id = newId,
            url = url.trimEnd('/'),
            name = name,
            enabled = true,
            assetEnabled = assetEnabled,
            apiEnabled = apiEnabled
        ))
        saveProxies()
    }
    
    /**
     * Remove a proxy by ID
     */
    fun removeProxy(id: Int) {
        _proxies.removeIf { it.id == id && !it.locked }
        saveProxies()
    }
    
    /**
     * Update a proxy
     */
    fun updateProxy(proxy: GithubProxyConfig) {
        val index = _proxies.indexOfFirst { it.id == proxy.id }
        if (index >= 0) {
            _proxies[index] = proxy
            saveProxies()
        }
    }
    
    /**
     * Check if URL is a GitHub URL
     */
    private fun isGithubUrl(url: String): Boolean {
        return try {
            val host = URL(url).host.lowercase()
            host.contains("github.com") || host.contains("githubusercontent.com")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if it's an API request
     */
    private fun isApiRequest(url: String): Boolean {
        return url.contains("api.github.com")
    }
    
    /**
     * Check if it's an asset request (releases, raw content)
     */
    private fun isAssetRequest(url: String): Boolean {
        return url.contains("/releases/") || 
               url.contains("raw.githubusercontent.com") ||
               url.contains("/archive/") ||
               url.contains("/download/")
    }
    
    /**
     * Process HTTP request with proxy and retry logic
     */
    fun processRequest(request: Http.HttpRequest) {
        if (!enabled.value) return
        
        val url = request.url
        if (!isGithubUrl(url)) return
        
        val isApi = isApiRequest(url)
        
        // Check cache first
        if (enableCache.value && !isApi) {
            val cached = cache[url]
            if (cached != null && !cached.isExpired(cacheExpireMinutes.value)) {
                Log.debug("Using cached response for: $url")
                // Note: We can't directly return cached data here as it's a before-request hook
                // The cache is mainly useful for tracking which URLs work
                return
            }
        }
        
        // Get enabled proxies that support this request type
        val availableProxies = _proxies.filter { it.shouldProxy(url, isApi) }
        
        if (availableProxies.isEmpty()) {
            Log.debug("No available proxies for: $url")
            return
        }
        
        // Use first available proxy (will implement retry in actual HTTP execution)
        val proxy = availableProxies.first()
        val proxiedUrl = applyProxyToUrl(url, proxy)
        
        if (proxiedUrl != url) {
            Log.debug("Proxying $url -> $proxiedUrl")
            request.url = proxiedUrl
        }
    }
    
    /**
     * Apply proxy to URL
     */
    private fun applyProxyToUrl(url: String, proxy: GithubProxyConfig): String {
        // If it's the source site (locked, id=0), don't proxy
        if (proxy.locked && proxy.id == 0) return url
        
        // Remove any existing proxy prefix first
        var cleanUrl = url
        
        // Pattern to detect if URL is already proxied
        val proxyPrefixPattern = Regex("^(https?://[^/]+/)+(https?://)")
        val match = proxyPrefixPattern.find(cleanUrl)
        
        if (match != null) {
            // Extract the original GitHub URL
            val lastHttpIndex = cleanUrl.lastIndexOf("http")
            if (lastHttpIndex > 0) {
                cleanUrl = cleanUrl.substring(lastHttpIndex)
            }
        }
        
        // Apply new proxy
        val proxyBase = proxy.url.trimEnd('/')
        return "$proxyBase/$cleanUrl"
    }
    
    /**
     * Cache response
     */
    fun cacheResponse(url: String) {
        if (enableCache.value) {
            cache[url] = CachedResponse(System.currentTimeMillis())
        }
    }
    
    /**
     * Clear cache
     */
    fun clearCache() {
        cache.clear()
    }
    
    /**
     * Cached response data
     */
    private data class CachedResponse(
        val timestamp: Long
    ) {
        fun isExpired(expireMinutes: Int): Boolean {
            val age = (System.currentTimeMillis() - timestamp) / 60000 // minutes
            return age > expireMinutes
        }
    }
}
