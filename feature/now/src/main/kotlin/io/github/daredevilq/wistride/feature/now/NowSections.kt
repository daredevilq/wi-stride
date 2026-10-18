package io.github.daredevilq.wistride.feature.now

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.daredevilq.wistride.core.designsystem.component.SectionHeader
import io.github.daredevilq.wistride.core.designsystem.component.ValueRow
import io.github.daredevilq.wistride.core.designsystem.theme.StatusColors
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import io.github.daredevilq.wistride.core.model.Availability
import io.github.daredevilq.wistride.core.model.UnavailableReason
import io.github.daredevilq.wistride.core.model.wifi.SignalLevel
import io.github.daredevilq.wistride.core.model.wifi.SignalQuality

private val PillShape = RoundedCornerShape(50)
private val MinRowHeight = 48.dp

/** SSID, BSSID, RSSI and the quality pill. */
@Composable
internal fun HeaderCard(
    details: ConnectionDetails,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = WiStrideTheme.spacing.l, vertical = WiStrideTheme.spacing.s)) {
            AvailabilityRow(stringResource(R.string.now_ssid), details.ssid, onCopy) { it }
            AvailabilityRow(stringResource(R.string.now_bssid), details.bssid, onCopy) { it }
            AvailabilityRow(stringResource(R.string.now_rssi), details.rssiDbm, onCopy) { dbmText(it) }
            QualityRow(details.quality)
        }
    }
}

@Composable
private fun QualityRow(quality: SignalQuality?) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = MinRowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.l),
    ) {
        Text(
            text = stringResource(R.string.now_signal_quality),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        val colors = WiStrideTheme.statusColors
        val (container, content) = quality?.level.colors(colors)
        Surface(color = container, contentColor = content, shape = PillShape) {
            Text(
                text =
                    quality?.let { stringResource(R.string.now_quality_value, it.percent, it.level.text()) }
                        ?: stringResource(R.string.now_quality_unavailable),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = WiStrideTheme.spacing.m, vertical = WiStrideTheme.spacing.xs),
            )
        }
    }
}

private fun SignalLevel?.colors(colors: StatusColors): Pair<Color, Color> =
    when (this) {
        SignalLevel.Excellent -> colors.excellent to colors.onExcellent
        SignalLevel.Good -> colors.good to colors.onGood
        SignalLevel.Fair -> colors.fair to colors.onFair
        SignalLevel.Weak, SignalLevel.Poor -> colors.weak to colors.onWeak
        null -> colors.none to colors.onNone
    }

/** Frequency, band, channel, width, generation, link speeds and security. */
@Composable
internal fun RadioSection(
    details: ConnectionDetails,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.now_section_radio))
        AvailabilityRow(stringResource(R.string.now_frequency), details.frequencyMhz, onCopy) { mhzText(it) }
        AvailabilityRow(stringResource(R.string.now_band), details.band, onCopy) { it.text() }
        AvailabilityRow(stringResource(R.string.now_channel), details.channel, onCopy) { it.toString() }
        AvailabilityRow(stringResource(R.string.now_channel_width), details.channelWidth, onCopy) { it.text() }
        AvailabilityRow(stringResource(R.string.now_generation), details.generation, onCopy) { it.text() }
        AvailabilityRow(stringResource(R.string.now_tx_speed), details.txLinkSpeedMbps, onCopy) { mbpsText(it) }
        AvailabilityRow(stringResource(R.string.now_rx_speed), details.rxLinkSpeedMbps, onCopy) { mbpsText(it) }
        AvailabilityRow(stringResource(R.string.now_max_tx_speed), details.maxTxLinkSpeedMbps, onCopy) { mbpsText(it) }
        AvailabilityRow(stringResource(R.string.now_max_rx_speed), details.maxRxLinkSpeedMbps, onCopy) { mbpsText(it) }
        AvailabilityRow(stringResource(R.string.now_security), details.security, onCopy) { it.text() }
    }
}

/** IPv4/IPv6, gateway, DNS and MTU. */
@Composable
internal fun IpSection(
    details: ConnectionDetails,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ip = details.ip
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.now_section_ip))
        AvailabilityRow(stringResource(R.string.now_ipv4), ip.ipv4, onCopy) { it.text() }
        AvailabilityRow(
            label = stringResource(R.string.now_ipv6),
            value = Availability.Available(ip.ipv6),
            onCopy = onCopy,
        ) { addresses -> addresses.joinOrNone { it.text() } }
        AvailabilityRow(stringResource(R.string.now_gateway), ip.gateway, onCopy) { it }
        AvailabilityRow(
            stringResource(R.string.now_dns),
            ip.dnsServers,
            onCopy,
        ) { servers -> servers.joinOrNone { it } }
        AvailabilityRow(stringResource(R.string.now_mtu), ip.mtu, onCopy) { it.toString() }
    }
}

@Composable
private fun <T> List<T>.joinOrNone(format: @Composable (T) -> String): String =
    if (isEmpty()) stringResource(R.string.now_value_none) else map { format(it) }.joinToString("\n")

/**
 * Row for an [Availability]: the formatted value (long-press copies it) or "Unavailable" plus the
 * reason, never a placeholder zero.
 */
@Composable
internal fun <T> AvailabilityRow(
    label: String,
    value: Availability<T>,
    onCopy: (String) -> Unit,
    format: @Composable (T) -> String,
) {
    when (value) {
        is Availability.Available -> {
            val text = format(value.value)
            ValueRow(
                label = label,
                value = text,
                onLongClick = { onCopy(text) },
                onLongClickLabel = stringResource(R.string.now_copy_value),
            )
        }

        is Availability.Unavailable -> {
            val reason: UnavailableReason = value.reason
            ValueRow(label = label, value = null, unavailableReason = reason.text())
        }
    }
}
