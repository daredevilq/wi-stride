package io.github.daredevilq.wistride.feature.now

import android.content.ClipData
import android.os.Build
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.daredevilq.wistride.core.ui.permission.PermissionRequirement
import io.github.daredevilq.wistride.core.ui.permission.rememberPermissionRequestState
import io.github.daredevilq.wistride.core.wifi.WifiPermissions
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object NowRoute

fun NavGraphBuilder.nowScreen() {
    composable<NowRoute> { NowScreenRoute() }
}

private const val WIFI_DETAILS_PERMISSION_ID = "wifi_details"

/** Wires [NowViewModel], the runtime-permission state and system intents to the stateless [NowScreen]. */
@Composable
internal fun NowScreenRoute(
    modifier: Modifier = Modifier,
    viewModel: NowViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requirement =
        remember {
            PermissionRequirement(
                id = WIFI_DETAILS_PERMISSION_ID,
                permissions = WifiPermissions.connectionDetails(),
                titleRes = R.string.now_permission_title,
                rationaleRes = R.string.now_permission_rationale,
            )
        }
    val permissionState = rememberPermissionRequestState(requirement, viewModel.permissionRequestHistory)
    // A null snapshot is still resolving; the screen stays on Loading rather than flash a wrong card.
    val permissionSnapshot = permissionState.snapshot
    LaunchedEffect(permissionSnapshot) { permissionSnapshot?.let(viewModel::onPermissionChanged) }

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val copiedMessage = stringResource(R.string.now_copied)
    val actions =
        NowActions(
            onRequestPermission = permissionState::launchRequest,
            onOpenAppSettings = permissionState::openAppSettings,
            onOpenWifiPanel = context::openWifiPanel,
            onOpenAirplaneSettings = context::openAirplaneSettings,
            onOpenLocationSettings = context::openLocationSettings,
            onCopy = { text ->
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, text)))
                    // Android 13+ shows its own "copied" confirmation; a second one would be noise.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        snackbarHostState.showSnackbar(copiedMessage)
                    }
                }
            },
        )
    NowScreen(state = state, actions = actions, modifier = modifier, snackbarHostState = snackbarHostState)
}
