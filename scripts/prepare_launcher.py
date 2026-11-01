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
    entry_manifest = dest / 'quickstep/AndroidManifest-launcher.xml'
    patch(entry_manifest, 'android:icon="@drawable/ic_launcher_home"',
          'android:icon="@drawable/hyper_launcher_icon" tools:replace="android:icon"')
    patch(entry_manifest, '<manifest', '<!-- Modified for HyperLauncher: highest-priority app icon override. -->\n<manifest')
    config = dest / 'lawnchair/res/values/config.xml'
    for name, before, after in [
        ('config_default_icon_shape', 'system', 'squircle'),
        ('config_default_hotseat_mode', 'lawnchair', 'disabled'),
        ('config_default_folder_color', 'default', 'custom|#e6ffffff'),
        ('config_default_accent_color', 'no_default', 'wallpaper_primary'),
    ]:
        patch(config, f'<string name="{name}" translatable="false">{before}</string>', f'<string name="{name}" translatable="false">{after}</string>')
    for name in [
        'config_default_enable_smartspace',
        'config_default_show_suggested_apps_at_drawer_top',
        'config_default_enable_two_line_allapps',
        'config_default_show_scrollbar',
    ]:
        patch(config, f'<bool name="{name}">true</bool>', f'<bool name="{name}">false</bool>')
    patch(config, '<item name="config_default_drawer_left_right_factor" type="dimen" format="float">0.1</item>',
          '<item name="config_default_drawer_left_right_factor" type="dimen" format="float">0.05</item>')
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
    activity = dest / 'lawnchair/src/app/lawnchair/ui/preferences/PreferenceActivity.kt'
    patch(activity, '        enableEdgeToEdge()', """        // HyperLauncher: custom hub for the normal settings entry; retain all deep links.
        if (!intent.hasExtra(EXTRA_DESTINATION_ROUTE)) {
            startActivity(Intent(this, au.steve.hyperlauncher.settings.HyperSettingsActivity::class.java))
            finish()
            return
        }
        enableEdgeToEdge()""")
    controller = dest / 'src/com/android/launcher3/model/ModelDbController.java'
    patch(controller, ': idp.defaultLayoutId;', ': ((idp.numColumns == 5 && idp.numRows == 6)\n                                ? com.android.launcher3.R.xml.default_workspace_5x6 : idp.defaultLayoutId);')
    controller.write_text('// Modified for HyperLauncher: select the fresh 5x6 preset explicitly.\n' + controller.read_text())
    workspace = dest / 'lawnchair/res/xml/default_workspace_5x6.xml'
    # A fresh-install Samsung-friendly preset. Existing databases are never reset.
    import re
    layout = workspace.read_text()
    layout = re.sub(r'    <resolve\n        launcher:container="-101"\n        launcher:screen="2".*?</resolve>\n', '', layout, count=1, flags=re.S)
    layout = layout.replace('launcher:screen="3"\n        launcher:x="3"', 'launcher:screen="2"\n        launcher:x="2"')
    layout = layout.replace('launcher:screen="4"\n        launcher:x="4"', 'launcher:screen="3"\n        launcher:x="3"')
    layout = layout.replace('    <!-- Screen 0 -->', '    <!-- Modified for HyperLauncher: four-app dock; open upper rows for Hyper cards. -->\n    <!-- Screen 0 -->')
    # Preview builds use the .debug application id. The weather card is added from
    # the widget picker because it needs a city selected first.
    widget_package = LOCK['application_id'] + '.debug'
    layout = layout.replace('</favorites>', f'''
    <!-- Modified for HyperLauncher: fresh-install Hyper clock and battery cards. -->
    <appwidget
        launcher:packageName="{widget_package}"
        launcher:className="au.steve.hyperlauncher.widgets.HyperClockWidget"
        launcher:screen="0" launcher:spanX="5" launcher:spanY="2"
        launcher:x="0" launcher:y="0" />
    <appwidget
        launcher:packageName="{widget_package}"
        launcher:className="au.steve.hyperlauncher.widgets.HyperBatteryWidget"
        launcher:screen="0" launcher:spanX="2" launcher:spanY="2"
        launcher:x="0" launcher:y="2" />
</favorites>''')
    workspace.write_text(layout)
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
