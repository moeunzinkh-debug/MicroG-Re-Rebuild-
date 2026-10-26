#!/usr/bin/env python3
"""Guard source manifests against known install-blocking sensitive permissions.

This is a narrow regression check, not a malware scanner. Android's XML parser
ignores comments, and Android's manifest merger strips permissions annotated
with tools:node="remove".
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path
from xml.etree import ElementTree

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
TOOLS_NS = "{http://schemas.android.com/tools}"
SENSITIVE_PERMISSIONS = {
    "android.permission.RECEIVE_SMS",
    "android.permission.READ_SMS",
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.REQUEST_INSTALL_PACKAGES",
    "android.permission.BIND_ACCESSIBILITY_SERVICE",
    "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
}
PERMISSION_TAGS = {
    "uses-permission",
    "uses-permission-sdk-23",
    "uses-permission-sdk-m",
}
COMPONENT_TAGS = {"activity", "activity-alias", "provider", "receiver", "service"}
EXCLUDED_DIRS = {".git", ".gradle", "build", "dist", "apks"}


def source_manifests(root: Path) -> list[Path]:
    return sorted(
        path
        for path in root.rglob("AndroidManifest.xml")
        if not EXCLUDED_DIRS.intersection(path.relative_to(root).parts)
    )


def inspect_manifest(path: Path, root: Path) -> list[str]:
    try:
        document = ElementTree.parse(path)
    except ElementTree.ParseError as error:
        return [f"{path.relative_to(root)}: invalid XML: {error}"]

    issues: list[str] = []
    for element in document.iter():
        tag = element.tag.rsplit("}", 1)[-1]
        tools_node = element.get(TOOLS_NS + "node", "")
        if tools_node == "remove":
            continue

        if tag in PERMISSION_TAGS:
            name = element.get(ANDROID_NS + "name", "")
            if name in SENSITIVE_PERMISSIONS:
                issues.append(f"{path.relative_to(root)}: declares {name}")

        if tag in COMPONENT_TAGS:
            permission = element.get(ANDROID_NS + "permission", "")
            if permission in SENSITIVE_PERMISSIONS:
                issues.append(
                    f"{path.relative_to(root)}: component requires {permission}"
                )

    return issues


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="repository root to inspect (defaults to this checkout)",
    )
    args = parser.parse_args()
    root = args.root.resolve()
    manifests = source_manifests(root)
    if not manifests:
        print(f"ERROR: no AndroidManifest.xml files found under {root}", file=sys.stderr)
        return 2

    issues = [
        issue
        for manifest in manifests
        for issue in inspect_manifest(manifest, root)
    ]
    if issues:
        print("ERROR: install-sensitive manifest declarations found:", file=sys.stderr)
        for issue in issues:
            print(f"  - {issue}", file=sys.stderr)
        return 1

    print(
        f"OK: checked {len(manifests)} MicroG-RE Android manifests; "
        "no blocked SMS, overlay, package-installer, accessibility, or "
        "notification-listener permissions are declared."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
