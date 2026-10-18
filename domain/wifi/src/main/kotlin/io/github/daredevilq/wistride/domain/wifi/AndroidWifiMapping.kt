package io.github.daredevilq.wistride.domain.wifi

import io.github.daredevilq.wistride.core.model.wifi.ChannelWidth
import io.github.daredevilq.wistride.core.model.wifi.WifiSecurity
import io.github.daredevilq.wistride.core.model.wifi.WifiStandard

/** Maps `ScanResult.WIFI_STANDARD_*`; unknown (0) and unrecognised values give `null`. */
fun wifiStandardFromAndroid(value: Int): WifiStandard? =
    when (value) {
        AndroidWifiConstants.WIFI_STANDARD_LEGACY -> WifiStandard.Legacy
        AndroidWifiConstants.WIFI_STANDARD_11N -> WifiStandard.Ieee80211n
        AndroidWifiConstants.WIFI_STANDARD_11AC -> WifiStandard.Ieee80211ac
        AndroidWifiConstants.WIFI_STANDARD_11AX -> WifiStandard.Ieee80211ax
        AndroidWifiConstants.WIFI_STANDARD_11AD -> WifiStandard.Ieee80211ad
        AndroidWifiConstants.WIFI_STANDARD_11BE -> WifiStandard.Ieee80211be
        else -> null
    }

/** Maps `ScanResult.CHANNEL_WIDTH_*` (320 MHz exists from API 33); unrecognised values give `null`. */
fun channelWidthFromAndroid(value: Int): ChannelWidth? =
    when (value) {
        AndroidWifiConstants.CHANNEL_WIDTH_20MHZ -> ChannelWidth.Mhz20
        AndroidWifiConstants.CHANNEL_WIDTH_40MHZ -> ChannelWidth.Mhz40
        AndroidWifiConstants.CHANNEL_WIDTH_80MHZ -> ChannelWidth.Mhz80
        AndroidWifiConstants.CHANNEL_WIDTH_160MHZ -> ChannelWidth.Mhz160
        AndroidWifiConstants.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> ChannelWidth.Mhz80Plus80
        AndroidWifiConstants.CHANNEL_WIDTH_320MHZ -> ChannelWidth.Mhz320
        else -> null
    }

private val SECURITY_BY_ANDROID_VALUE: Map<Int, WifiSecurity> =
    mapOf(
        AndroidWifiConstants.SECURITY_TYPE_OPEN to WifiSecurity.Open,
        AndroidWifiConstants.SECURITY_TYPE_WEP to WifiSecurity.Wep,
        AndroidWifiConstants.SECURITY_TYPE_PSK to WifiSecurity.Psk,
        AndroidWifiConstants.SECURITY_TYPE_EAP to WifiSecurity.Eap,
        AndroidWifiConstants.SECURITY_TYPE_SAE to WifiSecurity.Sae,
        AndroidWifiConstants.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT to WifiSecurity.Wpa3Enterprise192Bit,
        AndroidWifiConstants.SECURITY_TYPE_OWE to WifiSecurity.Owe,
        AndroidWifiConstants.SECURITY_TYPE_WAPI_PSK to WifiSecurity.WapiPsk,
        AndroidWifiConstants.SECURITY_TYPE_WAPI_CERT to WifiSecurity.WapiCert,
        AndroidWifiConstants.SECURITY_TYPE_EAP_WPA3_ENTERPRISE to WifiSecurity.Wpa3Enterprise,
        AndroidWifiConstants.SECURITY_TYPE_OSEN to WifiSecurity.Osen,
        AndroidWifiConstants.SECURITY_TYPE_PASSPOINT_R1_R2 to WifiSecurity.Passpoint,
        AndroidWifiConstants.SECURITY_TYPE_PASSPOINT_R3 to WifiSecurity.Passpoint,
        AndroidWifiConstants.SECURITY_TYPE_DPP to WifiSecurity.Dpp,
    )

/** Maps `WifiInfo.SECURITY_TYPE_*`; unknown (-1) and unrecognised values give `null`. */
fun wifiSecurityFromAndroid(value: Int): WifiSecurity? = SECURITY_BY_ANDROID_VALUE[value]
