package com.cis.indoorlocalization

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var wifiUtils: WifiUtils
    private lateinit var wifiSSIDTextView: TextView
    private lateinit var wifiInfoTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val wifiUpdateRunnable = object : Runnable {
        override fun run() {
            updateWifiInfo()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiUtils = WifiUtils(this)
        wifiSSIDTextView = findViewById(R.id.wifiSSIDTextView)
        wifiInfoTextView = findViewById(R.id.wifiInfoTextView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            startWifiUpdates()
        }

        checkAndPromptForWifiThrottling()
    }

    private fun startWifiUpdates() {
        handler.post(wifiUpdateRunnable)
    }

    private fun stopWifiUpdates() {
        handler.removeCallbacks(wifiUpdateRunnable)
    }

    private fun updateWifiInfo() {
        try {
            wifiUtils.startWifiScan()
            val closestNetwork = wifiUtils.getClosestWifiNetwork()

            if (closestNetwork != null) {
                wifiSSIDTextView.text = "SSID: ${closestNetwork.SSID}"
                wifiInfoTextView.text = wifiUtils.getFormattedWifiInfo(closestNetwork)
            } else {
                wifiSSIDTextView.text = "SSID: N/A"
                wifiInfoTextView.text = "No Wi-Fi networks found"
            }
        } catch (e: SecurityException) {
            wifiInfoTextView.text = "Failed to retrieve Wi-Fi information. Permission might be missing."
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startWifiUpdates()
            } else {
                wifiInfoTextView.text = "Permission denied"
            }
        }
    }

    private fun checkAndPromptForWifiThrottling() {
        val sharedPreferences = getSharedPreferences("wifi_prefs", Context.MODE_PRIVATE)
        val hasPromptedForThrottling = sharedPreferences.getBoolean("prompted_for_throttling", false)

        if (!hasPromptedForThrottling) {
            AlertDialog.Builder(this)
                .setTitle("Disable Wi-Fi Throttling")
                .setMessage("To get real-time Wi-Fi updates, please disable Wi-Fi scan throttling in Developer Options.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()

            sharedPreferences.edit().putBoolean("prompted_for_throttling", true).apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWifiUpdates()
    }

    fun onRouteSelectorClicked(view: View) {
        val intent = Intent(this, RouteSelectorActivity::class.java)
        startActivity(intent)
    }

    fun onMapSelectorClicked(view: View) {
        val intent = Intent(this, MapSelectorActivity::class.java)
        startActivity(intent)
    }

    fun onMapBuilderClicked(view: View) {
        val intent = Intent(this, MapBuilderActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1
    }
}
