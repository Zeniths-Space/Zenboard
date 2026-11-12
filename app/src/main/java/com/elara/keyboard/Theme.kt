package com.elara.keyboard

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight

/*
 * Design language shared with the owner's Kontract app (M3 Expressive): Elara brand purple,
 * an optional AMOLED near-black surface ladder in dark mode, white cards on a tinted page in light mode,
 * slate ink, semibold type, springy motion.
 *
 * shortcut: plain MaterialTheme, because MaterialExpressiveTheme and MotionScheme.expressive() are
 * not public in stable material3 1.4.0; switch when material3 1.5.0 is stable.
 */

private val SlateInk = Color(0xFF0F172A)
private val SlateMuted = Color(0xFF475569)

/** Near-black dark surfaces, lowest to highest, so tonal keys lift off an AMOLED page. */
private val DarkSurfaces = listOf(
    Color(0xFF050607), Color(0xFF0A0C0E), Color(0xFF0F1215), Color(0xFF151A1E), Color(0xFF1D242A),
)

/** Light layering: tinted page, white cards/keys; anything nested in a card is a step darker. */
private fun ColorScheme.withLightSurfaces(): ColorScheme {
    val page = surfaceContainer
    val card = surfaceContainerLowest
    return copy(
        surface = page,
        background = page,
        surfaceBright = card,
        surfaceContainerLow = page,
        surfaceContainer = card,
        surfaceContainerHigh = card,
        surfaceContainerHighest = surfaceContainerHigh,
        surfaceTint = primary,
        onSurface = SlateInk,
        onBackground = SlateInk,
        onSurfaceVariant = SlateMuted,
    )
}

/** Dark treatment: near-black ladder and light slate ink, keeping the scheme's accents. */
private fun ColorScheme.withDarkSurfaces(): ColorScheme {
    val s = DarkSurfaces
    return copy(
        surface = s[0],
        background = s[0],
        surfaceDim = s[0],
        surfaceBright = s[4],
        surfaceContainerLowest = s[0],
        surfaceContainerLow = s[1],
        surfaceContainer = s[2],
        surfaceContainerHigh = s[3],
        surfaceContainerHighest = s[4],
        outlineVariant = lerp(outline, s[0], 0.4f),
        onSurface = Color(0xFFF1F5F9),
        onBackground = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFFCBD5E1),
    )
}

/** Grey of the same brightness: M3 schemes tint every surface, which reads as a purple cast. */
private fun Color.neutral(): Color {
    val y = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return Color(y, y, y, alpha)
}

/** Neutral backgrounds, cards and keys; accents (primary, secondary, ...) keep the brand purple. */
private fun ColorScheme.withNeutralSurfaces(): ColorScheme = copy(
    surface = surface.neutral(),
    background = background.neutral(),
    surfaceDim = surfaceDim.neutral(),
    surfaceBright = surfaceBright.neutral(),
    surfaceVariant = surfaceVariant.neutral(),
    surfaceContainerLowest = surfaceContainerLowest.neutral(),
    surfaceContainerLow = surfaceContainerLow.neutral(),
    surfaceContainer = surfaceContainer.neutral(),
    surfaceContainerHigh = surfaceContainerHigh.neutral(),
    surfaceContainerHighest = surfaceContainerHighest.neutral(),
)

/** Elara brand purple (#7C4DFF) as M3 roles; secondary, tertiary and surfaces keep the purple-grey M3 baseline. */
private val BrandLight = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF23005C),
    inversePrimary = Color(0xFFCFBCFF),
    secondaryContainer = Color(0xFFE9DEF8),
    onSecondaryContainer = Color(0xFF1F182B),
    surfaceTint = Color(0xFF7C4DFF),
)

private val BrandDark = darkColorScheme(
    primary = Color(0xFFCFBCFF),
    onPrimary = Color(0xFF33009C),
    primaryContainer = Color(0xFF5A22D8),
    onPrimaryContainer = Color(0xFFE9DDFF),
    inversePrimary = Color(0xFF7C4DFF),
    secondaryContainer = Color(0xFF4B4358),
    onSecondaryContainer = Color(0xFFE9DEF8),
    surfaceTint = Color(0xFFCFBCFF),
)

/** A keyboard theme: its accent, and whether it fixes light or dark (null follows the system). */
enum class KeyboardTheme(val label: Int, val accent: Color, val dark: Boolean?, val amoled: Boolean = false) {
    Brand(R.string.theme_brand, Color(0xFF7C4DFF), null),
    Black(R.string.theme_black, Color(0xFF7C4DFF), true, amoled = true),
    Light(R.string.theme_light, Color(0xFF7C4DFF), false),
    Ocean(R.string.theme_ocean, Color(0xFF0E7FB8), null),
    Forest(R.string.theme_forest, Color(0xFF2E7D4F), null),
    Rose(R.string.theme_rose, Color(0xFFD6336C), null);

