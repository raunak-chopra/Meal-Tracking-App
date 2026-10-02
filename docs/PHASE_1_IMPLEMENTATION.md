# Phase 1: plan versus implementation

Date: 1 October 2026. Scope: personal-use reliability. See APP_UPGRADE_PLAN.md for the full roadmap.

| Plan item | Implementation | Verification |
| --- | --- | --- |
| P1.1 Reliable saves | Shared SaveOperation synchronously guards repeated taps, waits for persistence, propagates cancellation and exposes retryable errors. Used by manual/recent, photo, edit, barcode and workout saves, plus dashboard repeat logging. Manual completion lives in a ViewModel and is observed by the current screen. | Save-operation failure/retry/cancellation/duplicate tests; manual ViewModel completion/duplicate tests; workout save failure/retry tests; build. Phone interaction still pending. |
| P1.2 Input validation | Invalid goals keep Settings open with an explanatory message. Presets require valid calories. Workout saving rejects malformed/non-finite/negative input instead of silently substituting defaults. Bodyweight 0 is accepted. Explicit cardio mode hides strength fields and saves no strength sets. | Goal and workout validation tests; workout invalid-input test. |
| P1.3 Manual drafts | rememberSaveable stores title, search, timestamp and food items. Custom saver preserves IDs, portions and nutrition. Save state survives configuration changes through ManualMealViewModel. | Multi-item saver round trip and retained completion tests. Actual rotation/process recreation pending on phone. |
| P1.4 Health refresh | Dashboard observes resume, refreshes health data and follows today's date after a background midnight rollover. Date changes reset displayed health values. Previous health jobs are cancelled and obsolete responses rejected. Permission checks are inside error handling; cancellation is rethrown. | Code review and compilation. Actual Health Connect grant/revocation/resume/date behavior pending. |
| P1.5 Workout history | Exercise selection clears old sets; new entries no longer start with invented 60 kg sets. Request generation prevents old history from replacing another exercise or edits already made by the user. History failures permit manual entry. | Delayed-history tests for switching exercises and editing sets; correct-history prefill test. |
| P1.6 Backup integrity | Log tables export inside a single Room transaction. Decoder rejects duplicate IDs, unsupported old versions, invalid goals and invalid numeric/log values before the existing import path writes anything. | Backup round trip plus malformed/duplicate/invalid-value rejection tests. Transaction and device restore still need a real Room/device integration test. |
| P1.7 Review and checks | Focused regression tests, debug APK, unit-test/lint checks and full patch export. | Results below. |

## Verification results

- Ran `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline`: BUILD SUCCESSFUL.
- 71 unit tests passed; 0 failures/errors. This includes 14 new regression tests (original suite: 57).
- Lint completed with 0 errors, 51 warnings and 2 informational findings. Remaining warnings include existing dependency/platform, EXIF and resource/style issues; lint success does not mean all warnings were fixed.
- `git diff --check` and a whitespace check including new source/test files passed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- No phone/emulator run or real Gemini/Open Food Facts request was performed.

## Scope limits and remaining work

- This implements phase 1, not every upgrade in the roadmap. Phases 2–5 remain planned.
- No branding, colour palette, typography or visual redesign. Existing controls/styles are reused for errors, progress and the cardio switch. A few malformed text separators were normalized while converting mixed-encoding files to UTF-8.
- No database schema change and no existing user data migration is needed for this phase.
- Photo/edit/barcode/workout drafts remain in-memory; full process-death recovery is not implemented. Manual saved state also does not promise recovery after force-stop/uninstall or an exactly-once commit across process death.
- The log export is a consistent database snapshot. Goals still live in preferences and are captured separately; they do not share the Room transaction. Photos and AI/reminder settings remain outside the existing JSON backup.
- Import retains the existing merge-by-ID behavior. Stronger validation means previously malformed records may need correction before import. The import is not a full archive or a restore preview.
- Technical input ceilings prevent malformed/overflow values; they are not recommended health targets. Macro/calorie consistency and evidence-based target suggestions are later work.
- Device verification is still required for camera/barcode, real network requests, permissions, rotation during save, midnight behavior, widget refresh, import and upgrade with existing history.
- Existing PROJECT_STATUS_AND_ROADMAP.md predates the current local-only app and was left untouched.

## Next phase

Implement complete/partial day tracking and historical goals before adding stronger coaching. Ship a tested Room migration and extend backup coverage with those records. Then build the personal food library, recipes and favorites.

## Reviewing the changes

PHASE_1_IMPLEMENTATION.diff includes tracked source changes and new source/tests plus the plan/report. It excludes this patch file itself, build outputs and the pre-existing untracked roadmap. No commit or staging was performed.
