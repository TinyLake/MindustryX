package mindustryX.bundles

import arc.Core
import arc.util.Strings
import mindustryX.VarsX
import java.util.Locale

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
        private object ZH : UiTextBundle

        private val EN: UiTextBundle = UiTextBundleEn

        /**
         * Lightweight constant-text lookup.
         * Keep this for no-arg static labels only; any text with variables/order/formatting must use typed bundle methods.
         */
        @JvmStatic
        fun i(zh: String): String = default().i(zh)

        private val bundlesByLanguage: Map<String, UiTextBundle> = mapOf(
            Locale.CHINESE.language to ZH,
            Locale.ENGLISH.language to EN,
        )

        @JvmStatic
        fun default(): UiTextBundle {
            val language = (Core.bundle?.locale?.language ?: Locale.getDefault().language).lowercase(Locale.ROOT)
            return bundlesByLanguage[language] ?: EN
        }
  
        @JvmStatic
        fun bundle(): UiTextBundle = VarsX.uiTextBundle

            fun mdtxReport(): String = bundle().mdtxReport()
            fun mdtxQqLink(): String = bundle().mdtxQqLink()
            fun modsRecommendTitle(): String = bundle().modsRecommendTitle()
            fun modsRecommendInfo(): String = bundle().modsRecommendInfo()
            fun modsRecommendLastUpdated(value: String): String = bundle().modsRecommendLastUpdated(value)
            fun modsRecommendModName(value: String): String = bundle().modsRecommendModName(value)
            fun modsRecommendModAuthor(value: String): String = bundle().modsRecommendModAuthor(value)
            fun modsRecommendModMinGameVersion(value: String): String = bundle().modsRecommendModMinGameVersion(value)
            fun modsRecommendModLastUpdated(value: String): String = bundle().modsRecommendModLastUpdated(value)
            fun modsRecommendModStars(value: String): String = bundle().modsRecommendModStars(value)
            fun mdtxShareItem(name: String, stock: String, production: String): String = bundle().mdtxShareItem(name, stock, production)
            fun mdtxShareUnit(name: String, count: String, limit: Int): String = bundle().mdtxShareUnit(name, count, limit)

            @JvmStatic fun uiJavaWarnLog(javaVersion: String): String = bundle().javaWarnLog(javaVersion)
            @JvmStatic fun uiJavaWarnDialog(javaVersion: String): String = bundle().javaWarnDialog(javaVersion)
            @JvmStatic fun uiWindowTitle(version: String, enabledMods: Int, totalMods: Int, width: Int, height: Int): String =
                bundle().windowTitle(version, enabledMods, totalMods, width, height)

            @JvmStatic fun uiArcMessageCenter(): String = i("ARC-中央监控室")
            @JvmStatic fun uiMaxChatHistoryHint(): String = i("最大储存聊天记录(过高可能导致卡顿)：")
            @JvmStatic fun uiChatHistoryCleanupHint(): String = i("超出限制的聊天记录将在载入地图时清除")
            @JvmStatic fun uiClear(): String = i("清空")
            @JvmStatic fun uiExport(): String = i("导出")
            @JvmStatic fun uiExportChatHistory(): String = i("导出聊天记录")
            @JvmStatic fun uiLoadMap(mapName: String): String = bundle().loadMap(mapName)
            @JvmStatic fun uiIntroduction(description: String): String = bundle().introduction(description)
            @JvmStatic fun uiWaveEvent(wave: Int, detail: String): String = bundle().waveEvent(wave, detail)
            @JvmStatic fun uiCopiedChatRecord(): String = i("已导出本条聊天记录")
            @JvmStatic fun uiExportHeader(version: String): String = bundle().exportHeader(version)
            @JvmStatic fun uiExportMap(mapName: String, mode: String): String = bundle().exportMap(mapName, mode)
            @JvmStatic fun uiCurrentWave(wave: Int): String = bundle().currentWave(wave)
            @JvmStatic fun uiExportCount(count: Int): String = bundle().exportCount(count)
            @JvmStatic fun uiChatType(): String = i("聊天")
            @JvmStatic fun uiServerMsgType(): String = i("服务器信息")
            @JvmStatic fun uiMarkCoordinatesType(): String = i("标记~坐标")
            @JvmStatic fun uiMarkPlayerType(): String = i("标记~玩家")
            @JvmStatic fun uiCommandType(): String = i("指令")
            @JvmStatic fun uiLogicNoticeType(): String = i("逻辑~通报")
            @JvmStatic fun uiLogicAnnouncementType(): String = i("逻辑~公告")
            @JvmStatic fun uiEventMapLoadType(): String = i("事件~载入地图")
            @JvmStatic fun uiEventWaveType(): String = i("事件~波次")

            @JvmStatic fun uiLogicHelperX(): String = i("逻辑辅助器[gold]X[]")
            @JvmStatic fun uiHideLogicHelper(): String = i("隐藏逻辑辅助器")
            @JvmStatic fun uiUpdatedEditedLogic(): String = i("[orange]已更新编辑的逻辑！")
            @JvmStatic fun uiRefreshEditedLogic(): String = i("更新编辑的逻辑")
            @JvmStatic fun uiOn(): String = i("开启")
            @JvmStatic fun uiOff(): String = i("关闭")
            @JvmStatic fun uiToggleState(label: String, state: String): String = bundle().toggleState(label, state)
            @JvmStatic fun uiFlashOnChange(): String = i("变动闪烁")
            @JvmStatic fun uiFlashOnVariableChange(): String = i("变量变动闪烁")
            @JvmStatic fun uiAutoRefreshVariables(): String = i("变量自动更新")
            @JvmStatic fun uiAutoRefreshVariablesHint(): String = i("自动刷新变量")
            @JvmStatic fun uiPaused(): String = i("已暂停")
            @JvmStatic fun uiGameResumed(): String = i("已继续游戏")
            @JvmStatic fun uiPauseLogicGameExecution(): String = i("暂停逻辑(游戏)运行")
            @JvmStatic fun uiRefreshInterval(): String = i("刷新间隔")
            @JvmStatic fun uiCopiedVariableNameHint(value: String): String = bundle().copiedVariableNameHint(value)
            @JvmStatic fun uiCopiedVariableAttributesHint(value: String): String = bundle().copiedVariableAttributesHint(value)
            @JvmStatic fun uiCopiedPrintBufferHint(value: String): String = bundle().copiedPrintBufferHint(value)
            @JvmStatic fun uiCopiedMemory(value: Double): String = bundle().copiedMemory(value)
            @JvmStatic fun uiNoPermissionToEditViewOnly(): String = i("[yellow]当前无权编辑，仅供查阅")
            @JvmStatic fun uiResetAllLinks(): String = i("重置所有链接")
            @JvmStatic fun uiExtractCodeFromSchematic(): String = i("从蓝图中选择代码")
            @JvmStatic fun uiSelectCode(): String = i("选择代码")
            @JvmStatic fun uiTipAllSchematicsContainingProcessors(): String = i("TIP: 所有包含处理器的蓝图")

            @JvmStatic fun uiBasic(): String = i("基础对比")
            @JvmStatic fun uiSquared(): String = i("平方对比")
            @JvmStatic fun uiArcImageConverter(): String = i("arc-图片转换器")
            @JvmStatic fun uiSelectAndImportPictures(): String = i("选择并导入图片，可将其转成画板、像素画或是逻辑画")
            @JvmStatic fun uiSelectImagePng(): String = i("选择图片[white](png)")
            @JvmStatic fun uiWarnImageTooLarge(): String = i("[orange]警告：图片可能过大，请尝试压缩图片")
            @JvmStatic fun uiFailedReadImage(error: Throwable): String = bundle().failedReadImage(error)
            @JvmStatic fun uiAutomaticallySaveAsBlueprint(): String = i("自动保存为蓝图")
            @JvmStatic fun uiZoomZoom(): String = i("缩放: ")
            @JvmStatic fun uiHueMode(): String = i("色调函数:")
            @JvmStatic fun uiLabelWithEmoji(label: String, emoji: String): String = bundle().labelWithEmoji(label, emoji)
            @JvmStatic fun uiLogicArtWebsite(): String = i("逻辑画网站")
            @JvmStatic fun uiPath(): String = i("路径")
            @JvmStatic fun uiName(): String = i("名称")
            @JvmStatic fun uiOriginalSize(): String = i("原始大小")
            @JvmStatic fun uiScaledSize(): String = i("缩放后大小")
            @JvmStatic fun uiCanvas(): String = i("画板")
            @JvmStatic fun uiArtboard(): String = i("画板++")
            @JvmStatic fun uiPixelArt(): String = i("像素画")
            @JvmStatic fun uiSize(): String = i("大小：")
            @JvmStatic fun uiSavedBlueprint(name: String): String = bundle().savedBlueprint(name)

            @JvmStatic fun uiItemSelectionHeight(value: Int): String = bundle().itemSelectionHeight(value)
            @JvmStatic fun uiItemSelectionWidth(value: Int): String = bundle().itemSelectionWidth(value)
            @JvmStatic fun uiCurrentVersion(version: String): String = bundle().currentVersion(version)
            @JvmStatic fun uiNewVersion(version: String): String = bundle().newVersion(version)
            @JvmStatic fun uiInvalidBackgroundImage(path: String): String = bundle().invalidBackgroundImage(path)
            @JvmStatic fun uiCurrentGameSpeed(speed: Float): String = bundle().currentGameSpeed(speed)
            @JvmStatic fun uiFpsLockEnabled(targetFps: Int): String = bundle().fpsLockEnabled(targetFps)
            @JvmStatic fun uiFpsLockCurrent(targetFps: Int): String = bundle().fpsLockCurrent(targetFps)
            @JvmStatic fun uiFpsLockOff(gameSpeed: Float): String = bundle().fpsLockOff(gameSpeed)
            @JvmStatic fun uiCurrentMap(mapName: String): String = bundle().currentMap(mapName)
            @JvmStatic fun uiWorldProcessorSummary(processors: Int, instructions: Int, chars: Int): String =
                bundle().worldProcessorSummary(processors, instructions, chars)

            @JvmStatic fun uiRecording(path: String): String = bundle().recording(path)
            @JvmStatic fun uiCoordinateDistance(x: Int, y: Int, distance: Int): String = bundle().coordinateDistance(x, y, distance)

            @JvmStatic fun uiAtPlayer(playerName: String?): String = bundle().atPlayer(playerName)
            @JvmStatic fun uiAtNoticeFrom(senderName: String): String = bundle().atNoticeFrom(senderName)
            @JvmStatic fun uiShareCode(code: String): String = bundle().shareCode(code)
            @JvmStatic fun uiShareHeader(version: String): String = bundle().shareHeader(version)
            @JvmStatic fun uiWaveContains(ground: Int, air: Int): String = bundle().waveContains(ground, air)
            @JvmStatic fun uiWaveTitle(wave: Int): String = bundle().waveTitle(wave)
            @JvmStatic fun uiWaveEta(remainingWaves: Int, eta: String): String = bundle().waveEta(remainingWaves, eta)

            @JvmStatic fun uiAbilityShieldCapacity(max: String, radius: String, recovery: String, cooldown: String): String =
                bundle().shieldCapacity(max, radius, recovery, cooldown)

            @JvmStatic fun uiAbilityLiquidExplode(total: String, liquidName: String, liquidEmoji: String, radius: String): String =
                bundle().liquidExplode(total, liquidName, liquidEmoji, radius)

            @JvmStatic fun uiAbilityLiquidRegen(absorb: String, liquidName: String, liquidEmoji: String, heal: String, maxHeal: String): String =
                bundle().liquidRegen(absorb, liquidName, liquidEmoji, heal, maxHeal)

            @JvmStatic fun uiAbilityLightning(probability: String, damage: String, length: String, speed: String): String =
                bundle().lightning(probability, damage, length, speed)

            @JvmStatic fun uiAbilityDurationTiles(seconds: String, tiles: String): String = bundle().durationTiles(seconds, tiles)
    }
}
