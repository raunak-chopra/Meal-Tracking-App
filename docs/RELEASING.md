# Version upgrade cadence

Update GitHub for every completed app version upgrade, rather than on a time schedule.

1. Finish the change and verify it on a disposable emulator or phone. Back up real data before testing upgrades.
2. Increase both `versionName` (numeric `major.minor.patch`) and `versionCode` in `app/build.gradle.kts`.
3. Move the relevant Unreleased notes into `## [x.y.z] - YYYY-MM-DD` in `CHANGELOG.md`. Update README feature/setup details when behavior changes.
4. Review staged files for credentials, local paths, real meals/photos, backups and signing material. Keep local `.diff` reports out of commits.
5. Commit the complete upgrade, push its branch and open a pull request to `main`. CI checks versioning, changelog, unit tests, lint and debug/test APK builds.
6. Merge after checks and device verification. A successful push to `main` with a new version automatically creates `vx.y.z` and a GitHub source release using the changelog notes.
7. Check the Actions run and release page. If publishing fails, rerun the failed job; manual workflow dispatch can recover a missing release for the current version.

Ordinary commits run CI without creating a release. Keep one version upgrade per merge;
avoid batching several version bumps into one push. A version bump is the publication signal.
The workflow uses GitHub's built-in token; no personal access token or API key is required.
It publishes source releases only. Signed installable APK distribution needs a stable private
signing key and a separately reviewed signing workflow. Never distribute CI debug APKs as a
stable upgrade channel: their signing identity may change between runners.

## One-time repository setup

- Push these workflow files to GitHub and enable Actions if disabled.
- Protect `main` with a rule requiring the `verify` check and pull requests.
- Enable available secret scanning/push protection in repository settings.
- Choose a source license and add a LICENSE file before advertising the project as open source.
- Use a verified GitHub noreply address for future commits if you do not want your email public.
  Existing commit metadata is unchanged; removing an already published email requires coordinated history rewriting.
- Keep API keys and Android signing keys outside Git. Rotate any credential ever published;
  deleting it from the current files does not remove it from history.
