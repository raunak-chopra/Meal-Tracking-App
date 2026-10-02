# GitHub guide for this project

## Recommended setup

- Keep this repository public, with personal records and credentials outside Git.
- Use MIT if you want others to freely use and modify the app, including commercially.
  This repository uses the MIT License; retain its notice when redistributing code.
- Protect `main`: require a pull request and the `verify` check, block force pushes
  and branch deletion. As a solo maintainer, require zero external approvals.
- Enable secret scanning, push protection, private vulnerability reporting and
  dependency vulnerability alerts where available. Do not bypass a secret warning.
- Keep routine code changes separate from completed app version upgrades.
- Start with source releases. Add signed installable releases after establishing
  a stable signing key, private key backup and upgrade testing on a real device.

## Your everyday process

Ask the agent to finish and verify a change. It prepares a branch and pull request:
a pull request is a proposed change you can inspect before it becomes part of `main`.
When the checks pass, merge it. Ordinary improvements do not need a version bump.

When you want a new app version, ask: "Prepare the next version upgrade, update the
changelog, run checks and publish it through a pull request." Follow `RELEASING.md`.
Use `1.0.1` for a small fix, `1.1.0` for a feature and `2.0.0` for a breaking change.
Android's `versionCode` must also increase on every released upgrade.

After merging, the Actions tab shows checks and publishing progress. The Releases
page records the version and its changes. Green checks are automated verification;
they do not replace phone testing of camera, permissions, backups and data migrations.

## Privacy decisions

Future Git commits should use the verified noreply address shown in your GitHub
email settings. This setting does not remove email addresses in old commits.
Old machine paths are also present in earlier README versions. Rewriting history
would change commit identities and disrupt existing clones. Leave it as a separate,
explicit decision; deleting current text cannot erase existing public copies.

If a real secret is discovered, revoke or rotate it first, then remove it from
files and coordinate history cleanup. Do not put credentials into a chat or issue.

## One-time sign-in

GitHub CLI uses browser/device authorization so the agent can push and configure
the repository. Sign in to your own GitHub account in the browser and authorize
GitHub CLI. You do not need to give the agent your password or paste a token.
