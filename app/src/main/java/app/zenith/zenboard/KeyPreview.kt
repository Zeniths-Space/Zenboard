package app.zenith.zenboard

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

/** One press of a character key. A new instance per press, so a release only clears its own preview. */
class KeyPress(val label: String, val bounds: Rect)

/** Open long-press alternates row. [options] are already shift-resolved. */
@Stable
class AltPanel(val options: List<String>, val layout: AltLayout, val cellWidth: Float, val keyBounds: Rect, selected: Int) {
    var selected by mutableIntStateOf(selected)
}

@Stable
class PreviewState {
    var current by mutableStateOf<KeyPress?>(null)
    var panel by mutableStateOf<AltPanel?>(null)

    /** Key-press bubbles on or off (user setting). Read at press time, not state. */
    var bubbles = true

    /** Keyboard width in px, for keeping the alternates row on screen. Set by layout, not state. */
    var keyboardWidth = 0f
}

/** Room above the keyboard for top-row previews. */
private val Headroom = 72.dp
private val Gap = 4.dp

/** Previews ignore touches: flags that make the overlay window untouchable and unfocusable. */
private const val OverlayFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
    // Position stays relative to the IME window (FLAG_LAYOUT_IN_SCREEN would make it screen-relative).
    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

/**
 * A single transparent, untouchable window over the keyboard plus [Headroom] above it. It lives while
 * the keyboard is shown and is reused for every press, so typing never adds or removes windows.
 */
@Composable
fun KeyPreviewOverlay(preview: PreviewState, keyboardSize: IntSize) {
    val density = LocalDensity.current
    val headroomPx = with(density) { Headroom.roundToPx() }
    val position = remember(headroomPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize,
            ) = IntOffset(anchorBounds.left, anchorBounds.top - headroomPx)
        }
    }
    Popup(
        popupPositionProvider = position,
        properties = PopupProperties(
            flags = OverlayFlags,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val width = with(density) { keyboardSize.width.toDp() }
        val height = with(density) { keyboardSize.height.toDp() } + Headroom
        Box(Modifier.size(width, height)) {
            val panel = preview.panel
            if (panel != null) {
                key(panel) { AlternatesRow(panel, headroomPx) }
            } else {
                preview.current?.let { press -> key(press) { Bubble(press, headroomPx, keyboardSize.width) } }
            }
        }
    }
}

@Composable
private fun Bubble(press: KeyPress, headroomPx: Int, maxWidthPx: Int) {
    val density = LocalDensity.current
    val bubbleW: Dp
    val bubbleH: Dp
    val offset: IntOffset
    with(density) {
        bubbleW = press.bounds.width.toDp() + 12.dp
        bubbleH = press.bounds.height.toDp() * 1.15f
        val wPx = bubbleW.toPx()
        // Centered over the key, kept inside the keyboard at the q/p edges.
        val x = (press.bounds.center.x - wPx / 2).coerceIn(0f, maxWidthPx - wPx)
        val y = headroomPx + press.bounds.top - bubbleH.toPx() - Gap.toPx()
        offset = IntOffset(x.roundToInt(), y.roundToInt())
    }
    // Expressive pop: springs up from the key; disappears instantly so fast typing stays crisp.
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, Motion.press()) }
    Box(
        Modifier
            .offset { offset }
            .size(bubbleW, bubbleH)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                shadowElevation = 10.dp.toPx()
                shape = RoundedCornerShape(16.dp)
                clip = true
            }
            // Same tint as the pressed key, so bubble and key read as one and white-on-white is avoided.
            .background(ZenithColors.indicator),
        contentAlignment = Alignment.Center,
    ) {
        Text(press.label, color = ZenithColors.onIndicator, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Long-press alternates: tinted row above the key, selected option in solid secondary. */
@Composable
private fun AlternatesRow(panel: AltPanel, headroomPx: Int) {
    val density = LocalDensity.current
    val cs = MaterialTheme.colorScheme
    val cellW: Dp
    val cellH: Dp
    val offset: IntOffset
    with(density) {
        cellW = panel.cellWidth.toDp()
        cellH = panel.keyBounds.height.toDp() * 1.15f
        val y = headroomPx + panel.keyBounds.top - cellH.toPx() - Gap.toPx()
        offset = IntOffset(panel.layout.left.roundToInt(), y.roundToInt())
    }
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, Motion.press()) }
    // Grow out of the key's side of the row.
    val originX = if (panel.layout.reversed) 1f else 0f
    Row(
        Modifier
            .offset { offset }
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(originX, 1f)
                alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                shadowElevation = 10.dp.toPx()
                shape = RoundedCornerShape(18.dp)
                clip = true
            }
            .background(ZenithColors.indicator),
    ) {
        val order = if (panel.layout.reversed) panel.options.indices.reversed() else panel.options.indices
        for (i in order) {
            val selected = i == panel.selected
            Box(
                Modifier
                    .size(cellW, cellH)
                    .padding(3.dp)
                    .background(if (selected) cs.secondary else Color.Transparent, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    panel.options[i],
                    color = if (selected) cs.onSecondary else ZenithColors.onIndicator,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
