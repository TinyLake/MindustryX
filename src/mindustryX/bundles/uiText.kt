package mindustryX.bundles

import arc.Core
import java.util.Locale

interface UiTextBundle {
    fun addDestroyedBuildingsToBuildQueue(): String = "在建造列表加入被摧毁建筑"
    fun addPanel(): String = "添加面板"
    fun addPrefixT(): String = "前缀添加/t"
    fun addTeam(): String = "添加队伍"
    fun airOnly(): String = "仅对空"
    fun airX(): String = ",空x"
    fun all(): String = "全部"
    fun allPlayers(): String = "全部玩家"
    fun allowedRange29999(): String = "允许的范围：2~9999"
    fun alwaysOn(): String = "一直开启"
    fun arcAiConfigurator(): String = "ARC-AI设定器"
    fun arcImageConverter(): String = "arc-图片转换器"
    fun arcMessageCenter(): String = "ARC-中央监控室"
    fun arcOreStatistics(): String = "ARC-矿物统计"
    fun argArgNdistanceArg(): String = "@,@\n距离: @"
    fun argSArgTiles(): String = "@s~@格"
    fun argShieldCapacityArgGridArgRecoveryArgSCooldown(): String = "@盾容~@格~@恢复~@s冷却"
    fun armor(): String = "[teal]装甲"
    fun artboard(): String = "画板++"
    fun attack(): String = "攻击"
    fun attackNoBorder(): String = "攻击去边框"
    fun attackSpeed(): String = "[violet]攻速"
    fun autoAttack(): String = "自动攻击"
    fun autoFill(): String = "一键装填"
    fun autoRefreshVariables(): String = "变量自动更新"
    fun automaticallyRefreshVariables(): String = "自动刷新变量"
    fun automaticallySaveAsBlueprint(): String = "自动保存为蓝图"
    fun basic(): String = "基础对比"
    fun beforeAddingNewInstructionsPleaseSaveTheEditedInstructionsFirst(): String = "[yellow]添加新指令前，请先保存编辑的指令"
    fun belowIsMdtx(): String = "下面是[MDTX-"
    fun blockRendering(): String = "建筑显示"
    fun blueprintCodeLink(): String = "蓝图代码链接："
    fun blueprintCodeN(): String = "蓝图代码：\n"
    fun blueprintCost(): String = "蓝图造价："
    fun blueprintName(): String = "蓝图名："
    fun buildArea(): String = "建造区域"
    fun buildSpeed(): String = "[accent]建速"
    fun builderAi(): String = "重建AI"
    fun buildings(): String = "建筑："
    fun builtInPrefix(): String = "[内置]"
    fun bulletRendering(): String = "子弹显示"
    fun cancel(): String = "取消"
    fun canvas(): String = "画板"
    fun carriedItem(): String = "携带物品:"
    fun chat(): String = "聊天"
    fun chatHistoryExceedingTheLimitWillBeClearedWhenLoadingTheMap(): String = "超出限制的聊天记录将在载入地图时清除"
    fun cheat(): String = "无限火力"
    fun checkForUpdates(): String = "自动更新"
    fun clear(): String = "清空"
    fun clearAllCoreResources(): String = "清空核心的所有资源"
    fun clickTheScreenToReturn(): String = "[green]点击屏幕返回"
    fun clone(): String = "克隆"
    fun color(): String = "颜色"
    fun command(): String = "指令"
    fun commandMode(): String = "指挥模式"
    fun constructionSuspended(): String = "暂停建造"
    fun containsGroundX(): String = "包含(地×"
    fun copiedPrintBufferNArg(): String = "复制信息版\n@"
    fun copiedSuccessfully(): String = "复制成功:"
    fun copiedThisChatRecord(): String = "已导出本条聊天记录"
    fun copiedVariableAttributesNArg(): String = "复制变量属性\n@"
    fun copiedVariableNameNArg(): String = "复制变量名\n@"
    fun couldNotCheckForUpdatesNpleaseTryAgainLater(): String = "检查更新失败，请稍后再试"
    fun cross(): String = "十字"
    fun currentFrameRateLockedArg(): String = "当前帧率锁定：@"
    fun currentGameSpeedArgTimes(): String = "当前游戏速度：@倍"
    fun currentMapArg(): String = "当前地图:@"
    fun currentMapName(): String = "*** 当前地图名称:"
    fun currentSelectionIsEmptySelectABlockInTheInventory(): String = "[yellow]当前选中物品为空，请在物品栏选中建筑"
    fun currentUnitCannotBuild(): String = "[red]当前单位不可建筑"
    fun damage(): String = "[red]伤害"
    fun defenderAi(): String = "保护AI"
    fun detachedCamera(): String = "视角脱离玩家"
    fun displayName(): String = "显示名"
    fun downloadAndInstallUpdate(): String = "自动下载更新"
    fun dropPayload(): String = "丢下载荷"
    fun eachCellAbsorbsArgSArgArgArgSAndReturnsBloodMaximumArgS(): String = "每格吸收@/s@@~@/s回血~最大@/s"
    fun editor(): String = "编辑器"
    fun effectsLibrary(): String = "特效大全"
    fun effectsRendering(): String = "特效显示"
    fun emptySelectionAreaHint(): String = "当前选定区域为空，请通过F规划区域"
    fun enterTheConveyorBelt(): String = "进入传送带"
    fun eventMapLoad(): String = "事件~载入地图"
    fun eventWave(): String = "事件~波次"
    fun export(): String = "导出"
    fun exportChatHistory(): String = "导出聊天记录"
    fun exportedInGameChatHistory(): String = "] 导出的游戏内聊天记录"
    fun extractCodeFromSchematic(): String = "从蓝图中选择代码"
    fun failedToCreateReplay(): String = "创建回放出错!"
    fun failedToReadImagePleaseTryAnotherImageN(): String = "读取图片失败，请尝试更换图片\n"
    fun failedToReadPlayback(): String = "读取回放失败!"
    fun fillCoreResources(): String = "填满核心的所有资源"
    fun findBlocks(): String = "查找方块"
    fun fixedSize(): String = "固定大小"
    fun flashOnChange(): String = "变动闪烁"
    fun flashOnVariableChange(): String = "变量变动闪烁"
    fun flightMode(): String = "飞行模式"
    fun fogOfWar(): String = "战争迷雾"
    fun forceBoost(): String = "强制助推"
    fun forceSkipWaves(): String = "强制跳波"
    fun frameRateLockModeEnabledNcurrentFrameRateLockArg(): String = "已开启帧率锁定模式\n当前帧率锁定：@"
    fun frameRateLockModeTurnedOffNcurrentGameSpeedArgTimes(): String = "已关闭帧率锁定模式\n当前游戏速度：@倍"
    fun frameRateSimulation(): String = "帧率模拟"
    fun fx(): String = "效"
    fun gameResumed(): String = "已继续游戏"
    fun generateQuantity(): String = "生成数量:"
    fun generatedLabel(): String = "[orange]已"
    fun globalRange(): String = "全局检查"
    fun godMode(): String = "创世神"
    fun groundOnly(): String = "仅对地"
    fun healthLabel(): String = "[red]血量："
    fun hideAllBuildings(): String = "隐藏全部建筑"
    fun hideLogicHelper(): String = "隐藏逻辑辅助器"
    fun hitboxOverlay(): String = "碰撞箱显示"
    fun hourglass(): String = "沙漏："
    fun hp(): String = "[acid]血量"
    fun hueMode(): String = "色调函数:"
    fun icon(): String = "图标"
    fun index(): String = "序号"
    fun instant(): String = "瞬间完成"
    fun item(): String = "物品"
    fun kotlinLanguageStandardLibrary(): String = "Kotlin语言标准库"
    fun lightningArgProbabilityArgDamageArgLengthArgXSpeed(): String = "闪电@概率~@伤害~@长度 @x速度"
    fun liquids(): String = "液体"
    fun loadBuilding(): String = "装载建筑"
    fun loadMap(): String = "载入地图："
    fun loadReplayFile(): String = "加载回放文件"
    fun loadSelf(): String = "装载自己"
    fun loadUnit(): String = "装载单位"
    fun lock(): String = "锁定"
    fun lockTheLastMarkedPoint(): String = "锁定上个标记点"
    fun logicAnnouncement(): String = "逻辑~公告"
    fun logicArtWebsite(): String = "逻辑画网站"
    fun logicCameraLockRemoved(): String = "已移除逻辑视角锁定"
    fun logicHelperX(): String = "逻辑辅助器[gold]X[]"
    fun logicNotice(): String = "逻辑~通报"
    fun markCoordinates(): String = "标记~坐标"
    fun markMapLocation(): String = "标记地图位置"
    fun markModeTapTheScreenToPlaceAMark(): String = "[cyan]标记模式,点击屏幕标记."
    fun markPlayer(): String = "标记~玩家"
    fun maximumStorageOfChatHistoryTooHighMayCauseLag(): String = "最大储存聊天记录(过高可能导致卡顿)："
    fun messageArgJsStartsWithScript(): String = "消息(@js 开头为脚本)"
    fun messageCenter(): String = "中央监控室"
    fun mindustryxVersion(): String = "MindustryX | 版本号"
    fun minerAi(): String = "矿机AI"
    fun minerAiOreFilter(): String = "minerAI-矿物筛选器"
    fun minimap(): String = "小地图显示"
    fun mdtxReport(): String = "问题反馈"
    fun mdtxQqLink(): String = "QQ交流群"
    fun modsEnabled(): String = "| mod启用"
    fun modsRecommendTitle(): String = "[accent]MdtX[]推荐辅助模组列表"
    fun modsRecommendInfo(): String = "精选辅助模组"
    fun modsRecommendLastUpdated(value: Any?): String = "推荐列表更新时间：$value"
    fun modsRecommendModName(value: Any?): String = "模组：$value"
    fun modsRecommendModAuthor(value: Any?): String = "作者：$value"
    fun modsRecommendModMinGameVersion(value: Any?): String = "最低支持游戏版本：$value"
    fun modsRecommendModLastUpdated(value: Any?): String = "上次更新时间：$value"
    fun modsRecommendModStars(value: Any?): String = "Github收藏数：$value"
    fun moreTeams(): String = "更多队伍选择"
    fun movementSpeed(): String = "[cyan]移速"
    fun nTookUnits(): String = "\n[white]分走了单位:"
    fun name(): String = "名称"
    fun no(): String = "第"
    fun noCommandEntered(): String = "未输入指令"
    fun noPermissionToEditViewOnly(): String = "[yellow]当前无权编辑，仅供查阅"
    fun observerMode(): String = "观察者模式"
    fun off(): String = "关闭"
    fun on(): String = "开启"
    fun openPlaybackFile(): String = "打开回放文件"
    fun openReleasePage(): String = "打开发布页面"
    fun oreCountSurfaceWall(): String = "矿物矿(地表/墙矿)"
    fun oreInfo(): String = "矿物信息"
    fun originalSize(): String = "原始大小"
    fun packetCount(): String = "数据包总数："
    fun panelsCurrentlyUnavailable(): String = "当前不可用的面板:"
    fun path(): String = "路径"
    fun pauseLogicGameExecution(): String = "暂停逻辑(游戏)运行"
    fun pauseTime(): String = "暂停时间"
    fun paused(): String = "已暂停"
    fun permanentStatus(): String = "<永久状态>"
    fun pickUpPayload(): String = "捡起载荷"
    fun pixelArt(): String = "像素画"
    fun placeReplace(): String = "放置/替换"
    fun placeholderX(): String = "倍"
    fun playbackLength(): String = "回放长度:"
    fun playbackVersion(): String = "回放版本:"
    fun playerBuildRange(): String = "玩家建造区"
    fun playerName(): String = "玩家名:"
    fun pleaseDonTTagTooOften(): String = "请不要频繁标记!"
    fun pleaseUseJava17OrHigherToRun(): String = ")。请使用Java 17或更高版本运行MindustryX。\n[grey]该警告不存在设置，请更新Java版本。"
    fun pokedYouCheckTheMessageDialog(): String = "[gold]戳了一下，请注意查看信息框哦~"
    fun power(): String = "电力："
    fun previewReleasesNFasterUpdatesNewFeaturesBugFixes(): String = "预览版(更新更快,新功能体验,BUG修复)"
    fun radarScanning(): String = ">> 雷达扫描中 <<"
    fun radarToggle(): String = "雷达开关"
    fun rally(): String = "集合"
    fun randomlyGeneratedWithinThisRangeNearTheTargetPoint(): String = "在目标点附近的这个范围内随机生成"
    fun recordingArg(): String = "录制中: @"
    fun recordingEnded(): String = "录制结束"
    fun recordingError(): String = "录制出错!"
    fun refreshEditedLogic(): String = "更新编辑的逻辑"
    fun refreshInterval(): String = "刷新间隔"
    fun releaseNotes(): String = "发布说明"
    fun removeLogicLock(): String = "移除逻辑锁定"
    fun repairAi(): String = "修复AI"
    fun replayCreationTime(): String = "回放创建时间:"
    fun replayNotLoaded(): String = "未加载回放!"
    fun replayStats(): String = "回放统计"
    fun reset(): String = "重置"
    fun resetAllLinks(): String = "重置所有链接"
    fun resistance(): String = "[purple]阻力"
    fun restoreCurrentWave(): String = "恢复当前波次"
    fun returnToOriginalSpeed(): String = "恢复原速"
    fun ring(): String = "虚圆"
    fun ringCross(): String = "圆十字"
    fun rules(): String = "规则："
    fun sandbox(): String = "沙盒"
    fun savedToClipboard(): String = "已保存至剪贴板"
    fun scaledSize(): String = "缩放后大小"
    fun scanMode(): String = "扫描模式"
    fun sec(): String = "秒"
    fun selectAndImportPicturesWhichCanBeConverted(): String = "选择并导入图片，可将其转成画板、像素画或是逻辑画"
    fun selectCode(): String = "选择代码"
    fun selectImagePng(): String = "选择图片[white](png)"
    fun selectionRange(): String = "选择范围"
    fun selfDestruct(): String = "自杀"
    fun serverInfoBuild(): String = "服务器信息版"
    fun serverIp(): String = "服务器ip:"
    fun serverMsg(): String = "服务器信息"
    fun setTarget(): String = "设置目标"
    fun setTargetWave(): String = "设定查询波次"
    fun shareInventoryStatus(): String = "分享库存情况"
    fun sharePowerStatus(): String = "分享电力情况"
    fun shareUnitCount(): String = "分享单位数量"
    fun shareWaveInformation(): String = "分享波次信息"
    fun sharedBy(): String = "分享者："
    fun shield(): String = "[yellow]护盾："
    fun shortcutBlockRender(): String = "块"
    fun shortcutBulletRender(): String = "弹"
    fun shortcutFog(): String = "雾"
    fun shortcutHitbox(): String = "箱"
    fun shortcutMessageCenter(): String = "信"
    fun shortcutObserverMode(): String = "观"
    fun shortcutScanMode(): String = "扫"
    fun shortcutServerInfo(): String = "版"
    fun shortcutSurrenderVote(): String = "[white]法"
    fun shortcutUnitRender(): String = "兵"
    fun shortcutWallShadow(): String = "墙"
    fun showAll(): String = "全部显示"
    fun showAllMessageBlocks(): String = "信息板全显示"
    fun showBuildingStatusOnly(): String = "只显示建筑状态"
    fun singlePlayerMapToolsOnly(): String = "警告：该页功能主要供单机作图使用"
    fun size(): String = "大小："
    fun slowTheFlowOfTimeToHalf(): String = "将时间流速放慢到一半"
    fun spawnLocation(): String = "生成位置:"
    fun spawnRange(): String = "生成范围："
    fun spawnTeam(): String = "生成队伍:"
    fun spawned(): String = "[orange]生成！"
    fun spawnsUnitsThatFly(): String = "[orange]生成的单位会飞起来"
    fun speedUpTimeTo2x(): String = "将时间流速加快到两倍"
    fun squared(): String = "平方对比"
    fun stableReleases(): String = "正式版"
    fun stagingArea(): String = "暂存区"
    fun successfullySelectedATotalOf(): String = "成功选取共"
    fun surrenderVote(): String = "法国军礼"
    fun surrenderVoteConfirm(): String = "受不了，直接投降？"
    fun syncAWave(): String = "同步一波"
    fun tapTheScreenToCaptureCoordinates(): String = "[green]点击屏幕采集坐标"
    fun team(): String = "队伍："
    fun teamId(): String = "队伍ID:"
    fun teamRange(): String = "队伍区域"
    fun teamSelector(): String = "队伍选择器"
    fun templateAtNoticeFrom(): String = "[gold]你被[white]{0}[gold]戳了一下，请注意查看信息框哦~"
    fun templateAtPlayer(): String = "<AT>戳了{0}[white]一下，并提醒他留意对话框"
    fun templateCopiedMemory(): String = "[cyan]复制内存[white]\n {0}"
    fun templateCurrentVersion(): String = "当前版本号: {0}"
    fun templateCurrentWave(): String = "*** 当前波次: {0}"
    fun templateExportCount(): String = "成功选取共{0}条记录，如下：\n"
    fun templateExportHeader(): String = "下面是[MDTX-{0}] 导出的游戏内聊天记录"
    fun templateExportMap(): String = "*** 当前地图名称: {0}(模式: {1})\n"
    fun templateFailedReadImage(): String = "读取图片失败，请尝试更换图片\n{0}"
    fun templateIntroduction(): String = "简介：{0}"
    fun templateInvalidBackgroundImage(): String = "背景图片无效: {0}"
    fun templateItemSelectionHeight(): String = "{0} 行"
    fun templateItemSelectionWidth(): String = "{0} 列"
    fun templateJavaWarnDialog(): String = "Java版本 {0} 过低，不受支持。\n[grey]该警告不存在设置，请更新Java版本。"
    fun templateJavaWarnLog(): String = "Java版本 {0} 过低，不受支持。请使用Java 17或更高版本运行MindustryX。"
    fun templateLabelWithEmoji(): String = "{0} {1}"
    fun templateLoadMap(): String = "载入地图：{0}"
    fun mdtxShareItem(name: Any?, stock: Any?, production: Any?): String = "${name}：库存 ${stock}，产量 ${production}/秒"
    fun mdtxShareUnit(name: Any?, count: Any?, limit: Any?): String = "${name}：数量 ${count}，上限 ${limit}"
    fun templateNewVersion(): String = "[green]发现新版本[]: {0}"
    fun templateSavedBlueprint(): String = "已保存蓝图：{0}"
    fun templateShareCode(): String = "<ARCxMDTX><Schem>[black]一坨兼容[] {0}"
    fun templateShareHeader(): String = "这是一条来自 MDTX-{0} 的分享记录\n"
    fun templateToggleState(): String = "{0}: {1}"
    fun templateWaveContains(): String = "包含(地×{0},空x{1}):"
    fun templateWaveEta(): String = "(还有{0}波, {1})"
    fun templateWaveEvent(): String = "波次：{0} | {1}"
    fun templateWaveTitle(): String = "第{0}波"
    fun templateWindowTitle(): String = "MindustryX | 版本号 {0} | mod启用{1}/{2}"
    fun temporaryStatus(): String = "<瞬间状态>"
    fun terrainBlueprint(): String = "地形蓝图"
    fun theBlueprintCodeIsTooLongPleaseClickTheLinkToViewIt(): String = "蓝图代码过长，请点击链接查看"
    fun theMapHasArgWorldProcessorsArgInstructionLinesAndArgCharacters(): String = "地图共有@个世处，总共@行指令，@个字符"
    fun thereAreNoEnemiesInThisWave(): String = "该波次没有敌人"
    fun thereAreTooManyBuildingsToAvoidLagAndOnlyKeepTheFirst1000Plans(): String = "[yellow]建筑过多，避免卡顿，仅保留前1000个规划"
    fun thisIsACheatFeatureNjumpToThe(): String = "[red]这是一个作弊功能[]\n快速跳转到目标波次(不刷兵)"
    fun thisIsAMessageFromMdtx(): String = "这是一条来自 MDTX-"
    fun tiles(): String = "格"
    fun tipAllSchematicsContainingProcessors(): String = "TIP: 所有包含处理器的蓝图"
    fun toggle(): String = "开关"
    fun toggleYourTeamSCheat(): String = "开关自己队的无限火力"
    fun totalArgArgArgArgTileRadius(): String = "总计@@@~@格半径"
    fun uiIconLibrary(): String = "UI图标大全"
    fun uiToolkit(): String = "ui大全"
    fun unique(): String = "独一无二"
    fun unit(): String = "单位："
    fun unitFactory(): String = "单位工厂"
    fun unitFactoryX(): String = "单位工厂-X"
    fun unitRendering(): String = "兵种显示"
    fun unlimited(): String = "无限制"
    fun unlock(): String = "解禁"
    fun unlockAndAllowAllBlocks(): String = "显示并允许建造所有物品"
    fun updatedEditedLogic(): String = "[orange]已更新编辑的逻辑！"
    fun uploadFailedTryAgain(): String = "上传失败，再重试一下？"
    fun vanilla(): String = "原版"
    fun viewRecordingInfo(): String = "查看录制信息"
    fun wallShadowRendering(): String = "墙体阴影显示"
    fun warningImageMayBeTooLargePleaseTryCompressingImage(): String = "[orange]警告：图片可能过大，请尝试压缩图片"
    fun wave(): String = "波"
    fun waveInPrefix(): String = "还有"
    fun waveInfo(): String = "波次信息"
    fun waveSettings(): String = "波次设定"
    fun waves(): String = "波次："
    fun wavesSuffix(): String = "波,"
    fun xScanSpeed(): String = "倍搜索速度"
    fun youAreAlreadyOnTheLatestVersion(): String = "你已是最新版本，不需要更新！"
    fun youWerePokedPleasePayAttentionToTheInformationBox(): String = "[orange]你被戳了一下，请注意查看信息框哦~"
    fun zoom(): String = "缩放"
    fun zoomX(): String = "缩放: x"
    fun zoomZoom(): String = "缩放: "

