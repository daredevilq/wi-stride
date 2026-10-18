package io.github.daredevilq.wistride.core.network

import android.Manifest
import io.github.daredevilq.wistride.core.common.Dispatcher
import io.github.daredevilq.wistride.core.common.WiStrideDispatcher
import io.github.daredevilq.wistride.core.model.ping.LatencyEvent
import io.github.daredevilq.wistride.core.model.ping.LatencyUnavailableReason
import io.github.daredevilq.wistride.core.model.ping.PingMethod
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import io.github.daredevilq.wistride.core.network.di.SdkInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject

private const val FALLBACK_TCP_PORT = 443
private const val UNKNOWN_HOST_RETRY_MS = 10_000L
private const val DNS_PERIOD_MS = 30_000L
private const val LOCAL_NETWORK_PERMISSION_API = 37

/**
 * Implements the latency method selection on top of small collaborators: follow the Wi-Fi
 * path, resolve the target once per path on that network, pick TCP (user port, or Wi-Fi is not the
 * default network, or ICMP failed) or ICMP, and time DNS separately for host names.
 */
class DefaultLatencyMonitor
    @Inject
    internal constructor(
        private val pathSource: WifiPathSource,
        private val icmp: IcmpProber,
        private val tcp: TcpProber,
        private val dns: DnsTimer,
        private val localPermission: LocalNetworkPermissionChecker,
        private val clock: LatencyClock,
        @SdkInt private val sdkInt: Int,
        @Dispatcher(WiStrideDispatcher.IO) private val io: CoroutineDispatcher,
    ) : LatencyMonitor {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun monitor(
            target: PingTarget,
            intervalMs: Long,
        ): Flow<LatencyEvent> =
            pathSource.path.flatMapLatest { path ->
                if (path == null) {
                    flowOf(LatencyEvent.Unavailable(LatencyUnavailableReason.NoWifi))
                } else {
                    measure(target, path, intervalMs)
                }
            }

        private fun measure(
            target: PingTarget,
            path: WifiPath,
            intervalMs: Long,
        ): Flow<LatencyEvent> =
            flow {
                val address = resolve(target, path)
                val needsPermission =
                    sdkInt >= LOCAL_NETWORK_PERMISSION_API &&
                        isLocalAddress(address, path.linkAddresses) &&
                        !localPermission.isGranted()
                if (needsPermission) {
                    emit(
                        unavailable(LatencyUnavailableReason.NeedsPermission(Manifest.permission.ACCESS_LOCAL_NETWORK)),
                    )
                    awaitCancellation()
                }
                val probes = probe(target, path, address, intervalMs)
                val hostName = (target as? PingTarget.Host)?.host?.takeIf { parseIpLiteral(it) == null }
                emitAll(if (hostName == null) probes else merge(probes, dnsEvents(path, hostName)))
            }

        /**
         * The address to probe. Without a gateway it emits why and then waits for the Wi-Fi path to
         * change, which restarts [measure].
         */
        private suspend fun FlowCollector<LatencyEvent>.resolve(
            target: PingTarget,
            path: WifiPath,
        ): InetAddress =
            when (target) {
                PingTarget.Gateway -> {
                    path.gateway ?: run {
                        emit(unavailable(LatencyUnavailableReason.NoGateway))
                        awaitCancellation()
                    }
                }

                is PingTarget.Host -> {
                    parseIpLiteral(target.host) ?: resolveHostName(path, target.host)
                }
            }

        private suspend fun FlowCollector<LatencyEvent>.resolveHostName(
            path: WifiPath,
            host: String,
        ): InetAddress {
            while (true) {
                try {
                    val all = withContext(io) { path.network.resolve(host) }
                    return all.firstOrNull { it is Inet4Address } ?: all.first()
                } catch (_: UnknownHostException) {
                    emit(unavailable(LatencyUnavailableReason.UnknownHost))
                    delay(UNKNOWN_HOST_RETRY_MS)
                }
            }
        }

        private fun probe(
            target: PingTarget,
            path: WifiPath,
            address: InetAddress,
            intervalMs: Long,
        ): Flow<LatencyEvent> =
            flow {
                val ip = address.hostAddress.orEmpty()
                val port = (target as? PingTarget.Host)?.port
                if (port == null && path.isDefault) {
                    emit(LatencyEvent.MethodSelected(PingMethod.Icmp, ip))
                    // A failing ping is not an error for the user: switch to TCP and say so via MethodSelected.
                    emitAll(icmp.probe(address, intervalMs).catch { if (it !is IcmpUnusableException) throw it })
                }
                val tcpPort = port ?: FALLBACK_TCP_PORT
                emit(LatencyEvent.MethodSelected(PingMethod.TcpConnect(tcpPort), ip))
                emitAll(tcp.probe(path.network, address, tcpPort, intervalMs))
            }

        private fun dnsEvents(
            path: WifiPath,
            host: String,
        ): Flow<LatencyEvent> =
            flow {
                while (true) {
                    dns.measure(path.network, host)?.let { emit(LatencyEvent.DnsResolved(clock.wallClockMs(), it)) }
                    delay(DNS_PERIOD_MS)
                }
            }

        private fun unavailable(reason: LatencyUnavailableReason) = LatencyEvent.Unavailable(reason)
    }
