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
# Matches both:
#   A: package="org.microg.android.gms" (Raw: "org.microg.android.gms")
#   A: android:name(0x01010003)="..." (Raw: "...")
#   A: http://schemas.android.com/apk/res/android:authorities(0x01010018)="..." (Raw: "...")
ATTR_STR_RE = re.compile(
    r'^\s*A:\s+(?:\S+:)?([A-Za-z0-9_.-]+)(?:\(0x[0-9a-fA-F]+\))?="([^"]*)"'
)


def parse_xmltree_manifest(out: str, apk_name: str) -> dict:
    info = {
        "apk": apk_name,
        "package": set(),
        "sharedUserId": set(),
        "permission": set(),
        "permission-tree": set(),
        "permission-group": set(),
        "provider_authority": set(),
        "library": set(),
        "original-package": set(),
        "adopt-permissions": set(),
        "instrumentation": set(),
        "overlay": set(),
    }

    stack = []  # list of (indent_len, tag_name)
    for line in out.splitlines():
        m_el = ELEM_RE.match(line)
        if m_el:
            indent = len(m_el.group(1))
            tag = m_el.group(2)
            while stack and stack[-1][0] >= indent:
                stack.pop()
            stack.append((indent, tag))
            continue

        m_attr = ATTR_STR_RE.match(line)
        if not m_attr or not stack:
            continue
        current_tag = stack[-1][1]
        attr_name, val = m_attr.group(1), m_attr.group(2).strip()
        if not val:
            continue

        if current_tag == "manifest":
            if attr_name == "package":
                info["package"].add(val)
            elif attr_name == "sharedUserId":
                info["sharedUserId"].add(val)
        elif current_tag in (
            "permission",
            "permission-tree",
            "permission-group",
            "original-package",
            "adopt-permissions",
        ):
            if attr_name == "name":
                info[current_tag].add(val)
        elif current_tag in ("library", "static-library"):
            if attr_name == "name":
                info["library"].add(val)
        elif current_tag == "instrumentation":
            if attr_name == "name":
                info["instrumentation"].add(val)
        elif current_tag == "overlay":
            if attr_name == "targetPackage":
                info["overlay"].add(val)
        elif current_tag == "provider":
            if attr_name == "authorities":
                for auth in val.split(";"):
                    auth = auth.strip()
                    if auth:
                        info["provider_authority"].add(auth)

    return info


def extract_globals(aapt2: str, apk_path: str) -> dict:
    out = subprocess.check_output(
        [aapt2, "dump", "xmltree", apk_path, "--file", "AndroidManifest.xml"],
        text=True,
        errors="replace",
    )
    info = parse_xmltree_manifest(out, os.path.basename(apk_path))
    if len(info["permission"]) < 5 or len(info["provider_authority"]) < 5:
        raise SystemExit(
            f"ERROR: Unexpectedly low parsed manifest globals in {info['apk']}: "
            f"perms={len(info['permission'])} providers={len(info['provider_authority'])}. "
            f"Check aapt2 xmltree output format!"
        )
    return info


def main() -> int:
    apks = sys.argv[1:]
    if len(apks) < 2:
        print(
            "Usage: verify_side_by_side_isolation.py <apk1> <apk2> [<apk3> ...]",
            file=sys.stderr,
        )
        return 2

    aapt2 = find_aapt2()
    parsed = [extract_globals(aapt2, p) for p in apks]

    categories = [
        "package",
        "sharedUserId",
        "permission",
        "permission-tree",
        "permission-group",
        "provider_authority",
        "library",
        "original-package",
        "adopt-permissions",
        "instrumentation",
        "overlay",
    ]

    for item in parsed:
        print(
            f"[APK] {item['apk']}: pkg={sorted(item['package'])} "
            f"sharedUid={sorted(item['sharedUserId'])} "
            f"perms={len(item['permission'])} "
            f"permTrees={len(item['permission-tree'])} "
            f"permGroups={len(item['permission-group'])} "
            f"providers={len(item['provider_authority'])} "
            f"libs={sorted(item['library'])}"
        )

    legacy_forbidden = []
    for item in parsed:
        for perm in sorted(item["permission"]):
            if perm.startswith("org.microg.gms."):
                legacy_forbidden.append(("permission", perm, item["apk"]))
        for auth in sorted(item["provider_authority"]):
            if auth.startswith("org.microg.gms."):
                legacy_forbidden.append(("provider_authority", auth, item["apk"]))

    if legacy_forbidden:
        print(
            "\nERROR: Legacy un-isolated org.microg.gms.* declaration(s) detected "
            "(would collide with legacy ReVanced/MicroG-RE APKs):",
            file=sys.stderr,
        )
        for cat, val, apk_name in legacy_forbidden:
            print(f"  - [{cat}] '{val}' in {apk_name}", file=sys.stderr)
        return 1

    collisions = []
    for cat in categories:
        seen = {}
        for item in parsed:
            for val in sorted(item[cat]):
                if val in seen:
                    collisions.append((cat, val, seen[val], item["apk"]))
                else:
                    seen[val] = item["apk"]

    if collisions:
        print(
            "\nERROR: Side-by-side APK installation collision(s) detected:",
            file=sys.stderr,
        )
        for cat, val, apk_a, apk_b in collisions:
            print(
                f"  - [{cat}] '{val}' declared by BOTH {apk_a} AND {apk_b}",
                file=sys.stderr,
            )
        return 1

    print(
        f"OK: Verified 0 manifest collisions across {len(parsed)} APKs "
        f"({', '.join(x['apk'] for x in parsed)})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