    object ZH : UiTextBundle

    object EN : UiTextBundle {
        override fun addDestroyedBuildingsToBuildQueue(): String = "Add destroyed buildings to build queue"
        override fun addPanel(): String = "Add Panel"
        override fun addPrefixT(): String = "Add prefix /t"
        override fun addTeam(): String = "Add Team"
        override fun airOnly(): String = "Air only"
        override fun airX(): String = ", air x"
        override fun all(): String = "All"
        override fun allPlayers(): String = "All players"
        override fun allowedRange29999(): String = "Allowed range: 2~9999"
        override fun alwaysOn(): String = "Always On"
        override fun arcAiConfigurator(): String = "ARC-AI Configurator"
        override fun arcImageConverter(): String = "ARC Image Converter"
        override fun arcMessageCenter(): String = "ARC Message Center"
        override fun arcOreStatistics(): String = "ARC Ore Statistics"
        override fun argArgNdistanceArg(): String = "@,@\nDistance: @"
        override fun argSArgTiles(): String = "@s~@ tiles"
        override fun argShieldCapacityArgGridArgRecoveryArgSCooldown(): String = "@ shield capacity ~ @ grid ~ @ recovery ~ @s cooldown"
        override fun armor(): String = "[teal]Armor"
        override fun artboard(): String = "Artboard++"
        override fun attack(): String = "Attack"
        override fun attackNoBorder(): String = "Attack (no border)"
        override fun attackSpeed(): String = "[violet]Attack speed"
        override fun autoAttack(): String = "Auto Attack"
        override fun autoFill(): String = "Auto Fill"
        override fun autoRefreshVariables(): String = "Auto-refresh variables"
        override fun automaticallyRefreshVariables(): String = "Automatically refresh variables"
        override fun automaticallySaveAsBlueprint(): String = "Automatically save as blueprint"
        override fun basic(): String = "Basic"
        override fun beforeAddingNewInstructionsPleaseSaveTheEditedInstructionsFirst(): String = "[yellow]Before adding new instructions, please save the edited instructions first."
        override fun belowIsMdtx(): String = "Below is [MDTX-"
        override fun blockRendering(): String = "Block Rendering"
        override fun blueprintCodeLink(): String = "Blueprint code link:"
        override fun blueprintCodeN(): String = "Blueprint code: \n"
        override fun blueprintCost(): String = "Blueprint cost:"
        override fun blueprintName(): String = "Blueprint name:"
        override fun buildArea(): String = "Build area"
        override fun buildSpeed(): String = "[accent]Build Speed"
        override fun builderAi(): String = "Builder AI"
        override fun buildings(): String = "Buildings:"
        override fun builtInPrefix(): String = "[Built-in]"
        override fun bulletRendering(): String = "Bullet Rendering"
        override fun cancel(): String = "Cancel"
        override fun canvas(): String = "Canvas"
        override fun carriedItem(): String = "Carried item:"
        override fun chat(): String = "Chat"
        override fun chatHistoryExceedingTheLimitWillBeClearedWhenLoadingTheMap(): String = "Chat history exceeding the limit will be cleared when loading the map"
        override fun cheat(): String = "Cheat"
        override fun checkForUpdates(): String = "Check for Updates"
        override fun clear(): String = "Clear"
        override fun clearAllCoreResources(): String = "Clear all core resources"
        override fun clickTheScreenToReturn(): String = "[green]Click the screen to return"
        override fun clone(): String = "Clone"
        override fun color(): String = "Color"
        override fun command(): String = "Command"
        override fun commandMode(): String = "Command Mode"
        override fun constructionSuspended(): String = "Construction suspended"
        override fun containsGroundX(): String = "Contains (ground x"
        override fun copiedPrintBufferNArg(): String = "Copied print buffer\n@"
        override fun copiedSuccessfully(): String = "Copied successfully:"
        override fun copiedThisChatRecord(): String = "Copied this chat record"
        override fun copiedVariableAttributesNArg(): String = "Copied variable attributes\n@"
        override fun copiedVariableNameNArg(): String = "Copied variable name\n@"
        override fun couldNotCheckForUpdatesNpleaseTryAgainLater(): String = "Could not check for updates.\nPlease try again later."
        override fun cross(): String = "Cross"
        override fun currentFrameRateLockedArg(): String = "Current frame rate locked: @"
        override fun currentGameSpeedArgTimes(): String = "Current game speed: @ times"
        override fun currentMapArg(): String = "Current map: @"
        override fun currentMapName(): String = "*** Current map name:"
        override fun currentSelectionIsEmptySelectABlockInTheInventory(): String = "[yellow]Current selection is empty; select a block in the inventory."
        override fun currentUnitCannotBuild(): String = "[red]Current unit cannot build"
        override fun damage(): String = "[red]Damage"
        override fun defenderAi(): String = "Defender AI"
        override fun detachedCamera(): String = "Detached camera"
        override fun displayName(): String = "Display name"
        override fun downloadAndInstallUpdate(): String = "Download and Install Update"
        override fun dropPayload(): String = "Drop Payload"
        override fun eachCellAbsorbsArgSArgArgArgSAndReturnsBloodMaximumArgS(): String = "Each cell absorbs @/s@@~heals @/s~up to @/s"
        override fun editor(): String = "Editor"
        override fun effectsLibrary(): String = "Effects Library"
        override fun effectsRendering(): String = "Effects Rendering"
        override fun emptySelectionAreaHint(): String = "The currently selected area is empty. Please use F to plan the area."
        override fun enterTheConveyorBelt(): String = "Enter the conveyor belt"
        override fun eventMapLoad(): String = "Event~Map Load"
        override fun eventWave(): String = "Event~Wave"
        override fun export(): String = "Export"
        override fun exportChatHistory(): String = "Export chat history"
        override fun exportedInGameChatHistory(): String = "] Exported in-game chat history"
        override fun extractCodeFromSchematic(): String = "Extract code from schematic"
        override fun failedToCreateReplay(): String = "Failed to create replay!"
        override fun failedToReadImagePleaseTryAnotherImageN(): String = "Failed to read image, please try another image\n"
        override fun failedToReadPlayback(): String = "Failed to read playback!"
        override fun fillCoreResources(): String = "Fill core resources"
        override fun findBlocks(): String = "Find blocks"
        override fun fixedSize(): String = "Fixed size"
        override fun flashOnChange(): String = "Flash on Change"
        override fun flashOnVariableChange(): String = "Flash on variable change"
        override fun flightMode(): String = "Flight mode"
        override fun fogOfWar(): String = "Fog of War"
        override fun forceBoost(): String = "Force Boost"
        override fun forceSkipWaves(): String = "Force skip waves"
        override fun frameRateLockModeEnabledNcurrentFrameRateLockArg(): String = "Frame rate lock mode enabled\nCurrent frame rate lock: @"
        override fun frameRateLockModeTurnedOffNcurrentGameSpeedArgTimes(): String = "Frame rate lock mode turned off\nCurrent game speed: @ times"
        override fun frameRateSimulation(): String = "Frame rate simulation"
        override fun fx(): String = "FX"
        override fun gameResumed(): String = "Game resumed"
        override fun generateQuantity(): String = "Generate quantity:"
        override fun generatedLabel(): String = "Generated"
        override fun globalRange(): String = "Global range"
        override fun godMode(): String = "God Mode"
        override fun groundOnly(): String = "Ground only"
        override fun healthLabel(): String = "[red]HP:"
        override fun hideAllBuildings(): String = "Hide all buildings"
        override fun hideLogicHelper(): String = "Hide Logic Helper"
        override fun hitboxOverlay(): String = "Hitbox Overlay"
        override fun hourglass(): String = "Hourglass:"
        override fun hp(): String = "[acid]HP"
        override fun hueMode(): String = "Hue mode:"
        override fun icon(): String = "Icon"
        override fun index(): String = "Index"
        override fun instant(): String = "Instant"
        override fun item(): String = "Item"
        override fun kotlinLanguageStandardLibrary(): String = "Kotlin language standard library"
        override fun lightningArgProbabilityArgDamageArgLengthArgXSpeed(): String = "Lightning @ probability ~ @ damage ~ @ length @x speed"
        override fun liquids(): String = "Liquids"
        override fun loadBuilding(): String = "Load Building"
        override fun loadMap(): String = "Load map:"
        override fun loadReplayFile(): String = "Load replay file"
        override fun loadSelf(): String = "Load self"
        override fun loadUnit(): String = "Load Unit"
        override fun lock(): String = "Lock"
        override fun lockTheLastMarkedPoint(): String = "Lock the last marked point"
        override fun logicAnnouncement(): String = "Logic~Announcement"
        override fun logicArtWebsite(): String = "Logic art website"
        override fun logicCameraLockRemoved(): String = "Logic camera lock removed"
        override fun logicHelperX(): String = "Logic Helper[gold]X[]"
        override fun logicNotice(): String = "Logic~Notice"
        override fun markCoordinates(): String = "Mark~Coordinates"
        override fun markMapLocation(): String = "Mark map location"
        override fun markModeTapTheScreenToPlaceAMark(): String = "[cyan]Mark mode: tap the screen to place a mark."
        override fun markPlayer(): String = "Mark~Player"
        override fun maximumStorageOfChatHistoryTooHighMayCauseLag(): String = "Maximum storage of chat history (too high may cause lag):"
        override fun messageArgJsStartsWithScript(): String = "Message (@js starts with script)"
        override fun messageCenter(): String = "Message Center"
        override fun mindustryxVersion(): String = "MindustryX | Version"
        override fun minerAi(): String = "Miner AI"
        override fun minerAiOreFilter(): String = "Miner AI - Ore Filter"
        override fun minimap(): String = "Minimap"
        override fun mdtxReport(): String = "Report Issue"
        override fun mdtxQqLink(): String = "QQ Group"
        override fun modsEnabled(): String = "| mods enabled"
        override fun modsRecommendTitle(): String = "[accent]MdtX[]Recommended Mods List"
        override fun modsRecommendInfo(): String = "Selected Mods"
        override fun modsRecommendLastUpdated(value: Any?): String = "Recommended List Last Updated: $value"
        override fun modsRecommendModName(value: Any?): String = "Mod: $value"
        override fun modsRecommendModAuthor(value: Any?): String = "Author: $value"
        override fun modsRecommendModMinGameVersion(value: Any?): String = "Minimum Supported Game Version: $value"
        override fun modsRecommendModLastUpdated(value: Any?): String = "Last Updated: $value"
        override fun modsRecommendModStars(value: Any?): String = "Github Stars: $value"
        override fun moreTeams(): String = "More Teams"
        override fun movementSpeed(): String = "[cyan]Movement speed"
        override fun nTookUnits(): String = "\n[white] took units:"
        override fun name(): String = "Name"
        override fun no(): String = "No."
        override fun noCommandEntered(): String = "No command entered"
        override fun noPermissionToEditViewOnly(): String = "[yellow]No permission to edit; view only."
        override fun observerMode(): String = "Observer Mode"
        override fun off(): String = "Off"
        override fun on(): String = "On"
        override fun openPlaybackFile(): String = "Open playback file"
        override fun openReleasePage(): String = "Open Release Page"
        override fun oreCountSurfaceWall(): String = "Ore Count (surface/wall)"
        override fun oreInfo(): String = "Ore Info"
        override fun originalSize(): String = "Original size"
        override fun packetCount(): String = "Packet count:"
        override fun panelsCurrentlyUnavailable(): String = "Panels currently unavailable:"
        override fun path(): String = "Path"
        override fun pauseLogicGameExecution(): String = "Pause logic (game) execution"
        override fun pauseTime(): String = "Pause time"
        override fun paused(): String = "Paused"
        override fun permanentStatus(): String = "<Permanent Status>"
        override fun pickUpPayload(): String = "Pick up payload"
        override fun pixelArt(): String = "Pixel Art"
        override fun placeReplace(): String = "Place/Replace"
        override fun placeholderX(): String = "x"
        override fun playbackLength(): String = "Playback length:"
        override fun playbackVersion(): String = "Playback version:"
        override fun playerBuildRange(): String = "Player build range"
        override fun playerName(): String = "Player name:"
        override fun pleaseDonTTagTooOften(): String = "Please don't tag too often!"
        override fun pleaseUseJava17OrHigherToRun(): String = "). Please use Java 17 or higher to run MindustryX.\n[grey]This warning cannot be disabled, please update Java."
        override fun pokedYouCheckTheMessageDialog(): String = "[gold]You were poked; check the message dialog."
        override fun power(): String = "Power:"
        override fun previewReleasesNFasterUpdatesNewFeaturesBugFixes(): String = "Preview Releases\n(faster updates, new features, bug fixes)"
        override fun radarScanning(): String = ">> Radar Scanning <<"
        override fun radarToggle(): String = "Radar Toggle"
        override fun rally(): String = "Rally"
        override fun randomlyGeneratedWithinThisRangeNearTheTargetPoint(): String = "Randomly generated within this range near the target point"
        override fun recordingArg(): String = "Recording: @"
        override fun recordingEnded(): String = "Recording ended"
        override fun recordingError(): String = "Recording error!"
        override fun refreshEditedLogic(): String = "Refresh edited logic"
        override fun refreshInterval(): String = "Refresh interval"
        override fun releaseNotes(): String = "Release Notes"
        override fun removeLogicLock(): String = "Remove logic lock"
        override fun repairAi(): String = "Repair AI"
        override fun replayCreationTime(): String = "Replay creation time:"
        override fun replayNotLoaded(): String = "Replay not loaded!"
        override fun replayStats(): String = "Replay Stats"
        override fun reset(): String = "Reset"
        override fun resetAllLinks(): String = "Reset all links"
        override fun resistance(): String = "[purple]Resistance"
        override fun restoreCurrentWave(): String = "Restore current wave"
        override fun returnToOriginalSpeed(): String = "Return to original speed"
        override fun ring(): String = "Ring"
        override fun ringCross(): String = "Ring Cross"
        override fun rules(): String = "Rules:"
        override fun sandbox(): String = "Sandbox"
        override fun savedToClipboard(): String = "Saved to clipboard"
        override fun scaledSize(): String = "Scaled size"
        override fun scanMode(): String = "Scan Mode"
        override fun sec(): String = "sec"
        override fun selectAndImportPicturesWhichCanBeConverted(): String = "Select and import pictures, which can be converted into artboards, pixel paintings or logic paintings."
        override fun selectCode(): String = "Select Code"
        override fun selectImagePng(): String = "Select image[white](png)"
        override fun selectionRange(): String = "Selection Range"
        override fun selfDestruct(): String = "Self-Destruct"
        override fun serverInfoBuild(): String = "Server Info Build"
        override fun serverIp(): String = "Server IP:"
        override fun serverMsg(): String = "Server Msg"
        override fun setTarget(): String = "Set target"
        override fun setTargetWave(): String = "Set target wave"
        override fun shareInventoryStatus(): String = "Share inventory status"
        override fun sharePowerStatus(): String = "Share power status"
        override fun shareUnitCount(): String = "Share unit count"
        override fun shareWaveInformation(): String = "Share wave information"
        override fun sharedBy(): String = "Shared by:"
        override fun shield(): String = "[yellow]Shield:"
        override fun shortcutBlockRender(): String = "B"
        override fun shortcutBulletRender(): String = "P"
        override fun shortcutFog(): String = "F"
        override fun shortcutHitbox(): String = "H"
        override fun shortcutMessageCenter(): String = "M"
        override fun shortcutObserverMode(): String = "O"
        override fun shortcutScanMode(): String = "S"
        override fun shortcutServerInfo(): String = "V"
        override fun shortcutSurrenderVote(): String = "[white]S"
        override fun shortcutUnitRender(): String = "U"
        override fun shortcutWallShadow(): String = "W"
        override fun showAll(): String = "Show all"
        override fun showAllMessageBlocks(): String = "Show all message blocks"
        override fun showBuildingStatusOnly(): String = "Show building status only"
        override fun singlePlayerMapToolsOnly(): String = "Single-player map tools only."
        override fun size(): String = "Size:"
        override fun slowTheFlowOfTimeToHalf(): String = "Slow the flow of time to half"
        override fun spawnLocation(): String = "Spawn location:"
        override fun spawnRange(): String = "Spawn range:"
        override fun spawnTeam(): String = "Spawn Team:"
        override fun spawned(): String = "[orange]spawned!"
        override fun spawnsUnitsThatFly(): String = "[orange]spawns units that fly"
        override fun speedUpTimeTo2x(): String = "Speed up time to 2x"
        override fun squared(): String = "Squared"
        override fun stableReleases(): String = "Stable Releases"
        override fun stagingArea(): String = "Staging area"
        override fun successfullySelectedATotalOf(): String = "Successfully selected a total of"
        override fun surrenderVote(): String = "Surrender Vote"
        override fun surrenderVoteConfirm(): String = "Are you sure you want to surrender?"
        override fun syncAWave(): String = "Sync a wave"
        override fun tapTheScreenToCaptureCoordinates(): String = "[green]Tap the screen to capture coordinates"
        override fun team(): String = "Team:"
        override fun teamId(): String = "Team ID:"
        override fun teamRange(): String = "Team range"
        override fun teamSelector(): String = "Team selector"
        override fun templateAtNoticeFrom(): String = "[gold]You were poked by [white]{0}[gold]! Check the message dialog."
        override fun templateAtPlayer(): String = "<AT> poked {0}[white] to check their messages."
        override fun templateCopiedMemory(): String = "[cyan]Copied memory[white]\n {0}"
        override fun templateCurrentVersion(): String = "Current version: {0}"
        override fun templateCurrentWave(): String = "*** Current wave: {0}"
        override fun templateExportCount(): String = "Successfully selected a total of {0} records:\n"
        override fun templateExportHeader(): String = "[MDTX-{0}] Exported in-game chat history"
        override fun templateExportMap(): String = "*** Current map name: {0} (mode: {1})\n"
        override fun templateFailedReadImage(): String = "Failed to read image, please try another image\n{0}"
        override fun templateIntroduction(): String = "Introduction: {0}"
        override fun templateInvalidBackgroundImage(): String = "Invalid background image: {0}"
        override fun templateItemSelectionHeight(): String = "{0} rows"
        override fun templateItemSelectionWidth(): String = "{0} columns"
        override fun templateJavaWarnDialog(): String = "Java version {0} is too low and unsupported.\n[grey]This warning cannot be disabled; please update Java."
        override fun templateJavaWarnLog(): String = "Java version {0} is too low and unsupported. Please use Java 17+ to run MindustryX."
        override fun templateLabelWithEmoji(): String = "{0} {1}"
        override fun templateLoadMap(): String = "Load map: {0}"
        override fun mdtxShareItem(name: Any?, stock: Any?, production: Any?): String = "${name}: Stock ${stock}, Production ${production}/s"
        override fun mdtxShareUnit(name: Any?, count: Any?, limit: Any?): String = "${name}: Count ${count}, Limit ${limit}"
        override fun templateNewVersion(): String = "[green]New version found[]: {0}"
        override fun templateSavedBlueprint(): String = "Saved blueprint: {0}"
        override fun templateShareCode(): String = "<ARCxMDTX><Schem>[black]compat code[] {0}"
        override fun templateShareHeader(): String = "This is a share log from MDTX-{0}\n"
        override fun templateToggleState(): String = "{0}: {1}"
        override fun templateWaveContains(): String = "Contains (ground x{0}, air x{1}):"
        override fun templateWaveEta(): String = "(in {0} waves, {1})"
        override fun templateWaveEvent(): String = "Waves: {0} | {1}"
        override fun templateWaveTitle(): String = "Wave {0}"
        override fun templateWindowTitle(): String = "MindustryX | Version {0} | mods enabled {1}/{2}"
        override fun temporaryStatus(): String = "<Temporary State>"
        override fun terrainBlueprint(): String = "Terrain Blueprint"
        override fun theBlueprintCodeIsTooLongPleaseClickTheLinkToViewIt(): String = "The blueprint code is too long, please click the link to view it"
        override fun theMapHasArgWorldProcessorsArgInstructionLinesAndArgCharacters(): String = "The map has @ world processors, @ instruction lines, and @ characters."
        override fun thereAreNoEnemiesInThisWave(): String = "There are no enemies in this wave"
        override fun thereAreTooManyBuildingsToAvoidLagAndOnlyKeepTheFirst1000Plans(): String = "[yellow]There are too many buildings to avoid lag and only keep the first 1000 plans."
        override fun thisIsACheatFeatureNjumpToThe(): String = "[red]This is a cheat feature[]\nJump to the target wave instantly (without spawning enemies)"
        override fun thisIsAMessageFromMdtx(): String = "This is a message from MDTX-"
        override fun tiles(): String = "tiles"
        override fun tipAllSchematicsContainingProcessors(): String = "TIP: All schematics containing processors"
        override fun toggle(): String = "Toggle"
        override fun toggleYourTeamSCheat(): String = "Toggle your team's Cheat"
        override fun totalArgArgArgArgTileRadius(): String = "Total @@@~@ tile radius"
        override fun uiIconLibrary(): String = "UI Icon Library"
        override fun uiToolkit(): String = "UI Toolkit"
        override fun unique(): String = "Unique"
        override fun unit(): String = "Unit:"
        override fun unitFactory(): String = "Unit Factory"
        override fun unitFactoryX(): String = "Unit Factory-X"
        override fun unitRendering(): String = "Unit Rendering"
        override fun unlimited(): String = "Unlimited"
        override fun unlock(): String = "Unlock"
        override fun unlockAndAllowAllBlocks(): String = "Unlock and allow all blocks"
        override fun updatedEditedLogic(): String = "[orange]Updated edited logic!"
        override fun uploadFailedTryAgain(): String = "Upload failed, try again?"
        override fun vanilla(): String = "Vanilla"
        override fun viewRecordingInfo(): String = "View recording info"
        override fun wallShadowRendering(): String = "Wall Shadow Rendering"
        override fun warningImageMayBeTooLargePleaseTryCompressingImage(): String = "[orange]Warning: Image may be too large, please try compressing image"
        override fun wave(): String = "Wave"
        override fun waveInPrefix(): String = "in"
        override fun waveInfo(): String = "Wave Info"
        override fun waveSettings(): String = "Wave Settings"
        override fun waves(): String = "Waves:"
        override fun wavesSuffix(): String = "waves,"
        override fun xScanSpeed(): String = "x scan speed"
        override fun youAreAlreadyOnTheLatestVersion(): String = "You are already on the latest version."
        override fun youWerePokedPleasePayAttentionToTheInformationBox(): String = "[orange]You were poked. Please check the message dialog."
        override fun zoom(): String = "Zoom"
        override fun zoomX(): String = "Zoom: x"
        override fun zoomZoom(): String = "Zoom: "
    }

