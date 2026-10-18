package io.github.daredevilq.wistride.feature.now

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme

internal const val CHART_MIN_DBM = -100.0
internal const val CHART_MAX_DBM = 0.0
private const val CHART_MIN_X = 0.0
private val ChartHeight = 160.dp

/** Min/max/current over the history window, shown as text because the chart has no semantic data points. */
internal data class RssiSummary(
    val current: Int?,
    val min: Int,
    val max: Int,
)

/** Null when [history] holds no reading at all. */
internal fun List<Int?>.summarize(): RssiSummary? {
    val readings = filterNotNull()
    if (readings.isEmpty()) return null
    return RssiSummary(current = lastOrNull(), min = readings.min(), max = readings.max())
}

/** Right-aligned x positions (newest sample at the right edge) for the non-null readings. */
internal fun List<Int?>.chartPoints(windowSize: Int = RSSI_HISTORY_SIZE): Pair<List<Int>, List<Int>> {
    val offset = windowSize - size
    val indices = indices.filter { this[it] != null }
    return indices.map { it + offset } to indices.map { checkNotNull(this[it]) }
}

/** Live 60 s RSSI line chart (fixed -100..0 dBm axis) with a text summary underneath. */
@Composable
internal fun RssiChartCard(
    history: List<Int?>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(WiStrideTheme.spacing.l)) {
            Text(text = stringResource(R.string.now_chart_title), style = MaterialTheme.typography.titleMedium)
            val summary = history.summarize()
            if (summary == null) {
                Text(
                    text = stringResource(R.string.now_chart_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = WiStrideTheme.spacing.s),
                )
            } else {
                RssiLineChart(history, Modifier.padding(top = WiStrideTheme.spacing.s))
                Text(
                    text = summary.text(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = WiStrideTheme.spacing.s),
                )
            }
        }
    }
}

@Composable
private fun RssiSummary.text(): String {
    val none = stringResource(R.string.now_chart_no_reading)
    return stringResource(
        R.string.now_chart_summary,
        current?.let { dbmText(it) } ?: none,
        dbmText(min),
        dbmText(max),
    )
}

@Composable
private fun RssiLineChart(
    history: List<Int?>,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(history) {
        val (x, y) = history.chartPoints()
        if (x.isNotEmpty()) {
            modelProducer.runTransaction { lineModel { series(x = x, y = y) } }
        }
    }
    val xFormatter =
        remember {
            // x = 0 is 59 s ago, the right edge (x = 59) is "now".
            CartesianValueFormatter { _, value, _ -> (value - (RSSI_HISTORY_SIZE - 1)).toInt().toString() }
        }
    CartesianChartHost(
        chart =
            rememberCartesianChart(
                rememberLineCartesianLayer(
                    rangeProvider =
                        CartesianLayerRangeProvider.fixed(
                            minX = CHART_MIN_X,
                            maxX = (RSSI_HISTORY_SIZE - 1).toDouble(),
                            minY = CHART_MIN_DBM,
                            maxY = CHART_MAX_DBM,
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
