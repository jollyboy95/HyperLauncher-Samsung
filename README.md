# HyperLauncher-Samsung

Project to investigate adapting Xiaomi's latest selected HyperOS launcher to a stock Samsung S26 Ultra, with the Xiaomi 18 Pro Max stock HyperOS 4 appearance as the target.

## Implemented

The `compat` Android module provides a separate compatibility inspection app. It imports a donor APK through Android's document picker, verifies its hash, parses ARM64 ELF dependencies without executing them, checks runtime visibility on the phone, and exports JSON. It does not register as a home app or replace the current launcher.

Build workflow: [.github/workflows/compat.yml](.github/workflows/compat.yml). Build instructions and limitations: [docs/COMPATIBILITY_PROTOTYPE.md](docs/COMPATIBILITY_PROTOTYPE.md).

## Validation

Seven ELF parser unit tests pass locally. All four native libraries in launcher 8.01.02.8584 produce the same DT_NEEDED lists as readelf. See [reports/prototype-validation.md](reports/prototype-validation.md). A successful full Android build and phone test are separate milestones.

## Target and unresolved work

The actual launcher base remains `RELEASE-8.01.02.8584-260918-10231931-R`. Its native HyperOS runtime and framework bridges are not implemented by this prototype. There is no working Samsung port, Xiaomi widget support or complete stock theme yet.

[Runtime findings](docs/LATEST_RUNTIME_FINDINGS.md) | [Located library listings](docs/RUNTIME_SOURCES.md) | [Original plan](docs/PORTING.md)

Upstream Xiaomi binaries and third-party modifications retain their ownership and licence terms. They are not included in this prototype.
