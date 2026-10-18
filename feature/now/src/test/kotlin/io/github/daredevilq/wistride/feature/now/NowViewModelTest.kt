package io.github.daredevilq.wistride.feature.now

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.core.model.Availability
import io.github.daredevilq.wistride.core.model.UnavailableReason
import io.github.daredevilq.wistride.core.model.available
import io.github.daredevilq.wistride.core.model.permission.InMemoryPermissionRequestHistory
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.wifi.ConnectionState
import io.github.daredevilq.wistride.core.model.wifi.SignalLevel
import io.github.daredevilq.wistride.core.model.wifi.SignalQuality
import io.github.daredevilq.wistride.core.model.wifi.WifiBand
import io.github.daredevilq.wistride.core.model.wifi.WifiGeneration
import io.github.daredevilq.wistride.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) // virtual-time helpers (advanceTimeBy, runCurrent) are still experimental.
class NowViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private val repository = FakeWifiConnectionRepository()

    // Lazy: viewModelScope must be created after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy { NowViewModel(repository, InMemoryPermissionRequestHistory()) }

    private fun TestScope.collectState(): Job = backgroundScope.launch { viewModel.uiState.collect {} }

    private val loaded get() = viewModel.uiState.value as NowUiState.Loaded

    private val connectedDetails get() = (loaded.link as LinkStatus.Connected).details

    @Test
    fun startsLoadingThenMapsConnectedSnapshot() =
        runTest(dispatcher) {
            viewModel.uiState.test {
                assertThat(awaitItem()).isEqualTo(NowUiState.Loading)
                val state = awaitItem() as NowUiState.Loaded
                val details = (state.link as LinkStatus.Connected).details
                assertThat(details.ssid).isEqualTo("HomeNet".available())
                assertThat(details.band).isEqualTo(WifiBand.Ghz5.available())
                assertThat(details.channel).isEqualTo(36.available())
                assertThat(details.quality).isEqualTo(SignalQuality(90, SignalLevel.Good))
                val generation = (details.generation as Availability.Available).value
                assertThat(generation.generation).isEqualTo(WifiGeneration.WiFi6)
                assertThat(state.permission).isEqualTo(PermissionStatus.NotRequested)
                assertThat(state.locationServicesOff).isFalse()
            }
        }

    @Test
    fun sixGhzAxIsWifi6E() =
        runTest(dispatcher) {
            repository.source.value = snapshot(ConnectionState.Connected(connection(frequencyMhz = 5955.available())))
            collectState()
            runCurrent()
            val generation = (connectedDetails.generation as Availability.Available).value
            assertThat(generation.generation).isEqualTo(WifiGeneration.WiFi6E)
            assertThat(connectedDetails.band).isEqualTo(WifiBand.Ghz6.available())
        }

    @Test
    fun offGridFrequencyIsNotReportedNeverZero() =
        runTest(dispatcher) {
            repository.source.value = snapshot(ConnectionState.Connected(connection(frequencyMhz = 1234.available())))
            collectState()
            runCurrent()
            val notReported = Availability.Unavailable(UnavailableReason.NotReportedByDevice)
            assertThat(connectedDetails.channel).isEqualTo(notReported)
            assertThat(connectedDetails.band).isEqualTo(notReported)
        }

    @Test
    fun unavailableRssiHasNoQuality() =
        runTest(dispatcher) {
            repository.source.value = snapshot(ConnectionState.Connected(connection(rssiDbm = Redacted)))
            collectState()
            runCurrent()
            assertThat(connectedDetails.quality).isNull()
        }

    @Test
    fun mapsWifiOffAirplaneAndDisconnected() =
        runTest(dispatcher) {
            collectState()
            runCurrent()
            val expected =
                mapOf(
                    ConnectionState.WifiOff to LinkStatus.WifiOff,
                    ConnectionState.AirplaneMode to LinkStatus.AirplaneMode,
                    ConnectionState.Disconnected to LinkStatus.Disconnected,
                )
            expected.forEach { (state, link) ->
                repository.source.value = snapshot(state)
                runCurrent()
                assertThat(loaded.link).isEqualTo(link)
            }
        }

    @Test
    fun locationOffIsOnlyFlaggedWhenPermissionGranted() =
        runTest(dispatcher) {
            repository.source.value = snapshot(environment = DefaultEnvironment.copy(locationEnabled = false))
            collectState()
            runCurrent()
            assertThat(loaded.locationServicesOff).isFalse()
            viewModel.onPermissionStatusChanged(PermissionStatus.Granted)
            runCurrent()
            assertThat(loaded.locationServicesOff).isTrue()
        }

    @Test
    fun historyGrowsOncePerSecond() =
        runTest(dispatcher) {
            repository.source.value = connectedSnapshot(-50)
            collectState()
            runCurrent()
            assertThat(loaded.rssiHistory).isEmpty()
            advanceTimeBy(1_500)
            repository.source.value = connectedSnapshot(-60)
            advanceTimeBy(2_000)
            assertThat(loaded.rssiHistory).containsExactly(-50, -60, -60).inOrder()
        }

    @Test
    fun historyKeepsLast60SamplesAndDropsOldest() =
        runTest(dispatcher) {
            repository.source.value = connectedSnapshot(-50)
            collectState()
            runCurrent()
            advanceTimeBy(1_500)
            repository.source.value = connectedSnapshot(-60)
            advanceTimeBy(61_000)
            val history = loaded.rssiHistory
            assertThat(history).hasSize(60)
            assertThat(history.toSet()).containsExactly(-60)
        }

    @Test
    fun historyHoldsNullWhenNotConnectedOrRssiUnavailable() =
        runTest(dispatcher) {
            repository.source.value = snapshot(ConnectionState.Disconnected)
            collectState()
            runCurrent()
            advanceTimeBy(2_500)
            repository.source.value = snapshot(ConnectionState.Connected(connection(rssiDbm = Redacted)))
            advanceTimeBy(1_000)
            repository.source.value = connectedSnapshot(-70)
            advanceTimeBy(1_000)
            assertThat(loaded.rssiHistory).containsExactly(null, null, null, -70).inOrder()
        }

    @Test
    fun resubscribesWhenPermissionIsGranted() =
        runTest(dispatcher) {
            collectState()
            runCurrent()
            assertThat(repository.subscriptions).isEqualTo(1)

            viewModel.onPermissionStatusChanged(PermissionStatus.Denied)
            runCurrent()
            assertThat(repository.subscriptions).isEqualTo(2)

            viewModel.onPermissionStatusChanged(PermissionStatus.Granted)
            runCurrent()
            assertThat(repository.subscriptions).isEqualTo(3)
            assertThat(repository.activeCollectors).isEqualTo(1)
            assertThat(loaded.permission).isEqualTo(PermissionStatus.Granted)
        }

    @Test
    fun doesNothingWithoutACollector() =
        runTest(dispatcher) {
            advanceTimeBy(10_000)
            assertThat(repository.subscriptions).isEqualTo(0)
            assertThat(viewModel.uiState.value).isEqualTo(NowUiState.Loading)
        }

    @Test
    fun stopsSubscribingAndSamplingAfterTheCollectorLeaves() =
        runTest(dispatcher) {
            val collector = collectState()
            runCurrent()
            advanceTimeBy(2_500)
            collector.cancel()
            advanceTimeBy(5_001)
            assertThat(repository.activeCollectors).isEqualTo(0)
            val frozen = loaded.rssiHistory
            advanceTimeBy(10_000)
            assertThat(loaded.rssiHistory).isEqualTo(frozen)
        }

    @Test
    fun anyPermissionStatusChangeResubscribesToTheRepository() =
        runTest(dispatcher) {
            collectState()
            runCurrent()
            assertThat(repository.subscriptions).isEqualTo(1)
            viewModel.onPermissionStatusChanged(PermissionStatus.Denied)
            runCurrent()
            assertThat(repository.subscriptions).isEqualTo(2)
        }
}
