# WiStride

A native Android application combining real-time Wi-Fi diagnostics with indoor signal heatmapping powered by Pedestrian Dead Reckoning (PDR) sensor fusion.

WiStride answers two questions: *what is going on with my Wi-Fi right now?* and *where in my home or office is the signal weak?*

> The app is in active development. The list below is the target feature set; see [Progress](#progress) for what already works.

- **Live diagnostics** – SSID/BSSID, RSSI with a live chart, band, channel and channel width, Wi-Fi standard, link speeds, security, IP configuration.
- **Latency** – continuous ping to the gateway and public hosts with min/avg/max, jitter and packet loss.
- **Network scanner** – nearby networks and a channel graph with congestion score and a recommended channel.
- **Speed test** – download/upload throughput and latency under load.
- **Indoor mapper** – walk around with the phone; step detection and heading (PDR, no GPS, fully offline) track your position and build a 2D Wi-Fi signal heatmap, optionally over a floor plan.
- **Sessions & export** – saved mapping sessions exported to CSV, JSON or PNG.

All data stays on the device. The UI is available in English and Polish.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Coroutines/Flow · Hilt · Navigation Compose · Room · DataStore  
minSdk 29, target/compileSdk 37 · single activity, MVVM / unidirectional data flow · multi-module Gradle build with convention plugins (`build-logic`), pure-Kotlin domain modules.

Quality: Spotless (ktlint + Compose rules), detekt, Android lint, JUnit 4 + Truth + Turbine, Robolectric UI tests.

## Building

Requires JDK 17 and the Android SDK (platform 37).

```bash
./gradlew assembleDebug                                    # build the debug APK
./gradlew spotlessCheck detekt testDebugUnitTest lintDebug # checks and tests
```

## Progress

| Date | Milestone | Notes |
|---|---|---|
| 2026-10-18 | M0 – Project bootstrap | [docs/progress/2026-10-18-m0-bootstrap.md](docs/progress/2026-10-18-m0-bootstrap.md) |
