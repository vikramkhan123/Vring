package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.RingtoneApplication
import com.example.data.GeneralTrack
import com.example.data.RingtoneRepository
import com.example.data.VipContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RingtonePlaybackService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null

    private lateinit var repository: RingtoneRepository

    override fun onCreate() {
        super.onCreate()
        repository = (application as RingtoneApplication).repository
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_STOP
        val incomingNumber = intent?.getStringExtra(EXTRA_INCOMING_NUMBER) ?: ""

        when (action) {
            ACTION_START_INCOMING_CALL -> {
                handleIncomingCall(incomingNumber)
            }
            ACTION_STOP -> {
                stopPlaybackAndService()
            }
        }

        return START_NOT_STICKY
    }

    private fun handleIncomingCall(incomingNumber: String) {
        serviceScope.launch(Dispatchers.IO) {
            val config = repository.getConfigSync()
            if (!config.masterEnabled) {
                Log.d(TAG, "Master switch is OFF. Ignoring incoming call.")
                stopSelf()
                return@launch
            }

            muteSystemRingtone()

            val vipContacts = repository.getVipContactsSync()
            val generalTracks = repository.getGeneralTracksSync()

            // THIS WILL ONLY MATCH IF IT IS THE EXACT VIP NUMBER
            val matchedVip = findMatchingVip(incomingNumber, vipContacts)

            var selectedAudioUri: Uri? = null
            var selectedAudioTitle = "Default Ringtone"
            var isVipCall = false
            var callerDisplayName = if (incomingNumber.isNotBlank()) incomingNumber else "Unknown Caller"

            if (matchedVip != null) {
                // IT IS A VIP CALL
                isVipCall = true
                callerDisplayName = matchedVip.name
                
                if (!matchedVip.audioUriString.isNull_or_blank()) {
                    // Play THEIR specific song
                    selectedAudioUri = Uri.parse(matchedVip.audioUriString)
                    selectedAudioTitle = matchedVip.audioTitle ?: "VIP Custom Ringtone"
                } else {
                    // VIP found but no song assigned, fall back to general
                    val trackToPlay = selectGeneralTrack(config.playbackMode, config.lastPlayedIndex, generalTracks)
                    if (trackToPlay != null) {
                        selectedAudioUri = Uri.parse(trackToPlay.uriString)
                        selectedAudioTitle = trackToPlay.title
                    }
                }
            } else {
                // IT IS A GENERAL CALL (Not in VIP list)
                if (generalTracks.isNotEmpty()) {
                    val trackToPlay = selectGeneralTrack(config.playbackMode, config.lastPlayedIndex, generalTracks)
                    if (trackToPlay != null) {
                        selectedAudioUri = Uri.parse(trackToPlay.uriString)
                        selectedAudioTitle = trackToPlay.title
                    }
                }
            }

            if (selectedAudioUri == null) {
                selectedAudioUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                selectedAudioTitle = "System Default Tone"
            }

            repository.logCall(
                phoneNumber = incomingNumber.ifBlank { "Unknown" },
                callerName = matchedVip?.name,
                isVip = isVipCall,
                ringtoneName = selectedAudioTitle
            )

            withContext(Dispatchers.Main) {
                val notification = createNotification(callerDisplayName, selectedAudioTitle, isVipCall)
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting foreground service", e)
                }
                playAudioTrack(selectedAudioUri)
            }
        }
    }

    // NEW STRICT MATCHING LOGIC
    private fun findMatchingVip(incomingNumber: String, vipContacts: List<VipContact>): VipContact? {
        val cleanIncoming = incomingNumber.replace(Regex("[^0-9]"), "")
        if (cleanIncoming.isBlank()) return null

        // Extract last 10 digits for strict matching
        val incomingLast10 = if (cleanIncoming.length >= 10) cleanIncoming.takeLast(10) else cleanIncoming

        for (vip in vipContacts) {
            val cleanVip = vip.phoneNumber.replace(Regex("[^0-9]"), "")
            if (cleanVip.isBlank()) continue

            val vipLast10 = if (cleanVip.length >= 10) cleanVip.takeLast(10) else cleanVip

            if (incomingLast10 == vipLast10) {
                return vip // Exact 10-digit match found
            }
        }
        return null
    }

    private suspend fun selectGeneralTrack(
        mode: String,
        lastIndex: Int,
        tracks: List<GeneralTrack>
    ): GeneralTrack? {
        if (tracks.isEmpty()) return null
        return if (mode.equals("RANDOM", ignoreCase = true)) {
            tracks.random()
        } else {
            val nextIndex = (lastIndex) % tracks.size
            val track = tracks[nextIndex]
            repository.updateLastPlayedIndex((nextIndex + 1) % tracks.size)
            track
        }
    }

    private fun muteSystemRingtone() {
        try {
            audioManager?.let { am ->
                val prefs = getSharedPreferences("ringtone_prefs", Context.MODE_PRIVATE)
                val currentVol = am.getStreamVolume(AudioManager.STREAM_RING)
                if (currentVol > 0) {
                    prefs.edit().putInt("original_ring_volume", currentVol).apply()
                    am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error muting system ringtone", e)
        }
    }

    private fun restoreSystemRingtone() {
        try {
            audioManager?.let { am ->
                val prefs = getSharedPreferences("ringtone_prefs", Context.MODE_PRIVATE)
                val savedVol = prefs.getInt("original_ring_volume", -1)
                if (savedVol > 0) {
                    am.setStreamVolume(AudioManager.STREAM_RING, savedVol, 0)
                    prefs.edit().remove("original_ring_volume").apply()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring system ringtone", e)
        }
    }

    private fun playAudioTrack(uri: Uri) {
        stopPlaybackOnly()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaPlayer", e)
            try {
                val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(applicationContext, defaultUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback ringtone failed", fallbackError)
            }
        }
    }

    private fun stopPlaybackOnly() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun stopPlaybackAndService() {
        stopPlaybackOnly()
        restoreSystemRingtone()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    private fun createNotification(callerName: String, songTitle: String, isVip: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isVip) "VIP Incoming Call: $callerName" else "Incoming Call: $callerName"
        val text = "Playing Custom Ringtone: $songTitle"

        return NotificationCompat.Builder(this, RingtoneApplication.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaybackOnly()
        restoreSystemRingtone()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "RingtoneService"
        private const val NOTIFICATION_ID = 8801

        const val ACTION_START_INCOMING_CALL = "com.example.action.START_INCOMING_CALL"
        const val ACTION_STOP = "com.example.action.STOP"
        const val EXTRA_INCOMING_NUMBER = "extra_incoming_number"

        fun startPlayback(context: Context, incomingNumber: String) {
            val intent = Intent(context, RingtonePlaybackService::class.java).apply {
                action = ACTION_START_INCOMING_CALL
                putExtra(EXTRA_INCOMING_NUMBER, incomingNumber)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }

        fun stopPlayback(context: Context) {
            val intent = Intent(context, RingtonePlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.isBlank()
}
