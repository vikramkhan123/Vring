package com.example.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class RingtoneCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val direction = callDetails.callDirection
            if (direction == Call.Details.DIRECTION_INCOMING) {
                val handle = callDetails.handle
                val incomingNumber = handle?.schemeSpecificPart ?: ""
                Log.d(TAG, "CallScreeningService intercepted incoming call from: $incomingNumber")

                // Launch custom ringtone playback
                RingtonePlaybackService.startPlayback(applicationContext, incomingNumber)
            }

            // Allow call to proceed to user ring UI normally
            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

            respondToCall(callDetails, response)
        }
    }

    companion object {
        private const val TAG = "RingtoneScreeningService"
    }
}
