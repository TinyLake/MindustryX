package mindustryX.features.ui

import arc.math.Mathf
import arc.util.Strings
import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate")
data class Format(private var decimal: Int = 2) {
    /**以固定的有效位数输出*/
    fun fixedPrecision(v: Float): String {
        val exponent = Mathf.floor(Mathf.log(10f, abs(v)))
        if (exponent >= decimal) return v.toInt().toString()
        return Strings.fixed(v, decimal - exponent)
    }

    /** 科学计数法输出 */
    fun scienceFormat(number: Float): String {
        val exponent = Mathf.floor(Mathf.log(10f, abs(number)))
        val mantissa = number / Mathf.pow(10f, exponent.toFloat())
        return "${Strings.fixed(mantissa, decimal)}[gray]E$exponent[]"
    }

    /** 格式化浮点数，设计特性如下
     * 1. 支持特殊数值显示
     * 2. 支持超大超小数显示科学计数法, 支持K,M,B显示
     * 3. 避免出现1000K,0.99M这种数值
     */
    fun format(number: Float): String {
        if (java.lang.Float.isNaN(number)) return "NaN"
        if (number == Float.POSITIVE_INFINITY) return "Inf"
        if (number == Float.NEGATIVE_INFINITY) return "-Inf"

        val abs = abs(number)
        return when {
            abs <= java.lang.Float.MIN_NORMAL -> fixedPrecision(0f)
            abs < Mathf.pow(10f, -decimal.toFloat()) -> scienceFormat(number)
            abs < 1e3f || abs < Mathf.pow(10f, 1f + decimal) -> fixedPrecision(number) //直接渲染
            abs < 1e6f -> "${fixedPrecision(number / 1e3f)}[gray]K[]"
            abs < 1e9f -> "${fixedPrecision(number / 1e6f)}[gray]M[]"
            abs < 1e12f -> "${fixedPrecision(number / 1e9f)}[gray]B[]"
            else -> scienceFormat(number)
        }
    }

    /** @see format */
    fun format(number: Long): String {
        if (number == Long.MAX_VALUE) return "∞"
        if (number == Long.MIN_VALUE) return "-∞"
        if (-1000 < number && number < 1000) return number.toString()
        return format(number.toFloat())
    }

    @JvmOverloads
    fun percent(cur: Float, max: Float, percent: Float = 100 * cur / max, showPercent: Boolean = percent < 0.95f): String {
        return buildString {
            if (Mathf.zero(percent, 0.1f)) append('\uE815') else append(format(cur))
            if (percent < 0.99f) {
                append('/')
                append(format(max))
            }
            if (showPercent) {
                append(" [lightgray]| ")
                append(percent.toInt())
                append('%')
            }
        }
    }

    companion object {
        val default = Format()
    }
}

object FormatDefault {
    @JvmOverloads
    @JvmStatic
    fun percent(cur: Float, max: Float, percent: Float = 100 * cur / max, showPercent: Boolean = percent <= 0.99f): String = Format.default.percent(cur, max, percent, showPercent)

    @JvmStatic
    fun format(number: Float): String = Format.default.format(number)

    @JvmStatic
    fun format(number: Long): String = Format.default.format(number)
}