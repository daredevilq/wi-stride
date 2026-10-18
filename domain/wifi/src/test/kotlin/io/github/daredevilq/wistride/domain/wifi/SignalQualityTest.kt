package io.github.daredevilq.wistride.domain.wifi

import com.google.common.truth.Truth.assertWithMessage
import io.github.daredevilq.wistride.core.model.wifi.SignalLevel
import io.github.daredevilq.wistride.core.model.wifi.SignalQuality
import io.github.daredevilq.wistride.core.model.wifi.WifiBand
import io.github.daredevilq.wistride.core.model.wifi.WifiGeneration
import io.github.daredevilq.wistride.core.model.wifi.WifiStandard
import org.junit.Test

class SignalQualityTest {
    @Test
    fun `rssi maps to percent and level`() {
        val cases =
            listOf(
                -30 to SignalQuality(100, SignalLevel.Excellent),
                -50 to SignalQuality(100, SignalLevel.Excellent),
                -51 to SignalQuality(98, SignalLevel.Good),
                -60 to SignalQuality(80, SignalLevel.Good),
                -61 to SignalQuality(78, SignalLevel.Fair),
                -70 to SignalQuality(60, SignalLevel.Fair),
                -71 to SignalQuality(58, SignalLevel.Weak),
                -80 to SignalQuality(40, SignalLevel.Weak),
                -81 to SignalQuality(38, SignalLevel.Poor),
                -100 to SignalQuality(0, SignalLevel.Poor),
                -101 to SignalQuality(0, SignalLevel.Poor),
                0 to SignalQuality(100, SignalLevel.Excellent),
                AndroidWifiConstants.INVALID_RSSI to SignalQuality(0, SignalLevel.Poor),
            )
        cases.forEach { (rssi, expected) ->
            assertWithMessage("rssi $rssi").that(signalQuality(rssi)).isEqualTo(expected)
        }
    }

    @Test
    fun `generation covers every standard`() {
        val cases =
            listOf(
                Triple(WifiStandard.Legacy, null, WifiGeneration.Legacy),
                Triple(WifiStandard.Legacy, WifiBand.Ghz2Point4, WifiGeneration.Legacy),
                Triple(WifiStandard.Ieee80211n, WifiBand.Ghz2Point4, WifiGeneration.WiFi4),
                Triple(WifiStandard.Ieee80211ac, WifiBand.Ghz5, WifiGeneration.WiFi5),
                Triple(WifiStandard.Ieee80211ax, WifiBand.Ghz5, WifiGeneration.WiFi6),
                Triple(WifiStandard.Ieee80211ax, WifiBand.Ghz2Point4, WifiGeneration.WiFi6),
                Triple(WifiStandard.Ieee80211ax, null, WifiGeneration.WiFi6),
                Triple(WifiStandard.Ieee80211ax, WifiBand.Ghz6, WifiGeneration.WiFi6E),
                Triple(WifiStandard.Ieee80211ad, null, WifiGeneration.WiGig),
                Triple(WifiStandard.Ieee80211be, WifiBand.Ghz6, WifiGeneration.WiFi7),
                Triple(WifiStandard.Ieee80211be, WifiBand.Ghz5, WifiGeneration.WiFi7),
            )
        cases.forEach { (standard, band, expected) ->
            assertWithMessage("$standard $band").that(wifiGeneration(standard, band)).isEqualTo(expected)
        }
        val covered = cases.map { it.first }.toSet()
        assertWithMessage("every standard is tested").that(covered).containsExactlyElementsIn(WifiStandard.entries)
    }
}
