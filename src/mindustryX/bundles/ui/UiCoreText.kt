package mindustryX.bundles.ui

object UiCoreText {
    @JvmStatic
    fun itemSelectionHeight(value: Int): String = if (isChineseLocale()) "$value 行" else "$value rows"

    @JvmStatic
    fun itemSelectionWidth(value: Int): String = if (isChineseLocale()) "$value 列" else "$value columns"

    @JvmStatic
    fun javaWarnLog(javaVersion: String): String = if (isChineseLocale()) {
        "Java版本 $javaVersion 过低，不受支持。请使用Java 17或更高版本运行MindustryX。"
    } else {
        "Java version $javaVersion is too low and unsupported. Please use Java 17+ to run MindustryX."
    }

    @JvmStatic
    fun javaWarnDialog(javaVersion: String): String = if (isChineseLocale()) {
        "Java版本 $javaVersion 过低，不受支持。\n[grey]该警告不存在设置，请更新Java版本。"
    } else {
        "Java version $javaVersion is too low and unsupported.\n[grey]This warning cannot be disabled; please update Java."
    }

    @JvmStatic
    fun windowTitle(version: String, enabledMods: Int, totalMods: Int, width: Int, height: Int): String =
        if (isChineseLocale()) {
            "MindustryX | 版本号 $version | mod启用$enabledMods/$totalMods | ${width}x$height"
        } else {
            "MindustryX | Version $version | mods enabled $enabledMods/$totalMods | ${width}x$height"
        }

    @JvmStatic
    fun currentVersion(version: String): String = if (isChineseLocale()) "当前版本号: $version" else "Current version: $version"

    @JvmStatic
    fun newVersion(version: String): String = if (isChineseLocale()) "[green]发现新版本[]: $version" else "[green]New version found[]: $version"

    @JvmStatic
    fun loadMap(mapName: String): String = if (isChineseLocale()) "载入地图：$mapName" else "Load map: $mapName"

    @JvmStatic
    fun introduction(description: String): String = if (isChineseLocale()) "简介：$description" else "Introduction: $description"

    @JvmStatic
    fun waveEvent(wave: Int, detail: String): String = if (isChineseLocale()) {
        "波次：$wave | $detail"
    } else {
        "Waves: $wave | $detail"
    }

    @JvmStatic
    fun exportHeader(version: String): String = if (isChineseLocale()) {
        "下面是[MDTX-$version] 导出的游戏内聊天记录"
    } else {
        "[MDTX-$version] Exported in-game chat history"
    }

    @JvmStatic
    fun exportMap(mapName: String, mode: String): String = if (isChineseLocale()) {
        "*** 当前地图名称: $mapName(模式: $mode)\n"
    } else {
        "*** Current map name: $mapName (mode: $mode)\n"
    }

    @JvmStatic
    fun currentWave(wave: Int): String = if (isChineseLocale()) "*** 当前波次: $wave" else "*** Current wave: $wave"

    @JvmStatic
    fun exportCount(count: Int): String = if (isChineseLocale()) {
        "成功选取共${count}条记录，如下：\n"
    } else {
        "Successfully selected a total of $count records:\n"
    }

    @JvmStatic
    fun toggleState(label: String, state: String): String = "$label: $state"

    @JvmStatic
    fun copiedMemory(value: Double): String = if (isChineseLocale()) {
        "[cyan]复制内存[white]\n $value"
    } else {
        "[cyan]Copied memory[white]\n $value"
    }

    @JvmStatic
    fun copiedVariableNameHint(value: String): String = if (isChineseLocale()) {
        "复制变量名\n$value"
    } else {
        "Copied variable name\n$value"
    }

    @JvmStatic
    fun copiedVariableAttributesHint(value: String): String = if (isChineseLocale()) {
        "复制变量属性\n$value"
    } else {
        "Copied variable attributes\n$value"
    }

    @JvmStatic
    fun copiedPrintBufferHint(value: String): String = if (isChineseLocale()) {
        "复制信息版\n$value"
    } else {
        "Copied print buffer\n$value"
    }

    @JvmStatic
    fun failedReadImage(error: Any?): String = if (isChineseLocale()) {
        "读取图片失败，请尝试更换图片\n$error"
    } else {
        "Failed to read image, please try another image\n$error"
    }

    @JvmStatic
    fun invalidBackgroundImage(path: String): String = if (isChineseLocale()) {
        "背景图片无效: $path"
    } else {
        "Invalid background image: $path"
    }

    @JvmStatic
    fun labelWithEmoji(label: String, emoji: String): String = "$label $emoji"

    @JvmStatic
    fun savedBlueprint(name: String): String = if (isChineseLocale()) "已保存蓝图：$name" else "Saved blueprint: $name"

    @JvmStatic
    fun fpsLockOff(gameSpeed: Float): String = if (isChineseLocale()) {
        "已关闭帧率锁定模式\n当前游戏速度：${gameSpeed}倍"
    } else {
        "Frame rate lock mode turned off \nCurrent game speed: $gameSpeed times"
    }

    @JvmStatic
    fun currentGameSpeed(gameSpeed: Float): String = if (isChineseLocale()) {
        "当前游戏速度：${gameSpeed}倍"
    } else {
        "Current game speed: $gameSpeed times"
    }

    @JvmStatic
    fun fpsLockEnabled(targetFps: Int): String = if (isChineseLocale()) {
        "已开启帧率锁定模式\n当前帧率锁定：$targetFps"
    } else {
        "Frame rate lock mode enabled \nCurrent frame rate lock: $targetFps"
    }

    @JvmStatic
    fun fpsLockCurrent(targetFps: Int): String = if (isChineseLocale()) {
        "当前帧率锁定：$targetFps"
    } else {
        "Current frame rate locked: $targetFps"
    }

    @JvmStatic
    fun currentMap(mapName: String): String = if (isChineseLocale()) "当前地图:$mapName" else "Current map: $mapName"

    @JvmStatic
    fun worldProcessorSummary(processors: Int, instructions: Int, chars: Int): String = if (isChineseLocale()) {
        "地图共有${processors}个世处，总共${instructions}行指令，${chars}个字符"
    } else {
        "The map has $processors world processors, $instructions instruction lines, and $chars characters."
    }

    @JvmStatic
    fun recording(path: String): String = if (isChineseLocale()) "录制中: $path" else "Recording: $path"

    @JvmStatic
    fun coordinateDistance(x: Int, y: Int, distance: Int): String = if (isChineseLocale()) {
        "$x,$y\n距离: $distance"
    } else {
        "$x,$y\nDistance: $distance"
    }
}
