package app.zenith.zenboard

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** User settings. Defaults: pure black, key sound and hold-space switching and backspace double tap off; space label, space slide, backspace slide, glide typing, clipboard history (1 hour), popup, suggestions, autocorrect, auto-capitalization, space after full stop, vibration on. */
data class ZenboardSettings(
    val pureBlack: Boolean = false,
    val keyPopup: Boolean = true,
    val suggestions: Boolean = true,
    val autocorrect: Boolean = true,
    /** Capital letter at the start of a field and of each sentence. */
    val autoCaps: Boolean = true,
    /** A space after a full stop typed at the end of a word. */
    val autoSpace: Boolean = true,
    /** Key height as a multiple of the standard height, [KEY_HEIGHT_MIN] to [KEY_HEIGHT_MAX]. */
    val keyHeight: Float = 1f,
    val haptics: Boolean = true,
    val sound: Boolean = false,
    /** Holding space for [SPACE_SWITCH_HOLD_MS] opens the keyboard picker. */
    val spaceSwitch: Boolean = false,
    /** The language name on the space bar. */
    val spaceLabel: Boolean = true,
    /** Slide along space to move the cursor. */
    val spaceSlide: Boolean = true,
    /** Slide left on backspace to delete whole words. */
    val backspaceSlide: Boolean = true,
    /** Double tap backspace to delete a word. Off by default: quick taps to delete letters would take words. */
    val backspaceDoubleTap: Boolean = false,
    /** Keep copied text for pasting later. */
    val clipHistory: Boolean = true,
    val clipRetention: ClipRetention = ClipRetention.HOUR,
    /** One-handed mode: off, or the side the narrower keyboard sits on. */
    val oneHanded: OneHanded = OneHanded.OFF,
    /** Slide across letters to type a word. */
    val glide: Boolean = true,
)

/** Long on purpose: a resting thumb on space must not open the picker. */
const val SPACE_SWITCH_HOLD_MS = 1500L

/** One-handed mode: the keyboard narrows to [ONE_HANDED_WIDTH] of the screen on one side. */
enum class OneHanded { OFF, LEFT, RIGHT }

const val ONE_HANDED_WIDTH = 0.82f

const val KEY_HEIGHT_MIN = 0.8f
const val KEY_HEIGHT_MAX = 1.3f

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val PURE_BLACK = booleanPreferencesKey("pure_black")
private val KEY_POPUP = booleanPreferencesKey("key_popup")
private val SUGGESTIONS = booleanPreferencesKey("suggestions")
private val AUTOCORRECT = booleanPreferencesKey("autocorrect")
private val AUTO_CAPS = booleanPreferencesKey("auto_caps")
private val AUTO_SPACE = booleanPreferencesKey("auto_space")
private val KEY_HEIGHT = floatPreferencesKey("key_height")
private val HAPTICS = booleanPreferencesKey("haptics")
private val SOUND = booleanPreferencesKey("sound")
private val SPACE_SWITCH = booleanPreferencesKey("space_switch")
private val SPACE_LABEL = booleanPreferencesKey("space_label")
private val SPACE_SLIDE = booleanPreferencesKey("space_slide")
private val BACKSPACE_SLIDE = booleanPreferencesKey("backspace_slide")
private val GLIDE = booleanPreferencesKey("glide")
private val BACKSPACE_DOUBLE_TAP = booleanPreferencesKey("backspace_double_tap")
private val CLIP_HISTORY = booleanPreferencesKey("clip_history")
private val CLIP_RETENTION = stringPreferencesKey("clip_retention")
private val ONE_HANDED = stringPreferencesKey("one_handed")

