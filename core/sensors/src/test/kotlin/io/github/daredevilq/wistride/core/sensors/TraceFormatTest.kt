package io.github.daredevilq.wistride.core.sensors

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class TraceFormatTest {
    private val accel = AccelSample(1_000L, 0.5f, -9.81f, 2f)
    private val rotation = RotationSample(900L, 0f, 0.5f, 0f, 0.866025f)

    @Test
    fun header_matches_spec() {
        assertThat(TraceCsv.HEADER).isEqualTo("t_ns,ax,ay,az,qx,qy,qz,qw,step")
    }

    @Test
    fun row_formats_all_columns() {
        val builder = TraceRowBuilder()
        builder.onRotation(rotation)

        assertThat(builder.row(accel))
            .isEqualTo("1000,0.500000,-9.810000,2.000000,0.000000,0.500000,0.000000,0.866025,0")
    }

    @Test
    fun row_leaves_quaternion_empty_before_first_rotation() {
        val row = TraceRowBuilder().row(accel)

        assertThat(row).isEqualTo("1000,0.500000,-9.810000,2.000000,,,,,0")
        assertThat(row.split(',')).hasSize(TraceCsv.HEADER.split(',').size)
    }

    @Test
    fun steps_are_counted_on_the_first_row_covering_their_timestamp() {
        val builder = TraceRowBuilder()
        builder.onStep(StepEvent(900L))
        builder.onStep(StepEvent(950L))
        builder.onStep(StepEvent(1_030L))

        assertThat(builder.row(accel).substringAfterLast(',')).isEqualTo("2")
        assertThat(builder.row(accel.copy(timestampNs = 1_020L)).substringAfterLast(',')).isEqualTo("0")
        assertThat(builder.row(accel.copy(timestampNs = 1_040L)).substringAfterLast(',')).isEqualTo("1")
        assertThat(builder.row(accel.copy(timestampNs = 1_060L)).substringAfterLast(',')).isEqualTo("0")
        assertThat(builder.stepCount).isEqualTo(3)
        assertThat(builder.stepTimestampsNs).containsExactly(900L, 950L, 1_030L).inOrder()
        assertThat(builder.rowCount).isEqualTo(4)
    }

    @Test
    fun step_arriving_after_its_rows_lands_on_the_next_row() {
        val builder = TraceRowBuilder()
        builder.row(accel)
        builder.onStep(StepEvent(990L))

        assertThat(builder.row(accel.copy(timestampNs = 1_020L)).substringAfterLast(',')).isEqualTo("1")
    }

    @Test
    fun rotation_newer_than_the_row_is_not_used_yet() {
        val builder = TraceRowBuilder()
        builder.onRotation(rotation)
        builder.onRotation(rotation.copy(timestampNs = 1_010L, w = 1f, y = 0f))

        assertThat(builder.row(accel)).endsWith(",0.000000,0.500000,0.000000,0.866025,0")
        assertThat(builder.row(accel.copy(timestampNs = 1_020L)))
            .endsWith(",0.000000,0.000000,0.000000,1.000000,0")
    }

    @Test
    fun latest_rotation_wins() {
        val builder = TraceRowBuilder()
        builder.onRotation(rotation)
        builder.onRotation(rotation.copy(w = 1f, y = 0f))

        assertThat(builder.row(accel)).endsWith(",0.000000,0.000000,0.000000,1.000000,0")
    }

    @Test
    fun formatting_ignores_default_locale() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("pl-PL"))
        try {
            val row = TraceRowBuilder().row(accel)
            assertThat(row).contains("0.500000")
            assertThat(row).doesNotContain("0,500000")
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun rotation_without_w_component_is_completed_to_unit_quaternion() {
        val sample = rotationSampleFromValues(1L, floatArrayOf(0f, 0.6f, 0f))

        assertThat(sample.w).isWithin(1e-6f).of(0.8f)
    }

    @Test
    fun sanitize_replaces_unsafe_characters() {
        assertThat(sanitizeTraceName(" walk 20 steps/ąę ")).isEqualTo("walk_20_steps")
        assertThat(sanitizeTraceName("???")).isEqualTo("trace")
        assertThat(sanitizeTraceName("", fallback = "x")).isEqualTo("x")
    }

    @Test
    fun metadata_json_contains_counts_rate_and_notes() {
        val availability =
            SensorAvailability(
                accelerometer = SensorInfo("Accel \"X\"", "Acme", 0.01f, 100f),
                gameRotationVector = null,
                rotationVector = SensorInfo("RV", "Acme", 0.001f, null),
                stepDetector = null,
                stepDetectorState = StepDetectorState.NotAvailable,
            )
        val metadata =
            TraceMetadata(
                traceName = "t",
                deviceModel = "Pixel",
                sdkInt = 36,
                availability = availability,
                requestedRateHz = 50,
                startWallTimeMillis = 0L,
                durationMs = 2_000L,
                rowCount = 101,
                stepCount = 3,
                stepTimestampsNs = listOf(10L, 20L, 2_500_000_000L),
                firstTimestampNs = 0L,
                lastTimestampNs = 2_000_000_000L,
            )

        val json = metadata.toJson()

        assertThat(metadata.actualAverageRateHz).isWithin(1e-9).of(50.0)
        assertThat(json).contains("\"rowCount\":101")
        assertThat(json).contains("\"stepCount\":3")
        assertThat(json).contains("\"actualAverageRateHz\":50.00")
        assertThat(json).contains("\"headingSource\":\"RotationVector\"")
        assertThat(json).contains("\"startWallTime\":\"1970-01-01T00:00:00Z\"")
        assertThat(json).contains("Accel \\\"X\\\"")
        assertThat(json).contains("\"gameRotationVector\":null")
        assertThat(json).contains("Game rotation vector missing")
        assertThat(json).contains("\"stepTimestampsNs\":[10, 20, 2500000000]")
        assertThat(json).contains("Steps after the last accelerometer row")
    }

    @Test
    fun average_rate_is_null_with_fewer_than_two_rows() {
        val metadata =
            TraceMetadata(
                "t",
                "m",
                29,
                SensorAvailability(null, null, null, null, StepDetectorState.NotAvailable),
                50,
                0L,
                0L,
                1,
                0,
                emptyList(),
                5L,
                5L,
            )

        assertThat(metadata.actualAverageRateHz).isNull()
        assertThat(metadata.toJson()).contains("\"actualAverageRateHz\":null")
    }
}
