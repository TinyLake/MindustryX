package mindustryX.bundles.ui

object UiStatText {
    @JvmStatic
    fun shieldCapacity(max: String, radius: String, recovery: String, cooldown: String): String = if (isChineseLocale()) {
        "[lightgray]${max}盾容[accent]~[]${radius}格[accent]~[]${recovery}恢复[accent]~[]${cooldown}s冷却"
    } else {
        "[lightgray]${max} shield capacity[accent]~[]${radius} grid[accent]~[]${recovery} recovery[accent]~[]${cooldown}s cooldown"
    }

    @JvmStatic
    fun liquidExplode(total: String, liquidName: String, liquidEmoji: String, radius: String): String = if (isChineseLocale()) {
        "[lightgray]总计${total}${liquidName}${liquidEmoji}[accent]~[]${radius}格半径"
    } else {
        "[lightgray]Total ${total}${liquidName}${liquidEmoji}[accent]~[]${radius} tile radius"
    }

    @JvmStatic
    fun liquidRegen(absorb: String, liquidName: String, liquidEmoji: String, heal: String, maxHeal: String): String =
        if (isChineseLocale()) {
            "[lightgray]每格吸收${absorb}/s${liquidName}${liquidEmoji}[accent]~[]${heal}/s回血[accent]~[]最大${maxHeal}/s"
        } else {
            "[lightgray]Each cell absorbs ${absorb}/s${liquidName}${liquidEmoji}[accent]~[]heals ${heal}/s[accent]~[]up to ${maxHeal}/s"
        }

    @JvmStatic
    fun lightning(probability: String, damage: String, length: String, speed: String): String = if (isChineseLocale()) {
        "[lightgray]闪电${probability}概率[accent]~[]${damage}伤害[accent]~[]${length}长度 ${speed}x速度"
    } else {
        "[lightgray]Lightning ${probability} probability[accent]~[]${damage} damage[accent]~[]${length} length ${speed}x speed"
    }

    @JvmStatic
    fun durationTiles(seconds: String, tiles: String): String = if (isChineseLocale()) {
        "[lightgray]${seconds}s[accent]~[]${tiles}格"
    } else {
        "[lightgray]${seconds}s[accent]~[]${tiles} tiles"
    }
}
