package io.github.daredevilq.wistride.domain.wifi

import com.google.common.truth.Truth.assertWithMessage
import io.github.daredevilq.wistride.core.model.wifi.ChannelWidth
import io.github.daredevilq.wistride.core.model.wifi.WifiSecurity
import io.github.daredevilq.wistride.core.model.wifi.WifiStandard
import org.junit.Test

class AndroidWifiMappingTest {
    @Test
    fun `standard mapping`() {
        val cases =
            mapOf(
                0 to null,
                1 to WifiStandard.Legacy,
                2 to null,
                3 to null,
                4 to WifiStandard.Ieee80211n,
                5 to WifiStandard.Ieee80211ac,
                6 to WifiStandard.Ieee80211ax,
                7 to WifiStandard.Ieee80211ad,
                8 to WifiStandard.Ieee80211be,
                9 to null,
                -1 to null,
                Int.MAX_VALUE to null,
            )
        cases.forEach { (raw, expected) ->
            assertWithMessage("raw $raw").that(wifiStandardFromAndroid(raw)).isEqualTo(expected)
        }
    }

    @Test
    fun `channel width mapping`() {
        val cases =
            mapOf(
                0 to ChannelWidth.Mhz20,
                1 to ChannelWidth.Mhz40,
                2 to ChannelWidth.Mhz80,
                3 to ChannelWidth.Mhz160,
                4 to ChannelWidth.Mhz80Plus80,
                5 to ChannelWidth.Mhz320,
                6 to null,
                -1 to null,
                Int.MIN_VALUE to null,
            )
        cases.forEach { (raw, expected) ->
            assertWithMessage("raw $raw").that(channelWidthFromAndroid(raw)).isEqualTo(expected)
        }
    }

    @Test
    fun `security mapping`() {
        val cases =
            mapOf(
                -1 to null,
                0 to WifiSecurity.Open,
                1 to WifiSecurity.Wep,
                2 to WifiSecurity.Psk,
                3 to WifiSecurity.Eap,
                4 to WifiSecurity.Sae,
                5 to WifiSecurity.Wpa3Enterprise192Bit,
                6 to WifiSecurity.Owe,
                7 to WifiSecurity.WapiPsk,
                8 to WifiSecurity.WapiCert,
                9 to WifiSecurity.Wpa3Enterprise,
                10 to WifiSecurity.Osen,
                11 to WifiSecurity.Passpoint,
                12 to WifiSecurity.Passpoint,
                13 to WifiSecurity.Dpp,
                14 to null,
                -2 to null,
                Int.MAX_VALUE to null,
            )
        cases.forEach { (raw, expected) ->
            assertWithMessage("raw $raw").that(wifiSecurityFromAndroid(raw)).isEqualTo(expected)
        }
    }

    @Test
    fun `ssid normalization`() {
        val cases =
            listOf(
                null to null,
                "" to null,
                "\"\"" to null,
                "<unknown ssid>" to null,
                "\"<unknown ssid>\"" to null,
                "\"Home\"" to "Home",
                "\"My \"quoted\" net\"" to "My \"quoted\" net",
                "Home" to "Home",
                "4a6f686e" to "4a6f686e",
                "\"" to "\"",
                "\"half" to "\"half",
                "half\"" to "half\"",
                " " to " ",
            )
        cases.forEach { (raw, expected) ->
            assertWithMessage("raw $raw").that(normalizeSsid(raw)).isEqualTo(expected)
        }
    }

    @Test
    fun `bssid normalization`() {
        val cases =
            listOf(
                null to null,
                "02:00:00:00:00:00" to null,
                "AA:BB:CC:DD:EE:FF" to "aa:bb:cc:dd:ee:ff",
                "aa:bb:cc:dd:ee:ff" to "aa:bb:cc:dd:ee:ff",
                "" to "",
            )
        cases.forEach { (raw, expected) ->
            assertWithMessage("raw $raw").that(normalizeBssid(raw)).isEqualTo(expected)
        }
    }

    @Test
    fun `rssi and link speed sanitizing`() {
        val rssi =
            listOf(
                AndroidWifiConstants.INVALID_RSSI to null,
                -128 to null,
                -126 to -126,
                -60 to -60,
                0 to 0,
                1 to null,
                Int.MAX_VALUE to null,
            )
        rssi.forEach { (raw, expected) -> assertWithMessage("rssi $raw").that(rssiOrNull(raw)).isEqualTo(expected) }

        val speed =
            listOf(
                AndroidWifiConstants.LINK_SPEED_UNKNOWN to null,
                -5 to null,
                0 to null,
                1 to 1,
                866 to 866,
            )
        speed.forEach { (raw, expected) ->
            assertWithMessage("speed $raw").that(linkSpeedOrNull(raw)).isEqualTo(expected)
        }
    }
}
