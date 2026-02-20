package mindustryX.bundles.ui

interface UiShareTextBundle {
    fun atPlayer(playerName: String?): String = "<AT>戳了${playerName ?: ""}[white]一下，并提醒他留意对话框"
    fun atNoticeFrom(senderName: String): String = "[gold]你被[white]$senderName[gold]戳了一下，请注意查看信息框哦~"
    fun shareCode(code: String): String = "<ARCxMDTX><Schem>[black]一坨兼容[] $code"
    fun shareHeader(version: String): String = "这是一条来自 MDTX-$version 的分享记录\n"
    fun waveContains(ground: Int, air: Int): String = "包含(地×$ground,空x$air):"
    fun waveTitle(wave: Int): String = "第${wave}波"
    fun waveEta(remainingWaves: Int, eta: String): String = "(还有${remainingWaves}波, $eta)"

    fun mdtxShareItem(name: Any?, stock: Any?, production: Any?): String =
        "${name}：库存 ${stock}，产量 ${production}/秒"

    fun mdtxShareUnit(name: Any?, count: Any?, limit: Any?): String =
        "${name}：数量 ${count}，上限 ${limit}"
}
