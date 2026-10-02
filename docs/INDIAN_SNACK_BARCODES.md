# Indian snack barcode starter catalog

Date: 2026-10-02. State: Verified.

The barcode scanner now has a bundled catalog of 100 Indian-market snack/pack variants from Open Food Facts. Exact barcodes only; this is not a sales ranking. The catalog supplies local product matches without internet, while nutrition-label photo links need internet.

- 88 records have complete as-sold calorie/protein/carbohydrate/fat values. Before logging, users confirm that the product and nutrition match their packet.
- 12 records have incomplete or uncertain nutrition. They show a found-product explanation, label link and manual-entry action. Missing values are not replaced by zeros. Maggi's prepared-nutrition record is deliberately in this group.
- Explicit gram pack sizes provide the initial portion; ambiguous quantities default to an adjustable 100g. The product result card scrolls for smaller screens and larger text.
- Catalog rules apply before cache and remote lookup, including refresh requests. Catalog nutrition does not silently refresh into unreviewed remote values. Other barcodes retain the existing Open Food Facts lookup and cache behavior.
- Confirmation resets on a new scan or rescan; logging and product-selection callbacks are guarded against unchecked, saving or loading states.

## Source and maintenance

Bundled data: `app/src/main/assets/indian_snacks_100.json`. It includes per-record source and nutrition-image URLs, retrieval date, preparation basis, and attribution. Data is a community snapshot, not manufacturer-verified. Barcode check digits passed in the research catalog; this does not authenticate a barcode. Photos were not downloaded, visually verified or bundled.

Source: [Open Food Facts](https://world.openfoodfacts.org/). Database ODbL; individual contents DBCL; photo links refer to CC BY-SA images, with possible third-party rights. [Source/license documentation](https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/). [Image link format](https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/how-to-download-images/).

Updating the starter catalog should preserve exact packet barcode variants and missing values, review preparation basis against label photos, and run catalog tests. No database migration, paid provider, dependency change, publishing or installation is part of this change.

## Verification

Independent review found and repaired cache/refresh bypass and product-card scrolling concerns. Final re-review has no blocking findings. New unit regressions cover catalog provenance/count, Bhujia pack matching, confirmation gates, Maggi prepared-data blocking, unknown barcode fallback, incomplete nutrition, and refresh safety. Final command: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --max-workers=1`. Passed: 117 tests, zero failures/errors/skips; debug APK built with all 100 bundled records; lint zero errors and 45 pre-existing warnings. An initial attempt used Android Studio JDK 25 and failed before compilation; rerunning with the project-configured JDK 17 and SDK succeeded. Physical packet correctness and real-device camera/accessibility behavior remain unverified.
