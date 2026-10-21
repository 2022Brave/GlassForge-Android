package com.brave.glassforge

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews

class GlassBatteryWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_POWER_CONNECTED || intent.action == Intent.ACTION_POWER_DISCONNECTED) {
            val manager = AppWidgetManager.getInstance(context)
            onUpdate(context, manager, manager.getAppWidgetIds(android.content.ComponentName(context, GlassBatteryWidget::class.java)))
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_glass_canvas)
            views.setImageViewBitmap(R.id.widget_canvas, GlassCanvasRenderer.render(
                context, GlassCanvasRenderer.Kind.BATTERY, progress = pct
            ))
            manager.updateAppWidget(id, views)
        }
    }
}