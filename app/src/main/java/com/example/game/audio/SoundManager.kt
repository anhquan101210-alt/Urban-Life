package com.example.game.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors

class SoundManager(private val context: Context) {
    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    private val executor = Executors.newSingleThreadExecutor()
    private var toneGenerator: ToneGenerator? = null

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playBuildSound() {
        if (!soundEnabled) return
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
            } catch (_: Exception) {}
        }
        vibrate(30)
    }

    fun playDemolishSound() {
        if (!soundEnabled) return
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 120)
            } catch (_: Exception) {}
        }
        vibrate(50)
    }

    fun playCashChime() {
        if (!soundEnabled) return
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 90)
            } catch (_: Exception) {}
        }
    }

    fun playSirenAlert() {
        if (!soundEnabled) return
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 300)
            } catch (_: Exception) {}
        }
        vibrate(120)
    }

    fun playClick() {
        if (!soundEnabled) return
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 35)
            } catch (_: Exception) {}
        }
        vibrate(15)
    }

    private fun vibrate(millis: Long) {
        if (!hapticsEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(millis)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
