package com.brave.glassforge

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews

/** Shared Liquid Glass styling with per-widget-instance overrides and app-wide defaults. */
object WidgetStyle {
    private val accents = intArrayOf(
        0xFF9C5CFF.toInt(),
        0xFF38A8FF.toInt(),
        0xFF17D7B0.toInt(),
        0xFFFFB44A.toInt(),
        0xFFFF5E8A.toInt(),
        0xFFF0F0F4.toInt()
    )

    data class Appearance(val opacity: Float, val accentIndex: Int)

    fun appearanceFor(context: Context, appWidgetId: Int? = null): Appearance {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val defaultOpacity = prefs.getFloat("opacity", .72f)
        val defaultAccent = prefs.getInt("accent", 0)
        val instancePrefix = appWidgetId?.takeIf { it >= 0 }?.let { "widget.$it." }
        return Appearance(
            opacity = (instancePrefix?.let { prefs.getFloat(it + "opacity", defaultOpacity) } ?: defaultOpacity).coerceIn(.25f, 1f),
            accentIndex = (instancePrefix?.let { prefs.getInt(it + "accent", defaultAccent) } ?: defaultAccent).coerceIn(0, accents.lastIndex)
        )
    }

    fun saveForWidget(context: Context, appWidgetId: Int, opacity: Float, accentIndex: Int) {
        if (appWidgetId < 0) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("widget.$appWidgetId.opacity", opacity.coerceIn(.25f, 1f))
            .putInt("widget.$appWidgetId.accent", accentIndex.coerceIn(0, accents.lastIndex))
            .apply()
    }

    fun clearForWidget(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove("widget.$appWidgetId.opacity")
            .remove("widget.$appWidgetId.accent")
            .apply()
    }

    fun apply(context: Context, views: RemoteViews, rootId: Int, accentId: Int = R.id.widget_accent, appWidgetId: Int? = null) {
        val appearance = appearanceFor(context, appWidgetId)
        val bg = Color.argb((appearance.opacity * 220).toInt(), 23, 32, 48)
        views.setInt(rootId, "setBackgroundColor", bg)
        try { views.setTextColor(accentId, appearance.accentColor) } catch (_: Exception) { }
    }

    private const val PREFS = "glassforge"
}
