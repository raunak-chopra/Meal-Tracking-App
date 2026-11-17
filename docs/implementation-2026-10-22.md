> 28 October steering: search edits and APK compilation are complete; the owner will check the app. Follow the search checkpoint (local review evidence excluded) and do not resume automated native testing unless requested. This work does not approve the pending Room 6 / backup 3 plan.

# Approved Bowlful implementation checkpoint

22 October 2026. Owner approved proceeding with the updated Everyday Bowl direction and Gemini's useful shortcut ideas, retaining evidence standards and the trust-first sequence. Owner also requested continuation scheduling after interruption/token exhaustion.

## Scope and order

1. Save correctness: invalid visible portions must block UI and ViewModel persistence in photo review, saved-meal editing and barcode logging. Preserve raw input and corrections across recomposition/draft recovery; no silent clamping/substitution.
2. Trust quick wins: show estimate assumptions/partial-result warnings; preflight unconfigured AI; reminders match their declared meal-only rule and respect explicit completion.
3. Safe optional goals and consistent historical comparisons; missing nutrients remain unknown through a separately reviewed model/backup/migration plan.
4. Meal-first Today and shared logging; optional editable meal slots, Save as usual meal near successful logging, accessible completion controls directly in Progress. Preserve existing capabilities, records and fitness access. Never infer complete intake from meal count/calorie attainment; household units are food/recipe-specific.

The owner approved product direction, not publishing or blanket external actions. Future dependency/schema/build/large-refactor changes require a concrete Tier 2 plan/review as recorded in the audit roadmap. No such change is needed for the initial input fix. No version bump until a completed approved app upgrade.

## Current work

Status: this local trust/Progress tranche is implemented and verified. The next data-integrity tranche awaits its concrete Tier 2 approval. Baseline: 160 JVM tests passed. Extensive owner changes already exist; preserve them.

Implemented and verified: ViewModel-owned raw amount validity in shared photo/edit and barcode controls; UI and persistence guards; backward-compatible raw-input draft field; malformed/oversized AI result rejection instead of silent omission; visible photo assumptions; AI key preflight; explicit-completion-aware meal-only reminders. Automatic target suggestions now reject under-18 profiles while old stored profiles remain readable; later target checks use the same existing formula floor and report the actual applied change. This is an eligibility/consistency stopgap, not clinical clearance for all adults.

Room records/schema, dependencies and public backup format stay unchanged. Latest full checks passed: 173 JVM tests, 0 failures/errors/skips; lint 0 errors/49 warnings; debug and test APK builds. Updated IntegrationRunner passed on the disposable Android 36.1 emulator. Native zero-portion repro now blocks saving; correcting to 200 g saves the displayed 331 kcal. Keyless Photo preflight and local search/save were exercised. See implementation evidence (local review evidence excluded).

## Additional completed behavior

Progress has an expandable past-day list with explicit checkboxes and complete/partial descriptions. It uses existing day-status storage, excludes today from averages, permits reopening, and reports failed writes without claiming completion. Real-Room instrumentation passed for two meals above the calorie target staying partial, explicit completion, restart persistence, reopening, invalid dates/empty days and a failed write. Native completion/reopen updates the count and average correctly. Controls remain readable/reachable at 200% system font, although existing bottom-navigation labels wrap awkwardly; full native accessibility clearance remains pending.

Renamed foods retain their old nutrition basis and block UI/ViewModel saving until an estimate, manual correction or explicit “Keep these values.” Photo drafts preserve this review state. Mixed g/ml single-food re-estimates fail visibly instead of combining quantities; invalid raw/numeric mismatches cannot save. Native rename/keep/save/reopen and final-build zero blocking passed. Historical stale names/unknown zeros cannot be reconstructed automatically.

Barcode amount controls stack the field and 48dp buttons. Native bundled-record testing passed: packet confirmation was simulated for the fixture; visible 0 g disabled Save and hid totals, correcting to 50 g enabled 279 kcal, and saving produced exactly one 279 kcal diary entry. Measured adjustment targets are at least 126×126 px at density 420 (48×48 dp). This does not verify a real packet's nutrition accuracy.

Self-review routed completion updates through the existing cancellable Progress refresh. The final build/lint/173-test run passed in 7m05s (80 tasks, 22 executed/58 up-to-date); updated IntegrationRunner passed against the final debug/test APKs, including completion followed by a switch to 30 days. Native evidence uses only disposable data; no personal installation was changed.

The disposable read-only emulator was stopped after verification. Artifact/document links resolve. Existing unrelated trailing-EOF whitespace findings were recorded rather than cleaned up; see the evidence ledger and machine-readable summary.

## Continuation

Read this file and docs/work-status.md before resuming. Do not infer completion from filenames or plans. Continue the next unfinished authorized item; no concurrent implementation of the same files. One hourly thread heartbeat exists: `continue-bowlful-implementation`, ACTIVE. It stays quiet while unchanged/non-actionable and must respect pending approval. A prior usage-limit interruption prevented one checkpoint write; after the owner requested continuation, it was reconciled from recorded results. If limits block work again, checkpoint once if permitted and wait without repeated retries or bypasses.

Next concrete step: obtain the single Tier 2 approval for the [Room 6 / backup 3 data-integrity and shortcut plan](data-integrity-plan-2026-10-22.md), then implement, verify and get the required fresh-context review. Do not apply it before approval; existing product-direction approval does not by itself waive the concrete schema gate. The plan preserves old numbers, qualifies legacy zero macros, makes usual meals preserve per-item units, and leaves meal labels optional. Other audit recommendations remain open; this checkpoint is not full-app/release completion. No publication or version upgrade has occurred.
