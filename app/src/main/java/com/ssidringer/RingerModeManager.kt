package com.ssidringer

import android.content.Context
import android.media.AudioManager

object RingerModeManager {

    fun setRingerMode(context: Context, mode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val androidMode = when (mode) {
            SSIDRule.RINGER -> AudioManager.RINGER_MODE_NORMAL
            SSIDRule.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            SSIDRule.SILENT -> AudioManager.RINGER_MODE_SILENT
            else -> return
        }
        if (audioManager.ringerMode != androidMode) {
            audioManager.ringerMode = androidMode
        }
    }
}
