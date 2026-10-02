# StatusBar+

A custom status bar: choose the clock style, battery style, colours and the order of notification icons. Kotlin + Jetpack Compose.

## Build
Open the folder in Android Studio and run, or from a terminal:
```
gradlew assembleDebug
```

## Set up on the phone (once)
The app's checklist walks you through it:
1. **Turn on the StatusBar+ service**: Accessibility → StatusBar+ → on.
2. **Allow notification access**, so your bar can show the same app icons the real one does.
Then flip **Custom status bar** on.

## Using it
- The preview at the top updates live as you change things.
- **Clock**: follow the phone's setting, 24-hour, 12-hour, with seconds, minimal, or with the date.
- **Battery**: icon + percent, icon only, percent only, ring, or a dot that turns red when low. Charging shows a bolt and turns the level green.
- **Colors**: bar background and text/icon colour.
- **Notification icon order**: put the apps you care about first. Apps keep their place even when they have nothing showing, and *Show up to N icons* caps how many appear.
- **Show Wi-Fi / mobile data** and **Hide in landscape** (so games and videos keep the full screen).

Pulling down the notification shade works exactly as normal; the bar ignores touches.

## How it works (and its limits)
Android doesn't let apps change the real status bar without root, so StatusBar+ draws a look-alike strip on top of it. It uses an accessibility overlay because that's the only non-root window type that sits *above* the system status bar; an ordinary overlay would sit underneath it. This also means no foreground-service notification and automatic restart after a reboot. The service doesn't read screen content.

- Notification icons are the apps' own small status-bar glyphs, tinted to your colour. Minimized notifications are skipped, like the real bar does.
- Mobile signal strength isn't shown (it needs a phone-state permission), so on mobile data you'll see a data indicator instead of bars.
