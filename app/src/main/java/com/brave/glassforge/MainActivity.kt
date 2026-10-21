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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF090A0F)
private val Panel = Color(0xFF171920)
private val PanelLight = Color(0xFF242731)
private val Muted = Color(0xFF9A9DA8)
private val Accents = listOf(Color(0xFF9C5CFF), Color(0xFF38A8FF), Color(0xFF17D7B0), Color(0xFFFFB44A), Color(0xFFFF5E8A), Color(0xFFF0F0F4))

private data class WidgetDesign(val name: String, val category: String, val description: String, val icon: ImageVector, val provider: Int, val style: Int, val accent: Color)
private val designs = listOf(
    WidgetDesign("Orbit Digital", "Clock", "Live digital time · date", Icons.Default.Schedule, 0, 0, Color(0xFF9C5CFF)),
    WidgetDesign("Mono Clock", "Clock", "Minimal digital face", Icons.Default.AccessTime, 8, 1, Color(0xFFF0F0F4)),
    WidgetDesign("Violet Time", "Clock", "Oversized digital clock", Icons.Default.Timer, 9, 2, Color(0xFF9C5CFF)),
    WidgetDesign("Analog Atelier", "Clock", "Live analog clock face", Icons.Default.AccessTime, 4, 0, Color(0xFF9C5CFF)),
    WidgetDesign("Glow Battery", "Device", "Battery · charging state", Icons.Default.BatteryFull, 1, 0, Color(0xFF38A8FF)),
    WidgetDesign("Pulse Battery", "Device", "Compact battery glance", Icons.Default.BatteryChargingFull, 1, 1, Color(0xFF17D7B0)),
    WidgetDesign("Day Marker", "Calendar", "Day · date · month", Icons.Default.CalendarMonth, 2, 0, Color(0xFFFFB44A)),
    WidgetDesign("Daily Thought", "Lifestyle", "A little inspiration", Icons.Default.FormatQuote, 3, 0, Color(0xFFFF5E8A)),
    WidgetDesign("Soft Glass Date", "Calendar", "Quiet date panel", Icons.Default.Today, 2, 1, Color(0xFF38A8FF)),
    WidgetDesign("Screen Balance", "Productivity", "Today's screen-on and screen-off time", Icons.Default.Timelapse, 5, 0, Color(0xFF17D7B0)),
    WidgetDesign("Now Playing", "Music", "Track, artist and playback controls", Icons.Default.MusicNote, 6, 0, Color(0xFF9C5CFF)),
    WidgetDesign("Music Glass Bar", "Music", "Compact transport controls", Icons.Default.GraphicEq, 7, 1, Color(0xFF38A8FF))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("glassforge", MODE_PRIVATE)
        setContent {
            GlassForgeApp(
                initialOpacity = prefs.getFloat("opacity", .72f),
                initialAccent = prefs.getInt("accent", 0),
                onAppearanceChanged = { opacity, accent ->
                    prefs.edit().putFloat("opacity", opacity).putInt("accent", accent).apply()
                    refreshAllWidgets()
                },
                onPinWidget = { provider, opacity, accent -> pinWidget(provider, opacity, accent) }
            )
        }
    }

    private fun pinWidget(providerClass: Class<out AppWidgetProvider>, opacity: Float, accent: Int) {
        // Save the appearance before Android creates the pinned widget so its first render uses the chosen style.
        getSharedPreferences("glassforge", MODE_PRIVATE).edit().putFloat("opacity", opacity).putInt("accent", accent).apply()\n        GlassThemeManager.save(this, accentColor = Accents[accent.coerceIn(0, Accents.lastIndex)].toArgb(), glassAlpha = opacity, refresh = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && AppWidgetManager.getInstance(this).isRequestPinAppWidgetSupported) {
            val ok = AppWidgetManager.getInstance(this).requestPinAppWidget(ComponentName(this, providerClass), Bundle(), null)
            if (!ok) Toast.makeText(this, "Open your home screen → Widgets → GlassForge to add it.", Toast.LENGTH_LONG).show()
        } else Toast.makeText(this, "Long-press the home screen → Widgets → GlassForge.", Toast.LENGTH_LONG).show()
    }

    private fun refreshAllWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        listOf(GlassClockWidget::class.java, GlassMonoClockWidget::class.java, GlassNeonClockWidget::class.java, GlassBatteryWidget::class.java, GlassCalendarWidget::class.java, GlassQuoteWidget::class.java, GlassAnalogClockWidget::class.java, GlassScreenTimeWidget::class.java, GlassMusicWidget::class.java, GlassMusicMiniWidget::class.java).forEach { klass ->
            val component = ComponentName(this, klass)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) sendBroadcast(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setComponent(component).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
        }
    }
}