    companion object {
        fun parse(name: String?) = entries.firstOrNull { it.name == name } ?: Brand
    }
}

/** Accent colours offered on top of a theme (ARGB); 0 keeps the theme's own. */
val AccentChoices = listOf(0xFF7C4DFF, 0xFF0E7FB8, 0xFF2E7D4F, 0xFFD6336C, 0xFFE8590C, 0xFFC99700, 0xFF00897B, 0xFF5C6BC0).map { it.toInt() }

/** Primary, container and secondary roles worked out from one accent, for light or dark. */
private fun ColorScheme.withAccent(accent: Color, dark: Boolean): ColorScheme {
    val white = Color.White
    val black = Color.Black
    return if (dark) {
        val primary = lerp(accent, white, 0.55f)
        copy(
            primary = primary,
            onPrimary = lerp(accent, black, 0.75f),
            primaryContainer = lerp(accent, black, 0.25f),
            onPrimaryContainer = lerp(accent, white, 0.88f),
            inversePrimary = accent,
            secondary = primary,
            secondaryContainer = lerp(accent, black, 0.7f),
            onSecondaryContainer = lerp(accent, white, 0.88f),
            surfaceTint = primary,
        )
    } else {
        copy(
            primary = accent,
            onPrimary = white,
            primaryContainer = lerp(accent, white, 0.82f),
            onPrimaryContainer = lerp(accent, black, 0.7f),
            inversePrimary = lerp(accent, white, 0.5f),
            secondary = accent,
            secondaryContainer = lerp(accent, white, 0.85f),
            onSecondaryContainer = lerp(accent, black, 0.75f),
            surfaceTint = accent,
        )
    }
}

/** The scheme for [theme] and [accent] (0 for the theme's own), light or dark; dark may be the AMOLED ladder. */
internal fun schemeFor(dark: Boolean, theme: KeyboardTheme, accent: Int): ColorScheme {
    val seed = if (accent != 0) Color(accent) else theme.accent
    val base = if (dark) BrandDark else BrandLight
    val coloured = if (seed == KeyboardTheme.Brand.accent) base else base.withAccent(seed, dark)
    val scheme = when {
        dark -> if (theme.amoled) coloured.withDarkSurfaces() else coloured
        else -> coloured.withLightSurfaces()
    }
    return scheme.withNeutralSurfaces()
}

/** System font (the user's choice); only weights are emphasized. */
private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = t.labelSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val LocalDark = staticCompositionLocalOf { false }

object ZenithColors {
    /** Selected/active pill: the secondary accent washed over a card. */
    val indicator: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.run {
            secondary.copy(alpha = if (LocalDark.current) 0.30f else 0.18f).compositeOver(surfaceContainerHigh)
        }

    /** Pressed key: like [indicator], but a softer wash in dark mode so a tap doesn't flash bright. */
    val pressedKey: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.run {
            secondary.copy(alpha = if (LocalDark.current) 0.16f else 0.18f).compositeOver(surfaceContainerHigh)
        }

    /**
     * Function keys (shift, delete, ?123, globe): a tone deeper than letter keys. Light mode uses surfaceDim,
     * since the nested-card tone is nearly the page colour there and the keys would vanish.
     */
    val functionKey: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.run {
            if (LocalDark.current) surfaceContainerHighest else surfaceDim
        }

    /** Content on [indicator]. */
    val onIndicator: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.run {
            if (LocalDark.current) secondary else onSecondaryContainer
        }
}

/** Motion tokens (same values as Kontract's Motion.kt). */
object Motion {
    private val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Colour / opacity changes. */
    fun <T> effects(duration: Int = 300, delay: Int = 0): FiniteAnimationSpec<T> = tween(duration, delay, Standard)

    /** Things arriving on screen, such as a page sliding in. */
    fun <T> spatial(duration: Int = 450): FiniteAnimationSpec<T> = tween(duration, easing = EmphasizedDecelerate)

    /** Key-press morph: quick enough for fast typing, with a little overshoot. */
    fun <T> press(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)

    /** Gently springy, for small transforms such as a pressed button. */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
}

@Composable
fun ZenithTheme(
    theme: KeyboardTheme = KeyboardTheme.Brand,
    accent: Int = 0,
    content: @Composable () -> Unit,
) {
    val dark = theme.dark ?: isSystemInDarkTheme()
    val scheme = remember(dark, theme, accent) { schemeFor(dark, theme, accent) }
    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography) {
            // Screens draw on plain backgrounds, not Surfaces; without this, bare Icons default to black.
            CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
        }
    }
}
