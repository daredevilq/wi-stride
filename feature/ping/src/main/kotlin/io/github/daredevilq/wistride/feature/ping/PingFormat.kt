package io.github.daredevilq.wistride.feature.ping

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import io.github.daredevilq.wistride.core.model.ping.LatencyUnavailableReason
import io.github.daredevilq.wistride.core.model.ping.PingMethod
import java.text.NumberFormat
import java.util.Locale

private const val MS_PER_SECOND = 1_000.0
private const val ONE_DECIMAL_BELOW_MS = 10.0

/** Locale-aware milliseconds: one decimal below 10 ms (where it matters on a LAN), whole numbers above. */
internal fun formatMs(
    value: Double,
    locale: Locale,
): String {
    val digits = if (value < ONE_DECIMAL_BELOW_MS) 1 else 0
    return NumberFormat
        .getInstance(locale)
        .apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }.format(value)
}

/** Percentage with at most one decimal ("0", "3.3"). */
internal fun formatPercent(
    value: Double,
    locale: Locale,
): String =
    NumberFormat
        .getInstance(locale)
        .apply { maximumFractionDigits = 1 }
        .format(value)

/** Seconds with at most one decimal, for probe intervals ("0.5", "2"). */
internal fun formatSeconds(
    intervalMs: Long,
    locale: Locale,
): String =
    NumberFormat
        .getInstance(locale)
        .apply { maximumFractionDigits = 1 }
        .format(intervalMs / MS_PER_SECOND)

@Composable
internal fun currentLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.locales[0] }
}

@Composable
internal fun msText(value: Double): String = stringResource(R.string.ping_ms, formatMs(value, currentLocale()))

@Composable
internal fun percentText(value: Double): String =
    stringResource(R.string.ping_percent, formatPercent(value, currentLocale()))

@Composable
internal fun intervalText(intervalMs: Long): String =
    stringResource(R.string.ping_interval_seconds, formatSeconds(intervalMs, currentLocale()))

/** Short label for the method chip; both names are protocol terms and stay untranslated. */
@Composable
internal fun PingMethod.chipText(): String =
    when (this) {
        PingMethod.Icmp -> stringResource(R.string.ping_method_icmp)
        is PingMethod.TcpConnect -> stringResource(R.string.ping_method_tcp, port)
    }

/** Longer description for the detail view. */
@Composable
internal fun PingMethod.description(): String =
    when (this) {
        PingMethod.Icmp -> stringResource(R.string.ping_method_icmp_description)
        is PingMethod.TcpConnect -> stringResource(R.string.ping_method_tcp_description, port)
    }

@Composable
internal fun LatencyUnavailableReason.text(): String =
    stringResource(
        when (this) {
            LatencyUnavailableReason.NoWifi -> R.string.ping_reason_no_wifi
            LatencyUnavailableReason.NoGateway -> R.string.ping_reason_no_gateway
            LatencyUnavailableReason.UnknownHost -> R.string.ping_reason_unknown_host
            is LatencyUnavailableReason.NeedsPermission -> R.string.ping_reason_needs_permission
        },
    )
