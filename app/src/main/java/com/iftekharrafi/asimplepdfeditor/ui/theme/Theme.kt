package com.iftekharrafi.asimplepdfeditor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentPurple,
    tertiary = AccentCyan,
    background = EditorBackground,
    surface = EditorSurface,
    surfaceVariant = EditorSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = EditorOnSurface,
    onSurface = EditorOnSurface,
    onSurfaceVariant = EditorOnSurfaceVariant,
    outline = EditorSurfaceElevated
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentPurple,
    tertiary = AccentCyan,
    background = Color(0xFFF8F9FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEEFF5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F)
)
@Composable
fun ASimplePdfEditorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}