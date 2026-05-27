package com.insaner1980.framefix.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = FrameFixColors.Accent,
        secondary = FrameFixColors.Muted,
        tertiary = FrameFixColors.Success,
        background = FrameFixColors.Background,
        surface = FrameFixColors.Surface,
        onPrimary = FrameFixColors.OnAccent,
        onBackground = FrameFixColors.OnDark,
        onSurface = FrameFixColors.OnDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = FrameFixColors.Accent,
        secondary = FrameFixColors.Muted,
        tertiary = FrameFixColors.Success,
        onPrimary = FrameFixColors.OnAccent,
    )

@Composable
fun FrameFixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme

            else -> LightColorScheme
        }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
