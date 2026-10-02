# Phases 2–5: plan versus implementation

Date: 2 October 2026. Personal use, local primary storage, no accounts. Phase 1 and existing user changes are retained. Branding, colours, typography and visual redesign remain deferred.

## Plan comparison

| Phase / plan | Implemented behavior | Evidence / remaining acceptance |
| --- | --- | --- |
| 2: complete / partial days | Dashboard persists an explicit complete-day switch for the selected date. Trends show partial bars and exclude today, partial and missing meal days from averages and the complete-day streak. Missing days are not treated as zero intake. | CompleteDayTest; Room schema/migration tests. Explicit completion is the owner's assertion, not automatic proof that all intake was logged. |
| 2: historical goals | Effective-date calorie, macro, water and goal history is durably recorded. Daily hits and chart target segments use that day's recorded goal. Dates before the first recorded goal remain unknown. Saving unchanged goals does not advance the history date. | Room v4 export; backup round trip; integration runner checks persistence across repository recreation. History uses one effective record per local calendar day. |
| 2: evidence-gated coaching | Calorie checks require at least ten complete meal days under a consistent recorded goal, at least 70% of intake within 15% of the target, and at least three weight measurements spanning ten days within that evidence window. Suggestions still require an explicit Apply action. AI summaries use complete days. | CompleteDayTest and existing weight/advisor tests. Suggestions remain approximate heuristics; day completion does not establish measured intake accuracy. |
| 2: weight graph | Existing graph retained; horizontal spacing now follows actual timestamps instead of equally spacing irregular measurements. | Kotlin compilation; visual interaction acceptance remains a device check. |
| 3: foods / label values / favorites | Local library stores label values for a stated serving weight, supports editing/deleting and sorts favorites first. Log any positive gram portion at a selected time. | PersonalFoodTest; backup coverage for saved_foods. |
| 3: editable templates | Create a template from a recent meal or enter ingredients; edit/add/remove ingredients, save a named template and log scaled individual foods with new IDs. | Template portions/fresh IDs unit test. Ingredient edits retain the original draft row until Update is pressed. |
| 3: recipes / cooked yield | Store ingredient nutrition totals against the measured cooked batch yield. A portion scales batch nutrition by portion grams / cooked yield. | Recipe portion calculation and invalid-yield tests. Cooking losses are not inferred; the owner supplies the yield and label values. |
| 3: quick logging / corrections | Manual entry accepts kcal/protein estimates; photo review and meal editing allow direct kcal/protein/carbs/fat correction for the displayed portion, retaining per-gram scaling. | Build plus existing item/saver tests. Quick entries visibly disclose unknown carbs/fat recorded as zero and use a one-entry portion. |
| 3: barcode caching | Successful Open Food Facts labels persist locally. Repeat lookups use cache without network; explicit Refresh requests fresh data and falls back to cached data on I/O failure. Invalid label values are rejected. | Parser tests and instrumented cache-hit check. Unknown uncached barcodes still need network. No real API request was used for verification. |
| 4: multi-exercise workouts | Queue multiple strength/cardio exercises, retain per-exercise sets/time/burn, name a session and aggregate totals once. | New ViewModel test checks mixed-session totals and incomplete sets; validation tests. Burn estimates are approximate. |
| 4: routines / editing / completion | Save/load/delete local routines; open an existing workout for editing; transactionally replace its sets; persist completion flags. Legacy workouts remain editable. | ViewModel regression tests; instrumented replacement check. Routine loading replaces the current draft; saving a routine does not itself log a workout. |
| 4: optional rest timer | Start/stop a 90-second timer using a monotonic deadline in the workout ViewModel. | Build. Timer is foreground screen feedback, without a notification; process death ends it. |
| 4: complete photo archives | ZIP contains versioned backup.json and meal photo bytes with deterministic safe entry names. Missing referenced photos fail export rather than silently producing an incomplete archive. All new records and non-secret settings are included. | ArchiveCodecTest; instrumented photo/record round trip. JSON remains data-only and preserves an existing matching meal's photo during import. |
| 4: scheduled dated backups | Persist a user-chosen folder grant; optional daily WorkManager job writes a new dated ZIP. Last success/error is shown. Failed automatic exports attempt to remove their partial file. Older backups are retained. | Build; WorkManager/SAF device checks listed below. Scheduling is approximate and battery dependent, not an exact midnight alarm. |
| 4: restore preview | Parse and validate JSON/ZIP without modifying the database, show record/photo counts and merge effects, then import only after the owner confirms. Matching IDs update; unrelated local records remain. Goals/non-secret settings may change. | Decoder/archive tests; instrumented ID merge/photo preservation. The preview is held in memory; recreation may require choosing the file again. |
| 5: selective maintenance | Replaced platform EXIF with explicitly declared cached AndroidX EXIF 1.3.7; removed broad gallery permissions and startup camera prompt; added explicit cloud/transfer exclusions. Existing dependency versions otherwise retained. | Debug build and lint. No broad toolchain/platform upgrade during the functional repair. |
| 5: verification | JVM regression tests, exported-schema comparison, APK builds, lint and a dependency-free Android instrumentation runner. | Results below. Physical-phone acceptance remains pending. |

