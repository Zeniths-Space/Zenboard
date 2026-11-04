package app.zenith.zenboard

import android.view.inputmethod.EditorInfo

/** Pure keyboard logic. No Android framework calls, so it is unit-testable on the JVM. */

enum class Layer { LETTERS, SYMBOLS, MORE_SYMBOLS, EMOJI, CLIPBOARD }

enum class ShiftMode { OFF, ONCE, LOCKED }

sealed interface Key {
    data class Text(val value: String) : Key
    data object Shift : Key
    data object Backspace : Key
    data object Space : Key
    data object Enter : Key
    data object ToSymbols : Key
    /** The "=\<" key: the second symbols page. */
    data object ToMoreSymbols : Key
    data object ToLetters : Key
    data object Emoji : Key
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

/** Shifted letters are uppercased, except where the capital is not one character (ß -> "SS"). */
fun KeyboardState.textFor(key: Key.Text): String {
    if (layer != Layer.LETTERS || shift == ShiftMode.OFF) return key.value
    val upper = key.value.uppercase()
    return if (upper.length == key.value.length) upper else key.value
}

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

/** Word suggestions belong in ordinary text fields: not passwords, numbers, or fields that opt out. */
fun suggestionsAllowed(inputType: Int): Boolean {
    if (inputType and android.text.InputType.TYPE_MASK_CLASS != android.text.InputType.TYPE_CLASS_TEXT) return false
    if (inputType and android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0) return false
    return when (inputType and android.text.InputType.TYPE_MASK_VARIATION) {
        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD,
        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> false
        else -> true
    }
}

/** Glide typing types whole words: any text field except passwords. */
fun glideAllowed(inputType: Int): Boolean {
    if (inputType and android.text.InputType.TYPE_MASK_CLASS != android.text.InputType.TYPE_CLASS_TEXT) return false
    return when (inputType and android.text.InputType.TYPE_MASK_VARIATION) {
        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD,
        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> false
        else -> true
    }
}

/** Autocorrect also stays off where text is not prose: email addresses, URLs and filters. */
fun autocorrectAllowed(inputType: Int): Boolean = suggestionsAllowed(inputType) &&
    when (inputType and android.text.InputType.TYPE_MASK_VARIATION) {
        android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        android.text.InputType.TYPE_TEXT_VARIATION_URI,
        android.text.InputType.TYPE_TEXT_VARIATION_FILTER -> false
        else -> true
    }

/** Punctuation that ends a word and triggers autocorrect, like space does. */
fun endsWord(text: String): Boolean = text.length == 1 && text[0] in ".,?!:;"

/** Letters always continue a word; an apostrophe only inside one ("don't"). */
fun isWordChar(text: String, composing: CharSequence): Boolean =
    text.length == 1 && (text[0].isLetter() || (text[0] == '\'' && composing.isNotEmpty()))

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
        Key.Emoji,
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
            listOf(Key.ToMoreSymbols) + row("*\"':;!?") + Key.Backspace,
            bottom,
        )
        Layer.MORE_SYMBOLS -> listOf(
            row("~`|•√π÷×§∆"),
            row("£¢€¥^°={}\\"),
            listOf(Key.ToSymbols) + row("%©®™✓[]") + Key.Backspace,
            bottom,
        )
        // The emoji grid or clipboard fills the space of the upper rows; only the bottom row has keys.
        Layer.EMOJI, Layer.CLIPBOARD -> listOf(listOf(Key.ToLetters, Key.Backspace))
    }
}

/** Space absorbs whatever width the other bottom-row keys leave. */
fun keyWeight(key: Key, row: List<Key>): Float = when (key) {
    is Key.Text, Key.Emoji -> 1f
    Key.Shift, Key.Backspace, Key.Enter, Key.ToSymbols, Key.ToMoreSymbols, Key.ToLetters -> 1.5f
    Key.Space -> ROW_UNITS - row.filter { it != Key.Space }.sumOf { keyWeight(it, row).toDouble() }.toFloat()
}

/**
 * Long-press alternates, first option first. Top-row letters lead with their digit (shown as a hint).
 * shortcut: one English/Western-European set, upgrade to per-language sets with multilingual layouts.
 */
