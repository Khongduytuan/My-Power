package com.eagletech.mypower.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.media.Ringtone
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.eagletech.mypower.R
import com.eagletech.mypower.receiver.NotificationActionReceiver

class BatteryService : Service() {

    private lateinit var batteryReceiver: BroadcastReceiver
    private var ringtone: Ringtone? = null

    override fun onCreate() {
        super.onCreate()
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra("level", -1) ?: return
                val sharedPreferences = getSharedPreferences("BatteryPrefs", Context.MODE_PRIVATE)
                val targetLevel = sharedPreferences.getInt("battery_level", -1)

                if (level == targetLevel) {
                    sendNotification(level)
                    vibratePhone()
                    playSound()
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    private fun sendNotification(level: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "BatteryChannel"
        val channelName = "Battery Notification Channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "STOP_NOTIFICATION"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Battery Level Reached")
            .setContentText("Battery level has reached $level%")
            .setSmallIcon(R.drawable.ic_battery)
            .setVibrate(longArrayOf(0, 500, 1000)) // Set vibrate pattern
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent) // Add stop button
            .build()

        notificationManager.notify(1, notification)
    }

    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    private fun playSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        ringtone?.stop()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1) // Remove the notification
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}