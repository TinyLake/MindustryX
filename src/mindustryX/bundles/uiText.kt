package mindustryX.bundles

import arc.Core
import mindustryX.bundles.ui.UiTextBundle

object UiTexts {
    private const val uiPrefix = "mdtx.ui."

    @JvmStatic
    fun ui(key: String): String = try {
        Core.bundle.get("$uiPrefix$key")
    } catch (_: Throwable) {
        key
    }

    @JvmStatic
    fun bundle(): UiTextBundle = UiTextBundle.default()

    fun mdtxReport(): String = bundle().mdtxReport()
    fun mdtxQqLink(): String = bundle().mdtxQqLink()
    fun modsRecommendTitle(): String = bundle().modsRecommendTitle()
    fun modsRecommendInfo(): String = bundle().modsRecommendInfo()
    fun modsRecommendLastUpdated(value: Any?): String = bundle().modsRecommendLastUpdated(value)
    fun modsRecommendModName(value: Any?): String = bundle().modsRecommendModName(value)
    fun modsRecommendModAuthor(value: Any?): String = bundle().modsRecommendModAuthor(value)
    fun modsRecommendModMinGameVersion(value: Any?): String = bundle().modsRecommendModMinGameVersion(value)
    fun modsRecommendModLastUpdated(value: Any?): String = bundle().modsRecommendModLastUpdated(value)
    fun modsRecommendModStars(value: Any?): String = bundle().modsRecommendModStars(value)
    fun mdtxShareItem(name: Any?, stock: Any?, production: Any?): String = bundle().mdtxShareItem(name, stock, production)
    fun mdtxShareUnit(name: Any?, count: Any?, limit: Any?): String = bundle().mdtxShareUnit(name, count, limit)

    @JvmStatic fun uiJavaWarnLog(javaVersion: String): String = bundle().javaWarnLog(javaVersion)
    @JvmStatic fun uiJavaWarnDialog(javaVersion: String): String = bundle().javaWarnDialog(javaVersion)
    @JvmStatic fun uiWindowTitle(version: String, enabledMods: Int, totalMods: Int, width: Int, height: Int): String =
        bundle().windowTitle(version, enabledMods, totalMods, width, height)

    @JvmStatic fun uiArcMessageCenter(): String = ui("arc_message_center")
    @JvmStatic fun uiMaxChatHistoryHint(): String = ui("maximum_storage_of_chat_history_too_high_may_cause_lag")
    @JvmStatic fun uiChatHistoryCleanupHint(): String = ui("chat_history_exceeding_the_limit_will_be_cleared_when_loading_the_map")
    @JvmStatic fun uiClear(): String = ui("clear")
    @JvmStatic fun uiExport(): String = ui("export")
    @JvmStatic fun uiExportChatHistory(): String = ui("export_chat_history")
    @JvmStatic fun uiLoadMap(mapName: String): String = bundle().loadMap(mapName)
    @JvmStatic fun uiIntroduction(description: String): String = bundle().introduction(description)
    @JvmStatic fun uiWaveEvent(wave: Int, detail: String): String = bundle().waveEvent(wave, detail)
    @JvmStatic fun uiCopiedChatRecord(): String = ui("copied_this_chat_record")
    @JvmStatic fun uiExportHeader(version: String): String = bundle().exportHeader(version)
    @JvmStatic fun uiExportMap(mapName: String, mode: String): String = bundle().exportMap(mapName, mode)
    @JvmStatic fun uiCurrentWave(wave: Int): String = bundle().currentWave(wave)
    @JvmStatic fun uiExportCount(count: Int): String = bundle().exportCount(count)
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
    @JvmStatic fun uiToggleState(label: String, state: String): String = bundle().toggleState(label, state)
    @JvmStatic fun uiFlashOnChange(): String = ui("flash_on_change")
    @JvmStatic fun uiFlashOnVariableChange(): String = ui("flash_on_variable_change")
    @JvmStatic fun uiAutoRefreshVariables(): String = ui("auto_refresh_variables")
    @JvmStatic fun uiAutoRefreshVariablesHint(): String = ui("automatically_refresh_variables")
    @JvmStatic fun uiPaused(): String = ui("paused")
    @JvmStatic fun uiGameResumed(): String = ui("game_resumed")
    @JvmStatic fun uiPauseLogicGameExecution(): String = ui("pause_logic_game_execution")
    @JvmStatic fun uiRefreshInterval(): String = ui("refresh_interval")
    @JvmStatic fun uiCopiedVariableNameHint(value: String): String = bundle().copiedVariableNameHint(value)
    @JvmStatic fun uiCopiedVariableAttributesHint(value: String): String = bundle().copiedVariableAttributesHint(value)
    @JvmStatic fun uiCopiedPrintBufferHint(value: String): String = bundle().copiedPrintBufferHint(value)
    @JvmStatic fun uiCopiedMemory(value: Double): String = bundle().copiedMemory(value)
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
    @JvmStatic fun uiFailedReadImage(error: Any?): String = bundle().failedReadImage(error)
    @JvmStatic fun uiAutomaticallySaveAsBlueprint(): String = ui("automatically_save_as_blueprint")
    @JvmStatic fun uiZoomZoom(): String = ui("zoom_zoom")
    @JvmStatic fun uiHueMode(): String = ui("hue_mode")
    @JvmStatic fun uiLabelWithEmoji(label: String, emoji: String): String = bundle().labelWithEmoji(label, emoji)
    @JvmStatic fun uiLogicArtWebsite(): String = ui("logic_art_website")
    @JvmStatic fun uiPath(): String = ui("path")
    @JvmStatic fun uiName(): String = ui("name")
    @JvmStatic fun uiOriginalSize(): String = ui("original_size")
    @JvmStatic fun uiScaledSize(): String = ui("scaled_size")
    @JvmStatic fun uiCanvas(): String = ui("canvas")
    @JvmStatic fun uiArtboard(): String = ui("artboard")
    @JvmStatic fun uiPixelArt(): String = ui("pixel_art")
    @JvmStatic fun uiSize(): String = ui("size")
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
