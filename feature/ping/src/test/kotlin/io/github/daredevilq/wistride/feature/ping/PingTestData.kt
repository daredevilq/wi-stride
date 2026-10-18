package io.github.daredevilq.wistride.feature.ping

import io.github.daredevilq.wistride.core.datastore.PingSettingsRepository
import io.github.daredevilq.wistride.core.model.ping.LatencyEvent
import io.github.daredevilq.wistride.core.model.ping.LatencyUnavailableReason
import io.github.daredevilq.wistride.core.model.ping.PingMethod
import io.github.daredevilq.wistride.core.model.ping.PingSettings
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import io.github.daredevilq.wistride.core.network.LatencyMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** In-memory settings; ids of user targets are `user:<n>`. */
class FakePingSettingsRepository(
    initial: PingSettings = PingSettings.DEFAULT,
) : PingSettingsRepository {
    val state = MutableStateFlow(initial)
    private var counter = 0

    override val settings: Flow<PingSettings> = state

    override suspend fun addTarget(
        host: String,
        port: Int?,
    ): PingTarget.Host {
        val target = PingTarget.Host(id = "user:${counter++}", host = host, port = port)
        state.value = state.value.copy(targets = state.value.targets + target)
        return target
    }

    override suspend fun removeTarget(id: String) {
        state.value = state.value.copy(targets = state.value.targets.filterNot { it.id == id })
    }

    override suspend fun setInterval(intervalMs: Long) {
        state.value = state.value.copy(intervalMs = intervalMs)
    }

    override suspend fun restoreDefaultTargets() {
        state.value = state.value.copy(targets = PingSettings.DEFAULT_TARGETS)
    }
}

/** Counts how often and with which interval each target was collected, and which collections are live. */
class FakeLatencyMonitor : LatencyMonitor {
    private val sources = mutableMapOf<String, MutableSharedFlow<LatencyEvent>>()
    private val activeCounts = mutableMapOf<String, Int>()
    private val startLog = mutableListOf<Pair<String, Long>>()

    val starts: List<Pair<String, Long>> get() = startLog

    val activeIds: Set<String> get() = activeCounts.filterValues { it > 0 }.keys

    fun startsOf(id: String): Int = startLog.count { it.first == id }

    fun emit(
        id: String,
        vararg events: LatencyEvent,
    ) {
        val source = checkNotNull(sources[id]) { "no collection for $id" }
        events.forEach { check(source.tryEmit(it)) }
    }

    override fun monitor(
        target: PingTarget,
        intervalMs: Long,
    ): Flow<LatencyEvent> =
        flow {
            startLog += target.id to intervalMs
            activeCounts.merge(target.id, 1, Int::plus)
            val source = MutableSharedFlow<LatencyEvent>(extraBufferCapacity = EVENT_BUFFER)
            sources[target.id] = source
            try {
                emitAll(source)
            } finally {
                activeCounts.merge(target.id, -1, Int::plus)
            }
        }

    private companion object {
        const val EVENT_BUFFER = 256
    }
}

fun probe(rttMs: Double?) = LatencyEvent.Probe(timestampMs = 0L, rttMs = rttMs)

fun targetUi(
    id: String = "gateway",
    host: String? = null,
    method: PingMethod? = null,
    probes: List<Double?> = emptyList(),
    dnsMs: Double? = null,
    unavailable: LatencyUnavailableReason? = null,
    address: String? = null,
): TargetUi =
    TargetRuntime(
        method = method,
        address = address,
        probes = probes,
        dnsMs = dnsMs,
        unavailable = unavailable,
    ).toUi(if (host == null) PingTarget.Gateway else PingTarget.Host(id, host)).copy(id = id)
