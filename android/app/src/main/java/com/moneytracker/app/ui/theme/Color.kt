package com.moneytracker.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Restrained dark obsidian-emerald palette with muted surface hierarchy.
 */
object ObsidianColors {
    // Deep Canvas & Jade Surface Ramp
    val ObsidianCanvas = Color(0xFF0A0F0E)
    val JadeSurface = Color(0xFF121A18)
    val JadeElevated = Color(0xFF192320)
    val JadeMuted = Color(0xFF212E2A)
    val AmbientGlow = Color(0xFF133C2F)

    // Primary Emerald Ramp
    val EmeraldPrimary = Color(0xFF26B883)
    val EmeraldBright = Color(0xFF34C892)
    val EmeraldMid = Color(0xFF1D9A6D)
    val EmeraldDeep = Color(0xFF115E43)
    val TextOnAccent = Color(0xFF051A13)

    // Secondary Analytical Accents (muted for restraint)
    val ElectricViolet = Color(0xFF8378E6)
    val VioletDeep = Color(0xFF665BC9)
    val ChartreuseLime = Color(0xFF9AC95B)
    val SkyBlue = Color(0xFF4CA8D8)

    // Semantic Status
    val Danger = Color(0xFFE36475)
    val Warning = Color(0xFFDFA94B)

    // Text Hierarchy
    val TextPrimary = Color(0xFFF0F4F2)
    val TextSecondary = Color(0xFF93A39E)
    val TextMuted = Color(0xFF667872)

    // Specular Hairlines
    val Hairline = Color(0x14FFFFFF)
    val HairlineHover = Color(0x24FFFFFF)
    val SpecularTop = Color(0x08FFFFFF)

    // Ordered categorical chart palette
    val ChartPalette: List<Color> = listOf(
        ElectricViolet,
        EmeraldPrimary,
        ChartreuseLime,
        SkyBlue,
        Warning,
        Danger
    )
}
