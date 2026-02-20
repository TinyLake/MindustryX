package mindustryX.bundles.ui

interface UiCoreTextBundle {
    fun itemSelectionHeight(value: Int): String = "$value 行"
    fun itemSelectionWidth(value: Int): String = "$value 列"
    fun javaWarnLog(javaVersion: String): String = "Java版本 $javaVersion 过低，不受支持。请使用Java 17或更高版本运行MindustryX。"
    fun javaWarnDialog(javaVersion: String): String = "Java版本 $javaVersion 过低，不受支持。\n[grey]该警告不存在设置，请更新Java版本。"

    fun windowTitle(version: String, enabledMods: Int, totalMods: Int, width: Int, height: Int): String =
        "MindustryX | 版本号 $version | mod启用$enabledMods/$totalMods | ${width}x$height"

    fun currentVersion(version: String): String = "当前版本号: $version"
    fun newVersion(version: String): String = "[green]发现新版本[]: $version"
    fun loadMap(mapName: String): String = "载入地图：$mapName"
    fun introduction(description: String): String = "简介：$description"
    fun waveEvent(wave: Int, detail: String): String = "波次：$wave | $detail"
    fun exportHeader(version: String): String = "下面是[MDTX-$version] 导出的游戏内聊天记录"
    fun exportMap(mapName: String, mode: String): String = "*** 当前地图名称: $mapName(模式: $mode)\n"
    fun currentWave(wave: Int): String = "*** 当前波次: $wave"
    fun exportCount(count: Int): String = "成功选取共${count}条记录，如下：\n"

    fun toggleState(label: String, state: String): String = "$label: $state"
    fun copiedMemory(value: Double): String = "[cyan]复制内存[white]\n $value"
    fun copiedVariableNameHint(value: String): String = "复制变量名\n$value"
    fun copiedVariableAttributesHint(value: String): String = "复制变量属性\n$value"
    fun copiedPrintBufferHint(value: String): String = "复制信息版\n$value"

    fun failedReadImage(error: Any?): String = "读取图片失败，请尝试更换图片\n$error"
    fun invalidBackgroundImage(path: String): String = "背景图片无效: $path"
    fun labelWithEmoji(label: String, emoji: String): String = "$label $emoji"
    fun savedBlueprint(name: String): String = "已保存蓝图：$name"

    fun fpsLockOff(gameSpeed: Float): String = "已关闭帧率锁定模式\n当前游戏速度：${gameSpeed}倍"
    fun currentGameSpeed(gameSpeed: Float): String = "当前游戏速度：${gameSpeed}倍"
    fun fpsLockEnabled(targetFps: Int): String = "已开启帧率锁定模式\n当前帧率锁定：$targetFps"
    fun fpsLockCurrent(targetFps: Int): String = "当前帧率锁定：$targetFps"
    fun currentMap(mapName: String): String = "当前地图:$mapName"
    fun worldProcessorSummary(processors: Int, instructions: Int, chars: Int): String =
        "地图共有${processors}个世处，总共${instructions}行指令，${chars}个字符"

    fun recording(path: String): String = "录制中: $path"
    fun coordinateDistance(x: Int, y: Int, distance: Int): String = "$x,$y\n距离: $distance"
}
