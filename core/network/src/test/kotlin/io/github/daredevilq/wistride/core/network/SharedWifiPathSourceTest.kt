package io.github.daredevilq.wistride.core.network

import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class SharedWifiPathSourceTest {
    private val handle = FakeNetworkHandle()
    private val path = WifiPath(handle, true, ip("192.168.1.1"), emptyList())
    private val subscriptions = AtomicInteger()
    private val active = AtomicInteger()

    private val upstream =
        flow {
            subscriptions.incrementAndGet()
            active.incrementAndGet()
            try {
                emit(path)
                awaitCancellation()
            } finally {
                active.decrementAndGet()
            }
        }

    @Test
    fun `many monitors share one upstream subscription and release it together`() =
        runTest {
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val monitor =
                DefaultLatencyMonitor(
                    pathSource = SharedWifiPathSource(upstream, backgroundScope),
                    icmp = FakeIcmp(),
                    tcp = FakeTcp(),
                    dns = FakeDns(),
                    localPermission = { true },
                    clock = FakeClock(testScheduler),
                    sdkInt = 36,
                    io = dispatcher,
                )
            val jobs =
                List(MONITORS) { index ->
                    backgroundScope.launch(dispatcher) {
                        monitor.monitor(PingTarget.Host("t$index", "8.8.8.8", 80), 1_000).collect { }
                    }
                }
            runCurrent()
            assertThat(subscriptions.get()).isEqualTo(1)
            assertThat(active.get()).isEqualTo(1)

            jobs.forEach { it.cancel() }
            runCurrent()
            // Still registered during the restart grace period…
            assertThat(active.get()).isEqualTo(1)
            advanceTimeBy(GRACE_MS)
            runCurrent()
            assertThat(active.get()).isEqualTo(0)
        }

    @Test
    fun `a collector returning within the grace period reuses the subscription`() =
        runTest {
            val shared = SharedWifiPathSource(upstream, backgroundScope)
            assertThat(shared.path.first()).isEqualTo(path)
            advanceTimeBy(GRACE_MS / 2)
            assertThat(shared.path.first()).isEqualTo(path)
            assertThat(subscriptions.get()).isEqualTo(1)
        }

    @Test
    fun `a restart after everyone left does not replay the old path`() =
        runTest {
            val shared = SharedWifiPathSource(upstream, backgroundScope)
            assertThat(shared.path.first()).isEqualTo(path)
            advanceTimeBy(GRACE_MS)
            runCurrent()
            assertThat(shared.path.replayCache).isEmpty()
            assertThat(active.get()).isEqualTo(0)
        }

    private companion object {
        const val MONITORS = 8

        /** One millisecond past the source's restart grace period. */
        const val GRACE_MS = 1_001L
    }
}
