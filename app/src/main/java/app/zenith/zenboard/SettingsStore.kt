package app.zenith.zenboard

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** User settings. Defaults: pure black, key sound and hold-space switching off; popup, suggestions, autocorrect, vibration on. */
data class ZenboardSettings(
    val pureBlack: Boolean = false,
    val keyPopup: Boolean = true,
    val suggestions: Boolean = true,
    val autocorrect: Boolean = true,
    /** Key height as a multiple of the standard height, [KEY_HEIGHT_MIN] to [KEY_HEIGHT_MAX]. */
    val keyHeight: Float = 1f,
    val haptics: Boolean = true,
    val sound: Boolean = false,
    /** Holding space for [SPACE_SWITCH_HOLD_MS] opens the keyboard picker. */
    val spaceSwitch: Boolean = false,
)

/** Long on purpose: a resting thumb on space must not open the picker. */
const val SPACE_SWITCH_HOLD_MS = 2000L

const val KEY_HEIGHT_MIN = 0.8f
const val KEY_HEIGHT_MAX = 1.3f

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val PURE_BLACK = booleanPreferencesKey("pure_black")
private val KEY_POPUP = booleanPreferencesKey("key_popup")
private val SUGGESTIONS = booleanPreferencesKey("suggestions")
private val AUTOCORRECT = booleanPreferencesKey("autocorrect")
private val KEY_HEIGHT = floatPreferencesKey("key_height")
private val HAPTICS = booleanPreferencesKey("haptics")
private val SOUND = booleanPreferencesKey("sound")
private val SPACE_SWITCH = booleanPreferencesKey("space_switch")

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
                keyHeight = (it[KEY_HEIGHT] ?: 1f).coerceIn(KEY_HEIGHT_MIN, KEY_HEIGHT_MAX),
                haptics = it[HAPTICS] ?: true,
                sound = it[SOUND] ?: false,
                spaceSwitch = it[SPACE_SWITCH] ?: false,
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
}
