package io.github.daredevilq.wistride.feature.ping

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import kotlin.math.roundToInt

private val ChartHeight = 200.dp
private const val MS_PER_SECOND = 1_000.0

/**
 * Live latency line of the last [CHART_HISTORY_SIZE] probes. Each run of answered probes is its own
 * Vico series, so a lost probe leaves a gap instead of a line across it. Newest probe at the right edge.
 */
@Composable
internal fun LatencyChart(
    probes: List<Double?>,
    intervalMs: Long,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(probes) {
        val offset = CHART_HISTORY_SIZE - probes.size
        val runs = splitAtLosses(probes)
        if (runs.isNotEmpty()) {
            modelProducer.runTransaction {
                lineModel {
                    runs.forEach { run -> series(x = run.map { it.index + offset }, y = run.map { it.value }) }
                }
            }
        }
    }
    val xFormatter =
        remember(intervalMs) {
            // x = 119 is "now"; the label is the age of that probe in seconds.
            CartesianValueFormatter { _, value, _ ->
                ((value - (CHART_HISTORY_SIZE - 1)) * intervalMs / MS_PER_SECOND).roundToInt().toString()
            }
        }
    // One shared line style: the default provider would colour every series (every run) differently.
    val line =
        LineCartesianLayer.rememberLine(
            LineCartesianLayer.LineFill.single(Fill(MaterialTheme.colorScheme.primary)),
        )
    CartesianChartHost(
        chart =
            rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(line),
                    rangeProvider =
                        CartesianLayerRangeProvider.fixed(
                            minX = 0.0,
                            maxX = (CHART_HISTORY_SIZE - 1).toDouble(),
                            minY = 0.0,
                        ),
                ),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter),
            ),
        modelProducer = modelProducer,
        modifier = modifier.fillMaxWidth().height(ChartHeight),
        // The whole fixed window must be visible: Vico's default zoom spaces points for scrolling and
        // would open the chart scrolled to the oldest samples.
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content, minZoom = Zoom.Content),
        // The window slides every sample, so each update has a "new" x at the left edge; the diff
        // animation would grow it from y = 0 and draw a vertical spike there.
        animationSpec = null,
    )
}
