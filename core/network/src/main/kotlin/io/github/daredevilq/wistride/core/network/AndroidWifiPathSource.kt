package io.github.daredevilq.wistride.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.net.Inet4Address
import javax.inject.Inject

private const val NO_NETWORK_GRACE_MS = 500L

/**
 * Two callbacks feed a [WifiPathTracker]: one for Wi-Fi networks (INTERNET capability removed so a
 * captive or offline AP stays visible, as in core:wifi) and one for the system default network.
 * `distinctUntilChanged` on the whole [WifiPath] keeps unrelated link-property churn (MTU, DNS
 * servers do not matter here, but they re-fire the callback) from restarting probing.
 */
internal class AndroidWifiPathSource
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : WifiPathSource {
        private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

        override val path: Flow<WifiPath?> =
            callbackFlow {
                val tracker = WifiPathTracker<Network> { AndroidNetworkHandle(it) }
                // Callbacks run on the connectivity thread; the lock serialises tracker access and sends.
                val lock = Any()
                var published = false
                val publish: () -> Unit = {
                    synchronized(lock) {
                        if (tracker.isEmpty()) {
                            published = true
                            trySend(null)
                        } else {
                            tracker.current()?.let {
                                published = true
                                trySend(it)
                            }
                        }
                    }
                }
                val wifiCallback = WifiCallback(tracker, lock, publish)
                val defaultCallback = DefaultCallback(tracker, lock, publish)
                val registered = register(wifiCallback, defaultCallback)
                launch {
                    // Registration replays the connected network within milliseconds; sending null at
                    // once would flash "no Wi-Fi" on every start.
                    delay(NO_NETWORK_GRACE_MS)
                    synchronized(lock) { if (!published) trySend(null) }
                }
                awaitClose {
                    if (registered) {
                        connectivityManager.unregisterNetworkCallback(wifiCallback)
                        connectivityManager.unregisterNetworkCallback(defaultCallback)
                    }
                }
            }.conflate().distinctUntilChanged()

        private fun register(
            wifiCallback: ConnectivityManager.NetworkCallback,
            defaultCallback: ConnectivityManager.NetworkCallback,
        ): Boolean {
            val request =
                NetworkRequest
                    .Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
            return try {
                connectivityManager.registerNetworkCallback(request, wifiCallback)
                connectivityManager.registerDefaultNetworkCallback(defaultCallback)
                true
            } catch (_: SecurityException) {
                false
            }
        }
    }

private class WifiCallback(
    private val tracker: WifiPathTracker<Network>,
    private val lock: Any,
    private val publish: () -> Unit,
) : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        synchronized(lock) { tracker.onWifiAvailable(network) }
    }

    override fun onLinkPropertiesChanged(
        network: Network,
        linkProperties: LinkProperties,
    ) {
        synchronized(lock) { tracker.onLink(network, linkProperties.toWifiLink()) }
        publish()
    }

    override fun onLost(network: Network) {
        synchronized(lock) { tracker.onWifiLost(network) }
        publish()
    }
}

private class DefaultCallback(
    private val tracker: WifiPathTracker<Network>,
    private val lock: Any,
    private val publish: () -> Unit,
) : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        synchronized(lock) { tracker.onDefaultAvailable(network) }
        publish()
    }

    override fun onLost(network: Network) {
        synchronized(lock) { tracker.onDefaultLost(network) }
        publish()
    }
}

private fun LinkProperties.toWifiLink(): WifiLink {
    val gateways = routes.filter { it.isDefaultRoute }.mapNotNull { it.gateway }
    return WifiLink(
        gateway = gateways.firstOrNull { it is Inet4Address } ?: gateways.firstOrNull(),
        addresses = linkAddresses.map { LinkPrefix(it.address, it.prefixLength) },
    )
}
