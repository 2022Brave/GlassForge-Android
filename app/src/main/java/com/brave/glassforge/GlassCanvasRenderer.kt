package com.brave.glassforge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * Raster renderer for RemoteViews. Widgets receive a single bitmap so their cut-glass
 * surface, lighting, rims and typography are consistent across Android launchers.
 * Render at a 2x logical scale; ImageView scales the result to the launcher's cell.
 */
object GlassCanvasRenderer {
    enum class Kind { BATTERY, CLOCK, NEON_CLOCK, STACKED_CLOCK, CALENDAR, QUOTE, SEARCH_PILL, WEATHER, COMPASS, MUSIC, SCREEN_TIME }

    fun render(context: Context, kind: Kind, width: Int = 640, height: Int = 400, progress: Int = -1): Bitmap {
        val theme = GlassThemeManager.read(context)
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val sx = width / 640f
        val sy = height / 400f
        canvas.save()
        canvas.scale(sx, sy)
        val accent = theme.accentColor
        val radius = min(88f, min(640f, 400f) * .22f)
        val card = RectF(4f, 4f, 636f, 396f)
        drawGlassSurface(canvas, card, radius, theme.backgroundColor, theme.glassAlpha, theme.transparentMode, accent)
        when (kind) {
            Kind.BATTERY -> drawBattery(context, canvas, accent, progress)
            Kind.CLOCK -> drawClock(canvas, accent, false)
            Kind.NEON_CLOCK -> drawNeonClock(canvas, accent)
            Kind.STACKED_CLOCK -> drawStackedClock(canvas, accent)
            Kind.CALENDAR -> drawCalendar(canvas, accent)
            Kind.QUOTE -> drawQuote(canvas, accent)
            Kind.SEARCH_PILL -> drawSearch(canvas, accent)
            Kind.WEATHER -> drawWeather(canvas, accent)
            Kind.COMPASS -> drawCompass(canvas, accent)
            Kind.MUSIC -> drawMusic(canvas, accent)
            Kind.SCREEN_TIME -> drawScreenTime(canvas, accent)
        }
        canvas.restore()
        return bitmap
    }

