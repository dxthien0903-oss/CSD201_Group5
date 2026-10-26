"""Compare the compiled QuickBench (old revision) with the current source revision."""
import difflib
import pathlib
import subprocess

BASE = pathlib.Path(r"C:\Users\phucv\Downloads\csd\tv1")
OUT = BASE / "audit" / "revision"
OLD_CP = BASE / "out" / "classes"
NEW_CP = OUT / "current_classes"


def javap(classpath: pathlib.Path, cls: str) -> list[str]:
    proc = subprocess.run(
        ["javap", "-p", "-c", "-constants", "-classpath", str(classpath), cls],
        capture_output=True,
        text=True,
        check=True,
    )
    return proc.stdout.splitlines()


for cls in ("sorting.bench.QuickBench", "sorting.bench.QuickBench$Options"):
    old = javap(OLD_CP, cls)
    new = javap(NEW_CP, cls)
    old_path = OUT / (cls.replace(".", "_") + "_old.javap.txt")
    new_path = OUT / (cls.replace(".", "_") + "_new.javap.txt")
    old_path.write_text("\n".join(old), encoding="utf-8")
    new_path.write_text("\n".join(new), encoding="utf-8")
    diff = list(
        difflib.unified_diff(old, new, fromfile="out/classes (old)", tofile="current src", lineterm="")
    )
    print("=== " + cls + " ===")
    print("old_lines=" + str(len(old)) + " new_lines=" + str(len(new)) + " diff_lines=" + str(len(diff)))
    for line in diff:
        print(line)
