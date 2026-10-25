# HyperLauncher Samsung port

## Goal

Adapt Xiaomi's latest selected HyperOS launcher APK for a stock Samsung S26 Ultra without root. Preserve the latest launcher as the implementation target; older ports are compatibility references, not the product base.

## Selected target

- Package: com.miui.home
- Version: RELEASE-8.01.02.8584-260918-10231931-R
- Downloaded APK size: 58,057,517 bytes
- ZIP integrity check passed.
- Minimum SDK: 35; target SDK: 36.
- Declares library hyperos.rustruntime.v5. Its required flag and runtime dependencies still need detailed inspection.
- Native ARM64 libraries: libnative_widget_sdk.so, libresources_frb.so, libapp_launcher.so, libapp.so.

## Reference inputs

- APW HyperOS 3 port RELEASE-6.01.02.1318-10030153: opens on the user's Samsung, but widgets cannot be added, app drawer behavior is clunky, and several settings do not work.
- APW RELEASE-6.01.05.20: user reports failure both as an update and fresh installation. Exact installation error has not been captured.
- Xiaomi.eu Extension 1.4.4.
- Mods Center HyperOS Launcher V7.5 Final module.

## Investigation order

1. Decode manifests and compare startup paths and bundled dependencies across the selected target and older ports.
2. Inspect native dependencies and determine what hyperos.rustruntime.v5 supplies. Merely removing its declaration is not a compatibility fix.
3. Establish a reproducible decode/rebuild/sign process before changing behavior.
4. Make the latest launcher start on stock Samsung, then test normal app launching, home persistence and app drawer.
5. Repair standard Android widget binding/configuration/persistence; investigate Xiaomi widget providers separately.
6. Adapt settings and visual features supported by app-level permissions. Track features requiring system integration explicitly.

## Status and limits

No patched APK has been built or tested. Repository initialization and APK inspection do not establish port feasibility. Native code and Xiaomi framework integration may require substantial work. Do not represent an APK rebuilt from decompiled code as original Xiaomi source.

## Ownership

Xiaomi applications and third-party modifications retain their respective ownership and licence terms. No licence is assigned here to those upstream components.
