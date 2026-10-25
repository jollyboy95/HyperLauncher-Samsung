# Runtime source search

## Located file listings, not downloaded or verified binaries

Voyager HyperOS Update lists three extracted files from Xiaomi 17 Pro (pandora), HyperOS 4.0.0.22:

| File | Advertised size | Source |
| --- | --- | --- |
| libhyper_os_flutter.so | 17 MB | https://t.me/VoyagerMIUIUpdate/16558 |
| libmisqlite3.so | 1.7 MB | https://t.me/VoyagerMIUIUpdate/16557 |
| librust_maml_sdk.so | 10.9 MB | https://t.me/VoyagerMIUIUpdate/16559 |

Context: https://t.me/s/VoyagerMIUIUpdate?q=%23libhyper_os_flutter

These are third-party firmware extractions. The binaries have not been downloaded, hashed or checked against launcher 8.01.02.8584. Their existence is not a complete runtime distribution or evidence of Samsung compatibility. The publisher's compatibility claim concerns HyperOS 3, not Samsung One UI.

## Loader and framework dependencies

Primary reverse-engineering report:
https://github.com/wxxsfxyzm/MiuiBackGestureHook/blob/main/ANDROID_17_MIUI_HOME_NATIVE_LOADER_REPORT.md

This report concerns earlier 8.01.02.4349/4371 builds on Xiaomi Android 17 and archives an older injection experiment. It documents PackageManager Rust-package classification, ActivityManager RustProcessManager integration, a reserved hyos_spawner socket, /system_ext/bin/hyos_spawner, libhyper_os_shell.so and runtime preloads. These observations explain why a no-DEX launcher is not an ordinary Android Activity APK. Exact compatibility with our selected build remains untested.

Independent runtime API documentation:
https://github.com/LSPosed/ZygiskNext/blob/main/docs/hyos_runtime.md

The API explicitly describes an environment independent of ART with no JNI/Java objects. This is injection documentation for an existing Xiaomi runtime, not an implementation of that runtime for Samsung.

## Current result

Three partial library sources located. No complete matching hyperos.rustruntime.v5 package, Samsung-compatible process loader or no-root port of the selected native generation has been located. The other Xiaomi libraries in reports/8.01.02.8584.json remain uncollected.

## Practical next boundary

A matching complete HyperOS firmware could supply files for further static analysis, but packaging its files into an APK cannot by itself reproduce system-server modifications, process spawning and service contracts on stock One UI. No installable Samsung build is ready. A compatibility runtime would be a substantial engineering project, not a manifest/signature patch. Do not claim feasibility or ask the user to install these SO files directly.
