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
import android.telephony.TelephonyManager
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

            // Save original ring volume permanently & mute system stream
            muteSystemRingtone()

            val vipContacts = repository.getVipContactsSync()
            val generalTracks = repository.getGeneralTracksSync()

            val matchedVip = findMatchingVip(incomingNumber, vipContacts)

            var selectedAudioUri: Uri? = null
            var selectedAudioTitle = "Default Ringtone"
            var isVipCall = false
            var callerDisplayName = matchedVip?.name ?: if (incomingNumber.isNotBlank()) incomingNumber else "Unknown Caller"

            if (matchedVip != null && !matchedVip.audioUriString.isNull_or_blank()) {
                isVipCall = true
                selectedAudioUri = Uri.parse(matchedVip.audioUriString)
                selectedAudioTitle = matchedVip.audioTitle ?: "VIP Custom Ringtone"
            } else if (generalTracks.isNotEmpty()) {
                val trackToPlay = selectGeneralTrack(config.playbackMode, config.lastPlayedIndex, generalTracks)
                if (trackToPlay != null) {
                    selectedAudioUri = Uri.parse(trackToPlay.uriString)
                    selectedAudioTitle = trackToPlay.title
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
                startForeground(NOTIFICATION_ID, notification)
                playAudioTrack(selectedAudioUri)
            }
        }
    }

    private fun findMatchingVip(incomingNumber: String, vipContacts: List<VipContact>): VipContact? {
        if (incomingNumber.isBlank()) return null
        val normalizedIncoming = normalizePhoneNumber(incomingNumber)
        return vipContacts.find { vip ->
            val normalizedVip = normalizePhoneNumber(vip.phoneNumber)
            normalizedVip.isNotBlank() && (
                normalizedIncoming.endsWith(normalizedVip) || normalizedVip.endsWith(normalizedIncoming)
            )
        }
    }

    private fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^0-9]"), "").takeLast(10)
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

    // NAYA LOGIC: Volume ko permanent memory mein save karna
    private fun muteSystemRingtone() {
        try {
            audioManager?.let { am ->
                val prefs = getSharedPreferences("ringtone_prefs", Context.MODE_PRIVATE)
                val currentVol = am.getStreamVolume(AudioManager.STREAM_RING)
                
                // Volume agar 0 se zyada hai, tabhi save karein
                if (currentVol > 0) {
                    prefs.edit().putInt("original_ring_volume", currentVol).apply()
                    am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                    Log.d(TAG, "Muted system ringtone. Original volume ($currentVol) saved.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error muting system ringtone", e)
        }
    }

    // NAYA LOGIC: Volume ko permanent memory se nikal kar wapas restore karna
    private fun restoreSystemRingtone() {
        try {
            audioManager?.let { am ->
                val prefs = getSharedPreferences("ringtone_prefs", Context.MODE_PRIVATE)
                val savedVol = prefs.getInt("original_ring_volume", -1)
                
                if (savedVol > 0) {
                    am.setStreamVolume(AudioManager.STREAM_RING, savedVol, 0)
                    prefs.edit().remove("original_ring_volume").apply()
                    Log.d(TAG, "Restored system ringtone to volume: $savedVol")
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
                        .setUsage(AudioAttributes.USAGE_ALARM) // Alarm Stream Use Kiya Gaya Hai
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "Playing ringtone audio: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaPlayer for URI $uri", e)
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
                Log.e(TAG, "Fallback ringtone failed as well", fallbackError)
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
        stopForeground(STOP_FOREGROUND_REMOVE)
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
        restoreSystemRingtone() // Ek baar yahan bhi ensure karega
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
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopPlayback(context: Context) {
            val intent = Intent(context, RingtonePlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.isBlank()
}
