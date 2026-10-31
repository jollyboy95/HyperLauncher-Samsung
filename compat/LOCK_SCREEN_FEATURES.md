# Aurora lock-screen feature coverage (v23)

Aurora implements lock-screen styles and controls for Samsung. It does not install Xiaomi firmware or claim complete native HyperOS parity.

Reference sources checked on 31 October 2026:
- Xiaomi HyperOS 4: https://os.mi.com/ (clock scaling, position, typography and glass effects; device restrictions apply).
- Xiaomi global feature overview: https://www.mi.com/global/hyperos/ (cinematic lock screens, dynamic wallpapers and personalised editing; this page describes HyperOS 3 and later).
- XimiTime leak report (7 August 2026): https://ximitime.com/hyperos-4-setup-screen-leak-confirms-10-major-new-features-99419/ (secondary reporting; transparent clocks and layered glass notifications, not a complete native API specification).
- User-provided visual references: mixed filled/outline clocks, staggered and rotated layouts, vertical dates and saved designs.

## Included

- Thirteen clock templates: Depth Stack, Horizon, Offset, Editorial, Soft Stack, Split Outline, Poster, Staggered, Rotated, Cinema, Mountain, Magazine and Outline Cinema. Magazine supports a custom headline.
- Two-axis clock dragging, pinch resizing, width, vertical stretch, weight, spacing, 12/24-hour time and optional seconds.
- Independent hour/minute materials: filled/translucent, glass-style gradient/rim, outline and gradient. Custom colour, wallpaper-derived colour and opacity.
- Short/full/numeric/hidden dates; Poster includes a vertical weekday. Mixed templates can put outlines above or behind the subject.
- Photo depth with focused on-device segmentation, including supported VOC people, pets, vehicles and plants; transparent PNG cutouts for other subjects.
- Muted looping local video wallpapers with lifecycle-based release, static-photo fallback, photo motion and wake fades. Static depth/glass are not applied over video.
- Preview-first editor with phone-shaped preview, template thumbnails, Apply/Cancel drafts and reduced-motion support.
- Cached wallpaper blur, softly tinted notification cards and decorative collapsed-stack layers.
- Optional automatic-on-wake via a user-enabled Accessibility service. It receives protected wake broadcasts and System UI window-state events, requests no window content or gesture capability, avoids unlocked use/calls/duplicates, and uses a transient task that closes after system authentication.
- Saved-design thumbnail gallery: save, swipe, apply and delete up to eight local designs. Privacy settings are not part of a design.
- List, Stack and Count notification display modes with expand/collapse, bounded scrolling, open after authentication, horizontal swipe dismissal, individual × buttons and Clear all for clearable notifications (including beyond the display limit) and locked-content redaction.
- Real active Android media sessions: title/artist/artwork, previous/play-pause/next and seeking where supported. Notification access is required. Media details follow the notification privacy setting.
- Actual battery level and next alarm, plus configurable flashlight/camera/alarm/phone/settings shortcuts. Locked camera uses the secure camera intent.
- Existing settings and clock position are preserved on update. Selecting a different template or resetting the editor is an explicit change.

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

Framework launch/resume/draw tests, all template/material combinations, editor/gallery interactions, notification redaction and stack expansion, notification actions, real media-controller routing, snapshot numeric-type round trips and preservation of existing clock settings are covered by automated tests. Dismissal tests exercise actual notification-service cancellation, protected items, disconnected access, buttons and swipes inside a scrolling list. Automatic-wake policy, lifecycle/broadcast launches, legacy receiver compatibility, limited service capabilities, draft isolation and blur math are also covered. Local visual QA uses the supplied pug wallpaper, which is not committed to the public repository.
