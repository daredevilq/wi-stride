package io.github.daredevilq.wistride.feature.ping

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

private val SparklineHeight = 32.dp
private val StrokeWidth = 2.dp

/** A probe that was answered, at [index] in the window. */
internal data class SparkPoint(
    val index: Int,
    val value: Double,
)

/**
 * Splits [values] into runs of consecutive answers; every lost probe (null) ends a run, so a line is
 * never drawn across a gap.
 */
internal fun splitAtLosses(values: List<Double?>): List<List<SparkPoint>> {
    val runs = mutableListOf<MutableList<SparkPoint>>()
    var current: MutableList<SparkPoint>? = null
    values.forEachIndexed { index, value ->
        if (value == null) {
            current = null
        } else {
            val run = current ?: mutableListOf<SparkPoint>().also { runs += it }
            current = run
            run += SparkPoint(index, value)
        }
    }
    return runs
}

/**
 * Cheap Canvas line of the last probes for a list item (Vico per item would be too heavy).
 * Right-aligned so the newest probe is at the right edge; lost probes leave gaps.
 */
@Composable
internal fun Sparkline(
    values: List<Double?>,
    modifier: Modifier = Modifier,
    windowSize: Int = SPARKLINE_SIZE,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val summary = probeSummaryText(values)
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(SparklineHeight)
                .clearAndSetSemantics { contentDescription = summary },
    ) {
        val answered = values.filterNotNull()
        if (answered.isEmpty()) return@Canvas
        val low = answered.min()
        val span = (answered.max() - low).coerceAtLeast(1.0)
        val stepX = if (windowSize > 1) size.width / (windowSize - 1) else 0f
        val offset = windowSize - values.size
        val stroke = Stroke(width = StrokeWidth.toPx(), cap = StrokeCap.Round)
        splitAtLosses(values).forEach { run ->
            val points =
                run.map {
                    val y = size.height - ((it.value - low) / span).toFloat() * size.height
                    Offset((it.index + offset) * stepX, y)
                }
            if (points.size == 1) {
                drawCircle(lineColor, radius = StrokeWidth.toPx(), center = points.single())
            } else {
                val path =
                    Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                drawPath(path, lineColor, style = stroke)
            }
        }
    }
}

/** Text twin of a chart (it has no semantic data points): count, range and losses of [values]. */
@Composable
internal fun probeSummaryText(values: List<Double?>): String {
    val answered = values.filterNotNull()
    if (answered.isEmpty()) return stringResource(R.string.ping_chart_no_data)
    return stringResource(
        R.string.ping_chart_summary,
        values.size,
        msText(answered.min()),
        msText(answered.max()),
        values.size - answered.size,
    )
}
