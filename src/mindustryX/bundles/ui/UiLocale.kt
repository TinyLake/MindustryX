package mindustryX.bundles.ui

import arc.Core
import java.util.Locale

internal fun isChineseLocale(): Boolean {
    val language = Core.bundle.locale?.language ?: Locale.getDefault().language
    return language == Locale.CHINESE.language
}
