package mindustryX.features

import arc.Core
import arc.util.*
import arc.util.serialization.Jval
import mindustry.Vars
import mindustry.core.Version
import mindustry.mod.Mods.LoadedMod
import mindustry.net.CrashHandler
import mindustryX.VarsX
import java.security.MessageDigest
import kotlin.concurrent.thread
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object MetricCollector {
    private val enable = SettingsV2.CheckPref("collectMetrics", true)
    private var task: Thread? = null

    private fun postLog(data: Jval) {
        val req = Http.post("https://s1367486.eu-nbg-2.betterstackdata.com/")
            .header("Authorization", "Bearer cM5m9huGdtcFTiXcfdPK17zL")
            .header("Content-Type", "application/json")
            .content(data.toString())
            .timeout(3000)
        task = thread(start = true) {
            req.block {
                Log.info("Posted metrics successfully: ${it.status} ${it.resultAsString}")
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun getDeviceId(): String? = kotlin.runCatching {
        Vars.tmpDirectory.child("metric_device_id.txt").let { file ->
            if (!file.exists()) {
                file.writeString(Uuid.random().toString())
            }
            return file.readString()
        }
    }.getOrNull()

    @OptIn(ExperimentalStdlibApi::class)
    private fun getUserId(): String {
        val uid = Core.settings.getString("uuid") ?: return "unknown"
        return kotlin.runCatching {
            MessageDigest.getInstance("SHA-1").digest(uid.toByteArray()).toHexString()
        }.getOrElse { "Fail-HASH" }
    }

    private fun getEnvInfo() = Jval.newObject().apply {
        runCatching {
            put("os", "${OS.osName} x${OS.osArchBits} (${OS.osArch})")
            if ((OS.isAndroid || OS.isIos) && Core.app != null)
                put("Android", Core.app.version)
            put("javaVersion", OS.javaVersion)
            put("cpuCores", OS.cores)
            put("memory", Runtime.getRuntime().maxMemory())//B
            put("isLoader", VarsX.isLoader)
        }
    }

    private fun getModList() = Jval.newObject().apply {
        Vars.mods?.list()?.forEach { mod ->
            if (mod.enabled()) {
                put(mod.name, mod.meta.version)
            }
        }
    }


    private fun getDisabledModList() = Jval.newObject().apply {
        Vars.mods?.list()?.forEach { mod ->
            if (mod.enabled()) return@forEach
            put(mod.name, Jval.newObject().apply {
                put("version", mod.meta.version)
                put("disabled", !mod.shouldBeEnabled())
                put("notSupported", !mod.isSupported())
                put("hasContentErrors", mod.hasContentErrors())
                put("hasUnmetDependencies", mod.hasUnmetDependencies())
            })
        }
    }

    private fun getSettings() = Jval.newObject().apply {
        SettingsV2.ALL.values.forEach {
            if (it.value == it.def) return@forEach
            put(it.name, Strings.truncate(it.value.toString(), 20, "...")) //limit to 20 chars
        }
    }

    private fun getBaseInfo(): Jval {
        return Jval.newObject().apply {
            put("deviceId", getDeviceId())
            put("userId", getUserId())
            put("version", Version.combined())
            put("env", getEnvInfo())
            put("mods", getModList())
            put("disabledMods", getDisabledModList())
            put("settings", getSettings())
        }
    }

    private fun getModCause(e: Throwable): LoadedMod? {
        e.cause?.let { getModCause(it) }?.let { return it }
        return CrashHandler.getModCause(e)
    }

    fun handleException(e: Throwable) {
        if (!enable.value || VarsX.devVersion) {
            Log.warn("MetricCollector: Exception occurred, but metrics collection is disabled.")
            return
        }
        val data = getBaseInfo().apply {
            put("cause", e.stackTraceToString())
            put("state", Jval.newObject().apply {
                put("frameId", Core.graphics?.frameId ?: 0)
                put("state", Vars.state?.state?.toString())
                put("currentMod", Vars.content?.transformName("")?.removeSuffix("-"))
            })
            getModCause(e)?.let {
                put("likelyCause", it.name)
            }
        }
        Log.err("MetricCollector: Posting exception data: $e")
        postLog(data)
    }

    fun waitPost() {
        task?.join()
        task = null
    }

    fun onLaunch() {
        if (!enable.value) return
        val last = Core.settings.getLong("MetricCollector.lastPost")
        if (Time.timeSinceMillis(last) < 24 * 60 * 60 * 1000) {
            Log.infoTag("MetricCollector", "Skip posting metrics.")
            return
        }
        postLog(getBaseInfo())
        Core.settings.put("MetricCollector.lastPost", Time.millis())
    }
}