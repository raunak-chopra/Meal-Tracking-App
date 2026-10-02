"""Exercise release gates without modifying the app working tree."""
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("release_metadata.py").resolve()


class ReleaseGateTest(unittest.TestCase):
    def run_gate(self, name="1.0.1", code=2, notes=True):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "app").mkdir()
            build = root / "app/build.gradle.kts"
            build.write_text('versionName = "1.0.0"\nversionCode = 1\n')
            def git(*args):
                return subprocess.check_output(["git", *args], cwd=root, text=True)
            git("init", "-q")
            git("add", ".")
            git("-c", "user.name=Test", "-c", "user.email=test@example.invalid", "commit", "-qm", "Baseline")
            base = git("rev-parse", "HEAD").strip()
            build.write_text(f'versionName = "{name}"\nversionCode = {code}\n')
            (root / "CHANGELOG.md").write_text(f"## [{name}] - 2026-10-02\n\n- Improved tracking.\n" if notes else "## [Unreleased]\n")
            env = dict(os.environ, BASE_SHA=base, GITHUB_OUTPUT=str(root / "output"))
            result = subprocess.run([sys.executable, str(SCRIPT)], cwd=root, env=env, capture_output=True, text=True)
            output = (root / "output").read_text() if (root / "output").exists() else ""
            return result.returncode, output

    def test_upgrade(self):
        code, output = self.run_gate()
        self.assertEqual(code, 0)
        self.assertIn("changed=true", output)

    def test_ordinary_commit(self):
        code, output = self.run_gate("1.0.0", 1)
        self.assertEqual(code, 0)
        self.assertIn("changed=false", output)

    def test_invalid_upgrades(self):
        for name, code, notes in [("1.0.1", 1, True), ("0.9.0", 2, True), ("1.0.0", 2, True), ("1.0.1", 2, False)]:
            with self.subTest(name=name, code=code, notes=notes):
                self.assertNotEqual(self.run_gate(name, code, notes)[0], 0)


if __name__ == "__main__":
    unittest.main()
