package com.brave.glassforge

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class GlassMonoClockWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_glass_clock_mono)
            WidgetStyle.apply(context, views, R.id.widget_root, R.id.widget_accent, id)
            manager.updateAppWidget(id, views)
        }
    }
}