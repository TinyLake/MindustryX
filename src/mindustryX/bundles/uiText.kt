package mindustryX.bundles

import arc.util.Strings
import mindustryX.VarsX

interface UiTextBundle {
    fun i(key: String): String = key

    fun mdtxReport(): String = "问题反馈"
    fun mdtxQqLink(): String = "QQ交流群"
    fun modsRecommendTitle(): String = "[accent]MdtX[]推荐辅助模组列表"
    fun modsRecommendInfo(): String = "精选辅助模组"
    fun modsRecommendLastUpdated(value: String): String = "推荐列表更新时间：$value"
    fun modsRecommendModName(value: String): String = "模组：$value"
    fun modsRecommendModAuthor(value: String): String = "作者：$value"
    fun modsRecommendModMinGameVersion(value: String): String = "最低支持游戏版本：$value"
    fun modsRecommendModLastUpdated(value: String): String = "上次更新时间：$value"
    fun modsRecommendModStars(value: String): String = "Github收藏数：$value"

    fun mdtxShareItem(name: String, stock: String, production: String): String = "$name：库存 $stock，产量 $production/秒"
    fun mdtxShareUnit(name: String, count: String, limit: Int): String = "$name：数量 $count，上限 $limit"

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

    fun failedReadImage(error: Throwable): String = "读取图片失败，请尝试更换图片\n${error.message ?: error}"
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

    fun atPlayer(playerName: String?): String = "<AT>戳了${playerName ?: ""}[white]一下，并提醒他留意对话框"
    fun atNoticeFrom(senderName: String): String = "[gold]你被[white]$senderName[gold]戳了一下，请注意查看信息框哦~"
    fun shareCode(code: String): String = "<ARCxMDTX><Schem>[black]一坨兼容[] $code"
    fun shareHeader(version: String): String = "这是一条来自 MDTX-$version 的分享记录\n"
    fun waveContains(ground: Int, air: Int): String = "包含(地×$ground,空x$air):"
    fun waveTitle(wave: Int): String = "第${wave}波"
    fun waveEta(remainingWaves: Int, eta: String): String = "(还有${remainingWaves}波, $eta)"

    fun shieldCapacity(max: String, radius: String, recovery: String, cooldown: String): String =
        "[lightgray]${max}盾容[accent]~[]${radius}格[accent]~[]${recovery}恢复[accent]~[]${cooldown}s冷却"

    fun liquidExplode(total: String, liquidName: String, liquidEmoji: String, radius: String): String =
        "[lightgray]总计${total}${liquidName}${liquidEmoji}[accent]~[]${radius}格半径"

    fun liquidRegen(absorb: String, liquidName: String, liquidEmoji: String, heal: String, maxHeal: String): String =
        "[lightgray]每格吸收${absorb}/s${liquidName}${liquidEmoji}[accent]~[]${heal}/s回血[accent]~[]最大${maxHeal}/s"

    fun lightning(probability: String, damage: String, length: String, speed: String): String =
        "[lightgray]闪电${probability}概率[accent]~[]${damage}伤害[accent]~[]${length}长度 ${speed}x速度"

    fun durationTiles(seconds: String, tiles: String): String = "[lightgray]${seconds}s[accent]~[]${tiles}格"

    fun refreshInterval(value: Int): String = "刷新间隔$value"
    fun zoomScale(scale: String): String = "缩放: x$scale"
    fun teamRange(teamId: Int): String = "队伍：$teamId~${teamId + 9}"
    fun copiedSuccessfully(text: String): String = "复制成功:$text"
    fun builtInDisplayName(name: String): String = "[内置]$name"
    fun playbackVersion(value: String): String = "回放版本:$value"
    fun replayCreationTime(value: String): String = "回放创建时间:$value"
    fun serverIp(value: String): String = "服务器ip:$value"
    fun playerName(value: String): String = "玩家名:$value"
    fun packetCount(value: Int): String = "数据包总数：$value"
    fun playbackLength(value: String): String = "回放长度:$value"
    fun sizeWithDimensions(width: String, height: String): String = "大小：$width\uE815$height"
    fun tiles(value: Number): String = "${value}格"
    fun tilesOrOff(value: Int): String = if (value > 0) "${value}格" else "关闭"
    fun percentOrOff(value: Int): String = if (value > 0) "${value}%" else "关闭"
    fun hpOrAll(value: Int): String = if (value > 0) "${value}[red]HP" else "全部显示"
    fun radarSpeedMode(value: Int): String = when (value) {
        0 -> "关闭"
        30 -> "瞬间完成"
        else -> "[lightgray]x[white]" + Strings.autoFixed(value * 0.2f, 1) + "倍搜索速度"
    }

    fun radarSizeMode(value: Int): String = if (value == 0) "固定大小" else "[lightgray]x[white]" + Strings.autoFixed(value * 0.1f, 1) + "倍"

    fun turretShowRangeMode(value: Int): String = when (value) {
        0 -> "关闭"
        1 -> "仅对地"
        2 -> "仅对空"
        3 -> "全部"
        else -> ""
    }

    fun unitWeaponRangeMode(value: Int): String = when (value) {
        0 -> "关闭"
        30 -> "一直开启"
        else -> "${value}格"
    }

    fun unitTargetTypeMode(value: Int): String = when (value) {
        0 -> "关闭"
        1 -> "虚圆"
        2 -> "攻击"
        3 -> "攻击去边框"
        4 -> "圆十字"
        5 -> "十字"
        else -> value.toString()
    }

    fun superUnitEffectMode(value: Int): String = when (value) {
        0 -> "关闭"
        1 -> "独一无二"
        2 -> "全部玩家"
        else -> value.toString()
    }

    companion object {
        @JvmStatic
        fun i(zh: String): String = VarsX.bundle.i(zh)
    }
}
