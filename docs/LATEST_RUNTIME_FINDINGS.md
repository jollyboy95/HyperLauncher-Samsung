# Latest launcher runtime findings

## Verified locally

The selected 8.01.02.8584 APK has no classes*.dex and declares android:hasCode="false". It is a Flutter/native payload, not a conventional Java/smali launcher. The manifest requires hyperos.rustruntime.v5 (required=true).

ELF inspection found 36 Xiaomi-specific direct dependencies absent from the APK: 34 libhyper_os_* libraries plus libmisqlite3.so and librust_maml_sdk.so. These include the Flutter runtime, app widgets, UI, system, security and resources bridges. The report also lists ordinary Android dependencies; those are not counted as Xiaomi libraries.

The bundled Flutter version metadata is 3.38.3-mi-1.17.24-beta1. No editable Dart or Rust application source was identified. The APK includes compiled Flutter assets and native binaries.

## Consequences

Changing uses-library to optional would at most remove one installation prerequisite; it does not supply the runtime, native linkage, component loading or system services. Older Java launcher compatibility patches cannot be applied directly to this native architecture.

A useful Android rebuild requires resolving the runtime/loading architecture first. No patched or installable Samsung APK has been produced. A manifest-only rebuild is not a launch milestone.

## Next prerequisite

Obtain the matching runtime and framework implementation, or an existing non-HyperOS port of this native generation, for dependency and loader analysis. Extracting libraries alone does not establish Samsung compatibility: their transitive dependencies and service interfaces must also be examined. Continue targeting the latest build unless the user explicitly changes scope.

## Reproduce inspection

Install Python dependencies from requirements-audit.txt and make readelf available, then run:

    python scripts/audit_apk.py /path/to/launcher.apk

The script reads ZIP/manifest/ELF data only; it does not run APK code. Its output was checked against the selected build's package, absent DEX, hasCode flag and required runtime declaration.

## Related external investigation

A third-party native loader investigation describes a HyperOS process spawner and runtime preload architecture. This is contextual evidence, not a Samsung test or proof that every detail matches this build:

https://github.com/wxxsfxyzm/MiuiBackGestureHook/blob/main/ANDROID_17_MIUI_HOME_NATIVE_LOADER_REPORT.md
