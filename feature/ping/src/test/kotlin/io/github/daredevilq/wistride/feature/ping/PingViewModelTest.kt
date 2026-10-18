package io.github.daredevilq.wistride.feature.ping

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.core.model.permission.InMemoryPermissionRequestHistory
import io.github.daredevilq.wistride.core.model.permission.PermissionSnapshot
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.ping.LatencyEvent
import io.github.daredevilq.wistride.core.model.ping.LatencyUnavailableReason
import io.github.daredevilq.wistride.core.model.ping.PingMethod
import io.github.daredevilq.wistride.core.model.ping.PingSettings
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import io.github.daredevilq.wistride.core.network.LatencyMonitor
import io.github.daredevilq.wistride.core.testing.MainDispatcherRule
import io.github.daredevilq.wistride.domain.network.PING_STATS_WINDOW
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) // virtual-time helpers (advanceTimeBy, runCurrent) are still experimental.
class PingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private val repository = FakePingSettingsRepository()
    private val monitor = FakeLatencyMonitor()
    private val savedState = SavedStateHandle()

    // Lazy: viewModelScope must be created after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy {
        PingViewModel(repository, monitor, InMemoryPermissionRequestHistory(), savedState, dispatcher)
    }

    private val defaultIds = PingSettings.DEFAULT_TARGETS.map { it.id }

    private fun snapshotOf(
        status: PermissionStatus,
        vararg granted: String,
    ) = PermissionSnapshot(status, granted.toSet())

    private fun TestScope.collectState(initial: PermissionSnapshot? = snapshotOf(PermissionStatus.Granted)): Job {
        initial?.let(viewModel::onPermissionChanged)
        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return job
    }

    private val content get() = viewModel.uiState.value as PingUiState.Content

    private fun target(id: String) = content.targets.first { it.id == id }

    @Test
    fun nothingIsMeasuredBeforeTheFirstPermissionSnapshot() =
        runTest(dispatcher) {
            collectState(initial = null)
            assertThat(viewModel.uiState.value).isEqualTo(PingUiState.Loading)
            assertThat(monitor.starts).isEmpty()
        }

    @Test
    fun startsExactlyOneCollectionPerTarget() =
        runTest(dispatcher) {
            collectState()
            assertThat(monitor.starts.map { it.first }).containsExactlyElementsIn(defaultIds)
            assertThat(monitor.starts.map { it.second }.toSet()).containsExactly(PingSettings.DEFAULT_INTERVAL_MS)
            assertThat(content.targets).hasSize(defaultIds.size)
        }

    @Test
    fun addingATargetStartsOnlyThatTarget() =
        runTest(dispatcher) {
            collectState()
            monitor.emit("gateway", probe(5.0))
            runCurrent()

            viewModel.addTarget("example.org", 443)
            runCurrent()

            defaultIds.forEach { assertThat(monitor.startsOf(it)).isEqualTo(1) }
            assertThat(monitor.startsOf("user:0")).isEqualTo(1)
            assertThat(monitor.activeIds).contains("user:0")
            assertThat(target("gateway").stats.sent).isEqualTo(1)
            assertThat(target("user:0").host).isEqualTo("example.org")
        }

    @Test
    fun removingATargetCancelsOnlyThatCollection() =
        runTest(dispatcher) {
            collectState()
            monitor.emit("gateway", probe(5.0))
            runCurrent()

            viewModel.removeTarget("default:8.8.8.8")
            runCurrent()

            assertThat(monitor.activeIds).doesNotContain("default:8.8.8.8")
            assertThat(monitor.activeIds).hasSize(defaultIds.size - 1)
            assertThat(content.targets.map { it.id }).doesNotContain("default:8.8.8.8")
            assertThat(target("gateway").stats.sent).isEqualTo(1)
        }

    @Test
    fun intervalChangeRestartsEveryTargetWithTheNewInterval() =
        runTest(dispatcher) {
            collectState()
            monitor.emit("gateway", probe(5.0))
            runCurrent()

            viewModel.setInterval(2_000L)
            runCurrent()

            defaultIds.forEach { assertThat(monitor.startsOf(it)).isEqualTo(2) }
            assertThat(
                monitor.starts
                    .takeLast(defaultIds.size)
                    .map { it.second }
                    .toSet(),
            ).containsExactly(2_000L)
            assertThat(monitor.activeIds).containsExactlyElementsIn(defaultIds)
            assertThat(content.intervalMs).isEqualTo(2_000L)
            // A new session: the old history was recorded at another cadence.
            assertThat(target("gateway").stats.sent).isEqualTo(0)
        }

    @Test
    fun permissionChangeRestartsTheMeasurements() =
        runTest(dispatcher) {
            collectState(snapshotOf(PermissionStatus.NotRequested))

            viewModel.onPermissionChanged(snapshotOf(PermissionStatus.Granted, "p"))
            runCurrent()

            defaultIds.forEach { assertThat(monitor.startsOf(it)).isEqualTo(2) }
            assertThat(monitor.activeIds).containsExactlyElementsIn(defaultIds)
            assertThat(content.permission).isEqualTo(PermissionStatus.Granted)
        }

    @Test
    fun repeatedIdenticalSnapshotDoesNotRestart() =
        runTest(dispatcher) {
            collectState(snapshotOf(PermissionStatus.Granted))
            viewModel.onPermissionChanged(snapshotOf(PermissionStatus.Granted))
            runCurrent()
            defaultIds.forEach { assertThat(monitor.startsOf(it)).isEqualTo(1) }
        }

    @Test
    fun probesFeedStatsSparklineAndChart() =
        runTest(dispatcher) {
            collectState()
            monitor.emit(
                "gateway",
                LatencyEvent.MethodSelected(PingMethod.Icmp, "192.168.1.1"),
                probe(10.0),
                probe(null),
                probe(20.0),
            )
            runCurrent()

            val gateway = target("gateway")
            assertThat(gateway.method).isEqualTo(PingMethod.Icmp)
            assertThat(gateway.address).isEqualTo("192.168.1.1")
            assertThat(gateway.stats.sent).isEqualTo(3)
            assertThat(gateway.stats.received).isEqualTo(2)
            assertThat(gateway.stats.avgMs).isEqualTo(15.0)
            assertThat(gateway.stats.jitterMs).isEqualTo(10.0)
            assertThat(gateway.currentMs).isEqualTo(20.0)
            assertThat(gateway.sparkline).containsExactly(10.0, null, 20.0).inOrder()
            assertThat(gateway.chart).hasSize(3)
        }

    @Test
    fun windowsAreBoundedForStatsSparklineAndChart() =
        runTest(dispatcher) {
            collectState()
            monitor.emit("gateway", *Array(CHART_HISTORY_SIZE + 10) { probe(it.toDouble()) })
            runCurrent()

            val gateway = target("gateway")
            assertThat(gateway.chart).hasSize(CHART_HISTORY_SIZE)
            assertThat(gateway.sparkline).hasSize(SPARKLINE_SIZE)
            assertThat(gateway.stats.sent).isEqualTo(PING_STATS_WINDOW)
            assertThat(gateway.currentMs).isEqualTo((CHART_HISTORY_SIZE + 9).toDouble())
        }

    @Test
    fun methodFallbackUpdatesTheMethod() =
        runTest(dispatcher) {
            collectState()
            val id = "default:google.com"
            monitor.emit(id, LatencyEvent.MethodSelected(PingMethod.Icmp, "142.250.0.1"))
            runCurrent()
            assertThat(target(id).method).isEqualTo(PingMethod.Icmp)

            monitor.emit(id, LatencyEvent.MethodSelected(PingMethod.TcpConnect(443), "142.250.0.1"))
            runCurrent()
            assertThat(target(id).method).isEqualTo(PingMethod.TcpConnect(443))
        }

    @Test
    fun dnsTimeIsKept() =
        runTest(dispatcher) {
            collectState()
            monitor.emit("default:google.com", LatencyEvent.DnsResolved(0L, 12.0))
            runCurrent()
            assertThat(target("default:google.com").dnsMs).isEqualTo(12.0)
        }

    @Test
    fun unavailableReasonsMapPerTargetAndClearOnProbe() =
        runTest(dispatcher) {
            collectState()
            monitor.emit("gateway", LatencyEvent.Unavailable(LatencyUnavailableReason.NoGateway))
            monitor.emit("default:google.com", LatencyEvent.Unavailable(LatencyUnavailableReason.UnknownHost))
            monitor.emit(
                "default:1.1.1.1",
                LatencyEvent.Unavailable(LatencyUnavailableReason.NeedsPermission("perm")),
            )
            runCurrent()

            assertThat(target("gateway").unavailable).isEqualTo(LatencyUnavailableReason.NoGateway)
            assertThat(target("default:google.com").unavailable).isEqualTo(LatencyUnavailableReason.UnknownHost)
            assertThat(target("default:1.1.1.1").unavailable)
                .isEqualTo(LatencyUnavailableReason.NeedsPermission("perm"))
            assertThat(target("default:8.8.8.8").unavailable).isNull()

            monitor.emit("gateway", probe(3.0))
            runCurrent()
            assertThat(target("gateway").unavailable).isNull()
        }

    @Test
    fun everyTargetWithoutWifiBecomesAScreenLevelState() =
        runTest(dispatcher) {
            collectState()
            defaultIds.forEach { monitor.emit(it, LatencyEvent.Unavailable(LatencyUnavailableReason.NoWifi)) }
            runCurrent()
            assertThat(
                viewModel.uiState.value,
            ).isEqualTo(PingUiState.NoWifi(PingSettings.DEFAULT_INTERVAL_MS, defaultIds.size))
        }

    @Test
    fun partialNoWifiStaysContent() =
        runTest(dispatcher) {
            collectState()
            monitor.emit("gateway", LatencyEvent.Unavailable(LatencyUnavailableReason.NoWifi))
            runCurrent()
            assertThat(viewModel.uiState.value).isInstanceOf(PingUiState.Content::class.java)
        }

    @Test
    fun emptyTargetListIsEmptyState() =
        runTest(dispatcher) {
            repository.state.value = PingSettings(targets = emptyList(), intervalMs = 2_000L)
            collectState()
            assertThat(viewModel.uiState.value).isEqualTo(PingUiState.Empty(2_000L))
            assertThat(monitor.starts).isEmpty()
        }

    @Test
    fun selectionLivesInSavedStateAndIgnoresUnknownIds() =
        runTest(dispatcher) {
            collectState()
            assertThat(content.selectedId).isNull()

            viewModel.selectTarget("gateway")
            runCurrent()
            assertThat(content.selectedId).isEqualTo("gateway")
            assertThat(savedState.get<String?>("selectedId")).isEqualTo("gateway")

            viewModel.removeTarget("gateway")
            runCurrent()
            assertThat(content.selectedId).isNull()
            assertThat(savedState.get<String?>("selectedId")).isNull()

            viewModel.selectTarget("ghost")
            runCurrent()
            assertThat(content.selectedId).isNull()
        }

    @Test
    fun restoreDefaultTargetsReAddsRemovedOnes() =
        runTest(dispatcher) {
            collectState()
            viewModel.removeTarget("gateway")
            runCurrent()
            viewModel.restoreDefaultTargets()
            runCurrent()
            assertThat(monitor.startsOf("gateway")).isEqualTo(2)
            assertThat(content.targets.map { it.id }).containsExactlyElementsIn(defaultIds)
        }

    @Test
    fun unsubscribingStopsAllCollectionsAfterTheTimeout() =
        runTest(dispatcher) {
            val job = collectState()
            assertThat(monitor.activeIds).containsExactlyElementsIn(defaultIds)

            job.cancel()
            runCurrent()
            advanceTimeBy(4_999L)
            assertThat(monitor.activeIds).containsExactlyElementsIn(defaultIds)

            advanceTimeBy(2L)
            runCurrent()
            assertThat(monitor.activeIds).isEmpty()
        }

    @Test
    fun resubscribingWithinTheTimeoutKeepsMeasuring() =
        runTest(dispatcher) {
            collectState().cancel()
            runCurrent()
            advanceTimeBy(1_000L)
            collectState(initial = null)
            advanceTimeBy(10_000L)
            runCurrent()
            assertThat(monitor.activeIds).containsExactlyElementsIn(defaultIds)
            defaultIds.forEach { assertThat(monitor.startsOf(it)).isEqualTo(1) }
        }

    @Test
    fun aCancelledCollectionCannotWriteIntoTheNextSession() =
        runTest(dispatcher) {
            // Delivers one more event while it is being cancelled, like a collection that is still winding down.
            val leaking =
                PingViewModel(
                    repository,
                    LingeringMonitor,
                    InMemoryPermissionRequestHistory(),
                    SavedStateHandle(),
                    dispatcher,
                )
            leaking.onPermissionChanged(snapshotOf(PermissionStatus.Granted))
            backgroundScope.launch { leaking.uiState.collect {} }
            runCurrent()

            repository.state.value = repository.state.value.copy(intervalMs = 2_000L)
            runCurrent()

            val state = leaking.uiState.value as PingUiState.Content
            assertThat(state.targets.map { it.stats.sent }.toSet()).containsExactly(0)
        }

    @Test
    fun targetUiForHostCarriesItsName() {
        val ui = TargetRuntime().toUi(PingTarget.Host("a", "example.org", 22))
        assertThat(ui.host).isEqualTo("example.org")
        assertThat(ui.stats.sent).isEqualTo(0)
        assertThat(ui.currentMs).isNull()
    }
}

private object LingeringMonitor : LatencyMonitor {
    override fun monitor(
        target: PingTarget,
        intervalMs: Long,
    ): Flow<LatencyEvent> =
        object : Flow<LatencyEvent> {
            override suspend fun collect(collector: FlowCollector<LatencyEvent>) {
                try {
                    awaitCancellation()
                } finally {
                    collector.emit(probe(1.0))
                }
            }
        }
}
