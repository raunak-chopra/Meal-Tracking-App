"""Validate the app version and extract public release notes; no dependencies."""
import os
from pathlib import Path
import re
import subprocess


def version(text):
    name = re.search(r'versionName\s*=\s*"(\d+\.\d+\.\d+)"', text)
    code = re.search(r'versionCode\s*=\s*(\d+)', text)
    if not name or not code or int(code.group(1)) < 1:
        raise SystemExit("Use a numeric x.y.z versionName and integer versionCode.")
    return name.group(1), int(code.group(1))


current, code = version(Path("app/build.gradle.kts").read_text())
base = os.environ.get("BASE_SHA")
changed = True
if base and set(base) != {"0"}:
    old = subprocess.check_output(
        ["git", "show", f"{base}:app/build.gradle.kts"], text=True
    )
    previous, previous_code = version(old)
    changed = current != previous
    if changed and (code <= previous_code or tuple(map(int, current.split("."))) <= tuple(map(int, previous.split(".")))):
        raise SystemExit("Version upgrades must increase both versionName and versionCode.")
    if not changed and code != previous_code:
        raise SystemExit("Increase versionName along with versionCode.")

notes = re.search(
    rf"^## \[{re.escape(current)}\][^\n]*\n(.*?)(?=^## |\Z)",
    Path("CHANGELOG.md").read_text(), re.M | re.S,
)
if changed and (not notes or not notes.group(1).strip()):
    raise SystemExit(f"Add a nonempty CHANGELOG.md section for [{current}].")
if changed:
    Path("release-notes.md").write_text(notes.group(1).strip() + "\n", encoding="utf-8")
if os.environ.get("GITHUB_OUTPUT"):
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        output.write(f"version={current}\nchanged={str(changed).lower()}\n")
print(f"Version {current} ({code}); upgrade={changed}")