    private fun drawGlassSurface(c: Canvas, r: RectF, radius: Float, base: Int, alpha: Float, transparent: Boolean, accent: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(r.left, r.top, r.right, r.bottom,
            intArrayOf(Color.argb((alpha * 245).toInt(), Color.red(base), Color.green(base), Color.blue(base)),
                Color.argb((alpha * 235).toInt(), 18, 18, 23)), null, Shader.TileMode.CLAMP)
        if (transparent) p.alpha = 10
        c.drawRoundRect(r, radius, radius, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.2f
        p.shader = LinearGradient(r.left, r.top, r.right, r.bottom,
            intArrayOf(Color.argb(82, 255, 255, 255), GlassThemeManager.withAlpha(accent, 64), Color.argb(10, 255, 255, 255)),
            floatArrayOf(0f, .52f, 1f), Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(r.left + 1f, r.top + 1f, r.right - 1f, r.bottom - 1f), radius, radius, p)
        p.shader = RadialGradient(r.left + r.width() * .15f, r.top + r.height() * .15f,
            r.width() * .55f, intArrayOf(GlassThemeManager.withAlpha(accent, 38), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        p.style = Paint.Style.FILL
        c.save()
        val clip = Path().apply { addRoundRect(r, radius, radius, Path.Direction.CW) }
        c.clipPath(clip)
        c.drawRect(r, p)
        c.restore()
        // Fine vertical pinstripes, intentionally very subtle on OLED black.
        p.shader = null
        p.color = Color.argb(9, 255, 255, 255)
        var x = r.left + 12f
        while (x < r.right) { c.drawLine(x, r.top + 8f, x, r.bottom - 8f, p); x += 12f }
    }

    private fun text(c: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT, stroke: Boolean = false) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = Typeface.create(if (stroke) "sans-serif-condensed" else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
            strokeWidth = if (stroke) 4.2f else 1f
            strokeJoin = Paint.Join.ROUND
        }
        c.drawText(value, x, baseline, p)
    }

    private fun drawBattery(ctx: Context, c: Canvas, accent: Int, override: Int) {
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (override >= 0) override else if (level >= 0 && scale > 0) level * 100 / scale else 75
        text(c, "Standard Level", 48f, 78f, 26f, Color.argb(204, 255, 255, 255), true)
        val track = RectF(46f, 122f, 594f, 260f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(92, 0, 0, 0) }
        c.drawRoundRect(track, 48f, 48f, p)
        p.color = Color.argb(36, 255, 255, 255)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f
        c.drawRoundRect(track, 48f, 48f, p); p.style = Paint.Style.FILL
        val innerW = 504f
        val gap = 14f
        val segW = (innerW - gap * 4f) / 5f
        val left = 68f
        val active = ((pct.coerceIn(0, 100) + 19) / 20).coerceIn(0, 5)
        for (i in 0 until 5) {
            val x = left + i * (segW + gap)
            val seg = RectF(x, 144f, x + segW, 238f)
            if (i < active) {
                p.color = GlassThemeManager.withAlpha(accent, 75)
                p.maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
                c.drawRoundRect(seg, 18f, 18f, p)
                p.maskFilter = null
                p.color = Color.WHITE
            } else p.color = Color.argb(38, 255, 255, 255)
            c.drawRoundRect(seg, 18f, 18f, p)
        }
        text(c, "$pct%", 320f, 335f, 54f, Color.WHITE, true, Paint.Align.CENTER)
    }

    private fun drawClock(c: Canvas, accent: Int, ignored: Boolean) {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        text(c, time, 320f, 230f, 112f, Color.WHITE, true, Paint.Align.CENTER)
        text(c, SimpleDateFormat("EEE · dd MMM", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault()), 320f, 286f, 20f, accent, true, Paint.Align.CENTER)
    }

    private fun drawNeonClock(c: Canvas, accent: Int) {
        val time = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GlassThemeManager.withAlpha(accent, 90)
            textSize = 130f; textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            style = Paint.Style.STROKE; strokeWidth = 6f
            maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
        }
        c.drawText(time, 320f, 238f, glow)
        glow.maskFilter = null; glow.color = accent; glow.strokeWidth = 4.8f
        c.drawText(time, 320f, 238f, glow)
        text(c, "NEON / DIGITAL", 320f, 300f, 19f, Color.argb(180, 255, 255, 255), true, Paint.Align.CENTER)
    }

    private fun drawStackedClock(c: Canvas, accent: Int) {
        val t = SimpleDateFormat("HH mm", Locale.getDefault()).format(Date()).split(" ")
        text(c, t[0], 320f, 188f, 104f, accent, true, Paint.Align.CENTER)
        text(c, t[1], 320f, 298f, 104f, Color.WHITE, true, Paint.Align.CENTER)
    }

    private fun drawCalendar(c: Canvas, accent: Int) {
        text(c, SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault()), 48f, 110f, 23f, accent, true)
        text(c, SimpleDateFormat("dd", Locale.getDefault()).format(Date()), 48f, 260f, 142f, Color.WHITE, true)
        text(c, SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()), 52f, 312f, 25f, Color.argb(180, 255, 255, 255), false)
    }

    private fun drawQuote(c: Canvas, accent: Int) {
        text(c, "“", 54f, 124f, 70f, accent, true)
        text(c, "Make today count.", 58f, 214f, 44f, Color.WHITE, true)
        text(c, "A SMALL REMINDER TO KEEP MOVING", 60f, 270f, 18f, Color.argb(160, 255, 255, 255), true)
    }

    private fun drawSearch(c: Canvas, accent: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.argb(80, 0, 0, 0)
        c.drawRoundRect(RectF(28f, 112f, 612f, 288f), 88f, 88f, p)
        text(c, "G", 92f, 228f, 74f, Color.WHITE, true, Paint.Align.CENTER)
        p.color = GlassThemeManager.withAlpha(accent, 42)
        c.drawRoundRect(RectF(180f, 150f, 580f, 250f), 50f, 50f, p)
        text(c, "Google Lens", 380f, 213f, 28f, Color.WHITE, true, Paint.Align.CENTER)
    }

    private fun drawWeather(c: Canvas, accent: Int) {
        text(c, "Thu, 31 Jan", 48f, 82f, 30f, Color.WHITE, true)
        text(c, "23°", 48f, 143f, 38f, Color.WHITE, true)
        text(c, "now", 132f, 143f, 28f, Color.argb(102, 255, 255, 255))
        text(c, "in", 48f, 204f, 28f, Color.argb(102, 255, 255, 255))
        text(c, "New York", 83f, 204f, 31f, Color.WHITE, true)
        text(c, "feels like", 48f, 265f, 25f, Color.argb(102, 255, 255, 255))
        text(c, "26°", 180f, 265f, 30f, Color.WHITE, true)
        text(c, "Sunny", 48f, 330f, 35f, accent, true)
        text(c, "PREVIEW · CONNECT A WEATHER SOURCE FOR LIVE DATA", 48f, 366f, 12f, Color.argb(120, 255, 255, 255), true)
    }

    private fun drawCompass(c: Canvas, accent: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.argb(100, 255, 255, 255) }
        c.drawCircle(320f, 205f, 128f, p)
        c.drawCircle(320f, 205f, 98f, p)
        for (i in 0 until 36) {
            val a = Math.toRadians((i * 10 - 90).toDouble())
            val r1 = if (i % 3 == 0) 90f else 103f
            c.drawLine(320f + kotlin.math.cos(a).toFloat() * r1, 205f + kotlin.math.sin(a).toFloat() * r1, 320f + kotlin.math.cos(a).toFloat() * 118f, 205f + kotlin.math.sin(a).toFloat() * 118f, p)
        }
        p.style = Paint.Style.FILL; p.color = accent
        val needle = Path().apply { moveTo(320f, 85f); lineTo(344f, 216f); lineTo(320f, 202f); lineTo(296f, 216f); close() }
        c.drawPath(needle, p)
        text(c, "N", 320f, 58f, 23f, Color.WHITE, true, Paint.Align.CENTER)
        text(c, "COMPASS", 320f, 350f, 18f, accent, true, Paint.Align.CENTER)
    }

    private fun drawMusic(c: Canvas, accent: Int) {
        text(c, "NOW PLAYING", 48f, 90f, 20f, accent, true)
        text(c, "Connect a media session", 48f, 176f, 34f, Color.WHITE, true)
        text(c, "Playback controls remain native RemoteViews actions", 48f, 230f, 17f, Color.argb(155, 255, 255, 255))
        text(c, "◀      ▶      ▶|", 320f, 320f, 38f, Color.WHITE, true, Paint.Align.CENTER)
    }

    private fun drawScreenTime(c: Canvas, accent: Int) {
        text(c, "TODAY'S SCREEN TIME", 48f, 94f, 22f, Color.argb(180, 255, 255, 255), true)
        text(c, "2h 38m", 48f, 220f, 76f, Color.WHITE, true)
        text(c, "SCREEN ON", 52f, 266f, 17f, accent, true)
        text(c, "Screen-off estimate is shown in app/widget details", 52f, 322f, 16f, Color.argb(150, 255, 255, 255))
    }
}
