# Aurora Depth — HyperOS-inspired lock screen

This branch repurposes HyperLauncher-Samsung as a standalone Android lock-screen experience.

## First milestone
- Remove launcher HOME intent and launcher-only screens; retain a normal app settings entry.
- Full-screen lock-screen activity using Android's showWhenLocked behavior; do not disable secure Keyguard.
- Live 12/24-hour clock, date, adjustable clock placement and blue gradient typography.
- User-selected wallpaper (Duchess photo locally imported, not bundled by default).
- Depth subject mask generated or selected on-device; preserve original photo.
- Optional notification access with explicit consent; redacted previews by default.
- Shortcuts, battery and media widgets; weather only after consent.
- Test screen-on, fingerprint/PIN, screen rotation, AOD coexistence and battery behavior on Samsung Android 17.
- Shizuku is optional and must never be required for core UI.
- No root, bootloader unlock, Knox modifications, signature bypass or disabling Samsung security.

## Acceptance criteria
1. APK installs alongside stock Samsung lock screen.
2. Screen never bypasses secure unlock or exposes private notification content before authentication.
3. User can disable the feature and uninstall cleanly.
4. Build APK via GitHub Actions and document known platform limitations.

## Reference materials
Xiaomi Theme Manager and MIUI AOD APKs are for architecture study only. Do not redistribute proprietary binaries, fonts or code.
