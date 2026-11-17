# Changelog

Versioned entries describe shipped changes. Add upcoming work under Unreleased;
move it into a dated version section when upgrading the app.

## [Unreleased]

- Repair Recent meals search: search titles and ingredients before the display limit, retain same-name
  variants, show loading/retry/no-match/clear states, and confirm draft additions with Undo. Move Recent meals
  higher in the Add meal chooser; preserve historical portions, units and nutrient snapshots when copying.
- Align diary, library and common-food matching for case, spaces, punctuation and reordered query words.
  Saved recipes/templates are also searchable by ingredients, with type/favorite filters preserved.
  Add keyboard Search/clear controls and wrapping common-food dialog rows with 48dp targets.

- Review and reopen explicitly completed past days directly in Progress. Completion controls use
  existing storage and never infer completeness from meal counts or calorie totals.
- Renaming a food now blocks saving until nutrition is updated, manually edited, or explicitly kept.
  This review state survives photo-draft recovery. Incompatible g/ml re-estimates fail visibly.
- Trust-first input repair: photo review, saved-meal editing and barcode logging keep raw amount
  input in screen state and block saving invalid visible values; unfinished photo drafts retain that input.
  Malformed AI meals fail explicitly instead of silently omitting foods. Photo estimates show assumptions.
- Photo/text AI setup is checked before requesting estimates. Search remains available without a key.
  Meal reminders honor explicit completion and the fewer-than-two-meals rule, independent of exercise.
- Automatic target suggestions require an adult profile; older stored profiles remain readable. Later
  target checks use the same formula lower limit and report the actual change. These guardrails do not
  establish clinical suitability for all adults; manual journaling/targets remain available.

- Make description logging visible in the Add meal menu and explain restaurant/portion prompts.
  Photo and saved-meal reviews offer Update nutrition after correcting a food name. Pending
  estimates cannot be saved prematurely or overwrite newer food edits.

- Describe a meal in words on Log meal ("chicken bharta, 2 rotis") and Gemini adds the foods with approximate
  nutrition, using your dish names as written. A misread photo food (or a saved meal's food) can be renamed and
  re-estimated from its name, and photo correction notes now override what the photo seems to show.

- Rebrand: Kalo is now Bowlful. New name, bowl-and-dot logo, adaptive/themed/notification icons, widget preview and
  "Turmeric & Chai" light and dark palette (water and steps now have their own colours). The package name, database,
  preferences and backup file identifiers are unchanged so existing installs and backups keep working.

- Optional backup Gemini key, tried when the main key is rejected, rate-limited or offline.
- Photo results show each food in grams or millilitres with the amount editable right
  after the photo; the unit is saved with the meal (additive Room version 5 migration).
- An unfinished photo meal no longer blocks the camera; it waits as a Resume chip.
- "About you" in Settings: sex, age, height, weight and activity suggest calorie,
  macro and water targets (Mifflin-St Jeor); nothing changes until "Use these targets".
  Today shows a prompt until the details are filled in; backups include them.
- Item calories now round instead of truncating; invalid amounts are flagged instead of
  silently keeping the old value.
- Settings targets: calories now follow protein/carbs/fat (4/4/9 kcal per gram) and a
  new calorie target rescales macros at the current split; a goal change no longer
  clears unsaved target edits.
- Logic fixes: Today no longer opens future days; macro totals round; deleted meals'
  photos are removed once undo expires; Progress shows real patterns from complete
  days; applying a suggested target keeps protein; weigh-ins update the body weight
  behind targets; workout energy uses body weight and time and never overwrites an
  edited value; barcode drinks use ml and a typed amount; catalog/barcode calories
  round; quick entry rejects impossible macros; nutrition edits flag macros that don't
  add up; Search drafts allow changing an added food's amount.
- Brand pass (Warm Precision): sentence-case headings instead of all-caps labels,
  plain button wording, one quiet "Based on your log" card instead of coaching jargon
  or exercise-as-compensation advice, meal rows with time, source and an overflow
  menu, workout rows led by the session, calm error colours separate from macro
  colours, camera framing hint, and a lock-screen-safe reminder.
- Calmer photo review: estimate warnings removed and plainer wording throughout.
- Personal foods, favorites, editable recipes/templates and nutrition corrections.
- Photo-inclusive backup archives, scheduled dated backups and restore previews.
- Multi-exercise workout routines, short daily fitness logs and habit summaries.
- Photo-meal draft recovery, portion shortcuts and approximate energy displays.
- Updated appearance, navigation and validation for food and workout forms.
- Bundled 100-product Indian snack barcode catalog with attribution, confirmation
  gates and safe handling of incomplete nutrition.
- Additive Room version 4 migration and expanded regression/integration checks.

## [1.0.0] - Baseline

- Local Android meal, workout, water and progress tracking.
- Optional Gemini meal analysis and Open Food Facts barcode lookups.
- Room persistence, data export and Health Connect integration.

This documents the existing version number; it does not certify the uncommitted
development changes as tested or released.
