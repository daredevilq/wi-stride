package io.github.daredevilq.wistride.core.wifi

import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import io.github.daredevilq.wistride.domain.wifi.AndroidWifiConstants as K

/** The pure-Kotlin mirror in `domain:wifi` must match the platform; these are compile-time constants. */
class AndroidWifiConstantsTest {
    @Test
    fun standardsMatchScanResult() {
        assertThat(K.WIFI_STANDARD_UNKNOWN).isEqualTo(ScanResult.WIFI_STANDARD_UNKNOWN)
        assertThat(K.WIFI_STANDARD_LEGACY).isEqualTo(ScanResult.WIFI_STANDARD_LEGACY)
        assertThat(K.WIFI_STANDARD_11N).isEqualTo(ScanResult.WIFI_STANDARD_11N)
        assertThat(K.WIFI_STANDARD_11AC).isEqualTo(ScanResult.WIFI_STANDARD_11AC)
        assertThat(K.WIFI_STANDARD_11AX).isEqualTo(ScanResult.WIFI_STANDARD_11AX)
        assertThat(K.WIFI_STANDARD_11AD).isEqualTo(ScanResult.WIFI_STANDARD_11AD)
        assertThat(K.WIFI_STANDARD_11BE).isEqualTo(ScanResult.WIFI_STANDARD_11BE)
    }

    @Test
    fun channelWidthsMatchScanResult() {
        assertThat(K.CHANNEL_WIDTH_20MHZ).isEqualTo(ScanResult.CHANNEL_WIDTH_20MHZ)
        assertThat(K.CHANNEL_WIDTH_40MHZ).isEqualTo(ScanResult.CHANNEL_WIDTH_40MHZ)
        assertThat(K.CHANNEL_WIDTH_80MHZ).isEqualTo(ScanResult.CHANNEL_WIDTH_80MHZ)
        assertThat(K.CHANNEL_WIDTH_160MHZ).isEqualTo(ScanResult.CHANNEL_WIDTH_160MHZ)
        assertThat(K.CHANNEL_WIDTH_80MHZ_PLUS_MHZ).isEqualTo(ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ)
        assertThat(K.CHANNEL_WIDTH_320MHZ).isEqualTo(ScanResult.CHANNEL_WIDTH_320MHZ)
    }

    @Test
    fun securityTypesMatchWifiInfo() {
        assertThat(K.SECURITY_TYPE_UNKNOWN).isEqualTo(WifiInfo.SECURITY_TYPE_UNKNOWN)
        assertThat(K.SECURITY_TYPE_OPEN).isEqualTo(WifiInfo.SECURITY_TYPE_OPEN)
        assertThat(K.SECURITY_TYPE_WEP).isEqualTo(WifiInfo.SECURITY_TYPE_WEP)
        assertThat(K.SECURITY_TYPE_PSK).isEqualTo(WifiInfo.SECURITY_TYPE_PSK)
        assertThat(K.SECURITY_TYPE_EAP).isEqualTo(WifiInfo.SECURITY_TYPE_EAP)
        assertThat(K.SECURITY_TYPE_SAE).isEqualTo(WifiInfo.SECURITY_TYPE_SAE)
        assertThat(K.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT)
            .isEqualTo(WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT)
        assertThat(K.SECURITY_TYPE_OWE).isEqualTo(WifiInfo.SECURITY_TYPE_OWE)
        assertThat(K.SECURITY_TYPE_WAPI_PSK).isEqualTo(WifiInfo.SECURITY_TYPE_WAPI_PSK)
        assertThat(K.SECURITY_TYPE_WAPI_CERT).isEqualTo(WifiInfo.SECURITY_TYPE_WAPI_CERT)
        assertThat(K.SECURITY_TYPE_EAP_WPA3_ENTERPRISE).isEqualTo(WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE)
        assertThat(K.SECURITY_TYPE_OSEN).isEqualTo(WifiInfo.SECURITY_TYPE_OSEN)
        assertThat(K.SECURITY_TYPE_PASSPOINT_R1_R2).isEqualTo(WifiInfo.SECURITY_TYPE_PASSPOINT_R1_R2)
        assertThat(K.SECURITY_TYPE_PASSPOINT_R3).isEqualTo(WifiInfo.SECURITY_TYPE_PASSPOINT_R3)
        assertThat(K.SECURITY_TYPE_DPP).isEqualTo(WifiInfo.SECURITY_TYPE_DPP)
    }

    @Test
    fun sentinelsMatchPlatform() {
        assertThat(K.LINK_SPEED_UNKNOWN).isEqualTo(WifiInfo.LINK_SPEED_UNKNOWN)
        assertThat(K.UNKNOWN_SSID).isEqualTo(WifiManager.UNKNOWN_SSID)
    }
}
