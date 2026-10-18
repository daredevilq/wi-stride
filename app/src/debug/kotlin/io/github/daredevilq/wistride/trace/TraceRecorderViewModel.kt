package io.github.daredevilq.wistride.trace

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.daredevilq.wistride.core.sensors.SensorAvailability
import io.github.daredevilq.wistride.core.sensors.SensorAvailabilityProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class TraceUiState(
    val name: String,
    val availability: SensorAvailability,
    val recording: Boolean = false,
    val samples: Int = 0,
    val steps: Int = 0,
    val durationMs: Long = 0,
    val savedPath: String? = null,
    val saveFailed: Boolean = false,
)

@HiltViewModel
class TraceRecorderViewModel
    @Inject
    constructor(
        private val recorder: TraceRecorder,
        private val availabilityProvider: SensorAvailabilityProvider,
    ) : ViewModel() {
        private val _state = MutableStateFlow(TraceUiState(name = "", availability = availabilityProvider.read()))
        val state: StateFlow<TraceUiState> = _state.asStateFlow()

        private var recordingJob: Job? = null

        fun onNameChange(name: String) = _state.update { it.copy(name = name) }

        fun refreshAvailability() = _state.update { it.copy(availability = availabilityProvider.read()) }

        fun start() {
            if (recordingJob?.isActive == true || _state.value.availability.accelerometer == null) return
            val startedAt = SystemClock.elapsedRealtime()
            _state.update {
                it.copy(recording = true, samples = 0, steps = 0, durationMs = 0, savedPath = null, saveFailed = false)
            }
            val ticker =
                viewModelScope.launch {
                    while (true) {
                        delay(TICK_MS)
                        _state.update { it.copy(durationMs = SystemClock.elapsedRealtime() - startedAt) }
                    }
                }
            recordingJob =
                viewModelScope.launch {
                    try {
                        recorder.record(
                            traceName = _state.value.name,
                            availability = _state.value.availability,
                            onProgress = { p -> _state.update { it.copy(samples = p.samples, steps = p.steps) } },
                            onSaved = { file -> _state.update { it.copy(savedPath = file.absolutePath) } },
                        )
                    } catch (_: IOException) {
                        _state.update { it.copy(saveFailed = true) }
                    } finally {
                        ticker.cancel()
                        _state.update { it.copy(recording = false) }
                    }
                }
        }

        /** Cancels the recording job; the recorder unregisters sensors and flushes files before finishing. */
        fun stop() {
            recordingJob?.cancel()
        }

        override fun onCleared() {
            recordingJob?.cancel()
        }

        private companion object {
            const val TICK_MS = 200L
        }
    }
