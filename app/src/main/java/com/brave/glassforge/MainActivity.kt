package com.brave.glassforge

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
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
private val Blue = Color(0xFF8AB4FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioScreen { startActivity(Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)) } }
    }
}

@Composable
private fun StudioScreen(openPicker: () -> Unit) {
    var opacity by remember { mutableFloatStateOf(.72f) }
    var accent by remember { mutableIntStateOf(0) }
    val colors = listOf(Blue, Color(0xFF9AE6D1), Color(0xFFC5A7FF), Color(0xFFFFC6A5))
    val glow = colors[accent]
    MaterialTheme(colorScheme = darkColorScheme(primary = glow, background = Ink)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("GLASSFORGE", color = glow, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Text("Your widgets. Reframed.", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF344C70), Color(0xFF192335), Color(0xFF10151F)))).border(1.dp, Color.White.copy(alpha = .24f), RoundedCornerShape(28.dp)).padding(20.dp)) {
                    Text("LIVE PREVIEW", color = glow, fontSize = 11.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("9:41", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
                            Text("WEDNESDAY • 21 OCT", color = Color.White.copy(alpha = .72f), fontSize = 10.sp)
                        }
                        Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = opacity * .22f)).border(1.dp, Color.White.copy(alpha = .4f), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BatteryFull, contentDescription = "Battery widget preview", tint = glow, modifier = Modifier.size(34.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("A calmer kind of glance.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("Translucent surfaces • soft highlights • adaptive accents", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
                }
                Text("Widget studio", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                WidgetRow("Glass Clock", "Time and date, minimal and legible.", true, glow)
                WidgetRow("Glass Battery", "Battery level and charging state.", false, glow)
                Button(onClick = openPicker, modifier = Modifier.fillMaxWidth()) { Text("Add a home-screen widget") }
                Text("Glass appearance", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Preview opacity", color = Color.White.copy(alpha = .8f))
                Slider(value = opacity, onValueChange = { opacity = it }, valueRange = .25f..1f)
                Text("Accent color", color = Color.White.copy(alpha = .8f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    colors.forEachIndexed { i, c ->
                        Surface(onClick = { accent = i }, color = c, shape = RoundedCornerShape(50), modifier = Modifier.size(34.dp).then(if (accent == i) Modifier.border(3.dp, Color.White, RoundedCornerShape(50)) else Modifier)) {}
                    }
                }
                Spacer(Modifier.weight(1f, fill = false))
                Text("Android widgets • glass-inspired, battery-conscious", color = Color.White.copy(alpha = .45f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun WidgetRow(title: String, detail: String, clock: Boolean, accent: Color) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xB8232D40)).border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(20.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (clock) Icons.Default.Schedule else Icons.Default.BatteryFull, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(detail, color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
        }
    }
}