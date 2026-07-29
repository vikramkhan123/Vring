package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.example.service.RingtonePlaybackService

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneStateReceiver"
        private var lastTriggerTime: Long = 0
        private var lastNumber: String = ""
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

            Log.d(TAG, "Phone state changed: state=$state, number=$incomingNumber")

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val currentTime = System.currentTimeMillis()
                if (incomingNumber == lastNumber && (currentTime - lastTriggerTime < 3000)) {
                    return
                }
                
                lastTriggerTime = currentTime
                lastNumber = incomingNumber

                val numberToPass = if (incomingNumber.isNotBlank()) incomingNumber else "Unknown"

                try {
                    val serviceIntent = Intent(context, RingtonePlaybackService::class.java).apply {
                        action = RingtonePlaybackService.ACTION_START_INCOMING_CALL
                        putExtra(RingtonePlaybackService.EXTRA_INCOMING_NUMBER, numberToPass)
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            context.startForegroundService(serviceIntent)
                        } catch (e: Exception) {
                            context.startService(serviceIntent)
                        }
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start ringtone service from receiver", e)
                }

            } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                try {
                    RingtonePlaybackService.stopPlayback(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop playback", e)
                }
                lastNumber = ""
            }
        }
    }
}
