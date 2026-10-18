package io.github.daredevilq.wistride.domain.wifi

/**
 * Android platform constants mirrored here because this module cannot see the SDK. The Android
 * side asserts these equal the real `ScanResult`, `WifiInfo` and `WifiManager` values.
 */
object AndroidWifiConstants {
    const val WIFI_STANDARD_UNKNOWN = 0
    const val WIFI_STANDARD_LEGACY = 1
    const val WIFI_STANDARD_11N = 4
    const val WIFI_STANDARD_11AC = 5
    const val WIFI_STANDARD_11AX = 6
    const val WIFI_STANDARD_11AD = 7
    const val WIFI_STANDARD_11BE = 8

    const val CHANNEL_WIDTH_20MHZ = 0
    const val CHANNEL_WIDTH_40MHZ = 1
    const val CHANNEL_WIDTH_80MHZ = 2
    const val CHANNEL_WIDTH_160MHZ = 3
    const val CHANNEL_WIDTH_80MHZ_PLUS_MHZ = 4
    const val CHANNEL_WIDTH_320MHZ = 5

    const val SECURITY_TYPE_UNKNOWN = -1
    const val SECURITY_TYPE_OPEN = 0
    const val SECURITY_TYPE_WEP = 1
    const val SECURITY_TYPE_PSK = 2
    const val SECURITY_TYPE_EAP = 3
    const val SECURITY_TYPE_SAE = 4
    const val SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT = 5
    const val SECURITY_TYPE_OWE = 6
    const val SECURITY_TYPE_WAPI_PSK = 7
    const val SECURITY_TYPE_WAPI_CERT = 8
    const val SECURITY_TYPE_EAP_WPA3_ENTERPRISE = 9
    const val SECURITY_TYPE_OSEN = 10
    const val SECURITY_TYPE_PASSPOINT_R1_R2 = 11
    const val SECURITY_TYPE_PASSPOINT_R3 = 12
    const val SECURITY_TYPE_DPP = 13

    /** `WifiInfo.INVALID_RSSI`. */
    const val INVALID_RSSI = -127

    /** `WifiInfo.LINK_SPEED_UNKNOWN`. */
    const val LINK_SPEED_UNKNOWN = -1

    /** `WifiManager.UNKNOWN_SSID`. */
    const val UNKNOWN_SSID = "<unknown ssid>"

    /** Placeholder BSSID returned when location access is denied. */
    const val REDACTED_BSSID = "02:00:00:00:00:00"
}
