package io.github.daredevilq.wistride.core.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads which motion sensors exist and whether the step detector is usable. Cheap; call on demand.
 * The step detector needs `ACTIVITY_RECOGNITION`, which the app module declares (debug only for now);
 * without it the state is [StepDetectorState.NeedsPermission].
 */
class SensorAvailabilityProvider
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun read(): SensorAvailability {
            val manager = context.getSystemService(SensorManager::class.java)
            val stepDetector = manager?.info(Sensor.TYPE_STEP_DETECTOR)
            return SensorAvailability(
                accelerometer = manager?.info(Sensor.TYPE_ACCELEROMETER),
                gameRotationVector = manager?.info(Sensor.TYPE_GAME_ROTATION_VECTOR),
                rotationVector = manager?.info(Sensor.TYPE_ROTATION_VECTOR),
                stepDetector = stepDetector,
                stepDetectorState = stepDetectorState(stepDetector != null),
            )
        }

        private fun stepDetectorState(present: Boolean): StepDetectorState =
            when {
                !present -> StepDetectorState.NotAvailable

                context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) !=
                    PackageManager.PERMISSION_GRANTED -> StepDetectorState.NeedsPermission

                else -> StepDetectorState.Available
            }

        private fun SensorManager.info(type: Int): SensorInfo? =
            getDefaultSensor(type)?.let { sensor ->
                SensorInfo(
                    name = sensor.name,
                    vendor = sensor.vendor,
                    resolution = sensor.resolution,
                    maxRateHz = sensor.minDelay.takeIf { it > 0 }?.let { MICROS_PER_SECOND / it },
                )
            }
    }

private const val MICROS_PER_SECOND = 1_000_000f
