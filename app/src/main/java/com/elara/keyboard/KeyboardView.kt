package com.elara.keyboard

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.inputmethod.EditorInfo
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val KeyCorner = 10.dp

/** Pressed keys round a little more: a hint of the press, not a pill. */
private val PressedCorner = 12.dp

// LocalWindowInfo.containerSize (lint's suggestion) is the IME window, which is only as tall as the
// keyboard itself, so it cannot size the keyboard. The screen height from Configuration is what we need.
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun KeyboardView(
    state: KeyboardState,
    enterAction: Int?,
    systemBars: PaddingValues,
    showPreviews: Boolean,
    keyPopup: Boolean,
    spaceSwitch: Boolean,
    spaceLabel: Boolean,
    spaceSlide: Boolean,
    backspaceSlide: Boolean,
    backspaceDoubleTap: Boolean,
    /** Glide typing on for this field and layer. */
    glideEnabled: Boolean,
    onGlide: (path: List<Offset>, geo: KeyGeometry) -> Unit,
    emoji: List<EmojiCategory>?,
    recentEmoji: List<String>,
    emojiTones: EmojiTones,
    onEmoji: (String) -> Unit,
    /** Non-null while searching emoji: the query typed so far. */
    emojiQuery: String?,
    emojiIndex: List<EmojiInfo>?,
    onEmojiSearch: () -> Unit,
    onRemoveRecentEmoji: (String) -> Unit,
    onClearRecentEmoji: () -> Unit,
    onCloseEmojiSearch: () -> Unit,
    clips: List<Clip>,
    clipHistoryOn: Boolean,
    freshClip: String?,
    onPasteClip: (String) -> Unit,
    onPinClip: (Clip) -> Unit,
    onDeleteClip: (Clip) -> Unit,
    stripShown: Boolean,
    suggestions: Suggestions,
    onPick: (String) -> Unit,
    onToolbar: (ToolbarAction) -> Unit,
    onChangeKeyboard: () -> Unit,
    onDeleteWord: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    keyHeight: Float,
    /** Multiplies the size of every key label. */
    keyTextSize: Float,
    oneHanded: OneHanded,
    onOneHanded: (OneHanded) -> Unit,
    toolbarShown: Boolean,
    toolbarButtons: List<ToolbarAction>,
    feedback: KeyFeedback,
    onKey: (Key) -> Unit,
) = CompositionLocalProvider(LocalKeyFeedback provides feedback) {
    val preview = remember { PreviewState() }
    preview.bubbles = keyPopup
    // Key labels are drawn, not laid out: a layer switch used to re-measure ~40 texts (most of a dropped
    // frame). One shared cache holds every layer's labels, so switching back and forth reuses them.
    val keyText = rememberTextMeasurer(cacheSize = KEY_TEXT_CACHE)
    val glide = remember { GlideState() }
    val glideCoords = remember { object { var c: LayoutCoordinates? = null } }
    val trailColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    var size by remember { mutableStateOf(IntSize.Zero) }
    if (showPreviews && size != IntSize.Zero) KeyPreviewOverlay(preview, size)
    Row(
        Modifier
            .fillMaxWidth()
            .onSizeChanged {
                size = it
                preview.keyboardWidth = it.width.toFloat()
            }
            .background(MaterialTheme.colorScheme.surface)
            .padding(systemBars)
            // Only the one-handed strip needs the keyboard's height. The intrinsic pass measures every
            // key's text twice, which was most of the frame on a layer switch, so skip it otherwise.
            .then(if (oneHanded != OneHanded.OFF) Modifier.height(IntrinsicSize.Min) else Modifier),
    ) {
        // One-handed: the keyboard keeps ONE_HANDED_WIDTH on its side; the strip beside it swaps sides or exits.
        if (oneHanded == OneHanded.RIGHT) OneHandedStrip(oneHanded, onOneHanded, Modifier.weight(1f - ONE_HANDED_WIDTH))
        Column(
            Modifier
                .weight(if (oneHanded == OneHanded.OFF) 1f else ONE_HANDED_WIDTH)
                .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)
                .onGloballyPositioned { glideCoords.c = it }
                .glideGestures(
                    glide,
                    enabled = glideEnabled && state.layer == Layer.LETTERS,
                    panelOpen = { preview.panel != null },
                    toRoot = { p -> glideCoords.c?.takeIf { it.isAttached }?.localToRoot(p) ?: p },
                    onGlide = onGlide,
                )
                // The trail is read only here, so a glide redraws the keyboard without recomposing it.
                .drawWithContent {
                    drawContent()
                    val trail = glide.trail
                    for (i in 1 until trail.size) {
                        drawLine(trailColor, trail[i - 1], trail[i], strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                    }
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val topRow = when {
                emojiQuery != null -> TopRow.SEARCH
                stripShown && (suggestions.primary != null || suggestions.others.isNotEmpty()) -> TopRow.SUGGESTIONS
                !toolbarShown -> if (stripShown) TopRow.IDLE else TopRow.NONE
                else -> TopRow.TOOLBAR
            }
            // A quick fade between toolbar, suggestions and search; the rows are one height, so nothing moves.
            Crossfade(topRow, animationSpec = Motion.effects(150), label = "top row") { row ->
                when (row) {
                    TopRow.SEARCH -> if (emojiQuery != null) EmojiSearchBar(emojiQuery, emojiIndex, recentEmoji, emojiTones, onCloseEmojiSearch, onEmoji)
                    TopRow.SUGGESTIONS -> SuggestionStrip(suggestions, onPick)
                    // No toolbar: with suggestions on, the empty strip keeps its row. A row that came and
                    // went with each word would resize the keyboard while typing, and fast taps land on
                    // the wrong keys during the resize (tested: "hello there now" came out garbled).
                    TopRow.IDLE -> IdleStrip()
                    TopRow.NONE -> {}
                    // Shown with suggestions on or off: editing, clipboard and settings are one tap away.
                    TopRow.TOOLBAR -> Toolbar(
                        onToolbar,
                        freshClip,
                        onPasteFresh = { freshClip?.let(onPasteClip) },
                        clipboardOpen = state.layer == Layer.CLIPBOARD,
                        oneHandedOn = oneHanded != OneHanded.OFF,
                        buttons = toolbarButtons,
                    )
                }
            }
            // Layers swap in one frame with no fade, as Gboard does: a fade only delays the keys.
            // Cap rows at 9% of screen height so landscape keeps most of the app visible.
            val rowHeight = minOf(56.dp, (LocalConfiguration.current.screenHeightDp * 0.09f).dp) * keyHeight
            // Emoji and clipboard keep the same five-row height as letters and both symbol pages;
            // their compact ABC/delete row leaves more room inside the panel.
            val panelLayer = state.layer == Layer.EMOJI || state.layer == Layer.CLIPBOARD
            val emojiRowHeight = rowHeight * 0.7f
            val panelHeight = rowHeight * 5 - emojiRowHeight + 6.dp
            // Tab 0 (recents) each time the panel opens; it lives here so the bottom row can show "Clear all".
            var emojiTab by remember(state.layer == Layer.EMOJI) { mutableIntStateOf(0) }
            if (state.layer == Layer.EMOJI) {
                // Fills the panel area while the compact bottom row preserves the regular total height.
                EmojiPanel(
                    emoji, recentEmoji, emojiTones, emojiTab, { emojiTab = it },
                    height = panelHeight, onSearch = onEmojiSearch,
                    onRemoveRecent = onRemoveRecentEmoji, onEmoji = onEmoji,
                )
            }
            if (state.layer == Layer.CLIPBOARD) {
                ClipboardPanel(
                    clips, clipHistoryOn, height = panelHeight,
                    onPaste = onPasteClip, onPin = onPinClip, onDelete = onDeleteClip,
                )
            }
            keyboardRows(state.layer).forEach { row ->
                Row(Modifier.fillMaxWidth().height(if (panelLayer) emojiRowHeight else rowHeight)) {
                    val pad = (ROW_UNITS - row.sumOf { keyWeight(it, row).toDouble() }.toFloat()) / 2
                    // Short rows are centred, except the panel row: ABC at the left edge, delete at the right.
                    val spread = panelLayer
                    if (pad > 0f && !spread) Spacer(Modifier.weight(pad))
                    row.forEachIndexed { i, key ->
                        if (spread && i == row.lastIndex && pad > 0f) {
                            Box(Modifier.weight(pad * 2).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                when (state.layer) {
                                    Layer.EMOJI -> if (emojiTab == 0 && emoji != null && recentEmoji.isNotEmpty()) ClearRecents(onClearRecentEmoji)
                                    else -> {}
                                }
                            }
                        }
                        // Identity by slot: a layer switch updates the keys in place instead of building ~30
                        // new ones, which cost a dropped frame per switch. Keys in one slot share colours, so
                        // a press animation carried over (?123 into ABC) looks right.
                        KeyButton(
                            key, state, enterAction, preview, onKey, onChangeKeyboard.takeIf { spaceSwitch }, onDeleteWord,
                            backspaceSlide, backspaceDoubleTap, onMoveCursor.takeIf { spaceSlide }, spaceLabel, glide, keyText, keyTextSize, Modifier.weight(keyWeight(key, row)),
                        )
                    }
                    if (pad > 0f && !spread) Spacer(Modifier.weight(pad))
                }
            }
        }
        if (oneHanded == OneHanded.LEFT) OneHandedStrip(oneHanded, onOneHanded, Modifier.weight(1f - ONE_HANDED_WIDTH))
    }
}

/** Beside the one-handed keyboard: move it to the other side, or back to full width. */
@Composable
private fun OneHandedStrip(side: OneHanded, onChange: (OneHanded) -> Unit, modifier: Modifier) {
    val feedback = LocalKeyFeedback.current
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf(
            Triple(KeyIcons.SwapSides, R.string.one_handed_swap) { onChange(if (side == OneHanded.LEFT) OneHanded.RIGHT else OneHanded.LEFT) },
            Triple(KeyIcons.FullWidth, R.string.one_handed_exit) { onChange(OneHanded.OFF) },
        ).forEach { (icon, label, action) ->
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHigh)
                    .clickable(role = Role.Button) {
                        feedback.press()
                        action()
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(icon, stringResource(label), tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
private fun KeyButton(
    key: Key,
    state: KeyboardState,
    enterAction: Int?,
    preview: PreviewState,
    onKey: (Key) -> Unit,
    /** Null when hold-space switching is off. */
    onChangeKeyboard: (() -> Unit)?,
    onDeleteWord: () -> Unit,
    /** Slide left on backspace to delete words. */
    backspaceSlide: Boolean,
    /** A second backspace tap in quick succession deletes a word. */
    backspaceDoubleTap: Boolean,
    /** Null when sliding along space to move the cursor is off. */
    onMoveCursor: ((Int) -> Unit)?,
    spaceLabel: Boolean,
    glide: GlideState,
    keyText: TextMeasurer,
    textScale: Float,
    modifier: Modifier,
) {
    val feedback = LocalKeyFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val clickPressed by interaction.collectIsPressedAsState()
    var heldPressed by remember { mutableStateOf(false) }
    // A glide that started on this key: no press look, no preview bubble.
    val pressed = (clickPressed || heldPressed) && !glide.active

    // Expressive press: the key's corners round a little and it dips.
    // Both are read only in the draw phase, so each animation frame redraws the key without recomposing it.
    val morph = animateFloatAsState(if (pressed) 1f else 0f, Motion.press(), label = "press")
    val (container, content) = keyColors(key, state, pressed)
    val fill = animateColorAsState(container, tween(90), label = "fill")

    // Character keys show a preview bubble while pressed. Bounds are kept out of state: layout
    // updates them without recomposing.
    // inner: the key's own coordinates inside the press-scale layer, for mapping finger x to the root.
    val bounds = remember { object { var rect = Rect.Zero; var inner: LayoutCoordinates? = null } }
    if (key is Key.Text) {
        val label = state.textFor(key)
        // This key's own bubble, matched by identity: the key can move while held (the suggestion
        // row appearing above shifts it), so comparing positions would leave the bubble stuck.
        val shown = remember { object { var press: KeyPress? = null } }
        LaunchedEffect(pressed) {
            if (pressed) {
                if (preview.bubbles) preview.current = KeyPress(label, bounds.rect).also { shown.press = it }
            } else if (preview.current != null && preview.current === shown.press) {
                preview.current = null
            }
        }
    }

    val description = keyDescription(key, state, enterAction)
    val deleteWordLabel = stringResource(R.string.key_delete_word)
    val changeKeyboardLabel = stringResource(R.string.key_change_keyboard)
    val changeKeyboard: () -> Unit = {
        feedback.haptic(HapticFeedbackConstants.LONG_PRESS)
        onChangeKeyboard?.invoke()
    }
    val press = {
        feedback.press(key)
        onKey(key)
    }

    // Long-press alternates: open above the key, follow the finger, type the pick on release.
    val alternates = alternatesFor(key)
    val longPressMs = LocalViewConfiguration.current.longPressTimeoutMillis
    val doubleTapMs = LocalViewConfiguration.current.doubleTapTimeoutMillis
    // Backspace double tap: the first tap deletes a character at once (no wait), a quick second one
    // deletes the word, and so does each further quick tap.
    val lastBackspaceTap = remember { longArrayOf(0L) }
    val backspaceTap: () -> Unit = {
        val now = SystemClock.uptimeMillis()
        if (backspaceDoubleTap && now - lastBackspaceTap[0] <= doubleTapMs) {
            feedback.press(key)
            onDeleteWord()
        } else {
            press()
        }
        lastBackspaceTap[0] = now
    }
    val openPanel: () -> Unit = {
        val b = bounds.rect
        val options = alternates.map { state.textFor(Key.Text(it)) }
        val layout = altLayout(b.left, b.right, b.width, options.size, preview.keyboardWidth)
        preview.current = null
        preview.panel = AltPanel(options, layout, b.width, b, altIndexAt(b.center.x, layout, b.width, options.size))
        feedback.haptic(HapticFeedbackConstants.LONG_PRESS)
    }
    val movePanel = { localX: Float ->
        val panel = preview.panel
        val inner = bounds.inner
        if (panel != null && inner != null && inner.isAttached) {
            val rootX = inner.localToRoot(Offset(localX, 0f)).x
            val index = altIndexAt(rootX, panel.layout, panel.cellWidth, panel.options.size)
            if (index != panel.selected) {
                panel.selected = index
                feedback.haptic(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }
    // What the key shows: an icon or a label drawn over its fill.
    val icon = keyIcon(key, state, enterAction)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val baseStyle = LocalTextStyle.current
    val spaceText = stringResource(R.string.space_label)
    val label: String? = when {
        icon != null -> null
        key == Key.Space -> spaceText.takeIf { spaceLabel }
        key is Key.Text -> state.textFor(key)
        key == Key.ToSymbols && state.layer == Layer.MORE_SYMBOLS -> "2/2"
        key == Key.ToSymbols -> "?123"
        key == Key.ToMoreSymbols -> if (state.layer == Layer.SYMBOLS) "1/2" else "=\\<"
        else -> "ABC"
    }
    val labelStyle = when {
        key == Key.Space -> MaterialTheme.typography.labelMedium
        key is Key.Text -> baseStyle.merge(LetterStyle)
        else -> MaterialTheme.typography.labelLarge
    }.let { it.copy(fontSize = it.fontSize * textScale) }
    val labelColor = if (key == Key.Space) muted else content
    val hint = if (state.layer == Layer.LETTERS && key is Key.Text && key.value.length == 1 && key.value[0] in 'a'..'z') {
        alternatesFor(key).firstOrNull()
    } else null
    val hintStyle = TextStyle(fontSize = 10.sp * textScale, fontWeight = FontWeight.Normal)
    val closePanel = { commit: Boolean ->
        val panel = preview.panel
        preview.panel = null
        if (commit && panel != null) {
            feedback.press()
            onKey(Key.Text(panel.options[panel.selected]))
        }
    }
    Box(
        modifier
            .fillMaxHeight()
            // Gestures cover the whole cell, gaps included, and ignore the press scale: fast taps
            // that land between keys or near an edge still count.
            .onGloballyPositioned { bounds.inner = it }
            .then(
                when {
                    key == Key.Backspace -> Modifier
                        .backspaceGestures(
                            slide = backspaceSlide,
                            onPressedChange = { heldPressed = it },
                            onTap = backspaceTap,
                            onDelete = press,
                            onDeleteWord = {
                                feedback.haptic(HapticFeedbackConstants.CLOCK_TICK)
                                onDeleteWord()
                            },
                        )
                        .semantics {
                            role = Role.Button
                            onClick { press(); true }
                            customActions = listOf(CustomAccessibilityAction(deleteWordLabel) { onDeleteWord(); true })
                        }
                    // Every character key, so a tap whose finger drifts off the key still types.
                    key is Key.Text -> Modifier
                        .longPressAlternates(
                            longPressMs = longPressMs,
                            isGliding = { glide.active },
                            hasAlternates = alternates.isNotEmpty(),
                            onPressedChange = { heldPressed = it },
                            onTap = press,
                            onOpen = openPanel,
                            onMove = movePanel,
                            onEnd = closePanel,
                        )
                        .semantics {
                            role = Role.Button
                            onClick { press(); true }
                            // Screen readers get each alternate as an action instead of the drag gesture.
                            customActions = alternates.map { alt ->
                                CustomAccessibilityAction(alt) { onKey(Key.Text(alt)); true }
                            }
                        }
                    // Hold space to change keyboard (the system input-method picker) and slide to move the cursor, each if enabled.
                    key == Key.Space -> Modifier
                        .spaceGestures(
                            // Off: no timeout, so a long hold is still a tap or a slide.
                            longPressMs = if (onChangeKeyboard != null) SPACE_SWITCH_HOLD_MS else Long.MAX_VALUE,
                            onPressedChange = { heldPressed = it },
                            onTap = press,
                            onLongPress = changeKeyboard,
                            slide = onMoveCursor != null,
                            onMove = { steps ->
                                feedback.haptic(HapticFeedbackConstants.CLOCK_TICK)
                                onMoveCursor?.invoke(steps)
                            },
                        )
                        .semantics {
                            role = Role.Button
                            onClick { press(); true }
                            if (onChangeKeyboard != null) onLongClick(changeKeyboardLabel) { changeKeyboard(); true }
                        }
                    // Layer keys switch on touch down, as Gboard does. The key landing in this slot (?123 becomes
                    // ABC) keeps the gesture, so it shows pressed until the finger lifts.
                    key == Key.ToSymbols || key == Key.ToMoreSymbols || key == Key.ToLetters -> Modifier
                        .switchOnDown(onPressedChange = { heldPressed = it }, onDown = press)
                        .semantics {
                            role = Role.Button
                            onClick { press(); true }
                        }
                    else -> Modifier.clickable(interactionSource = interaction, indication = null, onClick = press)
                },
            )
            .semantics { contentDescription = description }
            .padding(horizontal = 2.5.dp, vertical = 2.5.dp)
            .onGloballyPositioned {
                bounds.rect = it.boundsInRoot()
                val c = (key as? Key.Text)?.value?.singleOrNull()
                if (state.layer == Layer.LETTERS && c != null && c in 'a'..'z') glide.keys[c] = bounds.rect
            }
            .graphicsLayer {
                val m = morph.value
                val s = 1f - 0.08f * m
                scaleX = s
                scaleY = s
                shape = keyShape(m.coerceIn(0f, 1f))
                clip = true
            }
            .drawWithCache {
                val main = label?.let { keyText.measure(it, labelStyle, softWrap = false, maxLines = 1) }
                val hintText = hint?.let { keyText.measure(it, hintStyle, softWrap = false, maxLines = 1) }
                onDrawBehind {
                    drawRect(fill.value)
                    main?.let {
                        drawText(it, labelColor, Offset((size.width - it.size.width) / 2, (size.height - it.size.height) / 2))
                    }
                    hintText?.let {
                        drawText(it, muted.copy(alpha = 0.78f), Offset(size.width - it.size.width - 5.dp.toPx(), 3.dp.toPx()))
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // The backspace glyph fills its whole 24-unit box, so it is drawn smaller to match the others.
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(if (key == Key.Backspace) 19.dp else 24.dp))
    }
}

/** Merged over the theme's text style, as Text(fontSize, fontWeight) did. */
private val LetterStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium)
private const val KEY_TEXT_CACHE = 160

private fun keyShape(morph: Float): Shape = RoundedCornerShape(lerp(KeyCorner, PressedCorner, morph))

/** Letters are cards; function keys sit a tone deeper; Enter carries the secondary accent, active shift only in its icon. */
@Composable
private fun keyColors(key: Key, state: KeyboardState, pressed: Boolean): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    val active = ZenithColors.pressedKey to ZenithColors.onIndicator
    return when (key) {
        Key.Enter -> cs.secondary to cs.onSecondary
        // Shift state shows in the icon and its colour only; the key background stays a function key.
        Key.Shift -> when {
            pressed -> active
            state.shift == ShiftMode.OFF -> ZenithColors.functionKey to cs.onSurfaceVariant
            else -> ZenithColors.functionKey to cs.secondary
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
    Key.Emoji -> KeyIcons.Emoji
    Key.Enter -> when (enterAction) {
        EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT -> KeyIcons.Next
        EditorInfo.IME_ACTION_SEARCH -> KeyIcons.Search
        EditorInfo.IME_ACTION_SEND -> KeyIcons.Send
        EditorInfo.IME_ACTION_PREVIOUS -> KeyIcons.Previous
        EditorInfo.IME_ACTION_DONE -> KeyIcons.Done
        else -> KeyIcons.Return
    }
    is Key.Text -> if (key.value == "♥") KeyIcons.Heart else null
    else -> null
}

/**
 * Tap types the key on release, wherever the finger lifts. Holding past [longPressMs] opens the
 * alternates (when [hasAlternates]); the finger then slides to pick and release types the pick.
 * Cancellation (keyboard hidden mid-hold) closes the panel.
 */
@Composable
private fun Modifier.longPressAlternates(
    longPressMs: Long,
    isGliding: () -> Boolean,
    hasAlternates: Boolean,
    onPressedChange: (Boolean) -> Unit,
    onTap: () -> Unit,
    onOpen: () -> Unit,
    onMove: (Float) -> Unit,
    onEnd: (commit: Boolean) -> Unit,
): Modifier {
    val pressedChange by rememberUpdatedState(onPressedChange)
    val tap by rememberUpdatedState(onTap)
    val open by rememberUpdatedState(onOpen)
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    // Keyed on what the gesture reads directly: a layer switch can hand this slot a key with other alternates.
    return pointerInput(hasAlternates, longPressMs) {
        awaitEachGesture {
            val down = awaitFirstDown()
            pressedChange(true)
            var panelOpen = false
            var committed = false
            try {
                var finished = false
                var up: PointerInputChange? = null
                withTimeoutOrNull(if (hasAlternates) longPressMs else Long.MAX_VALUE) {
                    up = awaitRelease(down.id)
                    finished = true
                }
                if (finished) {
                    // Released before the long press: a normal tap (null: cancelled, or it became a glide).
                    if (up != null) tap()
                    return@awaitEachGesture
                }
                // Still down at the long-press time because it is gliding: the glide owns this touch.
                if (isGliding()) {
                    awaitRelease(down.id)
                    return@awaitEachGesture
                }
                open()
                panelOpen = true
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (change.changedToUp()) {
                        committed = true
                        break
                    }
                    change.consume()
                    move(change.position.x)
                }
            } finally {
                if (panelOpen) end(committed)
                pressedChange(false)
            }
        }
    }
}

/**
 * Waits for pointer [id] to lift, even outside the key: fast typists' fingers drift past the edge
 * before release. Null when the pointer is gone without lifting, or its release was consumed (a
 * glide took over the touch).
 */
private suspend fun AwaitPointerEventScope.awaitRelease(id: PointerId): PointerInputChange? {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == id } ?: return null
        if (change.changedToUpIgnoreConsumed()) return change.takeUnless { it.isConsumed }
    }
}

/**
 * Tap ([onTap]) deletes on release. Holding repeats like a hardware key. With [slide], sliding left before the
 * repeat starts deletes a word, and one more for each further [WORD_STEP] of slide.
 * rememberUpdatedState keeps the gesture alive when a keystroke recomposes the key mid-hold.
 */
@Composable
private fun Modifier.backspaceGestures(
    slide: Boolean,
    onPressedChange: (Boolean) -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onDeleteWord: () -> Unit,
): Modifier {
    val delete by rememberUpdatedState(onDelete)
    val tap by rememberUpdatedState(onTap)
    val deleteWord by rememberUpdatedState(onDeleteWord)
    val pressedChange by rememberUpdatedState(onPressedChange)
    // Keyed on the setting: it can change while the keyboard is up.
    return pointerInput(slide) {
        awaitEachGesture {
            val down = awaitFirstDown()
            pressedChange(true)
            try {
                var swiping = false
                // true: released (a tap); false: slid off or started a swipe; null: held until the repeat.
                val tapped = withTimeoutOrNull(REPEAT_START_MS) {
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                        if (change.changedToUp()) return@withTimeoutOrNull true
                        if (slide && down.position.x - change.position.x > viewConfiguration.touchSlop) {
                            change.consume()
                            swiping = true
                            return@withTimeoutOrNull false
                        }
                    }
                    @Suppress("UNREACHABLE_CODE") null
                }
                when {
                    tapped == true -> tap()
                    swiping -> {
                        deleteWord()
                        val step = WORD_STEP.toPx()
                        var next = down.position.x - viewConfiguration.touchSlop - step
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            // The release position counts too, so the last stretch of slide is not lost.
                            while (change.position.x <= next) {
                                deleteWord()
                                next -= step
                            }
                            if (change.changedToUp()) break
                            change.consume()
                        }
                    }
                    tapped == null -> {
                        delete()
                        while (true) {
                            var released = false
                            withTimeoutOrNull(REPEAT_MS) {
                                waitForUpOrCancellation()
                                released = true
                            }
                            if (released) break
                            delete()
                        }
                    }
                }
            } finally {
                // Also runs when the keyboard hides mid-hold, so the key never stays "pressed".
                pressedChange(false)
            }
        }
    }
}

/**
 * Tap types a space. Holding still past [longPressMs] opens the keyboard picker. With [slide],
 * sliding sideways moves the cursor one character per [CURSOR_STEP], left or right; without it, a
 * slide is still a tap on release.
 */
@Composable
private fun Modifier.spaceGestures(
    longPressMs: Long,
    onPressedChange: (Boolean) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    slide: Boolean,
    onMove: (Int) -> Unit,
): Modifier {
    val pressedChange by rememberUpdatedState(onPressedChange)
    val tap by rememberUpdatedState(onTap)
    val longPress by rememberUpdatedState(onLongPress)
    val move by rememberUpdatedState(onMove)
    // Keyed on the settings: they can change while the keyboard is up.
    return pointerInput(longPressMs, slide) {
        awaitEachGesture {
            val down = awaitFirstDown()
            pressedChange(true)
            try {
                var dragging = false
                val tapped = withTimeoutOrNull(longPressMs) {
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                        if (change.changedToUp()) return@withTimeoutOrNull true
                        if (slide && abs(change.position.x - down.position.x) > viewConfiguration.touchSlop) {
                            change.consume()
                            dragging = true
                            return@withTimeoutOrNull false
                        }
                    }
                    @Suppress("UNREACHABLE_CODE") null
                }
                when {
                    tapped == true -> tap()
                    dragging -> {
                        val step = CURSOR_STEP.toPx()
                        var anchor = down.position.x
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            val steps = ((change.position.x - anchor) / step).toInt()
                            if (steps != 0) {
                                anchor += steps * step
                                move(steps)
                            }
                            if (change.changedToUp()) break
                            change.consume()
                        }
                    }
                    tapped == null -> longPress()
                }
            } finally {
                pressedChange(false)
            }
        }
    }
}

/** Fires [onDown] the moment the finger lands; pressed until it lifts. */
@Composable
private fun Modifier.switchOnDown(onPressedChange: (Boolean) -> Unit, onDown: () -> Unit): Modifier {
    // Updated each composition: after the switch this slot holds the other layer key, whose press differs.
    val down by rememberUpdatedState(onDown)
    val pressedChange by rememberUpdatedState(onPressedChange)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown()
            pressedChange(true)
            try {
                down()
                waitForUpOrCancellation()
            } finally {
                pressedChange(false)
            }
        }
    }
}

private const val REPEAT_START_MS = 400L
private const val REPEAT_MS = 50L
private val WORD_STEP = 40.dp
private val CURSOR_STEP = 10.dp

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
    Key.ToMoreSymbols -> stringResource(R.string.key_more_symbols)
    Key.ToLetters -> stringResource(R.string.key_letters)
    Key.Emoji -> stringResource(R.string.key_emoji)
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

/** What the row above the keys shows. */
private enum class TopRow { SEARCH, SUGGESTIONS, IDLE, NONE, TOOLBAR }
