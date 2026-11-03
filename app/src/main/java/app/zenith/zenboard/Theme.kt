package app.zenith.zenboard

import android.content.Context
import android.os.Build
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

/*
 * Design language shared with the owner's Kontract app (M3 Expressive): wallpaper colours,
 * an AMOLED near-black surface ladder in dark mode, white cards on a tinted page in light mode,
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

/** Wallpaper colours on Android 12+; the stock M3 baseline before that. */
private fun schemeFor(context: Context, dark: Boolean): ColorScheme = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context).withDarkSurfaces()
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context).withLightSurfaces()
    dark -> darkColorScheme().withDarkSurfaces()
    else -> lightColorScheme().withLightSurfaces()
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
    /** Key-press morph: quick enough for fast typing, with a little overshoot. */
    fun <T> press(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)

    /** Gently springy, for small transforms such as a pressed button. */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
}

@Composable
fun ZenithTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = remember(context, dark) { schemeFor(context, dark) }
    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography) {
            // Screens draw on plain backgrounds, not Surfaces; without this, bare Icons default to black.
            CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
        }
    }
}
