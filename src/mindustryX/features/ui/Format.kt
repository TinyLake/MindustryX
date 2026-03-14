package mindustryX.features.ui

import arc.math.Mathf
import arc.math.geom.Position
import arc.util.Strings
import mindustry.core.World
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate")
data class Format @JvmOverloads constructor(var decimal: Int = 2, var fixDecimals: Boolean = false) {
    /**以固定的有效位数输出*/
    fun fixedPrecision(v: Float): String {
        val exponent = Mathf.floor(Mathf.log(10f, abs(v))).coerceAtLeast(0)
        if (exponent >= decimal) return v.toInt().toString()
        return Strings.fixed(v, decimal - exponent)
    }

    /** 科学计数法输出 */
    fun scienceFormat(number: Float): String {
        val exponent = Mathf.floor(Mathf.log(10f, abs(number)))
        val mantissa = number / Mathf.pow(10f, exponent.toFloat())
        return "${Strings.fixed(mantissa, decimal)}[gray]E$exponent[]"
    }

    private fun format0(number: Float): String {
        if (fixDecimals) return Strings.fixed(number, decimal)
        return fixedPrecision(number)
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
            abs <= java.lang.Float.MIN_NORMAL -> format0(0f)
            abs < Mathf.pow(10f, -decimal.toFloat()) -> scienceFormat(number)
            abs < 1e3f || abs < Mathf.pow(10f, 1f + decimal) -> format0(number) //直接渲染
            abs < 1e6f -> "${format0(number / 1e3f)}[gray]K[]"
            abs < 1e9f -> "${format0(number / 1e6f)}[gray]M[]"
            abs < 1e12f -> "${format0(number / 1e9f)}[gray]B[]"
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

    fun format(pos: Position): String {
        return "(${format0(pos.x)},${format0(pos.y)})"
    }

    fun formatTile(pos: Position): String {
        return "(${World.toTile(pos.x)},${World.toTile(pos.y)})"
    }

    fun fileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024L * 1024L) return format(bytes / 1024f) + " KB"
        if (bytes < 1024L * 1024L * 1024L) return format(bytes / 1024f / 1024f) + " MB"
        return format(bytes / 1024f / 1024f / 1024f) + " GB"
    }

    fun duration(seconds: Float, unit: Boolean = true) = buildString {
        append(if (seconds > 0) "[orange]" else "[acid]")
        val s = abs(seconds)
        if (s >= 60) {
            append((seconds / 60).toInt())
            append(" : ")
            append((s % 60).toInt())
        } else {
            append(format0(seconds))
        }
        append("[]")
        if (unit) append(if (s >= 60) "min" else "s")
    }

    @JvmOverloads
    fun percent(cur: Float, max: Float, percent: Float = cur / max, showPercent: Boolean = percent < 0.95f): String {
        return buildString {
            append(format(cur))
            if (percent < 0.99f) {
                append('/')
                append(format(max))
            }
            if (showPercent) {
                append(" [lightgray]| ")
                append((percent * 100).toInt())
                append('%')
            }
        }
    }

    companion object {
        val default = Format()
    }
}

@Suppress("ConstantLocale")
object FormatDefault {
    @JvmOverloads
    @JvmStatic
    fun percent(cur: Float, max: Float, percent: Float = cur / max, showPercent: Boolean = percent <= 0.99f): String = Format.default.percent(cur, max, percent, showPercent)

    @JvmStatic
    fun format(number: Float): String = Format.default.format(number)

    @JvmStatic
    fun format(number: Long): String = Format.default.format(number)

    @JvmStatic
    fun format(pos: Position): String = Format.default.format(pos)

    @JvmStatic
    fun formatTile(pos: Position): String = Format.default.formatTile(pos)

    @JvmOverloads
    @JvmStatic
    fun duration(seconds: Float, unit: Boolean = true): String = Format.default.duration(seconds, unit)

    @JvmStatic
    fun fileSize(bytes: Long): String = Format.default.fileSize(bytes)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val datetimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    @JvmStatic
    fun date(date: Instant): String {
        return dateFormat.format(date)
    }

    @JvmStatic
    fun date(date: Date): String {
        return dateFormat.format(date)
    }

    @JvmStatic
    fun datetime(date: Instant): String {
        return datetimeFormat.format(date)
    }

    @JvmStatic
    fun datetime(date: Date): String {
        return datetimeFormat.format(date)
    }
}