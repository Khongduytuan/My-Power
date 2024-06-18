package com.eagletech.mypower.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.eagletech.mypower.service.BatteryService

class NotificationActionReceiver : BroadcastReceiver() {
    @SuppressLint("LongLogTag")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "STOP_NOTIFICATION") {
            Log.d("NotificationActionReceiver", "Stop action received")
            context?.let {
                val serviceIntent = Intent(it, BatteryService::class.java)
                it.stopService(serviceIntent)
            }
        }
    }
}