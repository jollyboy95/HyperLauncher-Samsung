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