    companion object {
        private val bundlesByLanguage = mapOf(
            Locale.CHINESE.language to ZH
        )

        fun default(): UiTextBundle = bundlesByLanguage[Core.bundle.locale.language] ?: EN
    }
}

object UiTexts {
    @JvmStatic
    fun bundle(): UiTextBundle = UiTextBundle.default()

    @JvmStatic
    fun ui(key: String): String = when(key) {
            "add_destroyed_buildings_to_build_queue" -> bundle().addDestroyedBuildingsToBuildQueue()
            "add_panel" -> bundle().addPanel()
            "add_prefix_t" -> bundle().addPrefixT()
            "add_team" -> bundle().addTeam()
            "air_only" -> bundle().airOnly()
            "air_x" -> bundle().airX()
            "all" -> bundle().all()
            "all_players" -> bundle().allPlayers()
            "allowed_range_2_9999" -> bundle().allowedRange29999()
            "always_on" -> bundle().alwaysOn()
            "arc_ai_configurator" -> bundle().arcAiConfigurator()
            "arc_image_converter" -> bundle().arcImageConverter()
            "arc_message_center" -> bundle().arcMessageCenter()
            "arc_ore_statistics" -> bundle().arcOreStatistics()
            "arg_arg_ndistance_arg" -> bundle().argArgNdistanceArg()
            "arg_s_arg_tiles" -> bundle().argSArgTiles()
            "arg_shield_capacity_arg_grid_arg_recovery_arg_s_cooldown" -> bundle().argShieldCapacityArgGridArgRecoveryArgSCooldown()
            "armor" -> bundle().armor()
            "artboard" -> bundle().artboard()
            "attack" -> bundle().attack()
            "attack_no_border" -> bundle().attackNoBorder()
            "attack_speed" -> bundle().attackSpeed()
            "auto_attack" -> bundle().autoAttack()
            "auto_fill" -> bundle().autoFill()
            "auto_refresh_variables" -> bundle().autoRefreshVariables()
            "automatically_refresh_variables" -> bundle().automaticallyRefreshVariables()
            "automatically_save_as_blueprint" -> bundle().automaticallySaveAsBlueprint()
            "basic" -> bundle().basic()
            "before_adding_new_instructions_please_save_the_edited_instructions_first" -> bundle().beforeAddingNewInstructionsPleaseSaveTheEditedInstructionsFirst()
            "below_is_mdtx" -> bundle().belowIsMdtx()
            "block_rendering" -> bundle().blockRendering()
            "blueprint_code_link" -> bundle().blueprintCodeLink()
            "blueprint_code_n" -> bundle().blueprintCodeN()
            "blueprint_cost" -> bundle().blueprintCost()
            "blueprint_name" -> bundle().blueprintName()
            "build_area" -> bundle().buildArea()
            "build_speed" -> bundle().buildSpeed()
            "builder_ai" -> bundle().builderAi()
            "buildings" -> bundle().buildings()
            "built_in_prefix" -> bundle().builtInPrefix()
            "bullet_rendering" -> bundle().bulletRendering()
            "cancel" -> bundle().cancel()
            "canvas" -> bundle().canvas()
            "carried_item" -> bundle().carriedItem()
            "chat" -> bundle().chat()
            "chat_history_exceeding_the_limit_will_be_cleared_when_loading_the_map" -> bundle().chatHistoryExceedingTheLimitWillBeClearedWhenLoadingTheMap()
            "cheat" -> bundle().cheat()
            "check_for_updates" -> bundle().checkForUpdates()
            "clear" -> bundle().clear()
            "clear_all_core_resources" -> bundle().clearAllCoreResources()
            "click_the_screen_to_return" -> bundle().clickTheScreenToReturn()
            "clone" -> bundle().clone()
            "color" -> bundle().color()
            "command" -> bundle().command()
            "command_mode" -> bundle().commandMode()
            "construction_suspended" -> bundle().constructionSuspended()
            "contains_ground_x" -> bundle().containsGroundX()
            "copied_print_buffer_n_arg" -> bundle().copiedPrintBufferNArg()
            "copied_successfully" -> bundle().copiedSuccessfully()
            "copied_this_chat_record" -> bundle().copiedThisChatRecord()
            "copied_variable_attributes_n_arg" -> bundle().copiedVariableAttributesNArg()
            "copied_variable_name_n_arg" -> bundle().copiedVariableNameNArg()
            "could_not_check_for_updates_nplease_try_again_later" -> bundle().couldNotCheckForUpdatesNpleaseTryAgainLater()
            "cross" -> bundle().cross()
            "current_frame_rate_locked_arg" -> bundle().currentFrameRateLockedArg()
            "current_game_speed_arg_times" -> bundle().currentGameSpeedArgTimes()
            "current_map_arg" -> bundle().currentMapArg()
            "current_map_name" -> bundle().currentMapName()
            "current_selection_is_empty_select_a_block_in_the_inventory" -> bundle().currentSelectionIsEmptySelectABlockInTheInventory()
            "current_unit_cannot_build" -> bundle().currentUnitCannotBuild()
            "damage" -> bundle().damage()
            "defender_ai" -> bundle().defenderAi()
            "detached_camera" -> bundle().detachedCamera()
            "display_name" -> bundle().displayName()
            "download_and_install_update" -> bundle().downloadAndInstallUpdate()
            "drop_payload" -> bundle().dropPayload()
            "each_cell_absorbs_arg_s_arg_arg_arg_s_and_returns_blood_maximum_arg_s" -> bundle().eachCellAbsorbsArgSArgArgArgSAndReturnsBloodMaximumArgS()
            "editor" -> bundle().editor()
            "effects_library" -> bundle().effectsLibrary()
            "effects_rendering" -> bundle().effectsRendering()
            "empty_selection_area_hint" -> bundle().emptySelectionAreaHint()
            "enter_the_conveyor_belt" -> bundle().enterTheConveyorBelt()
            "event_map_load" -> bundle().eventMapLoad()
            "event_wave" -> bundle().eventWave()
            "export" -> bundle().export()
            "export_chat_history" -> bundle().exportChatHistory()
            "exported_in_game_chat_history" -> bundle().exportedInGameChatHistory()
            "extract_code_from_schematic" -> bundle().extractCodeFromSchematic()
            "failed_to_create_replay" -> bundle().failedToCreateReplay()
            "failed_to_read_image_please_try_another_image_n" -> bundle().failedToReadImagePleaseTryAnotherImageN()
            "failed_to_read_playback" -> bundle().failedToReadPlayback()
            "fill_core_resources" -> bundle().fillCoreResources()
            "find_blocks" -> bundle().findBlocks()
            "fixed_size" -> bundle().fixedSize()
            "flash_on_change" -> bundle().flashOnChange()
            "flash_on_variable_change" -> bundle().flashOnVariableChange()
            "flight_mode" -> bundle().flightMode()
            "fog_of_war" -> bundle().fogOfWar()
            "force_boost" -> bundle().forceBoost()
            "force_skip_waves" -> bundle().forceSkipWaves()
            "frame_rate_lock_mode_enabled_ncurrent_frame_rate_lock_arg" -> bundle().frameRateLockModeEnabledNcurrentFrameRateLockArg()
            "frame_rate_lock_mode_turned_off_ncurrent_game_speed_arg_times" -> bundle().frameRateLockModeTurnedOffNcurrentGameSpeedArgTimes()
            "frame_rate_simulation" -> bundle().frameRateSimulation()
            "fx" -> bundle().fx()
            "game_resumed" -> bundle().gameResumed()
            "generate_quantity" -> bundle().generateQuantity()
            "generated_label" -> bundle().generatedLabel()
            "global_range" -> bundle().globalRange()
            "god_mode" -> bundle().godMode()
            "ground_only" -> bundle().groundOnly()
            "health_label" -> bundle().healthLabel()
            "hide_all_buildings" -> bundle().hideAllBuildings()
            "hide_logic_helper" -> bundle().hideLogicHelper()
            "hitbox_overlay" -> bundle().hitboxOverlay()
            "hourglass" -> bundle().hourglass()
            "hp" -> bundle().hp()
            "hue_mode" -> bundle().hueMode()
            "icon" -> bundle().icon()
            "index" -> bundle().index()
            "instant" -> bundle().instant()
            "item" -> bundle().item()
            "kotlin_language_standard_library" -> bundle().kotlinLanguageStandardLibrary()
            "lightning_arg_probability_arg_damage_arg_length_arg_x_speed" -> bundle().lightningArgProbabilityArgDamageArgLengthArgXSpeed()
            "liquids" -> bundle().liquids()
            "load_building" -> bundle().loadBuilding()
            "load_map" -> bundle().loadMap()
            "load_replay_file" -> bundle().loadReplayFile()
            "load_self" -> bundle().loadSelf()
            "load_unit" -> bundle().loadUnit()
            "lock" -> bundle().lock()
            "lock_the_last_marked_point" -> bundle().lockTheLastMarkedPoint()
            "logic_announcement" -> bundle().logicAnnouncement()
            "logic_art_website" -> bundle().logicArtWebsite()
            "logic_camera_lock_removed" -> bundle().logicCameraLockRemoved()
            "logic_helper_x" -> bundle().logicHelperX()
            "logic_notice" -> bundle().logicNotice()
            "mark_coordinates" -> bundle().markCoordinates()
            "mark_map_location" -> bundle().markMapLocation()
            "mark_mode_tap_the_screen_to_place_a_mark" -> bundle().markModeTapTheScreenToPlaceAMark()
            "mark_player" -> bundle().markPlayer()
            "maximum_storage_of_chat_history_too_high_may_cause_lag" -> bundle().maximumStorageOfChatHistoryTooHighMayCauseLag()
            "message_arg_js_starts_with_script" -> bundle().messageArgJsStartsWithScript()
            "message_center" -> bundle().messageCenter()
            "mindustryx_version" -> bundle().mindustryxVersion()
            "miner_ai" -> bundle().minerAi()
            "miner_ai_ore_filter" -> bundle().minerAiOreFilter()
            "minimap" -> bundle().minimap()
            "mods_enabled" -> bundle().modsEnabled()
            "more_teams" -> bundle().moreTeams()
            "movement_speed" -> bundle().movementSpeed()
            "n_took_units" -> bundle().nTookUnits()
            "name" -> bundle().name()
            "no" -> bundle().no()
            "no_command_entered" -> bundle().noCommandEntered()
            "no_permission_to_edit_view_only" -> bundle().noPermissionToEditViewOnly()
            "observer_mode" -> bundle().observerMode()
            "off" -> bundle().off()
            "on" -> bundle().on()
            "open_playback_file" -> bundle().openPlaybackFile()
            "open_release_page" -> bundle().openReleasePage()
            "ore_count_surface_wall" -> bundle().oreCountSurfaceWall()
            "ore_info" -> bundle().oreInfo()
            "original_size" -> bundle().originalSize()
            "packet_count" -> bundle().packetCount()
            "panels_currently_unavailable" -> bundle().panelsCurrentlyUnavailable()
            "path" -> bundle().path()
            "pause_logic_game_execution" -> bundle().pauseLogicGameExecution()
            "pause_time" -> bundle().pauseTime()
            "paused" -> bundle().paused()
            "permanent_status" -> bundle().permanentStatus()
            "pick_up_payload" -> bundle().pickUpPayload()
            "pixel_art" -> bundle().pixelArt()
            "place_replace" -> bundle().placeReplace()
            "placeholder_x" -> bundle().placeholderX()
            "playback_length" -> bundle().playbackLength()
            "playback_version" -> bundle().playbackVersion()
            "player_build_range" -> bundle().playerBuildRange()
            "player_name" -> bundle().playerName()
            "please_don_t_tag_too_often" -> bundle().pleaseDonTTagTooOften()
            "please_use_java_17_or_higher_to_run" -> bundle().pleaseUseJava17OrHigherToRun()
            "poked_you_check_the_message_dialog" -> bundle().pokedYouCheckTheMessageDialog()
            "power" -> bundle().power()
            "preview_releases_n_faster_updates_new_features_bug_fixes" -> bundle().previewReleasesNFasterUpdatesNewFeaturesBugFixes()
            "radar_scanning" -> bundle().radarScanning()
            "radar_toggle" -> bundle().radarToggle()
            "rally" -> bundle().rally()
            "randomly_generated_within_this_range_near_the_target_point" -> bundle().randomlyGeneratedWithinThisRangeNearTheTargetPoint()
            "recording_arg" -> bundle().recordingArg()
            "recording_ended" -> bundle().recordingEnded()
            "recording_error" -> bundle().recordingError()
            "refresh_edited_logic" -> bundle().refreshEditedLogic()
            "refresh_interval" -> bundle().refreshInterval()
            "release_notes" -> bundle().releaseNotes()
            "remove_logic_lock" -> bundle().removeLogicLock()
            "repair_ai" -> bundle().repairAi()
            "replay_creation_time" -> bundle().replayCreationTime()
            "replay_not_loaded" -> bundle().replayNotLoaded()
            "replay_stats" -> bundle().replayStats()
            "reset" -> bundle().reset()
            "reset_all_links" -> bundle().resetAllLinks()
            "resistance" -> bundle().resistance()
            "restore_current_wave" -> bundle().restoreCurrentWave()
            "return_to_original_speed" -> bundle().returnToOriginalSpeed()
            "ring" -> bundle().ring()
            "ring_cross" -> bundle().ringCross()
            "rules" -> bundle().rules()
            "sandbox" -> bundle().sandbox()
            "saved_to_clipboard" -> bundle().savedToClipboard()
            "scaled_size" -> bundle().scaledSize()
            "scan_mode" -> bundle().scanMode()
            "sec" -> bundle().sec()
            "select_and_import_pictures_which_can_be_converted" -> bundle().selectAndImportPicturesWhichCanBeConverted()
            "select_code" -> bundle().selectCode()
            "select_image_png" -> bundle().selectImagePng()
            "selection_range" -> bundle().selectionRange()
            "self_destruct" -> bundle().selfDestruct()
            "server_info_build" -> bundle().serverInfoBuild()
            "server_ip" -> bundle().serverIp()
            "server_msg" -> bundle().serverMsg()
            "set_target" -> bundle().setTarget()
            "set_target_wave" -> bundle().setTargetWave()
            "share_inventory_status" -> bundle().shareInventoryStatus()
            "share_power_status" -> bundle().sharePowerStatus()
            "share_unit_count" -> bundle().shareUnitCount()
            "share_wave_information" -> bundle().shareWaveInformation()
            "shared_by" -> bundle().sharedBy()
            "shield" -> bundle().shield()
            "shortcut_block_render" -> bundle().shortcutBlockRender()
            "shortcut_bullet_render" -> bundle().shortcutBulletRender()
            "shortcut_fog" -> bundle().shortcutFog()
            "shortcut_hitbox" -> bundle().shortcutHitbox()
            "shortcut_message_center" -> bundle().shortcutMessageCenter()
            "shortcut_observer_mode" -> bundle().shortcutObserverMode()
            "shortcut_scan_mode" -> bundle().shortcutScanMode()
            "shortcut_server_info" -> bundle().shortcutServerInfo()
            "shortcut_surrender_vote" -> bundle().shortcutSurrenderVote()
            "shortcut_unit_render" -> bundle().shortcutUnitRender()
            "shortcut_wall_shadow" -> bundle().shortcutWallShadow()
            "show_all" -> bundle().showAll()
            "show_all_message_blocks" -> bundle().showAllMessageBlocks()
            "show_building_status_only" -> bundle().showBuildingStatusOnly()
            "single_player_map_tools_only" -> bundle().singlePlayerMapToolsOnly()
            "size" -> bundle().size()
            "slow_the_flow_of_time_to_half" -> bundle().slowTheFlowOfTimeToHalf()
            "spawn_location" -> bundle().spawnLocation()
            "spawn_range" -> bundle().spawnRange()
            "spawn_team" -> bundle().spawnTeam()
            "spawned" -> bundle().spawned()
            "spawns_units_that_fly" -> bundle().spawnsUnitsThatFly()
            "speed_up_time_to_2x" -> bundle().speedUpTimeTo2x()
            "squared" -> bundle().squared()
            "stable_releases" -> bundle().stableReleases()
            "staging_area" -> bundle().stagingArea()
            "successfully_selected_a_total_of" -> bundle().successfullySelectedATotalOf()
            "surrender_vote" -> bundle().surrenderVote()
            "surrender_vote_confirm" -> bundle().surrenderVoteConfirm()
            "sync_a_wave" -> bundle().syncAWave()
            "tap_the_screen_to_capture_coordinates" -> bundle().tapTheScreenToCaptureCoordinates()
            "team" -> bundle().team()
            "team_id" -> bundle().teamId()
            "team_range" -> bundle().teamRange()
            "team_selector" -> bundle().teamSelector()
            "template.atNoticeFrom" -> bundle().templateAtNoticeFrom()
            "template.atPlayer" -> bundle().templateAtPlayer()
            "template.copiedMemory" -> bundle().templateCopiedMemory()
            "template.currentVersion" -> bundle().templateCurrentVersion()
            "template.currentWave" -> bundle().templateCurrentWave()
            "template.exportCount" -> bundle().templateExportCount()
            "template.exportHeader" -> bundle().templateExportHeader()
            "template.exportMap" -> bundle().templateExportMap()
            "template.failedReadImage" -> bundle().templateFailedReadImage()
            "template.introduction" -> bundle().templateIntroduction()
            "template.invalidBackgroundImage" -> bundle().templateInvalidBackgroundImage()
            "template.itemSelectionHeight" -> bundle().templateItemSelectionHeight()
            "template.itemSelectionWidth" -> bundle().templateItemSelectionWidth()
            "template.javaWarnDialog" -> bundle().templateJavaWarnDialog()
            "template.javaWarnLog" -> bundle().templateJavaWarnLog()
            "template.labelWithEmoji" -> bundle().templateLabelWithEmoji()
            "template.loadMap" -> bundle().templateLoadMap()
            "template.newVersion" -> bundle().templateNewVersion()
            "template.savedBlueprint" -> bundle().templateSavedBlueprint()
            "template.shareCode" -> bundle().templateShareCode()
            "template.shareHeader" -> bundle().templateShareHeader()
            "template.toggleState" -> bundle().templateToggleState()
            "template.waveContains" -> bundle().templateWaveContains()
            "template.waveEta" -> bundle().templateWaveEta()
            "template.waveEvent" -> bundle().templateWaveEvent()
            "template.waveTitle" -> bundle().templateWaveTitle()
            "template.windowTitle" -> bundle().templateWindowTitle()
            "temporary_status" -> bundle().temporaryStatus()
            "terrain_blueprint" -> bundle().terrainBlueprint()
            "the_blueprint_code_is_too_long_please_click_the_link_to_view_it" -> bundle().theBlueprintCodeIsTooLongPleaseClickTheLinkToViewIt()
            "the_map_has_arg_world_processors_arg_instruction_lines_and_arg_characters" -> bundle().theMapHasArgWorldProcessorsArgInstructionLinesAndArgCharacters()
            "there_are_no_enemies_in_this_wave" -> bundle().thereAreNoEnemiesInThisWave()
            "there_are_too_many_buildings_to_avoid_lag_and_only_keep_the_first_1000_plans" -> bundle().thereAreTooManyBuildingsToAvoidLagAndOnlyKeepTheFirst1000Plans()
            "this_is_a_cheat_feature_njump_to_the" -> bundle().thisIsACheatFeatureNjumpToThe()
            "this_is_a_message_from_mdtx" -> bundle().thisIsAMessageFromMdtx()
            "tiles" -> bundle().tiles()
            "tip_all_schematics_containing_processors" -> bundle().tipAllSchematicsContainingProcessors()
            "toggle" -> bundle().toggle()
            "toggle_your_team_s_cheat" -> bundle().toggleYourTeamSCheat()
            "total_arg_arg_arg_arg_tile_radius" -> bundle().totalArgArgArgArgTileRadius()
            "ui_icon_library" -> bundle().uiIconLibrary()
            "ui_toolkit" -> bundle().uiToolkit()
            "unique" -> bundle().unique()
            "unit" -> bundle().unit()
            "unit_factory" -> bundle().unitFactory()
            "unit_factory_x" -> bundle().unitFactoryX()
            "unit_rendering" -> bundle().unitRendering()
            "unlimited" -> bundle().unlimited()
            "unlock" -> bundle().unlock()
            "unlock_and_allow_all_blocks" -> bundle().unlockAndAllowAllBlocks()
            "updated_edited_logic" -> bundle().updatedEditedLogic()
            "upload_failed_try_again" -> bundle().uploadFailedTryAgain()
            "vanilla" -> bundle().vanilla()
            "view_recording_info" -> bundle().viewRecordingInfo()
            "wall_shadow_rendering" -> bundle().wallShadowRendering()
            "warning_image_may_be_too_large_please_try_compressing_image" -> bundle().warningImageMayBeTooLargePleaseTryCompressingImage()
            "wave" -> bundle().wave()
            "wave_in_prefix" -> bundle().waveInPrefix()
            "wave_info" -> bundle().waveInfo()
            "wave_settings" -> bundle().waveSettings()
            "waves" -> bundle().waves()
            "waves_suffix" -> bundle().wavesSuffix()
            "x_scan_speed" -> bundle().xScanSpeed()
            "you_are_already_on_the_latest_version" -> bundle().youAreAlreadyOnTheLatestVersion()
            "you_were_poked_please_pay_attention_to_the_information_box" -> bundle().youWerePokedPleasePayAttentionToTheInformationBox()
            "zoom" -> bundle().zoom()
            "zoom_x" -> bundle().zoomX()
            "zoom_zoom" -> bundle().zoomZoom()
        else -> key
    }

