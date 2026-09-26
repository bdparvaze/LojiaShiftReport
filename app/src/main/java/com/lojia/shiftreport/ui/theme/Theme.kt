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
    primary = PrimaryIndigoLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryTealLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryCyanLight,
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

// Alias DarkColorScheme to LightColorScheme so the business app never renders dark/black popups or dropdowns
val DarkColorScheme = LightColorScheme

@Composable
fun LojiaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always enforce the crisp White & Black Business LightColorScheme across the entire app
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PrimaryIndigoLight.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
