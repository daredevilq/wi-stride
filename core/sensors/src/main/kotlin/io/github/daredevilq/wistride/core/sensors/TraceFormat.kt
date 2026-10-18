package io.github.daredevilq.wistride.core.sensors

import java.time.Instant
import java.util.Locale

/** CSV layout of recorded sensor traces (used as PDR test fixtures). */
object TraceCsv {
    const val HEADER = "t_ns,ax,ay,az,qx,qy,qz,qw,step"
}

/**
 * Turns the interleaved sensor streams into trace CSV rows: one row per accelerometer sample,
 * carrying the latest quaternion (empty columns until one arrived) and a step flag that is 1 only
 * for the first row after a step-detector event. Not thread-safe; feed it from one coroutine.
 */
class TraceRowBuilder {
    private var latestRotation: RotationSample? = null
    private var stepPending = false

    var rowCount = 0
        private set
    var stepCount = 0
        private set

    fun onRotation(sample: RotationSample) {
        latestRotation = sample
    }

    fun onStep() {
        stepPending = true
        stepCount++
    }

    /** Returns the CSV line (without line terminator) for [sample] and resets the step flag. */
    fun row(sample: AccelSample): String {
        val q = latestRotation
        val step = if (stepPending) 1 else 0
        stepPending = false
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
