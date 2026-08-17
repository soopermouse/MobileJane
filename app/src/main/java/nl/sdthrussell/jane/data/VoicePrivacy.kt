package nl.sdthrussell.jane.data

import android.content.Context
import java.io.File

object VoicePrivacy {
    private const val PREFS = "jane_voice_privacy"
    private const val CONSENT = "continuous_recording_consent"
    const val RETENTION_DAYS = 7

    fun hasConsent(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CONSENT, false)

    fun setConsent(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CONSENT, value).apply()
    }

    fun migrateLegacyRecordings(context: Context) {
        val dir = File(context.filesDir, "voice_recordings")
        val crypto = JaneCrypto()
        dir.listFiles()?.filter { it.extension.equals("wav", true) }?.forEach { source ->
            runCatching {
                val dataBytes = (source.length() - 44).coerceAtLeast(0)
                val duration = dataBytes * 1000L / (16000L * 2L)
                val target = File(dir, source.nameWithoutExtension + "-${duration}ms.jrec")
                target.writeBytes(crypto.encrypt(source.readBytes()))
                target.setLastModified(source.lastModified())
                source.delete()
            }
        }
    }

    fun purgeExpired(context: Context) {
        val cutoff = System.currentTimeMillis() - RETENTION_DAYS * 24L * 60L * 60L * 1000L
        File(context.filesDir, "voice_recordings").listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }
}
