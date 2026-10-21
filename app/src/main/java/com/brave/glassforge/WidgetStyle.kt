package com.brave.glassforge

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews

object WidgetStyle {
    private val accents = intArrayOf(0xFF8AB4FF.toInt(), 0xFF9AE6D1.toInt(), 0xFFC5A7FF.toInt(), 0xFFFFC6A5.toInt())

    fun apply(context: Context, views: RemoteViews, rootId: Int, accentId: Int = R.id.widget_accent) {
        val prefs = context.getSharedPreferences("glassforge", Context.MODE_PRIVATE)
        val opacity = prefs.getFloat("opacity", .72f).coerceIn(.25f, 1f)
        val accentIndex = prefs.getInt("accent", 0).coerceIn(0, accents.lastIndex)
        val bg = Color.argb((opacity * 220).toInt(), 23, 32, 48)
        views.setInt(rootId, "setBackgroundColor", bg)
        try { views.setTextColor(accentId, accents[accentIndex]) } catch (_: Exception) { }
    }
}