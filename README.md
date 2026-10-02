# Kalo — Android meal, workout & progress tracker

Kalo is a native Android app (Kotlin, Jetpack Compose, Room) for one person: log food by photo, barcode or
search, log workouts and water, and see whether your habits are moving you toward your goal.

**Local-only by design.** All data is stored on the phone. There are no accounts and no server. The only
network calls are (1) Google Gemini, when you scan a meal photo or ask for an AI summary, using *your own*
API key, and (2) Open Food Facts, for barcode lookups.

## Features

- **AI meal scan** - photograph a meal (or pick from the gallery); Gemini estimates foods and portions.
  Every name and weight is editable, you can add/remove foods, add a note and re-analyze, and set the meal
  time. Nothing is saved until you confirm. If the key is missing, the photo is not food, or the network is
  down, you get a clear message, never a made-up meal.
- **Barcode scan** (on-device ML Kit + Open Food Facts). Unknown products or missing calorie data show
  "not found" with scan-again / enter-manually, never invented values.
- **Manual logging** from an 82-food catalog, with recent meals for one-tap re-logging.
- **Edit and undo** - edit any logged meal, undo deletes, log a meal again.
- **Workouts** with sets/reps, previous-session prefill and a progressive-overload hint.
- **Water, goals and macro presets**, Health Connect steps and active calories, home-screen widget.
- **Trends & coaching** - 7/30-day calorie and protein charts, streak, plain-language insights, weight log
  with a goal-aware calorie-target check, optional AI weekly summary.
- **Daily reminder** that only fires when the day looks under-logged.
- **Your data** - JSON backup/import, CSV export, delete-all.

## Setup

Requirements: JDK 17 and the Android SDK (compileSdk 35). Point Gradle at the SDK with `ANDROID_HOME` or a
`local.properties` file containing `sdk.dir=...`.

```bash
./gradlew testDebugUnitTest assembleDebug     # build + tests
./gradlew installDebug                        # install on a connected phone (USB debugging on)
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk` and can also be sideloaded by
copying it to the phone.

### Turn on photo scanning
1. Create a free key at <https://aistudio.google.com/apikey>.
2. In the app: Settings > AI meal scanning > paste the key > **Save & test**.

The key is stored only in the app's private storage. The default model is the rolling alias
`gemini-flash-latest`; you can change it in Settings if Google retires or renames a model.

## Project layout

```
app/src/main/java/com/kalotracker/app/
  core/
    ai/           NutritionInsightEngine, TrendAnalyzer, GoalAdvisor, OverloadCoach (pure, unit-tested)
    data/         food catalog, repositories, backup (BackupCodec/BackupManager)
    database/     Room database, DAOs, entities, Migrations
    health/       Health Connect
    network/      MealAnalysisService (Gemini), OpenFoodFactsService
    reminder/     WorkManager reminder
    settings/     AppSettings (API key, model, reminder)
  feature/        dashboard, meal, barcode, workout, trends, settings, health, widget
app/schemas/      exported Room schemas (used by the migration test)
```

## Database changes

Room exports its schema to `app/schemas/`. Never use a destructive migration: bump the version, write a
`Migration` in `Migrations.kt`, and extend `MigrationTest` (it runs on the JVM against the exported
schemas and checks that data survives and the structure matches a fresh database).

## Design docs

- [docs/BRANDING_AND_UIUX.md](docs/BRANDING_AND_UIUX.md) - visual language
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - original architecture notes (the cloud-sync parts are no longer applicable)

## Releases and contributions

Every completed app upgrade updates both Android version fields and [CHANGELOG.md](CHANGELOG.md). Pull requests run tests, lint and build checks. A validated version upgrade merged to main publishes a version tag and source release.

Start with [the GitHub guide](docs/GITHUB_GUIDE.md) and [release checklist](docs/RELEASING.md). Use fictional data in reports and read [security and privacy guidance](SECURITY.md).

Licensed under the [MIT License](LICENSE).