    @JvmStatic
    fun uiTemplate(key: String, vararg args: Any?): String {
        var text = ui("template.$key")
        args.forEachIndexed { index, arg ->
            text = text.replace("{$index}", arg?.toString() ?: "null")
        }
        return text
    }

    @JvmStatic fun uiJavaWarnLog(javaVersion: String): String = uiTemplate("javaWarnLog", javaVersion)
    @JvmStatic fun uiJavaWarnDialog(javaVersion: String): String = uiTemplate("javaWarnDialog", javaVersion)
    @JvmStatic fun uiWindowTitle(version: String, enabledMods: Int, totalMods: Int, width: Int, height: Int): String =
        uiTemplate("windowTitle", version, enabledMods, totalMods) + " | " + width + "x" + height
    
    @JvmStatic fun uiArcMessageCenter(): String = ui("arc_message_center")
    @JvmStatic fun uiMaxChatHistoryHint(): String = ui("maximum_storage_of_chat_history_too_high_may_cause_lag")
    @JvmStatic fun uiChatHistoryCleanupHint(): String = ui("chat_history_exceeding_the_limit_will_be_cleared_when_loading_the_map")
    @JvmStatic fun uiClear(): String = ui("clear")
    @JvmStatic fun uiExport(): String = ui("export")
    @JvmStatic fun uiExportChatHistory(): String = ui("export_chat_history")
    @JvmStatic fun uiLoadMap(mapName: String): String = uiTemplate("loadMap", mapName)
    @JvmStatic fun uiIntroduction(description: String): String = uiTemplate("introduction", description)
    @JvmStatic fun uiWaveEvent(wave: Int, detail: String): String = uiTemplate("waveEvent", wave, detail)
    @JvmStatic fun uiCopiedChatRecord(): String = ui("copied_this_chat_record")
    @JvmStatic fun uiExportHeader(version: String): String = uiTemplate("exportHeader", version)
    @JvmStatic fun uiExportMap(mapName: String, mode: String): String = uiTemplate("exportMap", mapName, mode)
    @JvmStatic fun uiCurrentWave(wave: Int): String = uiTemplate("currentWave", wave)
    @JvmStatic fun uiExportCount(count: Int): String = uiTemplate("exportCount", count)
    @JvmStatic fun uiChatType(): String = ui("chat")
    @JvmStatic fun uiServerMsgType(): String = ui("server_msg")
    @JvmStatic fun uiMarkCoordinatesType(): String = ui("mark_coordinates")
    @JvmStatic fun uiMarkPlayerType(): String = ui("mark_player")
    @JvmStatic fun uiCommandType(): String = ui("command")
    @JvmStatic fun uiLogicNoticeType(): String = ui("logic_notice")
    @JvmStatic fun uiLogicAnnouncementType(): String = ui("logic_announcement")
    @JvmStatic fun uiEventMapLoadType(): String = ui("event_map_load")
    @JvmStatic fun uiEventWaveType(): String = ui("event_wave")
    
