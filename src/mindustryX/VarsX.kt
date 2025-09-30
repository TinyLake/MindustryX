package mindustryX

import arc.Core
import arc.Files
import arc.files.Fi
import arc.util.Log
import arc.util.OS
import arc.util.serialization.Jval
import mindustry.Vars
import mindustryX.features.SettingsV2.CheckPref
import mindustryX.features.SettingsV2.SliderPref

object VarsX {
    const val repo = "TinyLake/MindustryX"
    const val qqLink = "https://qm.qq.com/cgi-bin/qm/qr?k=EpvRLTE26gKzbs8WVDxUomwu34k7a-w8&jump_from=webapi&authKey=nPltwJhZjjkhi6O4Nb/hQXhF9bn1fSK2lzo077KLOY4g2Ua80Itx1vp8PY1aDtq+"

    @JvmField
    var version: String

    @JvmField
    var devVersion: Boolean

    @JvmField
    var isLoader: Boolean = false


    init {
        val version = kotlin.runCatching {
            val file = if (OS.isAndroid || OS.isIos) Core.files.internal("mod.hjson") else Fi("mod.hjson", Files.FileType.internal)
            val meta = Jval.read(file.readString())
            meta.getString("version")!!
        }.getOrElse {
            Log.err("Failed to read mod version from mod.hjson, using default value", it)
            "custom-dev"
        }
        this.version = version
        devVersion = version.endsWith("-dev")
    }

    //此处存储所有需要在features外使用的设置项。
    // 使用时使用全限定名，避免在patch中使用import

    @JvmField
    val blockInventoryWidth = SliderPref("blockInventoryWidth", 3, 3, 16)

    @JvmField
    val minimapSize = SliderPref("minimapSize", 140, 40, 400, 10)

    @JvmField
    val arcTurretShowPlaceRange = CheckPref("arcTurretPlaceCheck")

    @JvmField
    val arcTurretShowAmmoRange = CheckPref("arcTurretPlacementItem")

    @JvmField
    val staticShieldsBorder = CheckPref("staticShieldsBorder")

    @JvmField
    val allUnlocked = CheckPref("allUnlocked")

    @JvmField
    val editorBrush = SliderPref("editorBrush", 6, 3, 13)

    @JvmField
    val noPlayerHitBox = CheckPref("noPlayerHitBox")

    @JvmField
    val githubMirror = CheckPref("githubMirror", true)

    @JvmField
    val arcSpecificInfoTable = CheckPref("gameUI.arcSpecificTable").apply {
        addFallbackName("arcSpecificTable")
    }

    @JvmField
    val itemSelectionHeight = SliderPref("gameUI.itemSelectionHeight", 4, 4, 12) { "$it 行" }.apply {
        addFallbackName("itemSelectionHeight")
    }

    @JvmField
    val itemSelectionWidth = SliderPref("gameUI.itemSelectionWidth", 4, 4, 12) { "$it 列" }.apply {
        addFallbackName("itemSelectionWidth")
    }

    @JvmField
    val researchViewer = CheckPref("gameUI.researchViewer").apply {
        addFallbackName("researchViewer")
    }

    @JvmField
    val maxSchematicSize = SliderPref("maxSchematicSize", Vars.maxSchematicSize, 64, 257) {
        if (it == 257) return@SliderPref "无限制"
        "${it}x${it}"
    }

    @JvmField
    val autoSelectSchematic = CheckPref("autoSelectSchematic").apply {
        addFallbackName("autoSelSchematic")
    }

    @JvmField
    val extendedCommandTable = CheckPref("gameUI.extendedCommandTable", true).apply {
        addFallbackName("arcCommandTable")
    }
}
