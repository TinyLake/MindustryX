package mindustryX.features

import arc.util.serialization.Jval
import java.io.Serializable

/**
 * Configuration for a GitHub proxy mirror
 */
data class GithubProxyConfig(
    val id: Int,
    val url: String,
    val name: String,
    var enabled: Boolean,
    var assetEnabled: Boolean,
    var apiEnabled: Boolean,
    val locked: Boolean = false
) : Serializable {
    
    companion object {
        const val serialVersionUID = 1L
        
        /**
         * Default proxy configurations
         */
        fun getDefaults(): List<GithubProxyConfig> {
            return listOf(
                GithubProxyConfig(
                    id = 0,
                    url = "https://github.com",
                    name = "源站 (github.com)",
                    enabled = true,
                    assetEnabled = true,
                    apiEnabled = true,
                    locked = true
                ),
                GithubProxyConfig(
                    id = 1,
                    url = "https://ghproxy.com",
                    name = "ghproxy.com",
                    enabled = true,
                    assetEnabled = true,
                    apiEnabled = false
                ),
                GithubProxyConfig(
                    id = 2,
                    url = "https://gh.tinylake.top",
                    name = "WZ镜像",
                    enabled = true,
                    assetEnabled = true,
                    apiEnabled = true
                )
            )
        }
        
        fun fromJson(json: Jval): GithubProxyConfig {
            return GithubProxyConfig(
                id = json.getInt("id", 0),
                url = json.getString("url", ""),
                name = json.getString("name", ""),
                enabled = json.getBool("enabled", true),
                assetEnabled = json.getBool("assetEnabled", true),
                apiEnabled = json.getBool("apiEnabled", true),
                locked = json.getBool("locked", false)
            )
        }
    }
    
    fun toJson(): Jval {
        val json = Jval.newObject()
        json.add("id", id)
        json.add("url", url)
        json.add("name", name)
        json.add("enabled", enabled)
        json.add("assetEnabled", assetEnabled)
        json.add("apiEnabled", apiEnabled)
        json.add("locked", locked)
        return json
    }
    
    /**
     * Check if this proxy should be used for the given URL
     */
    fun shouldProxy(url: String, isApiRequest: Boolean): Boolean {
        if (!enabled) return false
        if (locked && id == 0) return false // Never proxy through source site
        
        return if (isApiRequest) apiEnabled else assetEnabled
    }
}
