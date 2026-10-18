package io.github.daredevilq.wistride.core.designsystem.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.daredevilq.wistride.core.designsystem.R
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme

private val MinRowHeight = 48.dp

/**
 * Label/value row. A null [value] renders a localized "Unavailable" in a muted style (never "0" or
 * blank), with [unavailableReason] as supporting text when given.
 *
 * When [onLongClick] is set the row reacts to a long press (and exposes it to accessibility
 * services as a custom action named [onLongClickLabel]); when null the row is inert.
 */
@Composable
fun ValueRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    unavailableReason: String? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
) {
    // The lambda identity changes with every RSSI sample; reading it through a state keeps the gesture
    // detector (keyed on Unit) from restarting and cancelling an ongoing press.
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val haptics = LocalHapticFeedback.current
    Row(
        modifier =
            modifier.heightIn(min = MinRowHeight).longClickable(onLongClick != null, onLongClickLabel) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnLongClick?.invoke()
            },
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

private fun Modifier.longClickable(
    enabled: Boolean,
    label: String?,
    onLongPress: () -> Unit,
): Modifier =
    if (!enabled) {
        this
    } else {
        this
            .semantics(mergeDescendants = true) {
                onLongClick(label = label) {
                    onLongPress()
                    true
                }
            }.pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
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
