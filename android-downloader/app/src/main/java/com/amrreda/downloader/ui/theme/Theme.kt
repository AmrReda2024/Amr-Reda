package com.amrreda.downloader.ui.theme

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

private val Light = lightColorScheme(
    primary = Color(0xFF0B6BCB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFFE65100),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    primaryContainer = Color(0xFF004A8F),
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFFFFB74D),
)

@Composable
fun SmartDownloaderTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
