package com.brave.glassforge

import android.app.AppOpsManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.os.PowerManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.provider.Settings
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GlassScreenTimeWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            onUpdate(context, manager, manager.getAppWidgetIds(android.content.ComponentName(context, GlassScreenTimeWidget::class.java)))
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val granted = hasUsageAccess(context)
        val (onMs, offMs) = if (granted) calculateScreenTime(context) else 0L to 0L
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_glass_screen_time)
            WidgetStyle.apply(context, views, R.id.widget_root, R.id.widget_accent)
            views.setTextViewText(R.id.screen_on_value, if (granted) format(onMs) else "Tap to enable")
            views.setTextViewText(R.id.screen_off_value, if (granted) format(offMs) else "Usage access")
            val settingsIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val settingsPending = PendingIntent.getActivity(context, 801, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, settingsPending)
            val refresh = Intent(context, GlassScreenTimeWidget::class.java).setAction(ACTION_REFRESH)
            val refreshPending = PendingIntent.getBroadcast(context, 830, refresh, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.screen_on_value, refreshPending)
            views.setOnClickPendingIntent(R.id.screen_off_value, refreshPending)
            manager.updateAppWidget(id, views)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.brave.glassforge.SCREEN_TIME_REFRESH"

        fun hasUsageAccess(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
        }

        private fun calculateScreenTime(context: Context): Pair<Long, Long> {
            val now = System.currentTimeMillis()
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val events = usage.queryEvents(start, now)
            val event = UsageEvents.Event()
            var interactive = false
            var lastInteractiveAt = start
            var screenOn = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        if (!interactive) { interactive = true; lastInteractiveAt = event.timeStamp }
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        if (interactive) { screenOn += (event.timeStamp - lastInteractiveAt).coerceAtLeast(0); interactive = false }
                    }
                }
            }
            if (interactive || (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) {
                screenOn += (now - lastInteractiveAt).coerceAtLeast(0)
            }
            val elapsed = (now - start).coerceAtLeast(0)
            return screenOn.coerceAtMost(elapsed) to (elapsed - screenOn).coerceAtLeast(0)
        }

        private fun format(ms: Long): String {
            val minutes = ms / 60_000
            val hours = minutes / 60
            return if (hours > 0) String.format(Locale.getDefault(), "%dh %02dm", hours, minutes % 60)
            else String.format(Locale.getDefault(), "%dm", minutes)
        }
    }
}