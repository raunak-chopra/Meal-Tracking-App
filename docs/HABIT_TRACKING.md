# Everyday meal and daily fitness logging

Date: 2 October 2026. Scope: personal-use Kalo; approximate estimates and low-effort habit tracking.

## Implemented

- Photo review leads with approximate calories, food names, and Save. Grams/macros and manual corrections are optional expandable details. Smaller/larger actions adjust the current portion by 25%, preserving estimated nutrient density; whole-meal adjustments also scale extra cooking fat.
- AI prompt estimates plausible preparation, sauces and cooking fats within the dish, states assumptions, and accepts everyday quantity/food corrections through the existing optional note and re-analysis. No weighing requirement. Real model accuracy has not been evaluated.
- Optional extra oil or butter has separate nutrition and teaspoon/tablespoon choices. Copy warns that these extras should only account for something missed by the AI.
- Photo requests use cancellation/generation guards; gallery reading is bounded at 25 MB and runs off the UI thread. Superseded gallery files are cleaned after processing. Saving blocks reset, replacement photo capture, retry, discard and back navigation.
- One unfinished photo meal persists in app-private no-backup storage, with atomic writes. Resume/discard preserves date, item corrections and a stable meal ID. An existing committed ID suppresses recovery after an interrupted save. Draft writes are best effort; explicit recovery messages cover storage/missing-photo failures. Immediate process death before a write completes may lose the most recent edit. Drafts are excluded from exports and Android backup; delete-all clears the draft file. Photo files remain in the existing private meal-photo folder.
- Search/recent meal drafts have whole-meal smaller/larger shortcuts.
- New daily fitness form defaults to 7 minutes and 20 reps each of push-ups, sit-ups and crunches. Duration presets 5/7/10, easy/moderate/hard effort, editable reps and zero-to-skip. Body weight is entered once and remembered after a successful save. Last successful choices can be repeated with one Save. Detailed workouts remain available as an explicitly separate flow; quick fields survive switching back.
- Fitness uses the existing workout storage and backups, with total time allocated once across included exercises. Approximate gross energy includes resting energy and stays separate from food intake. Effort, pace and breaks are uncertain. Reps are not treated as a precise calorie measure.
- Progress leads with meal-recording days, fitness sessions, minutes and completed reps for the selected 7/30-day range. Existing complete-past-day nutrition averages and historical goal evidence rules are retained.

## Sources and reuse

2024 Adult Compendium conditioning exercise entries 02020 (vigorous calisthenics, 7.5 MET), 02022 (moderate, 3.8 MET), 02024 (light, 2.8 MET): https://pacompendium.com/conditioning-exercise/ . Intended adult reference population 19–59. Estimate = MET × 3.5 × kg / 200 × minutes, rounded per exercise. Moderate preset preserves the earlier crunch-specific 2.8 MET assumption; easy uses 2.8 and hard uses 7.5.

Researched on 2 October 2026: yuhonas/free-exercise-db (repository declares Unlicense), simonalveteg/Workout-Tracker (no root license confirmed), wger-project/wger (AGPL application, entry-specific CC content). No external code, exercise images, or datasets were imported; no new dependency/backend/account added. Source/asset licensing must be reviewed at a pinned revision before any future reuse.

## Verification and acceptance

State: Verified. Independent reviewer found decimal editing, gallery ingestion and quick/detail switching issues; all repaired. A second review found save/discard race; ViewModel/UI guards and focused regression were added. Final source re-review found no remaining blockers. Final checks passed. Independent root acceptance pending the final evidence review.

Final command: `gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --offline --max-workers=1`. Final rerun passed: **111 tests, 0 failures/errors/skips; debug APK and instrumentation APK built; lint 0 errors / 45 warnings**. The final optional gram-display synchronization was included in this rerun. No new lint warnings were introduced. An intermediate full unit/build/lint run passed; two initial oil tests caught truncation and were repaired. A new test teardown initially failed due to coroutine cancellation after resetting Main; corrected by draining cleanup before resetting the dispatcher.

Focused tests cover portion proportions/bounds, oil versus butter, draft serialization/recovery/committed-ID suppression, duplicate saves, obsolete AI results, save/discard race, skipped fitness exercises, intensity/weight scaling, aggregate persistence, selected-date boundaries and completed-only reps. Instrumented runner includes an isolated actual AtomicFile round-trip/clear plus the previous disposable database/archive checks.

Limitations: no personal phone attached. Live Gemini, physical camera/gallery/barcode, TalkBack, OEM battery policies, and real-meal estimation quality remain unverified. The attempted disposable read-only API 36.1 emulator did not finish booting during build contention; no installation or device-check pass is claimed. The expanded instrumentation runner compiled but was not executed. No personal installation, production signing, publishing, or existing user data was changed. Existing barcode/edit/workout draft process-death limits remain; this change adds photo-meal recovery only.

Deferred: natural-language fitness interpretation, automatic rep counting, exercise-image catalog, standalone session timer, expanded household-serving catalog, new onboarding. Existing rest timer remains in detailed workouts. These are optional subsequent enhancements; this implementation focuses on easy photo estimates and short daily session logs.

The incremental patch in HABIT_TRACKING.diff compares only this task's affected files against the preserved pre-task working tree; it does not attribute pre-existing changes to this task. No files staged or committed.
