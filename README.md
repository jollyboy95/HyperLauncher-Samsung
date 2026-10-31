# HyperLauncher-Samsung

The `feature/aurora-lockscreen` branch ships **Aurora Depth**, a HyperOS 4-inspired lock-screen preview for Samsung. Install the signed APK from GitHub Actions; it does not replace Samsung Keyguard.

## Download

1. Open [Actions → Build Aurora Depth preview](https://github.com/jollyboy95/HyperLauncher-Samsung/actions/workflows/compat.yml).
2. Open the latest green run on `feature/aurora-lockscreen`.
3. Download the `AuroraDepth-signed-release` artifact.

Application ID `au.steve.hyperlauncher.compat` is unchanged so updates install over previous Aurora builds.

Build workflow: [.github/workflows/compat.yml](.github/workflows/compat.yml). Launcher-inspection notes remain in [docs/COMPATIBILITY_PROTOTYPE.md](docs/COMPATIBILITY_PROTOTYPE.md).

## Validation

Seven ELF parser unit tests pass locally. All four native libraries in launcher 8.01.02.8584 produce the same DT_NEEDED lists as readelf. See [reports/prototype-validation.md](reports/prototype-validation.md). A successful full Android build and phone test are separate milestones.

## Target and unresolved work

The actual launcher base remains `RELEASE-8.01.02.8584-260918-10231931-R`. Its native HyperOS runtime and framework bridges are not implemented by this prototype. There is no working Samsung port, Xiaomi widget support or complete stock theme yet.

[Runtime findings](docs/LATEST_RUNTIME_FINDINGS.md) | [Located library listings](docs/RUNTIME_SOURCES.md) | [Original plan](docs/PORTING.md)

Upstream Xiaomi binaries and third-party modifications retain their ownership and licence terms. They are not included in this prototype.
