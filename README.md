# Kalo — Minimal AI Meal, Workout & Step Tracker

**Kalo** is a minimalist, high-utility native Android health app built with **Kotlin**, **Jetpack Compose**, **Android Health Connect**, and **Supabase (Edge Functions + Google Gemini Vision)** following clean architecture and the "Nordic / Japanese Utilitarian" aesthetic (*Eat. Move. Glance. Repeat.*).

---

## What's New & Upgraded

- 📸 **CameraX & Image Compression Pipeline**: Captured photos are downsampled to max 1024px and compressed as JPEG to prevent Out-Of-Memory (OOM) errors, saved to app internal storage for thumbnails in the activity stream, and Base64 encoded for Gemini Vision. Also supports picking food photos directly from the gallery.
- 📅 **Date Horizon Navigation**: Interactive date stepper on the dashboard (`< Yesterday`, `Tomorrow >`, `Jump to Today`) allows browsing and reviewing full meal and workout histories across any day.
- 🥗 **Manual Food Log & USDA Food Catalog**: Offline verified database with common healthy foods (chicken breast, salmon, oats, eggs, rice, avocado, Greek yogurt, etc.). Calculate calories and macros dynamically by gram portion, or log meals without taking photos.
- 💡 **AI Dietitian Daily Insights**: Smart engine that provides real-time personalized insights based on your daily macronutrient pacing, protein deficits, and post-workout glycogen recovery.
- 🏋️ **Upgraded Workout Logger**: Popular exercise quick selector chips with automatic retrieval and pre-fill of your previous session's weight and reps (`getLastSessionSetsForExercise`), custom duration and calories burned calculation, and set deletion.
- 🎯 **Goals & Settings Horizon**: Customize daily target calories, protein, carbs, fat, and step goals with 1-tap macro presets (High Protein 40/35/25, Balanced 30/45/25, Low Carb, Keto) and manual entry.
- ☁️ **Repository Pattern & Supabase Cloud Sync**: `MealRepository`, `WorkoutRepository`, `UserProfileRepository`, and `AuthRepository` provide a single source of truth, offline-first caching via Room, and automatic/manual background synchronization to Supabase Postgres.
- 🔐 **Authentication & Guest Mode**: Sign in with Supabase Auth or track immediately with zero barriers via Offline Guest Mode.
- 👟 **Health Connect Integration**: Dedicated rationale and permissions screen, reading aggregated daily steps and active energy expenditure directly from Google Fit, Samsung Health, and smartwatches.

---

## Architecture

```
[ CameraX / Gallery ] ──> [ ImageUtils (1024px JPEG) ] ──> [ Supabase Edge Function (Gemini 2.0 Flash) ]
                                                                      │
                                                                      ▼
[ Food Catalog Search ] ──────> [ MealReview Sheet ] ──────> [ MealRepository ] ──> [ Supabase Cloud Sync ]
                                                                      │
                                                                      ▼
                                                              [ Room Database ]
                                                                      ▲
                                                                      │
[ Health Connect ] ───────> [ Step & Active Calorie Aggregates ] ─────┘
```

- **UI Layer (`feature/`)**: Modularized by feature (`dashboard`, `meal`, `workout`, `settings`, `health`, `auth`) using Jetpack Compose and unidirectional `StateFlow`.
- **Domain & Repository Layer (`core/data/repository/`)**: `MealRepository`, `WorkoutRepository`, `UserProfileRepository`, and `AuthRepository`.
- **Local Persistence (`core/database/`)**: Room database with relational tables, foreign key cascades, and reactive `Flow` queries.
- **AI & Computer Vision (`supabase/functions/analyze-meal/`)**: Google Gemini 2.0 Flash with clinical prompt and strict JSON schema enforcement.

---

## Project Structure

```
Meal Tracking App/
├── docs/
│   ├── BRANDING_AND_UIUX.md          # Design tokens, typography & UX philosophy
│   └── ARCHITECTURE.md               # Architecture & subsystem integration guide
├── app/
│   ├── build.gradle.kts              # Compose, Health Connect, CameraX, Room, Supabase
│   └── src/main/
│       ├── AndroidManifest.xml       # Health Connect permissions & rationale declarations
│       └── java/com/kalotracker/app/
│           ├── core/
│           │   ├── ai/               # AI Nutrition Insight Engine
│           │   ├── data/
│           │   │   ├── food/         # USDA verified food catalog
│           │   │   └── repository/   # Meal, Workout, Profile & Auth Repositories
│           │   ├── database/         # Room DB, DAOs & Entities (Meals, Sets, Workouts)
│           │   ├── designsystem/     # OLED Dark theme, concentric rings, step gauge card
│           │   ├── health/           # Health Connect Manager (Google Fit & Samsung Health)
│           │   ├── network/          # Supabase client & Gemini Vision service
│           │   └── util/             # ImageUtils downsampling & compression pipeline
│           ├── feature/
│           │   ├── auth/             # Sign In, Sign Up & Guest Mode
│           │   ├── dashboard/        # Daily Horizon screen, date stepper & insight card
│           │   ├── health/           # Health Connect permissions rationale screen
│           │   ├── meal/             # CameraX viewfinder, review sheet & manual food log
│           │   ├── settings/         # Goals, macro split presets & cloud sync
│           │   └── workout/          # Multi-exercise set & rep logger with session history
│           └── navigation/           # Compose NavHost & destination routes
├── supabase/
│   ├── functions/analyze-meal/       # Gemini Flash Edge Function with JSON Schema enforcement
│   └── migrations/                   # Cloud sync schema & RLS policies
```

---

## Configuration & Getting Started

### 1. Android Studio Setup
1. Open Android Studio (Ladybug / Iguana or later).
2. Open directory: `C:\Users\raunak.chopra\Desktop\Al Bots\Meal Tracking App`.
3. Allow Gradle to sync.

### 2. Supabase & Gemini Setup
1. Deploy the Edge Function:
   ```bash
   cd supabase
   supabase functions deploy analyze-meal --no-verify-jwt
   supabase secrets set GEMINI_API_KEY="your-gemini-api-key"
   ```
2. Update your Supabase URL & Anon Key in [SupabaseModule.kt](file:///C:/Users/raunak.chopra/Desktop/Al%20Bots/Meal%20Tracking%20App/app/src/main/java/com/kalotracker/app/core/network/SupabaseModule.kt).
*(Note: A realistic mock fallback is included out-of-the-box so you can run and test the app immediately without configuring backend keys).*
