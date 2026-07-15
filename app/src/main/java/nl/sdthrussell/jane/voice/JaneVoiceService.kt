package nl.sdthrussell.jane.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class JaneVoiceService : Service() {
    companion object {
        const val CHANNEL_ID = "jane_voice"
        const val NOTIFICATION_ID = 4102
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Jane voice activation",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        startForeground(NOTIFICATION_ID, notification())
    }

    private fun notification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jane voice mode")
            .setContentText("Jane's local voice service is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        // v0.2 foundation. A private on-device wake-word detector is added in v0.3.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
