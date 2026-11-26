package com.moneytracker.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "The Obsidian Ledger" color palette — exact design tokens from DESIGN.md.
 */
object ObsidianColors {
    // Deep Canvas & Jade Surface Ramp
    val ObsidianCanvas = Color(0xFF09110F)
    val JadeSurface = Color(0xFF121E1B)
    val JadeElevated = Color(0xFF172622)
    val JadeMuted = Color(0xFF1D2E2A)
    val AmbientGlow = Color(0xFF133C2F)

    // Primary Emerald Ramp
    val EmeraldPrimary = Color(0xFF28C991)
    val EmeraldBright = Color(0xFF35D49A)
    val EmeraldMid = Color(0xFF17A376)
    val EmeraldDeep = Color(0xFF0E6E52)
    val TextOnAccent = Color(0xFF051A13)

    // Secondary Analytical & Highlight Accents
    val ElectricViolet = Color(0xFF8E7CF8)
    val VioletDeep = Color(0xFF6A58E8)
    val ChartreuseLime = Color(0xFFC8F865)
    val SkyBlue = Color(0xFF38BDF8)

    // Semantic Status
    val Danger = Color(0xFFF06478)
    val Warning = Color(0xFFF0B44A)

    // Text Hierarchy
    val TextPrimary = Color(0xFFF2F7F5)
    val TextSecondary = Color(0xFF98ADA6)
    val TextMuted = Color(0xFF6E857D)

    // Specular Hairlines
    val Hairline = Color(0x12FFFFFF)      // rgba(255, 255, 255, 0.07)
    val HairlineHover = Color(0x24FFFFFF) // rgba(255, 255, 255, 0.14)
    val SpecularTop = Color(0x0FFFFFFF)   // rgba(255, 255, 255, 0.06)

    // Ordered categorical chart palette (emerald, violet, chartreuse, sky, amber, rose)
    val ChartPalette: List<Color> = listOf(
        ElectricViolet,
        EmeraldPrimary,
        ChartreuseLime,
        SkyBlue,
        Warning,
        Danger
    )
}
