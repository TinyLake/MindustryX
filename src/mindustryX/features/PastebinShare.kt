package mindustryX.features

import arc.Core
import arc.scene.ui.CheckBox
import arc.scene.ui.TextButton
import arc.scene.ui.layout.Table
import arc.util.Http
import arc.util.Log
import arc.util.serialization.Jval
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.ui.Styles
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object PastebinShare {
    private const val typePastebin = "pastebin"
    private const val typePrivateBin = "privatebin"
    private const val pastebinDevKey = "sdBDjI5mWBnHl9vBEDMNiYQ3IZe0LFEk"
    private const val privateBinRequestedWith = "JSONHttpRequest"
    private const val privateBinIterations = 100000
    private const val privateBinKeyBits = 256
    private const val privateBinKeyBytes = privateBinKeyBits / 8
    private const val privateBinTagBits = 128
    private const val privateBinTagBytes = privateBinTagBits / 8
    private const val privateBinSaltBytes = 8
    private val secureRandom = SecureRandom()
    private val base64 = Base64.getEncoder()
    private val base64Decoder = Base64.getDecoder()
    private var preferredSourceId = 0

    data class Source(
        var id: Int,
        var name: String,
        var baseUrl: String,
        var enabled: Boolean,
        var type: String,
        var expire: String
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
        val link: String,
        val key: String? = null
    ) {
        val baseUrl: String get() = source.normalizedBaseUrl()
        val sourceType: String get() = source.normalizedType()

        fun useLegacyMessage(): Boolean = sourceType == typePastebin && isPastebinDotCom(baseUrl)

        fun chatPayload(): String {
            return if (useLegacyMessage()) {
                "<ARCxMDTX><Schem>[black]一坨兼容[] $id"
            } else {
                buildString {
                    append("<ARCxMDTX><SchemV2> ")
                    append(sourceType).append(' ')
                    append(baseUrl).append(' ')
                    append(id)
                    key?.takeIf { it.isNotBlank() }?.let {
                        append(' ').append(it)
                    }
                }
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
                    add("[yellow]修改后请点击保存图标，PrivateBin 需携带解密 key").colspan(columns).center().padTop(-4f).row()
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
        key: String?,
        callback: (String) -> Unit,
        failed: (Throwable) -> Unit
    ) {
        when (normalizeType(sourceType)) {
            typePrivateBin -> downloadPrivateBin(baseUrl, id, key, callback, failed)
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

        if (source.normalizedType() == typePrivateBin) {
            uploadPrivateBin(source, content, callback, onFailure)
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

    private fun uploadPrivateBin(
        source: Source,
        content: String,
        callback: (ShareLink?) -> Unit,
        failed: (String) -> Unit
    ) {
        val baseUrl = source.normalizedBaseUrl()
        val expire = source.normalizedExpire()
        val encrypted = runCatching { PrivateBinCodec.encrypt(content, expire) }
            .getOrElse {
                failed(it.message ?: it.toString())
                return
            }

        Http.post(baseUrl)
            .header("Content-Type", "application/json")
            .header("X-Requested-With", privateBinRequestedWith)
            .content(encrypted.requestJson)
            .timeout(10000)
            .error { failed(extractHttpError(it)) }
            .submit { res ->
                val raw = res.resultAsString
                val id = runCatching {
                    val json = Jval.read(raw)
                    if (json.getInt("status", -1) != 0) error(json.getString("message", "PrivateBin 返回失败"))
                    json.getString("id", "").takeIf { it.isNotBlank() } ?: error("PrivateBin 未返回 id")
                }.getOrElse {
                    failed(it.message ?: it.toString())
                    return@submit
                }

                preferredSourceId = source.id
                Core.app.post {
                    callback(
                        ShareLink(
                            source = source,
                            id = id,
                            link = "$baseUrl/?$id#${encrypted.passcode}",
                            key = encrypted.passcode
                        )
                    )
                }
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

    private fun downloadPrivateBin(
        baseUrl: String,
        id: String,
        key: String?,
        callback: (String) -> Unit,
        failed: (Throwable) -> Unit
    ) {
        if (key.isNullOrBlank()) {
            failed(IllegalArgumentException("PrivateBin 分享缺少解密 key"))
            return
        }

        Http.get("${normalizeBaseUrl(baseUrl)}/?pasteid=$id")
            .header("X-Requested-With", privateBinRequestedWith)
            .timeout(10000)
            .error(failed)
            .submit { res ->
                val content = runCatching {
                    PrivateBinCodec.decrypt(res.resultAsString, key).replace(" ", "+")
                }.getOrElse {
                    failed(it)
                    return@submit
                }
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
        Source(1, "PrivateBin 备用", "https://8.136.36.61:8080/", true, typePrivateBin, "1day")
    )

    private fun nextType(type: String): String = if (normalizeType(type) == typePastebin) typePrivateBin else typePastebin

    private fun typeLabel(type: String): String = when (normalizeType(type)) {
        typePrivateBin -> "PrivateBin"
        else -> "Pastebin"
    }

    private fun defaultExpire(type: String): String = when (normalizeType(type)) {
        typePrivateBin -> "1day"
        else -> "10M"
    }

    private fun normalizeExpire(expire: String, type: String): String {
        val trimmed = expire.trim()
        return if (trimmed.isNotEmpty()) trimmed else defaultExpire(type)
    }

    private fun normalizeType(type: String): String {
        return if (type.equals(typePrivateBin, ignoreCase = true)) typePrivateBin else typePastebin
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

    private object PrivateBinCodec {
        data class EncryptedPaste(val requestJson: String, val passcode: String)

        init {
            runSelfCheck()
        }

        fun encrypt(content: String, expire: String): EncryptedPaste {
            val iv = ByteArray(privateBinTagBytes).also(secureRandom::nextBytes)
            val salt = ByteArray(privateBinSaltBytes).also(secureRandom::nextBytes)
            val passphrase = ByteArray(privateBinKeyBytes).also(secureRandom::nextBytes)
            val adata = buildAdata(iv, salt)
            val derivedKey = deriveKey(passphrase, salt)
            val plaintext = Jval.newObject().put("paste", content).toString().toByteArray(StandardCharsets.UTF_8)
            val encrypted = crypt(Cipher.ENCRYPT_MODE, derivedKey, iv, adata.toString(), plaintext)
            val json = Jval.newObject()
                .put("v", 2)
                .put("ct", base64.encodeToString(encrypted))
                .put("adata", adata)
                .put("meta", Jval.newObject().put("expire", expire))
                .toString()
            return EncryptedPaste(json, Base58.encode(passphrase))
        }

        fun decrypt(raw: String, passcode: String): String {
            val json = Jval.read(raw)
            if (json.getInt("status", 0) != 0) error(json.getString("message", "PrivateBin 返回失败"))

            val adata = json.get("adata")
            val params = adata.asArray().get(0).asArray()
            val iv = base64Decoder.decode(params.get(0).asString())
            val salt = base64Decoder.decode(params.get(1).asString())
            val compression = params.get(7).asString()
            require(compression == "none") { "不支持的 PrivateBin 压缩方式: $compression" }

            val derivedKey = deriveKey(Base58.decode(passcode), salt)
            val encrypted = base64Decoder.decode(json.getString("ct", ""))
            val decrypted = crypt(Cipher.DECRYPT_MODE, derivedKey, iv, adata.toString(), encrypted)
            return Jval.read(String(decrypted, StandardCharsets.UTF_8)).getString("paste", "")
        }

        private fun buildAdata(iv: ByteArray, salt: ByteArray): Jval {
            return Jval.newArray()
                .add(
                    Jval.newArray()
                        .add(base64.encodeToString(iv))
                        .add(base64.encodeToString(salt))
                        .add(privateBinIterations)
                        .add(privateBinKeyBits)
                        .add(privateBinTagBits)
                        .add("aes")
                        .add("gcm")
                        .add("none")
                )
                .add("plaintext")
                .add(0)
                .add(0)
        }

        private fun deriveKey(passphrase: ByteArray, salt: ByteArray): ByteArray {
            return pbkdf2Sha256(passphrase, salt, privateBinIterations, privateBinKeyBytes)
        }

        private fun crypt(mode: Int, key: ByteArray, iv: ByteArray, aad: String, input: ByteArray): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(privateBinTagBits, iv))
            cipher.updateAAD(aad.toByteArray(StandardCharsets.UTF_8))
            return cipher.doFinal(input)
        }

        private fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, length: Int): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(password, "HmacSHA256"))

            val block = ByteBuffer.allocate(salt.size + 4)
                .put(salt)
                .putInt(1)
                .array()
            var u = mac.doFinal(block)
            val output = u.copyOf()
            repeat(iterations - 1) {
                u = mac.doFinal(u)
                for (i in output.indices) {
                    output[i] = (output[i].toInt() xor u[i].toInt()).toByte()
                }
            }
            return output.copyOf(length)
        }

        // PrivateBin compatibility depends on these low-level helpers being byte-exact.
        private fun runSelfCheck() {
            val password = hexToBytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
            val salt = hexToBytes("0001020304050607")
            check(
                pbkdf2Sha256(password, salt, privateBinIterations, privateBinKeyBytes)
                    .contentEquals(hexToBytes("0c22cef0bac57e3665d31565b9bbd940a6c110f0b11945d1cb6c6520cec59d4f"))
            ) { "PrivateBin PBKDF2 self-check failed" }

            check(Base58.encode(password) == "1thX6LZfHDZZKUs92febYZhYRcXddmzfzF2NvTkPNE") {
                "PrivateBin Base58 encode self-check failed"
            }
            check(Base58.decode("112VfUX").contentEquals(hexToBytes("000001020304"))) {
                "PrivateBin Base58 decode self-check failed"
            }
        }

        private fun hexToBytes(hex: String): ByteArray {
            require(hex.length % 2 == 0) { "Invalid hex length" }
            return ByteArray(hex.length / 2) { index ->
                hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        }
    }

    private object Base58 {
        private const val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        private val indexes = IntArray(128) { -1 }.also { table ->
            alphabet.forEachIndexed { index, c -> table[c.code] = index }
        }

        fun encode(input: ByteArray): String {
            if (input.isEmpty()) return ""

            var zeros = 0
            while (zeros < input.size && input[zeros].toInt() == 0) zeros++

            val encoded = CharArray(input.size * 2)
            val copy = input.copyOf()
            var outputStart = encoded.size
            var startAt = zeros
            while (startAt < copy.size) {
                val mod = divmod58(copy, startAt)
                if (copy[startAt].toInt() == 0) startAt++
                encoded[--outputStart] = alphabet[mod]
            }
            while (outputStart < encoded.size && encoded[outputStart] == alphabet[0]) outputStart++
            repeat(zeros) { encoded[--outputStart] = alphabet[0] }
            return String(encoded, outputStart, encoded.size - outputStart)
        }

        fun decode(input: String): ByteArray {
            if (input.isEmpty()) return ByteArray(0)

            val input58 = ByteArray(input.length)
            input.forEachIndexed { index, c ->
                val value = if (c.code < indexes.size) indexes[c.code] else -1
                require(value >= 0) { "非法的 PrivateBin key" }
                input58[index] = value.toByte()
            }

            var zeros = 0
            while (zeros < input58.size && input58[zeros].toInt() == 0) zeros++

            val decoded = ByteArray(input.length)
            var outputStart = decoded.size
            var startAt = zeros
            while (startAt < input58.size) {
                val mod = divmod256(input58, startAt)
                if (input58[startAt].toInt() == 0) startAt++
                decoded[--outputStart] = mod.toByte()
            }
            while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) outputStart++
            return ByteArray(decoded.size - outputStart + zeros).also { result ->
                decoded.copyInto(result, zeros, outputStart, decoded.size)
            }
        }

        private fun divmod58(number: ByteArray, startAt: Int): Int {
            var remainder = 0
            for (i in startAt until number.size) {
                val digit = number[i].toInt() and 0xff
                val temp = remainder * 256 + digit
                number[i] = (temp / 58).toByte()
                remainder = temp % 58
            }
            return remainder
        }

        private fun divmod256(number58: ByteArray, startAt: Int): Int {
            var remainder = 0
            for (i in startAt until number58.size) {
                val digit = number58[i].toInt() and 0xff
                val temp = remainder * 58 + digit
                number58[i] = (temp / 256).toByte()
                remainder = temp % 256
            }
            return remainder
        }
    }
}
