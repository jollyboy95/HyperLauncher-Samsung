# Compatibility prototype validation

- Compiled the pure Java ELF parser with JDK 17.
- JUnit 4.13.2: 7 tests passed, covering stripped ELF parsing, no dynamic segment, truncated input, wrong CPU architecture, unmapped strings, out-of-bounds strings and unterminated strings.
- Executed the Java parser against all four native libraries extracted from the selected 8.01.02.8584 launcher.
- Compared output to GNU readelf DT_NEEDED output: exact matches for libnative_widget_sdk.so (14), libresources_frb.so (15), libapp_launcher.so (45) and libapp.so (0).

This validates dependency parsing only. Android UI, document import/export, device visibility checks and a complete Android build are not covered by these checks. There is no successful native launcher load or Samsung home screen test.
