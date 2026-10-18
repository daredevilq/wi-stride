package io.github.daredevilq.wistride.core.network

import android.Manifest
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.core.model.ping.LatencyEvent
import io.github.daredevilq.wistride.core.model.ping.LatencyUnavailableReason
import io.github.daredevilq.wistride.core.model.ping.PingMethod
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLatencyMonitorTest {
    private val handle = FakeNetworkHandle()
    private val link = listOf(LinkPrefix(ip("192.168.1.20"), 24))
    private val gateway = ip("192.168.1.1")
    private val icmp = FakeIcmp()
    private val tcp = FakeTcp()
    private val dns = FakeDns()
    private var permissionGranted = false
    private val pathSource = FakePathSource(path())

    private fun path(
        isDefault: Boolean = true,
        gateway: java.net.InetAddress? = this.gateway,
    ) = WifiPath(handle, isDefault, gateway, link)

    private fun kotlinx.coroutines.test.TestScope.monitor(sdkInt: Int = 36) =
        DefaultLatencyMonitor(
            pathSource = pathSource,
            icmp = icmp,
            tcp = tcp,
            dns = dns,
            localPermission = { permissionGranted },
            clock = FakeClock(testScheduler),
            sdkInt = sdkInt,
            io = UnconfinedTestDispatcher(testScheduler),
        )

    private fun host(
        host: String,
        port: Int? = null,
    ) = PingTarget.Host("id", host, port)

    private suspend fun ReceiveTurbine<LatencyEvent>.method() = awaitItem() as LatencyEvent.MethodSelected

    @Test
    fun `target with a port uses TCP only`() =
        runTest {
            monitor().monitor(host("8.8.8.8", 80), 1_000).test {
                assertThat(awaitItem()).isEqualTo(LatencyEvent.MethodSelected(PingMethod.TcpConnect(80), "8.8.8.8"))
                assertThat(awaitItem()).isInstanceOf(LatencyEvent.Probe::class.java)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(icmp.calls.get()).isEqualTo(0)
            assertThat(tcp.calls).containsExactly(80)
        }

    @Test
    fun `Wi-Fi as default network uses ICMP`() =
        runTest {
            icmp.behaviour = { flow { emit(LatencyEvent.Probe(5, 9.0)) } }
            monitor().monitor(host("8.8.8.8"), 1_000).test {
                assertThat(awaitItem()).isEqualTo(LatencyEvent.MethodSelected(PingMethod.Icmp, "8.8.8.8"))
                assertThat(awaitItem()).isEqualTo(LatencyEvent.Probe(5, 9.0))
                // The fake ping ends, which counts as unusable; the next events belong to the fallback test.
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ICMP exiting early falls back to TCP 443`() =
        runTest {
            icmp.behaviour = { flow { throw IcmpUnusableException("ping process ended") } }
            monitor().monitor(host("8.8.8.8"), 1_000).test {
                assertThat(awaitItem()).isEqualTo(LatencyEvent.MethodSelected(PingMethod.Icmp, "8.8.8.8"))
                assertThat(awaitItem()).isEqualTo(LatencyEvent.MethodSelected(PingMethod.TcpConnect(443), "8.8.8.8"))
                assertThat(awaitItem()).isInstanceOf(LatencyEvent.Probe::class.java)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(tcp.calls).containsExactly(443)
        }

    @Test
    fun `Wi-Fi that is not the default network uses TCP 443`() =
        runTest {
            pathSource.state.value = path(isDefault = false)
            monitor().monitor(host("8.8.8.8"), 1_000).test {
                assertThat(awaitItem()).isEqualTo(LatencyEvent.MethodSelected(PingMethod.TcpConnect(443), "8.8.8.8"))
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(icmp.calls.get()).isEqualTo(0)
        }

    @Test
    fun `no Wi-Fi is unavailable and recovers when a path appears`() =
        runTest {
            pathSource.state.value = null
            monitor().monitor(host("8.8.8.8", 80), 1_000).test {
                assertThat(awaitItem()).isEqualTo(LatencyEvent.Unavailable(LatencyUnavailableReason.NoWifi))
                pathSource.state.value = path()
                assertThat(method().method).isEqualTo(PingMethod.TcpConnect(80))
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `losing Wi-Fi stops probing`() =
        runTest {
            monitor().monitor(host("8.8.8.8", 80), 1_000).test {
                awaitItem()
                awaitItem()
                pathSource.state.value = null
                assertThat(awaitItem()).isEqualTo(LatencyEvent.Unavailable(LatencyUnavailableReason.NoWifi))
                assertThat(tcp.active.get()).isEqualTo(0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `gateway target without a default route is unavailable`() =
        runTest {
            pathSource.state.value = path(gateway = null)
            monitor().monitor(PingTarget.Gateway, 1_000).test {
                assertThat(awaitItem()).isEqualTo(LatencyEvent.Unavailable(LatencyUnavailableReason.NoGateway))
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(icmp.calls.get()).isEqualTo(0)
        }

    @Test
    fun `gateway target probes the gateway address without DNS`() =
        runTest {
            monitor().monitor(PingTarget.Gateway, 1_000).test {
                assertThat(awaitItem()).isEqualTo(LatencyEvent.MethodSelected(PingMethod.Icmp, "192.168.1.1"))
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(dns.hosts).isEmpty()
        }

    @Test
    fun `local address on SDK 37 without permission starts no probe`() =
        runTest {
            monitor(sdkInt = 37).monitor(PingTarget.Gateway, 1_000).test {
                assertThat(awaitItem())
                    .isEqualTo(
                        LatencyEvent.Unavailable(
                            LatencyUnavailableReason.NeedsPermission(Manifest.permission.ACCESS_LOCAL_NETWORK),
                        ),
                    )
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(icmp.calls.get()).isEqualTo(0)
            assertThat(tcp.calls).isEmpty()
        }

    @Test
    fun `local address on SDK 37 with permission is probed`() =
        runTest {
            permissionGranted = true
            monitor(sdkInt = 37).monitor(host("192.168.1.50", 22), 1_000).test {
                assertThat(method().method).isEqualTo(PingMethod.TcpConnect(22))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `local address on SDK 36 is probed without the permission`() =
        runTest {
            monitor(sdkInt = 36).monitor(host("192.168.1.50", 22), 1_000).test {
                assertThat(method().method).isEqualTo(PingMethod.TcpConnect(22))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `public address on SDK 37 needs no permission`() =
        runTest {
            monitor(sdkInt = 37).monitor(host("8.8.8.8", 53), 1_000).test {
                assertThat(method().method).isEqualTo(PingMethod.TcpConnect(53))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `host names are resolved on the Wi-Fi network and timed separately`() =
        runTest {
            handle.onResolve = { listOf(ip("2001:4860::8888"), ip("142.250.1.1")) }
            monitor().monitor(host("example.com", 443), 1_000).test {
                assertThat(
                    awaitItem(),
                ).isEqualTo(LatencyEvent.MethodSelected(PingMethod.TcpConnect(443), "142.250.1.1"))
                val rest = listOf(awaitItem(), awaitItem())
                assertThat(rest.filterIsInstance<LatencyEvent.DnsResolved>().map { it.durationMs })
                    .containsExactly(FakeDns.DNS_MS)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(dns.hosts).containsExactly("example.com")
        }

    @Test
    fun `dns is measured again every 30 seconds`() =
        runTest {
            handle.onResolve = { listOf(ip("142.250.1.1")) }
            monitor().monitor(host("example.com", 443), 1_000).test {
                skipItems(3)
                testScheduler.advanceTimeBy(30_001)
                assertThat(awaitItem()).isInstanceOf(LatencyEvent.DnsResolved::class.java)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(dns.hosts).hasSize(2)
        }

    @Test
    fun `IP literals are not timed with DNS`() =
        runTest {
            monitor().monitor(host("8.8.8.8", 443), 1_000).test {
                skipItems(2)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(dns.hosts).isEmpty()
        }

    @Test
    fun `unknown host is reported and retried every 10 seconds`() =
        runTest {
            var attempts = 0
            handle.onResolve = {
                if (attempts++ == 0) throw UnknownHostException(it) else listOf(ip("93.184.216.34"))
            }
            monitor().monitor(host("example.com", 443), 1_000).test {
                assertThat(awaitItem()).isEqualTo(LatencyEvent.Unavailable(LatencyUnavailableReason.UnknownHost))
                expectNoEvents()
                testScheduler.advanceTimeBy(10_001)
                assertThat(method().address).isEqualTo("93.184.216.34")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `cancelling the collector stops the probers`() =
        runTest {
            monitor().monitor(host("8.8.8.8"), 1_000).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(icmp.active.get()).isEqualTo(0)
        }
}
