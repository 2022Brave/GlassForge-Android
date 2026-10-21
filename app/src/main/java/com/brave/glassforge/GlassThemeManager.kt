package com.brave.glassforge

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import android.os.Build

/**
 * One source of truth for the Studio and all launcher widgets.
 * Writes are synchronous in-memory immediately (SharedPreferences apply) and widgets are
 * explicitly refreshed so a launcher doesn't have to wait for its normal update interval.
 */
object GlassThemeManager {
    const val PREFS = "glassforge"
    const val DEFAULT_ACCENT = 0xFF8B3DFF.toInt()
    const val DEFAULT_BACKGROUND = 0xFF17171D.toInt()
    const val DEFAULT_ALPHA = 0.72f
    const val MIN_ALPHA = 0.20f
    const val MAX_ALPHA = 1.0f

    val presets: List<Pair<String, Int>> = listOf(
        "Neon Purple" to 0xFF8B3DFF.toInt(),
        "Cyber Lime" to 0xFFCCFF00.toInt(),
        "Electric Blue" to 0xFF1D9BF0.toInt(),
        "Crimson Red" to 0xFFEF4444.toInt(),
        "Solar Gold" to 0xFFF59E0B.toInt()
    )

    data class Theme(
        val accentColor: Int,
        val backgroundColor: Int,
        val glassAlpha: Float,
        val transparentMode: Boolean,
        val hue: Float
    )

    fun read(context: Context): Theme {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val legacy = intArrayOf(0xFF9C5CFF.toInt(), 0xFF38A8FF.toInt(), 0xFF17D7B0.toInt(), 0xFFFFB44A.toInt(), 0xFFFF5E8A.toInt(), 0xFFF0F0F4.toInt())
        val accent = when {
            p.contains("accentColor") -> p.getInt("accentColor", DEFAULT_ACCENT)
            p.contains("accentArgb") -> p.getInt("accentArgb", DEFAULT_ACCENT)
            else -> legacy[p.getInt("accent", 0).coerceIn(0, legacy.lastIndex)]
        }
        val hsv = FloatArray(3)
        colorToHSV(accent, hsv)
        return Theme(
            accentColor = accent,
            backgroundColor = p.getInt("backgroundColor", DEFAULT_BACKGROUND),
            glassAlpha = p.getFloat("glassAlpha", p.getFloat("opacity", DEFAULT_ALPHA)).coerceIn(MIN_ALPHA, MAX_ALPHA),
            transparentMode = p.getBoolean("transparentMode", false),
            hue = p.getFloat("accentHue", hsv[0]).mod(360f)
        )
    }

    fun save(
        context: Context,
        accentColor: Int? = null,
        backgroundColor: Int? = null,
        glassAlpha: Float? = null,
        transparentMode: Boolean? = null,
        hue: Float? = null,
        refresh: Boolean = true
    ): Theme {
        val before = read(context)
        val accent = accentColor ?: hue?.let { HSVToColor(floatArrayOf(it.mod(360f), 0.78f, 1f)) } ?: before.accentColor
        val hsv = FloatArray(3)
        colorToHSV(accent, hsv)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit()
            .putInt("accentColor", accent)
            .putInt("accentArgb", accent)
            .putInt("backgroundColor", backgroundColor ?: before.backgroundColor)
            .putFloat("glassAlpha", (glassAlpha ?: before.glassAlpha).coerceIn(MIN_ALPHA, MAX_ALPHA))
            .putFloat("opacity", (glassAlpha ?: before.glassAlpha).coerceIn(MIN_ALPHA, MAX_ALPHA))
            .putBoolean("transparentMode", transparentMode ?: before.transparentMode)
            .putFloat("accentHue", (hue ?: hsv[0]).mod(360f))
            .putInt("accent", nearestLegacyAccent(accent))
            .apply()
        val after = read(context)
        if (refresh) refreshAll(context)
        return after
    }

    fun reset(context: Context): Theme = save(
        context,
        accentColor = DEFAULT_ACCENT,
        backgroundColor = DEFAULT_BACKGROUND,
        glassAlpha = DEFAULT_ALPHA,
        transparentMode = false,
        hue = 270f
    )

    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val providers = listOf(
            GlassClockWidget::class.java, GlassMonoClockWidget::class.java,
            GlassNeonClockWidget::class.java, GlassBatteryWidget::class.java,
            GlassCalendarWidget::class.java, GlassQuoteWidget::class.java,
            GlassAnalogClockWidget::class.java, GlassScreenTimeWidget::class.java,
            GlassMusicWidget::class.java, GlassMusicMiniWidget::class.java
        )
        providers.forEach { provider ->
            val component = ComponentName(context, provider)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                context.sendBroadcast(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(component)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
            }
        }
    }

    fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun nearestLegacyAccent(color: Int): Int {
        val old = intArrayOf(0xFF9C5CFF.toInt(), 0xFF38A8FF.toInt(), 0xFF17D7B0.toInt(), 0xFFFFB44A.toInt(), 0xFFFF5E8A.toInt(), 0xFFF0F0F4.toInt())
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        return old.indices.minByOrNull { i ->
            val c = old[i]
            val dr = r - Color.red(c); val dg = g - Color.green(c); val db = b - Color.blue(c)
            dr * dr + dg * dg + db * db
        } ?: 0
    }
}
