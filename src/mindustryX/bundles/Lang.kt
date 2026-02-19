package mindustryX.bundles

import arc.Core
import java.util.Locale

/** Two-language helper: treat zh* as Chinese, everything else as non-Chinese. */
object Lang {
    @JvmStatic
    fun isChinese(locale: Locale?): Boolean {
        val lang = locale?.language ?: return false
        return lang.equals(Locale.CHINESE.language, ignoreCase = true)
    }

    @JvmStatic
    fun isChinese(): Boolean = isChinese(Core.bundle?.locale)
}
