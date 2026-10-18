package io.github.daredevilq.wistride.core.network

import kotlinx.coroutines.flow.Flow
import java.net.InetAddress

/** Source of wall-clock and monotonic time; injected so tests control both. */
interface LatencyClock {
    /** `System.currentTimeMillis`: timestamps shown to the user. */
    fun wallClockMs(): Long

    /** `System.nanoTime`: immune to clock adjustments, used for every RTT. */
    fun monotonicNanos(): Long
}

/** An address configured on the Wi-Fi link (`LinkAddress`): [address] with its subnet [prefixLength]. */
data class LinkPrefix(
    val address: InetAddress,
    val prefixLength: Int,
)

/**
 * The Wi-Fi `Network` as seen by the measurement code. Hides `android.net.Network` so the
 * orchestration is unit-testable; every call blocks and belongs on the IO dispatcher.
 */
interface NetworkHandle {
    /** Resolves [host] through this network's DNS servers; throws `UnknownHostException`. */
    fun resolve(host: String): List<InetAddress>

    /**
     * Opens a TCP connection to [address]:[port] over this network and closes it again. Throws
     * `ConnectException` (refused/unreachable) or `SocketTimeoutException` (no answer in [timeoutMs]).
     */
    fun connect(
        address: InetAddress,
        port: Int,
        timeoutMs: Int,
    )
}

/**
 * The connected Wi-Fi network.
 *
 * @property isDefault the system default network is this Wi-Fi network (not cellular, not a VPN);
 * only then does the system `ping` (which uses the default route) measure Wi-Fi.
 * @property gateway next hop of the default route (IPv4 preferred), null without a default route.
 */
data class WifiPath(
    val network: NetworkHandle,
    val isDefault: Boolean,
    val gateway: InetAddress?,
    val linkAddresses: List<LinkPrefix>,
)

/** Follows the Wi-Fi network; emits null while none is connected. */
interface WifiPathSource {
    val path: Flow<WifiPath?>
}
