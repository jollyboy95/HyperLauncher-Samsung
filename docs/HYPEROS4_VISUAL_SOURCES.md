# HyperOS 4 visual sources — 1 November 2026

## Preferred reference

https://hyperos.mi.com/ — Xiaomi's official China HyperOS 4 feature page. Search-indexed text explicitly describes stacked lock-screen notifications and swiping upward to browse, flexible clock adjustment, soft-light glass, environment-aware colour and content-adaptive transparency. Model and software restrictions apply. The web reader timed out, but the page subsequently loaded in the in-app browser. Its live accessibility text confirms those descriptions. Embedded demo media reported unable to play; no frame-level visual measurements were made in this pass.

## Supporting visual walkthroughs

- https://www.youtube.com/watch?v=R9Kz_IWm9NU — Cj Porto tech, 25 August 2026. Published chapters identify clock resizing (01:32), depth (02:15), saved styles (04:55) and notification menu (06:46). These are indexed chapter metadata, not personally inspected video frames.
- https://www.youtube.com/watch?v=nv2h1YDtdAE — Vikas Pal, 29 August 2026. Describes glass and stacked notifications. Independent demonstration; its enablement/modification steps are not an authoritative stock specification.
- https://www.youtube.com/watch?v=drH3fgJ-BMc — TECH MUKUL, 15 August 2026. Short demonstration labelled glass and stacked notifications; early material requires comparison with the official release.
- https://nokiapoweruser.com/xiaomi-hyperos-4-lock-screen-notification-menu-leak-list-stack-count/ — early notification-mode leak. Search metadata supports List/Stack/Count; direct access returned 403. Treat as historical reporting, not a current definitive settings specification.

## Implementation direction

Aurora should use one consistent visual language across its own settings, editor, notifications and media: wallpaper-aware glass with readable text, rounded grouped controls, compact headings, clock/material previews and clear selected states. These are design goals, not measured Xiaomi dimensions. The uploaded Theme Manager and AOD APKs provide additional static editor evidence in REA_REFERENCE_FINDINGS.md. Samsung's actual system Settings app remains outside Aurora's UI.

The full settings redesign is outstanding. This patch addresses the demonstrated stack gesture and notification filtering, not complete visual parity. Use official visual frames and the supplied editor metadata before claiming pixel-level similarity.

## v26 behavior changes

- Swipe upward on the collapsed notification stack to expand the existing scrollable list. Tapping Expand still works. Horizontal dismissal remains a separate gesture; cancelled vertical gestures do not expand.
- Respect available Android ranking visibility overrides, blocked/minimum importance and suspended status. Hidden items are removed on ranking refresh.
- Suppress routine service/foreground notifications and low-importance system-category housekeeping from Android/System UI/Settings. Do not blanket-block all system packages; important system alerts and call/alarm categories remain eligible (subject to visibility/importance).
- Notification filtering affects Aurora's display only. It does not disable the system notification or cancel hidden notices.
- Stock Samsung may apply additional vendor policies not exposed through notification-listener ranking; physical phone verification remains required.

Android ranking API source: https://developer.android.com/reference/android/service/notification/NotificationListenerService.Ranking

## Additional repositories supplied by the user

### HyperChanger

- Supplied copy: https://github.com/ringmembranouslabyrinth195/HyperChanger — inspected commit 550b35029bd96d228394b55129397e3bf42283ca.
- Original project referenced by its Chinese README: https://github.com/ColdP/HyperChanger — current main reported c84c224b2285590604e9e30e896a9a379e07c293 during this review.
- Source files inspected: HookSettings.kt, HyperSystemUiModule.kt and the relevant settings entries in MainActivity.kt. This is ordinary repository source review; no REA binary observation is implied.
- Useful behavior model: independent lock-screen notification/media/focus material controls; opacity, tint and blur; advanced glass controls for light/refraction/edges; shortcut background styles and charging-text control. These are third-party module options, not proof of stock HyperOS settings appearance.
- Hooks explicitly reference Xiaomi MiBackgroundStyle, MiBlurCompat, MiGlassCompat, MiuiShortcutController and depth-avoidance classes. They require Xiaomi internals and LSPosed/root; they are not directly usable in stock Samsung Aurora.
- The depth hook changes an image threshold and bypasses hierarchy avoidance. Treat this as modified behavior, not the native reference for legibility policy. Aurora should preserve deliberate legibility safeguards.
- Provenance/licensing discrepancy: the supplied copy has an MIT license file, while the referenced original currently has AGPL-3.0. No source was copied or integrated; resolve provenance before any reuse. This review does not establish the supplied copy's right to relicense every file.
- Aurora design direction: separate normal/media card material settings if needed, consistent glass shortcut controls, and grouped visual controls. Implement independently with Android drawing APIs and original assets; do not introduce Xiaomi reflection hooks.

### Mods-Center HyperOS-AOD

https://github.com/Mods-Center/HyperOS-AOD/releases/tag/V3

- Published V3 asset SHA-256 is 8aa9c7220bba9e23b5cc46e86c7dbecb95f163f4546cc5fd1bba84f837349526 — exactly matches the user's uploaded ZIP analysed earlier.
- The repository tree contains a README, not the app's implementation source. The release is a Magisk distribution of modified Xiaomi AOD.
- Maintainer release notes claim additional depth/magazine, animal, AI wallpaper and large-clock options; increased template capacity; and transition changes. These remain maintainer claims unless independently confirmed by static/runtime evidence.
- The February 2026 notes explicitly remove forced full-screen AOD support following bootloop concerns. “All devices” in release wording does not prove Samsung compatibility.
- Useful for mapping styles and studying the already supplied binary, not a portable AOD implementation or a complete HyperOS 4 UI specification. No new download, execution or installation was needed.

