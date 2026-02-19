package mindustryX.bundles

import arc.Core
import arc.struct.ObjectMap
import arc.util.I18NBundle
import arc.util.Log
import arc.util.Reflect
import arc.util.TextFormatter
import java.util.Locale

/**
 * MDTX's custom bundle overlay.
 *
 * Behavior matches the two-language sample:
 * - Chinese locale -> use Chinese bundle
 * - Otherwise -> use English bundle
 *
 * The loaded bundle is attached as Core.bundle and chained to the original bundle as parent,
 * so missing keys fall back to the game's original translations.
 */
object MdtxBundleLoader {
    @JvmStatic
    fun register() {
        try {
            val origin = Core.bundle ?: return
            val locale = origin.locale ?: Locale.getDefault()

            // Sample-like fallback chain:
            // - zh* locale:      ZH -> origin
            // - otherwise:       EN -> ZH -> origin
            val zh = loadBundle(MdtxBundleData.zh, locale)
            if (Lang.isChinese(locale)) {
                attachParentChain(zh, origin)
                Core.bundle = zh
                Log.info("MDTX: bundle has been loaded: zh (${locale.language}).")
            } else {
                val en = loadBundle(MdtxBundleData.en, locale)
                attachParentChain(en, zh)
                attachParentChain(zh, origin)
                Core.bundle = en
                Log.info("MDTX: bundle has been loaded: en->zh (${locale.language}).")
            }
        } catch (t: Throwable) {
            // Keep the game usable even if bundle loading fails.
            Log.err(t)
        }
    }

    private fun loadBundle(data: ObjectMap<String, String>, locale: Locale): I18NBundle {
        val bundle = I18NBundle.createEmptyBundle()
        bundle.setProperties(data.copy())

        // I18NBundle may fall back to ROOT locale; keep locale/formatter consistent with the game's locale.
        Reflect.set(bundle, "locale", locale)
        Reflect.set(bundle, "formatter", TextFormatter(locale, !I18NBundle.getSimpleFormatter()))
        return bundle
    }

    private fun attachParentChain(bundle: I18NBundle, parent: I18NBundle) {
        var root = bundle
        while (root.parent != null) {
            root = root.parent
        }
        Reflect.set(root, "parent", parent)
    }
}
