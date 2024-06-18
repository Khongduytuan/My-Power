package com.eagletech.mypower

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.eagletech.mypower.data.ManagerData
import com.eagletech.mypower.databinding.ActivityMainBinding
import com.eagletech.mypower.service.BatteryService
import com.ramotion.circlemenu.CircleMenuView


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var batteryLevel: Int = 30
    private lateinit var myData: ManagerData

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        myData = ManagerData.getInstance(this)
        // Kiểm tra và yêu cầu quyền truy cập vị trí
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        binding.circleMenu.eventListener = object : CircleMenuView.EventListener() {
            override fun onMenuOpenAnimationStart(view: CircleMenuView) {
                Log.d("D", "onMenuOpenAnimationStart")
            }

            override fun onMenuOpenAnimationEnd(view: CircleMenuView) {
                Log.d("D", "onMenuOpenAnimationEnd")
            }

            override fun onMenuCloseAnimationStart(view: CircleMenuView) {
                Log.d("D", "onMenuCloseAnimationStart")
            }

            override fun onMenuCloseAnimationEnd(view: CircleMenuView) {
                Log.d("D", "onMenuCloseAnimationEnd")
            }

            override fun onButtonClickAnimationStart(view: CircleMenuView, index: Int) {
                if (index == 0) {
                    Log.d("D", "onButtonClickAnimationStart| index: $index")
                    val intent = Intent(this@MainActivity, BuyActivity::class.java)
                    startActivity(intent)
                } else if (index == 1) {
                    showInfoDialog()
                    Log.d("D", "onButtonClickAnimationStart| index: $index")
                }
            }

            override fun onButtonClickAnimationEnd(view: CircleMenuView, index: Int) {
                Log.d("D", "onButtonClickAnimationEnd| index: $index")
            }
        };

        binding.editTextBatteryLevel.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                batteryLevel = progress
                binding.textViewSeekBarValue.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Do nothing
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Do nothing
            }
        })
        binding.buttonSetBatteryLevel.setOnClickListener {
            if (myData.isPremium == true){
                setBattery()
            } else if(myData.getData() > 0){
                setBattery()
                myData.removeData()
            } else{
                Toast.makeText(this, "Your turn has expired", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, BuyActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun setBattery() {
        if (batteryLevel in 1..100) {
            val sharedPreferences = getSharedPreferences("BatteryPrefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putInt("battery_level", batteryLevel)
                apply()
            }
            startBatteryService()
            Toast.makeText(this, "Battery level set to $batteryLevel%", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(
                this,
                "Please enter a valid battery level (1-100)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra("level", -1) ?: return
            val scale = intent.getIntExtra("scale", -1)
            val batteryPct = level / scale.toFloat() * 100
            val bettery = batteryPct.toInt()
            binding.tvPower.text = "$bettery%"
            binding.circularProgressBar.apply {
                progressMax = 100f
                setProgressWithAnimation(batteryPct, 1500)
            }
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo: WifiInfo = wifiManager.connectionInfo
            val level = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
            binding.textViewWifiInfo.text = "WiFi Signal Strength: $level/4"
        }
    }

    private fun startBatteryService() {
        val intent = Intent(this, BatteryService::class.java)
        startService(intent)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val wifiFilter = IntentFilter(WifiManager.RSSI_CHANGED_ACTION).apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        registerReceiver(wifiReceiver, wifiFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
        unregisterReceiver(wifiReceiver)
    }

    // Xử lý kết quả yêu cầu quyền
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Quyền truy cập vị trí đã được cấp
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Quyền truy cập vị trí bị từ chối
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_info, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        val positiveButton = dialogView.findViewById<Button>(R.id.btnPositive)

        if (myData.isPremium == true) {
            messageTextView.text = "You have successfully registered"
        } else {
            messageTextView.text = "You have ${myData.getData()} use"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        positiveButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
