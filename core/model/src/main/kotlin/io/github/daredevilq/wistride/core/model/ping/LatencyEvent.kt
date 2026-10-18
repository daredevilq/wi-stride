package io.github.daredevilq.wistride.core.model.ping

/** How a latency probe is made; always shown in the UI. */
sealed interface PingMethod {
    /** ICMP echo through the system `ping` process. */
    data object Icmp : PingMethod

    /** Time to complete a TCP handshake with [port] (a refused connection still counts as an answer). */
    data class TcpConnect(
        val port: Int,
    ) : PingMethod
}

/** What a running latency measurement of one target reports. */
sealed interface LatencyEvent {
    /**
     * The measurement (re)started with [method] towards [address] (the resolved IP literal). Emitted
     * first and again whenever the method changes, e.g. on fallback from ICMP to TCP.
     */
    data class MethodSelected(
        val method: PingMethod,
        val address: String,
    ) : LatencyEvent

    /** One probe sent at [timestampMs] (wall clock); [rttMs] is null when no answer arrived in time. */
    data class Probe(
        val timestampMs: Long,
        val rttMs: Double?,
    ) : LatencyEvent

    /** Time to resolve the target's host name, measured without the resolver cache (host names only). */
    data class DnsResolved(
        val timestampMs: Long,
        val durationMs: Double,
    ) : LatencyEvent

    /** The target cannot be measured right now; the flow keeps waiting for conditions to change. */
    data class Unavailable(
        val reason: LatencyUnavailableReason,
    ) : LatencyEvent
}

/** Why a target cannot be measured; each case has its own explanation in the UI. */
sealed interface LatencyUnavailableReason {
    /** No Wi-Fi network is connected (measurements never fall back to mobile data). */
    data object NoWifi : LatencyUnavailableReason

    /** The Wi-Fi network has no default route, so there is no gateway to measure. */
    data object NoGateway : LatencyUnavailableReason

    /** The host name did not resolve on the Wi-Fi network. */
    data object UnknownHost : LatencyUnavailableReason

    /** Reaching a LAN address needs the runtime [permission] (Android 17 `ACCESS_LOCAL_NETWORK`). */
    data class NeedsPermission(
        val permission: String,
    ) : LatencyUnavailableReason
}
