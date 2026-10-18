package io.github.daredevilq.wistride.domain.wifi

import io.github.daredevilq.wistride.core.model.wifi.WifiBand
import io.github.daredevilq.wistride.core.model.wifi.WifiGeneration
import io.github.daredevilq.wistride.core.model.wifi.WifiStandard

/**
 * Wi-Fi Alliance generation name for [standard]: n = Wi-Fi 4, ac = Wi-Fi 5, ax = Wi-Fi 6
 * (Wi-Fi 6E when [band] is 6 GHz), be = Wi-Fi 7, ad = WiGig, a/b/g = Legacy.
 */
fun wifiGeneration(
    standard: WifiStandard,
    band: WifiBand?,
): WifiGeneration =
    when (standard) {
        WifiStandard.Legacy -> WifiGeneration.Legacy
        WifiStandard.Ieee80211n -> WifiGeneration.WiFi4
        WifiStandard.Ieee80211ac -> WifiGeneration.WiFi5
        WifiStandard.Ieee80211ax -> if (band == WifiBand.Ghz6) WifiGeneration.WiFi6E else WifiGeneration.WiFi6
        WifiStandard.Ieee80211ad -> WifiGeneration.WiGig
        WifiStandard.Ieee80211be -> WifiGeneration.WiFi7
    }
