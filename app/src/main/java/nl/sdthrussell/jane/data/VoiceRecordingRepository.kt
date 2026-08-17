package nl.sdthrussell.jane.data

import android.content.Context
import java.io.File
import nl.sdthrussell.jane.model.VoiceRecording

class VoiceRecordingRepository(private val context: Context) {
    private val dir = File(context.filesDir, "voice_recordings").apply { mkdirs() }

    fun list(): List<VoiceRecording> {
        VoicePrivacy.migrateLegacyRecordings(context)
        VoicePrivacy.purgeExpired(context)
        return dir.listFiles()
            ?.filter { it.extension.equals("jrec", true) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                val duration = Regex("-(\\d+)ms\\.jrec$").find(file.name)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
                VoiceRecording(file.absolutePath, file.lastModified(), duration, file.length())
            } ?: emptyList()
    }

    fun delete(item: VoiceRecording) = File(item.path).delete()
}
