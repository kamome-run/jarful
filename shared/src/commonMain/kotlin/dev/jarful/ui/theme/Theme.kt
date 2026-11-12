package dev.jarful.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Warm paper-like palette: sticky-note yellow accent, receipt-white surfaces. */
object JarfulColors {
    val Ink = Color(0xFF1F1B16)
    val Paper = Color(0xFFFFFCF5)
    val PaperDark = Color(0xFF17150F)
    val Sticky = Color(0xFFFFD84D)
    val StickyDeep = Color(0xFFB88900)
    val Mint = Color(0xFF2E9E6B)
    val MintSoft = Color(0xFFD9F3E5)
    val Coral = Color(0xFFE85D4A)
    val Jar = Color(0xFF8FC7E8)
    val PaperBall = Color(0xFFF2E8CF)
    val PaperBallShade = Color(0xFFC9B991)
}

private val Light = lightColorScheme(
    primary = JarfulColors.StickyDeep,
    onPrimary = Color.White,
    primaryContainer = JarfulColors.Sticky,
    onPrimaryContainer = JarfulColors.Ink,
    secondary = JarfulColors.Mint,
    onSecondary = Color.White,
    secondaryContainer = JarfulColors.MintSoft,
    onSecondaryContainer = Color(0xFF0D3B26),
    tertiary = JarfulColors.Jar,
    background = JarfulColors.Paper,
    onBackground = JarfulColors.Ink,
    surface = Color.White,
    onSurface = JarfulColors.Ink,
    surfaceVariant = Color(0xFFF1EBDD),
    onSurfaceVariant = Color(0xFF5B5446),
    error = JarfulColors.Coral,
    outline = Color(0xFFCFC6B2),
)

private val Dark = darkColorScheme(
    primary = JarfulColors.Sticky,
    onPrimary = JarfulColors.Ink,
    primaryContainer = Color(0xFF5A4400),
    onPrimaryContainer = JarfulColors.Sticky,
    secondary = Color(0xFF7ED3A6),
    onSecondary = Color(0xFF00391F),
    secondaryContainer = Color(0xFF1F5A3B),
    onSecondaryContainer = JarfulColors.MintSoft,
    tertiary = JarfulColors.Jar,
    background = JarfulColors.PaperDark,
    onBackground = Color(0xFFEDE6D8),
    surface = Color(0xFF221F18),
    onSurface = Color(0xFFEDE6D8),
    surfaceVariant = Color(0xFF3A362C),
    onSurfaceVariant = Color(0xFFCFC6B2),
    error = Color(0xFFFF8A75),
    outline = Color(0xFF6B6455),
)

@Composable
fun JarfulTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
