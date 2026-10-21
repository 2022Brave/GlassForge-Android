package com.brave.glassforge

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class GlassClockWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_glass_canvas)
            views.setImageViewBitmap(R.id.widget_canvas, GlassCanvasRenderer.render(context, GlassCanvasRenderer.Kind.CLOCK))
            manager.updateAppWidget(id, views)
        }
    }
}