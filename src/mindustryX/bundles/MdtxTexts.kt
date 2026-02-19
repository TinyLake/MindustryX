package mindustryX.bundles

import arc.Core

/** Accessors for MDTX bundle keys, centralized in one place. */
object MdtxTexts {
    @JvmStatic
    fun text(key: String, def: String = key): String = Core.bundle.get(key, def)

    @JvmStatic
    fun textOrNull(key: String): String? = Core.bundle.getOrNull(key)

    @JvmStatic
    fun format(key: String, vararg args: Any?): String = Core.bundle.format(key, *args)
}
