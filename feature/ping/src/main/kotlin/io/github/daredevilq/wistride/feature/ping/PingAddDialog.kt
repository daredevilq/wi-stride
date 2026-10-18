package io.github.daredevilq.wistride.feature.ping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import io.github.daredevilq.wistride.domain.network.TargetInputError
import io.github.daredevilq.wistride.domain.network.TargetValidation
import io.github.daredevilq.wistride.domain.network.validateTarget

/**
 * Add-target form with live validation. Errors appear as the user types (an empty host only after the
 * field was edited), and "Add" is enabled only for a valid pair.
 */
@Composable
internal fun AddTargetDialog(
    onDismiss: () -> Unit,
    onConfirm: (host: String, port: Int?) -> Unit,
) {
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("") }
    var hostEdited by rememberSaveable { mutableStateOf(false) }
    val validation = validateTarget(host, port)
    val error = (validation as? TargetValidation.Invalid)?.error
    val hostError =
        error.takeIf {
            it == TargetInputError.InvalidHost ||
                (it == TargetInputError.EmptyHost && hostEdited)
        }
    val portError = error.takeIf { it == TargetInputError.InvalidPort }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ping_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.m)) {
                HostField(
                    value = host,
                    error = hostError,
                    onValueChange = {
                        host = it
                        hostEdited = true
                    },
                )
                PortField(value = port, error = portError, onValueChange = { port = it })
                Text(
                    text = stringResource(R.string.ping_add_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    (validation as? TargetValidation.Valid)?.let { onConfirm(it.host, it.port) }
                },
                enabled = validation is TargetValidation.Valid,
            ) {
                Text(stringResource(R.string.ping_add_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ping_add_cancel)) }
        },
    )
}

@Composable
private fun HostField(
    value: String,
    error: TargetInputError?,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.ping_add_host)) },
        isError = error != null,
        supportingText = error?.let { { Text(it.text()) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The helper text explains that a port switches the method to a TCP connect. */
@Composable
private fun PortField(
    value: String,
    error: TargetInputError?,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.ping_add_port)) },
        isError = error != null,
        supportingText = { Text(error?.text() ?: stringResource(R.string.ping_add_port_helper)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TargetInputError.text(): String =
    stringResource(
        when (this) {
            TargetInputError.EmptyHost -> R.string.ping_error_empty_host
            TargetInputError.InvalidHost -> R.string.ping_error_invalid_host
            TargetInputError.InvalidPort -> R.string.ping_error_invalid_port
        },
    )
