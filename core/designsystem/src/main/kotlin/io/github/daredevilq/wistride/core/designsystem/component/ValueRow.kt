package io.github.daredevilq.wistride.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.daredevilq.wistride.core.designsystem.R
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme

private val MinRowHeight = 48.dp

/**
 * Label/value row. A null [value] renders a localized "Unavailable" in a muted style (never "0" or
 * blank), with [unavailableReason] as supporting text when given.
 */
@Composable
fun ValueRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    unavailableReason: String? = null,
) {
    Row(
        modifier = modifier.heightIn(min = MinRowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.l),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Column(
            modifier = Modifier.weight(1f).padding(vertical = WiStrideTheme.spacing.s),
            horizontalAlignment = Alignment.End,
        ) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.End,
                )
            } else {
                Text(
                    text = stringResource(R.string.designsystem_value_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
                if (unavailableReason != null) {
                    Text(
                        text = unavailableReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ValueRowPreview() {
    WiStrideTheme(dynamicColor = false) {
        Column {
            ValueRow(label = "SSID", value = "HomeNet")
            ValueRow(label = "Link speed", value = null, unavailableReason = "Requires Android 11")
        }
    }
}
