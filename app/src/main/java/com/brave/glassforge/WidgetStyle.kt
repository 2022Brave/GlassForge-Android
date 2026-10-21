package com.brave.glassforge

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews

/** Compatibility bridge for legacy text-based RemoteViews and per-instance preferences. */
object WidgetStyle {
    private val legacyAccents = intArrayOf(
        0xFF9C5CFF.toInt(), 0xFF38A8FF.toInt(), 0xFF17D7B0.toInt(),
        0xFFFFB44A.toInt(), 0xFFFF5E8A.toInt(), 0xFFF0F0F4.toInt()
    )

    data class Appearance(val opacity: Float, val accentIndex: Int)

    fun appearanceFor(context: Context, appWidgetId: Int? = null): Appearance {
        val prefs = context.getSharedPreferences(GlassThemeManager.PREFS, Context.MODE_PRIVATE)
        val instancePrefix = appWidgetId?.takeIf { it >= 0 }?.let { "widget.$it." }
        return Appearance(
            opacity = (instancePrefix?.let { prefs.getFloat(it + "opacity", prefs.getFloat("opacity", GlassThemeManager.DEFAULT_ALPHA)) }
                ?: prefs.getFloat("opacity", GlassThemeManager.DEFAULT_ALPHA)).coerceIn(GlassThemeManager.MIN_ALPHA, 1f),
            accentIndex = (instancePrefix?.let { prefs.getInt(it + "accent", prefs.getInt("accent", 0)) }
                ?: prefs.getInt("accent", 0)).coerceIn(0, legacyAccents.lastIndex)
        )
    }

    fun saveForWidget(context: Context, appWidgetId: Int, opacity: Float, accentIndex: Int) {
        if (appWidgetId < 0) return
        context.getSharedPreferences(GlassThemeManager.PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("widget.$appWidgetId.opacity", opacity.coerceIn(GlassThemeManager.MIN_ALPHA, 1f))
            .putInt("widget.$appWidgetId.accent", accentIndex.coerceIn(0, legacyAccents.lastIndex))
            .apply()
        GlassThemeManager.refreshAll(context)
    }

    fun clearForWidget(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(GlassThemeManager.PREFS, Context.MODE_PRIVATE).edit()
            .remove("widget.$appWidgetId.opacity")
            .remove("widget.$appWidgetId.accent")
            .apply()
        GlassThemeManager.refreshAll(context)
    }

    fun apply(context: Context, views: RemoteViews, rootId: Int, accentId: Int = R.id.widget_accent, appWidgetId: Int? = null) {
        val appearance = appearanceFor(context, appWidgetId)
        val theme = GlassThemeManager.read(context)
        val opacity = appearance.opacity
        val bg = if (theme.transparentMode) Color.TRANSPARENT else Color.argb((opacity * 235).toInt(), Color.red(theme.backgroundColor), Color.green(theme.backgroundColor), Color.blue(theme.backgroundColor))
        views.setInt(rootId, "setBackgroundColor", bg)
        try {
            val instanceAccent = if (context.getSharedPreferences(GlassThemeManager.PREFS, Context.MODE_PRIVATE).contains("accentColor")) theme.accentColor else legacyAccents[appearance.accentIndex]
            views.setTextColor(accentId, instanceAccent)
        } catch (_: Exception) { }
    }
}
