package io.github.daredevilq.wistride.core.sensors

/** Static description of a hardware sensor; [maxRateHz] is null when the driver reports no minimum delay. */
data class SensorInfo(
    val name: String,
    val vendor: String,
    val resolution: Float,
    val maxRateHz: Float?,
)

/** Whether the step detector can deliver events right now. */
enum class StepDetectorState {
    Available,
    NeedsPermission,
    NotAvailable,
}

/** Which rotation-vector sensor provides heading. Game RV is preferred (no magnetometer). */
enum class HeadingSource {
    GameRotationVector,
    RotationVector,
    None,
}

/** Sensors present on this device; a null entry means "not available on this device". */
data class SensorAvailability(
    val accelerometer: SensorInfo?,
    val gameRotationVector: SensorInfo?,
    val rotationVector: SensorInfo?,
    val stepDetector: SensorInfo?,
    val stepDetectorState: StepDetectorState,
) {
    val headingSource: HeadingSource
        get() =
            when {
                gameRotationVector != null -> HeadingSource.GameRotationVector
                rotationVector != null -> HeadingSource.RotationVector
                else -> HeadingSource.None
            }
}
