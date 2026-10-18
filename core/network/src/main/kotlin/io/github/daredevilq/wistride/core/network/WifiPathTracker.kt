package io.github.daredevilq.wistride.core.network

import java.net.InetAddress

/** `LinkProperties` reduced to what path selection needs. */
internal data class WifiLink(
    val gateway: InetAddress?,
    val addresses: List<LinkPrefix>,
)

/**
 * Merges the Wi-Fi network callback and the default-network callback into [WifiPath]s. Generic in
 * the network type [N] so it can be tested without `android.net.Network`. Not thread safe: the
 * caller serialises calls (callbacks arrive on the connectivity thread).
 */
internal class WifiPathTracker<N : Any>(
    private val handleOf: (N) -> NetworkHandle,
) {
    private val wifi = LinkedHashMap<N, WifiLink?>()
    private var default: N? = null

    fun isEmpty(): Boolean = wifi.isEmpty()

    fun onWifiAvailable(network: N) {
        if (network !in wifi) wifi[network] = null
    }

    fun onLink(
        network: N,
        link: WifiLink,
    ) {
        if (network in wifi) wifi[network] = link
    }

    fun onWifiLost(network: N) {
        wifi.remove(network)
    }

    fun onDefaultAvailable(network: N) {
        default = network
    }

    fun onDefaultLost(network: N) {
        if (default == network) default = null
    }

    /**
     * The most recent Wi-Fi network that already reported its link properties, or null when there is
     * none (also while the only network is still waiting for them, so the gateway is never
     * reported as missing just because the callback has not arrived yet).
     */
    fun current(): WifiPath? {
        val (network, link) = wifi.entries.lastOrNull { it.value != null } ?: return null
        val resolved = checkNotNull(link)
        return WifiPath(handleOf(network), network == default, resolved.gateway, resolved.addresses)
    }
}
