# Work status

## 2026-10-28 · Search repair and hygiene compiled; owner checking the app

**State:** local edits and debug APK compilation are complete. Recent meals searches names/ingredients before limiting the display and preserves same-title variants. Diary, saved library and common-food dialog use consistent word matching; library includes ingredients while preserving type/favorite filters. Search/clear/retry/no-match controls, draft-add Undo, earlier Recent shortcut and wrapping dialog rows are implemented. No schema/dependency/version change.

**Checks:** original two Recent meals regressions failed before the fix. Final `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --offline --no-daemon --max-workers=2` passed in 4m28s (80 tasks: 23 executed, 57 up-to-date). 191 JVM tests, 0 failures/errors/skips; lint 0 errors, 40 warnings, 5 informational items. Exact evidence (local review evidence excluded).

**APK:** `app/build/outputs/apk/debug/app-debug.apk`, 46,856,015 bytes; SHA-256 `7A38BEFD703F36DAAFDE0B4BAF999E4065777952B9DE931229835BC7CC0E9415`. All current source files predate this build. Debug/test APKs installed successfully on a disposable Android 36.1 emulator with 19 fictional meals and three library entries. Native entry/chooser/picker rendering passed; search/add/Undo/save/other-surface interaction checks are not completed. Startup System UI ANR and unstable automated input interrupted the device run.

**Owner steering:** “I will check the app. You edit and compile the APK.” Automated device interaction stopped. Do not resume native checks or mark them passed on continuation. Next step: owner checks this APK and reports any remaining search/UI issue. Preserve records and unrelated work; no commit/push/PR/release. The separate Room 6 / backup 3 plan still awaits Tier 2 approval. Whole-tree whitespace check still reports the four previously recorded EOF blank lines; those were preserved.

## 2026-10-22 · Approved trust-first implementation, unreleased

**State:** owner approved Everyday Bowl with Gemini's useful shortcuts. Local trust fixes, stale-name nutrition review, explicit Progress completion and barcode amount/layout checks are implemented and verified. The next concrete data migration plan awaits its Tier 2 approval. [Checkpoint and continuation instructions](implementation-2026-10-22.md).

- Fixed invalid visible portions saving previous values, malformed AI result omission, missing AI preflight, hidden photo assumptions and meal-reminder rule mismatch. Automatic goals now require adult eligibility and share the existing formula floor between initial/later suggestions; broader eligibility/clinical review remains unresolved.
- Verified: 173 unit tests passed, lint 0 errors/49 warnings, debug/test APK builds and updated IntegrationRunner passed. Native invalid `0` disables Save; corrected `200 g` saves the displayed 331 kcal. Keyless search/save and rename/keep/save/reopen work. Actual evidence and limits (local review evidence excluded).
- Progress completion/reopen/restart/error checks passed against real Room, with native count/average changes confirmed. No automatic completion from meals or calories. New controls are reachable at 200% font; existing bottom-navigation wrapping remains unresolved.
- Final native barcode check passed: confirmed fixture with 0 g cannot save; 50 g saves one displayed 279 kcal entry. The final full build passed in 7m05s; updated integration also covers completion while switching to a 30-day range.
- A single hourly continuation heartbeat is active for interrupted unfinished authorized work. Extensive owner changes remain preserved. No schema/dependency/public-backup/version change or external publication.

**Next step:** approve the [concrete missing-nutrient/usual-meal/optional-slot plan](data-integrity-plan-2026-10-22.md), then implement and verify it, followed by the required fresh-context review. Missing nutrient representation and broader navigation/accessibility/release recommendations remain open.

## 2026-10-22 · Product, UI/UX and technical audit

**State at audit completion:** audit and prioritized recommendations (local review evidence excluded) completed for owner review; three visual directions and interactive mockups (local review evidence excluded) and phased roadmap (local review evidence excluded) proposed. Subsequent approval and implementation are recorded above.

- Recommended: Everyday Bowl, a meal-first diary with unified logging, household servings, transparent uncertainty and optional goals/activity. Findings distinguish source/runtime evidence from hypotheses and research needs.
- Verified: fresh 160 unit tests passed; lint 0 errors/50 warnings; debug and instrumentation APK builds passed; existing persistence IntegrationRunner passed on disposable Android 36.1 emulator. Prototype interactions/responsive checks passed. Exact evidence and limitations (local review evidence excluded).
- High-priority native defect reproduced: invalid visible zero portion can still save the last valid amount. Other trust issues include unknown nutrients represented as zero, partial AI results, stale food corrections and automatic-goal eligibility/policy inconsistencies. No app fix implemented.
- Initial emulator ANR observed during slow boot/host contention; later launch worked. Cause remains unverified. No native accessibility, complete feature matrix, live AI quality, release R8 or long-history profiling clearance is claimed.
- Pre-existing app/design changes preserved. Audit adds dated docs/design/evidence plus this status update; no app source/dependency/schema/version changes, commit, push, PR or publication.

**Next step:** owner reviews primary audience, Direction A, Today/Library/Progress with optional Activity, and trust-first phases. Confirm separate concrete Tier 2 plans for schema/build/dependency/large-refactor implementation; exact Tier 3 approval remains required for any external action.

## 2026-10-15 · Bowlful UI/UX refresh

**State:** fresh interactive design concept ready for review; Android implementation awaits owner confirmation of the proposed broad UI plan under Engineer Tier 2.

- Deliverable: Everyday journal concept (local review evidence excluded), design/handoff (local review evidence excluded), verification (local review evidence excluded).
- Based on product capabilities in README. Current screens, UI source and historical visual-design documents were not inspected.
- Uses two fonts from the owner's Envato library; local asset provenance and app-embedding limitation are in ASSETS.md (local review evidence excluded).
- Existing repository work was already substantially modified. This study adds a separate design directory and these status documents; it does not alter Android application files or those prior changes.
- Browser concept checks cover compact and wide layouts and primary interactions. No native build, emulator, TalkBack or Android behavior validation was performed because the Android app was not changed.

**Blockers before native completion:** owner direction/plan confirmation; font app-embedding rights; Android feature-parity inspection and implementation; native testing and fresh-context review.

**Next step:** owner reviews the concept; on approval, implement the staged plan in `DESIGN.md` while preserving all tracking/data capabilities and unrelated local changes.

No app version bump, commit, push, PR, merge, publication or release was performed.
