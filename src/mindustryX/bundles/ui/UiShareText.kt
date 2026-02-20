package mindustryX.bundles.ui

object UiShareText {
    @JvmStatic
    fun atPlayer(playerName: String?): String = if (isChineseLocale()) {
        "<AT>戳了${playerName ?: ""}[white]一下，并提醒他留意对话框"
    } else {
        "<AT> poked ${playerName ?: ""}[white] to check their messages."
    }

    @JvmStatic
    fun atNoticeFrom(senderName: String): String = if (isChineseLocale()) {
        "[gold]你被[white]$senderName[gold]戳了一下，请注意查看信息框哦~"
    } else {
        "[gold]You were poked by [white]$senderName[gold]! Check the message dialog."
    }

    @JvmStatic
    fun shareCode(code: String): String = if (isChineseLocale()) {
        "<ARCxMDTX><Schem>[black]一坨兼容[] $code"
    } else {
        "<ARCxMDTX><Schem>[black]compat code[] $code"
    }

    @JvmStatic
    fun shareHeader(version: String): String = if (isChineseLocale()) {
        "这是一条来自 MDTX-$version 的分享记录\n"
    } else {
        "This is a share log from MDTX-$version\n"
    }

    @JvmStatic
    fun waveContains(ground: Int, air: Int): String = if (isChineseLocale()) {
        "包含(地×$ground,空x$air):"
    } else {
        "Contains (ground x$ground, air x$air):"
    }

    @JvmStatic
    fun waveTitle(wave: Int): String = if (isChineseLocale()) "第${wave}波" else "Wave $wave"

    @JvmStatic
    fun waveEta(remainingWaves: Int, eta: String): String =
        if (isChineseLocale()) "(还有${remainingWaves}波, $eta)" else "(in $remainingWaves waves, $eta)"
}