private val ALTERNATES: Map<String, List<String>> = mapOf(
    "q" to listOf("1"),
    "w" to listOf("2"),
    "e" to listOf("3", "è", "é", "ê", "ë", "ē", "ę"),
    "r" to listOf("4"),
    "t" to listOf("5", "þ"),
    "y" to listOf("6", "ý", "ÿ"),
    "u" to listOf("7", "ù", "ú", "û", "ü", "ū"),
    "i" to listOf("8", "ì", "í", "î", "ï", "ī"),
    "o" to listOf("9", "ò", "ó", "ô", "ö", "õ", "ø", "œ", "ō"),
    "p" to listOf("0"),
    "a" to listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā"),
    "s" to listOf("ß", "ś", "š"),
    "d" to listOf("ð"),
    "g" to listOf("ğ"),
    "l" to listOf("ł"),
    "z" to listOf("ž", "ź", "ż"),
    "c" to listOf("ç", "ć", "č"),
    "n" to listOf("ñ", "ń"),
    "." to listOf("?", "!", "'", "\"", ":", ";", "…"),
    "1" to listOf("¹", "½", "⅓", "¼", "⅛"),
    "2" to listOf("²", "⅔"),
    "3" to listOf("³", "¾", "⅜"),
    "4" to listOf("⁴"),
    "5" to listOf("⅝"),
    "7" to listOf("⅞"),
    "0" to listOf("°", "ⁿ", "∅"),
    "$" to listOf("€", "£", "¥", "¢", "₹", "₩"),
    "-" to listOf("–", "—", "·"),
    "+" to listOf("±"),
    "(" to listOf("<", "[", "{"),
    ")" to listOf(">", "]", "}"),
    "/" to listOf("\\", "|"),
    "&" to listOf("§", "¶"),
    "#" to listOf("№"),
    "%" to listOf("‰", "℅"),
    "*" to listOf("†", "‡", "★"),
    "\"" to listOf("“", "”", "„", "«", "»"),
    "'" to listOf("‘", "’", "‚", "‹", "›"),
    "?" to listOf("¿"),
    "!" to listOf("¡"),
)

fun alternatesFor(key: Key): List<String> = if (key is Key.Text) ALTERNATES[key.value].orEmpty() else emptyList()

/** Small corner hint: the digit on top-row letters. */
fun hintFor(key: Key, layer: Layer): String? =
    alternatesFor(key).firstOrNull()?.takeIf { layer == Layer.LETTERS && it.single().isDigit() }

/** Where the alternates row sits: starts at the key, or ends at it (reversed) when it would overflow. */
data class AltLayout(val left: Float, val reversed: Boolean)

fun altLayout(keyLeft: Float, keyRight: Float, cellWidth: Float, count: Int, maxWidth: Float): AltLayout {
    val width = cellWidth * count
    return if (keyLeft + width <= maxWidth) {
        AltLayout(keyLeft, reversed = false)
    } else {
        AltLayout((keyRight - width).coerceAtLeast(0f), reversed = true)
    }
}

/** Option under x. In a reversed row the first option is the rightmost cell, above the key. */
fun altIndexAt(x: Float, layout: AltLayout, cellWidth: Float, count: Int): Int {
    val cell = ((x - layout.left) / cellWidth).toInt().coerceIn(0, count - 1)
    return if (layout.reversed) count - 1 - cell else cell
}

/** Characters a word delete removes before the cursor: trailing spaces, then back to the previous space. */
fun wordDeleteLength(before: CharSequence): Int {
    var i = before.length
    while (i > 0 && before[i - 1].isWhitespace()) i--
    while (i > 0 && !before[i - 1].isWhitespace()) i--
    // The text was cut off mid-emoji: keep the half whose other half we cannot see.
    if (i == 0 && before.isNotEmpty() && before[0].isLowSurrogate()) i = 1
    return before.length - i
}

/**
 * Characters the cursor moves for [steps] characters through [text] (the text before the cursor
 * when going back, after it when going forward), never splitting an emoji's surrogate pair.
 */
fun cursorDelta(text: CharSequence, steps: Int, forward: Boolean): Int {
    var moved = 0
    repeat(steps) {
        if (moved == text.length) return moved
        moved++
        if (moved < text.length) {
            val (a, b) = if (forward) text[moved - 1] to text[moved] else text[text.length - moved - 1] to text[text.length - moved]
            if (a.isHighSurrogate() && b.isLowSurrogate()) moved++
        }
    }
    return moved
}
