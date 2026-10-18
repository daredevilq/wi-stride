package io.github.daredevilq.wistride.feature.now

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import io.github.daredevilq.wistride.core.ui.permission.PermissionRationaleCard

private val MaxContentWidth = 640.dp

/** Callbacks of the "Now" screen; the defaults do nothing so previews and tests stay terse. */
internal data class NowActions(
    val onRequestPermission: () -> Unit = {},
    val onOpenAppSettings: () -> Unit = {},
    val onOpenWifiPanel: () -> Unit = {},
    val onOpenAirplaneSettings: () -> Unit = {},
    val onOpenLocationSettings: () -> Unit = {},
    val onCopy: (String) -> Unit = {},
)

/** Stateless "Now" screen: live Wi-Fi connection details with no refresh button. */
@Composable
internal fun NowScreen(
    state: NowUiState,
    actions: NowActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        // The enclosing app scaffold already consumed the system bar insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when (state) {
                NowUiState.Loading -> LoadingContent()
                is NowUiState.Loaded -> LoadedContent(state, actions)
            }
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
            text = stringResource(R.string.now_loading),
            modifier = Modifier.padding(top = WiStrideTheme.spacing.m),
        )
    }
}

@Composable
private fun LoadedContent(
    state: NowUiState.Loaded,
    actions: NowActions,
) {
    Column(
        modifier =
            Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(WiStrideTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(WiStrideTheme.spacing.m),
    ) {
        PermissionRationaleCard(
            title = stringResource(R.string.now_permission_title),
            rationale = stringResource(R.string.now_permission_rationale),
            status = state.permission,
            onRequest = actions.onRequestPermission,
            onOpenSettings = actions.onOpenAppSettings,
        )
        if (state.locationServicesOff) LocationOffCard(actions.onOpenLocationSettings)
        when (val link = state.link) {
            LinkStatus.AirplaneMode -> {
                AirplaneModeCard(actions.onOpenAirplaneSettings)
            }

            LinkStatus.WifiOff -> {
                WifiOffCard(actions.onOpenWifiPanel)
            }

            LinkStatus.Disconnected -> {
                DisconnectedCard(actions.onOpenWifiPanel)
            }

            is LinkStatus.Connected -> {
                HeaderCard(link.details, actions.onCopy)
                RssiChartCard(state.rssiHistory)
                RadioSection(link.details, actions.onCopy)
                IpSection(link.details, actions.onCopy)
            }
        }
    }
}
