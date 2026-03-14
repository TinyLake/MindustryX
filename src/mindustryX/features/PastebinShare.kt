package mindustryX.features

import arc.Core
import arc.scene.ui.CheckBox
import arc.scene.ui.TextButton
import arc.scene.ui.layout.Table
import arc.util.Http
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.ui.Styles
import mindustryX.VarsX
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

object PastebinShare {
    private const val typePastebin = "pastebin"
    private const val typeDirect = "direct"
    private const val pastebinDevKey = "sdBDjI5mWBnHl9vBEDMNiYQ3IZe0LFEk"
    private const val directUploadUserAgent = "MindustryX-schematic-share"
    private var preferredSourceId = 0

    data class Source(
        val id: Int,
        val name: String,
        val baseUrl: String,
        val enabled: Boolean,
        val type: String,
        val expire: String
    ) {
        constructor() : this(0, "", "https://pastebin.com", true, typePastebin, "10M")

        fun normalizedBaseUrl(): String = normalizeBaseUrl(baseUrl)
        fun normalizedType(): String = normalizeType(type)
        fun normalizedExpire(): String = normalizeExpire(expire, normalizedType())

        fun displayName(): String {
            val trimmedName = name.trim()
            if (trimmedName.isNotEmpty()) return trimmedName

            val base = normalizedBaseUrl()
            return runCatching {
                URL(base).host.takeIf { it.isNotBlank() } ?: base
            }.getOrDefault(base.ifBlank { "source-$id" })
        }
    }

    data class ShareLink(
        val source: Source,
        val id: String,
        val link: String
    ) {
        val baseUrl: String get() = source.normalizedBaseUrl()
        val sourceType: String get() = source.normalizedType()

        fun useLegacyMessage(): Boolean = sourceType == typePastebin && isPastebinDotCom(baseUrl)

        fun chatPayload(): String {
            return if (useLegacyMessage()) {
                "<ARCxMDTX><Schem>[black]一坨兼容[] $id"
            } else {
                "<ARCxMDTX><SchemV2> $sourceType $baseUrl $id"
            }
        }
    }

