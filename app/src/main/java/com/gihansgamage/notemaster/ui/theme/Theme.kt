package com.gihansgamage.notemaster.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Cloud,
    primaryContainer = IndigoPrimary, // FAB Background in Light Mode
    onPrimaryContainer = Cloud,       // FAB Icon in Light Mode
    secondary = IndigoSecondary,
    onSecondary = Cloud,
    tertiary = Sage,
    background = SlateBackground,
    surface = SlateSurface,
    surfaceVariant = IndigoContainer,
    onBackground = SlateTextPrimary,
    onSurface = SlateTextPrimary,
    outline = SlateOutline,
)

private val DarkColors = darkColorScheme(
    primary = Sage,
    onPrimary = SlateTextPrimary,
    primaryContainer = Cloud,         // FAB Background in Dark Mode (Swapping to Light Colors)
    onPrimaryContainer = IndigoPrimary, // FAB Icon in Dark Mode (Swapping to Dark Color)
    secondary = Clay,
    onSecondary = Cloud,
    tertiary = Linen,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
)

@Composable
fun NoteMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NoteMasterTypography,
        content = content,
    )
}
