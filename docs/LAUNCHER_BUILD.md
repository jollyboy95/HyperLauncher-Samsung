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
- No Xiaomi runtime, root or Shizuku required for the intended normal launcher flow.

These are implemented source changes and inherited capabilities. Actual Samsung behavior still requires testing. Samsung retains system Recents and system app-transition integration; QuickSwitch/root integration is not a requirement for this project.

## Not yet implemented

A complete Xiaomi stock theme/icon pack, weather widget/data service, large folders, stacked widgets and exact HyperOS animations. Xiaomi-exclusive widgets are not automatically portable. The current widgets are original recreations, not extracted Xiaomi binaries or artwork.

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

Keep Nova installed. Open HyperLauncher Preview and choose it as Home only when ready. Test app launch/drawer, create a folder, add a standard widget requiring configuration, resize it, and verify it survives restarting the launcher. Add Hyper Clock and Hyper Battery from the widget picker. Test gesture navigation through Samsung's Recents. A failed flow should include its exact action and screenshot or logcat.

## Home screen and settings follow-up

The normal settings entry opens an original Xiaomi-inspired card hub. Every row links to a functioning Lawnchair preference destination; deeper preference pages retain their existing design. Fresh 5x6 layouts use a four-app dock (dialer, messaging, browser, camera) and reserve the upper rows for manually added widgets. Existing home databases are preserved. This does not yet supply the Xiaomi stock icon pack, wallpaper, weather service, or automatic clock placement. Clock and battery card backgrounds now have compact heights instead of stretching to the full host allocation. The battery remains a snapshot.
