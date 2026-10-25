# Compatibility prototype validation

- Compiled the pure Java ELF parser with JDK 17.
- JUnit 4.13.2: 7 tests passed, covering stripped ELF parsing, no dynamic segment, truncated input, wrong CPU architecture, unmapped strings, out-of-bounds strings and unterminated strings.
- Executed the Java parser against all four native libraries extracted from the selected 8.01.02.8584 launcher.
- Compared output to GNU readelf DT_NEEDED output: exact matches for libnative_widget_sdk.so (14), libresources_frb.so (15), libapp_launcher.so (45) and libapp.so (0).

## Full Android build attempt

Obtained JDK 17, Gradle 8.11.1 and Android SDK command-line tools locally. Gradle could not resolve the com.android.application 8.9.2 plugin from configured repositories. SDK manager also failed to fetch package manifests and could not install platforms;android-35. A subsequent Gradle attempt using environment network/trust settings was stopped while unresolved. No local Android APK was produced.

GitHub Actions workflow is committed, but its run/conclusion has not been verified. The connector's workflow lookup exposes pull-request runs, while this change was pushed to main; the separate browser is not signed in to the private repository. An empty connector result is not proof that CI did or did not run.

This validates dependency parsing only. Android UI, document import/export and device visibility checks have not been tested on a phone. There is no successful native launcher load or Samsung home screen test.
