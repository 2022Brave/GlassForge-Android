# GlassForge Android

GlassForge is a native Android app exploring a premium glass-widget studio aesthetic.

## Current UI
- Three main sections: **Settings, Widgets, Wallpapers**.
- Searchable widget catalog with category filters and large live-style previews.
- Widget detail/studio screen with opacity and accent controls.
- Four working Android widget provider families: clock, battery, calendar/date and daily quote.
- Android launcher pin flow for adding those providers to the home screen.
- Saved global opacity/accent settings; supported widgets refresh when settings change.
- Wallpaper gallery screen with original gradient preview concepts.
- GitHub Actions builds a debug APK artifact on pushes to main.

## Build and install
Open **Actions → Android CI → latest run → Artifacts** and download `glassforge-debug-apk`. Install the debug APK on an Android phone.

## Important current limitations
This is an early implementation and not yet equivalent to commercial catalogs advertising hundreds of widgets. The catalog contains eight design previews, but multiple previews currently map to the same underlying native widget provider. Per-widget independent configuration, custom widget-specific layouts, a large asset library, real downloadable/applicable wallpapers, weather, music controls, photos, shortcuts and icon packs are not implemented yet.

Android's standard home-screen widget API uses `RemoteViews`. It supports text, icons, click actions and resizing, but not arbitrary live wallpaper refraction or full Compose animations. The current widgets use a translucent glass-inspired appearance rather than claiming true live background blur.

## Next engineering milestones
1. Implement per-widget configuration persisted by app-widget ID.
2. Create separate layout variants and preview-to-provider mappings so each design preview matches the installed widget.
3. Add real wallpaper assets and wallpaper application flow.
4. Expand the catalog with independently implemented weather, music, photo and shortcut widgets.
5. Validate build, widget pinning, resize and refresh on Samsung One UI and Motorola launchers.
