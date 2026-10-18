package io.github.daredevilq.wistride.core.network

import io.github.daredevilq.wistride.core.model.ping.LatencyEvent
import kotlinx.coroutines.flow.Flow
import java.net.InetAddress

/** Thrown by an [IcmpProber] flow when the `ping` process cannot be used (exec failed or it exited). */
class IcmpUnusableException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** ICMP echo probing through the system `ping` binary. */
interface IcmpProber {
    /**
     * One long-running `ping` process per collection. Emits a probe per reply or loss; fails with
     * [IcmpUnusableException] when the process cannot start or ends. Cancelling the collector
     * destroys the process.
     */
    fun probe(
        address: InetAddress,
        intervalMs: Long,
    ): Flow<LatencyEvent.Probe>
}

/** TCP-handshake probing over the Wi-Fi network; infinite until cancelled. */
interface TcpProber {
    fun probe(
        network: NetworkHandle,
        address: InetAddress,
        port: Int,
        intervalMs: Long,
    ): Flow<LatencyEvent.Probe>
}

/** Times a DNS lookup without the resolver cache. */
interface DnsTimer {
    /** Milliseconds the lookup of [host] on [network] took, or null when it failed or timed out. */
    suspend fun measure(
        network: NetworkHandle,
        host: String,
    ): Double?
}

/** Whether the Android 17 `ACCESS_LOCAL_NETWORK` runtime permission is granted right now. */
fun interface LocalNetworkPermissionChecker {
    fun isGranted(): Boolean
}
