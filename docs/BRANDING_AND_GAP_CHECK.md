# Kalo: branding and second gap check

Date: 2 October 2026. Scope: personal-use Android app. Preserve phases 1–5 and existing local records; no accounts or backend. The owner explicitly authorized applying the supplied brand package after the earlier branding deferral.

## Brand source and decisions

Source: `docs/brand/`.

Reviewed strategy, UI/UX specification, implementation handoff, provenance, token files, HTML source, and rendered light/dark Today concepts. Applied the recommended **Warm Precision / Open Plate** route. Retained **Kalo**, the package ID, native system fonts and the existing database. This is a personal app; alternate-name exploration, public store material and publishing are outside this change.

Original supplied tokens and SVG are retained in `docs/brand/`. The adaptive launcher uses the supplied path geometry, including a monochrome resource. Generated food photos and fictional nutrition fixtures were not imported into the app or the user's logs.

## Double-check findings and resolution

| Finding | Resolution | Evidence |
|---|---|---|
| Past-day logging initialized a new entry with today's timestamp | Manual, photo, barcode, library and workout entry inherit the selected day; past days default to local noon and remain editable | LogDate unit test; factory/screen propagation; device check below |
| Dashboard used current goals for older days | Select the latest recorded goal effective on that date. Missing historical targets remain explicitly unavailable; suppress goal-based daily advice when unavailable | Source review; existing historical-goal/coaching tests; device check below |
| Energy UI could hide above-target intake behind a clamped remaining number | Show actual logged calories, target and true above/below delta. Clamp only the decorative arc | EnergyDisplay unit tests |
| Failed workout load could leave a default draft eligible to overwrite an existing session | A successful edit load is required before any write | Failed-load regression test |
| Barcode responses could return after rescan; refreshing could overlap saving | Cancel obsolete requests, reject stale generations, preserve coroutine cancellation, and block save during lookup | Source review; offline-cache integration coverage; live network races unperformed |
| Repeated library saves assigned fresh IDs | Keep the validated ID in the editor, so subsequent saves update the same entry | Source review; existing ID-merge integration coverage |
| Catalog portion input accepted negative/non-finite values or silently substituted a default | Require finite positive grams up to 5000; show invalid input and disable Add | Boundary/invalid-number regression test; final UI binding build check |
| Catalog rows reused another food's portion state after draft/list insertions | Stable food-ID lazy-list keys and saveable state keyed by food ID | Observed during native recent-meal test; source fix; final verification below |
| Recent-meal tap saved immediately | Add copied food items with new IDs to the editable draft; require the regular save action | Source review; device check below |
| Meal/recent-list read failures could escape without recovery | Handle load failures, preserve cancellation, keep save blocked until an original meal is available, and show a recovery message | Source review |
| Unsupported model-confidence percentages / accuracy range | Remove numeric confidence and 20–30% error claims. Clearly label photo estimates and review expectations | UI/source review |
| Steps-only settings change could reset nutrition goal evidence | Avoid recording a nutrition goal change for a steps-only edit | Source review |
| Widget retained old palette and camera-only shortcut | Apply paired brand colours, actual logged energy and factual delta; open the shared Add meal chooser; avoid fabricating a zero total on read failure | Build/source review; launcher-host verification remains pending |

Historical step targets are not stored in the current schema; the step card continues to use the current preference. Nutrition/water goal history is date-aware. A same-day goal change replaces that day's goal record, as before.

## Plan versus implemented branding

| Supplied direction | Implemented result |
|---|---|
| Warm charcoal/oat/paprika, paired light/dark tokens | Central semantic palettes and complete Material roles, including surfaces, error roles and input outlines |
| System/light/dark appearance | Settings preference, persisted locally, applied throughout Compose; exported/restored with backward-compatible defaults |
| Native typography, clear hierarchy and shapes | Supplied scale and line heights, radii, 56dp primary action and enlarged utility targets |
| Open Plate identity | Exact supplied mark paths on Today; adaptive and monochrome launcher resources |
| Meal-first Today | Date, actual energy, macro bars, meals, then water/steps/workouts and complete-day controls |
| Today / Meals / Progress navigation | Three root tabs, a persistent Add meal button, and selected-day state shared between Today and Meals |
| Add meal methods | Photo disclosure, barcode, Search/quick log, Recent review and personal library; Search and Recent share the native editable meal screen |
| Meal library | Search within the selected day's logs, editing, repeat/delete/undo, and access to personal foods/favorites/recipes |
| Large-font accommodation | Energy arc yields space at >=1.3x; macros and library nutrition fields stack; content scrolls |
| Candid source/privacy language | Photo estimate labels, Gemini photo disclosure, summary data disclosure, Open Food Facts source/cache details |
| Calm progress feedback | Above-target energy is factual, without alarm/error styling; complete/partial-day evidence rules retained |
| Widget | Brand palette, actual calories, factual delta and Add meal entry chooser |

