#!/usr/bin/env python3
"""Apply checked, reproducible HyperLauncher changes to pinned Lawnchair source."""
# Original project tooling. SPDX-License-Identifier: Apache-2.0
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
LOCK = json.loads((ROOT / 'launcher/upstream.json').read_text())

def patch(path, old, new):
    text = path.read_text()
    if text.count(old) != 1:
        raise RuntimeError(f'{path}: expected exactly one patch match, got {text.count(old)}')
    path.write_text(text.replace(old, new, 1))

def prepare(dest):
    actual = subprocess.check_output(['git', '-C', str(dest), 'rev-parse', 'HEAD'], text=True).strip()
    if actual != LOCK['commit']:
        raise RuntimeError(f'Upstream commit mismatch: {actual}')
    if (dest / 'hyperlauncher').exists():
        raise RuntimeError('Source already prepared; use a fresh checkout')
    # Fail before editing if the user supplied a modified checkout.
    if subprocess.check_output(['git', '-C', str(dest), 'status', '--porcelain'], text=True).strip():
        raise RuntimeError('Refusing to modify a non-clean upstream checkout')
    gradle = dest / 'build.gradle'
    patch(gradle, "applicationId 'app.lawnchair'", "applicationId 'au.steve.hyperlauncher'")
    patch(gradle, 'resValue("string", "derived_app_name", "Lawnchair (Debug)")', 'resValue("string", "derived_app_name", "HyperLauncher Preview")')
    patch(gradle, 'resValue("string", "derived_app_name", "Lawnchair")', 'resValue("string", "derived_app_name", "HyperLauncher")')
    patch(gradle, 'manifest.srcFile "github/AndroidManifest.xml"', '''manifest.srcFile "hyperlauncher/AndroidManifest.xml"
            java.srcDirs = ['hyperlauncher/src']
            res.srcDirs = ['hyperlauncher/res']''')
    patch(gradle, '"Lawnchair.${variant.versionName}.$channel.${variant.buildType.name}.apk"', '"HyperLauncher.Preview.${variant.versionName}.$channel.${variant.buildType.name}.apk"')
    gradle.write_text('// Modified for HyperLauncher: identity and channel source overlay.\n' + gradle.read_text())
    common = dest / 'AndroidManifest-common.xml'
    patch(common, 'android:icon="@drawable/ic_launcher_home"', 'android:icon="@drawable/hyper_launcher_icon"')
    patch(common, '<manifest', '<!-- Modified for HyperLauncher: shared app icon. -->\n<manifest')
    config = dest / 'lawnchair/res/values/config.xml'
    for name, before, after in [
        ('config_default_icon_shape', 'system', 'squircle'),
        ('config_default_hotseat_mode', 'lawnchair', 'disabled'),
    ]:
        patch(config, f'<string name="{name}" translatable="false">{before}</string>', f'<string name="{name}" translatable="false">{after}</string>')
    patch(config, '<bool name="config_default_enable_smartspace">true</bool>', '<bool name="config_default_enable_smartspace">false</bool>')
    patch(config, '<resources>', '<!-- Modified for HyperLauncher: first-run defaults. -->\n<resources>')
    prefs = dest / 'lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt'
    for old, new in [
        ('val workspaceColumns = IntPref("pref_workspaceColumns", 4)', 'val workspaceColumns = IntPref("pref_workspaceColumns", 5)'),
        ('val workspaceRows = IntPref("pref_workspaceRows", 5)', 'val workspaceRows = IntPref("pref_workspaceRows", 6)'),
        ('val hotseatBG = BoolPref("pref_hotseatBG", false, recreate)', 'val hotseatBG = BoolPref("pref_hotseatBG", true, recreate)'),
        ('val hotseatBGAlpha = IntPref("pref_hotseatBGTransparency", 100, recreate)', 'val hotseatBGAlpha = IntPref("pref_hotseatBGTransparency", 75, recreate)'),
        ('val addIconToHome = BoolPref("pref_add_icon_to_home", true)', 'val addIconToHome = BoolPref("pref_add_icon_to_home", false)'),
    ]:
        patch(prefs, old, new)
    prefs.write_text('// Modified for HyperLauncher: grid, dock and app-drawer defaults.\n' + prefs.read_text())
    settings = dest / 'settings.gradle'
    # Do not publish upstream's automatic build scans for this private project.
    patch(settings, 'plugins {\n    id "com.gradle.develocity" version "4.3.1"\n}', '// HyperLauncher: upstream build-scan plugin removed.')
    start = settings.read_text().index('def isCI = providers.environmentVariable')
    end = settings.read_text().index('dependencyResolutionManagement {', start)
    text = settings.read_text()
    settings.write_text(text[:start] + '// Modified for HyperLauncher: no automatic build-scan upload.\n\n' + text[end:])
    shutil.copytree(ROOT / 'launcher', dest / 'hyperlauncher')
    for path in (dest / 'hyperlauncher').rglob('*.xml'):
        ET.parse(path)
    print(f'Prepared HyperLauncher from Lawnchair {actual}')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('source', type=Path)
    prepare(parser.parse_args().source.resolve())
