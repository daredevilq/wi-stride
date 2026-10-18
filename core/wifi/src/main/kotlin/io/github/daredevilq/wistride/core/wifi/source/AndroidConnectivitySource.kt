package io.github.daredevilq.wistride.core.wifi.source

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LEGACY_POLL_INTERVAL_MS = 1_000L
private const val NO_NETWORK_GRACE_MS = 500L

/** Network callback per API level; API 29-30 additionally polls `WifiManager.connectionInfo`. */
internal class AndroidConnectivitySource
    @Inject
    constructor(
        private val connectivityManager: ConnectivityManager,
        private val wifiManager: WifiManager,
    ) : ConnectivitySource {
        override val wifiNetwork: Flow<WifiNetworkInfo?> =
            callbackFlow {
                val tracker = WifiNetworkTracker<Network>()
                val wifiPresent = MutableStateFlow(false)
                // Callbacks (ConnectivityThread) and the legacy poller publish concurrently; the lock keeps
                // emissions in the order of tracker updates.
                val lock = Any()
                var published = false
                val publish: () -> Unit = {
                    synchronized(lock) {
                        published = true
                        wifiPresent.value = !tracker.isEmpty()
                        trySend(tracker.current())
                    }
                }
                val callback = createCallback(tracker, publish)
                val registered = register(callback)
                launch {
                    // Registration replays onAvailable for a connected network within milliseconds; emitting
                    // null up front would flash "Disconnected" every time the screen starts collecting.
                    delay(NO_NETWORK_GRACE_MS)
                    synchronized(lock) { if (!published) trySend(null) }
                }
                if (registered && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    pollLegacyInfo(tracker, wifiPresent, publish)
                }
                awaitClose { if (registered) connectivityManager.unregisterNetworkCallback(callback) }
            }.distinctUntilChanged()

        private fun register(callback: ConnectivityManager.NetworkCallback): Boolean {
            // Removing INTERNET keeps Wi-Fi without internet access (captive portal, offline AP) visible.
            val request =
                NetworkRequest
                    .Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
            return try {
                connectivityManager.registerNetworkCallback(request, callback)
                true
            } catch (_: SecurityException) {
                false
            }
        }

        private fun createCallback(
            tracker: WifiNetworkTracker<Network>,
            publish: () -> Unit,
        ): ConnectivityManager.NetworkCallback =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                LocationAwareCallback(tracker, publish)
            } else {
                LegacyCallback(tracker, publish)
            }

        private fun ProducerScope<WifiNetworkInfo?>.pollLegacyInfo(
            tracker: WifiNetworkTracker<Network>,
            wifiPresent: Flow<Boolean>,
            publish: () -> Unit,
        ) {
            launch {
                wifiPresent.collectLatest { present ->
                    while (present) {
                        tracker.setPolledInfo(readLegacyInfo())
                        publish()
                        delay(LEGACY_POLL_INTERVAL_MS)
                    }
                }
            }
        }

        // connectionInfo is deprecated from API 31 but is the only source of WifiInfo on 29-30.
        @Suppress("DEPRECATION")
        private fun readLegacyInfo(): RawWifiInfo? =
            try {
                wifiManager.connectionInfo?.toRaw()
            } catch (_: SecurityException) {
                null
            }
    }

private class LegacyCallback(
    private val tracker: WifiNetworkTracker<Network>,
    private val publish: () -> Unit,
) : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        tracker.onAvailable(network)
        publish()
    }

    override fun onLinkPropertiesChanged(
        network: Network,
        linkProperties: LinkProperties,
    ) {
        tracker.onLinkInfo(network, linkProperties.toRawLinkInfo())
        publish()
    }

    override fun onLost(network: Network) {
        tracker.onLost(network)
        publish()
    }
}

/** FLAG_INCLUDE_LOCATION_INFO makes the framework deliver unredacted SSID/BSSID in `transportInfo`. */
@RequiresApi(Build.VERSION_CODES.S)
private class LocationAwareCallback(
    private val tracker: WifiNetworkTracker<Network>,
    private val publish: () -> Unit,
) : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
    override fun onAvailable(network: Network) {
        tracker.onAvailable(network)
        publish()
    }

    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
    ) {
        tracker.onWifiInfo(network, (networkCapabilities.transportInfo as? WifiInfo)?.toRaw())
        publish()
    }

    override fun onLinkPropertiesChanged(
        network: Network,
        linkProperties: LinkProperties,
    ) {
        tracker.onLinkInfo(network, linkProperties.toRawLinkInfo())
        publish()
    }

    override fun onLost(network: Network) {
        tracker.onLost(network)
        publish()
    }
}
