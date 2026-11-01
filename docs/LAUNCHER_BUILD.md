# HyperLauncher: independent launcher direction

The product is now a source-built launcher inspired by Xiaomi's home-screen features. The Xiaomi 8.x native APK and older ports are references, not executable dependencies.

## First preview implementation

Base: Lawnchair 15-dev, pinned at 505dbc40e6154c05158b5d0271c45f6a885a411b, with its pinned SystemUI submodule. All upstream licence notices remain in the fetched source. Original HyperLauncher additions use Apache-2.0 notices.

- Separate application ID: au.steve.hyperlauncher.debug.
- App label: HyperLauncher Preview; custom H icon.
- Default 5-column, 6-row workspace, four-icon dock, squircle icon masks, translucent dock background, no dock search bar, no automatic newly-installed app shortcuts.
- Inherited app drawer, folders, search, icon pack support and standard Android widget binding, configuration, resize and persistence.
- Original Hyper Clock widget (time/date, taps open alarms).
- Original Hyper Battery widget (snapshot, 30-minute platform schedule plus power connection changes). Battery percentage is not continuously live; Android controls update timing.
- Original Hyper Weather widget with current conditions and today's high/low. City search, Celsius/Fahrenheit and cached offline readings use the free Open-Meteo API (CC BY 4.0); no API key, account or root access is needed.
- Original Wallpaper & style studio. Six on-device gradient wallpapers are rendered at screen resolution, with optional lock-screen application and a shortcut to the system wallpaper picker. No downloaded artwork is bundled.
- Fresh 5x6 installs place Hyper Clock (5x2) and Hyper Battery (2x2) on the first home screen. Existing databases are never rewritten. The weather card is added from the widget picker after a city is chosen.
- Wallpaper-derived accent colour by default, translucent white folder surface, single-line app-drawer labels, hidden drawer scrollbar and wider drawer margins.
- Animated, card-based Hyper settings hub with working links to home layout, icons, drawer, folders, dock, gestures, backup/restore, wallpapers and weather.
- No Xiaomi runtime, root or Shizuku required for the intended normal launcher flow.

These are implemented source changes and inherited capabilities. Actual Samsung behavior still requires testing. Samsung retains system Recents and system app-transition integration; QuickSwitch/root integration is not a requirement for this project.

## Not yet implemented

A complete Xiaomi stock theme/icon pack, multi-day weather forecast, large folders, stacked widgets and exact HyperOS system animations. Xiaomi-exclusive widgets are not automatically portable. The current widgets are original recreations, not extracted Xiaomi binaries or artwork.

The separate `feature/aurora-lockscreen` branch contains the Aurora HyperOS-inspired lock-screen preview. It does not replace Samsung Keyguard, and Samsung continues to own fingerprint, face, PIN, Always On Display and system lock-screen authentication. No root is used there either.

## Reproducible source

launcher/upstream.json pins the donor. scripts/prepare_launcher.py checks the commit and a clean checkout, applies exact-count patches, copies the original widget overlay and validates XML. Run it once on a fresh checkout. It disables automatic upstream build-scan publication. The new GitHub workflow builds a real home app; the older compat module remains a separate diagnostic tool.

    git clone https://github.com/LawnchairLauncher/lawnchair.git work/launcher
    git -C work/launcher checkout 505dbc40e6154c05158b5d0271c45f6a885a411b
    git -C work/launcher submodule update --init --recursive
    python scripts/prepare_launcher.py work/launcher
    cd work/launcher
    ./gradlew assembleLawnWithQuickstepGithubDebug

Toolchain: JDK 21, upstream Gradle wrapper, Android platform 36.1 and build tools 36.1.0.

## Phone validation after a successful build

Keep Nova installed. Open HyperLauncher Preview and choose it as Home only when ready. Test app launch/drawer, create a folder, add a standard widget requiring configuration, resize it, and verify it survives restarting the launcher. Check the fresh-install Hyper Clock and Hyper Battery cards, add Hyper Weather from the widget picker, choose a city in Settings → Weather and wait for a reading. Apply a wallpaper from Settings → Wallpaper & style. Test gesture navigation through Samsung's Recents. A failed flow should include its exact action and screenshot or logcat.

## Home screen and settings follow-up

The normal settings entry opens an original Xiaomi-inspired card hub. Every row links to a functioning Lawnchair preference destination; deeper preference pages retain their existing design. Fresh 5x6 layouts use a four-app dock (dialer, messaging, browser, camera) and place Hyper Clock and Hyper Battery in the upper rows. Existing home databases are preserved. The preview build uses the `.debug` application id, so the default widget components are declared as `au.steve.hyperlauncher.debug`; a release build needs the same provider id adjustment. Clock and battery card backgrounds have compact heights instead of stretching to the full host allocation. The battery remains a snapshot.

Weather data and city search use Open-Meteo over HTTPS. The launcher already holds the INTERNET permission inherited from Lawnchair, and the widget caches the last successful reading for offline display. Open-Meteo attribution is visible on the card and in Settings → About. The gradient wallpapers are drawn locally, so there are no image-licence or download concerns.
