package io.github.daredevilq.wistride.core.wifi

import io.github.daredevilq.wistride.core.model.wifi.WifiSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Live view of the device's Wi-Fi connection for the "Now" screen.
 *
 * [snapshots] is cold: platform callbacks and receivers are registered on collection and
 * unregistered when the collector goes away, so nothing runs without a subscriber. Re-collect after
 * a permission grant: redaction of SSID/BSSID is decided when the platform callback is registered.
 */
interface WifiConnectionRepository {
    /** Emits the current snapshot immediately, then on every change (RSSI at most ~1 Hz). */
    val snapshots: Flow<WifiSnapshot>
}
