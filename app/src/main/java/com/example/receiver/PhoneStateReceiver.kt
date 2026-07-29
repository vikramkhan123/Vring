package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                // Anti-Duplicate Lock: 3 second ke andar same number ka duplicate signal ignore karega
                val currentTime = System.currentTimeMillis()
                if (incomingNumber == lastNumber && (currentTime - lastTriggerTime < 3000)) {
                    Log.d(TAG, "Duplicate phone state trigger ignored for $incomingNumber")
                    return
                }
                
                lastTriggerTime = currentTime
                lastNumber = incomingNumber

                if (incomingNumber.isNotBlank()) {
                    RingtonePlaybackService.startPlayback(context, incomingNumber)
                } else {
                    RingtonePlaybackService.startPlayback(context, "Unknown")
                }
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call cut hone par playback band karein aur volume restore karein
                RingtonePlaybackService.stopPlayback(context)
                lastNumber = ""
            }
        }
    }
}
