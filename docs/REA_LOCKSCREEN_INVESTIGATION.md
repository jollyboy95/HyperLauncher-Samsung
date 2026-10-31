# Lock-screen investigation — 1 November 2026

Superseded reference status: the user supplied four reference files after this initial pass. See [REA_REFERENCE_FINDINGS.md](REA_REFERENCE_FINDINGS.md) for their identities, static findings, clean-room backlog and the successful GitHub signed build.

## Scope and preserved baseline

Continue `feature/aurora-lockscreen`, not the native Xiaomi launcher port. The clean local checkout was fast-forwarded from 7edd5b5 (v24) to GitHub 59e102f (v25). Existing clock, depth, glass, Movie, signature, notification, media and wake features are preserved. No proprietary source, fonts or assets were imported.

## Evidence ledger

| Question | Status and evidence | Boundary |
| --- | --- | --- |
| REA connected | Observed: `binary_session` returns Android and resource tools; no native target open | No native session needed for Android tools |
| Apktool access | Observed: REA `inspect_apktool_client`, ev_9539c522298e93ee69b993965091f5e709743cbaa759826305d097d7630ff1e0, detects 3.0.3 | Command reported as `apktool`, selected through server PATH; absolute executable not reported |
| Actual resource decoding | Observed: ev_b4078d375f2caf5f4aa5992f8a5999af4aea70df39da91d5304b191844480024 decoded 11 files with complete resource projection from Aurora v24 | No smali; string tables not requested |
| JADX access | Observed: package inspection parent ev_8c658e7e8cdadbe7ea7e5b7a240a3b446aa54862ba38c3dd15aa04bc4d07ca3a; summary ev_fd4ba3afed1f1f6c1cbc953ea9366683c975d9edfb80188435619eb47792a82a | JADX bridge 0.7.1, one worker, 2048 MiB heap; static only |
| APK identity | Aurora v24, au.steve.hyperlauncher.compat, 1.3.1-preview, min 28 / target 35; SHA-256 e0cce934deab47dde681edb48ef808812cf60cf645aae1fc55aa9af9f0e84db5 | Our existing preview, not a Xiaomi reference; signature not verified by these calls |
| Xiaomi lock-screen architecture | Unresolved: user confirms reference APKs are on phone and will upload | Existing launcher report is historical repository data, not new REA analysis; unrelated car/firmware APKs were not selected |
| GitHub build | Observed failed run 41234787689 at 59e102f: Android Gradle Plugin 8.11.0 rejects workflow Gradle 8.11.1 | Requires 8.13; workflow corrected locally |

## Existing implementation observations

Source observations at 59e102f, separate from Xiaomi behavior:

- `AuroraSceneView` renders clocks and depth composition; preset animation uses a 420 ms PathInterpolator and respects both app motion preference and system animator availability.
- `AuroraActivity` hosts wallpaper, notification and media cards, requests system Keyguard dismissal for protected actions, and redacts text while locked according to the privacy setting.
- `AuroraWakeService` and `WakeLaunchPolicy` gate optional automatic launch on locked/interactive state, visibility, call state and one launch per wake cycle.
- Manifest uses showWhenLocked; notification and accessibility services require their respective Android binding permissions. These declarations do not establish phone behavior.
- Samsung authentication and AOD remain system-owned. Actual wake timing, PIN/fingerprint handoff, AOD coexistence and device background limits require phone testing.

## Clean-room feature plan after upload

1. Identify each APK by SHA-256, package, version, SDK and components with REA. Determine which package actually owns lock-screen rendering, editor, theme definitions and AOD; do not assume the launcher owns them.
2. Trace focused entry points for clock templates, widget data, subject masks, composition order and screen-on/off transitions. Inspect packaged configuration alongside code. Record observations, inferences and unresolved dependencies separately.
3. Produce a behavior-only specification: normalized anchors, scaling, foreground/background order, privacy states, animation timing and cancellation rules. Do not transplant decompiled methods, proprietary assets, fonts or firmware libraries.
4. Compare that specification with Aurora v25 before selecting changes. Prioritize demonstrated gaps in clock/depth composition, editor behavior and wake transitions. Use standard Android APIs with user-selected media and original drawing code.
5. Validate existing presets/materials, save/apply/cancel, redaction, media state and reduced motion with automated checks. Validate secure unlock, notification opening, calls, rotation, battery and AOD on the Samsung phone before claiming compatibility.

## Build repair

The workflow now selects Gradle 8.13 while preserving AGP 8.11.0 and the existing v25 product changes. Android's compatibility table confirms this requirement:
https://developer.android.com/build/releases/agp-8-11-0-release-notes

Local validation results are recorded in the accompanying investigation summary after the run completes. No GitHub push or signed release is implied by a local build.

## Validation results

- Gradle 8.13 / JDK 17: 58 unit tests, zero failures/errors/skips.
- Debug lint report: zero errors, 34 warnings (including drawing allocations, accessibility, API use and app metadata); these remain existing issues to triage.
- Release compilation and vital lint reached packaging, which failed because the local environment has no release signing storeFile. GitHub's existing workflow restores its signing key from secrets; no replacement key was generated and no credentials were accessed.
- Debug validation completed successfully: testDebugUnitTest, lintDebug and assembleDebug (BUILD SUCCESSFUL, 9 seconds on the final incremental run). No Samsung runtime testing or Xiaomi APK behavior is established by these results.
