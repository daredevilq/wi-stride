package io.github.daredevilq.wistride.feature.ping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.daredevilq.wistride.core.designsystem.component.SectionHeader
import io.github.daredevilq.wistride.core.designsystem.component.ValueRow
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.ping.LatencyUnavailableReason
import io.github.daredevilq.wistride.core.ui.permission.PermissionRationaleCard

private val MinTouchTarget = 48.dp

/** Detail of one target: live chart (lost probes as gaps), full statistics, method, address, DNS, delete. */
@Composable
internal fun TargetDetail(
    target: TargetUi,
    intervalMs: Long,
    permission: PermissionStatus,
    showBack: Boolean,
    actions: PingActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(WiStrideTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.m),
    ) {
        if (showBack) {
            TextButton(onClick = actions.onBack, modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget)) {
                Text(stringResource(R.string.ping_detail_back))
            }
        }
        TargetHeader(target)
        val reason = target.unavailable
        if (reason != null) {
            Text(stringResource(R.string.ping_unavailable), style = MaterialTheme.typography.headlineSmall)
            Text(reason.text(), style = MaterialTheme.typography.bodyMedium)
            if (reason is LatencyUnavailableReason.NeedsPermission) {
                PermissionRationaleCard(
                    title = stringResource(R.string.ping_permission_title),
                    rationale = stringResource(R.string.ping_permission_rationale),
                    status = permission,
                    onRequest = actions.onRequestPermission,
                    onOpenSettings = actions.onOpenAppSettings,
                )
            }
        } else {
            ChartCard(target, intervalMs)
            StatsSection(target)
        }
        ConnectionSection(target)
        OutlinedButton(
            onClick = { actions.onRemoveTarget(target.id) },
            modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget),
        ) {
            Text(stringResource(R.string.ping_action_delete))
        }
    }
}

@Composable
private fun ChartCard(
    target: TargetUi,
    intervalMs: Long,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(WiStrideTheme.spacing.l)) {
            Text(
                text = stringResource(R.string.ping_chart_title, CHART_HISTORY_SIZE),
                style = MaterialTheme.typography.titleMedium,
            )
            val summary = probeSummaryText(target.chart)
            if (target.chart.none { it != null }) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = WiStrideTheme.spacing.s),
                )
            } else {
                LatencyChart(target.chart, intervalMs, Modifier.padding(top = WiStrideTheme.spacing.s))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = WiStrideTheme.spacing.s),
                )
            }
        }
    }
}

@Composable
private fun StatsSection(target: TargetUi) {
    val stats = target.stats
    SectionHeader(stringResource(R.string.ping_section_stats))
    ValueRow(stringResource(R.string.ping_stat_current), target.currentMs?.let { msText(it) })
    ValueRow(stringResource(R.string.ping_stat_min_long), stats.minMs?.let { msText(it) })
    ValueRow(stringResource(R.string.ping_stat_avg_long), stats.avgMs?.let { msText(it) })
    ValueRow(stringResource(R.string.ping_stat_max_long), stats.maxMs?.let { msText(it) })
    ValueRow(stringResource(R.string.ping_stat_jitter_long), stats.jitterMs?.let { msText(it) })
    ValueRow(stringResource(R.string.ping_stat_loss_long), stats.lossPercent?.let { percentText(it) })
    ValueRow(
        stringResource(R.string.ping_stat_probes),
        stringResource(R.string.ping_probes_value, stats.received, stats.sent),
    )
}

@Composable
private fun ConnectionSection(target: TargetUi) {
    SectionHeader(stringResource(R.string.ping_section_connection))
    ValueRow(stringResource(R.string.ping_method), target.method?.description())
    ValueRow(stringResource(R.string.ping_address), target.address)
    // Only host names are resolved; for an IP literal or the gateway DNS is not applicable.
    if (target.host != null) {
        ValueRow(stringResource(R.string.ping_dns_time), target.dnsMs?.let { msText(it) })
    }
}
