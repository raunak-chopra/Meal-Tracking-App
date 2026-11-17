# Next data-integrity tranche — approval required

22 October 2026. **Proposed, not implemented.** This is the concrete Tier 2 plan for audit R04 and the approved shortcut direction. The verified local trust fixes and Progress controls do not require this migration.

## Outcome

Missing nutrition stays visibly unknown from barcode parsing through saved meals, summaries, trends, widgets and backup restore. “Save as usual meal” near a successful save preserves each item's amount, unit and nutrient snapshot. Optional meal labels remain editable and unassigned by default. No household measure is given a universal gram conversion.

## Current constraints verified in source

- Room is version 5. `FoodItemEntity` supports g/ml, but numeric nutrient fields cannot distinguish missing data from zero. Meal totals are cached numeric fields.
- `ScannedFoodProduct` defaults missing macros to zero. Barcode cache payloads serialize this model.
- `Ingredient` has a grams-only amount. `PersonalFood.portion()` creates gram rows, and template creation sums quantities as grams. Reusing that path unchanged would corrupt mixed g/ml meals.
- Public backup format is version 2; its decoder rejects newer versions. Both item and cached total fields need metadata to preserve meaning through restore.

## Proposed implementation boundaries

One additive **Room 5→6** migration and **backup 2→3** contract, delivered in small verified steps:

1. Add an availability bitmask to food and saved-food records, with explicit bits for energy/protein/carbohydrate/fat; add source metadata (`LEGACY`, `CATALOG`, `LABEL`, `AI_PHOTO`, `AI_TEXT`, `MANUAL`). Availability means a value was supplied, not that its accuracy was independently verified. A supplied numeric zero stays known zero. A missing field never sets its bit. AI values remain labeled estimates.
2. Preserve every old number, record ID, timestamp, image reference and relationship. For legacy data, keep recorded energy and positive macro values; label zero macros as unverified until the user confirms them. Past zero values cannot reliably be classified after the fact. Do not silently reconstruct them from a current food catalog or a new AI estimate.
3. Add availability-aware totals alongside existing numeric storage. When any item lacks a nutrient, display “X recorded · N items missing,” or “unknown” if no value is available. Keep estimate labels visible; recorded estimates are not a proven lower bound on actual intake. Do not fill a ring or claim a protein hit using an incomplete total. Day completion confirms logging coverage, independently of nutrient availability. Goal advice and AI summaries must disclose/exclude insufficient nutrient evidence as appropriate.
4. Add item units/availability/source to serialized `Ingredient` snapshots. Add a serving basis to `SavedFoodEntity`: new usual-meal templates use a yield of **one meal**, scale each item by meal count, and retain separate g/ml quantities. Existing gram-based templates and recipes keep their old basis and values. Recipe cooked yield remains a real measured weight. No implicit g↔ml conversion.
5. Place “Save as usual meal” after successful logging with a name, item preview and optional favorite. This saves a library snapshot independently of the diary record and cannot duplicate that meal. Repeating opens the shared review; logging writes fresh IDs and offers Undo. Test a rice + milk template specifically.
6. Add nullable `mealSlot` to meals and their backup/draft/repeat contracts. Empty means unassigned. Offer optional suggestions and editable labels with a reasonable length limit; preserve timestamps for history. A slot never creates a meal, asserts completion, or changes nutrition. Existing time-based grouping remains the fallback for old/unassigned records.

Affected paths: `core/database/{KaloDatabase,Migrations,entity/MealEntity,entity/PersonalEntities}.kt`, exported schema, `core/data/{food/PersonalFood,repository/MealRepository,backup/BackupCodec,backup/BackupManager}.kt`, barcode/AI models and parsers, shared meal review/manual/edit/save flows, dashboard/Meals/Progress/widget summaries, library UI and focused tests. No new dependency, target-SDK/signing change, service, analytics, key use or publication is included.

## Compatibility and rollback

- Export format 3 includes the new fields; old apps reject it explicitly through their existing newer-version guard. The updated app accepts formats 1/2, applies the documented legacy policy, and validates all fields before writing. Archive/photos remain intact; cache records also retain availability metadata.
- Migration only adds columns and defaults; no destructive Room fallback. Verify migration from each available schema through 6 with real Room validation, row counts, IDs, numeric values, image references and foreign-key checks.
- Before using a migrated personal installation, require a verified complete pre-migration archive. Local development continues on disposable data. Source rollback is scoped to this tranche; installing an older app over schema 6 is not a safe database rollback. Recover using the pre-migration archive in a compatible installation. Do not reset/delete an existing personal database.

## Required evidence before acceptance

- Parser tests: missing versus supplied zero, mixed availability, invalid/nonfinite values, cached/offline payloads, unsupported preparation basis.
- Totals/UI tests: missing protein cannot look like zero or a complete protein hit; complete logging with unknown nutrients remains visibly qualified; partial evidence does not drive unqualified advice.
- Persistence tests: migration chains, backup 1/2 import and 3 archive round-trip/merge, rejected future/malformed versions, atomic failed restore, mixed g/ml usual-meal round-trip, repeat IDs/Undo, optional slot edit/restart/restore.
- Native keyless manual/quick/barcode review, template save/repeat/edit/Undo, completion/availability interaction, enlarged text and TalkBack traversal. Build, relevant JVM/instrumented checks and lint; record gaps rather than claiming all-device readiness.
- Fresh-context review after verification, as required by Engineer's Tier 2 policy. Fix blocking findings before marking this tranche complete.

Approval requested: implement this local Room 6 / backup 3 tranche with the legacy-zero policy above. Approval would cover these concrete data/UI changes and their verification/review, not push, PR, deployment or release. Until approval, continuation must stay with independent Tier 1 work and must not apply this migration.
