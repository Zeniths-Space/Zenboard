package app.zenith.keyboard

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import android.view.inputmethod.EditorInfo
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp

private val KeyCorner = 10.dp

/** Pressed keys round toward a pill (radius is clamped to half the key height). */
private val PressedCorner = 24.dp

// LocalWindowInfo.containerSize (lint's suggestion) is the IME window, which is only as tall as the
// keyboard itself, so it cannot size the keyboard. The screen height from Configuration is what we need.
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun KeyboardView(
    state: KeyboardState,
    enterAction: Int?,
    systemBars: PaddingValues,
    onChangeKeyboard: () -> Unit,
    onKey: (Key) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(systemBars)
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Cap rows at 9% of screen height so landscape keeps most of the app visible.
        val rowHeight = minOf(56.dp, (LocalConfiguration.current.screenHeightDp * 0.09f).dp)
        keyboardRows(state.layer).forEach { row ->
            Row(Modifier.fillMaxWidth().height(rowHeight)) {
                val pad = (ROW_UNITS - row.sumOf { keyWeight(it, row).toDouble() }.toFloat()) / 2
                if (pad > 0f) Spacer(Modifier.weight(pad))
                row.forEach { key ->
                    KeyButton(key, state, enterAction, onKey, onChangeKeyboard, Modifier.weight(keyWeight(key, row)))
                }
                if (pad > 0f) Spacer(Modifier.weight(pad))
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: Key,
    state: KeyboardState,
    enterAction: Int?,
    onKey: (Key) -> Unit,
    onChangeKeyboard: () -> Unit,
    modifier: Modifier,
) {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val clickPressed by interaction.collectIsPressedAsState()
    var heldPressed by remember { mutableStateOf(false) }
    val pressed = clickPressed || heldPressed

    // Expressive press: the key springs toward a pill and dips slightly.
    val morph by animateFloatAsState(if (pressed) 1f else 0f, Motion.press(), label = "press")
    val shape = keyShape(morph.coerceIn(0f, 1f))
    val (container, content) = keyColors(key, state, pressed)
    val fill by animateColorAsState(container, tween(90), label = "fill")

    val description = keyDescription(key, state, enterAction)
    val press = {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onKey(key)
    }

    Box(
        modifier
            .fillMaxHeight()
            .padding(horizontal = 2.5.dp, vertical = 2.5.dp)
            .graphicsLayer {
                val s = 1f - 0.05f * morph
                scaleX = s
                scaleY = s
                this.shape = shape
                clip = true
            }
            .background(fill)
            .then(
                when {
                    key == Key.Backspace -> Modifier
                        .repeatWhileHeld(onPressedChange = { heldPressed = it }, action = press)
                        .semantics {
                            role = Role.Button
                            onClick { press(); true }
                        }
                    // Hold space to change keyboard (the system input-method picker).
                    key == Key.Space -> Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onLongClickLabel = stringResource(R.string.key_change_keyboard),
                        onLongClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onChangeKeyboard()
                        },
                        onClick = press,
                    )
                    else -> Modifier.clickable(interactionSource = interaction, indication = null, onClick = press)
                },
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val icon = keyIcon(key, state, enterAction)
        when {
            // The backspace glyph fills its whole 24-unit box, so it is drawn smaller to match the others.
            icon != null -> Icon(
                icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(if (key == Key.Backspace) 19.dp else 24.dp),
            )
            key == Key.Space -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    KeyIcons.Globe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(R.string.space_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            key is Key.Text -> Text(
                state.textFor(key),
                color = content,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            else -> Text(
                if (key == Key.ToSymbols) "?123" else "ABC",
                color = content,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun keyShape(morph: Float): Shape = RoundedCornerShape(lerp(KeyCorner, PressedCorner, morph))

/** Letters are cards; function keys sit a tone deeper; Enter and caps lock carry the secondary accent. */
@Composable
private fun keyColors(key: Key, state: KeyboardState, pressed: Boolean): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    val active = ZenithColors.indicator to ZenithColors.onIndicator
    return when (key) {
        Key.Enter -> cs.secondary to cs.onSecondary
        Key.Shift -> when (state.shift) {
            ShiftMode.LOCKED -> cs.secondary to cs.onSecondary
            ShiftMode.ONCE -> active
            ShiftMode.OFF -> if (pressed) active else ZenithColors.functionKey to cs.onSurfaceVariant
        }
        is Key.Text, Key.Space -> if (pressed) active else cs.surfaceContainerHigh to cs.onSurface
        else -> if (pressed) active else ZenithColors.functionKey to cs.onSurfaceVariant
    }
}

private fun keyIcon(key: Key, state: KeyboardState, enterAction: Int?): ImageVector? = when (key) {
    Key.Shift -> when (state.shift) {
        ShiftMode.OFF -> KeyIcons.ShiftOff
        ShiftMode.ONCE -> KeyIcons.ShiftOn
        ShiftMode.LOCKED -> KeyIcons.CapsLock
    }
    Key.Backspace -> KeyIcons.Backspace
    Key.Enter -> when (enterAction) {
        EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT -> KeyIcons.Next
        EditorInfo.IME_ACTION_SEARCH -> KeyIcons.Search
        EditorInfo.IME_ACTION_SEND -> KeyIcons.Send
        EditorInfo.IME_ACTION_PREVIOUS -> KeyIcons.Previous
        EditorInfo.IME_ACTION_DONE -> KeyIcons.Done
        else -> KeyIcons.Return
    }
    else -> null
}

/**
 * Fires once on press, then repeats after a short hold, like a hardware key.
 * rememberUpdatedState keeps the gesture alive when a keystroke recomposes the key mid-hold.
 */
@Composable
private fun Modifier.repeatWhileHeld(onPressedChange: (Boolean) -> Unit, action: () -> Unit): Modifier {
    val current by rememberUpdatedState(action)
    val pressedChange by rememberUpdatedState(onPressedChange)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown()
            pressedChange(true)
            try {
                current()
                var delayMs = 400L
                while (true) {
                    var released = false
                    withTimeoutOrNull(delayMs) {
                        waitForUpOrCancellation()
                        released = true
                    }
                    if (released) break
                    current()
                    delayMs = 50L
                }
            } finally {
                // Also runs when the keyboard hides mid-hold, so the key never stays "pressed".
                pressedChange(false)
            }
        }
    }
}

@Composable
private fun keyDescription(key: Key, state: KeyboardState, enterAction: Int?): String = when (key) {
    is Key.Text -> state.textFor(key)
    Key.Shift -> stringResource(
        when (state.shift) {
            ShiftMode.OFF -> R.string.key_shift
            ShiftMode.ONCE -> R.string.key_shift_on
            ShiftMode.LOCKED -> R.string.key_caps_lock
        },
    )
    Key.Backspace -> stringResource(R.string.key_delete)
    Key.Space -> stringResource(R.string.key_space)
    Key.ToSymbols -> stringResource(R.string.key_symbols)
    Key.ToLetters -> stringResource(R.string.key_letters)
    Key.Enter -> stringResource(
        when (enterAction) {
            EditorInfo.IME_ACTION_GO -> R.string.key_go
            EditorInfo.IME_ACTION_SEARCH -> R.string.key_search
            EditorInfo.IME_ACTION_SEND -> R.string.key_send
            EditorInfo.IME_ACTION_NEXT -> R.string.key_next
            EditorInfo.IME_ACTION_PREVIOUS -> R.string.key_previous
            EditorInfo.IME_ACTION_DONE -> R.string.key_done
            else -> R.string.key_enter
        },
    )
}
