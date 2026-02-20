package mindustryX.bundles

import arc.Core
import mindustryX.bundles.ui.UiCoreText
import mindustryX.bundles.ui.UiShareText
import mindustryX.bundles.ui.UiStatText
import mindustryX.bundles.ui.isChineseLocale

object UiTexts {
    private const val uiPrefix = "mdtx.ui."

    @JvmStatic
    fun ui(key: String): String = try {
        Core.bundle.get("$uiPrefix$key")
    } catch (_: Throwable) {
        key
    }

    @JvmStatic
    fun bundle(): UiTexts = this

    fun mdtxReport(): String = if (isChineseLocale()) "问题反馈" else "Report Issue"
    fun mdtxQqLink(): String = if (isChineseLocale()) "QQ交流群" else "QQ Group"
    fun modsRecommendTitle(): String = if (isChineseLocale()) "[accent]MdtX[]推荐辅助模组列表" else "[accent]MdtX[]Recommended Mods List"
    fun modsRecommendInfo(): String = if (isChineseLocale()) "精选辅助模组" else "Selected Mods"
    fun modsRecommendLastUpdated(value: Any?): String = if (isChineseLocale()) "推荐列表更新时间：$value" else "Recommended List Last Updated: $value"
    fun modsRecommendModName(value: Any?): String = if (isChineseLocale()) "模组：$value" else "Mod: $value"
    fun modsRecommendModAuthor(value: Any?): String = if (isChineseLocale()) "作者：$value" else "Author: $value"
    fun modsRecommendModMinGameVersion(value: Any?): String = if (isChineseLocale()) "最低支持游戏版本：$value" else "Minimum Supported Game Version: $value"
    fun modsRecommendModLastUpdated(value: Any?): String = if (isChineseLocale()) "上次更新时间：$value" else "Last Updated: $value"
    fun modsRecommendModStars(value: Any?): String = if (isChineseLocale()) "Github收藏数：$value" else "Github Stars: $value"
    fun mdtxShareItem(name: Any?, stock: Any?, production: Any?): String =
        if (isChineseLocale()) "${name}：库存 ${stock}，产量 ${production}/秒" else "${name}: Stock ${stock}, Production ${production}/s"
    fun mdtxShareUnit(name: Any?, count: Any?, limit: Any?): String =
        if (isChineseLocale()) "${name}：数量 ${count}，上限 ${limit}" else "${name}: Count ${count}, Limit ${limit}"

    @JvmStatic fun uiJavaWarnLog(javaVersion: String): String = UiCoreText.javaWarnLog(javaVersion)
    @JvmStatic fun uiJavaWarnDialog(javaVersion: String): String = UiCoreText.javaWarnDialog(javaVersion)
    @JvmStatic fun uiWindowTitle(version: String, enabledMods: Int, totalMods: Int, width: Int, height: Int): String =
        UiCoreText.windowTitle(version, enabledMods, totalMods, width, height)

    @JvmStatic fun uiArcMessageCenter(): String = ui("arc_message_center")
    @JvmStatic fun uiMaxChatHistoryHint(): String = ui("maximum_storage_of_chat_history_too_high_may_cause_lag")
    @JvmStatic fun uiChatHistoryCleanupHint(): String = ui("chat_history_exceeding_the_limit_will_be_cleared_when_loading_the_map")
    @JvmStatic fun uiClear(): String = ui("clear")
    @JvmStatic fun uiExport(): String = ui("export")
    @JvmStatic fun uiExportChatHistory(): String = ui("export_chat_history")
    @JvmStatic fun uiLoadMap(mapName: String): String = UiCoreText.loadMap(mapName)
    @JvmStatic fun uiIntroduction(description: String): String = UiCoreText.introduction(description)
    @JvmStatic fun uiWaveEvent(wave: Int, detail: String): String = UiCoreText.waveEvent(wave, detail)
    @JvmStatic fun uiCopiedChatRecord(): String = ui("copied_this_chat_record")
    @JvmStatic fun uiExportHeader(version: String): String = UiCoreText.exportHeader(version)
    @JvmStatic fun uiExportMap(mapName: String, mode: String): String = UiCoreText.exportMap(mapName, mode)
    @JvmStatic fun uiCurrentWave(wave: Int): String = UiCoreText.currentWave(wave)
    @JvmStatic fun uiExportCount(count: Int): String = UiCoreText.exportCount(count)
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
    @JvmStatic fun uiToggleState(label: String, state: String): String = UiCoreText.toggleState(label, state)
    @JvmStatic fun uiFlashOnChange(): String = ui("flash_on_change")
    @JvmStatic fun uiFlashOnVariableChange(): String = ui("flash_on_variable_change")
    @JvmStatic fun uiAutoRefreshVariables(): String = ui("auto_refresh_variables")
    @JvmStatic fun uiAutoRefreshVariablesHint(): String = ui("automatically_refresh_variables")
    @JvmStatic fun uiPaused(): String = ui("paused")
    @JvmStatic fun uiGameResumed(): String = ui("game_resumed")
    @JvmStatic fun uiPauseLogicGameExecution(): String = ui("pause_logic_game_execution")
    @JvmStatic fun uiRefreshInterval(): String = ui("refresh_interval")
    @JvmStatic fun uiCopiedVariableNameHint(value: String): String = UiCoreText.copiedVariableNameHint(value)
    @JvmStatic fun uiCopiedVariableAttributesHint(value: String): String = UiCoreText.copiedVariableAttributesHint(value)
    @JvmStatic fun uiCopiedPrintBufferHint(value: String): String = UiCoreText.copiedPrintBufferHint(value)
    @JvmStatic fun uiCopiedMemory(value: Double): String = UiCoreText.copiedMemory(value)
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
    @JvmStatic fun uiFailedReadImage(error: Any?): String = UiCoreText.failedReadImage(error)
    @JvmStatic fun uiAutomaticallySaveAsBlueprint(): String = ui("automatically_save_as_blueprint")
    @JvmStatic fun uiZoomZoom(): String = ui("zoom_zoom")
    @JvmStatic fun uiHueMode(): String = ui("hue_mode")
    @JvmStatic fun uiLabelWithEmoji(label: String, emoji: String): String = UiCoreText.labelWithEmoji(label, emoji)
    @JvmStatic fun uiLogicArtWebsite(): String = ui("logic_art_website")
    @JvmStatic fun uiPath(): String = ui("path")
    @JvmStatic fun uiName(): String = ui("name")
    @JvmStatic fun uiOriginalSize(): String = ui("original_size")
    @JvmStatic fun uiScaledSize(): String = ui("scaled_size")
    @JvmStatic fun uiCanvas(): String = ui("canvas")
    @JvmStatic fun uiArtboard(): String = ui("artboard")
    @JvmStatic fun uiPixelArt(): String = ui("pixel_art")
    @JvmStatic fun uiSize(): String = ui("size")
    @JvmStatic fun uiSavedBlueprint(name: String): String = UiCoreText.savedBlueprint(name)