    @JvmStatic fun uiLogicHelperX(): String = ui("logic_helper_x")
    @JvmStatic fun uiHideLogicHelper(): String = ui("hide_logic_helper")
    @JvmStatic fun uiUpdatedEditedLogic(): String = ui("updated_edited_logic")
    @JvmStatic fun uiRefreshEditedLogic(): String = ui("refresh_edited_logic")
    @JvmStatic fun uiOn(): String = ui("on")
    @JvmStatic fun uiOff(): String = ui("off")
    @JvmStatic fun uiToggleState(label: String, state: String): String = uiTemplate("toggleState", label, state)
    @JvmStatic fun uiFlashOnChange(): String = ui("flash_on_change")
    @JvmStatic fun uiFlashOnVariableChange(): String = ui("flash_on_variable_change")
    @JvmStatic fun uiAutoRefreshVariables(): String = ui("auto_refresh_variables")
    @JvmStatic fun uiAutoRefreshVariablesHint(): String = ui("automatically_refresh_variables")
    @JvmStatic fun uiPaused(): String = ui("paused")
    @JvmStatic fun uiGameResumed(): String = ui("game_resumed")
    @JvmStatic fun uiPauseLogicGameExecution(): String = ui("pause_logic_game_execution")
    @JvmStatic fun uiRefreshInterval(): String = ui("refresh_interval")
    @JvmStatic fun uiCopiedVariableNameHint(): String = ui("copied_variable_name_n_arg")
    @JvmStatic fun uiCopiedVariableAttributesHint(): String = ui("copied_variable_attributes_n_arg")
    @JvmStatic fun uiCopiedPrintBufferHint(): String = ui("copied_print_buffer_n_arg")
    @JvmStatic fun uiCopiedMemory(value: Double): String = uiTemplate("copiedMemory", value)
    @JvmStatic fun uiNoPermissionToEditViewOnly(): String = ui("no_permission_to_edit_view_only")
    @JvmStatic fun uiResetAllLinks(): String = ui("reset_all_links")
    @JvmStatic fun uiExtractCodeFromSchematic(): String = ui("extract_code_from_schematic")
    @JvmStatic fun uiSelectCode(): String = ui("select_code")
    @JvmStatic fun uiTipAllSchematicsContainingProcessors(): String = ui("tip_all_schematics_containing_processors")
    
