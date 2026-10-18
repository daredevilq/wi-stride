package io.github.daredevilq.wistride.feature.ping

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.daredevilq.wistride.core.common.Dispatcher
import io.github.daredevilq.wistride.core.common.WiStrideDispatcher
import io.github.daredevilq.wistride.core.datastore.PingSettingsRepository
import io.github.daredevilq.wistride.core.model.permission.PermissionRequestHistory
import io.github.daredevilq.wistride.core.model.permission.PermissionSnapshot
import io.github.daredevilq.wistride.core.model.ping.PingSettings
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import io.github.daredevilq.wistride.core.network.LatencyMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
private const val KEY_SELECTED_ID = "selectedId"

/**
 * State holder of the "Ping" screen. Measuring runs only while the UI collects [uiState]:
 * the monitor flows are cold and live inside that subscription, so they are cancelled 5 s after the
 * screen stops being visible.
 */
@HiltViewModel
class PingViewModel
    @Inject
    constructor(
        private val settingsRepository: PingSettingsRepository,
        private val monitor: LatencyMonitor,
        /** Exposed so the screen's permission state can tell "never asked" from "don't ask again". */
        val permissionRequestHistory: PermissionRequestHistory,
        private val savedState: SavedStateHandle,
        @Dispatcher(WiStrideDispatcher.Default) private val defaultDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        // Null until the route reports the first resolved snapshot; nothing is measured before that.
        private val permission = MutableStateFlow<PermissionSnapshot?>(null)
        private val runtimes = MutableStateFlow<Map<String, TargetRuntime>>(emptyMap())
        private val selectedId = savedState.getStateFlow<String?>(KEY_SELECTED_ID, null)

        @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest has no stable alternative for re-subscribing.
        val uiState: StateFlow<PingUiState> =
            permission
                .filterNotNull()
                .distinctUntilChanged()
                // The monitor checks permissions only when a target's collection starts, so a changed
                // grant set needs fresh collections even when the status stays the same.
                .flatMapLatest { measure(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), PingUiState.Loading)

        /** Called by the route with every resolved local-network permission snapshot. */
        fun onPermissionChanged(snapshot: PermissionSnapshot) {
            permission.value = snapshot
        }

        fun selectTarget(id: String?) {
            savedState[KEY_SELECTED_ID] = id
        }

        /** [host] and [port] must come from a valid `validateTarget` result. */
        fun addTarget(
            host: String,
            port: Int?,
        ) {
            viewModelScope.launch { settingsRepository.addTarget(host, port) }
        }

        fun removeTarget(id: String) {
            // Otherwise restoring a default target with the same id would reopen a stale detail view.
            if (savedState.get<String?>(KEY_SELECTED_ID) == id) selectTarget(null)
            viewModelScope.launch { settingsRepository.removeTarget(id) }
        }

        fun setInterval(intervalMs: Long) {
            viewModelScope.launch { settingsRepository.setInterval(intervalMs) }
        }

        fun restoreDefaultTargets() {
            viewModelScope.launch { settingsRepository.restoreDefaultTargets() }
        }

        private fun measure(snapshot: PermissionSnapshot): Flow<PingUiState> =
            flow {
                // A history kept across a restart or an unsubscribed gap would draw a line over the missing time.
                runtimes.value = emptyMap()
                coroutineScope {
                    val settings = MutableStateFlow<PingSettings?>(null)
                    superviseTargets(settings)
                    combine(settings.filterNotNull(), runtimes, selectedId) { current, results, selected ->
                        toPingUiState(current, results, selected, snapshot.status)
                    }.collect { emit(it) }
                }
            }

        /**
         * Keeps exactly one collection job per target id: a new target starts only its own job, a removed
         * one cancels only its own, and a changed interval restarts all because the interval is fixed per
         * collection.
         */
        private fun CoroutineScope.superviseTargets(latest: MutableStateFlow<PingSettings?>) =
            launch {
                val jobs = mutableMapOf<String, Job>()
                var intervalMs: Long? = null
                settingsRepository.settings.collect { settings ->
                    if (settings.intervalMs != intervalMs) {
                        jobs.values.forEach(Job::cancel)
                        jobs.clear()
                        runtimes.value = emptyMap()
                        intervalMs = settings.intervalMs
                    }
                    val ids = settings.targets.map(PingTarget::id).toSet()
                    (jobs.keys - ids).forEach { id ->
                        jobs.remove(id)?.cancel()
                        runtimes.update { it - id }
                    }
                    settings.targets.filter { it.id !in jobs }.forEach { target ->
                        runtimes.update { it - target.id }
                        jobs[target.id] = launch(defaultDispatcher) { collectTarget(target, settings.intervalMs) }
                    }
                    latest.value = settings
                }
            }

        private suspend fun collectTarget(
            target: PingTarget,
            intervalMs: Long,
        ) {
            monitor.monitor(target, intervalMs).collect { event ->
                runtimes.update { all -> all + (target.id to (all[target.id] ?: TargetRuntime()).apply(event)) }
            }
        }
    }
