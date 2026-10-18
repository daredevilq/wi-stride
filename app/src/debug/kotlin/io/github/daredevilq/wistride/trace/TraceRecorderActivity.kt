package io.github.daredevilq.wistride.trace

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.daredevilq.wistride.R
import io.github.daredevilq.wistride.core.designsystem.component.ValueRow
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import io.github.daredevilq.wistride.core.sensors.SensorInfo
import io.github.daredevilq.wistride.core.sensors.StepDetectorState

/** Debug-only screen to record sensor traces. */
@AndroidEntryPoint
class TraceRecorderActivity : ComponentActivity() {
    private val viewModel: TraceRecorderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WiStrideTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val permissionLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                        viewModel.refreshAvailability()
                    }
                KeepScreenOn(state.recording)
                TraceRecorderScreen(
                    state = state,
                    onNameChange = viewModel::onNameChange,
                    onStart = viewModel::start,
                    onStop = viewModel::stop,
                    onGrantPermission = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAvailability()
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun TraceRecorderScreen(
    state: TraceUiState,
    onNameChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onGrantPermission: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.trace_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.trace_name_label)) },
                singleLine = true,
                enabled = !state.recording,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, enabled = !state.recording && state.availability.accelerometer != null) {
                    Text(stringResource(R.string.trace_start))
                }
                OutlinedButton(onClick = onStop, enabled = state.recording) {
                    Text(stringResource(R.string.trace_stop))
                }
            }
            if (state.availability.accelerometer == null) {
                Text(stringResource(R.string.trace_no_accelerometer), color = MaterialTheme.colorScheme.error)
            }
            Counters(state)
            state.savedPath?.let {
                Text(
                    stringResource(R.string.trace_saved, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.saveFailed) {
                Text(stringResource(R.string.trace_save_failed), color = MaterialTheme.colorScheme.error)
            }
            SensorList(state, onGrantPermission)
        }
    }
}

@Composable
private fun Counters(state: TraceUiState) {
    Column {
        ValueRow(stringResource(R.string.trace_samples), state.samples.toString())
        ValueRow(stringResource(R.string.trace_steps), state.steps.toString())
        ValueRow(
            stringResource(R.string.trace_duration),
            stringResource(
                R.string.trace_duration_value,
                state.durationMs / MS_PER_SECOND,
                state.durationMs % MS_PER_SECOND / MS_PER_TENTH,
            ),
        )
    }
}

@Composable
private fun SensorList(
    state: TraceUiState,
    onGrantPermission: () -> Unit,
) {
    val availability = state.availability
    Column {
        Text(stringResource(R.string.trace_sensors_header), style = MaterialTheme.typography.titleMedium)
        ValueRow(stringResource(R.string.trace_sensor_accelerometer), describe(availability.accelerometer))
        ValueRow(stringResource(R.string.trace_sensor_game_rv), describe(availability.gameRotationVector))
        ValueRow(stringResource(R.string.trace_sensor_rv), describe(availability.rotationVector))
        ValueRow(
            stringResource(R.string.trace_sensor_step),
            when (availability.stepDetectorState) {
                StepDetectorState.NeedsPermission -> stringResource(R.string.trace_step_needs_permission)
                else -> describe(availability.stepDetector)
            },
        )
        if (availability.stepDetectorState == StepDetectorState.NeedsPermission) {
            OutlinedButton(onClick = onGrantPermission) { Text(stringResource(R.string.trace_grant_permission)) }
        }
    }
}

@Composable
private fun describe(info: SensorInfo?): String =
    when {
        info == null -> stringResource(R.string.trace_sensor_not_available)
        info.maxRateHz == null -> stringResource(R.string.trace_sensor_details, info.name, info.vendor)
        else -> stringResource(R.string.trace_sensor_details_rate, info.name, info.vendor, info.maxRateHz!!.toInt())
    }

private const val MS_PER_SECOND = 1000
private const val MS_PER_TENTH = 100
