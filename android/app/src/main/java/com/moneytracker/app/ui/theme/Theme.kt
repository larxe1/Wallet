package com.moneytracker.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = ObsidianColors.EmeraldPrimary,
    onPrimary = ObsidianColors.TextOnAccent,
    primaryContainer = ObsidianColors.EmeraldDeep,
    onPrimaryContainer = ObsidianColors.TextPrimary,
    secondary = ObsidianColors.ElectricViolet,
    onSecondary = ObsidianColors.ObsidianCanvas,
    secondaryContainer = ObsidianColors.VioletDeep,
    onSecondaryContainer = ObsidianColors.TextPrimary,
    tertiary = ObsidianColors.ChartreuseLime,
    onTertiary = ObsidianColors.TextOnAccent,
    tertiaryContainer = Color(0xFF2E3C14),
    onTertiaryContainer = ObsidianColors.ChartreuseLime,
    background = ObsidianColors.ObsidianCanvas,
    onBackground = ObsidianColors.TextPrimary,
    surface = ObsidianColors.JadeSurface,
    onSurface = ObsidianColors.TextPrimary,
    surfaceVariant = ObsidianColors.JadeElevated,
    onSurfaceVariant = ObsidianColors.TextSecondary,
    surfaceContainerLowest = ObsidianColors.ObsidianCanvas,
    surfaceContainerLow = Color(0xFF0E1815),
    surfaceContainer = ObsidianColors.JadeSurface,
    surfaceContainerHigh = ObsidianColors.JadeElevated,
    surfaceContainerHighest = ObsidianColors.JadeMuted,
    outline = ObsidianColors.HairlineHover,
    outlineVariant = ObsidianColors.Hairline,
    error = ObsidianColors.Danger,
    onError = Color.White,
    errorContainer = Color(0xFF42161E),
    onErrorContainer = Color(0xFFFFD9DE)
)

@Composable
fun MoneyTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianDarkColorScheme,
        typography = ObsidianTypography,
        content = content
    )
}
