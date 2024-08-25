package com.cis.indoorlocalization

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MarkerEditActivity : AppCompatActivity() {

    private lateinit var etMarkerName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnGatherWifi: Button

    private var markerData: MarkerData? = null
    private var markerIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_edit)

        etMarkerName = findViewById(R.id.etMarkerName)
        btnSave = findViewById(R.id.btnSave)
        btnGatherWifi = findViewById(R.id.btnGatherWifi)

        // Get marker data from intent
        markerIndex = intent.getIntExtra("markerIndex", -1)
        markerData = intent.getParcelableExtra("markerData")

        markerData?.let {
            etMarkerName.setText(it.name)
        }

        btnSave.setOnClickListener {
            markerData?.name = etMarkerName.text.toString()
            saveMarkerData()
            finish() // Go back to the map
        }

        btnGatherWifi.setOnClickListener {
            // Logic to gather Wi-Fi data will go here
        }
    }

    private fun saveMarkerData() {
        // Save the updated marker data back to the MapBuilderActivity
        val resultIntent = intent.apply {
            putExtra("markerIndex", markerIndex)
            putExtra("markerData", markerData)
        }
        setResult(RESULT_OK, resultIntent)
    }
}
