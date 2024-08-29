package com.cis.indoorlocalization

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MarkerEditActivity : AppCompatActivity() {

    private lateinit var etMarkerName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnGatherWifi: Button
    private lateinit var wifiTextView: TextView

    private var markerData: MarkerData? = null
    private var markerIndex: Int = -1

    private lateinit var wifiUtils: WifiUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_edit)

        etMarkerName = findViewById(R.id.etMarkerName)
        btnSave = findViewById(R.id.btnSave)
        btnGatherWifi = findViewById(R.id.btnGatherWifi)
        wifiTextView = findViewById(R.id.wifiTextView)

        wifiUtils = WifiUtils(this)  // Initialize WifiUtils

        markerIndex = intent.getIntExtra("markerIndex", -1)
        markerData = intent.getParcelableExtra("markerData")

        markerData?.let {
            etMarkerName.setText(it.name)
            wifiTextView.text = it.wifiData
        }

        btnSave.setOnClickListener {
            markerData?.let { data ->
                data.name = etMarkerName.text.toString()
                data.wifiData = wifiTextView.text.toString()
                saveMarkerData()
            }
            finish()
        }

        btnGatherWifi.setOnClickListener {
            gatherWifiData()
        }
    }

    private fun gatherWifiData() {
        wifiUtils.startWifiScan()  // Start a Wi-Fi scan

        // After the scan, get the closest Wi-Fi network
        val closestWifi = wifiUtils.getClosestWifiNetwork()

        // If a network is found, display its info in the TextView
        closestWifi?.let {
            val wifiInfo = wifiUtils.getClosestWifioneLine(it)
            wifiTextView.text = wifiInfo
        } ?: run {
            wifiTextView.text = "No Wi-Fi network found"
        }
    }

    private fun saveMarkerData() {
        val resultIntent = intent.apply {
            putExtra("markerIndex", markerIndex)
            putExtra("markerData", markerData)
        }
        setResult(RESULT_OK, resultIntent)
    }
}
