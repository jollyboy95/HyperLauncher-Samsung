# HyperLauncher-Samsung

Project to investigate adapting Xiaomi's latest selected HyperOS launcher to a stock Samsung S26 Ultra, with the Xiaomi 18 Pro Max stock HyperOS 4 appearance as the target.

## Launcher preview

The `feature/hyperos4-integration` branch builds an independent HyperOS 4-inspired home app from the open-source Lawnchair launcher (Apache-2.0). It does not include or execute Xiaomi binaries and needs no root or Shizuku.

- 5-column, 6-row workspace with a four-app dock, squircle icons and wallpaper-derived accent colour.
- Original Hyper Clock, Hyper Battery and Hyper Weather widgets. Clock offers Stacked, Centred and Mono card styles with a live preview and date toggle. Weather uses the free Open-Meteo API (CC BY 4.0), supports city search and Celsius/Fahrenheit, and caches its last reading offline.
- Fresh installs place Hyper Clock and Hyper Battery on the first home screen.
- Wallpaper & style studio with six locally rendered gradients, optional lock-screen application and a system-picker shortcut.
- Animated card settings hub linking to working home, icon, drawer, folder, dock, gesture, clock, weather, wallpaper and backup preferences.
- Inherited app drawer, folders, search, icon packs and standard Android widget binding, resizing and persistence.

Build workflow: [.github/workflows/launcher.yml](.github/workflows/launcher.yml). Architecture, defaults and phone-test notes: [docs/LAUNCHER_BUILD.md](docs/LAUNCHER_BUILD.md).

The separate `feature/aurora-lockscreen` branch contains the Aurora HyperOS-inspired lock-screen preview. It does not replace Samsung Keyguard; fingerprint, face, PIN and Always On Display remain OS-owned.

## Compatibility prototype

The `compat` Android module provides a separate compatibility inspection app. It imports a donor APK through Android's document picker, verifies its hash, parses ARM64 ELF dependencies without executing them, checks runtime visibility on the phone, and exports JSON. It does not register as a home app or replace the current launcher.

Build workflow: [.github/workflows/compat.yml](.github/workflows/compat.yml). Build instructions and limitations: [docs/COMPATIBILITY_PROTOTYPE.md](docs/COMPATIBILITY_PROTOTYPE.md).

## Validation

Seven ELF parser unit tests pass locally. All four native libraries in launcher 8.01.02.8584 produce the same DT_NEEDED lists as readelf. See [reports/prototype-validation.md](reports/prototype-validation.md). A successful full Android build and phone test are separate milestones.

## Target and unresolved work

The actual launcher base remains `RELEASE-8.01.02.8584-260918-10231931-R`. Its native HyperOS runtime and framework bridges are not implemented by this prototype. There is no working Samsung port, Xiaomi widget support or complete stock theme yet.

[Runtime findings](docs/LATEST_RUNTIME_FINDINGS.md) | [Located library listings](docs/RUNTIME_SOURCES.md) | [Original plan](docs/PORTING.md)

Upstream Xiaomi binaries and third-party modifications retain their ownership and licence terms. They are not included in this prototype.
