package io.github.daredevilq.wistride.core.sensors

import java.time.Instant
import java.util.Locale

/** CSV layout of recorded sensor traces (used as PDR test fixtures). */
object TraceCsv {
    const val HEADER = "t_ns,ax,ay,az,qx,qy,qz,qw,step"
}

/**
 * Turns the interleaved sensor streams into trace CSV rows: one row per accelerometer sample,
 * carrying a quaternion and a `step` column.
 *
 * Sensors deliver in batches (up to [MotionSensorSource.MAX_REPORT_LATENCY_US] late) and the step
 * detector is unbatched, so streams are matched by sensor timestamp, not arrival order. The
 * quaternion is the newest received rotation with `timestampNs <= t_ns` (empty columns until there
 * is one). `step` = number of not-yet-written step events with `timestampNs <= t_ns`. A
 * step whose rows were already written lands on the next row; steps after the last row are only in
 * [stepTimestampsNs]. Not thread-safe; feed it from one coroutine.
 */
class TraceRowBuilder {
    private var currentRotation: RotationSample? = null
    private val pendingRotations = ArrayDeque<RotationSample>()
    private val pendingSteps = ArrayDeque<Long>()
    private val steps = mutableListOf<Long>()

    var rowCount = 0
        private set
    val stepCount: Int
        get() = steps.size

    /** Raw step-detector timestamps in arrival order (lossless ground truth for the meta file). */
    val stepTimestampsNs: List<Long>
        get() = steps.toList()

    fun onRotation(sample: RotationSample) {
        pendingRotations.addLast(sample)
    }

    fun onStep(event: StepEvent) {
        pendingSteps.addLast(event.timestampNs)
        steps += event.timestampNs
    }

    /** Returns the CSV line (without line terminator) for [sample], consuming the steps it covers. */
    fun row(sample: AccelSample): String {
        while (pendingRotations.isNotEmpty() && pendingRotations.first().timestampNs <= sample.timestampNs) {
            currentRotation = pendingRotations.removeFirst()
        }
        val q = currentRotation
        var step = 0
        // Remove every covered step, not just a sorted prefix, in case events arrive out of order.
        val iterator = pendingSteps.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() <= sample.timestampNs) {
                iterator.remove()
                step++
            }
        }
        rowCount++
        return buildString {
            append(sample.timestampNs).append(',')
            append(formatFloat(sample.x)).append(',')
            append(formatFloat(sample.y)).append(',')
            append(formatFloat(sample.z)).append(',')
            if (q == null) {
                append(",,,")
            } else {
                append(formatFloat(q.x)).append(',')
                append(formatFloat(q.y)).append(',')
                append(formatFloat(q.z)).append(',')
                append(formatFloat(q.w))
            }
            append(',').append(step)
        }
    }

    private fun formatFloat(value: Float): String = String.format(Locale.ROOT, "%.6f", value)
}

/**
 * Makes a user-typed trace name safe as a file name: anything outside `[A-Za-z0-9_-]` becomes `_`,
 * surrounding underscores are trimmed, and a blank result falls back to [fallback].
 */
fun sanitizeTraceName(
    raw: String,
    fallback: String = "trace",
): String {
    val cleaned = raw.trim().replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_')
    return cleaned.ifEmpty { fallback }
}

/** Describes one recording; serialised next to the CSV so fixtures carry their provenance. */
data class TraceMetadata(
    val traceName: String,
    val deviceModel: String,
    val sdkInt: Int,
    val availability: SensorAvailability,
    val requestedRateHz: Int,
    val startWallTimeMillis: Long,
    val durationMs: Long,
    val rowCount: Int,
    val stepCount: Int,
    val stepTimestampsNs: List<Long>,
    val firstTimestampNs: Long,
    val lastTimestampNs: Long,
) {
    /** Average accelerometer rate from sensor timestamps: (rows - 1) / elapsed seconds; null if undefined. */
    val actualAverageRateHz: Double?
        get() {
            val elapsedNs = lastTimestampNs - firstTimestampNs
            return if (rowCount < 2 || elapsedNs <= 0) null else (rowCount - 1) * NANOS_PER_SECOND / elapsedNs
        }

    fun toJson(): String {
        val heading = availability.headingSource
        val notes =
            buildList {
                if (heading == HeadingSource.RotationVector) {
                    add("Game rotation vector missing; qx..qw come from the (magnetometer-based) rotation vector.")
                }
                if (heading == HeadingSource.None) add("No rotation sensor; quaternion columns are empty.")
                if (availability.stepDetectorState != StepDetectorState.Available) {
                    add("Step detector unusable (${availability.stepDetectorState}); step column is all 0.")
                }
                if (stepTimestampsNs.any { it > lastTimestampNs }) {
                    add("Steps after the last accelerometer row are only listed in stepTimestampsNs.")
                }
            }
        val fields =
            listOf(
                "traceName" to str(traceName),
                "deviceModel" to str(deviceModel),
                "sdkInt" to sdkInt.toString(),
                "startWallTime" to str(Instant.ofEpochMilli(startWallTimeMillis).toString()),
                "durationMs" to durationMs.toString(),
                "rowCount" to rowCount.toString(),
                "stepCount" to stepCount.toString(),
                "stepTimestampsNs" to stepTimestampsNs.joinToString(prefix = "[", postfix = "]"),
                "requestedRateHz" to requestedRateHz.toString(),
                "actualAverageRateHz" to (
                    actualAverageRateHz?.let {
                        String.format(
                            Locale.ROOT,
                            "%.2f",
                            it,
                        )
                    } ?: "null"
                ),
                "headingSource" to str(heading.name),
                "stepDetectorState" to str(availability.stepDetectorState.name),
                "sensors" to
                    obj(
                        "accelerometer" to sensor(availability.accelerometer),
                        "gameRotationVector" to sensor(availability.gameRotationVector),
                        "rotationVector" to sensor(availability.rotationVector),
                        "stepDetector" to sensor(availability.stepDetector),
                    ),
                "notes" to notes.joinToString(prefix = "[", postfix = "]") { str(it) },
            )
        return obj(*fields.toTypedArray()) + "\n"
    }

    private fun sensor(info: SensorInfo?): String =
        if (info == null) {
            "null"
        } else {
            obj(
                "name" to str(info.name),
                "vendor" to str(info.vendor),
                "resolution" to String.format(Locale.ROOT, "%.6f", info.resolution),
                "maxRateHz" to (info.maxRateHz?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "null"),
            )
        }

    private fun obj(vararg fields: Pair<String, String>): String =
        fields.joinToString(prefix = "{", postfix = "}") { (key, value) -> "${str(key)}:$value" }

    private fun str(value: String): String {
        val escaped =
            buildString {
                for (c in value) {
                    when {
                        c == '"' -> append("\\\"")
                        c == '\\' -> append("\\\\")
                        c < ' ' -> append(String.format(Locale.ROOT, "\\u%04x", c.code))
                        else -> append(c)
                    }
                }
            }
        return "\"$escaped\""
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
