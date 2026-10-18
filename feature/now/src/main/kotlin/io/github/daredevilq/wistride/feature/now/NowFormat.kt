package io.github.daredevilq.wistride.feature.now

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.daredevilq.wistride.core.model.UnavailableReason
import io.github.daredevilq.wistride.core.model.wifi.SignalLevel

private const val SDK_ANDROID_10 = 29
private const val SDK_ANDROID_11 = 30
private const val SDK_ANDROID_12 = 31
private const val SDK_ANDROID_12L = 32

/**
 * Marketing name of the Android release that introduced API level [sdkInt] (29 -> "10", 33 -> "13").
 * API 32 (12L) is shown as 12; levels from 33 on map to `sdkInt - 20`.
 */
internal fun androidVersionName(sdkInt: Int): String =
    when {
        sdkInt <= SDK_ANDROID_10 -> "10"
        sdkInt == SDK_ANDROID_11 -> "11"
        sdkInt == SDK_ANDROID_12 || sdkInt == SDK_ANDROID_12L -> "12"
        else -> (sdkInt - ANDROID_VERSION_OFFSET_FROM_API_33).toString()
    }

private const val ANDROID_VERSION_OFFSET_FROM_API_33 = 20

@Composable
internal fun UnavailableReason.text(): String =
    when (this) {
        is UnavailableReason.RequiresApi -> stringResource(R.string.now_reason_requires_api, androidVersionName(sdkInt))
        is UnavailableReason.NeedsPermission -> stringResource(R.string.now_reason_needs_permission)
        UnavailableReason.NeedsLocationOn -> stringResource(R.string.now_reason_needs_location)
        UnavailableReason.AwaitingData -> stringResource(R.string.now_reason_awaiting_data)
        UnavailableReason.NotReportedByDevice -> stringResource(R.string.now_reason_not_reported)
    }

@Composable
internal fun SignalLevel.text(): String =
    stringResource(
        when (this) {
            SignalLevel.Excellent -> R.string.now_level_excellent
            SignalLevel.Good -> R.string.now_level_good
            SignalLevel.Fair -> R.string.now_level_fair
            SignalLevel.Weak -> R.string.now_level_weak
            SignalLevel.Poor -> R.string.now_level_poor
        },
    )

@Composable
internal fun dbmText(dbm: Int): String = stringResource(R.string.now_value_dbm, dbm)

@Composable
internal fun mbpsText(mbps: Int): String = stringResource(R.string.now_value_mbps, mbps)

@Composable
internal fun mhzText(mhz: Int): String = stringResource(R.string.now_value_mhz, mhz)
