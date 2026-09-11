# StatusBar+ — Project 3 / 6

Reorder status bar icons, custom battery & clock styles. Native Android, Kotlin.

## Important — how this actually works (read this before you start)
Android does **not** give any non-root, non-system app a way to reach into the real
SystemUI status bar and reorder its icons or change its clock/battery renderer — that
UI is owned by the `com.android.systemui` privileged system process, not something
`SYSTEM_ALERT_WINDOW` or any public API can touch. Apps that claim to do this on
un-rooted phones (and the ones on the Play Store that look like they do) all use the
same workaround StatusBar+ uses:

**Draw a full-width strip on top of the real status bar** (via a
`TYPE_APPLICATION_OVERLAY` window) showing your own clock, battery, and icon row —
so visually it looks reordered/restyled, even though the real status bar underneath
is untouched. That's what's built here. I wanted to flag this plainly rather than
ship something that quietly does less than "reorder status bar icons" sounds like.

If you specifically need the *real* system status bar modified (not an overlay), that
requires either a rooted device + a Xposed/LSPosed module, or being a preloaded
OEM system app (this is literally how Samsung Good Lock does it) — both are a very
different, much bigger project than a 2-week sprint. Worth deciding now whether the
overlay approach is good enough for what you want to ship.

## What's included here (the actual code)
```
StatusBarPlus/
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/statusbarplus/app/
        │   ├── MainActivity.kt
        │   ├── data/
        │   │   ├── Styles.kt              (ClockStyle / BatteryStyle enums)
        │   │   └── PrefsStore.kt          (DataStore-backed settings)
        │   ├── overlay/
        │   │   ├── OverlayService.kt      (owns the overlay window, foreground service)
        │   │   ├── StatusBarCanvasView.kt (draws clock/battery/icons via Canvas)
        │   │   └── BatteryReader.kt
        │   ├── service/
        │   │   ├── NotificationIconListenerService.kt  (tracks apps with active notifications)
        │   │   └── BootReceiver.kt        (restarts overlay after reboot if it was on)
        │   └── ui/
        │       ├── HomeScreen.kt
        │       ├── IconReorderScreen.kt
        │       ├── ClockStyleScreen.kt
        │       └── BatteryStyleScreen.kt
        └── res/values/ (strings.xml, themes.xml)
```

## What YOU need to add locally
1. Open in Android Studio — generates `gradle/wrapper/`, `local.properties`, and pulls
   every dependency (Compose, DataStore) into your Gradle cache, same as HaptiKit.
2. Launcher icon via Image Asset (New → Image Asset) — cosmetic, skipped here.
3. On the device you test on: grant "draw over other apps" and "notification access"
   from the Home screen buttons — both open the correct system settings screen for you.

## How the pieces fit together
- **NotificationIconListenerService** is the data source for "which apps currently have
  an icon that would show" — it can't read the *real* status bar's icon list (no API
  exists for that either), so it infers the same thing from active notifications, which
  is what actually puts icons in the real status bar in the first place.
- **IconReorderScreen** lets you set a priority order for those apps with plain up/down
  buttons — no external drag-and-drop dependency needed for v0.1.
- **PrefsStore** (Jetpack DataStore) is the single source of truth; **OverlayService**
  collects it as Flows so style/order changes apply live without restarting the overlay.
- **StatusBarCanvasView** does the actual drawing — ticks once a second for the clock,
  redraws battery % from a fresh `ACTION_BATTERY_CHANGED` sticky-intent read, and lays
  out app icons from `iconOrder` using `PackageManager.getApplicationIcon`.
- **BootReceiver** restarts the overlay after a reboot only if it was switched on before
  (checked via `PrefsStore.overlayEnabled`).

## Known v0.1 limitations (matches the roadmap's scope)
- This replaces the *visual* status bar with a look-alike; the real one is still there
  underneath (invisible behind the overlay, but still functionally present).
- Notification-shade pull-down still works normally — the overlay doesn't intercept touch
  (`FLAG_NOT_FOCUSABLE`), it's purely a visual layer.
- No quick-settings tile to toggle the overlay yet (roadmap mentions the Quick Settings
  Tile API as part of the stack) — the Home screen switch covers this for v0.1; adding a
  QS Tile is a quick follow-up if you want one-tap toggling from the shade.

## Next when you're ready
Tell me when this one's running and I'll move on to **SoundSkin** (Kotlin, animated custom volume HUD overlay).
