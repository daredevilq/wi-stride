package io.github.daredevilq.wistride.trace

import android.content.Context
import android.os.Build
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.daredevilq.wistride.core.common.Dispatcher
import io.github.daredevilq.wistride.core.common.WiStrideDispatcher
import io.github.daredevilq.wistride.core.sensors.AccelSample
import io.github.daredevilq.wistride.core.sensors.HeadingSource
import io.github.daredevilq.wistride.core.sensors.MotionSensorSource
import io.github.daredevilq.wistride.core.sensors.RotationSample
import io.github.daredevilq.wistride.core.sensors.SensorAvailability
import io.github.daredevilq.wistride.core.sensors.StepDetectorState
import io.github.daredevilq.wistride.core.sensors.StepEvent
import io.github.daredevilq.wistride.core.sensors.TraceCsv
import io.github.daredevilq.wistride.core.sensors.TraceMetadata
import io.github.daredevilq.wistride.core.sensors.TraceRowBuilder
import io.github.daredevilq.wistride.core.sensors.sanitizeTraceName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** Live counters reported while recording. */
data class TraceProgress(
    val samples: Int,
    val steps: Int,
)

/**
 * Records accelerometer + heading + step streams into `<externalFiles>/traces/<name>.csv` plus a
 * `.meta.json`. Cancelling [record] stops collection (which unregisters every sensor) and then
 * flushes and closes the files, so Stop and Activity destruction share one code path.
 */
class TraceRecorder
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val sensors: MotionSensorSource,
        @param:Dispatcher(WiStrideDispatcher.IO) private val io: CoroutineDispatcher,
    ) {
        private sealed interface Event {
            data class Accel(
                val sample: AccelSample,
            ) : Event

            data class Rotation(
                val sample: RotationSample,
            ) : Event

            data class Step(
                val event: StepEvent,
            ) : Event
        }

        /**
         * Suspends until cancelled. [onProgress] is called every [PROGRESS_EVERY_ROWS] rows;
         * [onSaved] receives the CSV file once it is closed (also after cancellation).
         *
         * @throws IOException if the target directory is unavailable or writing fails.
         */
        suspend fun record(
            traceName: String,
            availability: SensorAvailability,
            onProgress: (TraceProgress) -> Unit,
            onSaved: (File) -> Unit,
        ) = withContext(io) {
            val dir = context.getExternalFilesDir(TRACES_DIR) ?: throw IOException("External files dir unavailable")
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create $dir")
            val base = uniqueBaseName(dir, sanitizeTraceName(traceName))
            val csv = File(dir, "$base.csv")
            val builder = TraceRowBuilder()
            val startWall = System.currentTimeMillis()
            val startElapsed = SystemClock.elapsedRealtime()
            var firstNs = 0L
            var lastNs = 0L
            val writer = csv.bufferedWriter(Charsets.UTF_8)
            try {
                writer.write(TraceCsv.HEADER)
                writer.write("\n")
                events(availability).collect { event ->
                    when (event) {
                        is Event.Rotation -> {
                            builder.onRotation(event.sample)
                        }

                        is Event.Step -> {
                            builder.onStep()
                        }

                        is Event.Accel -> {
                            if (builder.rowCount == 0) firstNs = event.sample.timestampNs
                            lastNs = event.sample.timestampNs
                            writer.write(builder.row(event.sample))
                            writer.write("\n")
                            if (builder.rowCount % PROGRESS_EVERY_ROWS == 0) {
                                onProgress(TraceProgress(builder.rowCount, builder.stepCount))
                            }
                        }
                    }
                }
            } finally {
                writer.close()
                val metadata =
                    TraceMetadata(
                        traceName = base,
                        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                        sdkInt = Build.VERSION.SDK_INT,
                        availability = availability,
                        requestedRateHz = MotionSensorSource.SAMPLING_RATE_HZ,
                        startWallTimeMillis = startWall,
                        durationMs = SystemClock.elapsedRealtime() - startElapsed,
                        rowCount = builder.rowCount,
                        stepCount = builder.stepCount,
                        firstTimestampNs = firstNs,
                        lastTimestampNs = lastNs,
                    )
                File(dir, "$base.meta.json").writeText(metadata.toJson(), Charsets.UTF_8)
                onProgress(TraceProgress(builder.rowCount, builder.stepCount))
                onSaved(csv)
            }
        }

        /** One merged stream so the row builder is only ever touched by the collecting coroutine. */
        private fun events(availability: SensorAvailability): Flow<Event> {
            val rotation: Flow<RotationSample> =
                when (availability.headingSource) {
                    HeadingSource.GameRotationVector -> sensors.gameRotationVector()
                    HeadingSource.RotationVector -> sensors.rotationVector()
                    HeadingSource.None -> emptyFlow()
                }
            val steps: Flow<StepEvent> =
                if (availability.stepDetectorState == StepDetectorState.Available) sensors.steps() else emptyFlow()
            return merge(
                sensors.accelerometer().map { Event.Accel(it) },
                rotation.map { Event.Rotation(it) },
                steps.map { Event.Step(it) },
            )
        }

        private fun uniqueBaseName(
            dir: File,
            name: String,
        ): String {
            var candidate = name
            var suffix = 2
            while (File(dir, "$candidate.csv").exists()) candidate = "$name-${suffix++}"
            return candidate
        }

        private companion object {
            const val TRACES_DIR = "traces"
            const val PROGRESS_EVERY_ROWS = 10
        }
    }
