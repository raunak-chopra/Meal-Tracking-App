## 2026-11-06 — Unreleased source preservation checkpoint

- Behavior: preserve the owner's Bowlful rebrand, meal/search/trust and progress work with existing Room5 schema/migration tests. No new app feature, dependency upgrade, Room6/backup3 migration, version bump or release is introduced by this checkpoint.
- Changed files: scoped app source/resources/tests/schema, branding/public documentation and release-name metadata captured in this commit. Four existing EOF blank-line defects are normalized only in this isolated copy; private review outputs and local configuration are excluded. Unshipped local evidence links are explicitly labelled.
- Reason: resume approved Git preservation without altering the owner's source or staging. Both sides of widget renames are preserved.
- Verification: 191 JVM tests passed, zero failures/errors/skips in the fresh isolated run; final lint and APK assembly status recorded in the chat workspace daily report. No emulator/device acceptance was run; owner phone acceptance remains pending.
- Rollback: revert this checkpoint on its feature branch; preserve the original working tree/index. No main merge, app release, personal-data reinstall or destructive migration is authorized.
# Version history

## 2026-10-28 · Search repair and hygiene, unreleased

Recent meals queries previously hid history and searched only the bundled catalog; history was also truncated to 12 distinct titles before filtering. The picker now searches titles/ingredients in the existing 200-record window, groups exact repeats while retaining variant snapshots, and supports loading/retry, no-match/clear, more results, review and draft-add Undo. Recent meals is higher in the chooser. Diary and library share tolerant literal word matching; saved-library ingredient names are indexed on library updates, preserving type/favorite filters. Shared common-food dialog controls and row wrapping improve recovery and large-text layout.

Files: `SearchText`, `FoodCatalog`, `FoodSearch`, `MealSearch`, manual screen/ViewModel/repository/navigation; Meals and PersonalFoods screens; shared food dialog and its open-state flags in camera/edit; search regressions and explicit empty-installation fictional integration fixtures. No schema, dependency, public backup or version-field change. Exact verification and limitations: search evidence (local review evidence excluded).

Rollback: reverse only this task's scoped search changes against its starting working tree, preserving prior owner edits. There is no data transformation to reverse; original meals, saved foods, units and nutrient snapshots remain unchanged. No reset, personal-data reinstall, commit, push, PR or release.


## 2026-10-22 · Trust-first fixes, unreleased

Owner approved the updated Everyday Bowl direction with optional meal slots, Save as usual meal and Progress completion controls, retaining the trust-first sequence. The local trust tranche repairs invalid visible amounts being saved as previous values; it also adds AI preflight/assumption visibility, rejects incomplete malformed estimate results, aligns meal reminders with their promise, and adds adult eligibility/shared existing-formula floor checks for automatic targets. Progress now exposes explicit past-day completion/reopening. Renamed foods require updated, edited or explicitly retained nutrition before saving, including recovered photo drafts; incompatible g/ml re-estimates fail visibly.

Files: meal review/edit ViewModels and shared row/draft code; barcode ViewModel/screen; AI service/models; manual screen/ViewModel; navigation; reminders/settings; TargetCalculator/GoalAdvisor/Progress state and UI; focused JVM regressions and real-Room IntegrationRunner checks. Barcode controls now preserve 48dp adjustment targets without crowding the field, and completion refresh shares the existing cancellation path with range changes. Detailed status and verification are in [implementation checkpoint](implementation-2026-10-22.md). Room schema/dependencies/public backup format/version fields are unchanged. Existing records and unrelated working changes are preserved.

Rollback: reverse only the scoped trust-fix changes against the task's working baseline, not the repository HEAD, which predates substantial owner work. Old readers ignore the optional photo-draft amount/review fields. Progress uses existing day-status rows, which remain readable after UI rollback. No database rollback, reset, reinstall over personal data or broad cleanup is required. No commit/push/PR/release performed. Unit-safe usual meals, optional slots and missing nutrients are in the [proposed data-integrity plan](data-integrity-plan-2026-10-22.md), awaiting its separate Tier 2 approval.

## 2026-10-15 · Design study, no Android version change

Created the independent Everyday journal UI/UX concept for owner review. It explores a new typography-led visual system, daily meal journal, explicit draft review, hydration, movement, date selection, completion status and progress presentation using fictional data.

Files: `design/refresh-2026-10-15/` (editable HTML/CSS/JavaScript, handoff, asset record, QA) and `docs/work-status.md`. Android behavior and application versions are unchanged. Existing unrelated changes were preserved.

Rollback: remove only this newly added concept directory and the new design-study documentation if the direction is rejected. Do not reset or restore the repository broadly; it contains pre-existing uncommitted work.
