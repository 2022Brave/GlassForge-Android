package com.brave.glassforge

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class GlassClockWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val accent = GlassThemeManager.read(context).accentColor
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_glass_clock_live)
            views.setImageViewBitmap(R.id.widget_canvas, GlassCanvasRenderer.render(context, GlassCanvasRenderer.Kind.SURFACE))
            views.setTextColor(R.id.clock_live_date, accent)
            manager.updateAppWidget(id, views)
        }
    }
}