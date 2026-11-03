package app.zenith.keyboard

import android.view.inputmethod.EditorInfo

/** Pure keyboard logic. No Android framework calls, so it is unit-testable on the JVM. */

enum class Layer { LETTERS, SYMBOLS }

enum class ShiftMode { OFF, ONCE, LOCKED }

sealed interface Key {
    data class Text(val value: String) : Key
    data object Shift : Key
    data object Backspace : Key
    data object Space : Key
    data object Enter : Key
    data object ToSymbols : Key
    data object ToLetters : Key
}

const val DOUBLE_TAP_MS = 400L

/** Width of a full row, in key units. Shorter rows are centered. */
const val ROW_UNITS = 10f

data class KeyboardState(
    val layer: Layer = Layer.LETTERS,
    val shift: ShiftMode = ShiftMode.OFF,
    /** Time of the shift tap that turned shift on, used for double-tap caps lock. */
    val lastShiftTap: Long? = null,
)

/** Tap: off -> once. Second tap within [DOUBLE_TAP_MS]: caps lock. Any other tap: off. */
fun KeyboardState.tapShift(now: Long): KeyboardState = when (shift) {
    ShiftMode.OFF -> copy(shift = ShiftMode.ONCE, lastShiftTap = now)
    ShiftMode.ONCE -> {
        val doubleTap = lastShiftTap != null && now - lastShiftTap <= DOUBLE_TAP_MS
        copy(shift = if (doubleTap) ShiftMode.LOCKED else ShiftMode.OFF, lastShiftTap = null)
    }
    ShiftMode.LOCKED -> copy(shift = ShiftMode.OFF, lastShiftTap = null)
}

fun KeyboardState.textFor(key: Key.Text): String =
    if (layer == Layer.LETTERS && shift != ShiftMode.OFF) key.value.uppercase() else key.value

/** One-shot shift ends after a character is typed. */
fun KeyboardState.afterText(): KeyboardState =
    if (shift == ShiftMode.ONCE) copy(shift = ShiftMode.OFF, lastShiftTap = null) else this

/** Apply the editor's auto-capitalization at the cursor. Caps lock is never overridden. */
fun KeyboardState.withAutoCaps(capsOn: Boolean): KeyboardState = when {
    shift == ShiftMode.LOCKED -> this
    capsOn -> copy(shift = ShiftMode.ONCE, lastShiftTap = null)
    else -> copy(shift = ShiftMode.OFF, lastShiftTap = null)
}

/** Number, phone and date fields open on the symbols layer, which has the digit row. */
fun initialLayer(inputType: Int): Layer = when (inputType and android.text.InputType.TYPE_MASK_CLASS) {
    android.text.InputType.TYPE_CLASS_NUMBER,
    android.text.InputType.TYPE_CLASS_PHONE,
    android.text.InputType.TYPE_CLASS_DATETIME -> Layer.SYMBOLS
    else -> Layer.LETTERS
}

/** Editor action for the Enter key, or null when Enter should send a plain Enter key event. */
fun enterAction(imeOptions: Int): Int? {
    if (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return null
    val action = imeOptions and EditorInfo.IME_MASK_ACTION
    return action.takeIf { it != EditorInfo.IME_ACTION_NONE && it != EditorInfo.IME_ACTION_UNSPECIFIED }
}

private fun row(chars: String): List<Key> = chars.map { Key.Text(it.toString()) }

fun keyboardRows(layer: Layer): List<List<Key>> {
    val bottom = listOf(
        if (layer == Layer.LETTERS) Key.ToSymbols else Key.ToLetters,
        Key.Text(","),
        Key.Space,
        Key.Text("."),
        Key.Enter,
    )
    return when (layer) {
        Layer.LETTERS -> listOf(
            row("qwertyuiop"),
            row("asdfghjkl"),
            listOf(Key.Shift) + row("zxcvbnm") + Key.Backspace,
            bottom,
        )
        Layer.SYMBOLS -> listOf(
            row("1234567890"),
            row("@#\$_&-+()/"),
            row("%*\"':;!?") + Key.Backspace,
            bottom,
        )
    }
}

/** Space absorbs whatever width the other bottom-row keys leave. */
fun keyWeight(key: Key, row: List<Key>): Float = when (key) {
    is Key.Text -> 1f
    Key.Shift, Key.Backspace, Key.Enter, Key.ToSymbols, Key.ToLetters -> 1.5f
    Key.Space -> ROW_UNITS - row.filter { it != Key.Space }.sumOf { keyWeight(it, row).toDouble() }.toFloat()
}

