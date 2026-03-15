package mindustryX.bundles

import arc.util.Strings

internal object UiTextBundleEn : UiTextBundle {
    override fun mdtxReport(): String = "Report Issue"
    override fun mdtxQqLink(): String = "QQ Group"
    override fun modsRecommendTitle(): String = "[accent]MdtX[]Recommended Mods List"
    override fun modsRecommendInfo(): String = "Selected Mods"
    override fun modsRecommendLastUpdated(value: String): String = "Recommended List Last Updated: $value"
    override fun modsRecommendModName(value: String): String = "Mod: $value"
    override fun modsRecommendModAuthor(value: String): String = "Author: $value"
    override fun modsRecommendModMinGameVersion(value: String): String = "Minimum Supported Game Version: $value"
    override fun modsRecommendModLastUpdated(value: String): String = "Last Updated: $value"
    override fun modsRecommendModStars(value: String): String = "Github Stars: $value"
    override fun mdtxShareItem(name: String, stock: String, production: String): String = "$name: Stock $stock, Production $production/s"
    override fun mdtxShareUnit(name: String, count: String, limit: Int): String = "$name: Count $count, Limit $limit"
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
    override fun failedReadImage(error: Throwable): String = "Failed to read image, please try another image\n${error.message ?: error}"
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
        "[lightgray]Lightning $probability probability[accent]~[]${damage} damage[accent]~[]${length} length ${speed}x speed"

    override fun durationTiles(seconds: String, tiles: String): String = "[lightgray]${seconds}s[accent]~[]${tiles} tiles"
    override fun refreshInterval(value: Int): String = "Refresh interval$value"
    override fun zoomScale(scale: String): String = "Zoom: x$scale"
    override fun teamRange(teamId: Int): String = "Team: $teamId~${teamId + 9}"
    override fun copiedSuccessfully(text: String): String = "Copied successfully:$text"
    override fun builtInDisplayName(name: String): String = "[Built-in]$name"
    override fun playbackVersion(value: String): String = "Playback version:$value"
    override fun replayCreationTime(value: String): String = "Replay creation time:$value"
    override fun serverIp(value: String): String = "Server IP:$value"
    override fun playerName(value: String): String = "Player name:$value"
    override fun packetCount(value: Int): String = "Packet count:$value"
    override fun playbackLength(value: String): String = "Playback length:$value"
    override fun sizeWithDimensions(width: String, height: String): String = "Size:$width\uE815$height"
    override fun tiles(value: Number): String = "$value tiles"
    override fun tilesOrOff(value: Int): String = if (value > 0) "$value tiles" else "Off"
    override fun percentOrOff(value: Int): String = if (value > 0) "$value%" else "Off"
    override fun hpOrAll(value: Int): String = if (value > 0) "$value[red]HP" else "Show all"
    override fun radarSpeedMode(value: Int): String = when (value) {
        0 -> "Off"
        30 -> "Instant"
        else -> "[lightgray]x[white]" + Strings.autoFixed(value * 0.2f, 1) + "x scan speed"
    }

    override fun radarSizeMode(value: Int): String = if (value == 0) "Fixed size" else "[lightgray]x[white]" + Strings.autoFixed(value * 0.1f, 1) + "x"

    override fun turretShowRangeMode(value: Int): String = when (value) {
        0 -> "Off"
        1 -> "Ground only"
        2 -> "Air only"
        3 -> "All"
        else -> ""
    }

    override fun unitWeaponRangeMode(value: Int): String = when (value) {
        0 -> "Off"
        30 -> "Always On"
        else -> "$value tiles"
    }

    override fun unitTargetTypeMode(value: Int): String = when (value) {
        0 -> "Off"
        1 -> "Ring"
        2 -> "Attack"
        3 -> "Attack (no border)"
        4 -> "Ring Cross"
        5 -> "Cross"
        else -> value.toString()
    }

    override fun superUnitEffectMode(value: Int): String = when (value) {
        0 -> "Off"
        1 -> "Unique"
        2 -> "All players"
        else -> value.toString()
    }

    override val labelsResFile: String get() = "labels_en"
}
