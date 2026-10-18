package io.github.daredevilq.wistride.core.network

import io.github.daredevilq.wistride.core.common.Dispatcher
import io.github.daredevilq.wistride.core.common.WiStrideDispatcher
import io.github.daredevilq.wistride.core.model.ping.LatencyEvent
import io.github.daredevilq.wistride.domain.network.PingOutputParser
import io.github.daredevilq.wistride.domain.network.PingSequenceTracker
import io.github.daredevilq.wistride.domain.network.ProbeOutcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.util.Locale
import javax.inject.Inject

private const val GRACEFUL_EXIT_WAIT_MS = 300L
private const val MS_PER_SECOND = 1000.0

/**
 * Runs `ping -n -O -i <interval> <ip>` (`ping6` for IPv6) as one long-lived process and maps its
 * output to probes with [PingSequenceTracker]. With `-O`, iputils reports "no answer yet" when it
 * sends the next probe, so a probe is lost when no reply arrives within one interval; `-W` has no
 * effect without `-c`, hence it is not passed. Because a blocking `readLine` cannot be interrupted
 * by coroutine cancellation, cancelling destroys the process instead, which closes the stream and
 * ends the reader.
 */
class ProcessIcmpProber
    @Inject
    constructor(
        private val runner: ProcessRunner,
        private val clock: LatencyClock,
        @Dispatcher(WiStrideDispatcher.IO) private val io: CoroutineDispatcher,
    ) : IcmpProber {
        override fun probe(
            address: InetAddress,
            intervalMs: Long,
        ): Flow<LatencyEvent.Probe> =
            channelFlow {
                // withContext's prompt cancellation discards its result when the collector is cancelled
                // while ping is being forked, even under NonCancellable; the process is therefore
                // published from inside the block so the finally below can always destroy it.
                var started: RunningProcess? = null
                try {
                    withContext(io) {
                        started =
                            try {
                                runner.start(command(address, intervalMs))
                            } catch (e: IOException) {
                                throw IcmpUnusableException("ping could not be started", e)
                            }
                    }
                    val process = checkNotNull(started)
                    launch(io) {
                        val tracker = PingSequenceTracker()
                        var hadReplies = false
                        generateSequence { process.readLine() }
                            .flatMap { tracker.onLine(PingOutputParser.parsePingLine(it)).asSequence() }
                            .forEach {
                                if (it is ProbeOutcome.Answered) hadReplies = true
                                send(it.toProbe())
                            }
                        // ping without -c only ends on errors or when destroyed; either way it is not usable.
                        close(IcmpUnusableException("ping process ended", hadReplies = hadReplies))
                    }
                    awaitClose()
                } finally {
                    val process = started
                    if (process != null) withContext(NonCancellable + io) { terminate(process) }
                }
            }

        private fun ProbeOutcome.toProbe(): LatencyEvent.Probe =
            LatencyEvent.Probe(clock.wallClockMs(), (this as? ProbeOutcome.Answered)?.rttMs)

        private fun terminate(process: RunningProcess) {
            process.destroy()
            if (!process.waitForExit(GRACEFUL_EXIT_WAIT_MS)) {
                process.destroyForcibly()
                process.waitForExit(GRACEFUL_EXIT_WAIT_MS)
            }
        }

        private fun command(
            address: InetAddress,
            intervalMs: Long,
        ): List<String> =
            listOf(
                if (address is Inet6Address) "ping6" else "ping",
                "-n",
                "-O",
                "-i",
                String.format(Locale.ROOT, "%.1f", intervalMs / MS_PER_SECOND),
                address.hostAddress.orEmpty(),
            )
    }