/** Settings shared by the settings screen and the keyboard; both observe it, so changes apply live. */
object SettingsStore {
    fun settings(context: Context): Flow<ZenboardSettings> = context.applicationContext.settingsStore.data
        // An unreadable file falls back to defaults instead of crashing the keyboard.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            ZenboardSettings(
                pureBlack = it[PURE_BLACK] ?: false,
                keyPopup = it[KEY_POPUP] ?: true,
                suggestions = it[SUGGESTIONS] ?: true,
                autocorrect = it[AUTOCORRECT] ?: true,
                autoCaps = it[AUTO_CAPS] ?: true,
                autoSpace = it[AUTO_SPACE] ?: true,
                keyHeight = (it[KEY_HEIGHT] ?: 1f).coerceIn(KEY_HEIGHT_MIN, KEY_HEIGHT_MAX),
                haptics = it[HAPTICS] ?: true,
                sound = it[SOUND] ?: false,
                spaceSwitch = it[SPACE_SWITCH] ?: false,
                spaceLabel = it[SPACE_LABEL] ?: true,
                spaceSlide = it[SPACE_SLIDE] ?: true,
                backspaceSlide = it[BACKSPACE_SLIDE] ?: true,
                glide = it[GLIDE] ?: true,
                backspaceDoubleTap = it[BACKSPACE_DOUBLE_TAP] ?: false,
                clipHistory = it[CLIP_HISTORY] ?: true,
                clipRetention = ClipRetention.entries.firstOrNull { r -> r.name == it[CLIP_RETENTION] } ?: ClipRetention.HOUR,
                oneHanded = OneHanded.entries.firstOrNull { o -> o.name == it[ONE_HANDED] } ?: OneHanded.OFF,
            )
        }

    suspend fun setPureBlack(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[PURE_BLACK] = value }
    }

    suspend fun setKeyPopup(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[KEY_POPUP] = value }
    }

    suspend fun setSuggestions(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[SUGGESTIONS] = value }
    }

    suspend fun setAutocorrect(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[AUTOCORRECT] = value }
    }

    suspend fun setAutoCaps(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[AUTO_CAPS] = value }
    }

    suspend fun setAutoSpace(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[AUTO_SPACE] = value }
    }

    suspend fun setKeyHeight(context: Context, value: Float) {
        context.applicationContext.settingsStore.edit { it[KEY_HEIGHT] = value }
    }

    suspend fun setHaptics(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[HAPTICS] = value }
    }

    suspend fun setSound(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[SOUND] = value }
    }

    suspend fun setSpaceSwitch(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[SPACE_SWITCH] = value }
    }

    suspend fun setSpaceLabel(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[SPACE_LABEL] = value }
    }

    suspend fun setSpaceSlide(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[SPACE_SLIDE] = value }
    }

    suspend fun setBackspaceSlide(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[BACKSPACE_SLIDE] = value }
    }

    suspend fun setGlide(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[GLIDE] = value }
    }

    suspend fun setBackspaceDoubleTap(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[BACKSPACE_DOUBLE_TAP] = value }
    }

    suspend fun setClipHistory(context: Context, value: Boolean) {
        context.applicationContext.settingsStore.edit { it[CLIP_HISTORY] = value }
    }

    suspend fun setClipRetention(context: Context, value: ClipRetention) {
        context.applicationContext.settingsStore.edit { it[CLIP_RETENTION] = value.name }
    }

    suspend fun setOneHanded(context: Context, value: OneHanded) {
        context.applicationContext.settingsStore.edit { it[ONE_HANDED] = value.name }
    }
}

/**
 * The launcher icon: the `.Launcher` activity-alias, off in the manifest and switched on and off
 * through PackageManager. Android keeps this state itself, so it is not stored with the settings.
 */
object LauncherIcon {
    private fun component(context: Context) = ComponentName(context, "app.zenith.zenboard.Launcher")

    fun isShown(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(component(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setShown(context: Context, shown: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            component(context),
            // Off goes back to the manifest default (off), so no App info stand-in appears.
            if (shown) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
            // The settings screen is open while this changes: do not kill the app.
            PackageManager.DONT_KILL_APP,
        )
    }
}
