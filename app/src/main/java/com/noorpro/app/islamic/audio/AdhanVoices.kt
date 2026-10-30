package com.noorpro.app.islamic.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import com.noorpro.app.prayer.AdhanSound
import com.noorpro.app.prayer.PrayerPrefs
import com.noorpro.app.prayer.channels.PrayerNotificationChannels

/* 1.1.6 Adhan voices (adhan_voices_enabled). NoorPro does not bundle any adhan recording (none is
 * licensed for redistribution yet), so this picker works with the three real AdhanSound options and
 * the phone's own tones, with preview and apply. Recorded voices slot in here once licensed audio
 * exists: each needs its own notification channel because Android freezes a channel's sound. */

data class AdhanVoiceOption(val sound: AdhanSound, val pref: String, val title: String, val description: String, val channel: String)

object AdhanVoiceLogic {
    val OPTIONS = listOf(
        AdhanVoiceOption(AdhanSound.ALARM, "Alarm tone", "Alarm tone", "Plays like an alarm at each prayer time, even when the phone is on vibrate (your phone's alarm sound).",
            PrayerNotificationChannels.ADHAN_ALARM),
        AdhanVoiceOption(AdhanSound.NOTIFICATION, "Notification tone", "Notification tone", "A shorter chime using your phone's notification sound.",
            PrayerNotificationChannels.ADHAN_SOFT),
        AdhanVoiceOption(AdhanSound.SILENT, "Silent", "Vibrate only", "A heads-up with vibration and no sound, for work or class.",
            PrayerNotificationChannels.ADHAN_SILENT),
    )

    fun current(pref: String?): AdhanVoiceOption = AdhanSound.fromPref(pref).let { s -> OPTIONS.first { it.sound == s } }

    /** Preview length; long enough to recognise the tone, short enough not to disturb. */
    const val PREVIEW_MS = 4_000L

    fun previewUri(sound: AdhanSound) = when (sound) {
        AdhanSound.ALARM -> RingtoneManager.TYPE_ALARM
        AdhanSound.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
        AdhanSound.SILENT -> null
    }
}

/** Plays a short preview of a tone (or a vibration for "Vibrate only"). */
class AdhanPreview(private val ctx: Context) {
    private var ringtone: Ringtone? = null

    fun play(option: AdhanVoiceOption) {
        stop()
        val type = AdhanVoiceLogic.previewUri(option.sound)
        if (type == null) { vibrate(); return }
        val uri = RingtoneManager.getDefaultUri(type) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
        ringtone = runCatching { RingtoneManager.getRingtone(ctx, uri) }.getOrNull()?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(if (option.sound == AdhanSound.ALARM) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            runCatching { play() }
        }
    }

    fun stop() { runCatching { ringtone?.stop() }; ringtone = null }

    @Suppress("DEPRECATION")
    private fun vibrate() = runCatching {
        val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator ?: return@runCatching
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 350, 250, 350), -1)) else v.vibrate(longArrayOf(0, 350, 250, 350), -1)
    }

    companion object {
        fun currentPref(ctx: Context) = PrayerPrefs(ctx).alarmSound
    }
}
