package io.github.daredevilq.wistride.feature.now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.wifi.WifiSnapshot
import io.github.daredevilq.wistride.core.ui.permission.PermissionRequestHistory
import io.github.daredevilq.wistride.core.wifi.WifiConnectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val RSSI_HISTORY_SIZE = 60
internal const val RSSI_SAMPLE_PERIOD_MS = 1_000L
private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * State holder of the "Now" screen. Nothing runs while the UI is not collecting [uiState]: the
 * repository flow is cold and the 1 Hz RSSI sampler lives inside the same subscription.
 */
@HiltViewModel
class NowViewModel
    @Inject
    constructor(
        private val repository: WifiConnectionRepository,
        /** Exposed so the screen's permission state can tell "never asked" from "don't ask again". */
        val permissionRequestHistory: PermissionRequestHistory,
    ) : ViewModel() {
        private val permissionStatus = MutableStateFlow(PermissionStatus.NotRequested)
        private val rssiHistory = MutableStateFlow<List<Int?>>(emptyList())

        @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest has no stable alternative for re-subscribing.
        val uiState: StateFlow<NowUiState> =
            permissionStatus
                // SSID/BSSID redaction is decided when the platform callback is registered, so any status
                // change (a grant, a revocation, or a partial grant such as location without Nearby devices)
                // needs a fresh subscription to the repository.
                .flatMapLatest { observeConnection() }
                // A history kept across an unsubscribed gap would show a misleading continuous line.
                .onStart { rssiHistory.value = emptyList() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), NowUiState.Loading)

        /** Called by the route whenever the runtime-permission status changes. */
        fun onPermissionStatusChanged(status: PermissionStatus) {
            permissionStatus.value = status
        }

        private fun observeConnection(): Flow<NowUiState> =
            flow {
                coroutineScope {
                    val latest = MutableStateFlow<WifiSnapshot?>(null)
                    launch { repository.snapshots.collect { latest.value = it } }
                    launch { sampleRssiEverySecond(latest) }
                    combine(latest.filterNotNull(), rssiHistory, permissionStatus) { snapshot, history, permission ->
                        snapshot.toUiState(permission, history)
                    }.collect { emit(it) }
                }
            }

        private suspend fun sampleRssiEverySecond(latest: StateFlow<WifiSnapshot?>) {
            while (true) {
                delay(RSSI_SAMPLE_PERIOD_MS)
                val sample = latest.value.rssiSample()
                rssiHistory.update { (it + sample).takeLast(RSSI_HISTORY_SIZE) }
            }
        }
    }
