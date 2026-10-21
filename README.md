# GlassForge Android

GlassForge is a native Android widget studio focused on glass-inspired home-screen widgets.

## Current implemented starter
- A real widget library screen with **Add** actions that request the Android launcher to pin a selected widget.
- Digital clock/date, battery level + charging state, calendar/date, and daily quote widgets.
- Resizable home-screen widget definitions.
- Shared opacity and accent settings saved on-device and applied to existing GlassForge widgets.
- Battery widget refreshes when the charger is connected or disconnected.
- GitHub Actions debug APK build.

## Build and install
Open **Actions → Android CI → latest run → Artifacts** and download `glassforge-debug-apk`. Install the debug APK on your Android phone. To add a widget, open GlassForge and tap **Add**; approve the launcher prompt. If the launcher does not show the prompt, long-press the home screen and open **Widgets → GlassForge**.

## Important platform note
Android's standard home-screen widget API uses `RemoteViews`. It supports real text, icons, click actions and resizing, but not arbitrary live wallpaper refraction or full Compose animations. GlassForge therefore uses a translucent, glass-inspired surface with highlights rather than claiming true live background blur.

## Roadmap
The reference apps offer much larger catalogs. Planned expansion includes analog clock variants, weather, music controls, app shortcuts, photo widgets, wallpaper gallery, icon-pack assets and per-widget configuration. The current build is an early functional foundation, not a 350-widget replacement yet.
