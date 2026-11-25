package com.mediwell.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MedchckColorScheme = lightColorScheme(
    primary            = ForestGreen,
    onPrimary          = White,
    primaryContainer   = SageGreen,
    onPrimaryContainer = ForestGreen,
    secondary          = SlateSecondary,
    onSecondary        = SlateOnSecondary,
    secondaryContainer = SageGreen,
    onSecondaryContainer = ForestGreen,
    background         = Ivory,
    surface            = White,
    surfaceVariant     = SageGreen,
    onBackground       = TextPrimary,
    onSurface          = TextPrimary,
    onSurfaceVariant   = TextSecondary,
    outline            = BorderGray,
    error              = CriticalRed
)

@Composable
fun MediwellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MedchckColorScheme,
        typography  = Typography,
        content     = content
    )
}

// Alias so any code that calls MedchckTheme also compiles
@Composable
fun MedchckTheme(content: @Composable () -> Unit) = MediwellTheme(content)
