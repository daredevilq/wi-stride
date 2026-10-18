package io.github.daredevilq.wistride.feature.ping

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.daredevilq.wistride.core.ui.permission.PermissionRequirement
import io.github.daredevilq.wistride.core.ui.permission.rememberPermissionRequestState
import kotlinx.serialization.Serializable

@Serializable
data object PingRoute

fun NavGraphBuilder.pingScreen() {
    composable<PingRoute> { PingScreenRoute() }
}

private const val LOCAL_NETWORK_PERMISSION_ID = "local_network"

// ACCESS_LOCAL_NETWORK exists from Android 17; earlier versions need no runtime grant.
private const val LOCAL_NETWORK_MIN_SDK = 37

/** Wires [PingViewModel], the local-network permission and system intents to the stateless [PingScreen]. */
@Composable
internal fun PingScreenRoute(
    modifier: Modifier = Modifier,
    viewModel: PingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requirement =
        remember {
            PermissionRequirement(
                id = LOCAL_NETWORK_PERMISSION_ID,
                permissions = listOf(Manifest.permission.ACCESS_LOCAL_NETWORK),
                titleRes = R.string.ping_permission_title,
                rationaleRes = R.string.ping_permission_rationale,
                minSdk = LOCAL_NETWORK_MIN_SDK,
            )
        }
    val permissionState = rememberPermissionRequestState(requirement, viewModel.permissionRequestHistory)
    // A null snapshot is still resolving; nothing is measured until it is known (the monitor reads
    // permissions when it starts).
    val snapshot = permissionState.snapshot
    LaunchedEffect(snapshot) { snapshot?.let(viewModel::onPermissionChanged) }

    val actions =
        PingActions(
            onSelect = viewModel::selectTarget,
            onBack = { viewModel.selectTarget(null) },
            onAddTarget = viewModel::addTarget,
            onRemoveTarget = viewModel::removeTarget,
            onSetInterval = viewModel::setInterval,
            onRestoreDefaults = viewModel::restoreDefaultTargets,
            onRequestPermission = permissionState::launchRequest,
            onOpenAppSettings = permissionState::openAppSettings,
            onOpenWifiPanel = context::openWifiPanel,
        )
    PingScreen(state = state, actions = actions, modifier = modifier)
}
