package com.brave.glassforge

import android.appwidget.AppWidgetProvider

/**
 * Shared metadata and provider routing for the native widget catalog.
 *
 * Keep widget identity and platform capabilities here rather than scattering provider
 * selection across UI click handlers. The catalog can grow by adding a definition and
 * implementing its provider without duplicating routing logic in Compose screens.
 */
enum class WidgetFamily { CLOCK, DEVICE, CALENDAR, LIFESTYLE, PRODUCTIVITY, MUSIC }
enum class WidgetUpdateMode { PLATFORM_CLOCK, SYSTEM_EVENT, USER_REFRESH, MEDIA_SESSION, PERIODIC }
enum class WidgetInteraction { OPEN_APP, REFRESH, MEDIA_CONTROLS, OPEN_SETTINGS, NONE }

data class WidgetDefinition(
    val catalogId: Int,
    val key: String,
    val family: WidgetFamily,
    val provider: Class<out AppWidgetProvider>,
    val supportedSizes: Set<String>,
    val updateMode: WidgetUpdateMode,
    val interactions: Set<WidgetInteraction>
)

object WidgetProviderRegistry {
    private val definitions = listOf(
        WidgetDefinition(0, "orbit-digital", WidgetFamily.CLOCK, GlassClockWidget::class.java, setOf("small", "wide"), WidgetUpdateMode.PLATFORM_CLOCK, setOf(WidgetInteraction.OPEN_APP)),
        WidgetDefinition(1, "glow-battery", WidgetFamily.DEVICE, GlassBatteryWidget::class.java, setOf("small", "wide"), WidgetUpdateMode.SYSTEM_EVENT, setOf(WidgetInteraction.REFRESH)),
        WidgetDefinition(2, "day-marker", WidgetFamily.CALENDAR, GlassCalendarWidget::class.java, setOf("small", "wide"), WidgetUpdateMode.PERIODIC, setOf(WidgetInteraction.OPEN_APP)),
        WidgetDefinition(3, "daily-thought", WidgetFamily.LIFESTYLE, GlassQuoteWidget::class.java, setOf("wide", "large"), WidgetUpdateMode.PERIODIC, setOf(WidgetInteraction.OPEN_APP)),
        WidgetDefinition(4, "analog-atelier", WidgetFamily.CLOCK, GlassAnalogClockWidget::class.java, setOf("small", "wide"), WidgetUpdateMode.PLATFORM_CLOCK, setOf(WidgetInteraction.OPEN_APP)),
        WidgetDefinition(5, "screen-balance", WidgetFamily.PRODUCTIVITY, GlassScreenTimeWidget::class.java, setOf("wide", "large"), WidgetUpdateMode.USER_REFRESH, setOf(WidgetInteraction.REFRESH, WidgetInteraction.OPEN_SETTINGS)),
        WidgetDefinition(6, "now-playing", WidgetFamily.MUSIC, GlassMusicWidget::class.java, setOf("wide", "large"), WidgetUpdateMode.MEDIA_SESSION, setOf(WidgetInteraction.MEDIA_CONTROLS, WidgetInteraction.OPEN_SETTINGS)),
        WidgetDefinition(7, "music-glass-bar", WidgetFamily.MUSIC, GlassMusicMiniWidget::class.java, setOf("small", "wide"), WidgetUpdateMode.MEDIA_SESSION, setOf(WidgetInteraction.MEDIA_CONTROLS, WidgetInteraction.OPEN_SETTINGS)),
        WidgetDefinition(8, "mono-clock", WidgetFamily.CLOCK, GlassMonoClockWidget::class.java, setOf("small", "wide"), WidgetUpdateMode.PLATFORM_CLOCK, setOf(WidgetInteraction.OPEN_APP)),
        WidgetDefinition(9, "violet-time", WidgetFamily.CLOCK, GlassNeonClockWidget::class.java, setOf("small", "wide"), WidgetUpdateMode.PLATFORM_CLOCK, setOf(WidgetInteraction.OPEN_APP))
    )

    fun providerFor(catalogId: Int): Class<out AppWidgetProvider>? =
        definitions.firstOrNull { it.catalogId == catalogId }?.provider

    fun definitionFor(catalogId: Int): WidgetDefinition? =
        definitions.firstOrNull { it.catalogId == catalogId }

    fun all(): List<WidgetDefinition> = definitions
}
