package com.yaqeengulf.deepfake.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = TextPrimaryDark,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = TrustBlue,
    onSecondary = TextPrimaryDark,
    background = DarkNavyBackground,
    onBackground = TextPrimaryDark,
    surface = DarkNavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkNavySurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = DangerRed,
    onError = TextPrimaryDark
)

@Composable
fun YaqeenTheme(
    darkTheme: Boolean = true, // Force dark mode for high-contrast verification UI
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = MintTealHeader.toArgb()
                window.navigationBarColor = BottomNavBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