The supplied HTML concepts predate the new favorites, recipes, multi-exercise workouts and photo archives. These features remain functional and themed. The older prototype's logged-day averages and photo-excluding backup copy were not applied: the app retains complete-day trends and full photo archives.

This is a native adaptation, not a pixel-for-pixel transplant of all 22 prototype screens. Separate onboarding, public store art, custom font licensing, a standalone weight screen and a new bottom-sheet architecture for every editor were not introduced. Existing utility screens inherit the brand system and retain their workflows.

## Verification

- Unit tests: **96 passed, 0 failures/errors/skips**. Includes energy truth, selected-day timestamps, failed-edit write prevention, text/action contrast in both palettes and appearance backup backward compatibility.
- Debug app and instrumentation APK builds: passed. The final catalog-state correction is rechecked below.
- Lint: no errors; final warning/info totals recorded below. Existing deprecation/maintenance warnings are not represented as fully resolved.
- Room remains version 4. No additional migration or destructive migration fallback was introduced by branding. Existing actual 3→4 migration and all-record/photo archive integration checks are rerun below.

### Final build/device evidence

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --offline --max-workers=1`: passed for the branded app; 95 unit tests, no lint errors. Final catalog-state recheck recorded below; the subsequent input guard brings the final test count to 96.
- Lint: **45 warnings, 5 informational items, 0 errors**. Existing maintenance warnings remain.
- Disposable read-only Medium_Phone_API_36.1 AVD installed both APKs. First instrumentation launch reported Process crashed while Android native services were failing and System UI was unresponsive under memory pressure. The crash buffer did not show a Kalo Java exception; choosing Wait allowed the app to display.
- The retry returned **PASS** for actual Room 3→4 migration, all-record/photo-byte restore, ID merge, offline cache lookup, workout set replacement and goal persistence. Appearance was included in the archive fixture and round-trip check.
- Native cold launch returned Status: ok. Light and dark Today screenshots were opened and visually inspected. Changing appearance through Settings changed the full app palette; dark mode persisted across force-stop/relaunch.
- Selected 1 October from Today. Manual logging opened at **Yesterday, 12:00**. Created a 100 kcal quick-entry fixture through the actual UI and confirmed the saved meal appeared on 1 October at 12:00, with unavailable historical targets labelled rather than today's goals.
- Meals tab retained the selected 1 October and showed the same saved record. Choosing Recent and tapping that record produced a **100 kcal draft** with the ordinary Save action; closing it returned without saving another record.
- Set Android font scale to **2.0**, force-stopped and relaunched. Today removed the decorative energy arc, stacked all macro summaries, kept the primary action and three tab labels visible, and allowed scrolling. Screenshot inspected. This does not verify all utility screens or TalkBack.
- Screenshot artifacts: `docs/brand/screenshots/today-light.png`, `today-dark.png`, `today-large-font.png`. They contain empty disposable Today state, not personal records.
- Emulator sessions are closed without saving the original AVD state. No physical phone or production installation was changed.

Final catalog-state recheck: **passed** on the rebuilt APK. The widget entry intent opened the shared chooser. Edited chicken to 175 g and added it: its catalog row retained 175 g, salmon retained 150 g, and the draft contained the correct 175 g / 288 kcal item. Filtering to egg showed Whole Large Egg at 50 g and Liquid Egg Whites at 100 g, without inheriting chicken's value. This second cold launch initially timed out behind a System UI non-response dialog; after Wait, native interactions worked. No performance certification is claimed.

A final input guard was added afterwards to reject blank, malformed, zero, negative, non-finite and >5000 g portions. Its boundary test and full build/lint verification passed (96 tests, no failures/errors/skips, debug and instrumentation APKs built, lint with 0 errors / 45 warnings / 5 informational items); that small invalid-input UI state was not separately re-exercised on a third emulator session.

## Remaining acceptance gaps

Physical phone camera/gallery/barcode capture, real Health Connect permissions/data, real Gemini and fresh Open Food Facts requests, SAF grants/revocation, background backup timing under OEM battery policy, launcher widget placement/refresh, TalkBack traversal and production signing compatibility remain unperformed. No physical phone is attached. Existing photo/barcode/edit/workout drafts remain in-memory across process death, as documented in phase 1; branding does not add recovery guarantees.

Large-font smoke checks are limited to the screens and scales explicitly recorded below; no claim of full native accessibility certification or exhaustive state coverage. Existing lint warnings and other phase-5 device acceptance items remain visible in PHASES_2_5_IMPLEMENTATION.md.

## Review artifacts

- `BRANDING_AND_GAP_CHECK.diff`: incremental patch against the pre-branding working-tree snapshot, including new source and documentation.
- `FULL_IMPLEMENTATION.diff`: full current changes against Git HEAD, including phase 1, phases 2–5 and this pass, with new files.
- Historical phase-specific patches remain unchanged. Patch files, build outputs and the pre-existing untracked PROJECT_STATUS_AND_ROADMAP.md are excluded. No files were staged or committed.
