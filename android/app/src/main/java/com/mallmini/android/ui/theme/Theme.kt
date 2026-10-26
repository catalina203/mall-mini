package com.mallmini.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MallColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = TerracottaSoft,
    onPrimaryContainer = ColorTokens.OnPrimaryContainer,
    secondary = Success,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = ColorTokens.SuccessSoft,
    onSecondaryContainer = Success,
    tertiary = Warning,
    background = Cream,
    onBackground = Ink,
    surface = SurfaceWarm,
    onSurface = Ink,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = ColorTokens.OutlineSoft,
    error = Danger,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = ColorTokens.DangerSoft,
    onErrorContainer = Danger,
)

private object ColorTokens {
    val OnPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF5A2416)
    val SuccessSoft = androidx.compose.ui.graphics.Color(0xFFE8F4EF)
    val OutlineSoft = androidx.compose.ui.graphics.Color(0xFFF2EAE1)
    val DangerSoft = androidx.compose.ui.graphics.Color(0xFFFBE8E4)
}

@Composable
fun MallMiniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MallColors,
        typography = MallMiniTypography,
        shapes = MallMiniShapes,
        content = content,
    )
}
