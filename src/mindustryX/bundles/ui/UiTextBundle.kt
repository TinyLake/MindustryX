package mindustryX.bundles.ui

import arc.Core
import java.util.Locale

interface UiTextBundle : UiCoreTextBundle, UiShareTextBundle, UiStatTextBundle {
    fun mdtxReport(): String = "问题反馈"
    fun mdtxQqLink(): String = "QQ交流群"
    fun modsRecommendTitle(): String = "[accent]MdtX[]推荐辅助模组列表"
    fun modsRecommendInfo(): String = "精选辅助模组"
    fun modsRecommendLastUpdated(value: Any?): String = "推荐列表更新时间：$value"
    fun modsRecommendModName(value: Any?): String = "模组：$value"
    fun modsRecommendModAuthor(value: Any?): String = "作者：$value"
    fun modsRecommendModMinGameVersion(value: Any?): String = "最低支持游戏版本：$value"
    fun modsRecommendModLastUpdated(value: Any?): String = "上次更新时间：$value"
    fun modsRecommendModStars(value: Any?): String = "Github收藏数：$value"

    companion object {
        object ZH : UiTextBundle

        object EN : UiTextBundle {
            override fun mdtxReport(): String = "Report Issue"
            override fun mdtxQqLink(): String = "QQ Group"
            override fun modsRecommendTitle(): String = "[accent]MdtX[]Recommended Mods List"
            override fun modsRecommendInfo(): String = "Selected Mods"
            override fun modsRecommendLastUpdated(value: Any?): String = "Recommended List Last Updated: $value"
            override fun modsRecommendModName(value: Any?): String = "Mod: $value"
            override fun modsRecommendModAuthor(value: Any?): String = "Author: $value"
            override fun modsRecommendModMinGameVersion(value: Any?): String = "Minimum Supported Game Version: $value"
            override fun modsRecommendModLastUpdated(value: Any?): String = "Last Updated: $value"
            override fun modsRecommendModStars(value: Any?): String = "Github Stars: $value"

            override fun mdtxShareItem(name: Any?, stock: Any?, production: Any?): String =
                "${name}: Stock ${stock}, Production ${production}/s"

            override fun mdtxShareUnit(name: Any?, count: Any?, limit: Any?): String =
                "${name}: Count ${count}, Limit ${limit}"

            override fun itemSelectionHeight(value: Int): String = "$value rows"
            override fun itemSelectionWidth(value: Int): String = "$value columns"
            override fun javaWarnLog(javaVersion: String): String =
                "Java version $javaVersion is too low and unsupported. Please use Java 17+ to run MindustryX."

            override fun javaWarnDialog(javaVersion: String): String =
                "Java version $javaVersion is too low and unsupported.\n[grey]This warning cannot be disabled; please update Java."

            override fun windowTitle(version: String, enabledMods: Int, totalMods: Int, width: Int, height: Int): String =
                "MindustryX | Version $version | mods enabled $enabledMods/$totalMods | ${width}x$height"

            override fun currentVersion(version: String): String = "Current version: $version"
            override fun newVersion(version: String): String = "[green]New version found[]: $version"
            override fun loadMap(mapName: String): String = "Load map: $mapName"
            override fun introduction(description: String): String = "Introduction: $description"
            override fun waveEvent(wave: Int, detail: String): String = "Waves: $wave | $detail"
            override fun exportHeader(version: String): String = "[MDTX-$version] Exported in-game chat history"
            override fun exportMap(mapName: String, mode: String): String = "*** Current map name: $mapName (mode: $mode)\n"
            override fun currentWave(wave: Int): String = "*** Current wave: $wave"
            override fun exportCount(count: Int): String = "Successfully selected a total of $count records:\n"

            override fun copiedMemory(value: Double): String = "[cyan]Copied memory[white]\n $value"
            override fun copiedVariableNameHint(value: String): String = "Copied variable name\n$value"
            override fun copiedVariableAttributesHint(value: String): String = "Copied variable attributes\n$value"
            override fun copiedPrintBufferHint(value: String): String = "Copied print buffer\n$value"
            override fun failedReadImage(error: Any?): String = "Failed to read image, please try another image\n$error"
            override fun invalidBackgroundImage(path: String): String = "Invalid background image: $path"
            override fun savedBlueprint(name: String): String = "Saved blueprint: $name"

            override fun fpsLockOff(gameSpeed: Float): String = "Frame rate lock mode turned off \nCurrent game speed: $gameSpeed times"
            override fun currentGameSpeed(gameSpeed: Float): String = "Current game speed: $gameSpeed times"
            override fun fpsLockEnabled(targetFps: Int): String = "Frame rate lock mode enabled \nCurrent frame rate lock: $targetFps"
            override fun fpsLockCurrent(targetFps: Int): String = "Current frame rate locked: $targetFps"
            override fun currentMap(mapName: String): String = "Current map: $mapName"
            override fun worldProcessorSummary(processors: Int, instructions: Int, chars: Int): String =
                "The map has $processors world processors, $instructions instruction lines, and $chars characters."

            override fun recording(path: String): String = "Recording: $path"
            override fun coordinateDistance(x: Int, y: Int, distance: Int): String = "$x,$y\nDistance: $distance"

            override fun atPlayer(playerName: String?): String = "<AT> poked ${playerName ?: ""}[white] to check their messages."
            override fun atNoticeFrom(senderName: String): String = "[gold]You were poked by [white]$senderName[gold]! Check the message dialog."
            override fun shareCode(code: String): String = "<ARCxMDTX><Schem>[black]compat code[] $code"
            override fun shareHeader(version: String): String = "This is a share log from MDTX-$version\n"
            override fun waveContains(ground: Int, air: Int): String = "Contains (ground x$ground, air x$air):"
            override fun waveTitle(wave: Int): String = "Wave $wave"
            override fun waveEta(remainingWaves: Int, eta: String): String = "(in $remainingWaves waves, $eta)"

            override fun shieldCapacity(max: String, radius: String, recovery: String, cooldown: String): String =
                "[lightgray]${max} shield capacity[accent]~[]${radius} grid[accent]~[]${recovery} recovery[accent]~[]${cooldown}s cooldown"

            override fun liquidExplode(total: String, liquidName: String, liquidEmoji: String, radius: String): String =
                "[lightgray]Total ${total}${liquidName}${liquidEmoji}[accent]~[]${radius} tile radius"

            override fun liquidRegen(absorb: String, liquidName: String, liquidEmoji: String, heal: String, maxHeal: String): String =
                "[lightgray]Each cell absorbs ${absorb}/s${liquidName}${liquidEmoji}[accent]~[]heals ${heal}/s[accent]~[]up to ${maxHeal}/s"

            override fun lightning(probability: String, damage: String, length: String, speed: String): String =
                "[lightgray]Lightning ${probability} probability[accent]~[]${damage} damage[accent]~[]${length} length ${speed}x speed"

            override fun durationTiles(seconds: String, tiles: String): String = "[lightgray]${seconds}s[accent]~[]${tiles} tiles"
        }

        @JvmStatic
        fun default(): UiTextBundle {
            val language = Core.bundle.locale?.language ?: Locale.getDefault().language
            return if (language == Locale.CHINESE.language) ZH else EN
        }
    }
}
