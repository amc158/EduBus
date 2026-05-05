package com.alberto.edubus.ui.theme

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

// --- TEMA CLARO ---
private val LightColorScheme = lightColorScheme(
    primary = DeepSpaceBlue,
    onPrimary = White,
    primaryContainer = DeepSkyBlue,
    onPrimaryContainer = DeepSpaceBlue,
    secondary = LightSeaGreen,
    onSecondary = White,
    tertiary = Orange,
    onTertiary = DeepSpaceBlue,
    background = LightBackground,
    surface = White,
    onBackground = DeepSpaceBlue,
    onSurface = DeepSpaceBlue,
    error = Color(0xFFB3261E)
)

// --- TEMA OSCURO ---
private val DarkColorScheme = darkColorScheme(
    primary = DeepSkyBlue,
    onPrimary = DeepSpaceBlue,
    primaryContainer = DeepSpaceBlue,
    onPrimaryContainer = DeepSkyBlue,
    secondary = LightSeaGreen,
    onSecondary = DarkBackground,
    tertiary = Orange,
    onTertiary = DarkBackground,
    background = DarkBackground,
    surface = DeepSpaceBlue,
    onBackground = White,
    onSurface = White,
    error = Color(0xFFF2B8B5)
)

@Composable
fun EduBusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // IMPORTANTE: Ponemos dynamicColor a 'false' por defecto para que Android
    // no sobrescriba tu paleta con los colores del fondo de pantalla del usuario (Material You).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Asegúrate de tener tu archivo Type.kt
        content = content
    )
}