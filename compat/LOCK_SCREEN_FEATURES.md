# Aurora lock-screen feature coverage (v27)

Aurora implements lock-screen styles and controls for Samsung, focusing on Xiaomi HyperOS 4 and HyperOS 2 Movie-like aesthetics.

Reference sources checked on 31 October 2026:
- Xiaomi HyperOS 4: https://os.mi.com/ (glass frames, transparent clocks, subtle outlines, customizable signatures).
- Xiaomi HyperOS 2: Movie lock screens, dynamic layered depth ("动态层次感锁屏"), neon wireframe outlines and left vertical poster blocks.
- XimiTime leak report (7 August 2026): https://ximitime.com/hyperos-4-setup-screen-leak-confirms-10-major-new-features-99419/ (HyperOS 4 setup editor with Cancel/Apply pills, signature "点击输入个性签名", and List/Stack/Count notification cards).
- User-provided visual references: HyperOS 4 setup screen, Movie-like Lock Screens ("UP IN THE AIR", neon retriever with capsule date badge, cyan snowboarder block), and HyperOS 1/4 clean horizon & ultra-tall art clocks.

## Included

- Nineteen clock templates:
  - Classic & Editorial: Depth Stack, Horizon, Offset, Editorial, Soft Stack, Split Outline, Poster, Staggered, Rotated, Cinema, Mountain, Magazine, Outline Cinema.
  - HyperOS 4: **HyperOS 4 Glass** (frosted rounded glass frame, top date, glass gradient clock digits and custom signature/motto line).
  - HyperOS 2 Movie-like: **Movie: Up In Air** (top cinematic title + bottom corner accent date & time), **Movie: Neon Dog** (glowing double-stroke wireframe outline + top-right vertical date capsule pill), **Movie: Block** (left-aligned cyan stacked time, day and date poster block), **Classic Left** (HyperOS clean left-aligned bold time + date/weather + signature line), **Ultra-Tall Art** (four full-height vertical condensed numerals with date pill badge).
- Customizable signature ("个性签名"): set personalized mottos ("Never stop exploring", "Stay hungry", etc.) rendered directly into the HyperOS 4 glass frame or classic layouts.
- Customizable Movie Title for Movie poster layouts.
- Neon Wireframe / Double-Stroke material (dual inner/outer stroke with luminous aura glow).
- Graphical Notification Display selector in Editor matching HyperOS 4 (List with 3 bars, Stack with layered lip, Count with pill badge).
- Two-axis clock dragging, pinch resizing, width, vertical stretch, weight, spacing, 12/24-hour time and optional seconds.
- Independent hour/minute materials: filled/translucent, glass-style gradient/rim, outline, gradient and neon wireframe. Custom colour, wallpaper-derived colour and opacity.
- Authentic HyperOS 4 editor styling with top Cancel dark pill, Apply blue capsule button, camera hole-punch preview frame and direct customization.
- Photo depth with focused on-device segmentation, supporting people, pets, vehicles and plants; transparent PNG cutouts for other subjects.
- Muted looping local video wallpapers with lifecycle-based release, static-photo fallback, photo motion and wake fades.
- Optional automatic-on-wake via a user-enabled Accessibility service.
- Saved-design thumbnail gallery: save, swipe, apply and delete local designs.
- List, Stack and Count notification display modes with swipe dismissal, individual × buttons and Clear all.
- Real active Android media sessions with playback controls and artwork.
- Actual battery level, next alarm, and frosted glass flashlight/camera/app shortcuts.

## System boundaries and limitations

- Samsung owns PIN, fingerprint, face authentication, native always-on display and the system lock screen. Automatic wake is optional and requires one-time Accessibility enablement, then Apply in Aurora. It cannot bypass force-stop, revoked access or additional device-specific background restrictions, and has not been verified on the physical S26 Ultra.
- Video backgrounds do not include moving-subject segmentation; no per-frame foreground tracking is implemented.
- Xiaomi's proprietary AI image-to-video service, firmware transition engine, full glass refraction shaders and native HyperIsland integrations are not ported. Photo motion is a local pan; video wallpapers use a video chosen by the user.
- Segmentation is not universal: buildings, arbitrary flowers, complex hair and unusual artwork may require a manually prepared transparent cutout.
- Video decoding depends on the device's supported codecs. Unsupported videos fall back to the photo without crashing.
- Notification and music features depend on Android notification-listener access and the originating application's supported actions.

## Automatic wake setup

1. Install the signed update, then open Aurora once.
2. Long-press the scene, open Features, and select Automatic wake setup.
3. Enable **Aurora automatic lock screen** in Accessibility / Installed apps. If the OS blocks sideloaded accessibility access, use App info / More / Allow restricted settings first.
4. Return to Aurora and tap **Apply**. Lock and wake the phone without manually reopening Aurora.
5. Disable the feature in Aurora (and Apply), or disable the Accessibility service at any time.

This is a user-authorised lock-screen launch helper, not a screen reader. Its XML explicitly disables window-content retrieval and gesture execution and filters events to System UI. Private application broadcasts use non-exported receivers and a signature-protected fallback on older Android releases. The Accessibility service listens only to protected system broadcasts.

## Verification

Framework launch/resume/draw tests, all template/material combinations, editor/gallery interactions, notification redaction and stack expansion, notification actions, real media-controller routing, media visibility/expiry and player switching, snapshot numeric-type round trips and preservation of existing clock settings are covered by automated tests. Dismissal tests exercise actual notification-service cancellation, protected items, disconnected access, buttons and swipes inside a scrolling list. Automatic-wake policy, lifecycle/broadcast launches, legacy receiver compatibility, limited service capabilities, draft isolation and blur math are also covered. Local visual QA uses the supplied pug wallpaper, which is not committed to the public repository.

Version 24 adds an upper-subject inference pass for small details such as ears and hair, merging it with the body mask. Older automatic masks regenerate with the new depth-v5 cache format.

## v27 editor redesign

- Large style gallery with Customize and My designs entry points; existing choices are preserved when customising the current style.
- Separate editing view with rounded live preview, expandable controls, horizontal category navigation and selected states.
- Light/dark surfaces, grouped controls, readable slider values and consistently sized touch targets.
- Dedicated notification and About pages. Draft isolation and Apply/Cancel behavior remain.
- Portrait, compact landscape and API 28/35 paths have framework tests. The available emulator cannot boot because its system image is absent; physical Samsung behavior is unverified.
- This is an original HyperOS-inspired UI reconstruction, not a claim of pixel-identical HyperOS 4 parity. Proprietary fonts, assets, shaders and Samsung-owned authentication/AOD are not replaced.
