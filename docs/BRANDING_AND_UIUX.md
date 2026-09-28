# Kalo — Branding & UI/UX Design System

> **Application Name**: **Kalo**  
> **Tagline**: *Eat. Move. Glance. Repeat.*  
> **Aesthetic**: Minimal Nordic / Japanese Utilitarian (Pure dark surfaces, tabular metrics, calm micro-interactions).

---

## 1. Design Philosophy

- **Zero Accounting Friction**: Logging meals should feel like taking a quick photo, not doing tax paperwork.
- **The 3-Second Rule**: Primary tasks (snap food, check steps, log a gym set) take under 3 seconds.
- **Objective, Not Punitive**: Data is presented cleanly without loud red "over-budget" alarms.

---

## 2. Color Palette & Tokens

```
Background Primary:      #090A0F  (Deep OLED Obsidian)
Surface Elevated:        #14161F  (Dark Slate Cards)
Surface Higher:          #1D212E  (Modals & Action Sheets)
Border Subtle:           #262B3D  (Hairline Dividers)
Text Primary:            #F8FAFC  (High-contrast white)
Text Secondary:          #94A3B8  (Muted labels & timestamps)

Macro Accents:
  Calories:              #FFFFFF  (Clean White Ring)
  Protein:               #38BDF8  (Sky Blue 400)
  Carbs:                 #FBBF24  (Amber Gold 400)
  Fat:                   #F43F5E  (Rose Coral 500)
  Steps / Health:        #10B981  (Emerald Mint 500)
```

---

## 3. Typography Hierarchy

- Built with tabular figures (`FontFeatureSettings = "tnum"`) so real-time counters do not jitter.
- Display Hero: 48sp Bold (Steps & Calories).
- Section Titles: 20sp SemiBold.
- Data Values: 16sp Medium (Grams, Weights, Reps).
- Metric Badges: 11sp Bold All-Caps (+0.08em letter spacing).

---

## 4. Key Screens

1. **Dashboard (The Daily Horizon)**:
   - Macro Rings (Calorie budget, Protein, Carbs, Fat).
   - Health Connect Step Gauge (Real-time sync badge with Samsung Health / Google Fit).
   - Today's chronological feed (Meals with photo thumbnails + workouts).
2. **CameraX Quick Capture**:
   - Ultra-clean viewfinder, focal guides, shutter button with instant haptic pulse.
   - Quick modifiers: `[Cooked with Oil?]` toggle.
3. **Meal Review & Calibrate Sheet**:
   - AI estimated item list with weight in grams.
   - 1-tap `+` / `-` 10% steppers to adjust portion size dynamically.
4. **Minimal Workout Logger**:
   - Direct sets/reps input with "Repeat Last Session" prefill.
5. **Health Connect Onboarding**:
   - Explicit trust-building privacy screen for Google Fit & Samsung Health permissions.
