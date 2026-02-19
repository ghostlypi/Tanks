#!/usr/bin/env python3
"""Replace tabs with spaces in all Java files under src/.

Each tab is expanded to the next 4-space tab-stop boundary using
str.expandtabs(4), which correctly handles mixed tab/space indentation.
For example, a tab at column 2 becomes 2 spaces (to reach column 4),
not 4 — preserving the intended indent level.
"""

from pathlib import Path

TABSIZE = 4
root = Path(__file__).parent / "src"
files_changed = 0
files_checked = 0

for java_file in sorted(root.rglob("*.java")):
    files_checked += 1
    original = java_file.read_text(encoding="utf-8")
    replaced = original.expandtabs(TABSIZE)
    if replaced != original:
        java_file.write_text(replaced, encoding="utf-8")
        files_changed += 1
        print(f"  fixed: {java_file.relative_to(root.parent)}")

print(f"\n{files_changed}/{files_checked} files updated.")
