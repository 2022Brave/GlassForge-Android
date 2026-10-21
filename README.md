# GlassForge Android

GlassForge is a native Android app exploring a premium liquid-glass widget studio aesthetic.

## Widget families currently implemented in source
- **Digital clocks:** Orbit, minimalist Mono and Violet/neon digital faces.
- **Analog clock:** live analog clock widget with a glass-style panel.
- **Battery:** battery percentage and charging state.
- **Calendar/date:** live date and day.
- **Daily thought:** static quote panel.
- **Screen time:** daily estimated screen-on and screen-off duration using Android Usage Access events.
- **Music:** two widget layouts with previous, play/pause and next controls via the active Android media session.

The searchable library contains design previews, an editor with transparency/accent controls, and the Android launcher pin flow. The current appearance controls are still global rather than stored independently per widget ID.

## Permissions and setup
- **Screen time:** tap the screen-time widget's background to open Android Usage Access settings and enable GlassForge. Tap either duration value to refresh the reading manually. Android's widget refresh scheduling is periodic, not second-by-second.
- **Music:** tap the track title to open Notification Access settings and enable GlassForge. A compatible music app must expose an active media session; play/pause/next/previous availability depends on that player.
- **Clock and calendar:** use Android's built-in clock/date views for live time/date display.
- **Resize:** the launcher can resize supported widgets using its normal resize handles. Studio size selection currently changes the preview only.

## Build and install
GitHub Actions is configured to build a debug APK on pushes to the main branch. Open Actions → Android CI → latest run → Artifacts and download glassforge-debug-apk only after the run succeeds. A successful build and device test are still required before considering this feature set verified.

## Important limitations
This is an early implementation, not yet equivalent to commercial catalogs advertising hundreds of widgets. Some catalog designs remain visual variants of an existing provider. Per-widget independent styling, fully size-specific responsive layouts, downloadable/applicable wallpapers, weather, photo frames, shortcuts, focus timer and extensive clock-face customization remain future work.

Android's standard home-screen widget API uses RemoteViews. It supports text, icons, click actions and resizing, but not arbitrary live wallpaper refraction or full Compose animations. GlassForge uses translucent glass-inspired surfaces and should not claim true live background blur.

## Next engineering milestones
1. Store configuration independently for every app-widget ID.
2. Build responsive layouts for launcher size changes.
3. Add richer clock faces and more independently designed music players.
4. Implement real wallpaper assets and wallpaper application.
5. Validate build, permission flows, pinning, resize and refresh on Samsung One UI and Motorola launchers.