## Storage, migrations and backup contract

- Room version 3 → 4 is additive: day_status, goal_history, saved_foods, barcode_cache, workout_routines and workouts.exercisesJson. Version 2 → 3 remains available. No destructive fallback was added.
- Migration tests seed the old log tables, check retained records, compare migrated columns/indexes/foreign keys against the exported v4 schema, and check cascades. The instrumented runner additionally asks actual Room to open and validate a v3 database after migration; its execution status is recorded below.
- Backup version 2 includes day status, goal history, food/recipe/template records, favorites, ingredient payloads, barcode cache, routines, multi-exercise payloads, set completion and non-secret AI/reminder preferences. Version 1 JSON files remain readable.
- ZIP paths cannot escape the manifest namespace; duplicate entries, unmatched/missing photos and invalid record values are rejected before database writes. Limits: 30 MiB JSON manifest, 20 MiB per photo, 200 MiB total uncompressed archive. Larger libraries need a future streaming/chunked archive implementation.
- Database import is transactional. Profile/settings preferences are applied after the database transaction; a preference write failure can therefore leave the database restored with an error reported. Room and SharedPreferences do not share one transaction. Referenced restored photos are retained in that case.
- Goal history is durably held in private preferences and mirrored into Room. Imported dates merge by date. Earlier unknown goals are not invented. Current targets/API key remain after Delete all; logs, library, history and cache are cleared, with a new current-goal baseline. Previously exported backup files are not deleted.
- API keys and device-specific backup folder grants are excluded. Primary records remain local; optional Gemini calls and Open Food Facts lookups remain explicit existing features. A selected cloud-backed document folder may sync archives through its provider.
- Old replaced photos can remain as safe orphan files until Delete all. Restore does not delete unrelated photos or logs. Other existing draft/process-death limits from phase 1 remain; no general autosave layer was introduced.

## Verification results

- Latest unit suite: **88 tests, zero failures/errors/skips** (phase 1 baseline: 71).
- `gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --offline --max-workers=2`: successful build; final source build passed. A subsequent test/lint rerun covers the added coaching boundary cases.
- Lint: **0 errors, 46 warnings, 4 informational findings**. Existing dependency, deprecated icon and resource/platform warnings remain.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; instrumentation APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- `git diff --check` completed without whitespace errors. No staging, commit, signing for release, publication or production-phone installation was performed.

## Device acceptance and concrete blockers

Emulator integration execution passed; details are recorded below. A temporary read-only AVD is used so its original saved state is not overwritten. Its stale ADB authorization required restarting the local ADB server. The host has roughly 6 GiB RAM; concurrent emulator/build verification caused memory pressure, so build and device checks were sequenced.

