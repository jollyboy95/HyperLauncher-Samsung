# Supplied lock-screen references — 1 November 2026

## Result

REA successfully inspected all three supplied APKs and the AOD APK extracted from the ZIP. Apktool 3.0.3 decoded the standalone AOD and Theme Manager resources successfully. Nothing from the references was executed or copied into Aurora. The ZIP's install scripts were read as data only.

The existing Aurora v25 branch is the implementation baseline. The build-only correction is commit `258baa2`: GitHub Actions now uses Gradle 8.13, required by Android Gradle Plugin 8.11.0. No product feature changes are attributed to this investigation.

## Artifact identity

| File | Package | Version | Minimum / target SDK |
| --- | --- | --- | --- |
| base.apk | com.appiolab.hyperlockscreenos | 0.1.2 (13) | 32 / 36 |
| Xiaomi AOD APK | com.miui.aod | DEV-2437.1.0.1-07272237 (22437101) | 33 / 36 |
| Theme Manager APK | com.android.thememanager | 11.5.3.7 (11537) | 34 / 35 |
| MIUIAod.apk from module | com.miui.aod | DEV-2312.2.0.1-11172116 (22312201) | 33 / 35 |

`base.apk` is a third-party Hyper Lock Screen app, not Xiaomi System UI or a Xiaomi launcher. No Xiaomi System UI APK or launcher APK was supplied in this batch. That limits conclusions about the live secure lock-screen host.

The ZIP identifies itself as HyperOS AOD Mod v3.0 and contains a different, older AOD build than the standalone APK. Its file inventory includes nine pet photo/mask pairs, an overlay APK, installer binaries and a privileged-permission XML. These assets were not imported. An APK's package name or version does not authenticate its publisher; reference signatures were not verified.

## Findings and evidence

All findings below are static observations unless marked as inference. Evidence IDs refer to this REA session; retained local JSON is private analysis material, not repository source.

### AOD is integrated with Xiaomi System UI

AOD manifest decoding reports `android.uid.system`. Its DozeServicePluginImpl initializes separate System UI and plugin contexts and checks the plugin manager version. Its inventory exposes DozeServicePlugin.RequestDoze, system-UI actions, dreaming callbacks and linkage-animation state.

Inference: this build expects Xiaomi host contracts rather than providing a standalone Samsung AOD replacement. Samsung runtime compatibility has not been tested. The module's privileged-permission configuration does not establish those contracts on Samsung.

Evidence: AOD package parent `ev_79d7a679294987b540d5b848e867a31f52c7dc72939973d182422bcdafe8970c`; resource decode `ev_dd01086fdc71e7733a1f62a9e0d234816fa939750b81fdd8d012072126885207`; plugin inventory `ev_93ecdff19c1b601e446aaf5d558fae1c0dba9880262649da5cff5d85ae204eda`; onCreate `ev_21ab26cce411eb9c225780f0ee83a45385496de9e18e640a32ef76bab15a4c41`.

### The editor has explicit clock and wallpaper state

Theme Manager contains a keyguard editor with template/history storage, separate wallpaper configuration and clock configuration. ClockInfo metadata includes clock top/bottom ratios, time width/height, weight, depth type, hollow style, glass transparency, separate primary/secondary colour settings, automatic colour flags and signature lines. This confirms configurable properties, not the exact semantics or visible availability of every field.

Theme Manager's manifest declares a theme shared UID. Resource decoding also confirms the editor and depth-download service declarations. These are Xiaomi components, not APIs Aurora should depend on.

Evidence: editor class search `ev_f6c380b6dd856424d623b927abd3004bc71b39a95053e0a48b6893584fed168a`; ClockInfo inventory `ev_ee1544de4f1cd5f43a2c91c536d5b2c43decb54fc27a1787d071cdfd958a81a3`; resource decode `ev_7c74b1e84e5e64546d2d122ac059b824108c7d96a85d0e3f629ca1e2061340fa`.

### Depth includes layout-dependent exclusion regions

ClockDepthAvoidRuleUtils computes a region within clock bounds according to clock region and single/double-line layout. Its empty-bounds path returns an empty rectangle. This supports a layout-aware depth policy. It does not establish a universal occlusion threshold or prove which live templates call it. A base gallery clock's getDepthAvoidRect returns empty; one inspected pet-clock overload only forwards to its superclass. These negative results are retained rather than presented as working depth implementations.

Aurora currently removes foreground-covered pixels from its clock layer using its own foreground mask. It already supports some outline drawing above the subject. A useful next change is a legibility safeguard, designed and tested independently rather than reproducing Xiaomi's rectangle formula or constants.

Evidence: rule inventory `ev_11e800c6cb0f1a3b3207c6fd434c83d8b1a1c5460693f9bfd6db126a9d7d2ba1`; computeAvoidRect overload 1 `ev_0ad6104014181b9f6db3f733045e29a25bbbb5a5c36f0fe9e7b6bd09a05b8ef7`; base empty result `ev_8cd388ee6d363c199d57341caf1815fa1ad96eba9ffd68e9368f7243714810b0`; forwarding method `ev_9445e4921aa0936e87563da148927cd7b61a595a335b206dadce2777c5b45500`. Aurora source: AuroraSceneView.onDraw at v25.

### Editor transitions coordinate separate views

An inspected forward-transition method receives lock and home template views and coordinates an outline element with a transition consumer. It waits for a missing outline element rather than establishing a complete transition immediately. Exact animation timing, easing, cancellation and secure-unlock transitions remain unresolved; this is editor-transition evidence only.

