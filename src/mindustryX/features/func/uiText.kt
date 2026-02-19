@file:JvmName("FuncX")
@file:JvmMultifileClass

package mindustryX.features.func

import arc.Core
import java.util.Locale

private fun isZh(): Boolean = Core.bundle.locale.language == Locale.CHINESE.language

fun uiJavaWarnLog(javaVersion: String): String =
    if(isZh()) "Java版本 $javaVersion 过低，不受支持。请使用Java 17或更高版本运行MindustryX。"
    else "Java version $javaVersion is too low and unsupported. Please use Java 17+ to run MindustryX."

fun uiJavaWarnDialog(javaVersion: String): String =
    if(isZh()) "Java版本 $javaVersion 过低，不受支持。\n[grey]该警告不存在设置，请更新Java版本。"
    else "Java version $javaVersion is too low and unsupported.\n[grey]This warning cannot be disabled; please update Java."

fun uiWindowTitle(version: String, enabledMods: Int, totalMods: Int, width: Int, height: Int): String =
    if(isZh()) "MindustryX | 版本号 $version | mod启用$enabledMods/$totalMods | ${width}x$height"
    else "MindustryX | Version $version | mods enabled $enabledMods/$totalMods | ${width}x$height"

fun uiArcMessageCenter(): String = if(isZh()) "ARC-中央监控室" else "ARC Message Center"
fun uiMaxChatHistoryHint(): String = if(isZh()) "最大储存聊天记录(过高可能导致卡顿)：" else "Maximum storage of chat history (too high may cause lag):"
fun uiChatHistoryCleanupHint(): String = if(isZh()) "超出限制的聊天记录将在载入地图时清除" else "Chat history exceeding the limit will be cleared when loading the map"
fun uiClear(): String = if(isZh()) "清空" else "Clear"
fun uiExport(): String = if(isZh()) "导出" else "Export"
fun uiExportChatHistory(): String = if(isZh()) "导出聊天记录" else "Export chat history"
fun uiLoadMap(mapName: String): String = if(isZh()) "载入地图：$mapName" else "Load map: $mapName"
fun uiIntroduction(description: String): String = if(isZh()) "简介：$description" else "Introduction: $description"
fun uiWaveEvent(wave: Int, detail: String): String = if(isZh()) "波次：$wave | $detail" else "Waves: $wave | $detail"
fun uiCopiedChatRecord(): String = if(isZh()) "已导出本条聊天记录" else "Copied this chat record"
fun uiExportHeader(version: String): String = if(isZh()) "下面是[MDTX-$version] 导出的游戏内聊天记录" else "[MDTX-$version] Exported in-game chat history"
fun uiExportMap(mapName: String, mode: String): String = if(isZh()) "*** 当前地图名称: $mapName(模式: $mode)\n" else "*** Current map name: $mapName (mode: $mode)\n"
fun uiCurrentWave(wave: Int): String = if(isZh()) "*** 当前波次: $wave" else "*** Current wave: $wave"
fun uiExportCount(count: Int): String = if(isZh()) "成功选取共${count}条记录，如下：\n" else "Successfully selected a total of $count records:\n"
fun uiChatType(): String = if(isZh()) "聊天" else "Chat"
fun uiServerMsgType(): String = if(isZh()) "服务器信息" else "Server Msg"
fun uiMarkCoordinatesType(): String = if(isZh()) "标记~坐标" else "Mark~Coordinates"
fun uiMarkPlayerType(): String = if(isZh()) "标记~玩家" else "Mark~Player"
fun uiCommandType(): String = if(isZh()) "指令" else "Command"
fun uiLogicNoticeType(): String = if(isZh()) "逻辑~通报" else "Logic~Notice"
fun uiLogicAnnouncementType(): String = if(isZh()) "逻辑~公告" else "Logic~Announcement"
fun uiEventMapLoadType(): String = if(isZh()) "事件~载入地图" else "Event~Map Load"
fun uiEventWaveType(): String = if(isZh()) "事件~波次" else "Event~Wave"

