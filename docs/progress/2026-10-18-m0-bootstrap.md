# M0 – Project bootstrap (2026-10-18)

The foundation of the app: build, architecture, app shell and shared UI. No Wi-Fi features yet.

## Done
- **Modular Gradle build** – version catalog, convention plugins in `build-logic`, modules `app`, `core:{model, common, testing, designsystem, ui}`, `feature:{now, ping, networks, speed, map}`. AGP 9.4 with built-in Kotlin 2.4, Hilt + KSP.
- **App shell** – single activity, edge-to-edge, five tabs (Now, Ping, Networks, Speed, Map) with Navigation Compose type-safe routes. Bottom bar on phones, navigation rail on large screens. Settings and About reachable from the overflow menu (placeholders).
- **Design system** – Material 3 theme (light, dark, dynamic color), spacing tokens, signal-quality status colors, components `ValueRow` (with an "unavailable + why" variant), `StatusCard`, `SectionHeader`.
- **Runtime permissions** – reusable flow: rationale before every request, "don't ask again" detection with a shortcut to app settings.
- **Localization** – all strings in English and Polish, missing translations fail lint.
- **Quality tooling** – Spotless (ktlint + Compose rules), detekt, lint, Robolectric UI tests; 19 unit/UI tests.

## Works
- The app builds, installs and runs on a real device (Samsung Galaxy A53, Android 16): switching between tabs, the selected tab survives rotation, no crashes.
- Each tab currently shows a placeholder screen.

## Next
M1 – the **Now** screen with live connection diagnostics.
