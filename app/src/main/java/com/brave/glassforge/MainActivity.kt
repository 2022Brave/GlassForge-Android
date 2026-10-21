package com.brave.glassforge

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF080B12)
private val AccentOptions = listOf(Color(0xFF8AB4FF), Color(0xFF9AE6D1), Color(0xFFC5A7FF), Color(0xFFFFC6A5))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("glassforge", MODE_PRIVATE)
        setContent {
            StudioScreen(
                initialOpacity = prefs.getFloat("opacity", .72f),
                initialAccent = prefs.getInt("accent", 0),
                onPinWidget = { provider -> pinWidget(provider) },
                onAppearanceChanged = { opacity, accent ->
                    prefs.edit().putFloat("opacity", opacity).putInt("accent", accent).apply()
                    refreshAllWidgets()
                }
            )
        }
    }

    private fun pinWidget(providerClass: Class<out AppWidgetProvider>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(this, "Widget pinning requires Android 8 or newer. Add it from your launcher's Widgets menu.", Toast.LENGTH_LONG).show()
            return
        }
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) {
            Toast.makeText(this, "Long-press your home screen, then choose Widgets → GlassForge.", Toast.LENGTH_LONG).show()
            return
        }
        val requested = manager.requestPinAppWidget(ComponentName(this, providerClass), Bundle(), null)
        if (!requested) Toast.makeText(this, "Could not open the widget pin prompt. Try your home screen's Widgets menu.", Toast.LENGTH_LONG).show()
    }

    private fun refreshAllWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        listOf(GlassClockWidget::class.java, GlassBatteryWidget::class.java, GlassCalendarWidget::class.java, GlassQuoteWidget::class.java).forEach { klass ->
            val component = ComponentName(this, klass)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) sendBroadcast(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setComponent(component).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
        }
    }
}

@Composable
private fun StudioScreen(initialOpacity: Float, initialAccent: Int, onPinWidget: (Class<out AppWidgetProvider>) -> Unit, onAppearanceChanged: (Float, Int) -> Unit) {
    var opacity by remember { mutableFloatStateOf(initialOpacity.coerceIn(.25f, 1f)) }
    var accent by remember { mutableIntStateOf(initialAccent.coerceIn(0, AccentOptions.lastIndex)) }
    val glow = AccentOptions[accent]
    MaterialTheme(colorScheme = darkColorScheme(primary = glow, background = Ink)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("GLASSFORGE", color = glow, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Text("Widget Studio", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Text("Choose a design. Add it to your home screen.", color = Color.White.copy(alpha = .65f), fontSize = 13.sp)
                PreviewCard(opacity, glow)
                Text("WIDGET LIBRARY", color = glow, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                WidgetCard("Digital Glass Clock", "Live time and date • resizable", Icons.Default.Schedule, glow) { onPinWidget(GlassClockWidget::class.java) }
                WidgetCard("Glass Battery", "Battery percentage and charging state", Icons.Default.BatteryFull, glow) { onPinWidget(GlassBatteryWidget::class.java) }
                WidgetCard("Glass Calendar", "Today's date with a clean glass layout", Icons.Default.CalendarMonth, glow) { onPinWidget(GlassCalendarWidget::class.java) }
                WidgetCard("Daily Quote", "A short motivational line on your home screen", Icons.Default.FormatQuote, glow) { onPinWidget(GlassQuoteWidget::class.java) }
                Text("APPEARANCE", color = glow, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xB8232D40)).border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(22.dp)).padding(16.dp)) {
                    Text("Glass opacity", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Slider(value = opacity, onValueChange = { opacity = it; onAppearanceChanged(opacity, accent) }, valueRange = .25f..1f)
                    Text("${(opacity * 100).toInt()}% • applied to GlassForge widgets", color = glow, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Accent color", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccentOptions.forEachIndexed { index, color ->
                            Surface(onClick = { accent = index; onAppearanceChanged(opacity, accent) }, color = color, shape = RoundedCornerShape(50),
                                modifier = Modifier.size(38.dp).then(if (accent == index) Modifier.border(3.dp, Color.White, RoundedCornerShape(50)) else Modifier)) {}
                        }
                    }
                }
                Text("To add: tap Add widget, then approve the launcher prompt. If unavailable, long-press your home screen → Widgets → GlassForge.", color = Color.White.copy(alpha = .55f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PreviewCard(opacity: Float, accent: Color) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF344C70), Color(0xFF192335), Color(0xFF10151F)))).border(1.dp, Color.White.copy(alpha = .24f), RoundedCornerShape(28.dp)).padding(20.dp)) {
        Text("PREVIEW", color = accent, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("10:30", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
                Text("WEDNESDAY • 21 OCT", color = Color.White.copy(alpha = .72f), fontSize = 10.sp)
            }
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = opacity * .22f)).border(1.dp, Color.White.copy(alpha = .45f), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.BatteryFull, contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Glass that fits your setup.", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Text("Opacity and accent changes are saved and applied to supported widgets.", color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
    }
}

@Composable
private fun WidgetCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xB8232D40)).border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(20.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = .08f)).border(1.dp, Color.White.copy(alpha = .16f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = Color.White.copy(alpha = .65f), fontSize = 11.sp)
        }
        Button(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Text("Add", fontSize = 12.sp) }
    }
}
