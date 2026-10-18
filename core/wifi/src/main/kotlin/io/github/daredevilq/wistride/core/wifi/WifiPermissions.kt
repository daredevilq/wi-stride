package io.github.daredevilq.wistride.core.wifi

import android.Manifest
import android.os.Build

/** Runtime permissions WiStride needs to read Wi-Fi details, per API level. */
object WifiPermissions {
    /**
     * Permissions to request together for SSID/BSSID of the connected network and scan results.
     * FINE + COARSE in one request (API 31+ otherwise lets the user pick "approximate", which redacts
     * Wi-Fi details); NEARBY_WIFI_DEVICES additionally gates scan results on API 33+.
     */
    fun connectionDetails(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (sdkInt >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
}
