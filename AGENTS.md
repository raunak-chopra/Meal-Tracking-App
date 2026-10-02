# Repository maintenance

This repository is public. Do not commit credentials, signing keys, personal records,
photos, backup exports, machine-specific paths, or local review diff artifacts.
Use fictional data in examples, screenshots and fixtures.

For every completed app version upgrade, follow `docs/RELEASING.md`: increase both
Android version fields, add dated changelog notes, update public documentation, run
relevant checks, and prepare the complete upgrade for GitHub. Report whether the push
and release actually completed; never claim a local edit is already on GitHub.
Do not bump a version for every small edit. A version upgrade merged to `main`
signals publication by `.github/workflows/upgrade.yml`.

Preserve unrelated working changes. Never rewrite published Git history or remove
existing app data as part of routine release cleanup.
