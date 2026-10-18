package io.github.daredevilq.wistride.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme

/**
 * Card conveying a state or problem. The action button is shown only when both [actionLabel] and
 * [onAction] are provided.
 */
@Composable
fun StatusCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val container = tone.containerColor()
    val content = tone.contentColor()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(modifier = Modifier.padding(WiStrideTheme.spacing.l)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = WiStrideTheme.spacing.xs),
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = content, contentColor = container),
                    modifier = Modifier.padding(top = WiStrideTheme.spacing.m),
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
private fun StatusTone.containerColor(): Color =
    when (this) {
        StatusTone.Info -> MaterialTheme.colorScheme.secondaryContainer
        StatusTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer
        StatusTone.Error -> MaterialTheme.colorScheme.errorContainer
    }

@Composable
private fun StatusTone.contentColor(): Color =
    when (this) {
        StatusTone.Info -> MaterialTheme.colorScheme.onSecondaryContainer
        StatusTone.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
        StatusTone.Error -> MaterialTheme.colorScheme.onErrorContainer
    }

@PreviewLightDark
@Composable
private fun StatusCardPreview() {
    WiStrideTheme(dynamicColor = false) {
        Column(verticalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.s)) {
            StatusCard(title = "Info", message = "Scanning is active.")
            StatusCard(
                title = "Location off",
                message = "Wi-Fi scans need location.",
                tone = StatusTone.Warning,
                actionLabel = "Open settings",
                onAction = {},
            )
            StatusCard(title = "Error", message = "Scan failed.", tone = StatusTone.Error)
        }
    }
}
