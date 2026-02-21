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

    override fun simple(key: String): String = zhToEn[key] ?: key
    override val labelsResFile: String get() = "labels_en"

    private val placeholderBracePattern = Regex("""\{\d+}""")
    private val placeholderAtPattern = Regex("(^|[^A-Za-z0-9])@($|[^A-Za-z0-9])")
    private fun isConstantTextKey(key: String): Boolean {
        if (placeholderBracePattern.containsMatchIn(key)) return false
        if (placeholderAtPattern.containsMatchIn(key)) return false
        return true
    }

    private val zhToEn = hashMapOf(
        "\n[white]分走了单位:" to "\n[white] took units:",
        "<永久状态>" to "<Permanent Status>",
        "<瞬间状态>" to "<Temporary State>",
        ">> 雷达扫描中 <<" to ">> Radar Scanning <<",
        "ARC-AI设定器" to "ARC-AI Configurator",
        "ARC-中央监控室" to "ARC Message Center",
        "ARC-矿物统计" to "ARC Ore Statistics",
        "Kotlin语言标准库" to "Kotlin language standard library",
        "TIP: 所有包含处理器的蓝图" to "TIP: All schematics containing processors",
        "UI图标大全" to "UI Icon Library",
        "[accent]建速" to "[accent]Build Speed",
        "[acid]血量" to "[acid]HP",
        "[cyan]标记模式,点击屏幕标记." to "[cyan]Mark mode: tap the screen to place a mark.",
        "[cyan]移速" to "[cyan]Movement speed",
        "[green]点击屏幕返回" to "[green]Click the screen to return",
        "[green]点击屏幕采集坐标" to "[green]Tap the screen to capture coordinates",
        "[orange]你被戳了一下，请注意查看信息框哦~" to "[orange]You were poked. Please check the message dialog.",
        "[orange]已更新编辑的逻辑！" to "[orange]Updated edited logic!",
        "[orange]生成的单位会飞起来" to "[orange]spawns units that fly",
        "[orange]生成！" to "[orange]spawned!",
        "[orange]警告：图片可能过大，请尝试压缩图片" to "[orange]Warning: Image may be too large, please try compressing image",
        "[purple]阻力" to "[purple]Resistance",
        "[red]伤害" to "[red]Damage",
        "[red]当前单位不可建筑" to "[red]Current unit cannot build",
        "[red]血量：" to "[red]HP:",
        "[red]这是一个作弊功能[]\n快速跳转到目标波次(不刷兵)" to "[red]This is a cheat feature[]\nJump to the target wave instantly (without spawning enemies)",
        "[teal]装甲" to "[teal]Armor",
        "[violet]攻速" to "[violet]Attack speed",
        "[white]法" to "[white]S",
        "[yellow]建筑过多，避免卡顿，仅保留前1000个规划" to "[yellow]There are too many buildings to avoid lag and only keep the first 1000 plans.",
        "[yellow]当前无权编辑，仅供查阅" to "[yellow]No permission to edit; view only.",
        "[yellow]当前选中物品为空，请在物品栏选中建筑" to "[yellow]Current selection is empty; select a block in the inventory.",
        "[yellow]护盾：" to "[yellow]Shield:",
        "[yellow]添加新指令前，请先保存编辑的指令" to "[yellow]Before adding new instructions, please save the edited instructions first.",
        "arc-图片转换器" to "ARC Image Converter",
        "minerAI-矿物筛选器" to "Miner AI - Ore Filter",
        "ui大全" to "UI Toolkit",
        "一键装填" to "Auto Fill",
        "上传失败，再重试一下？" to "Upload failed, try again?",
        "丢下载荷" to "Drop Payload",
        "中央监控室" to "Message Center",
        "事件~波次" to "Event~Wave",
        "事件~载入地图" to "Event~Map Load",
        "从蓝图中选择代码" to "Extract code from schematic",
        "你已是最新版本，不需要更新！" to "You are already on the latest version.",
        "保护AI" to "Defender AI",
        "信" to "M",
        "信息板全显示" to "Show all message blocks",
        "修复AI" to "Repair AI",
        "像素画" to "Pixel Art",
        "允许的范围：2~9999" to "Allowed range: 2~9999",
        "克隆" to "Clone",
        "全局检查" to "Global range",
        "全部显示" to "Show all",
        "关闭" to "Off",
        "兵" to "U",
        "兵种显示" to "Unit Rendering",
        "分享单位数量" to "Share unit count",
        "分享库存情况" to "Share inventory status",
        "分享波次信息" to "Share wave information",
        "分享电力情况" to "Share power status",
        "分享者：" to "Shared by:",
        "创世神" to "God Mode",
        "创建回放出错!" to "Failed to create replay!",
        "前缀添加/t" to "Add prefix /t",
        "加载回放文件" to "Load replay file",
        "单位工厂" to "Unit Factory",
        "单位工厂-X" to "Unit Factory-X",
        "单位：" to "Unit:",
        "原始大小" to "Original size",
        "原版" to "Vanilla",
        "发布说明" to "Release Notes",
        "受不了，直接投降？" to "Are you sure you want to surrender?",
        "变动闪烁" to "Flash on Change",
        "变量变动闪烁" to "Flash on variable change",
        "变量自动更新" to "Auto-refresh variables",
        "只显示建筑状态" to "Show building status only",
        "同步一波" to "Sync a wave",
        "名称" to "Name",
        "回放统计" to "Replay Stats",
        "图标" to "Icon",
        "在建造列表加入被摧毁建筑" to "Add destroyed buildings to build queue",
        "在目标点附近的这个范围内随机生成" to "Randomly generated within this range near the target point",
        "地形蓝图" to "Terrain Blueprint",
        "块" to "B",
        "基础对比" to "Basic",
        "填满核心的所有资源" to "Fill core resources",
        "墙" to "W",
        "墙体阴影显示" to "Wall Shadow Rendering",
        "子弹显示" to "Bullet Rendering",
        "导出" to "Export",
        "导出聊天记录" to "Export chat history",
        "将时间流速加快到两倍" to "Speed up time to 2x",
        "将时间流速放慢到一半" to "Slow the flow of time to half",
        "小地图显示" to "Minimap",
        "已保存至剪贴板" to "Saved to clipboard",
        "已导出本条聊天记录" to "Copied this chat record",
        "已暂停" to "Paused",
        "已移除逻辑视角锁定" to "Logic camera lock removed",
        "已继续游戏" to "Game resumed",
        "帧率模拟" to "Frame rate simulation",
        "平方对比" to "Squared",
        "序号" to "Index",
        "建筑显示" to "Block Rendering",
        "建筑：" to "Buildings:",
        "建造区域" to "Build area",
        "开关" to "Toggle",
        "开关自己队的无限火力" to "Toggle your team's Cheat",
        "开启" to "On",
        "弹" to "P",
        "强制助推" to "Force Boost",
        "强制跳波" to "Force skip waves",
        "当前不可用的面板:" to "Panels currently unavailable:",
        "当前选定区域为空，请通过F规划区域" to "The currently selected area is empty. Please use F to plan the area.",
        "录制出错!" to "Recording error!",
        "录制结束" to "Recording ended",
        "恢复原速" to "Return to original speed",
        "恢复当前波次" to "Restore current wave",
        "战争迷雾" to "Fog of War",
        "打开发布页面" to "Open Release Page",
        "打开回放文件" to "Open playback file",
        "扫" to "S",
        "扫描模式" to "Scan Mode",
        "指令" to "Command",
        "指挥模式" to "Command Mode",
        "捡起载荷" to "Pick up payload",
        "携带物品:" to "Carried item:",
        "放置/替换" to "Place/Replace",
        "效" to "FX",
        "无限制" to "Unlimited",
        "无限火力" to "Cheat",
        "显示名" to "Display name",
        "显示并允许建造所有物品" to "Unlock and allow all blocks",
        "暂停建造" to "Construction suspended",
        "暂停时间" to "Pause time",
        "暂停逻辑(游戏)运行" to "Pause logic (game) execution",
        "暂存区" to "Staging area",
        "更多队伍选择" to "More Teams",
        "更新编辑的逻辑" to "Refresh edited logic",
        "最大储存聊天记录(过高可能导致卡顿)：" to "Maximum storage of chat history (too high may cause lag):",
        "服务器信息" to "Server Msg",
        "服务器信息版" to "Server Info Build",
        "未加载回放!" to "Replay not loaded!",
        "未输入指令" to "No command entered",
        "查找方块" to "Find blocks",
        "查看录制信息" to "View recording info",
        "标记~坐标" to "Mark~Coordinates",
        "标记~玩家" to "Mark~Player",
        "标记地图位置" to "Mark map location",
        "格" to "tiles",
        "检查更新失败，请稍后再试" to "Could not check for updates.\nPlease try again later.",
        "正式版" to "Stable Releases",
        "沙漏：" to "Hourglass:",
        "沙盒" to "Sandbox",
        "法国军礼" to "Surrender Vote",
        "波次信息" to "Wave Info",
        "波次设定" to "Wave Settings",
        "消息(@js 开头为脚本)" to "Message (@js starts with script)",
        "液体" to "Liquids",
        "添加队伍" to "Add Team",
        "添加面板" to "Add Panel",
        "清空" to "Clear",
        "清空核心的所有资源" to "Clear all core resources",
        "版" to "V",
        "物品" to "Item",
        "特效大全" to "Effects Library",
        "特效显示" to "Effects Rendering",
        "玩家建造区" to "Player build range",
        "生成位置:" to "Spawn location:",
        "生成数量:" to "Generate quantity:",
        "生成范围：" to "Spawn range:",
        "生成队伍:" to "Spawn Team:",
        "电力：" to "Power:",
        "画板" to "Canvas",
        "画板++" to "Artboard++",
        "瞬间完成" to "Instant",
        "矿机AI" to "Miner AI",
        "矿物信息" to "Ore Info",
        "矿物矿(地表/墙矿)" to "Ore Count (surface/wall)",
        "碰撞箱显示" to "Hitbox Overlay",
        "秒" to "sec",
        "移除逻辑锁定" to "Remove logic lock",
        "箱" to "H",
        "编辑器" to "Editor",
        "缩放" to "Zoom",
        "缩放: " to "Zoom: ",
        "缩放后大小" to "Scaled size",
        "聊天" to "Chat",
        "自动下载更新" to "Download and Install Update",
        "自动保存为蓝图" to "Automatically save as blueprint",
        "自动刷新变量" to "Automatically refresh variables",
        "自动攻击" to "Auto Attack",
        "自动更新" to "Check for Updates",
        "自杀" to "Self-Destruct",
        "色调函数:" to "Hue mode:",
        "蓝图代码过长，请点击链接查看" to "The blueprint code is too long, please click the link to view it",
        "蓝图代码链接：" to "Blueprint code link:",
        "蓝图代码：\n" to "Blueprint code: \n",
        "蓝图名：" to "Blueprint name:",
        "蓝图造价：" to "Blueprint cost:",
        "装载单位" to "Load Unit",
        "装载建筑" to "Load Building",
        "装载自己" to "Load self",
        "观" to "O",
        "观察者模式" to "Observer Mode",
        "规则：" to "Rules:",
        "视角脱离玩家" to "Detached camera",
        "解禁" to "Unlock",
        "警告：该页功能主要供单机作图使用" to "Single-player map tools only.",
        "设定查询波次" to "Set target wave",
        "设置目标" to "Set target",
        "该波次没有敌人" to "There are no enemies in this wave",
        "请不要频繁标记!" to "Please don't tag too often!",
        "读取回放失败!" to "Failed to read playback!",
        "超出限制的聊天记录将在载入地图时清除" to "Chat history exceeding the limit will be cleared when loading the map",
        "路径" to "Path",
        "进入传送带" to "Enter the conveyor belt",
        "选择代码" to "Select Code",
        "选择图片[white](png)" to "Select image[white](png)",
        "选择并导入图片，可将其转成画板、像素画或是逻辑画" to "Select and import pictures, which can be converted into artboards, pixel paintings or logic paintings.",
        "选择范围" to "Selection Range",
        "逻辑~公告" to "Logic~Announcement",
        "逻辑~通报" to "Logic~Notice",
        "逻辑画网站" to "Logic art website",
        "逻辑辅助器[gold]X[]" to "Logic Helper[gold]X[]",
        "重建AI" to "Builder AI",
        "重置" to "Reset",
        "重置所有链接" to "Reset all links",
        "锁定" to "Lock",
        "锁定上个标记点" to "Lock the last marked point",
        "队伍ID:" to "Team ID:",
        "队伍区域" to "Team range",
        "队伍选择器" to "Team selector",
        "队伍：" to "Team:",
        "隐藏全部建筑" to "Hide all buildings",
        "隐藏逻辑辅助器" to "Hide Logic Helper",
        "集合" to "Rally",
        "雷达开关" to "Radar Toggle",
        "雾" to "F",
        "预览版(更新更快,新功能体验,BUG修复)" to "Preview Releases\n(faster updates, new features, bug fixes)",
        "颜色" to "Color",
        "飞行模式" to "Flight mode",
    )
        .filterKeys(::isConstantTextKey)
        .toMap()
}