Evidence: TransitionPerformerImpl inventory `ev_286579441ec56c710987c51fe68ff1bbdc6f74072b53f498926f2aca7e791544`; forward method `ev_e3299552ca4e2215233e90a17602ba1fc5a703531f70e036800d730b8f51d202`.

### Widget data and downloadable depth video are separate concerns

AOD's weather controller cancels previous work, tracks request identity and schedules a timeout. Its selected method does not identify the actual weather provider. The pet-clock base exposes health and weather JSON members; their real service availability on Samsung is unknown.

Theme Manager's depth preset manager has an IVideoDownloadService field and a depth-download action. Its inspected pause method passes video identity and download metadata to that service. This confirms a download service interaction, not an on-device video segmentation implementation. AI failure constants alone do not prove segmentation capability on Samsung.

Evidence: weather inventory `ev_da20bacfefb72d9d5d1be76f84cab58930dbde1f2356e00b4e961548e84b80a0`; fetch method `ev_74b3e9e821c62823b59e28f7ede1b497309e4c80fefbfa86b725b089af62dd22`; pet base `ev_e27805f3f9b4334188af5e8cbd4a25693ddd60c02c75a95a635bcaa3658dc1cf`; download manager `ev_63ea9798a5a511a44c9098a8bf0667111cce05f9f30248a0d08f9a36a93cda14`; pause method `ev_9bb6f6809b23cafb2a3e7ac4b1c5315d9bb7ae7601919ff86a78a61dbfd864ed`.

### Third-party base.apk uses its own service model

Its manifest declares overlay and special-use foreground-service permissions. Its lock-screen service onCreate registers screen-on/off events and checks device lock state. This distinguishes its model from Xiaomi's System UI plugin. It does not demonstrate reliable Samsung wake behavior or secure authentication. Aurora retains Samsung's system authentication rather than adopting the app's password/pattern screens.

Evidence: package parent `ev_a87936a49ccc5a1e65f6d7f0a99dcb9363b9fe5f46ad10a0507c9cb8cc364380`; service inventory `ev_5f88d08d1ea07331165da54fff3448c137b463666ea1eb4109205d10dfed2565`; onCreate `ev_f3589fd985c039b0bbed826d931c0f388d94f9a11384059924780c781dd6ece9`.

## Clean-room implementation backlog

These are proposed Aurora designs, not copied Xiaomi implementation details:

1. **Depth legibility:** evaluate coverage of the actual rendered time glyphs, separately from decorative text; offer an explicit “keep time readable” control and preserve existing depth preferences. Define original thresholds using Aurora fixtures. Acceptance: time remains readable with large subjects, transparent cutouts, stacked/horizontal clocks and changed wallpaper crops; no wallpaper-lighting seam or per-frame segmentation.
2. **Editor consistency:** store user modifications separately from automatic wallpaper colour choices; preserve separate hour/minute materials and saved looks. Acceptance: Apply/Cancel, rotation and upgrade keep user choices without changing privacy settings. Audit what v25 already covers before adding fields.
3. **Transition lifecycle:** specify a common cancellable editor transition across wallpaper, clock and frame. Respect reduced motion and release animation/video work when paused. Acceptance: rapid preset switching, editor exit and screen-off leave no stale animation or blank frame. Do not claim Xiaomi secure-unlock animation parity.
4. **Widget providers:** retain Android battery, alarm and media sources; weather requires an explicitly selected independent provider and consent. Acceptance: revoked access, stale results and timeout have clear fallback states; no dependency on Xiaomi health/weather services.
5. **Video depth:** retain local video playback with photo fallback. Moving-subject tracking needs a separate performance and quality prototype; downloaded Xiaomi video packs and native services are not part of Aurora.

## Validation and remaining boundaries

- Local baseline: 58 tests passed, zero failures/errors/skips; debug build succeeded; lint had zero errors and 34 warnings.
- GitHub Actions run 41239091466 at commit 258baa2 completed successfully, including tests, lint, release signing and artifact upload: https://github.com/jollyboy95/HyperLauncher-Samsung/actions/runs/41239091466
- Downloaded release: AuroraDepth-v25-1.4.0-hyperos4-signed.apk. Android apksigner verification succeeded. Its signing certificate matches the existing v24 APK (SHA-256 9aaec8e03e10ae2b5bc37ef5e9dd55e6e778a0e508ba2e4c9c97b8784c657ce3).
- This release contains the existing v25 features plus the CI repair, not the proposed backlog above.
- No physical Samsung test, live Xiaomi execution, native AOD replacement, moving-video segmentation or comprehensive widget/launcher analysis was completed. Launcher/home-grid work is deferred under the user's lock-screen priority.

## SHA-256 reference identities

- base: `132e35a8fd6105a621eca56910e39cacdfc15369e1063a5bac09c33000f2215e`
- aod: `8a952db4c2733c47186b6e0833e4149d79ff8298e70d85e2967ec34a4ca5e504`
- themes: `9c8a971d286e9d7494d739ee747bbbf3c5c4868145df8bb65baf244df12ee96d`
- module: `2a7223b6bea3e009406548954b68f733e1fd0c4ba77e451bc7a06a32af40b117`
- module ZIP: `8aa9c7220bba9e23b5cc46e86c7dbecb95f163f4546cc5fd1bba84f837349526`
- GitHub v25 release: `5e7feccf5933c7c01d584962005cb10c000c6a767f860f5e45ef0190185baab4`
