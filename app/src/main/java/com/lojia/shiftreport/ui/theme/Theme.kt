package com.lojia.shiftreport.ui.theme


import android.app.Activity


import androidx.compose.material3.MaterialTheme


import androidx.compose.material3.lightColorScheme


import androidx.compose.runtime.Composable


import androidx.compose.runtime.SideEffect


import androidx.compose.ui.graphics.toArgb


import androidx.compose.ui.platform.LocalView


import androidx.core.view.WindowCompat

// Solid 100% Light Color Scheme (No Dark Skin / Mode)
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = PureWhite,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondaryTeal,
    onSecondary = PureWhite,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = WarningOrange,
    onTertiary = PureWhite,
    tertiaryContainer = WarningContainer,
    onTertiaryContainer = WarningOrange,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorRed
)

@Composable
fun LojiaTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
