# GlassForge Android

GlassForge is a native Android widget pack and companion Studio built with Jetpack Compose and launcher-compatible `AppWidgetProvider` / `RemoteViews`.

## Current architecture

- **Studio UI:** Jetpack Compose library, search and category filters, editor, wallpapers gallery, dark-glass appearance controls and floating three-tab navigation.
- **Theme persistence:** `GlassThemeManager` stores accent color, HSV hue, background tint, surface alpha and transparent mode in SharedPreferences. Saving appearance broadcasts `ACTION_APPWIDGET_UPDATE` to all registered widget providers.
- **Bitmap widget pipeline:** `GlassCanvasRenderer` draws a high-resolution ARGB bitmap with a diagonal glass surface, directional specular rim, ambient accent glow, subtle pinstripes and widget-specific Canvas artwork. The shared `widget_glass_canvas.xml` is a single ImageView RemoteViews layout.
- **Native interactive widgets:** Music controls and screen-time tracking retain RemoteViews interactions and their platform permissions. Canvas-based static designs are used where the entire face can be rendered as one bitmap.
- **Registry:** `WidgetEngine.kt` defines widget family metadata and maps catalog entries to installed provider classes.

## Canvas-rendered designs

The shared renderer currently provides styles for segmented battery, digital clock, neon outline clock, stacked clock, calendar, quote, search pill, weather preview, compass, music artwork and screen-time artwork. Battery, digital/stacked/neon clocks, calendar and quote currently use the shared bitmap layout. Existing interactive music and screen-time widgets keep their native RemoteViews controls.

The weather artwork is a **preview**, not live meteorological data. It explicitly labels itself as a preview until a weather source and location preference are configured.

## Build

Open the project in Android Studio and build the `app` module, or use the GitHub Actions workflow at [Actions](https://github.com/2022Brave/GlassForge-Android/actions). The workflow builds `assembleDebug` and uploads an artifact named `glassforge-debug-apk`.

## Platform notes

- Android launcher widgets use RemoteViews and cannot run arbitrary Compose layouts.
- The renderer creates a frosted-glass *visual treatment* using gradients and rim lighting; Android does not expose a general live wallpaper-blur API to RemoteViews.
- Android limits frequent widget refreshes. Theme saves request immediate refreshes, while periodic updates follow launcher/system scheduling.
- Usage access is needed for screen-time reporting. Media notification access is needed for active media-session controls.
- The current catalog reflects implemented designs and providers, not the hundreds of designs advertised by commercial widget packs.