No Aurora code change or new APK was required for this source review. v26 remains the latest validated GitHub-built release; complete settings restyling remains outstanding.

### Direct review of ColdP/HyperChanger

Reviewed original commit c84c224b2285590604e9e30e896a9a379e07c293 independently of the earlier copy. The original has substantially more settings/features. Its app/build.gradle.kts uses Miuix UI, preferences, icons and blur libraries at version 0.9.4, with Compose. MainActivity uses MiuixTheme and light/dark/dynamic palette choices. NotificationStackBlurSettings contains a configurable stack-depth blur policy; this is custom module behavior, not evidence of stock Xiaomi scroll physics. LockscreenWidget targets Xiaomi clock DataUtils; SettingsAppearanceSettings concerns modification of Xiaomi Settings and should not be confused with the module's own settings UI.

The most actionable independent dependency for a Samsung-compatible settings redesign is https://github.com/compose-miuix-ui/miuix — an Apache-2.0 Compose UI library. Its components can be evaluated separately from HyperChanger's AGPL application code and Xiaomi hooks. Aurora currently uses Java Views, so adopting it would require a deliberate Kotlin/Compose settings migration and GitHub build/interaction validation. No dependency or application code was changed during this review.

## User-supplied visual references added during v27

- https://ximitime.com/3-hyperos-lock-screen-setups-to-make-your-xiaomi-devices-feel-high-end-81883/ — dated 5 February 2026. Discusses neon depth, natural macro/vertical clock and illustrated outline-clock compositions. The displayed hero photograph was inspected: a tall stacked lavender clock, right-side date and a flower overlapping the digits. It is general HyperOS styling evidence, not a complete HyperOS 4 settings specification. The photograph is not bundled in Aurora.
- https://mobileosupdates.com/apps/com.miui.aod — mirror listing reports DEV-2455.1.0.1-09282249, versionCode 22455101, updated 15 October 2026. Seven listed screenshots were inspected as visual references: large style carousel, Customize entry, separate wallpaper/filter/depth editing, and grouped AOD settings. Screenshot build dates are not established by the listing. Mirror authenticity statements were not independently verified. No APK from this listing was installed or incorporated.
- https://mobileosupdates.com/apps/com.android.deskclock — listing reports 18.26.0, versionCode 1306002600, updated 7 September 2026. This is an alarm/clock reference, not the keyguard editor. Mirror package claims are not a Samsung compatibility result.

## v27 editor implementation and fidelity boundary

The editor now starts with a horizontally browsable large style gallery and Customize action. The detail screen uses a large preview, an expandable controls sheet, horizontally scrollable categories, themed grouped controls, explicit selection states, and light/dark surfaces. Notifications and About have dedicated pages. Android status/navigation/cutout insets and compact windows are accommodated. Existing artwork/templates, draft settings and notification fixes remain.

This is original Java/View implementation, not an import of HyperChanger's AGPL implementation or Xiaomi resources. Miuix/Compose adoption was evaluated as a possible migration but is not a dependency of this change. Exact proprietary typography, shaders, Xiaomi's system AOD/PIN/fingerprint screens and every device-specific behavior are not reproduced. The external screenshot sets do not define one complete, version-matched HyperOS 4 specification, so pixel-identical parity cannot be asserted.

Local emulator attempt: the configured Pixel 4 API 31 AVD cannot boot because its system-image directory is missing. Rendered Android framework tests provide visual and interaction checks; no emulator/phone runtime result is claimed.

### Firmware links supplied during the editor work

SourceForge's mutable latest-download URL was checked using HTTP headers only. At this review it resolved to xiaomi.eu_SHENNONG_OS3.0.310.0.WNBCNXM_16_a59f1c0fda.zip, 7,063,774,214 bytes. It is not a HyperOS 4 reference, and its cached website listing differed from the live redirect. No ROM body was downloaded.

The supplied Aliyun OTA link for popsicle-ota_full-OS4.0.5.0.XPBCNXM-user-17.0-03fc15795d.zip was inspected with bounded HTTP range reads. Its declared archive size is 9,172,351,158 bytes. ZIP central-directory inventory contains seven entries, including payload.bin and Android OTA metadata; there are no directly accessible APK members in the outer ZIP. Metadata identifies Xiaomi/popsicle/popsicle Android 17, SDK 37, incremental 17OS4.0.261020.223031081.QCPECN.S, security patch 2026-10-01. These are observed metadata declarations, not signature validation. The multipart ETag is not treated as SHA-256. System UI APK extraction and full OTA verification remain pending; the 9 GB body was not downloaded. Metadata-only inventory is saved privately for further investigation.

A temporary signed parts-download link supplied by the user returned HTTP 403; its expiry parameter was already past (1 November 2026, 07:53:31 Australia/Sydney). The signed query is intentionally omitted from public notes. A fresh link is needed to inspect that archive.

Final local v27 checks: 66 tests passed with zero failures/errors/skips; debug APK assembled; lint passed with warnings. Eight light/dark/compact rendered framework previews were inspected. No physical phone result is implied.
