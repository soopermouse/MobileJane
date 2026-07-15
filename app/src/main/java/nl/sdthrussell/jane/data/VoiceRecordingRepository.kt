package nl.sdthrussell.jane.data

import android.content.Context
import java.io.File
import nl.sdthrussell.jane.model.VoiceRecording

class VoiceRecordingRepository(context:Context){
    private val dir=File(context.filesDir,"voice_recordings").apply{mkdirs()}
    fun list():List<VoiceRecording> = dir.listFiles()?.filter{it.extension.equals("wav",true)}?.sortedByDescending{it.lastModified()}?.map{
        val data=(it.length()-44).coerceAtLeast(0); VoiceRecording(it.absolutePath,it.lastModified(),data*1000/(16000*2),it.length())
    } ?: emptyList()
    fun delete(item:VoiceRecording)=File(item.path).delete()
}
