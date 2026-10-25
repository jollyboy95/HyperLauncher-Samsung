# Compatibility prototype 0.1

This Android app is a development instrument for porting the selected latest Xiaomi launcher. It is not a launcher replacement or an implementation of the missing Xiaomi runtime.

## Current behavior

- Select a donor APK through Android's file picker (no broad storage permission).
- Verify its SHA-256 against the inspected 8.01.02.8584 build.
- Report package metadata, DEX entries, ARM64 native dependencies and unbundled Xiaomi libraries.
- Check runtime registration, readable runtime paths and visible widget provider count on the actual phone.
- Save the JSON report through Android's document picker.
- Does not execute native donor code, install packages or change the default home app.

The ELF reader uses program headers and DT_NEEDED, so it works with stripped ARM64 binaries. Presence of files and advertised libraries is not evidence that a runtime can load successfully.

## Build

JDK 17, Gradle 8.11.1, Android SDK platform/build tools 35.

    gradle :compat:testDebugUnitTest :compat:lintDebug :compat:assembleDebug

GitHub Actions builds the debug APK as HyperLauncher-compatibility-debug. A CI debug key is for this prototype only; future runs may have different keys, so an upgrade may require uninstalling the earlier diagnostic app. No runtime or launcher APK is bundled.

## Still missing

The native component host, HyperOS service bridges, matching runtime binaries and a working latest-launcher home screen. Device inspection cannot solve those dependencies by itself. The Xiaomi 18 Pro Max stock theme remains the appearance target; its complete asset set has not been obtained.
