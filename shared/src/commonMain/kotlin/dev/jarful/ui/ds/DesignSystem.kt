package dev.jarful.ui.ds

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jarful.platform.fluentFontFamily
import dev.jarful.ui.theme.JarfulColors
import dev.jarful.ui.theme.JarfulTheme

/**
 * Which platform design system the primitives in this package render as (NFR-8):
 * Material 3 on Android, Fluent Design (WinUI 3) on Windows/desktop. Brand colors are shared.
 */
enum class DesignSystem { MATERIAL, FLUENT }

val LocalDesignSystem = staticCompositionLocalOf { DesignSystem.MATERIAL }

/** Default design system for the current platform: Material on Android, Fluent on desktop. */
expect fun platformDesignSystem(): DesignSystem

/**
 * Fluent (WinUI 3) design tokens tinted with the Jarful brand.
 * Names follow the WinUI resource keys (TextFillColorPrimary, CardBackgroundFillColorDefault, ...).
 */
data class FluentTokens(
    val dark: Boolean,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val textOnAccent: Color,
    val accent: Color,
    val accentHover: Color,
    val accentPressed: Color,
    val accentSoft: Color,
    /** Window / Mica backdrop. */
    val micaBase: Color,
    /** Content layer drawn on top of Mica (LayerFillColorDefault). */
    val layerFill: Color,
    val cardFill: Color,
    val cardFillSecondary: Color,
    val controlFill: Color,
    val controlFillSecondary: Color,
    val controlFillTertiary: Color,
    val subtleFill: Color,
    val controlStroke: Color,
    val cardStroke: Color,
    val dividerStroke: Color,
    val focusStroke: Color,
    val success: Color,
    val caution: Color,
    val critical: Color,
) {
    val controlRadius: Dp get() = 4.dp
    val overlayRadius: Dp get() = 8.dp
}

fun fluentTokens(dark: Boolean): FluentTokens = if (!dark) FluentTokens(
    dark = false,
    textPrimary = Color(0xE4000000), textSecondary = Color(0x9E000000), textDisabled = Color(0x5C000000), textOnAccent = JarfulColors.Ink,
    accent = JarfulColors.Sticky, accentHover = Color(0xFFF2CB3F), accentPressed = Color(0xFFE0BB35), accentSoft = Color(0xFFFFF1BD),
    micaBase = JarfulColors.Paper, layerFill = Color(0x80FFFFFF), cardFill = Color(0xB3FFFFFF), cardFillSecondary = Color(0x80F6F6F6),
    controlFill = Color(0xB3FFFFFF), controlFillSecondary = Color(0x80F9F9F9), controlFillTertiary = Color(0x4DF9F9F9), subtleFill = Color(0x0A000000),
    controlStroke = Color(0x0F000000), cardStroke = Color(0x0F000000), dividerStroke = Color(0x14000000), focusStroke = Color(0xE4000000),
    success = Color(0xFF0F7B0F), caution = Color(0xFF9D5D00), critical = Color(0xFFC42B1C),
) else FluentTokens(
    dark = true,
    textPrimary = Color(0xFFFFFFFF), textSecondary = Color(0xC5FFFFFF), textDisabled = Color(0x5DFFFFFF), textOnAccent = JarfulColors.Ink,
    accent = JarfulColors.Sticky, accentHover = Color(0xFFFFE070), accentPressed = Color(0xFFE6C245), accentSoft = Color(0xFF4A3A00),
    micaBase = JarfulColors.PaperDark, layerFill = Color(0x0DFFFFFF), cardFill = Color(0x0DFFFFFF), cardFillSecondary = Color(0x08FFFFFF),
    controlFill = Color(0x0FFFFFFF), controlFillSecondary = Color(0x15FFFFFF), controlFillTertiary = Color(0x08FFFFFF), subtleFill = Color(0x0FFFFFFF),
    controlStroke = Color(0x12FFFFFF), cardStroke = Color(0x19FFFFFF), dividerStroke = Color(0x15FFFFFF), focusStroke = Color(0xFFFFFFFF),
    success = Color(0xFF6CCB5F), caution = Color(0xFFFCE100), critical = Color(0xFFFF99A4),
)

val LocalFluent = staticCompositionLocalOf { fluentTokens(false) }

/** Fluent type ramp (WinUI 3): Caption 12, Body 14, BodyStrong 14/600, Subtitle 20/600, Title 28/600. */
object FluentType {
    fun family(): FontFamily = fluentFontFamily() ?: FontFamily.SansSerif
    val caption get() = TextStyle(fontFamily = family(), fontSize = 12.sp, lineHeight = 16.sp)
    val body get() = TextStyle(fontFamily = family(), fontSize = 14.sp, lineHeight = 20.sp)
    val bodyStrong get() = TextStyle(fontFamily = family(), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val subtitle get() = TextStyle(fontFamily = family(), fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val title get() = TextStyle(fontFamily = family(), fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)
}

/**
 * Root theme. Material mode delegates to [JarfulTheme]. Fluent mode installs the Fluent tokens and
 * ALSO a MaterialTheme mapped from those tokens, so `Text`/`Icon` defaults and any remaining
 * Material composable pick up Fluent colors and the Segoe type ramp.
 */
@Composable
fun JarfulDesignTheme(system: DesignSystem, dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDesignSystem provides system) {
        when (system) {
            DesignSystem.MATERIAL -> JarfulTheme(dark = dark, content = content)
            DesignSystem.FLUENT -> {
                val t = fluentTokens(dark)
                val base = if (dark) darkColorScheme() else lightColorScheme()
                val scheme = base.copy(
                    primary = if (dark) t.accent else JarfulColors.StickyDeep, onPrimary = t.textOnAccent,
                    primaryContainer = t.accentSoft, onPrimaryContainer = t.textPrimary,
                    secondary = t.success, onSecondary = Color.White,
                    secondaryContainer = if (dark) Color(0xFF1F5A3B) else JarfulColors.MintSoft, onSecondaryContainer = t.textPrimary,
                    tertiary = JarfulColors.Jar,
                    background = t.micaBase, onBackground = t.textPrimary,
                    surface = t.micaBase, onSurface = t.textPrimary,
                    surfaceVariant = if (dark) Color(0xFF2B2B2B) else Color(0xFFF3EFE4), onSurfaceVariant = t.textSecondary,
                    error = t.critical, outline = if (dark) Color(0xFF5A5A5A) else Color(0xFFCFC6B2),
                )
                val m3 = MaterialTheme.typography
                val f = FluentType.family()
                val typography = m3.copy(
                    headlineSmall = m3.headlineSmall.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
                    titleLarge = FluentType.subtitle,
                    titleMedium = FluentType.bodyStrong.copy(fontSize = 16.sp, lineHeight = 22.sp),
                    titleSmall = FluentType.bodyStrong,
                    bodyLarge = FluentType.body.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    bodyMedium = FluentType.body,
                    bodySmall = FluentType.caption,
                    labelLarge = FluentType.bodyStrong,
                    labelMedium = FluentType.caption.copy(fontWeight = FontWeight.SemiBold),
                    labelSmall = FluentType.caption.copy(fontSize = 11.sp),
                )
                CompositionLocalProvider(LocalFluent provides t) {
                    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
                }
            }
        }
    }
}

@Composable
fun isFluent(): Boolean = LocalDesignSystem.current == DesignSystem.FLUENT
