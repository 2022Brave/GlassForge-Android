package com.brave.glassforge

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import android.provider.Settings

class GlassMusicWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> activeController(context)?.let { controller ->
                if (controller.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING) controller.transportControls.pause()
                else controller.transportControls.play()
            }
            ACTION_NEXT -> activeController(context)?.transportControls?.skipToNext()
            ACTION_PREVIOUS -> activeController(context)?.transportControls?.skipToPrevious()
        }
        updateAll(context)
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.brave.glassforge.MUSIC_PLAY_PAUSE"
        const val ACTION_NEXT = "com.brave.glassforge.MUSIC_NEXT"
        const val ACTION_PREVIOUS = "com.brave.glassforge.MUSIC_PREVIOUS"

        fun activeController(context: Context): MediaController? = try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            manager.getActiveSessions(ComponentName(context, GlassMediaNotificationListener::class.java)).firstOrNull()
        } catch (_: SecurityException) { null }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, GlassMusicWidget::class.java))
            val controller = activeController(context)
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_glass_music)
                WidgetStyle.apply(context, views, R.id.widget_root, R.id.widget_accent)
                val metadata = controller?.metadata
                views.setTextViewText(R.id.music_title, metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "No active track")
                views.setTextViewText(R.id.music_artist, metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Open your music player")
                val playing = controller?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
                views.setTextViewText(R.id.music_play_pause, if (playing) "Ⅱ" else "▶")
                views.setOnClickPendingIntent(R.id.music_previous, actionPending(context, ACTION_PREVIOUS, 811))
                views.setOnClickPendingIntent(R.id.music_play_pause, actionPending(context, ACTION_PLAY_PAUSE, 812))
                views.setOnClickPendingIntent(R.id.music_next, actionPending(context, ACTION_NEXT, 813))
                val settings = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                views.setOnClickPendingIntent(R.id.music_title, PendingIntent.getActivity(context, 814, settings, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                manager.updateAppWidget(id, views)
            }
            GlassMusicMiniWidget.updateAll(context)
        }

        private fun actionPending(context: Context, action: String, request: Int) =
            PendingIntent.getBroadcast(context, request, Intent(context, GlassMusicWidget::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

class GlassMediaNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) { GlassMusicWidget.updateAll(this) }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { GlassMusicWidget.updateAll(this) }
}