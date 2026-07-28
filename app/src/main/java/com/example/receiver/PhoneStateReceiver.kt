package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.service.RingtonePlaybackService

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

            Log.d(TAG, "PhoneStateReceiver: state=$state, number=$incomingNumber")

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    RingtonePlaybackService.startPlayback(context, incomingNumber)
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK, TelephonyManager.EXTRA_STATE_IDLE -> {
                    RingtonePlaybackService.stopPlayback(context)
                }
            }
        }
    }

    companion object {
        private const val TAG = "PhoneStateReceiver"
    }
}