fun uiLogicHelperX(): String = if(isZh()) "逻辑辅助器[gold]X[]" else "Logic Helper[gold]X[]"
fun uiHideLogicHelper(): String = if(isZh()) "隐藏逻辑辅助器" else "Hide Logic Helper"
fun uiUpdatedEditedLogic(): String = if(isZh()) "[orange]已更新编辑的逻辑！" else "[orange]Updated edited logic!"
fun uiRefreshEditedLogic(): String = if(isZh()) "更新编辑的逻辑" else "Refresh edited logic"
fun uiOn(): String = if(isZh()) "开启" else "On"
fun uiOff(): String = if(isZh()) "关闭" else "Off"
fun uiToggleState(label: String, state: String): String = "$label: $state"
fun uiFlashOnChange(): String = if(isZh()) "变动闪烁" else "Flash on Change"
fun uiFlashOnVariableChange(): String = if(isZh()) "变量变动闪烁" else "Flash on variable change"
fun uiAutoRefreshVariables(): String = if(isZh()) "变量自动更新" else "Auto-refresh variables"
fun uiAutoRefreshVariablesHint(): String = if(isZh()) "自动刷新变量" else "Automatically refresh variables"
fun uiPaused(): String = if(isZh()) "已暂停" else "Paused"
fun uiGameResumed(): String = if(isZh()) "已继续游戏" else "Game resumed"
fun uiPauseLogicGameExecution(): String = if(isZh()) "暂停逻辑(游戏)运行" else "Pause logic (game) execution"
fun uiRefreshInterval(): String = if(isZh()) "刷新间隔" else "Refresh interval"
fun uiCopiedVariableNameHint(): String = if(isZh()) "复制变量名\n@" else "Copied variable name\n@"
fun uiCopiedVariableAttributesHint(): String = if(isZh()) "复制变量属性\n@" else "Copied variable attributes\n@"
fun uiCopiedPrintBufferHint(): String = if(isZh()) "复制信息版\n@" else "Copied print buffer\n@"
fun uiCopiedMemory(value: Double): String = if(isZh()) "[cyan]复制内存[white]\n $value" else "[cyan]Copied memory[white]\n $value"
fun uiNoPermissionToEditViewOnly(): String = if(isZh()) "[yellow]当前无权编辑，仅供查阅" else "[yellow]No permission to edit; view only."
fun uiResetAllLinks(): String = if(isZh()) "重置所有链接" else "Reset all links"
fun uiExtractCodeFromSchematic(): String = if(isZh()) "从蓝图中选择代码" else "Extract code from schematic"
fun uiSelectCode(): String = if(isZh()) "选择代码" else "Select Code"
fun uiTipAllSchematicsContainingProcessors(): String = if(isZh()) "TIP: 所有包含处理器的蓝图" else "TIP: All schematics containing processors"

fun uiBasic(): String = if(isZh()) "基础对比" else "Basic"
fun uiSquared(): String = if(isZh()) "平方对比" else "Squared"
fun uiArcImageConverter(): String = if(isZh()) "arc-图片转换器" else "ARC Image Converter"
fun uiSelectAndImportPictures(): String = if(isZh()) "选择并导入图片，可将其转成画板、像素画或是逻辑画" else "Select and import pictures, which can be converted into artboards, pixel paintings or logic paintings."
fun uiSelectImagePng(): String = if(isZh()) "选择图片[white](png)" else "Select image[white](png)"
fun uiWarnImageTooLarge(): String = if(isZh()) "[orange]警告：图片可能过大，请尝试压缩图片" else "[orange]Warning: Image may be too large, please try compressing image"
fun uiFailedReadImage(error: Any?): String = if(isZh()) "读取图片失败，请尝试更换图片\n$error" else "Failed to read image, please try another image\n$error"
fun uiAutomaticallySaveAsBlueprint(): String = if(isZh()) "自动保存为蓝图" else "Automatically save as blueprint"
fun uiZoomZoom(): String = if(isZh()) "缩放: \uE815" else "Zoom: \uE815"
fun uiHueMode(): String = if(isZh()) "色调函数:" else "Hue mode:"
fun uiLabelWithEmoji(label: String, emoji: String): String = "$label $emoji"
fun uiLogicArtWebsite(): String = if(isZh()) "逻辑画网站" else "Logic art website"
fun uiPath(): String = if(isZh()) "路径" else "Path"
fun uiName(): String = if(isZh()) "名称" else "Name"
fun uiOriginalSize(): String = if(isZh()) "原始大小" else "Original size"
fun uiScaledSize(): String = if(isZh()) "缩放后大小" else "Scaled size"
fun uiCanvas(): String = if(isZh()) "画板" else "Canvas"
fun uiArtboard(): String = if(isZh()) "画板++" else "Artboard++"
fun uiPixelArt(): String = if(isZh()) "像素画" else "Pixel Art"
fun uiSize(): String = if(isZh()) "大小：" else "Size:"
fun uiSavedBlueprint(name: String): String = if(isZh()) "已保存蓝图：$name" else "Saved blueprint: $name"
