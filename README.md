# Kalo — Android meal, workout & progress tracker

Kalo is a native Android app (Kotlin, Jetpack Compose, Room) for personal tracking: log food by photo, barcode or
search, log workouts and water, and see whether your habits are moving you toward your goal.

**Local storage by design.** Tracking records are stored on the phone. There are no accounts or app backend. The
external services are (1) Google Gemini, when you scan a meal photo or ask for an AI summary, using *your own*
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
- **Workouts** with multiple exercises, saved routines, editing, set completion, a rest timer, previous-session prefill and a progressive-overload hint.
- **Water, goals and macro presets**, Health Connect steps and active calories, home-screen widget.
- **Trends & coaching** - 7/30-day calorie and protein charts, streak, plain-language insights, weight log
  with historical daily goals and evidence-gated calorie-target checks, optional AI weekly summary. Mark fully logged days complete on the dashboard; averages exclude partial/missing days and today.
- **Daily reminder** that only fires when the day looks under-logged.
- **Personal foods** - label values, favorites, editable templates, recipes with cooked yield, quick kcal/protein estimates and direct nutrition corrections. Previously looked-up barcodes work from a local cache.
- **Your data** - photo-inclusive ZIP archives, daily dated backups to a chosen folder, restore preview/merge, JSON backup/import, CSV export and delete-all. API keys and folder grants are excluded from exports.

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

## Instrumented integration checks

`assembleDebugAndroidTest` builds a dependency-free instrumentation runner. On a disposable emulator, install both debug APKs and run:

```bash
adb -s emulator-5554 shell am instrument -w com.kalotracker.app.test/com.kalotracker.app.IntegrationRunner
```

It uses disposable databases/photos/preferences for Room migration, archive/merge, barcode cache, workout replacement and goal persistence checks. Do not install a test build over a real personal-use installation without checking signing and making a complete backup.

## Database changes

Room exports its schema to `app/schemas/`. Never use a destructive migration: bump the version, write a
`Migration` in `Migrations.kt`, and extend `MigrationTest` (it runs on the JVM against the exported
schemas and checks that data survives and the structure matches a fresh database).

## Design docs

- [docs/BRANDING_AND_UIUX.md](docs/BRANDING_AND_UIUX.md) - visual language
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - original architecture notes (the cloud-sync parts are no longer applicable)

See [phases 2–5 implementation and verification](docs/PHASES_2_5_IMPLEMENTATION.md) for implementation details and verification limitations. Local diff artifacts referenced in development reports are excluded from the public repository.

## Appearance

Warm Precision / Open Plate is applied with system/light/dark appearance, Today / Meals / Progress navigation and a shared Add meal chooser. Existing local records, personal foods, workout routines and full photo archives are retained. See [the gap-check and branding report](docs/BRANDING_AND_GAP_CHECK.md) for verification and remaining phone acceptance checks.

## Everyday habits

Photo meals now lead with approximate calories and optional portion adjustments; grams and macros are expandable. Daily fitness logs short bodyweight sessions with remembered choices. Photo-meal drafts recover locally. See [implementation and verification](docs/HABIT_TRACKING.md) for scope, review, evidence and device-testing limits.

## Releases and contributions

Every completed app version upgrade updates both version fields and the [changelog](CHANGELOG.md).
Pull requests run unit tests, lint and debug/test builds. After a validated version upgrade reaches
`main`, GitHub Actions publishes its version tag and source release. See [the release checklist](docs/RELEASING.md).
New to GitHub? Start with [the project GitHub guide](docs/GITHUB_GUIDE.md).
The working tree can contain unreleased development features; use tagged releases to identify versions.

Contributions should include relevant verification and fictional test data. Read [security and privacy guidance](SECURITY.md)
before filing reports. Optional AI analysis sends selected images to Gemini; local backups can contain personal health records and photos.

Source code is licensed under the [MIT License](LICENSE). Bundled Open Food Facts data
retains its own licenses; see [third-party notices](THIRD_PARTY_NOTICES.md).
