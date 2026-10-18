package io.github.daredevilq.wistride.core.network

import io.github.daredevilq.wistride.core.common.Dispatcher
import io.github.daredevilq.wistride.core.common.WiStrideDispatcher
import io.github.daredevilq.wistride.core.model.ping.LatencyEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import javax.inject.Inject

private const val MIN_TIMEOUT_MS = 500L
private const val MAX_TIMEOUT_MS = 2_000L
private const val NANOS_PER_MS = 1_000_000.0

/**
 * Measures the time to complete a TCP handshake. A refused connection (RST) still proves the host
 * answered, so it counts with its RTT; timeouts and unreachable errors are lost probes.
 *
 * Schedule: probe k starts at `start + k * interval` (fixed rate, no drift); a probe that overruns
 * is followed immediately by the next one.
 */
class ConnectTcpProber
    @Inject
    constructor(
        private val clock: LatencyClock,
        @Dispatcher(WiStrideDispatcher.IO) private val io: CoroutineDispatcher,
    ) : TcpProber {
        override fun probe(
            network: NetworkHandle,
            address: InetAddress,
            port: Int,
            intervalMs: Long,
        ): Flow<LatencyEvent.Probe> =
            flow {
                val timeoutMs = intervalMs.coerceIn(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS).toInt()
                val startNanos = clock.monotonicNanos()
                var sent = 0L
                while (true) {
                    emit(connectOnce(network, address, port, timeoutMs))
                    sent++
                    val dueMs = (startNanos / NANOS_PER_MS) + sent * intervalMs
                    val waitMs = dueMs - clock.monotonicNanos() / NANOS_PER_MS
                    if (waitMs > 0) delay(waitMs.toLong())
                }
            }

        private suspend fun connectOnce(
            network: NetworkHandle,
            address: InetAddress,
            port: Int,
            timeoutMs: Int,
        ): LatencyEvent.Probe {
            val timestamp = clock.wallClockMs()
            val rtt =
                withContext(io) {
                    val begin = clock.monotonicNanos()
                    try {
                        network.connect(address, port, timeoutMs)
                        elapsedMs(begin)
                    } catch (e: ConnectException) {
                        if (e.isRefused()) elapsedMs(begin) else null
                    } catch (_: IOException) {
                        null
                    }
                }
            return LatencyEvent.Probe(timestamp, rtt)
        }

        private fun elapsedMs(beginNanos: Long): Double = (clock.monotonicNanos() - beginNanos) / NANOS_PER_MS

        // Android reports ECONNREFUSED as ConnectException("... ECONNREFUSED (Connection refused)").
        private fun ConnectException.isRefused(): Boolean =
            message.orEmpty().let { "ECONNREFUSED" in it || it.contains("refused", ignoreCase = true) }
    }