    @JvmStatic fun uiBasic(): String = ui("basic")
    @JvmStatic fun uiSquared(): String = ui("squared")
    @JvmStatic fun uiArcImageConverter(): String = ui("arc_image_converter")
    @JvmStatic fun uiSelectAndImportPictures(): String = ui("select_and_import_pictures_which_can_be_converted")
    @JvmStatic fun uiSelectImagePng(): String = ui("select_image_png")
    @JvmStatic fun uiWarnImageTooLarge(): String = ui("warning_image_may_be_too_large_please_try_compressing_image")
    @JvmStatic fun uiFailedReadImage(error: Any?): String = uiTemplate("failedReadImage", error)
    @JvmStatic fun uiAutomaticallySaveAsBlueprint(): String = ui("automatically_save_as_blueprint")
    @JvmStatic fun uiZoomZoom(): String = ui("zoom_zoom")
    @JvmStatic fun uiHueMode(): String = ui("hue_mode")
    @JvmStatic fun uiLabelWithEmoji(label: String, emoji: String): String = uiTemplate("labelWithEmoji", label, emoji)
    @JvmStatic fun uiLogicArtWebsite(): String = ui("logic_art_website")
    @JvmStatic fun uiPath(): String = ui("path")
    @JvmStatic fun uiName(): String = ui("name")
    @JvmStatic fun uiOriginalSize(): String = ui("original_size")
    @JvmStatic fun uiScaledSize(): String = ui("scaled_size")
    @JvmStatic fun uiCanvas(): String = ui("canvas")
    @JvmStatic fun uiArtboard(): String = ui("artboard")
    @JvmStatic fun uiPixelArt(): String = ui("pixel_art")
    @JvmStatic fun uiSize(): String = ui("size")
    @JvmStatic fun uiSavedBlueprint(name: String): String = uiTemplate("savedBlueprint", name)
}
