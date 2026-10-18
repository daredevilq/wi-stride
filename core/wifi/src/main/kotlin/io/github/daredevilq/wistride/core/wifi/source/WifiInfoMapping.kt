package io.github.daredevilq.wistride.core.wifi.source

import android.net.wifi.WifiInfo
import android.os.Build

/** Copies the fields without interpretation; getters above the running API level give null. */
internal fun WifiInfo.toRaw(): RawWifiInfo =
    RawWifiInfo(
        ssid = ssid,
        bssid = bssid,
        rssi = rssi,
        frequencyMhz = frequency,
        txLinkSpeedMbps = txLinkSpeedMbps,
        rxLinkSpeedMbps = rxLinkSpeedMbps,
        // Lint only understands SDK_INT comparisons written inline, hence no shared local.
        maxTxLinkSpeedMbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) maxSupportedTxLinkSpeedMbps else null,
        maxRxLinkSpeedMbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) maxSupportedRxLinkSpeedMbps else null,
        wifiStandard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) wifiStandard else null,
        currentSecurityType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) currentSecurityType else null,
    )
