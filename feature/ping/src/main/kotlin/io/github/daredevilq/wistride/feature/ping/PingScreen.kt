package io.github.daredevilq.wistride.feature.ping

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.daredevilq.wistride.core.designsystem.component.StatusCard
import io.github.daredevilq.wistride.core.designsystem.component.StatusTone
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import io.github.daredevilq.wistride.core.model.ping.PingSettings

private val MinTouchTarget = 48.dp
private val MaxListWidth = 640.dp
private val SideBySideBreakpoint = 600.dp
private const val DETAIL_WEIGHT = 1.4f

/** Callbacks of the "Ping" screen; the defaults do nothing so previews and tests stay terse. */
internal data class PingActions(
    val onSelect: (String) -> Unit = {},
    val onBack: () -> Unit = {},
    val onAddTarget: (host: String, port: Int?) -> Unit = { _, _ -> },
    val onRemoveTarget: (String) -> Unit = {},
    val onSetInterval: (Long) -> Unit = {},
    val onRestoreDefaults: () -> Unit = {},
    val onRequestPermission: () -> Unit = {},
    val onOpenAppSettings: () -> Unit = {},
    val onOpenWifiPanel: () -> Unit = {},
)

/** Stateless "Ping" screen: a live latency list with a detail view per target. */
@Composable
internal fun PingScreen(
    state: PingUiState,
    actions: PingActions,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        // The enclosing app scaffold already consumed the system bar insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { PingTopBar(state.intervalMs(), actions, onAdd = { showAddDialog = true }) },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when (state) {
                PingUiState.Loading -> LoadingContent()
                is PingUiState.Empty -> EmptyContent(actions)
                is PingUiState.NoWifi -> NoWifiContent(actions)
                is PingUiState.Content -> TargetsContent(state, actions)
            }
        }
    }
    if (showAddDialog) {
        AddTargetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { host, port ->
                actions.onAddTarget(host, port)
                showAddDialog = false
            },
        )
    }
}

private fun PingUiState.intervalMs(): Long? =
    when (this) {
        PingUiState.Loading -> null
        is PingUiState.Empty -> intervalMs
        is PingUiState.NoWifi -> intervalMs
        is PingUiState.Content -> intervalMs
    }

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in the pinned Material 3 version.
@Composable
private fun PingTopBar(
    intervalMs: Long?,
    actions: PingActions,
    onAdd: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.feature_ping_title)) },
        actions = {
            if (intervalMs != null) {
                TextButton(onClick = onAdd, modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget)) {
                    Text(stringResource(R.string.ping_action_add))
                }
                IntervalMenu(intervalMs, actions.onSetInterval)
                OverflowMenu(actions.onRestoreDefaults)
            }
        },
    )
}

@Composable
private fun IntervalMenu(
    intervalMs: Long,
    onSelect: (Long) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget)) {
            Text(stringResource(R.string.ping_action_interval, intervalText(intervalMs)))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PingSettings.ALLOWED_INTERVALS_MS.forEach { option ->
                val label = intervalText(option)
                DropdownMenuItem(
                    text = {
                        Text(if (option == intervalMs) stringResource(R.string.ping_interval_current, label) else label)
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun OverflowMenu(onRestoreDefaults: () -> Unit) {
    val moreLabel = stringResource(R.string.ping_action_more)
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.defaultMinSize(MinTouchTarget, MinTouchTarget)) {
            // A text glyph avoids pulling in the icon library for a single overflow marker.
            Text(
                text = stringResource(R.string.ping_overflow_glyph),
                modifier = Modifier.semantics { contentDescription = moreLabel },
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ping_action_restore)) },
                onClick = {
                    expanded = false
                    onRestoreDefaults()
                },
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.ping_loading),
            modifier = Modifier.padding(top = WiStrideTheme.spacing.m),
        )
    }
}

@Composable
private fun EmptyContent(actions: PingActions) {
    StatusCard(
        title = stringResource(R.string.ping_empty_title),
        message = stringResource(R.string.ping_empty_message),
        modifier = Modifier.widthIn(max = MaxListWidth).padding(WiStrideTheme.spacing.l),
        actionLabel = stringResource(R.string.ping_action_restore),
        onAction = actions.onRestoreDefaults,
    )
}

@Composable
private fun NoWifiContent(actions: PingActions) {
    StatusCard(
        title = stringResource(R.string.ping_no_wifi_title),
        message = stringResource(R.string.ping_no_wifi_message),
        modifier = Modifier.widthIn(max = MaxListWidth).padding(WiStrideTheme.spacing.l),
        tone = StatusTone.Warning,
        actionLabel = stringResource(R.string.ping_no_wifi_action),
        onAction = actions.onOpenWifiPanel,
    )
}

@Composable
private fun TargetsContent(
    state: PingUiState.Content,
    actions: PingActions,
) {
    val selected = state.targets.firstOrNull { it.id == state.selectedId }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= SideBySideBreakpoint) {
            Row(modifier = Modifier.fillMaxSize()) {
                TargetList(state, actions, Modifier.weight(1f).fillMaxHeight())
                Box(modifier = Modifier.weight(DETAIL_WEIGHT).fillMaxHeight()) {
                    if (selected != null) {
                        TargetDetail(selected, state.intervalMs, state.permission, showBack = false, actions = actions)
                    } else {
                        Text(
                            text = stringResource(R.string.ping_detail_placeholder),
                            modifier = Modifier.align(Alignment.Center).padding(WiStrideTheme.spacing.l),
                        )
                    }
                }
            }
        } else if (selected != null) {
            BackHandler(onBack = actions.onBack)
            TargetDetail(
                target = selected,
                intervalMs = state.intervalMs,
                permission = state.permission,
                showBack = true,
                actions = actions,
                modifier = Modifier.widthIn(max = MaxListWidth).fillMaxWidth(),
            )
        } else {
            TargetList(state, actions, Modifier.widthIn(max = MaxListWidth).fillMaxSize())
        }
    }
}

@Composable
private fun TargetList(
    state: PingUiState.Content,
    actions: PingActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(WiStrideTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.m),
    ) {
        items(state.targets, key = { it.id }) { target ->
            TargetCard(target, state.permission, selected = target.id == state.selectedId, actions = actions)
        }
    }
}