    @JvmField
    val sources = object : SettingsV2.Data<List<Source>>("arcExtra.schematicShare.pastebinSources", defaults()) {
        init {
            persistentProvider = SettingsV2.PersistentProvider.AsUBJson(
                SettingsV2.PersistentProvider.Arc(name),
                List::class.java,
                Source::class.java
            )
        }

        override fun set(value: List<Source>) {
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

                    add("#").width(24f)
                    add("启用").width(36f)
                    add("类型").width(76f)
                    add("名称 / 地址").growX()
                    add("过期").width(72f)
                    add("操作").width(78f)
                    row()

                    value.forEachIndexed { index, source ->
                        var edited = source
                        add(index.toString()).width(24f)

                        val enabledBox = CheckBox("")
                        enabledBox.isChecked = source.enabled
                        enabledBox.changed { edited = edited.copy(enabled = enabledBox.isChecked) }
                        add(enabledBox).width(36f)

                        val typeButton = TextButton("", Styles.togglet)
                        typeButton.clicked {
                            val nextType = nextType(edited.type)
                            val keepExpire = edited.expire.trim().isNotEmpty() && edited.expire != normalizeExpire(edited.expire, edited.type)
                            edited = edited.copy(
                                type = nextType,
                                expire = if (keepExpire) edited.expire.trim() else defaultExpire(nextType)
                            )
                        }
                        typeButton.update { typeButton.setText(typeLabel(edited.type)) }
                        add(typeButton).width(76f)

                        table {
                            it.defaults().growX().left()
                            it.field(source.name) { text -> edited = edited.copy(name = text) }.maxTextLength(32).row()
                            it.field(source.baseUrl) { text -> edited = edited.copy(baseUrl = text) }.maxTextLength(240)
                        }.growX()

                        field(source.expire) { text -> edited = edited.copy(expire = text) }.maxTextLength(24).width(72f)

                        table { ops ->
                            ops.button(Icon.trashSmall, Styles.clearNonei, Vars.iconMed) {
                                set(value.filterNot { it === source })
                            }
                            ops.button(Icon.saveSmall, Styles.clearNonei, Vars.iconMed) {
                                set(value.map { if (it === source) edited else it })
                            }.disabled { normalizeOne(edited) == source }
                        }.width(78f)
                        row()
                    }

                    button("@add", Icon.addSmall) {
                        val nextId = (value.maxOfOrNull { it.id } ?: -1) + 1
                        set(value + Source(nextId, "新分享源", "https://", true, typePastebin, "10M"))
                    }.colspan(columns).fillX().row()
                    add("[yellow]0x0.st 直链更稳定，但仍是公共文件托管服务").colspan(columns).center().padTop(-4f).row()
                }
            }) { shown }.growX()
            root.row()
        }
    }

    fun upload(content: String, callback: (ShareLink?) -> Unit) {
        val candidates = orderedEnabledSources()
        if (candidates.isEmpty()) {
            Core.app.post {
                Vars.ui.showInfo("未配置启用的蓝图分享源")
                callback(null)
            }
            return
        }

        val failures = mutableListOf<String>()
        tryUpload(candidates, 0, content, failures, callback)
    }

    fun download(
        sourceType: String,
        baseUrl: String,
        id: String,
        callback: (String) -> Unit,
        failed: (Throwable) -> Unit
    ) {
        when (normalizeType(sourceType)) {
            typeDirect -> downloadDirect(baseUrl, id, callback, failed)
            else -> downloadPastebinLike(baseUrl, id, callback, failed)
        }
    }

    private fun tryUpload(
        candidates: List<Source>,
        index: Int,
        content: String,
        failures: MutableList<String>,
        callback: (ShareLink?) -> Unit
    ) {
        if (index >= candidates.size) {
            Core.app.post {
                Vars.ui.showInfo(
                    buildString {
                        append("蓝图上传失败")
                        if (failures.isNotEmpty()) {
                            append("：\n")
                            append(failures.joinToString("\n"))
                        }
                    }
                )
                callback(null)
            }
            return
        }

        val source = candidates[index]
        val onFailure: (String) -> Unit = { reason ->
            failures += "${source.displayName()}: $reason"
            tryUpload(candidates, index + 1, content, failures, callback)
        }

        if (source.normalizedType() == typeDirect) {
            uploadDirectHost(source, content, callback, onFailure)
        } else {
            uploadPastebinLike(source, content, callback, onFailure)
        }
    }

    private fun uploadPastebinLike(
        source: Source,
        content: String,
        callback: (ShareLink?) -> Unit,
        failed: (String) -> Unit
    ) {
        val baseUrl = source.normalizedBaseUrl()
        val expire = source.normalizedExpire()
        val body = formEncode(
            "api_dev_key" to pastebinDevKey,
            "api_option" to "paste",
            "api_paste_expire_date" to expire,
            "api_paste_code" to content
        )

        Http.post("$baseUrl/api/api_post.php")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .content(body)
            .timeout(10000)
            .error { failed(extractHttpError(it)) }
            .submit { res ->
                val raw = res.resultAsString.trim()
                val shareLink = parsePastebinResponse(source, raw)
                if (shareLink == null) {
                    failed(raw.ifBlank { "返回内容为空" })
                    return@submit
                }
                preferredSourceId = source.id
                Core.app.post { callback(shareLink) }
            }
    }

    private fun uploadDirectHost(
        source: Source,
        content: String,
        callback: (ShareLink?) -> Unit,
        failed: (String) -> Unit
    ) {
        val baseUrl = source.normalizedBaseUrl()
        val expire = source.normalizedExpire()
        val timestamp = System.currentTimeMillis()
        val boundary = "----MindustryX${UUID.randomUUID()}"
        val body = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"file\"; filename=\"schematic-").append(timestamp).append(".txt\"\r\n")
            append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
            append(content).append("\r\n")

            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"secret\"\r\n\r\n")
            append("1\r\n")

            if (expire.isNotBlank()) {
                append("--").append(boundary).append("\r\n")
                append("Content-Disposition: form-data; name=\"expires\"\r\n\r\n")
                append(expire).append("\r\n")
            }

            append("--").append(boundary).append("--\r\n")
        }

        Http.post(baseUrl)
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .header("User-Agent", "$directUploadUserAgent/${VarsX.version}")
            .content(body)
            .timeout(15000)
            .error { failed(extractHttpError(it)) }
            .submit { res ->
                val raw = res.resultAsString.trim()
                val shareLink = parseDirectResponse(source, raw)
                if (shareLink == null) {
                    failed(raw.ifBlank { "返回内容为空" })
                    return@submit
                }
                preferredSourceId = source.id
                Core.app.post { callback(shareLink) }
            }
    }

    private fun downloadPastebinLike(
        baseUrl: String,
        id: String,
        callback: (String) -> Unit,
        failed: (Throwable) -> Unit
    ) {
        Http.get("${normalizeBaseUrl(baseUrl)}/raw/$id")
            .timeout(10000)
            .error(failed)
            .submit { res ->
                val content = res.resultAsString.replace(" ", "+")
                Core.app.post { callback(content) }
            }
    }

    private fun downloadDirect(
        baseUrl: String,
        id: String,
        callback: (String) -> Unit,
        failed: (Throwable) -> Unit
    ) {
        Http.get("${normalizeBaseUrl(baseUrl)}/$id")
            .timeout(10000)
            .error(failed)
            .submit { res ->
                val content = res.resultAsString.replace(" ", "+")
                Core.app.post { callback(content) }
            }
    }

    private fun parsePastebinResponse(source: Source, raw: String): ShareLink? {
        if (raw.isBlank() || raw.startsWith("Bad API request", ignoreCase = true)) return null

        val id = when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw.substringBefore('?').substringAfterLast('/').trim()
            raw.matches(Regex("[A-Za-z0-9]+")) -> raw
            else -> return null
        }
        if (id.isBlank()) return null

        val baseUrl = source.normalizedBaseUrl()
        return ShareLink(source, id, "$baseUrl/$id")
    }

    private fun parseDirectResponse(source: Source, raw: String): ShareLink? {
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) return null

        val url = runCatching { URL(raw) }.getOrNull() ?: return null
        val path = url.path.trim('/').takeIf { it.isNotBlank() } ?: return null
        return ShareLink(source, path, raw)
    }

    private fun orderedEnabledSources(): List<Source> {
        val enabled = sources.value.filter { it.enabled && it.normalizedBaseUrl().isNotBlank() }
        if (enabled.isEmpty()) return emptyList()

        val preferred = enabled.firstOrNull { it.id == preferredSourceId } ?: enabled.first()
        return listOf(preferred) + enabled.filterNot { it.id == preferred.id }
    }

    private fun normalize(raw: List<Source>): List<Source> {
        return raw.mapNotNull { normalizeOne(it) }
    }

    private fun normalizeOne(source: Source): Source? {
        val baseUrl = normalizeBaseUrl(source.baseUrl)
        if (baseUrl.isBlank()) return null

        val type = normalizeType(source.type)
        return source.copy(
            name = source.name.trim(),
            baseUrl = baseUrl,
            type = type,
            expire = normalizeExpire(source.expire, type)
        )
    }

    private fun defaults(): List<Source> = listOf(
        Source(0, "pastebin.com", "https://pastebin.com", true, typePastebin, "10M"),
        Source(1, "0x0.st 备用", "https://0x0.st", true, typeDirect, "24")
    )

    private fun nextType(type: String): String = if (normalizeType(type) == typePastebin) typeDirect else typePastebin

    private fun typeLabel(type: String): String = when (normalizeType(type)) {
        typeDirect -> "0x0.st"
        else -> "Pastebin"
    }

    private fun defaultExpire(type: String): String = when (normalizeType(type)) {
        typeDirect -> "24"
        else -> "10M"
    }

    private fun normalizeExpire(expire: String, type: String): String {
        val trimmed = expire.trim()
        return if (trimmed.isNotEmpty()) trimmed else defaultExpire(type)
    }

    private fun normalizeType(type: String): String {
        return if (type.equals(typeDirect, ignoreCase = true)) typeDirect else typePastebin
    }

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank() || trimmed == "https://" || trimmed == "http://") return ""
        return when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
    }

    private fun isPastebinDotCom(baseUrl: String): Boolean = runCatching {
        val host = URL(baseUrl).host.lowercase()
        host == "pastebin.com" || host == "www.pastebin.com"
    }.getOrDefault(false)

    private fun formEncode(vararg parts: Pair<String, String>): String {
        return parts.joinToString("&") { (key, value) ->
            "${urlEncode(key)}=${urlEncode(value)}"
        }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun extractHttpError(error: Throwable): String {
        if (error is Http.HttpStatusException) {
            val body = error.response.getResultAsString().trim()
            return buildString {
                append("HTTP ").append(error.status.code)
                if (body.isNotBlank()) append(": ").append(body)
            }
        }
        return error.message ?: error.toString()
    }
}