@Composable
private fun GlassForgeApp(initialOpacity: Float, initialAccent: Int, onAppearanceChanged: (Float, Int) -> Unit, onPinWidget: (Class<out AppWidgetProvider>, Float, Int) -> Unit) {
    var tab by remember { mutableStateOf("Widgets") }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var selected by remember { mutableStateOf<WidgetDesign?>(null) }
    var opacity by remember { mutableFloatStateOf(initialOpacity.coerceIn(.25f, 1f)) }
    var accentIndex by remember { mutableIntStateOf(initialAccent.coerceIn(0, Accents.lastIndex)) }
    val accent = Accents[accentIndex]

    MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = Ink, surface = Panel, onBackground = Color.White, onSurface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize()) {
                if (selected != null) {
                    EditorScreen(selected!!, opacity, accentIndex, onBack = { selected = null }, onOpacity = { opacity = it; onAppearanceChanged(opacity, accentIndex) }, onAccent = { accentIndex = it; onAppearanceChanged(opacity, accentIndex) }, onAdd = { design ->
                        WidgetProviderRegistry.providerFor(design.provider)?.let { onPinWidget(it, opacity, accentIndex) }
                    })
                } else {
                    when (tab) {
                        "Widgets" -> WidgetLibrary(query, { query = it }, category, { category = it }, accent, onSelect = { selected = it }, onAdd = { design ->
                            WidgetProviderRegistry.providerFor(design.provider)?.let { onPinWidget(it, opacity, accentIndex) }
                        })
                        "Wallpapers" -> WallpaperGallery(accent)
                        else -> SettingsScreen(opacity, accentIndex, onOpacity = { opacity = it; onAppearanceChanged(opacity, accentIndex) }, onAccent = { accentIndex = it; onAppearanceChanged(opacity, accentIndex) })
                    }
                    Spacer(Modifier.height(6.dp))
                    NavigationBar(containerColor = Color(0xFF101116), contentColor = Color.White, tonalElevation = 0.dp) {
                        listOf(Triple("Settings", Icons.Default.Tune, "Settings"), Triple("Widgets", Icons.Default.Widgets, "Widgets"), Triple("Wallpapers", Icons.Default.Wallpaper, "Wallpapers")).forEach { item ->
                            NavigationBarItem(selected = tab == item.first, onClick = { tab = item.first }, icon = { Icon(item.second, contentDescription = item.first) }, label = { Text(item.third, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = accent, selectedTextColor = accent, indicatorColor = accent.copy(alpha = .15f), unselectedIconColor = Muted, unselectedTextColor = Muted))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetLibrary(query: String, onQuery: (String) -> Unit, category: String, onCategory: (String) -> Unit, accent: Color, onSelect: (WidgetDesign) -> Unit, onAdd: (WidgetDesign) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GLASSFORGE", color = accent, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Text("Make it yours.", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("Glass widgets for your home screen", color = Muted, fontSize = 13.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(accent, accent.copy(alpha = .25f)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        FeaturedPreview(accent)
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search widgets", color = Muted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, null) } }, singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = Color(0xFF30333D), focusedContainerColor = Panel, unfocusedContainerColor = Panel, cursorColor = accent))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Clock", "Music", "Productivity", "Device", "Calendar", "Lifestyle").forEach { name ->
                FilterChip(selected = category == name, onClick = { onCategory(name) }, label = { Text(name) }, shape = RoundedCornerShape(50), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = .22f), selectedLabelColor = Color.White, labelColor = Muted), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = category == name, borderColor = Color(0xFF333640), selectedBorderColor = accent.copy(alpha = .75f)))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Widget library", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${designs.count { (category == "All" || it.category == category) && it.name.contains(query, true) || (category == "All" || it.category == category) && it.category.contains(query, true) }} designs", color = Muted, fontSize = 12.sp)
        }
        val filtered = designs.filter { (category == "All" || it.category == category) && (query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) || it.description.contains(query, true)) }
        if (filtered.isEmpty()) Text("No matching widgets yet.", color = Muted, modifier = Modifier.padding(20.dp))
        filtered.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { design -> WidgetTile(design, Modifier.weight(1f), onOpen = { onSelect(design) }, onAdd = { onAdd(design) }) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(16.dp)) {
            Text("MORE WIDGET FAMILIES", color = accent, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Next up: Weather · Photos · Shortcuts · Focus timer", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("New clock faces, music players and screen-time tracking are now in the catalog. More families are planned.", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FeaturedPreview(accent: Color) {
    Box(Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Color(0xFF34234E), Color(0xFF171A25), Color(0xFF0E1016)))).border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(26.dp)).padding(18.dp)) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text("THE GLASS COLLECTION", color = accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Your screen.\nYour atmosphere.", color = Color.White, fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Explore styles", color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
        }
        Box(Modifier.align(Alignment.CenterEnd).size(122.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = .08f)).border(1.dp, Color.White.copy(alpha = .24f), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("10:08", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                Text("WED · OCT 21", color = accent, fontSize = 8.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(3) { Box(Modifier.size(17.dp).clip(CircleShape).background(accent.copy(alpha = .8f))) } }
            }
        }
    }
}

