package io.github.daredevilq.wistride.core.model.wifi

/** Wi-Fi frequency band. */
enum class WifiBand {
    Ghz2Point4,
    Ghz5,
    Ghz6,
}

/** A channel resolved from a centre frequency. */
data class WifiChannel(
    val band: WifiBand,
    val number: Int,
    val frequencyMhz: Int,
)

/** Channel bandwidth as reported by a scan result. */
enum class ChannelWidth(
    val totalMhz: Int,
) {
    Mhz20(20),
    Mhz40(40),
    Mhz80(80),
    Mhz160(160),

    /** Two non-contiguous 80 MHz segments. */
    Mhz80Plus80(160),
    Mhz320(320),
}

/** IEEE 802.11 amendment the link uses (`WifiInfo.getWifiStandard`). */
enum class WifiStandard {
    /** 802.11a/b/g. */
    Legacy,
    Ieee80211n,
    Ieee80211ac,
    Ieee80211ax,
    Ieee80211ad,
    Ieee80211be,
}

/** Marketing generation shown to the user; 6E is 802.11ax on the 6 GHz band. */
enum class WifiGeneration {
    Legacy,
    WiFi4,
    WiFi5,
    WiFi6,
    WiFi6E,
    WiFi7,

    /** 802.11ad (60 GHz) has no Wi-Fi N name. */
    WiGig,
}

/** Security of the current connection (`WifiInfo.getCurrentSecurityType`). */
enum class WifiSecurity {
    Open,
    Wep,

    /** WPA/WPA2-Personal. */
    Psk,

    /** WPA/WPA2-Enterprise. */
    Eap,

    /** WPA3-Personal. */
    Sae,
    Wpa3Enterprise,
    Wpa3Enterprise192Bit,

    /** Enhanced Open. */
    Owe,
    WapiPsk,
    WapiCert,
    Osen,
    Passpoint,
    Dpp,
}

/** Verbal signal strength scale, from best to worst. */
enum class SignalLevel {
    Excellent,
    Good,
    Fair,
    Weak,
    Poor,
}

/** RSSI expressed for humans: [percent] in 0..100 plus a [level]. */
data class SignalQuality(
    val percent: Int,
    val level: SignalLevel,
)