Still unperformed on a physical phone:

- Camera/gallery/barcode capture and permission denial/revocation/re-grant; real Health Connect permission and resume behavior. No physical phone is connected and emulator data is not equivalent to real health data.
- Rotation/process recreation during actual save flows; background midnight/date rollover; home-screen widget refresh.
- Real storage-full/unwritable-device failure. Unit tests cover injected persistence failure/retry and duplicate save prevention, not an actually full phone filesystem.
- Chosen-folder SAF grant/revocation, background daily worker execution under OEM battery policy, and manual restore-preview confirmation interactions.
- Updating the owner's installed app with existing history/signing. The migration fixtures prove database behavior but do not establish production APK signing compatibility. Make a complete archive before a real phone update.
- Real Gemini and fresh Open Food Facts requests. No API key was used and no personal records were sent during verification.

Phase 5 is therefore **partially verified**, not a claim that every phone smoke test passed. No blocker prevents the implemented local features from being reviewed; the remaining acceptance checks require the owner's phone and real providers.

## Review artifacts

- `PHASES_2_5_IMPLEMENTATION.diff`: incremental changes against the captured phase 1 working-tree snapshot, including new files.
- `FULL_IMPLEMENTATION.diff`: all current implementation changes against Git HEAD, including phase 1 and all relevant untracked source/tests/docs.
- Both omit generated patch files, build outputs and the pre-existing untracked PROJECT_STATUS_AND_ROADMAP.md, which was left untouched. They are review artifacts; no Git staging was needed to include new files.
- The original phase 1 report/patch remains available as historical evidence. APP_UPGRADE_PLAN.md now points to this report for the current status.

## Platform references for maintenance

The permission and transfer exclusions follow Android's [permission minimization guidance](https://developer.android.com/privacy-and-security/minimize-permission-requests) and [Auto Backup rules documentation](https://developer.android.com/identity/data/autobackup). These platform references explain the maintenance choice; device-specific behavior remains subject to the acceptance checks above.

## Executed emulator checks

On the temporary read-only Medium_Phone_API_36.1 AVD:

- Installed both debug APKs after Android reported boot complete. Initial streamed install during boot failed without a useful error; non-streamed installation then succeeded.
- Executed `adb -s emulator-5554 shell am instrument -w com.kalotracker.app.test/com.kalotracker.app.IntegrationRunner`. It returned **PASS** for actual Room 3→4 schema validation/preserved logs, all-record/photo-byte restore, ID merge, offline cache-hit lookup, workout set replacement and goal history persistence across repository recreation. All fixture databases/preferences/photos are disposable.
- Cold launch returned Status: ok. The emulator first displayed a **System UI** non-response dialog under host memory pressure; choosing Wait exposed Kalo's dashboard. No AndroidRuntime crash was found during this smoke check. This is limited launch evidence, not an ANR/performance certification.
- Toggled the complete-day switch, force-stopped and relaunched Kalo: the UI still displayed “Day complete”.
- Opened Trends: complete past-day count 0/7, no fabricated average on an empty history, and historical-target/partial-bar labels were displayed.
- Opened More ways to log → Add food manually: My foods & recipes and Quick kcal / protein were present. Opened the library: label serving, favorites, nutrition fields, recipe/template selectors and save/log controls appeared.
- No actual camera capture, new-food UI save, full multi-exercise UI session, manual restore preview confirmation, SAF folder selection or background worker execution was performed. Their data paths have the automated/instrumented coverage described above; full interaction acceptance remains pending.

The AVD session is closed without saving its original state. The owner's physical phone and production app were not modified.

## Subsequent authorized branding and gap check

The owner subsequently authorized applying the supplied brand package. See [BRANDING_AND_GAP_CHECK.md](BRANDING_AND_GAP_CHECK.md) for the latest fixes, native light/dark and large-font verification, plan-versus-implemented mapping and current full diff. This phase report and its incremental patch retain their original phase-specific evidence.
