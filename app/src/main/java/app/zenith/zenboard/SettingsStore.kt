package app.zenith.zenboard

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** User settings. Defaults: pure black off; key-press popup, word suggestions and autocorrect on. */
data class ZenboardSettings(
    val pureBlack: Boolean = false,
    val keyPopup: Boolean = true,
    val suggestions: Boolean = true,
    val autocorrect: Boolean = true,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val PURE_BLACK = booleanPreferencesKey("pure_black")
private val KEY_POPUP = booleanPreferencesKey("key_popup")
private val SUGGESTIONS = booleanPreferencesKey("suggestions")
private val AUTOCORRECT = booleanPreferencesKey("autocorrect")

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
}
