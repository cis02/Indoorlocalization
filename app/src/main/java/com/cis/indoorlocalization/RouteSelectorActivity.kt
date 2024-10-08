package com.cis.indoorlocalization

import android.graphics.PointF
import android.graphics.BitmapFactory
import android.net.wifi.ScanResult
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileReader
import java.io.IOException
import kotlin.math.pow

class RouteSelectorActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var frameLayout: FrameLayout
    private lateinit var tvMapTitle: TextView
    private val markers = mutableListOf<MarkerData>()
    private lateinit var wifiUtils: WifiUtils
    private var currentLocationMarker: MarkerData? = null
    private var destinationMarker: MarkerData? = null

    private val handler = Handler(Looper.getMainLooper())
    private val checkLocationRunnable = object : Runnable {
        override fun run() {
            if (destinationMarker != null) {
                checkCurrentLocation()
            }
            handler.postDelayed(this, 5000) // Check every 5 seconds
        }
    }
    private fun calculateDistance(p1: PointF, p2: PointF): Float {
        return kotlin.math.sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }

    private var edges: MutableMap<Pair<String, String>, Float> = mutableMapOf()

    private fun setupGraph() {
        edges.clear()  // Clear previous edges
        markers.forEach { start ->
            markers.forEach { end ->
                if (start != end) {  // Ensure no node connects to itself
                    val distance = calculateDistance(start.position, end.position)
                    edges[Pair(start.name, end.name)] = distance
                }
            }
        }
    }


    private fun dijkstra(startName: String, endName: String): List<String> {
        val shortestPaths = mutableMapOf<String, Float>().withDefault { Float.MAX_VALUE }
        val previousNodes = mutableMapOf<String, String?>()
        val unvisited = markers.map { it.name }.toMutableSet()

        shortestPaths[startName] = 0f

        while (unvisited.isNotEmpty()) {
            val current = unvisited.minByOrNull { shortestPaths.getValue(it) } ?: break
            unvisited.remove(current)

            markers.forEach { neighbor ->
                if (neighbor.name != current) {
                    val edgeKey = Pair(current, neighbor.name)
                    val distance = edges[edgeKey]!!
                    val newDistance = shortestPaths.getValue(current) + distance
                    if (newDistance < shortestPaths.getValue(neighbor.name)) {
                        shortestPaths[neighbor.name] = newDistance
                        previousNodes[neighbor.name] = current
                    }
                }
            }
        }

        return generatePath(previousNodes, startName, endName)
    }


    private fun generatePath(previousNodes: Map<String, String?>, startName: String, endName: String): List<String> {
        var currentName = endName
        val path = mutableListOf<String>()
        while (currentName != startName) {
            path.add(currentName)
            currentName = previousNodes[currentName] ?: break
        }
        path.add(startName)
        path.reverse()
        return path
    }

    private fun displayPath(path: List<String>) {
        val pathTextView = findViewById<TextView>(R.id.pathTextView)
        pathTextView.text = "Follow this path: " + path.joinToString(" -> ")
    }


    private fun checkCurrentLocation() {
        wifiUtils.startWifiScan()
        val currentWifiResults = wifiUtils.getStrongestWifiNetworks(3)

        Log.d("RouteSelectorActivity", "Checking current location with ${currentWifiResults.size} networks")

        currentWifiResults.forEach { scanResult ->
            Log.d("RouteSelectorActivity", "Checking network SSID=${scanResult.SSID}, BSSID=${scanResult.BSSID}, RSSI=${scanResult.level}")
            if (destinationMarker != null && isMatchingWifiData(scanResult, destinationMarker!!.wifiData)) {
                Log.d("RouteSelectorActivity", "Match found with destination marker!")
                onArrivalAtDestination()
                return
            }
        }

        Log.d("RouteSelectorActivity", "No matching Wi-Fi network found for destination marker")
    }


    private fun onArrivalAtDestination() {
        showArrivalDialog(destinationMarker?.name ?: "your destination")
        resetMap()
    }
    private fun showArrivalDialog(destinationName: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_arrival, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvArrivalMessage)
        tvMessage.text = "You have arrived at $destinationName!"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)  // Optional: makes it so the dialog can't be dismissed by pressing outside it
            .create()

        dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
            resetMap()  // Reset the map once the dialog is closed
        }

        dialog.show()
    }


    private fun resetMap() {
        currentLocationMarker = null
        destinationMarker = null
        updateMarkersOnMap()  // Refresh the map to only show normal markers
        stopLocationChecks()  // Stop checking location
    }

    private fun startLocationChecks() {
        handler.post(checkLocationRunnable)
    }

    private fun stopLocationChecks() {
        handler.removeCallbacks(checkLocationRunnable)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_selector)

        imageView = findViewById(R.id.imageView)
        frameLayout = findViewById(R.id.frameLayout)
        tvMapTitle = findViewById(R.id.tvMapTitle)
        wifiUtils = WifiUtils(this)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish() // Go back to the previous activity
        }

        findViewById<Button>(R.id.btnLocate).setOnClickListener {
            locateCurrentPosition()
        }

        loadTitle()
        loadImage()
        loadMarkersFromCSV()

        imageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                imageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                updateMarkersOnMap()
            }
        })
    }

    private fun loadTitle() {
        val file = File(getMapDirectory(), MapBuilderActivity.TITLE_FILE_NAME)
        if (file.exists()) {
            try {
                FileReader(file).use { reader ->
                    val title = reader.readText()
                    tvMapTitle.text = title
                }
                Log.d("RouteSelectorActivity", "Title loaded successfully: ${file.absolutePath}")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("RouteSelectorActivity", "Error loading title: ${e.message}")
            }
        } else {
            Log.e("RouteSelectorActivity", "Title file does not exist: ${file.absolutePath}")
        }
    }

    private fun loadImage() {
        val file = File(getMapDirectory(), MapBuilderActivity.IMAGE_FILE_NAME)
        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    Log.d("RouteSelectorActivity", "Image loaded successfully: ${file.absolutePath}")
                } else {
                    Log.e("RouteSelectorActivity", "Failed to decode image: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("RouteSelectorActivity", "Error loading image: ${e.message}")
            }
        } else {
            Log.e("RouteSelectorActivity", "Image file does not exist: ${file.absolutePath}")
        }
    }

    private fun loadMarkersFromCSV() {
        val file = File(getMapDirectory(), MapBuilderActivity.MARKERS_FILE_NAME)
        if (file.exists()) {
            try {
                FileReader(file).use { reader ->
                    reader.readLines().forEach { line ->
                        val parts = line.split(",")
                        if (parts.size >= 4) {
                            val x = parts[0].toFloat()
                            val y = parts[1].toFloat()
                            val name = parts[2]
                            val wifiData = parts[3]
                            markers.add(MarkerData(PointF(x, y), name, wifiData))
                        }
                    }
                }
                Log.d("RouteSelectorActivity", "Markers loaded successfully: ${file.absolutePath}")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("RouteSelectorActivity", "Error loading markers: ${e.message}")
            }
        } else {
            Log.e("RouteSelectorActivity", "Markers file does not exist: ${file.absolutePath}")
        }
        if (markers.isNotEmpty()) {
            setupGraph()  // Only setup graph if markers are loaded
        } else {
            Log.e("RouteSelectorActivity", "No markers loaded.")
        }
    }

    private fun updateMarkersOnMap() {
        //frameLayout.removeAllViews()

        markers.forEach { marker ->
            val markerView = View.inflate(this, R.layout.marker_view, null)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            val actualX = marker.position.x * imageView.width
            val actualY = marker.position.y * imageView.height

            layoutParams.leftMargin = actualX.toInt() - 10
            layoutParams.topMargin = actualY.toInt() - 10

            markerView.setOnClickListener {
                if (currentLocationMarker == null) {
                    Toast.makeText(this, "Locate your position first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Proceed if currentLocationMarker is not null
                if (destinationMarker != null) {
                    updateMarkersOnMap()
                }

                destinationMarker = marker
                val path = dijkstra(currentLocationMarker!!.name, destinationMarker!!.name)
                displayPath(path)
                val destinationView = View.inflate(this, R.layout.marker_dest, null)
                frameLayout.addView(destinationView, layoutParams)
                Log.d("RouteSelectorActivity", "Destination marker set: ${marker.name}")
                startLocationChecks()
            }


            frameLayout.addView(markerView, layoutParams)

            // Optionally, add name views as before
            if (marker.name.isNotEmpty()) {
                val nameView = TextView(this).apply {
                    text = marker.name
                    setBackgroundResource(android.R.color.transparent)
                }
                val nameLayoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                nameLayoutParams.leftMargin = actualX.toInt() - 10
                nameLayoutParams.topMargin = actualY.toInt() - 30
                frameLayout.addView(nameView, nameLayoutParams)
            }
        }

        // Highlight the current location
        currentLocationMarker?.let {
            val currentLocationView = View.inflate(this, R.layout.marker_loc, null)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            val actualX = it.position.x * imageView.width
            val actualY = it.position.y * imageView.height

            layoutParams.leftMargin = actualX.toInt() - 10
            layoutParams.topMargin = actualY.toInt() - 10

            frameLayout.addView(currentLocationView, layoutParams)
        }

        // Highlight the destination marker
        destinationMarker?.let {
            val destinationView = View.inflate(this, R.layout.marker_dest, null)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            val actualX = it.position.x * imageView.width
            val actualY = it.position.y * imageView.height

            layoutParams.leftMargin = actualX.toInt() - 10
            layoutParams.topMargin = actualY.toInt() - 10

            frameLayout.addView(destinationView, layoutParams)
        }
    }

    private fun locateCurrentPosition() {
        wifiUtils.startWifiScan()
        val currentWifiResults = wifiUtils.getAllWifiNetworks()

        Log.d("RouteSelectorActivity", "Current Wi-Fi Results: ${currentWifiResults.size} networks found.")

        currentWifiResults.forEach { scanResult ->
            Log.d("RouteSelectorActivity", "Found Wi-Fi: SSID=${scanResult.SSID}, BSSID=${scanResult.BSSID}, RSSI=${scanResult.level} dBm")
        }

        // Iterate over all markers
        for (marker in markers) {
            if (marker.wifiData.isBlank()) {
                Log.w("RouteSelectorActivity", "Skipping marker with missing or malformed Wi-Fi data: ${marker.name}")
                continue
            }

            // Check each scan result to see if it matches this marker
            for (scanResult in currentWifiResults) {
                if (isMatchingWifiData(scanResult, marker.wifiData)) {
                    Log.d("RouteSelectorActivity", "Matched marker: ${marker.name}")
                    highlightCurrentLocation(marker)
                    return // Exit the function immediately after finding the match
                }
            }
        }

        // If no match was found
        Log.e("RouteSelectorActivity", "No matching marker found for the current Wi-Fi network")
        Toast.makeText(this, "Current location not found on map", Toast.LENGTH_SHORT).show()
    }




    private fun isMatchingWifiData(scanResult: ScanResult, markerWifiData: String): Boolean {
        val parsedData = parseMarkerWifiData(markerWifiData)

        val matches = scanResult.SSID.trim('"') == parsedData["SSID"] &&
                scanResult.BSSID == parsedData["BSSID"] &&
                kotlin.math.abs(scanResult.level - (parsedData["RSSI"]?.toInt() ?: 0)) <= 10  // Increased RSSI tolerance

        if (!matches) {
            Log.d("RouteSelectorActivity", "Failed match: SSID=${parsedData["SSID"]} vs ${scanResult.SSID}, " +
                    "BSSID=${parsedData["BSSID"]} vs ${scanResult.BSSID}, " +
                    "RSSI Diff=${kotlin.math.abs(scanResult.level - (parsedData["RSSI"]?.toInt() ?: 0))}")
        }

        return matches
    }


    private fun parseMarkerWifiData(wifiData: String): Map<String, String> {
        val result = mutableMapOf<String, String>()

        // Extract SSID, RSSI, and BSSID using regex patterns
        val ssidRegex = "SSID: (.*?) Signal Strength".toRegex()
        val rssiRegex = "Signal Strength: (.*?) dBm".toRegex()
        val bssidRegex = "MAC Address: (.*)".toRegex()

        ssidRegex.find(wifiData)?.let { result["SSID"] = it.groupValues[1].trim() }
        rssiRegex.find(wifiData)?.let { result["RSSI"] = it.groupValues[1].trim() }
        bssidRegex.find(wifiData)?.let { result["BSSID"] = it.groupValues[1].trim() }

        return result
    }

    private fun highlightCurrentLocation(marker: MarkerData) {
        currentLocationMarker = marker  // Set the current location marker

        updateMarkersOnMap()  // You might want to call this before setting the marker to ensure it doesn't reset.

        val currentLocationView = View.inflate(this, R.layout.marker_loc, null)
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        val actualX = marker.position.x * imageView.width
        val actualY = marker.position.y * imageView.height

        layoutParams.leftMargin = actualX.toInt() - 10
        layoutParams.topMargin = actualY.toInt() - 10

        frameLayout.addView(currentLocationView, layoutParams)
    }

    private fun getMapDirectory(): File {
        return File(filesDir, MapBuilderActivity.MAP_DIRECTORY).apply {
            if (!exists()) mkdirs()
        }
    }
}