    @JvmStatic fun uiItemSelectionHeight(value: Int): String = UiCoreText.itemSelectionHeight(value)
    @JvmStatic fun uiItemSelectionWidth(value: Int): String = UiCoreText.itemSelectionWidth(value)
    @JvmStatic fun uiCurrentVersion(version: String): String = UiCoreText.currentVersion(version)
    @JvmStatic fun uiNewVersion(version: String): String = UiCoreText.newVersion(version)
    @JvmStatic fun uiInvalidBackgroundImage(path: String): String = UiCoreText.invalidBackgroundImage(path)
    @JvmStatic fun uiCurrentGameSpeed(speed: Float): String = UiCoreText.currentGameSpeed(speed)
    @JvmStatic fun uiFpsLockEnabled(targetFps: Int): String = UiCoreText.fpsLockEnabled(targetFps)
    @JvmStatic fun uiFpsLockCurrent(targetFps: Int): String = UiCoreText.fpsLockCurrent(targetFps)
    @JvmStatic fun uiFpsLockOff(gameSpeed: Float): String = UiCoreText.fpsLockOff(gameSpeed)
    @JvmStatic fun uiCurrentMap(mapName: String): String = UiCoreText.currentMap(mapName)
    @JvmStatic fun uiWorldProcessorSummary(processors: Int, instructions: Int, chars: Int): String =
        UiCoreText.worldProcessorSummary(processors, instructions, chars)
    @JvmStatic fun uiRecording(path: String): String = UiCoreText.recording(path)
    @JvmStatic fun uiCoordinateDistance(x: Int, y: Int, distance: Int): String = UiCoreText.coordinateDistance(x, y, distance)

    @JvmStatic fun uiAtPlayer(playerName: String?): String = UiShareText.atPlayer(playerName)
    @JvmStatic fun uiAtNoticeFrom(senderName: String): String = UiShareText.atNoticeFrom(senderName)
    @JvmStatic fun uiShareCode(code: String): String = UiShareText.shareCode(code)
    @JvmStatic fun uiShareHeader(version: String): String = UiShareText.shareHeader(version)
    @JvmStatic fun uiWaveContains(ground: Int, air: Int): String = UiShareText.waveContains(ground, air)
    @JvmStatic fun uiWaveTitle(wave: Int): String = UiShareText.waveTitle(wave)
    @JvmStatic fun uiWaveEta(remainingWaves: Int, eta: String): String = UiShareText.waveEta(remainingWaves, eta)

    @JvmStatic fun uiAbilityShieldCapacity(max: String, radius: String, recovery: String, cooldown: String): String =
        UiStatText.shieldCapacity(max, radius, recovery, cooldown)
    @JvmStatic fun uiAbilityLiquidExplode(total: String, liquidName: String, liquidEmoji: String, radius: String): String =
        UiStatText.liquidExplode(total, liquidName, liquidEmoji, radius)
    @JvmStatic fun uiAbilityLiquidRegen(absorb: String, liquidName: String, liquidEmoji: String, heal: String, maxHeal: String): String =
        UiStatText.liquidRegen(absorb, liquidName, liquidEmoji, heal, maxHeal)
    @JvmStatic fun uiAbilityLightning(probability: String, damage: String, length: String, speed: String): String =
        UiStatText.lightning(probability, damage, length, speed)
    @JvmStatic fun uiAbilityDurationTiles(seconds: String, tiles: String): String = UiStatText.durationTiles(seconds, tiles)
}
