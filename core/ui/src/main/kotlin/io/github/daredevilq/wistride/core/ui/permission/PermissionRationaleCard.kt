package io.github.daredevilq.wistride.core.ui.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.ui.R

private val MinTouchTarget = 48.dp

/**
 * Explains why a permission is needed and offers the next step for [status]; renders nothing
 * when it is [PermissionStatus.Granted].
 */
@Composable
fun PermissionRationaleCard(
    title: String,
    rationale: String,
    status: PermissionStatus,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (status == PermissionStatus.Granted) return
    val blocked = status == PermissionStatus.PermanentlyDenied
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = rationale, style = MaterialTheme.typography.bodyMedium)
            if (blocked) {
                Text(
                    text = stringResource(R.string.permission_blocked_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = if (blocked) onOpenSettings else onRequest,
                modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget),
            ) {
                Text(
                    stringResource(
                        if (blocked) R.string.permission_action_open_settings else R.string.permission_action_allow,
                    ),
                )
            }
        }
    }
}
