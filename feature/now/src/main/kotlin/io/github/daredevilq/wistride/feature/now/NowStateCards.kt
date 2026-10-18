package io.github.daredevilq.wistride.feature.now

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.daredevilq.wistride.core.designsystem.component.StatusCard
import io.github.daredevilq.wistride.core.designsystem.component.StatusTone

/** Wi-Fi radio is off because of airplane mode. */
@Composable
internal fun AirplaneModeCard(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusCard(
        title = stringResource(R.string.now_airplane_title),
        message = stringResource(R.string.now_airplane_message),
        modifier = modifier,
        tone = StatusTone.Warning,
        actionLabel = stringResource(R.string.now_airplane_action),
        onAction = onOpenSettings,
    )
}

@Composable
internal fun WifiOffCard(
    onOpenWifiPanel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusCard(
        title = stringResource(R.string.now_wifi_off_title),
        message = stringResource(R.string.now_wifi_off_message),
        modifier = modifier,
        tone = StatusTone.Warning,
        actionLabel = stringResource(R.string.now_wifi_off_action),
        onAction = onOpenWifiPanel,
    )
}

@Composable
internal fun DisconnectedCard(
    onOpenWifiPanel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusCard(
        title = stringResource(R.string.now_disconnected_title),
        message = stringResource(R.string.now_disconnected_message),
        modifier = modifier,
        actionLabel = stringResource(R.string.now_disconnected_action),
        onAction = onOpenWifiPanel,
    )
}

/** Location services are off while the permission is granted: SSID/BSSID stay redacted. */
@Composable
internal fun LocationOffCard(
    onOpenLocationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusCard(
        title = stringResource(R.string.now_location_off_title),
        message = stringResource(R.string.now_location_off_message),
        modifier = modifier,
        tone = StatusTone.Warning,
        actionLabel = stringResource(R.string.now_location_off_action),
        onAction = onOpenLocationSettings,
    )
}
