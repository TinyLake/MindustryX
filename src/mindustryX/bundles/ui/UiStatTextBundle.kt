package mindustryX.bundles.ui

interface UiStatTextBundle {
    fun shieldCapacity(max: String, radius: String, recovery: String, cooldown: String): String =
        "[lightgray]${max}盾容[accent]~[]${radius}格[accent]~[]${recovery}恢复[accent]~[]${cooldown}s冷却"

    fun liquidExplode(total: String, liquidName: String, liquidEmoji: String, radius: String): String =
        "[lightgray]总计${total}${liquidName}${liquidEmoji}[accent]~[]${radius}格半径"

    fun liquidRegen(absorb: String, liquidName: String, liquidEmoji: String, heal: String, maxHeal: String): String =
        "[lightgray]每格吸收${absorb}/s${liquidName}${liquidEmoji}[accent]~[]${heal}/s回血[accent]~[]最大${maxHeal}/s"

    fun lightning(probability: String, damage: String, length: String, speed: String): String =
        "[lightgray]闪电${probability}概率[accent]~[]${damage}伤害[accent]~[]${length}长度 ${speed}x速度"

    fun durationTiles(seconds: String, tiles: String): String = "[lightgray]${seconds}s[accent]~[]${tiles}格"
}
