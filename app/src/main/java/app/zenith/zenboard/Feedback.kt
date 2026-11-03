package app.zenith.zenboard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Key vibration and click sound, each switchable in settings. Both also follow the system settings.
 *
 * Vibration goes straight to the Vibrator as touch feedback. View.performHapticFeedback is not
 * used: Samsung files an IME's calls under usage UNKNOWN, whose intensity is off by default, so
 * the keyboard never vibrated there. Touch usage follows the system touch-vibration setting.
 */
class KeyFeedback(context: Context, private val audio: AudioManager, val haptics: Boolean, val sound: Boolean) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }?.takeIf { it.hasVibrator() }

    /** [type] is a HapticFeedbackConstants value: a tap, a slide step or a long press. */
    fun haptic(type: Int) {
        val v = vibrator ?: return
        if (!haptics) return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(
                when (type) {
                    HapticFeedbackConstants.LONG_PRESS -> VibrationEffect.EFFECT_HEAVY_CLICK
                    HapticFeedbackConstants.CLOCK_TICK -> VibrationEffect.EFFECT_TICK
                    else -> VibrationEffect.EFFECT_CLICK
                },
            )
        } else {
            val ms = when (type) {
                HapticFeedbackConstants.LONG_PRESS -> 30L
                HapticFeedbackConstants.CLOCK_TICK -> 8L
                else -> 15L
            }
            VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
        }
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
        KeyFeedback(view.context, view.context.getSystemService(AudioManager::class.java), haptics, sound)
    }
}
