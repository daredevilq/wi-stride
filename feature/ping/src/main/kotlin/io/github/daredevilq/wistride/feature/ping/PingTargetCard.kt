package io.github.daredevilq.wistride.feature.ping

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.ping.LatencyUnavailableReason
import io.github.daredevilq.wistride.core.ui.permission.PermissionRationaleCard

private val MinTouchTarget = 48.dp

/** One target in the list: name, current RTT, stats row and sparkline, or "unavailable + why". */
@Composable
internal fun TargetCard(
    target: TargetUi,
    permission: PermissionStatus,
    selected: Boolean,
    actions: PingActions,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (selected) colors.secondaryContainer else colors.surfaceContainer
    Card(
        onClick = { actions.onSelect(target.id) },
        modifier = modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(WiStrideTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.s),
        ) {
            TargetHeader(target)
            val reason = target.unavailable
            if (reason != null) {
                UnavailableBlock(reason, permission, actions)
            } else {
                Readout(target)
                Sparkline(target.sparkline)
            }
        }
    }
}

/** Name plus the method and DNS chips; the chips wrap on narrow screens. */
@Composable
internal fun TargetHeader(target: TargetUi) {
    Column(verticalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.xs)) {
        Text(
            text = target.host ?: stringResource(R.string.ping_gateway),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.s)) {
            target.method?.let { Chip(it.chipText()) }
            target.dnsMs?.let { Chip(stringResource(R.string.ping_chip_dns, msText(it))) }
        }
    }
}

/** Read-only label; a disabled M3 chip would render with low-contrast text. */
@Composable
private fun Chip(label: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = WiStrideTheme.spacing.s, vertical = WiStrideTheme.spacing.xs),
        )
    }
}

@Composable
private fun UnavailableBlock(
    reason: LatencyUnavailableReason,
    permission: PermissionStatus,
    actions: PingActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.s)) {
        Text(
            text = stringResource(R.string.ping_unavailable),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = reason.text(), style = MaterialTheme.typography.bodyMedium)
        if (reason is LatencyUnavailableReason.NeedsPermission) {
            PermissionRationaleCard(
                title = stringResource(R.string.ping_permission_title),
                rationale = stringResource(R.string.ping_permission_rationale),
                status = permission,
                onRequest = actions.onRequestPermission,
                onOpenSettings = actions.onOpenAppSettings,
            )
        }
    }
}

@Composable
private fun Readout(target: TargetUi) {
    val stats = target.stats
    val headline =
        when {
            target.currentMs != null -> msText(target.currentMs)
            stats.sent == 0 -> stringResource(R.string.ping_measuring)
            else -> stringResource(R.string.ping_lost)
        }
    Column(verticalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.s)) {
        Text(text = headline, style = MaterialTheme.typography.headlineMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.s)) {
            StatCell(R.string.ping_stat_min, stats.minMs?.let { msText(it) })
            StatCell(R.string.ping_stat_avg, stats.avgMs?.let { msText(it) })
            StatCell(R.string.ping_stat_max, stats.maxMs?.let { msText(it) })
            StatCell(R.string.ping_stat_jitter, stats.jitterMs?.let { msText(it) })
            StatCell(R.string.ping_stat_loss, stats.lossPercent?.let { percentText(it) })
        }
    }
}

@Composable
private fun RowScope.StatCell(
    @StringRes labelRes: Int,
    value: String?,
) {
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value ?: stringResource(R.string.ping_no_value), style = MaterialTheme.typography.bodyMedium)
    }
}
