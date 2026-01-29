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
    ) { "$it${arc.Core.bundle.get("unit.minutes", "分钟")}" }
    
    val maxRetries = SettingsV2.SliderPref(
        "githubAcceleration.maxRetries",
        3, 1, 10
    ) { "$it ${arc.Core.bundle.get("unit.times", "次")}" }
    
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
            button(arc.Core.bundle.get("githubAcceleration.button.config"), Icon.settings, Styles.cleart) {
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
     * Process HTTP request with proxy
     * Note: This is a before-request hook, so we can only modify the URL,
     * not implement full retry logic here. Retry would need to be implemented
     * at a different level (e.g., in the HTTP client itself).
     * 
     * The cache currently tracks successfully accessed URLs to help identify
     * which proxies work best (future optimization).
     */
    fun processRequest(request: Http.HttpRequest) {
        if (!enabled.value) return
        
        val url = request.url
        if (!isGithubUrl(url)) return
        
        val isApi = isApiRequest(url)
        
        // Track previously successful URLs (for future optimization)
        if (enableCache.value && !isApi) {
            val cached = cache[url]
            if (cached != null && !cached.isExpired(cacheExpireMinutes.value)) {
                Log.debug("URL recently accessed: $url")
            }
        }
        
        // Get enabled proxies that support this request type
        val availableProxies = _proxies.filter { it.shouldProxy(url, isApi) }
        
        if (availableProxies.isEmpty()) {
            Log.debug("No available proxies for: $url")
            return
        }
        
        // Use first available proxy
        // TODO: Implement retry logic with fallback to other proxies
        val proxy = availableProxies.first()
        val proxiedUrl = applyProxyToUrl(url, proxy)
        
        if (proxiedUrl != url) {
            Log.debug("Proxying $url -> $proxiedUrl")
            request.url = proxiedUrl
        }
    }
    
    /**
     * Apply proxy to URL with better cleaning logic
     */
    private fun applyProxyToUrl(originalUrl: String, proxy: GithubProxyConfig): String {
        // If it's the source site (locked, id=0), don't proxy
        if (proxy.locked && proxy.id == 0) return originalUrl
        
        var cleanUrl = originalUrl
        
        // Check if URL is already proxied by looking for common proxy patterns
        // Pattern: https://proxy.com/https://github.com/...
        val doubleProxyPattern = Regex("^https?://[^/]+/(https?://(?:github\\.com|raw\\.githubusercontent\\.com)/)")
        val match = doubleProxyPattern.find(cleanUrl)
        
        if (match != null) {
            // Extract the GitHub URL part
            cleanUrl = match.groupValues[1]
        }
        
        // Apply new proxy
        val proxyBase = proxy.url.trimEnd('/')
        return "$proxyBase/$cleanUrl"
    }
    
    /**
     * Track successful URL access (for future proxy optimization)
     */
    fun trackSuccess(url: String) {
        if (enableCache.value && isGithubUrl(url)) {
            cache[url] = CachedResponse(System.currentTimeMillis())
        }
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
