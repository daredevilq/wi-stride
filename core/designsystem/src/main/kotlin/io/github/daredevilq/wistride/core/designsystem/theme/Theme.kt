package io.github.daredevilq.wistride.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * Root theme of the app. Uses Material You dynamic color on Android 12+ (when [dynamicColor]) and
 * the static WiStride palette otherwise. Also provides [Spacing] and [StatusColors].
 */
@Composable
fun WiStrideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                WiStrideDarkColorScheme
            }

            else -> {
                WiStrideLightColorScheme
            }
        }
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalStatusColors provides statusColors,
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = WiStrideTypography, content = content)
    }
}

/** Accessors for WiStride-specific theme tokens, mirroring `MaterialTheme`. */
object WiStrideTheme {
    /** Current [Spacing] scale. */
    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    /** Current signal-quality [StatusColors]. */
    val statusColors: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}
