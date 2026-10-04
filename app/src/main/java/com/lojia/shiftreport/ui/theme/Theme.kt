package com.lojia.shiftreport.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LightColorScheme = lightColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryTealLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryPurpleLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorRedLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = PureWhite,
    onBackground = OnBackgroundLight,
    surface = PureWhite,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = PureWhite,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = Color(0x66000000),
    surfaceBright = PureWhite,
    surfaceDim = Color(0xFFF1F5F9),
    surfaceContainer = PureWhite,
    surfaceContainerHigh = PureWhite,
    surfaceContainerHighest = Color(0xFFF8FAFC),
    surfaceContainerLow = PureWhite,
    surfaceContainerLowest = PureWhite
)

@Composable
fun LojiaTheme(
    content: @Composable () -> Unit
) {
    // Enforce the crisp White & Black Business LightColorScheme across the entire app
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val currentLanguage = androidx.compose.runtime.remember(configuration) {
        val locale = configuration.locales[0]
        locale?.language ?: com.lojia.shiftreport.util.AppLanguageManager.getCurrentLanguageCode()
    }
    val currentTypography = androidx.compose.runtime.remember(currentLanguage) {
        typographyFor(currentLanguage)
    }

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    // Soft clamp fontScale to max 1.25 to prevent extreme system accessibility sizes from breaking dialogs and compact grids
    val clampedDensity = androidx.compose.ui.unit.Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale.coerceIn(0.85f, 1.25f)
    )

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides clampedDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = currentTypography,
            content = content
        )
    }
}