@Composable
private fun WidgetTile(design: WidgetDesign, modifier: Modifier, onOpen: () -> Unit, onAdd: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(22.dp)).clickable(onClick = onOpen).padding(10.dp)) {
        Box(Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(design.accent.copy(alpha = .28f), Color(0xFF22242D), Color(0xFF111218)))).border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            when (design.provider) {
                0 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (design.style == 1) "10:08" else "10:08", color = Color.White, fontSize = if (design.style == 2) 36.sp else 30.sp, fontWeight = if (design.style == 1) FontWeight.Light else FontWeight.Bold)
                    Text("WED · 21 OCT", color = design.accent, fontSize = 9.sp, letterSpacing = 1.sp)
                }
                1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BatteryFull, null, tint = design.accent, modifier = Modifier.size(36.dp))
                    Text("76%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("BATTERY", color = Muted, fontSize = 8.sp, letterSpacing = 1.sp)
                }
                2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WED", color = design.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("21", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
                    Text("OCTOBER", color = Muted, fontSize = 9.sp)
                }
                3 -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
                    Icon(Icons.Default.FormatQuote, null, tint = design.accent, modifier = Modifier.size(25.dp))
                    Text("Make today count.", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                4 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccessTime, null, tint = design.accent, modifier = Modifier.size(48.dp))
                    Text("ANALOG", color = Color.White, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                8 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("10:08", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Light)
                    Text("MONO · MINIMAL", color = design.accent, fontSize = 8.sp, letterSpacing = 1.sp)
                }
                9 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("10:08", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Text("VIOLET DIGITAL", color = design.accent, fontSize = 8.sp, letterSpacing = 1.sp)
                }
                5 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCREEN ON", color = design.accent, fontSize = 9.sp)
                    Text("2h 38m", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("OFF · 6h 12m", color = Muted, fontSize = 9.sp)
                }
                6, 7 -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Default.GraphicEq, null, tint = design.accent, modifier = Modifier.size(30.dp))
                    Text(if (design.style == 1) "NOW PLAYING" else "Dreams", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("‹   ▶   ›", color = design.accent, fontSize = 17.sp)
                }
                else -> Unit
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(design.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
        Text(design.description, color = Muted, fontSize = 10.sp, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Preview", color = design.accent, fontSize = 12.sp, modifier = Modifier.weight(1f).clickable(onClick = onOpen))
            FilledTonalIconButton(onClick = onAdd, modifier = Modifier.size(34.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = design.accent.copy(alpha = .18f), contentColor = design.accent)) { Icon(Icons.Default.Add, contentDescription = "Add ${design.name}", modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun EditorScreen(design: WidgetDesign, opacity: Float, accentIndex: Int, onBack: () -> Unit, onOpacity: (Float) -> Unit, onAccent: (Int) -> Unit, onAdd: (WidgetDesign) -> Unit) {
    val accent = Accents[accentIndex]
    var previewSize by remember(design.name) { mutableStateOf("Wide") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) { Text("WIDGET STUDIO", color = accent, fontSize = 10.sp, letterSpacing = 2.sp); Text(design.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        }
        Box(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(Color(0xFF242938), Color(0xFF111218)))).padding(20.dp), contentAlignment = Alignment.Center) {
            val previewWidth = when (previewSize) { "Small" -> .52f; "Large" -> .96f; else -> .8f }
            val previewHeight = when (previewSize) { "Small" -> 125.dp; "Large" -> 220.dp; else -> 175.dp }
            Box(Modifier.fillMaxWidth(previewWidth).height(previewHeight).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = opacity * .17f)).border(1.dp, Color.White.copy(alpha = .28f), RoundedCornerShape(28.dp)).padding(18.dp), contentAlignment = Alignment.Center) {
                when (design.provider) {
                    0 -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("10:08", color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Light); Text("WEDNESDAY · 21 OCT", color = accent, fontSize = 10.sp, letterSpacing = 1.sp) }
                    1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.BatteryFull, null, tint = accent, modifier = Modifier.size(52.dp)); Text("76%", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold); Text("BATTERY", color = Muted, fontSize = 10.sp) }
                    2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("WEDNESDAY", color = accent, fontSize = 12.sp); Text("21", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Light); Text("OCTOBER 2026", color = Muted, fontSize = 12.sp) }
                    3 -> Text("“Make today count.”", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    4 -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AccessTime, null, tint = accent, modifier = Modifier.size(68.dp)); Text("ANALOG ATELIER", color = Color.White, fontSize = 10.sp, letterSpacing = 2.sp) }
                    8 -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("10:08", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Light); Text("MONO · MINIMAL", color = accent, fontSize = 10.sp, letterSpacing = 2.sp) }
                    9 -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("10:08", color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Bold); Text("VIOLET DIGITAL", color = accent, fontSize = 10.sp, letterSpacing = 2.sp) }
                    5 -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("SCREEN ON", color = accent, fontSize = 10.sp); Text("2h 38m", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold); Text("SCREEN OFF · 6h 12m", color = Muted, fontSize = 10.sp) }
                    6, 7 -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.GraphicEq, null, tint = accent, modifier = Modifier.size(42.dp)); Text("Now Playing", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Text("‹    ▶    ›", color = accent, fontSize = 18.sp) }
                    else -> Unit
                }
            }
        }
        Text("GLASS EFFECT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.5.sp)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Transparency", color = Color.White, modifier = Modifier.weight(1f)); Text("${(opacity * 100).toInt()}%", color = accent, fontWeight = FontWeight.Bold) }
            Slider(value = opacity, onValueChange = onOpacity, valueRange = .25f..1f)
            Text("Accent color", color = Color.White, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Accents.forEachIndexed { i, c ->
                    Box(Modifier.size(34.dp).clip(CircleShape).background(c).then(if (i == accentIndex) Modifier.border(3.dp, Color.White, CircleShape) else Modifier).clickable { onAccent(i) })
                }
            }
        }
        Text("SIZE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.5.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Small", "Wide", "Large").forEach { s -> Surface(color = if (previewSize == s) accent.copy(alpha = .2f) else Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).clickable { previewSize = s }) { Text(s, color = if (previewSize == s) accent else Muted, modifier = Modifier.padding(vertical = 13.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 13.sp) } } }
        Button(onClick = { onAdd(design) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) { Icon(Icons.Default.AddToHomeScreen, null); Spacer(Modifier.width(8.dp)); Text("Add to home screen", fontWeight = FontWeight.Bold) }
        Text("Transparency and accent are applied to the real widget. Size here changes the preview only; after adding, resize the widget on your home screen using the launcher's resize handles. Widget styles are still shared globally in this build.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun WallpaperGallery(accent: Color) {
    val wallpapers = listOf(
        Triple("Violet Pulse", listOf(Color(0xFF5E1AB8), Color(0xFF160D29), Color(0xFF08090F)), "ABSTRACT"),
        Triple("Electric Blue", listOf(Color(0xFF0758B8), Color(0xFF101A32), Color(0xFF08090F)), "DARK"),
        Triple("Solar Ember", listOf(Color(0xFFFF7A18), Color(0xFF6E174F), Color(0xFF130D20)), "VIBRANT"),
        Triple("Mint Haze", listOf(Color(0xFF16A085), Color(0xFF152A35), Color(0xFF08090F)), "CALM"),
        Triple("Rose Nebula", listOf(Color(0xFFFF4D8D), Color(0xFF49206F), Color(0xFF08090F)), "GRADIENT"),
        Triple("Midnight", listOf(Color(0xFF434A56), Color(0xFF151820), Color(0xFF050609)), "MINIMAL")
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(10.dp))
        Text("WALLPAPERS", color = accent, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        Text("Set the mood.", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Text("Original gradient concepts to coordinate with your glass widgets.", color = Muted, fontSize = 13.sp)
        wallpapers.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { item ->
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(item.second)).border(1.dp, Color.White.copy(alpha = .13f), RoundedCornerShape(22.dp)).padding(14.dp)) {
                            Text(item.third, color = Color.White.copy(alpha = .75f), fontSize = 9.sp, letterSpacing = 2.sp)
                            Box(Modifier.align(Alignment.Center).size(95.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = .12f)).border(1.dp, Color.White.copy(alpha = .3f), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                                Text("10:08", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                            }
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White.copy(alpha = .8f), modifier = Modifier.align(Alignment.BottomEnd))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.first, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Preview concept", color = Muted, fontSize = 11.sp)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text("Wallpapers are preview concepts in this build; applying or downloading full-resolution wallpaper files is not implemented yet.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsScreen(opacity: Float, accentIndex: Int, onOpacity: (Float) -> Unit, onAccent: (Int) -> Unit) {
    val context = LocalContext.current
    val initialTheme = remember { GlassThemeManager.read(context) }
    var accentHue by remember { mutableFloatStateOf(initialTheme.hue) }
    var backgroundHue by remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialTheme.backgroundColor, hsv)
        mutableFloatStateOf(hsv[0])
    }
    var backgroundBrightness by remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialTheme.backgroundColor, hsv)
        mutableFloatStateOf(hsv[2].coerceIn(.04f, .35f))
    }
    var transparent by remember { mutableStateOf(initialTheme.transparentMode) }
    var saved by remember { mutableStateOf(false) }
    val accentColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(accentHue, .78f, 1f)))
    val rainbow = listOf(Color.Red, Color(0xFFFF9800), Color.Yellow, Color(0xFF00D084), Color(0xFF1D9BF0), Color(0xFF6842FF), Color(0xFFFF3DAB))
    fun updateTheme() {
        val background = android.graphics.Color.HSVToColor(floatArrayOf(backgroundHue, .22f, backgroundBrightness))
        GlassThemeManager.save(context, accentColor = android.graphics.Color.HSVToColor(floatArrayOf(accentHue, .78f, 1f)), backgroundColor = background, glassAlpha = opacity, transparentMode = transparent, hue = accentHue)
        saved = false
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(10.dp))
        Text("GLASS FORGE  /  STUDIO", color = accentColor, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        Text("Pick your colors.", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Text("A single visual language for your app and home-screen widgets.", color = Muted, fontSize = 13.sp)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF25252D), Color(0xFF111115)))).border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .32f), accentColor.copy(alpha = .25f), Color.White.copy(alpha = .04f))), RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PICK YOUR COLORS", color = Color.White, fontSize = 14.sp, letterSpacing = 2.sp, fontWeight = FontWeight.ExtraBold)
            Text("ACCENT HUE  ·  ${accentHue.toInt()}°", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Brush.horizontalGradient(rainbow)))
            Slider(value = accentHue, onValueChange = { accentHue = it; updateTheme() }, valueRange = 0f..360f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor, inactiveTrackColor = Color.White.copy(alpha = .18f)))
            Text("BACKGROUND TINT  ·  ${backgroundHue.toInt()}°", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Brush.horizontalGradient(rainbow)))
            Slider(value = backgroundHue, onValueChange = { backgroundHue = it; updateTheme() }, valueRange = 0f..360f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor, inactiveTrackColor = Color.White.copy(alpha = .18f)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SURFACE DARKNESS", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("${(backgroundBrightness * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
            }
            Slider(value = backgroundBrightness, onValueChange = { backgroundBrightness = it; updateTheme() }, valueRange = .04f.. .35f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Glass Effect", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("${(opacity * 100).toInt()}% surface opacity", color = Muted, fontSize = 12.sp)
                }
            }
            Slider(value = opacity, onValueChange = { onOpacity(it); updateTheme() }, valueRange = .20f..1f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Transparent Color", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Keep the rim and accent glow; remove the dark fill", color = Muted, fontSize = 11.sp)
                }
                Switch(checked = transparent, onCheckedChange = { transparent = it; updateTheme() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor))
            }
            Text("QUICK PRESETS", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassThemeManager.presets.forEach { (name, colorInt) ->
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color(colorInt)).border(1.dp, Color.White.copy(alpha = .65f), CircleShape).clickable {
                        android.graphics.Color.colorToHSV(colorInt, FloatArray(3).also { accentHue = it[0] })
                        updateTheme()
                    })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    val reset = GlassThemeManager.reset(context)
                    accentHue = reset.hue
                    backgroundHue = 270f
                    backgroundBrightness = .09f
                    transparent = false
                    onOpacity(reset.glassAlpha)
                    onAccent(0)
                    saved = true
                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reset")
                }
                Button(onClick = { updateTheme(); saved = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)) {
                    Icon(if (saved) Icons.Default.Check else Icons.Default.Lock, null); Spacer(Modifier.width(6.dp)); Text(if (saved) "Saved" else "Save")
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Color.White.copy(alpha = .09f), RoundedCornerShape(22.dp)).padding(16.dp)) {
            Text("LIVE STYLE", color = accentColor, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(if (transparent) listOf(Color.Transparent, Color.Transparent) else listOf(Color(android.graphics.Color.HSVToColor(floatArrayOf(backgroundHue, .22f, backgroundBrightness))), Color(0xFF101014)))).border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .32f), accentColor.copy(alpha = .25f), Color.White.copy(alpha = .04f))), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("02:38", color = accentColor, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("DARK FROSTED GLASS", color = Color.White.copy(alpha = .72f), fontSize = 9.sp, letterSpacing = 2.sp)
                }
            }
        }
        SettingsRow(Icons.Default.Widgets, "Widget library", "Browse implemented native widget designs")
        SettingsRow(Icons.Default.Palette, "Theme engine", "Shared persistent styling and instant widget refresh")
        SettingsRow(Icons.Default.Info, "About GlassForge", "Native Android widgets · dark glass studio")
        Text("Wallpaper blur is approximated with translucent gradients because Android launchers don't expose live wallpaper blur to RemoteViews.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = .06f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Accents[0]) }
        Spacer(Modifier.width(13.dp))
        Column { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 12.sp) }
    }
}
