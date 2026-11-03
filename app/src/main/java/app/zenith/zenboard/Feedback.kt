package app.zenith.zenboard

import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/** Key vibration and click sound, each switchable in settings. Both also follow the system settings. */
class KeyFeedback(private val view: View, private val audio: AudioManager, val haptics: Boolean, val sound: Boolean) {

    fun haptic(type: Int) {
        if (haptics) view.performHapticFeedback(type)
    }

    /** A key press: a tap vibration and, if on, the system click for that kind of key. */
    fun press(key: Key? = null) {
        haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        // -1: the system's key-click volume. Silent when the phone is on silent or vibrate.
        if (sound) audio.playSoundEffect(soundFor(key), -1f)
    }

    private fun soundFor(key: Key?) = when (key) {
        Key.Backspace -> AudioManager.FX_KEYPRESS_DELETE
        Key.Space -> AudioManager.FX_KEYPRESS_SPACEBAR
        Key.Enter -> AudioManager.FX_KEYPRESS_RETURN
        else -> AudioManager.FX_KEYPRESS_STANDARD
    }
}

val LocalKeyFeedback = staticCompositionLocalOf<KeyFeedback> { error("No KeyFeedback provided") }

@Composable
fun rememberKeyFeedback(haptics: Boolean, sound: Boolean): KeyFeedback {
    val view = LocalView.current
    return remember(view, haptics, sound) {
        KeyFeedback(view, view.context.getSystemService(AudioManager::class.java), haptics, sound)
    }
}
