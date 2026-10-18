package io.github.daredevilq.wistride.core.sensors

import kotlin.math.sqrt

/** Accelerometer reading in m/s^2 (includes gravity), device axes. [timestampNs] is `SensorEvent.timestamp`. */
data class AccelSample(
    val timestampNs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
)

/** Unit quaternion (x, y, z, w) from a rotation-vector sensor. */
data class RotationSample(
    val timestampNs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
)

/** One step reported by the hardware step detector. */
data class StepEvent(
    val timestampNs: Long,
)

/**
 * Builds a [RotationSample] from `SensorEvent.values` of a (game) rotation vector sensor.
 *
 * Android guarantees `values[3] = cos(theta/2)` since API 18, but some drivers only fill three
 * components; then w = sqrt(1 - x^2 - y^2 - z^2) (the scalar part of a unit quaternion, >= 0).
 */
fun rotationSampleFromValues(
    timestampNs: Long,
    values: FloatArray,
): RotationSample {
    val x = values[0]
    val y = values[1]
    val z = values[2]
    val w =
        if (values.size >= 4 && !values[3].isNaN()) {
            values[3]
        } else {
            sqrt(maxOf(0f, 1f - x * x - y * y - z * z))
        }
    return RotationSample(timestampNs, x, y, z, w)
}
