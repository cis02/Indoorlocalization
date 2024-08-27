package com.cis.indoorlocalization

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import kotlin.math.pow

class WifiUtils(private val context: Context) {

    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val smoothingMap = mutableMapOf<String, MutableList<Int>>()

    // Start a Wi-Fi scan
    fun startWifiScan() {
        wifiManager.startScan()
    }

    // Get all available Wi-Fi networks
    fun getAllWifiNetworks(): List<ScanResult> {
        return wifiManager.scanResults
    }

    // Filter Wi-Fi networks by minimum RSSI and optional SSID pattern
    fun filterWifiNetworks(minRssi: Int = -80, ssidPattern: String? = null): List<ScanResult> {
        return getAllWifiNetworks().filter {
            it.level >= minRssi && (ssidPattern?.let { pattern -> it.SSID.contains(pattern) } ?: true)
        }
    }

    // Smooth RSSI values using a simple moving average
    private fun smoothRssi(bssid: String, rssi: Int, windowSize: Int = 5): Int {
        val rssiList = smoothingMap.getOrPut(bssid) { mutableListOf() }

        if (rssiList.size >= windowSize) {
            rssiList.removeAt(0)
        }
        rssiList.add(rssi)

        return rssiList.average().toInt()
    }

    // Get the closest Wi-Fi network based on smoothed signal strength
    fun getClosestWifiNetwork(minRssi: Int = -80, ssidPattern: String? = null): ScanResult? {
        val filteredNetworks = filterWifiNetworks(minRssi, ssidPattern)
        return filteredNetworks.maxByOrNull { smoothRssi(it.BSSID, it.level) }
    }

    // Get the Wi-Fi network with a specific SSID, with optional filtering
    fun getWifiNetworkBySSID(ssid: String, minRssi: Int = -80): ScanResult? {
        return filterWifiNetworks(minRssi, ssid).find { it.SSID == ssid }
    }

    // Get formatted Wi-Fi information (SSID, smoothed signal strength, MAC address)
    fun getFormattedWifiInfo(scanResult: ScanResult): String {
        val smoothedRssi = smoothRssi(scanResult.BSSID, scanResult.level)
        return "SSID: ${scanResult.SSID}\nSignal Strength: $smoothedRssi dBm\nMAC Address: ${scanResult.BSSID}"
    }

    // Collect and return current Wi-Fi fingerprints (SSID, BSSID, smoothed signal strength)
    fun collectWifiFingerprint(minRssi: Int = -80, ssidPattern: String? = null): List<WifiFingerprint> {
        return filterWifiNetworks(minRssi, ssidPattern).map {
            WifiFingerprint(it.SSID, it.BSSID, smoothRssi(it.BSSID, it.level))
        }
    }

    // Estimate the distance from the access point using the log-distance path loss model
    fun estimateDistance(rssi: Int, txPower: Int, pathLossExponent: Double = 2.0): Double {
        return 10.0.pow((txPower - rssi) / (10 * pathLossExponent))
    }

    // Estimate distance to a specific Wi-Fi network by SSID
    fun estimateDistanceToSSID(ssid: String, txPower: Int, minRssi: Int = -80, pathLossExponent: Double = 2.0): Double? {
        val network = getWifiNetworkBySSID(ssid, minRssi)
        return network?.let { estimateDistance(smoothRssi(it.BSSID, it.level), txPower, pathLossExponent) }
    }
}

// Data class to represent Wi-Fi fingerprint
data class WifiFingerprint(val ssid: String, val bssid: String, val rssi: Int)
