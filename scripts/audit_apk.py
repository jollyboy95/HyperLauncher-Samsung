#!/usr/bin/env python3
"""Audit a donor APK without executing it. Requires androguard and readelf."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile
from loguru import logger
logger.remove()
from androguard.core.apk import APK

def audit(path):
    apk = APK(str(path))
    root = apk.get_android_manifest_xml()
    ns = "{http://schemas.android.com/apk/res/android}"
    with zipfile.ZipFile(path) as archive, tempfile.TemporaryDirectory() as temp:
        native = {}
        for name in archive.namelist():
            if not (name.startswith("lib/") and name.endswith(".so")):
                continue
            target = Path(temp) / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(name))
            result = subprocess.run(["readelf", "-d", str(target)], check=True,
                                    capture_output=True, text=True)
            native[name] = sorted(set(re.findall(r"Shared library: \[(.*?)\]", result.stdout)))
        bundled = {Path(name).name for name in native}
        absent = sorted({dep for deps in native.values() for dep in deps} - bundled)
        libraries = [{"name": e.get(ns + "name"), "required": e.get(ns + "required", "true")}
                     for e in root.iter("uses-library")]
        return {
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "bytes": path.stat().st_size,
            "package": apk.get_package(), "version": apk.get_androidversion_name(),
            "min_sdk": apk.get_min_sdk_version(), "target_sdk": apk.get_target_sdk_version(),
            "has_code": root.find("application").get(ns + "hasCode", "true"),
            "dex_files": list(apk.get_dex_names()),
            "uses_libraries": libraries, "native_dependencies": native,
            "not_bundled_direct_native_dependencies": absent,
            "note": "Not bundled does not mean unavailable: standard Android libraries come from the OS. Xiaomi libraries require separate investigation."
        }

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    print(json.dumps(audit(args.apk), indent=2))
