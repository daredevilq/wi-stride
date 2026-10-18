package io.github.daredevilq.wistride.core.wifi

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.core.model.wifi.ConnectionState
import io.github.daredevilq.wistride.core.wifi.source.ConnectivitySource
import io.github.daredevilq.wistride.core.wifi.source.RawScanResult
import io.github.daredevilq.wistride.core.wifi.source.ScanResultsSource
import io.github.daredevilq.wistride.core.wifi.source.WifiNetworkInfo
import io.github.daredevilq.wistride.core.wifi.source.WifiPermissionChecker
import io.github.daredevilq.wistride.core.wifi.source.WifiStateSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

private class Subscribable<T>(
    initial: T,
) {
    val state = MutableStateFlow(initial)
    val subscribers = AtomicInteger()
    val flow: Flow<T> =
        state
            .onStart { subscribers.incrementAndGet() }
            .onCompletion { subscribers.decrementAndGet() }
}

private class FakeSources {
    val wifiEnabled = Subscribable(true)
    val airplane = Subscribable(false)
    val location = Subscribable(true)
    val network = Subscribable<WifiNetworkInfo?>(null)
    val scans = Subscribable<List<RawScanResult>>(emptyList())
    var hasFine = true
    var canScan = true
    var scanRequests = 0

    val all get() =
        listOf(
            wifiEnabled.subscribers,
            airplane.subscribers,
            location.subscribers,
            network.subscribers,
            scans.subscribers,
        )

    val state =
        object : WifiStateSource {
            override val wifiEnabled = this@FakeSources.wifiEnabled.flow
            override val airplaneModeOn = airplane.flow
            override val locationEnabled = location.flow
        }
    val connectivity =
        object : ConnectivitySource {
            override val wifiNetwork = network.flow
        }
    val scan =
        object : ScanResultsSource {
            override val scanResults = scans.flow

            override fun requestScan(): Boolean {
                scanRequests++
                return true
            }
        }
    val permissions =
        object : WifiPermissionChecker {
            override fun hasFineLocation() = hasFine

            override fun canReadScanResults() = canScan
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWifiConnectionRepositoryTest {
    private fun TestScope.repo(fakes: FakeSources) =
        DefaultWifiConnectionRepository(
            fakes.state,
            fakes.connectivity,
            fakes.scan,
            fakes.permissions,
            36,
            UnconfinedTestDispatcher(testScheduler),
        )

    @Test
    fun noSourceIsSubscribedBeforeCollectionAndAllReleasedAfterCancel() =
        runTest {
            val fakes = FakeSources()
            val repository = repo(fakes)
            assertThat(fakes.all.map { it.get() }).containsExactly(0, 0, 0, 0, 0)

            repository.snapshots.test {
                awaitItem()
                assertThat(fakes.all.map { it.get() }).containsExactly(1, 1, 1, 1, 1)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(fakes.all.map { it.get() }).containsExactly(0, 0, 0, 0, 0)
        }

    @Test
    fun emitsStateTransitions() =
        runTest {
            val fakes = FakeSources()
            repo(fakes).snapshots.test {
                assertThat(awaitItem().state).isEqualTo(ConnectionState.Disconnected)
                fakes.wifiEnabled.state.value = false
                assertThat(awaitItem().state).isEqualTo(ConnectionState.WifiOff)
                fakes.airplane.state.value = true
                assertThat(awaitItem().state).isEqualTo(ConnectionState.AirplaneMode)
                fakes.network.state.value = network()
                assertThat(awaitItem().state).isInstanceOf(ConnectionState.Connected::class.java)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun identicalSnapshotsAreNotRepeated() =
        runTest {
            val fakes = FakeSources()
            repo(fakes).snapshots.test {
                awaitItem()
                fakes.scans.state.value = listOf(rawScan())
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun scanIsRequestedOncePerCollectionWhenConnectedBssidIsNotCached() =
        runTest {
            val fakes = FakeSources()
            fakes.network.state.value = network()
            val repository = repo(fakes)
            repository.snapshots.test {
                awaitItem()
                assertThat(fakes.scanRequests).isEqualTo(1)
                fakes.network.state.value = network(info = rawWifiInfo(rssi = -60))
                awaitItem()
                assertThat(fakes.scanRequests).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
            repository.snapshots.test {
                awaitItem()
                assertThat(fakes.scanRequests).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun scanIsNotRequestedWithoutPermission() =
        runTest {
            val fakes = FakeSources()
            fakes.canScan = false
            fakes.network.state.value = network()
            repo(fakes).snapshots.test {
                awaitItem()
                assertThat(fakes.scanRequests).isEqualTo(0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun scanIsNotRequestedWhenCacheHasBssidOrBssidIsUnknownOrDisconnected() =
        runTest {
            val cached =
                FakeSources().apply {
                    network.state.value = network()
                    scans.state.value = listOf(rawScan())
                }
            val redacted =
                FakeSources().apply {
                    network.state.value =
                        network(info = rawWifiInfo(bssid = "02:00:00:00:00:00"))
                }
            val disconnected = FakeSources()
            listOf(cached, redacted, disconnected).forEach { fakes ->
                repo(fakes).snapshots.test {
                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }
                assertThat(fakes.scanRequests).isEqualTo(0)
            }
        }
}
