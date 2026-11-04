package app.zenith.zenboard

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.inputmethod.EditorInfo
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawWithContent
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
    stripShown: Boolean,
    suggestions: Suggestions,
    onPick: (String) -> Unit,
    onToolbar: (ToolbarAction) -> Unit,
    onChangeKeyboard: () -> Unit,
    onDeleteWord: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    keyHeight: Float,
    feedback: KeyFeedback,
    onKey: (Key) -> Unit,
) = CompositionLocalProvider(LocalKeyFeedback provides feedback) {
    val preview = remember { PreviewState() }
    preview.bubbles = keyPopup
    val glide = remember { GlideState() }
    val glideCoords = remember { object { var c: LayoutCoordinates? = null } }
    val trailColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    var size by remember { mutableStateOf(IntSize.Zero) }
    if (showPreviews && size != IntSize.Zero) KeyPreviewOverlay(preview, size)
    Column(
        Modifier
            .fillMaxWidth()
            .onSizeChanged {
                size = it
                preview.keyboardWidth = it.width.toFloat()
            }
            .background(MaterialTheme.colorScheme.surface)
            .padding(systemBars)
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
        if (emojiQuery != null) {
            EmojiSearchBar(emojiQuery, emojiIndex, recentEmoji, emojiTones, onCloseEmojiSearch, onEmoji)
        } else if (stripShown) {
            // Words only while a word is being typed; otherwise the same space holds the toolbar.
            if (suggestions.primary == null && suggestions.others.isEmpty()) Toolbar(onToolbar) else SuggestionStrip(suggestions, onPick)
        }
        // Cap rows at 9% of screen height so landscape keeps most of the app visible.
        val rowHeight = minOf(56.dp, (LocalConfiguration.current.screenHeightDp * 0.09f).dp) * keyHeight
        // Emoji layer: a shorter bottom row (ABC, delete) gives the grid more room.
        val emojiRowHeight = rowHeight * 0.7f
        // Tab 0 (recents) each time the panel opens; it lives here so the bottom row can show "Clear all".
        var emojiTab by remember(state.layer == Layer.EMOJI) { mutableIntStateOf(0) }
        if (state.layer == Layer.EMOJI) {
            // Takes the place of the three upper rows, so the keyboard height does not jump.
            EmojiPanel(
                emoji, recentEmoji, emojiTones, emojiTab, { emojiTab = it },
                height = rowHeight * 4 - emojiRowHeight + 4.dp, onSearch = onEmojiSearch,
                onRemoveRecent = onRemoveRecentEmoji, onEmoji = onEmoji,
            )
        }
        keyboardRows(state.layer).forEach { row ->
            Row(Modifier.fillMaxWidth().height(if (state.layer == Layer.EMOJI) emojiRowHeight else rowHeight)) {
                val pad = (ROW_UNITS - row.sumOf { keyWeight(it, row).toDouble() }.toFloat()) / 2
                // Short rows are centred, except the emoji row: ABC at the left edge, delete at the right.
                val spread = state.layer == Layer.EMOJI
                if (pad > 0f && !spread) Spacer(Modifier.weight(pad))
                row.forEachIndexed { i, key ->
                    if (spread && i == row.lastIndex && pad > 0f) {
                        Box(Modifier.weight(pad * 2).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            if (emojiTab == 0 && emoji != null && recentEmoji.isNotEmpty()) ClearRecents(onClearRecentEmoji)
                        }
                    }
                    // Identity by key, not slot: switching layers must not hand one key's press and
                    // colour animations to whichever key lands in the same position.
                    key(key) {
                        KeyButton(
                            key, state, enterAction, preview, onKey, onChangeKeyboard.takeIf { spaceSwitch }, onDeleteWord,
                            backspaceSlide, backspaceDoubleTap, onMoveCursor.takeIf { spaceSlide }, spaceLabel, glide, Modifier.weight(keyWeight(key, row)),
                        )
                    }
                }
                if (pad > 0f && !spread) Spacer(Modifier.weight(pad))
            }
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
    modifier: Modifier,
) {
    val feedback = LocalKeyFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val clickPressed by interaction.collectIsPressedAsState()
    var heldPressed by remember { mutableStateOf(false) }
    // A glide that started on this key: no press look, no preview bubble.
    val pressed = (clickPressed || heldPressed) && !glide.active

    // Expressive press: the key springs toward a pill and dips slightly.
    val morph by animateFloatAsState(if (pressed) 1f else 0f, Motion.press(), label = "press")
    val shape = keyShape(morph.coerceIn(0f, 1f))
    val (container, content) = keyColors(key, state, pressed)
    val fill by animateColorAsState(container, tween(90), label = "fill")

    // Character keys show a preview bubble while pressed. Bounds are kept out of state: layout
    // updates them without recomposing.
    // inner: the key's own coordinates inside the press-scale layer, for mapping finger x to the root.
    val bounds = remember { object { var rect = Rect.Zero; var inner: LayoutCoordinates? = null } }
    if (key is Key.Text) {
        val label = state.textFor(key)
        LaunchedEffect(pressed) {
            if (pressed) {
                if (preview.bubbles) preview.current = KeyPress(label, bounds.rect)
            } else if (preview.current?.bounds == bounds.rect) {
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
                val s = 1f - 0.05f * morph
                scaleX = s
                scaleY = s
                this.shape = shape
                clip = true
            }
            .background(fill),
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
            key == Key.Space -> if (spaceLabel) {
                Text(
                    stringResource(R.string.space_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            key is Key.Text -> {
                Text(
                    state.textFor(key),
                    color = content,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                )
                hintFor(key, state.layer)?.let { hint ->
                    Text(
                        hint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 5.dp),
                    )
                }
            }
            else -> Text(
                when (key) {
                    Key.ToSymbols -> "?123"
                    Key.ToMoreSymbols -> "=\\<"
                    else -> "ABC"
                },
                color = content,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun keyShape(morph: Float): Shape = RoundedCornerShape(lerp(KeyCorner, PressedCorner, morph))

/** Letters are cards; function keys sit a tone deeper; Enter carries the secondary accent, active shift only in its icon. */
@Composable
private fun keyColors(key: Key, state: KeyboardState, pressed: Boolean): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    val active = ZenithColors.indicator to ZenithColors.onIndicator
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
    return pointerInput(Unit) {
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
