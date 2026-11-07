package app.zenith.zenboard

import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.geometry.Offset
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * The input method. Holds no typed text: each key goes straight to the editor's InputConnection.
 * Nothing is logged, stored, or sent anywhere.
 */
class ZenboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var state by mutableStateOf(KeyboardState())
    private var enterAction by mutableStateOf<Int?>(null)
    private var emoji by mutableStateOf<List<EmojiCategory>?>(null)
    private var recentEmoji by mutableStateOf<List<String>>(emptyList())
    private var emojiTones by mutableStateOf(EmojiTones.NONE)
    private val emojiRecents by lazy { EmojiRecents(this) }
    /** Non-null while searching emoji: letter keys type into this query instead of the editor. */
    private var emojiQuery by mutableStateOf<String?>(null)
    private var emojiIndex by mutableStateOf<List<EmojiInfo>?>(null)
    /** Clipboard history, newest first; pinned clips stay. Loaded once at start. */
    private var clips by mutableStateOf<List<Clip>>(emptyList())
    private val clipStore by lazy { ClipStore(this) }
    /** Text copied in the last [FRESH_CLIP_MS], offered as a one-tap paste until pasted, typed past or ignored. */
    private var freshClip by mutableStateOf<String?>(null)
    private var freshClipTime = 0L
    /** The fresh clip was on screen when this field opened; closing the keyboard then dismisses it. */
    private var freshClipSeen = false
    /** One timer per copy that takes the chip down when it is no longer fresh. */
    private var freshClipExpiry: Job? = null
    private val clipboard by lazy { getSystemService(ClipboardManager::class.java) }
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { onClipChanged() }
    private var barInsets by mutableStateOf(Rect())
    private var inputShown by mutableStateOf(false)
    private var settings by mutableStateOf(ZenboardSettings())

    /** Loaded on the first text field that allows suggestions; never for number-only use. */
    private var dictionary: Dictionary? = null
    private var dictionaryLoading = false

    /** The word being typed, shown underlined in the editor as composing text. */
    private val composing = StringBuilder()
    private var suggestions by mutableStateOf(Suggestions.NONE)
    // Suggestions for the latest keystroke, being worked out off the main thread.
    private var suggesting: Job? = null
    private var stripShown by mutableStateOf(false)
    private var autocorrectOn = false
    /** This field takes glided words (any text field but a password). */
    private var glideField by mutableStateOf(false)
    /** The word being composed came from a glide: backspace removes it whole. */
    private var glided = false
    /** A space was just added after a full stop: the next key may take it back. */
    private var autoSpaced = false
    private val learnedWords by lazy { LearnedWords(this) }

    /** The last autocorrection and what followed it, so an immediate backspace can undo it. */
    private class Correction(val original: String, val corrected: String, val separator: String)
    private var lastCorrection: Correction? = null

    /** The cursor (selection end), kept here so quick cursor moves need not wait for the editor. */
    private var cursor = 0

    /** Positions set by cursor moves whose onUpdateSelection has not arrived yet. */
    private val pendingMoves = ArrayDeque<Int>()

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        // Live settings: toggles in the settings screen apply to the open keyboard immediately.
        lifecycleScope.launch {
            clips = clipStore.load()
            SettingsStore.settings(this@ZenboardService).collect {
                settings = it
                // History off deletes the unpinned clips; a shorter retention drops the old ones.
                updateClips(if (it.clipHistory) clips else clips.filter(Clip::pinned))
            }
        }
        lifecycleScope.launch {
            ClipClears.events.collect { all -> updateClips(if (all) emptyList() else clips.filter(Clip::pinned)) }
        }
        clipboard.addPrimaryClipChangedListener(clipListener)
    }

    override fun onDestroy() {
        clipboard.removePrimaryClipChangedListener(clipListener)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        // Compose finds its lifecycle owners by walking up to the IME window's decor view.
        window.window?.decorView?.let {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
            // A framework parent with fitsSystemWindows stops insets before they reach the
            // ComposeView, so read them here. Android 15+ reports the IME's own nav-bar strip
            // (back / switch-keyboard buttons) as captionBar, which systemBars includes.
            it.setOnApplyWindowInsetsListener { v, insets ->
                barInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars())
                    Rect(bars.left, 0, bars.right, bars.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    Rect(insets.systemWindowInsetLeft, 0, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                v.onApplyWindowInsets(insets)
            }
        }
        return ComposeView(this).apply {
            setContent {
                ZenithTheme(pureBlack = settings.pureBlack) {
                    val bars = with(LocalDensity.current) {
                        PaddingValues(
                            start = barInsets.left.toDp(),
                            end = barInsets.right.toDp(),
                            bottom = barInsets.bottom.toDp(),
                        )
                    }
                    KeyboardView(
                        state = state,
                        enterAction = enterAction,
                        systemBars = bars,
                        showPreviews = inputShown,
                        keyPopup = settings.keyPopup,
                        spaceSwitch = settings.spaceSwitch,
                        spaceLabel = settings.spaceLabel,
                        spaceSlide = settings.spaceSlide,
                        backspaceSlide = settings.backspaceSlide,
                        backspaceDoubleTap = settings.backspaceDoubleTap,
                        glideEnabled = settings.glide && glideField && emojiQuery == null,
                        onGlide = ::onGlide,
                        emoji = emoji,
                        recentEmoji = recentEmoji,
                        emojiTones = emojiTones,
                        onEmoji = ::onEmoji,
                        emojiQuery = emojiQuery,
                        emojiIndex = emojiIndex,
                        onEmojiSearch = ::openEmojiSearch,
                        onRemoveRecentEmoji = { e ->
                            recentEmoji = recentEmoji - e
                            emojiRecents.save(recentEmoji)
                        },
                        onClearRecentEmoji = {
                            recentEmoji = emptyList()
                            emojiRecents.save(recentEmoji)
                        },
                        onCloseEmojiSearch = ::openEmoji,
                        clips = panelOrder(clips),
                        clipHistoryOn = settings.clipHistory,
                        freshClip = freshClip,
                        onPasteClip = ::pasteClip,
                        // Unpinning restarts the clip's clock, so an old clip does not vanish the moment it is unpinned.
                        onPinClip = { c -> updateClips(clips.map { if (it.text == c.text) it.copy(pinned = !it.pinned, time = System.currentTimeMillis()) else it }) },
                        onDeleteClip = { c ->
                            clipStore.forgetBefore = System.currentTimeMillis()
                            updateClips(clips.filter { it.text != c.text })
                        },
                        stripShown = stripShown,
                        suggestions = suggestions,
                        onPick = ::onPick,
                        onToolbar = ::onToolbar,
                        onChangeKeyboard = ::showKeyboardPicker,
                        onDeleteWord = ::deleteWord,
                        onMoveCursor = ::moveCursor,
                        keyHeight = settings.keyHeight,
                        oneHanded = settings.oneHanded,
                        toolbarShown = settings.toolbarShown,
                        toolbarButtons = settings.toolbar,
                        onOneHanded = { mode -> lifecycleScope.launch { SettingsStore.setOneHanded(this@ZenboardService, mode) } },
                        feedback = rememberKeyFeedback(settings.haptics, settings.sound),
                        onKey = ::onKey,
                    )
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateNavBarAppearance()
        inputShown = true
        emojiQuery = null
        if (System.currentTimeMillis() - freshClipTime > FRESH_CLIP_MS) freshClip = null
        freshClipSeen = freshClip != null
        state = KeyboardState(layer = initialLayer(info.inputType)).withAutoCaps(capsAtCursor())
        enterAction = enterAction(info.imeOptions)
        composing.clear()
        suggestions = Suggestions.NONE
        stripShown = settings.suggestions && suggestionsAllowed(info.inputType)
        autocorrectOn = settings.autocorrect && autocorrectAllowed(info.inputType)
        lastCorrection = null
        cursor = info.initialSelEnd.coerceAtLeast(0)
        pendingMoves.clear()
        glideField = glideAllowed(info.inputType)
        glided = false
        autoSpaced = false
        if (stripShown || autocorrectOn || (settings.glide && glideField)) ensureDictionary()
    }

    // The preview overlay window exists only while the keyboard is on screen.
    override fun onFinishInputView(finishingInput: Boolean) {
        inputShown = false
        finishComposing()
        // Offered once: ignored in a field means not wanted. A copy made in this field survives for the next one.
        if (freshClipSeen) freshClip = null
        freshClipSeen = false
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        val echo = pendingMoves.indexOf(newSelEnd)
        if (newSelStart == newSelEnd && echo >= 0) {
            // Our own move arriving late: only the newest one is current.
            repeat(echo + 1) { pendingMoves.removeFirst() }
            if (pendingMoves.isEmpty()) cursor = newSelEnd
        } else {
            pendingMoves.clear()
            cursor = newSelEnd
        }
        // The cursor left the word we were composing (tap elsewhere, selection, app edit): let it go.
        if (composing.isNotEmpty() && (newSelStart != newSelEnd || newSelEnd != candidatesEnd)) {
            composing.clear()
            currentInputConnection?.finishComposingText()
            suggestions = Suggestions.NONE
        }
        state = state.withAutoCaps(capsAtCursor())
    }

    /** Dark nav-strip buttons (back, switch keyboard) on a light keyboard, light ones on a dark keyboard. */
    private fun updateNavBarAppearance() {
        val w = window.window ?: return
        val light = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val flag = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            w.insetsController?.setSystemBarsAppearance(if (light) flag else 0, flag)
        } else {
            @Suppress("DEPRECATION")
            val decor = w.decorView
            @Suppress("DEPRECATION")
            decor.systemUiVisibility = if (light) {
                decor.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                decor.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
    }

    /**
     * Shift for the next letter. With auto-capitalization on, only fields that ask for capitals get
     * them: sentence starts found by the keyboard itself, and every word or all capitals through
     * Android's own caps mode.
     */
    private fun capsAtCursor(): Boolean {
        if (!settings.autoCaps) return false
        val inputType = currentInputEditorInfo?.inputType ?: return false
        if (!autoCapsAllowed(inputType) || !fieldWantsCaps(inputType)) return false
        val ic = currentInputConnection ?: return false
        if (inputType and WORD_CAPS_FLAGS != 0) return ic.getCursorCapsMode(inputType) != 0
        val before = ic.getTextBeforeCursor(CAPS_LOOKBACK, 0) ?: return false
        return sentenceStart(before)
    }

    private fun onKey(key: Key) {
        val ic = currentInputConnection ?: return
        if (emojiQuery != null && searchKey(key)) return
        val wasGlided = glided
        glided = false
        if (autoSpaced) {
            autoSpaced = false
            when {
                // Already spaced: a space now would double it.
                key == Key.Space -> return
                // "3.14", "e.g.", "...": a digit or punctuation joins the full stop, so the space goes.
                key is Key.Text && joinsFullStop(state.textFor(key)) &&
                    ic.getTextBeforeCursor(2, 0)?.toString() == ". " -> ic.deleteSurroundingText(1, 0)
            }
        }
        // Undo only applies to the very next key.
        val undo = lastCorrection.takeIf { key == Key.Backspace }
        lastCorrection = null
        when (key) {
            is Key.Text -> {
                // Typing instead of pasting: the user does not want the clip here.
                freshClip = null
                val text = state.textFor(key)
                when {
                    (stripShown || autocorrectOn) && isWordChar(text, composing) && startOrContinueWord(ic) -> {
                        composing.append(text)
                        ic.setComposingText(composing, 1)
                        refreshSuggestions()
                    }
                    endsWord(text) -> {
                        // A full stop after a word brings its space along; the correction undo covers both.
                        val space = text == "." && settings.autoSpace &&
                            autoCapsAllowed(currentInputEditorInfo?.inputType ?: 0) &&
                            (composing.isNotEmpty() || ic.getTextBeforeCursor(1, 0)?.firstOrNull()?.isLetterOrDigit() == true)
                        commitWord(ic, if (space) ". " else text)
                        autoSpaced = space
                    }
                    else -> {
                        finishComposing()
                        ic.commitText(text, 1)
                    }
                }
                state = state.afterText()
            }
            Key.Space -> commitWord(ic, " ")
            Key.Backspace -> if (undo != null && undoCorrection(ic, undo)) {
                // Reverted to what was typed.
            } else if (wasGlided && composing.isNotEmpty()) {
                // A glided word goes in one press, the way it came.
                ic.setComposingText("", 1)
                composing.clear()
                suggestions = Suggestions.NONE
            } else if (composing.isNotEmpty()) {
                composing.deleteCharAt(composing.lastIndex)
                ic.setComposingText(composing, 1)
                refreshSuggestions()
            } else {
                // A key event deletes the selection or a whole emoji, which deleteSurroundingText does not.
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
            Key.Enter -> {
                commitWord(ic, "", record = false)
                enterAction?.let { ic.performEditorAction(it) } ?: sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
            Key.Shift -> state = state.tapShift(SystemClock.uptimeMillis())
            Key.ToSymbols -> state = state.copy(layer = Layer.SYMBOLS)
            Key.ToMoreSymbols -> state = state.copy(layer = Layer.MORE_SYMBOLS)
            Key.ToLetters -> state = state.copy(layer = Layer.LETTERS)
            Key.Emoji -> openEmoji()
        }
    }

    private fun ensureDictionary() {
        if (dictionary != null || dictionaryLoading) return
        dictionaryLoading = true
        lifecycleScope.launch {
            dictionary = Dictionary.load(this@ZenboardService).also { it.learnAll(learnedWords.load()) }
        }
    }

    /**
     * Before the first letter of a word: pick up letters already left of the cursor (so editing
     * "hel|" continues "hel"), and refuse when the cursor is inside a word (plain typing there).
     */
    private fun startOrContinueWord(ic: android.view.inputmethod.InputConnection): Boolean {
        if (composing.isNotEmpty()) return true
        val after = ic.getTextAfterCursor(1, 0)
        if (!after.isNullOrEmpty() && after[0].isLetter()) return false
        val before = ic.getTextBeforeCursor(MAX_WORD, 0) ?: return true
        val start = before.indexOfLast { !(it.isLetter() || it == '\'') } + 1
        if (start < before.length) {
            val word = before.substring(start)
            ic.beginBatchEdit()
            ic.deleteSurroundingText(word.length, 0)
            ic.setComposingText(word, 1)
            ic.endBatchEdit()
            composing.append(word)
        }
        return true
    }

    /**
     * End the word being typed with [separator]: autocorrected when there is a confident fix,
     * otherwise as typed. Remembers the correction so the next backspace can undo it.
     */
    private fun commitWord(ic: android.view.inputmethod.InputConnection, separator: String, record: Boolean = true) {
        val typed = composing.toString()
        // A space typed before the last letter's suggestions arrived: work them out now, so the fix
        // is always for the word as typed.
        val current = if (suggesting?.isActive == true) dictionary?.let { suggest(it, typed, autocorrectOn) } ?: suggestions else suggestions
        suggesting?.cancel()
        val fix = current.autocorrect?.takeIf { autocorrectOn && typed.isNotEmpty() && it != typed }
        ic.beginBatchEdit()
        if (fix != null) ic.commitText(fix, 1) else ic.finishComposingText()
        if (separator.isNotEmpty()) ic.commitText(separator, 1)
        ic.endBatchEdit()
        composing.clear()
        suggestions = Suggestions.NONE
        if (fix != null && record) lastCorrection = Correction(typed, fix, separator)
    }

    /** Backspace right after an autocorrect: put back what was typed, and stop correcting it. */
    private fun undoCorrection(ic: android.view.inputmethod.InputConnection, undo: Correction): Boolean {
        val tail = undo.corrected + undo.separator
        if (ic.getTextBeforeCursor(tail.length, 0)?.toString() != tail) return false
        ic.beginBatchEdit()
        ic.deleteSurroundingText(tail.length, 0)
        ic.setComposingText(undo.original, 1)
        ic.endBatchEdit()
        composing.clear()
        composing.append(undo.original)
        keep(undo.original)
        refreshSuggestions()
        return true
    }

    /** The user insisted on this word: learn it, unless the field is private. */
    private fun keep(word: String) {
        dictionary?.learn(word)
        if (!isIncognito(currentInputEditorInfo)) learnedWords.add(word)
    }

    /** Commit the word being typed as it is. */
    private fun finishComposing() {
        if (composing.isEmpty()) return
        currentInputConnection?.finishComposingText()
        composing.clear()
        suggestions = Suggestions.NONE
    }

    /**
     * Off the main thread: scoring a half-typed word against the whole dictionary can take longer
     * than a frame, which made typing stutter. The newest keystroke wins; a result for a word that
     * has since changed is dropped.
     */
    private fun refreshSuggestions() {
        suggesting?.cancel()
        val dict = dictionary
        val typed = composing.toString()
        if (dict == null || typed.isEmpty()) {
            suggestions = Suggestions.NONE
            return
        }
        val autocorrect = autocorrectOn
        suggesting = lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) { suggest(dict, typed, allowAutocorrect = autocorrect) }
            if (composing.contentEquals(typed)) suggestions = result
        }
    }

    /** Tap on a suggestion: it replaces the word being typed, followed by a space. */
    private fun onPick(word: String) {
        val ic = currentInputConnection ?: return
        lastCorrection = null
        // Tapping the typed word itself means "this is a word": stop correcting it.
        if (suggestions.typedIsUnknown && word == composing.toString()) keep(word)
        ic.beginBatchEdit()
        ic.commitText(word, 1)
        ic.commitText(" ", 1)
        ic.endBatchEdit()
        composing.clear()
        suggestions = Suggestions.NONE
    }

    /** Backspace slid left: delete the word being typed, else the word before the cursor. */
    private fun deleteWord() {
        val ic = currentInputConnection ?: return
        if (emojiQuery != null) {
            emojiQuery = ""
            return
        }
        lastCorrection = null
        when {
            composing.isNotEmpty() -> {
                ic.setComposingText("", 1)
                composing.clear()
                suggestions = Suggestions.NONE
            }
            // Selected text goes as one, the same as a plain backspace.
            !ic.getSelectedText(0).isNullOrEmpty() -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            else -> {
                val n = wordDeleteLength(ic.getTextBeforeCursor(MAX_WORD, 0) ?: return)
                if (n > 0) ic.deleteSurroundingText(n, 0)
            }
        }
    }

    /**
     * Space bar slid sideways: move the cursor [steps] characters (negative is left). Uses
     * setSelection rather than arrow-key events, which move focus out of the field at its ends.
     */
    private fun moveCursor(steps: Int) {
        val ic = currentInputConnection ?: return
        if (emojiQuery != null) return
        finishComposing()
        lastCorrection = null
        val n = kotlin.math.abs(steps)
        val forward = steps > 0
        // Twice the steps: room for emoji, which take two characters each.
        val text = (if (forward) ic.getTextAfterCursor(n * 2, 0) else ic.getTextBeforeCursor(n * 2, 0)) ?: return
        val delta = cursorDelta(text, n, forward)
        if (delta == 0) return
        cursor = (if (forward) cursor + delta else cursor - delta).coerceAtLeast(0)
        pendingMoves.addLast(cursor)
        ic.setSelection(cursor, cursor)
    }

    private fun onToolbar(action: ToolbarAction) {
        val ic = currentInputConnection ?: return
        lastCorrection = null
        if (action == ToolbarAction.ONE_HANDED) {
            val next = if (settings.oneHanded == OneHanded.OFF) OneHanded.RIGHT else OneHanded.OFF
            lifecycleScope.launch { SettingsStore.setOneHanded(this@ZenboardService, next) }
            return
        }
        if (action == ToolbarAction.CLIPBOARD) {
            if (state.layer == Layer.CLIPBOARD) state = state.copy(layer = Layer.LETTERS) else openClipboard()
            return
        }
        if (action == ToolbarAction.SETTINGS) {
            requestHideSelf(0)
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        finishComposing()
        // The editor's own menu actions: they respect fields that block copying, such as passwords.
        ic.performContextMenuAction(
            when (action) {
                ToolbarAction.SELECT_ALL -> android.R.id.selectAll
                ToolbarAction.CUT -> android.R.id.cut
                ToolbarAction.COPY -> android.R.id.copy
                else -> android.R.id.paste
            },
        )
    }

    /** Loaded once, on first use, so keyboards that never open emoji pay nothing. */
    private fun openEmoji() {
        emojiQuery = null
        state = state.copy(layer = Layer.EMOJI)
        if (recentEmoji.isEmpty()) recentEmoji = emojiRecents.load()
        if (emoji == null) {
            lifecycleScope.launch {
                emojiTones = EmojiTones(loadEmojiTones(this@ZenboardService), emojiRecents.loadTones())
                emoji = loadEmoji(this@ZenboardService)
            }
        }
    }

    /** A finished glide: decode it off the main thread, then type the best word. */
    private fun onGlide(path: List<Offset>, geo: KeyGeometry) {
        val dict = dictionary ?: return ensureDictionary()
        val shift = state.shift
        lifecycleScope.launch {
            val words = withContext(Dispatchers.Default) { decodeGlide(path, geo, dict) }
            if (words.isEmpty()) return@launch
            commitGlide(
                words.map {
                    when (shift) {
                        ShiftMode.LOCKED -> it.uppercase()
                        ShiftMode.ONCE -> it.replaceFirstChar(Char::uppercase)
                        ShiftMode.OFF -> it
                    }
                },
            )
        }
    }

    /**
     * Types a glided word as composing text, with a space before it when it follows a word. The
     * other matches go to the suggestion strip; a space accepts the word as is (no autocorrect).
     */
    private fun commitGlide(words: List<String>) {
        val ic = currentInputConnection ?: return
        lastCorrection = null
        ic.beginBatchEdit()
        val afterWord = if (composing.isNotEmpty()) {
            ic.finishComposingText()
            composing.clear()
            true
        } else {
            ic.getTextBeforeCursor(1, 0)?.let { it.isNotEmpty() && !it[0].isWhitespace() } == true
        }
        if (afterWord) ic.commitText(" ", 1)
        composing.append(words[0])
        ic.setComposingText(composing, 1)
        ic.endBatchEdit()
        suggesting?.cancel()
        suggestions = Suggestions(words[0], words.drop(1), autocorrect = null, typedIsUnknown = false)
        glided = true
        state = state.afterText()
    }

    /** Emoji search: the letter keys come back and type into the query. The index loads on first use. */
    private fun openEmojiSearch() {
        emojiQuery = ""
        state = KeyboardState(layer = Layer.LETTERS)
        if (emojiIndex == null) lifecycleScope.launch { emojiIndex = loadEmojiIndex(this@ZenboardService) }
    }

    /** A key while searching emoji. False: not a search key, handle it as usual (shift, layer keys). */
    private fun searchKey(key: Key): Boolean {
        val query = emojiQuery ?: return false
        when (key) {
            is Key.Text -> {
                emojiQuery = query + state.textFor(key)
                state = state.afterText()
            }
            Key.Space -> emojiQuery = "$query "
            Key.Backspace -> emojiQuery = query.dropLast(1)
            // Enter types the best match, if there is one.
            Key.Enter -> emojiIndex?.let { searchEmoji(it, query, max = 1).firstOrNull() }?.let(::onEmoji)
            Key.Emoji -> openEmoji()
            else -> return false
        }
        return true
    }

    private fun onEmoji(value: String) {
        val ic = currentInputConnection ?: return
        finishComposing()
        ic.commitText(value, 1)
        if (!isIncognito(currentInputEditorInfo)) {
            recentEmoji = pushRecent(recentEmoji, value)
            emojiRecents.save(recentEmoji)
            val tones = emojiTones.withChoice(value)
            if (tones !== emojiTones) {
                emojiTones = tones
                emojiRecents.saveTones(tones.chosen)
            }
        }
    }

    /**
     * Something was copied. Only the default keyboard and the focused app may read the clipboard
     * (Android 10+), so a read can fail; that copy is then just not recorded.
     */
    private fun onClipChanged() {
        val text = runCatching { clipText(clipboard.primaryClip) }.getOrNull() ?: return
        if (inputShown && isIncognito(currentInputEditorInfo)) return
        freshClip = text
        freshClipTime = System.currentTimeMillis()
        freshClipExpiry?.cancel()
        freshClipExpiry = lifecycleScope.launch {
            delay(FRESH_CLIP_MS)
            freshClip = null
        }
        if (settings.clipHistory) updateClips(addClip(clips, text, freshClipTime))
    }

    /** New history: expired clips dropped, saved to disk when it changed. */
    private fun updateClips(new: List<Clip>) {
        val pruned = pruneClips(new, System.currentTimeMillis(), settings.clipRetention)
        if (pruned == clips) return
        clips = pruned
        lifecycleScope.launch { clipStore.save(pruned) }
    }

    private fun openClipboard() {
        finishComposing()
        // A copy made while the keyboard was not running was not heard: pick up what is on the clipboard
        // now, unless it was copied before the last delete (then it was deleted on purpose).
        val current = runCatching {
            clipboard.primaryClip?.takeIf { it.description.timestamp > clipStore.forgetBefore }?.let(::clipText)
        }.getOrNull()
        updateClips(if (settings.clipHistory && current != null && clips.none { it.text == current }) addClip(clips, current, System.currentTimeMillis()) else clips)
        state = state.copy(layer = Layer.CLIPBOARD)
    }

    private fun pasteClip(text: String) {
        val ic = currentInputConnection ?: return
        finishComposing()
        lastCorrection = null
        ic.commitText(text, 1)
        freshClip = null
    }

    /** Hold space (when enabled): the system keyboard picker. */
    private fun showKeyboardPicker() {
        getSystemService(InputMethodManager::class.java).showInputMethodPicker()
    }

    private companion object {
        /** Longest word picked up from before the cursor. */
        const val MAX_WORD = 48

        /** Fields asking for more than sentence capitals. */
        const val WORD_CAPS_FLAGS = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_FLAG_CAP_WORDS

        /** Enough text before the cursor to see past trailing spaces to the last mark. */
        const val CAPS_LOOKBACK = 64
    }
}
