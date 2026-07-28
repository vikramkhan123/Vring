package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.RingtoneRepository

class RingtoneApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: RingtoneRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        repository = RingtoneRepository(database.ringtoneDao())
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ringtone Mixer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active dynamic ringtone playback during incoming calls"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ringtone_mixer_channel"
        lateinit var instance: RingtoneApplication
            private set
    }
}
