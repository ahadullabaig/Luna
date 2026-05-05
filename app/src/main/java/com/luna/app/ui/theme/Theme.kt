package com.luna.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    background = LunaDeepNavy,
    surface = LunaDeepNavy,
    primary = LunaBlush,
    onPrimary = LunaDeepNavy,
    secondary = LunaSand,
    onSecondary = LunaDeepNavy,
    onBackground = LunaCream,
    onSurface = LunaCream
)

@Composable
fun LunaTheme(
    content: @Composable () -> Unit
) {
    // We strictly use DarkColorScheme because the app design is dark-themed by nature.
    val colorScheme = DarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
