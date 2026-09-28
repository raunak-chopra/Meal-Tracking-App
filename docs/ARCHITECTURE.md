# Kalo — Architecture & Technical Reference

## 1. System Overview

```
[ CameraX Capture ] ──> [ WebP Compression ] ──> [ Supabase Edge Function ]
                                                              │
                                                     (Google Gemini Flash)
                                                              │
                                                              ▼
[ Room Database ] <─── [ Quick-Adjust Sheet ] <─── [ Structured JSON Macros ]
        ▲
        │
[ Health Connect ] <─── Samsung Health / Google Fit (Daily Steps & Active Calories)
```

## 2. Layers

- **UI Layer (`feature/`)**: Jetpack Compose declarative UI following unidirectional data flow (MVI/MVVM).
- **Domain/Data Layer (`data/`)**: Repositories abstracting local Room DB and remote Supabase APIs.
- **Health Subsystem (`core/health/`)**: Health Connect client managing permission handshakes, availability states (Android 14+ vs 9-13), and aggregate time-window queries.
- **Edge Function (`supabase/functions/analyze-meal`)**: Deno Edge Function securing the Gemini 2.5 Flash API key, enforcing JSON schema output for food detection and portion estimation.
