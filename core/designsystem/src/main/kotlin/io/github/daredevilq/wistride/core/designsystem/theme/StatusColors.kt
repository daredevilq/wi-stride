package io.github.daredevilq.wistride.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Signal-quality colors with matching content colors; `none` means no signal / unknown. */
@Immutable
data class StatusColors(
    val excellent: Color,
    val onExcellent: Color,
    val good: Color,
    val onGood: Color,
    val fair: Color,
    val onFair: Color,
    val weak: Color,
    val onWeak: Color,
    val none: Color,
    val onNone: Color,
)

/** Provides [StatusColors]; supplied by `WiStrideTheme`. */
val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
