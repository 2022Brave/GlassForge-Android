package com.brave.glassforge

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.widget.RemoteViews

class GlassMusicMiniWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAll(context)

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, GlassMusicMiniWidget::class.java))
            val controller = GlassMusicWidget.activeController(context)
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_glass_music_mini)
                views.setTextColor(R.id.widget_accent, GlassThemeManager.read(context).accentColor)
            views.setImageViewBitmap(R.id.widget_canvas, GlassCanvasRenderer.render(context, GlassCanvasRenderer.Kind.SURFACE))
                views.setTextViewText(R.id.music_mini_title, controller?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Nothing playing")
                val playing = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
                views.setTextViewText(R.id.music_mini_play, if (playing) "Ⅱ" else "▶")
                views.setOnClickPendingIntent(R.id.music_mini_previous, action(context, GlassMusicWidget.ACTION_PREVIOUS, 821))
                views.setOnClickPendingIntent(R.id.music_mini_play, action(context, GlassMusicWidget.ACTION_PLAY_PAUSE, 822))
                views.setOnClickPendingIntent(R.id.music_mini_next, action(context, GlassMusicWidget.ACTION_NEXT, 823))
                manager.updateAppWidget(id, views)
            }
        }

        private fun action(context: Context, name: String, requestCode: Int) =
            PendingIntent.getBroadcast(context, requestCode, Intent(context, GlassMusicWidget::class.java).setAction(name), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}