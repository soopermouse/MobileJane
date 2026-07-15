package nl.sdthrussell.jane.voice

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.ArrayDeque
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.sqrt

class VoiceActivatedRecorderService : Service() {
    companion object {
        const val ACTION_START="nl.sdthrussell.jane.voice.START_RECORDER"
        const val ACTION_STOP="nl.sdthrussell.jane.voice.STOP_RECORDER"
        const val CHANNEL="jane_voice_recorder"
        const val ID=4210
        private const val RATE=16000
        private const val THRESHOLD=1450.0
        private const val SILENCE_MS=2200L
        private const val MIN_MS=900L
    }
    @Volatile private var running=false
    private var wake: PowerManager.WakeLock?=null
    override fun onCreate(){
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL,"Jane voice recorder",NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(intent?.action==ACTION_STOP) running=false else startLoop()
        return START_STICKY
    }
    private fun startLoop(){
        if(running) return
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){ stopSelf(); return }
        running=true; startForeground(ID,note("Listening for speech"))
        wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Jane:Recorder").apply{acquire()}
        thread(name="JaneVAD"){ audioLoop() }
    }
    private fun audioLoop(){
        val min=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
        val size=maxOf(min,4096)
        val rec=AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size)
        val buf=ByteArray(size); val pre=ArrayDeque<ByteArray>(); var writer:WavWriter?=null; var file:File?=null; var start=0L; var last=0L
        try{
            rec.startRecording()
            while(running){
                val n=rec.read(buf,0,buf.size); if(n<=0) continue
                val chunk=buf.copyOf(n); pre.addLast(chunk); while(pre.size>10) pre.removeFirst()
                val now=System.currentTimeMillis(); val voice=rms(chunk)>=THRESHOLD
                if(writer==null && voice){
                    val dir=File(filesDir,"voice_recordings").apply{mkdirs()}; file=File(dir,"jane-${now}-${UUID.randomUUID()}.wav")
                    writer=WavWriter(file!!,RATE); pre.forEach{writer!!.write(it,it.size)}; start=now; last=now; update("Recording")
                } else if(writer!=null){
                    writer!!.write(chunk,n); if(voice) last=now
                    if(now-last>=SILENCE_MS){ writer!!.close(); writer=null; if(now-start<MIN_MS) file?.delete(); file=null; update("Listening for speech") }
                }
            }
        } finally {
            runCatching{writer?.close()}; runCatching{rec.stop()}; rec.release(); stopForeground(STOP_FOREGROUND_REMOVE)
            wake?.let{if(it.isHeld) it.release()}; stopSelf()
        }
    }
    private fun rms(b:ByteArray):Double{ var sum=0.0; var c=0; var i=0; while(i+1<b.size){ val s=(((b[i+1].toInt() shl 8) or (b[i].toInt() and 255))).toShort().toInt(); sum+=s.toDouble()*s; c++; i+=2 }; return if(c==0)0.0 else sqrt(sum/c) }
    private fun note(t:String)=NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Jane voice recorder").setContentText(t).setOngoing(true).build()
    private fun update(t:String)=getSystemService(NotificationManager::class.java).notify(ID,note(t))
    override fun onBind(intent:Intent?):IBinder?=null
}
