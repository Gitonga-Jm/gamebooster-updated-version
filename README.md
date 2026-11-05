# GameBoost Pro

Production-ready Android gaming optimization app built with **Kotlin**, **Jetpack Compose**, and a **single-activity** architecture.

## Features

- **5-tab bottom navigation**: Home, Games, Tools, Stats, Settings
- **Universal BOOST NOW FAB** on every main screen
- **3-second Lottie splash** with animated logo
- **3-screen onboarding** with permission shortcuts (notifications, usage access, battery)
- **Foreground boost service** with live notification (CPU temp, RAM free, ping)
- **Dark gaming theme** — neon green / cyan accents, monospace HUD typography
- **DataStore** preferences (boost score, settings, onboarding state)
- **Game library** — detects installed games via category + keyword heuristics

## Requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35
- Min SDK 26

## Build

```bash
cd GameBoostPro
./gradlew assembleDebug
```

Install on device/emulator:

```bash
./gradlew installDebug
```

## Architecture

```
app/
├── MainActivity.kt          # Single activity, Compose root
├── GameBoostApplication.kt  # App-wide DI (preferences)
├── data/                    # Repositories, metrics, game catalog
├── domain/                  # BoostEngine optimization pipeline
├── service/                 # BoostForegroundService
└── ui/
    ├── navigation/          # Routes & destinations
    ├── screens/             # Splash, onboarding, tabs
    ├── components/          # FAB, neon cards
    └── theme/               # Colors, typography
```

## Permissions

| Permission | Purpose |
|------------|---------|
| `POST_NOTIFICATIONS` | Boost monitor notification |
| `FOREGROUND_SERVICE` | Persistent performance monitor |
| `PACKAGE_USAGE_STATS` | Game detection (optional, via Settings) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Uninterrupted gaming sessions |

## Package

`com.gameboost.pro`

## License

Proprietary — demo project for GameBoost Pro.
