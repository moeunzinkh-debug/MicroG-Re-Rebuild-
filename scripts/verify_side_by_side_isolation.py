#!/usr/bin/env python3
"""Verify that multiple compiled APKs can be installed side-by-side on Android / HarmonyOS
with zero package, sharedUserId, permission, permission-tree, provider authority, or library collisions."""

import os
import pathlib
import re
import subprocess
import sys


def find_aapt2() -> str:
    if os.environ.get("AAPT2"):
        return os.environ["AAPT2"]
    android_home = os.environ.get("ANDROID_HOME", "")
    bt = pathlib.Path(android_home) / "build-tools"
    if bt.is_dir():
        versions = sorted(bt.iterdir(), key=lambda p: p.name)
        if versions:
            cand = versions[-1] / "aapt2"
            if cand.is_file():
                return str(cand)
    raise SystemExit("ERROR: aapt2 not found")


ELEM_RE = re.compile(r"^(\s*)E:\s+([A-Za-z0-9_.-]+)\s+\(line=\d+\)")
ATTR_STR_RE = re.compile(r'^\s*A:\s+(?:[A-Za-z0-9_.-]+:)?([A-Za-z0-9_.-]+)(?:\(0x[0-9a-fA-F]+\))?="([^"]*)"')


def extract_globals(aapt2: str, apk_path: str) -> dict:
    out = subprocess.check_output(
        [aapt2, "dump", "xmltree", apk_path, "--file", "AndroidManifest.xml"],
        text=True,
        errors="replace",
    )
    info = {
        "apk": os.path.basename(apk_path),
        "package": set(),
        "sharedUserId": set(),
        "permission": set(),
        "permission-tree": set(),
        "permission-group": set(),
        "provider_authority": set(),
        "library": set(),
        "original-package": set(),
    }

    current_tag = None
    for line in out.splitlines():
        m_el = ELEM_RE.match(line)
        if m_el:
            current_tag = m_el.group(2)
            continue
        m_attr = ATTR_STR_RE.match(line)
        if not m_attr or not current_tag:
            continue
        attr_name, val = m_attr.group(1), m_attr.group(2).strip()
        if not val:
            continue

        if current_tag == "manifest":
            if attr_name == "package":
                info["package"].add(val)
            elif attr_name == "sharedUserId":
                info["sharedUserId"].add(val)
        elif current_tag in ("permission", "permission-tree", "permission-group"):
            if attr_name == "name" and not val.startswith("android.permission-group."):
                info[current_tag].add(val)
        elif current_tag == "provider" and attr_name == "authorities":
            for auth in val.split(";"):
                if auth.strip():
                    info["provider_authority"].add(auth.strip())
        elif current_tag in ("library", "static-library") and attr_name == "name":
            info["library"].add(val)
        elif current_tag == "original-package" and attr_name == "name":
            info["original-package"].add(val)

    return info


def main() -> int:
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <apk1> <apk2> [apk3 ...]", file=sys.stderr)
        return 2

    aapt2 = find_aapt2()
    records = [extract_globals(aapt2, p) for p in sys.argv[1:]]

    for r in records:
        print(
            f"[APK] {r['apk']}: pkg={sorted(r['package'])} "
            f"perms={len(r['permission'])} providers={len(r['provider_authority'])} "
            f"libs={sorted(r['library'])}"
        )

    categories = [
        "package",
        "sharedUserId",
        "permission",
        "permission-tree",
        "permission-group",
        "provider_authority",
        "library",
        "original-package",
    ]

    collisions = []
    for i in range(len(records)):
        for j in range(i + 1, len(records)):
            a, b = records[i], records[j]
            for cat in categories:
                overlap = sorted(a[cat] & b[cat])
                if overlap:
                    collisions.append((a["apk"], b["apk"], cat, overlap))

    if collisions:
        for apk_a, apk_b, cat, items in collisions:
            print(
                f"COLLISION [{cat}] between {apk_a} and {apk_b}: {items}",
                file=sys.stderr,
            )
        return 1

    print(
        f"OK: Verified 0 manifest collisions across {len(records)} APKs "
        f"({', '.join(r['apk'] for r in records)})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
