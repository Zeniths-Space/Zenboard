package app.zenith.zenboard

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
    private val emojiRecents by lazy { EmojiRecents(this) }
    private var barInsets by mutableStateOf(Rect())
    private var inputShown by mutableStateOf(false)
    private var settings by mutableStateOf(ZenboardSettings())

    /** Loaded on the first text field that allows suggestions; never for number-only use. */
    private var dictionary: Dictionary? = null
    private var dictionaryLoading = false

    /** The word being typed, shown underlined in the editor as composing text. */
    private val composing = StringBuilder()
    private var suggestions by mutableStateOf(Suggestions.NONE)
    private var stripShown by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        // Live settings: toggles in the settings screen apply to the open keyboard immediately.
        lifecycleScope.launch { SettingsStore.settings(this@ZenboardService).collect { settings = it } }
    }

    override fun onDestroy() {
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
                        emoji = emoji,
                        recentEmoji = recentEmoji,
                        onEmoji = ::onEmoji,
                        stripShown = stripShown,
                        suggestions = suggestions,
                        onPick = ::onPick,
                        onChangeKeyboard = ::showKeyboardPicker,
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
        state = KeyboardState(layer = initialLayer(info.inputType)).withAutoCaps(capsAtCursor())
        enterAction = enterAction(info.imeOptions)
        composing.clear()
        suggestions = Suggestions.NONE
        stripShown = settings.suggestions && suggestionsAllowed(info.inputType)
        if (stripShown) ensureDictionary()
    }

    // The preview overlay window exists only while the keyboard is on screen.
    override fun onFinishInputView(finishingInput: Boolean) {
        inputShown = false
        finishComposing()
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
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

    private fun capsAtCursor(): Boolean {
        val inputType = currentInputEditorInfo?.inputType ?: return false
        // Skip the IPC round trip when the field never auto-capitalizes.
        if (inputType and CAPS_FLAGS == 0) return false
        return (currentInputConnection?.getCursorCapsMode(inputType) ?: 0) != 0
    }

    private fun onKey(key: Key) {
        val ic = currentInputConnection ?: return
        when (key) {
            is Key.Text -> {
                val text = state.textFor(key)
                if (stripShown && isWordChar(text, composing) && startOrContinueWord(ic)) {
                    composing.append(text)
                    ic.setComposingText(composing, 1)
                    refreshSuggestions()
                } else {
                    finishComposing()
                    ic.commitText(text, 1)
                }
                state = state.afterText()
            }
            Key.Space -> {
                finishComposing()
                ic.commitText(" ", 1)
            }
            Key.Backspace -> if (composing.isNotEmpty()) {
                composing.deleteCharAt(composing.lastIndex)
                ic.setComposingText(composing, 1)
                refreshSuggestions()
            } else {
                // A key event deletes the selection or a whole emoji, which deleteSurroundingText does not.
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
            Key.Enter -> {
                finishComposing()
                enterAction?.let { ic.performEditorAction(it) } ?: sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
            Key.Shift -> state = state.tapShift(SystemClock.uptimeMillis())
            Key.ToSymbols -> state = state.copy(layer = Layer.SYMBOLS)
            Key.ToLetters -> state = state.copy(layer = Layer.LETTERS)
            Key.Emoji -> openEmoji()
        }
    }

    private fun ensureDictionary() {
        if (dictionary != null || dictionaryLoading) return
        dictionaryLoading = true
        lifecycleScope.launch { dictionary = Dictionary.load(this@ZenboardService) }
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

    /** Commit the word being typed as it is. */
    private fun finishComposing() {
        if (composing.isEmpty()) return
        currentInputConnection?.finishComposingText()
        composing.clear()
        suggestions = Suggestions.NONE
    }

    private fun refreshSuggestions() {
        val dict = dictionary
        suggestions = if (dict == null || composing.isEmpty()) Suggestions.NONE else suggest(dict, composing.toString(), allowAutocorrect = false)
    }

    /** Tap on a suggestion: it replaces the word being typed, followed by a space. */
    private fun onPick(word: String) {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        ic.commitText(word, 1)
        ic.commitText(" ", 1)
        ic.endBatchEdit()
        composing.clear()
        suggestions = Suggestions.NONE
    }

    /** Loaded once, on first use, so keyboards that never open emoji pay nothing. */
    private fun openEmoji() {
        state = state.copy(layer = Layer.EMOJI)
        if (recentEmoji.isEmpty()) recentEmoji = emojiRecents.load()
        if (emoji == null) lifecycleScope.launch { emoji = loadEmoji(this@ZenboardService) }
    }

    private fun onEmoji(value: String) {
        val ic = currentInputConnection ?: return
        finishComposing()
        ic.commitText(value, 1)
        if (!isIncognito(currentInputEditorInfo)) {
            recentEmoji = pushRecent(recentEmoji, value)
            emojiRecents.save(recentEmoji)
        }
    }

    /** Hold space: the system keyboard picker, also the way back to another keyboard. */
    private fun showKeyboardPicker() {
        getSystemService(InputMethodManager::class.java).showInputMethodPicker()
    }

    private companion object {
        /** Longest word picked up from before the cursor. */
        const val MAX_WORD = 48

        const val CAPS_FLAGS = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or
            InputType.TYPE_TEXT_FLAG_CAP_WORDS or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    }
}
