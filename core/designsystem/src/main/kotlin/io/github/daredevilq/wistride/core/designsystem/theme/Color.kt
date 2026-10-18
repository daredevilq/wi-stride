package io.github.daredevilq.wistride.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette: teal primary, blue secondary. The only place raw colors may appear.
private val LightPrimary = Color(0xFF006A6A)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFF9CF1F0)
private val LightOnPrimaryContainer = Color(0xFF002020)
private val LightSecondary = Color(0xFF2F628E)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFCFE5FF)
private val LightOnSecondaryContainer = Color(0xFF001D33)
private val LightTertiary = Color(0xFF7A5900)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFFFDF9E)
private val LightOnTertiaryContainer = Color(0xFF261A00)
private val LightError = Color(0xFFBA1A1A)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFFDAD6)
private val LightOnErrorContainer = Color(0xFF410002)
private val LightBackground = Color(0xFFF4FAF9)
private val LightOnBackground = Color(0xFF161D1D)
private val LightSurfaceVariant = Color(0xFFDAE5E4)
private val LightOnSurfaceVariant = Color(0xFF3F4948)
private val LightOutline = Color(0xFF6F7979)

private val DarkPrimary = Color(0xFF80D5D4)
private val DarkOnPrimary = Color(0xFF003737)
private val DarkPrimaryContainer = Color(0xFF004F4F)
private val DarkOnPrimaryContainer = Color(0xFF9CF1F0)
private val DarkSecondary = Color(0xFF9DCBFC)
private val DarkOnSecondary = Color(0xFF003355)
private val DarkSecondaryContainer = Color(0xFF0E4A75)
private val DarkOnSecondaryContainer = Color(0xFFCFE5FF)
private val DarkTertiary = Color(0xFFEFC048)
private val DarkOnTertiary = Color(0xFF3F2E00)
private val DarkTertiaryContainer = Color(0xFF5C4300)
private val DarkOnTertiaryContainer = Color(0xFFFFDF9E)
private val DarkError = Color(0xFFFFB4AB)
private val DarkOnError = Color(0xFF690005)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)
private val DarkBackground = Color(0xFF0E1514)
private val DarkOnBackground = Color(0xFFDDE4E3)
private val DarkSurfaceVariant = Color(0xFF3F4948)
private val DarkOnSurfaceVariant = Color(0xFFBEC9C8)
private val DarkOutline = Color(0xFF899392)

internal val WiStrideLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        secondary = LightSecondary,
        onSecondary = LightOnSecondary,
        secondaryContainer = LightSecondaryContainer,
        onSecondaryContainer = LightOnSecondaryContainer,
        tertiary = LightTertiary,
        onTertiary = LightOnTertiary,
        tertiaryContainer = LightTertiaryContainer,
        onTertiaryContainer = LightOnTertiaryContainer,
        error = LightError,
        onError = LightOnError,
        errorContainer = LightErrorContainer,
        onErrorContainer = LightOnErrorContainer,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightBackground,
        onSurface = LightOnBackground,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
    )

internal val WiStrideDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        secondaryContainer = DarkSecondaryContainer,
        onSecondaryContainer = DarkOnSecondaryContainer,
        tertiary = DarkTertiary,
        onTertiary = DarkOnTertiary,
        tertiaryContainer = DarkTertiaryContainer,
        onTertiaryContainer = DarkOnTertiaryContainer,
        error = DarkError,
        onError = DarkOnError,
        errorContainer = DarkErrorContainer,
        onErrorContainer = DarkOnErrorContainer,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkBackground,
        onSurface = DarkOnBackground,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
    )

internal val LightStatusColors =
    StatusColors(
        excellent = Color(0xFF1B8E3E),
        onExcellent = Color(0xFFFFFFFF),
        good = Color(0xFF6A9A00),
        onGood = Color(0xFFFFFFFF),
        fair = Color(0xFFB87900),
        onFair = Color(0xFFFFFFFF),
        weak = Color(0xFFC4481C),
        onWeak = Color(0xFFFFFFFF),
        none = Color(0xFF6F7979),
        onNone = Color(0xFFFFFFFF),
    )

internal val DarkStatusColors =
    StatusColors(
        excellent = Color(0xFF6FDD8B),
        onExcellent = Color(0xFF00391A),
        good = Color(0xFFB4DB4A),
        onGood = Color(0xFF243300),
        fair = Color(0xFFFFBA3B),
        onFair = Color(0xFF412C00),
        weak = Color(0xFFFFA283),
        onWeak = Color(0xFF5B1A00),
        none = Color(0xFF899392),
        onNone = Color(0xFF1A2120),
    )
