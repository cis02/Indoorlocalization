package com.cis.indoorlocalization

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.view.View

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
}