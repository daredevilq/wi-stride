package io.github.daredevilq.wistride.core.network

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectTcpProberTest {
    private fun TestScope.prober(clock: FakeClock) = ConnectTcpProber(clock, StandardTestDispatcher(testScheduler))

    private val address = ip("8.8.8.8")

    @Test
    fun `successful connect reports its duration`() =
        runTest {
            val clock = FakeClock(testScheduler)
            val handle = FakeNetworkHandle(onConnect = { _, _, _ -> clock.extraNanos += 12_000_000L })
            prober(clock).probe(handle, address, 443, 1_000).test {
                val probe = awaitItem()
                assertThat(probe.rttMs).isWithin(0.001).of(12.0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refused connection counts as answered`() =
        runTest {
            val clock = FakeClock(testScheduler)
            val handle =
                FakeNetworkHandle(
                    onConnect = { _, _, _ ->
                        clock.extraNanos += 3_000_000L
                        throw ConnectException("failed to connect: ECONNREFUSED (Connection refused)")
                    },
                )
            prober(clock).probe(handle, address, 80, 1_000).test {
                assertThat(awaitItem().rttMs).isWithin(0.001).of(3.0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `timeout and unreachable are lost probes`() =
        runTest {
            val clock = FakeClock(testScheduler)
            var calls = 0
            val handle =
                FakeNetworkHandle(
                    onConnect = { _, _, _ ->
                        if (calls++ == 0) throw SocketTimeoutException("timed out")
                        throw NoRouteToHostException("no route")
                    },
                )
            prober(clock).probe(handle, address, 443, 1_000).test {
                assertThat(awaitItem().rttMs).isNull()
                assertThat(awaitItem().rttMs).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `connect timeout is the interval clamped to 500 to 2000 ms`() =
        runTest {
            val clock = FakeClock(testScheduler)
            listOf(100L to 500, 1_000L to 1_000, 5_000L to 2_000).forEach { (interval, expected) ->
                val handle = FakeNetworkHandle()
                prober(clock).probe(handle, address, 443, interval).test {
                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }
                assertThat(handle.connects.first().third).isEqualTo(expected)
            }
        }

    @Test
    fun `probes run at a fixed rate`() =
        runTest {
            val clock = FakeClock(testScheduler)
            val handle = FakeNetworkHandle()
            val job = backgroundScope.launch { prober(clock).probe(handle, address, 443, 1_000).collect { } }
            testScheduler.advanceTimeBy(2_500)
            testScheduler.runCurrent()
            assertThat(handle.connects).hasSize(3)
            job.cancel()
        }
}
