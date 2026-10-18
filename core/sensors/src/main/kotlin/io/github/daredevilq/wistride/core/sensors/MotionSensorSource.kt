package io.github.daredevilq.wistride.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Cold flows over the motion sensors. Nothing is registered until a flow is collected and the
 * listener is unregistered when collection ends. Callbacks arrive on a dedicated [HandlerThread]
 * (never Main); the sensor `values` array is copied because Android reuses it between events.
 *
 * A flow completes immediately (emits nothing) when its sensor is missing or, for the step
 * detector, when ACTIVITY_RECOGNITION is not granted; check [SensorAvailabilityProvider] first.
 */
class MotionSensorSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val availability: SensorAvailabilityProvider,
    ) {
        fun accelerometer(): Flow<AccelSample> =
            sensorFlow(Sensor.TYPE_ACCELEROMETER, SAMPLING_PERIOD_US, MAX_REPORT_LATENCY_US) { event ->
                AccelSample(event.timestamp, event.values[0], event.values[1], event.values[2])
            }

        fun gameRotationVector(): Flow<RotationSample> =
            sensorFlow(Sensor.TYPE_GAME_ROTATION_VECTOR, SAMPLING_PERIOD_US, MAX_REPORT_LATENCY_US) { event ->
                rotationSampleFromValues(event.timestamp, event.values)
            }

        fun rotationVector(): Flow<RotationSample> =
            sensorFlow(Sensor.TYPE_ROTATION_VECTOR, SAMPLING_PERIOD_US, MAX_REPORT_LATENCY_US) { event ->
                rotationSampleFromValues(event.timestamp, event.values)
            }

        /** Step detector is event-driven: no sampling period, no batching, to keep step timing exact. */
        fun steps(): Flow<StepEvent> =
            sensorFlow(Sensor.TYPE_STEP_DETECTOR, 0, 0) { event -> StepEvent(event.timestamp) }

        private fun <T> sensorFlow(
            type: Int,
            samplingPeriodUs: Int,
            maxReportLatencyUs: Int,
            map: (SensorEvent) -> T,
        ): Flow<T> =
            callbackFlow {
                val manager = context.getSystemService(SensorManager::class.java)
                val sensor = manager?.getDefaultSensor(type)
                val stepsBlocked =
                    type == Sensor.TYPE_STEP_DETECTOR &&
                        availability.read().stepDetectorState != StepDetectorState.Available
                if (manager == null || sensor == null || stepsBlocked) {
                    close()
                    return@callbackFlow
                }
                val thread = HandlerThread("wistride-sensor-$type").apply { start() }
                val listener =
                    object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            trySend(map(event))
                        }

                        override fun onAccuracyChanged(
                            sensor: Sensor,
                            accuracy: Int,
                        ) = Unit
                    }
                val registered =
                    try {
                        manager.registerListener(
                            listener,
                            sensor,
                            samplingPeriodUs,
                            maxReportLatencyUs,
                            Handler(thread.looper),
                        )
                    } catch (_: SecurityException) {
                        false
                    }
                if (!registered) {
                    thread.quitSafely()
                    close()
                    return@callbackFlow
                }
                awaitClose {
                    manager.unregisterListener(listener)
                    thread.quitSafely()
                }
            }.buffer(BUFFER_CAPACITY)

        companion object {
            /** 50 Hz. */
            const val SAMPLING_PERIOD_US = 20_000
            const val SAMPLING_RATE_HZ = 50

            /** Batching window; the hardware FIFO may deliver up to this late, saving wake-ups. */
            const val MAX_REPORT_LATENCY_US = 200_000
            private const val BUFFER_CAPACITY = 512
        }
    }
