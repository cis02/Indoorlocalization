package com.cis.indoorlocalization

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager

class WifiUtils(private val context: Context) {

    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    // Start a Wi-Fi scan
    fun startWifiScan() {
        wifiManager.startScan()
    }

    // Get all available Wi-Fi networks
    fun getAllWifiNetworks(): List<ScanResult> {
        return wifiManager.scanResults
    }

    // Get the closest Wi-Fi network based on signal strength
    fun getClosestWifiNetwork(): ScanResult? {
        return getAllWifiNetworks().maxByOrNull { it.level }
    }

    // Get the Wi-Fi network with a specific SSID
    fun getWifiNetworkBySSID(ssid: String): ScanResult? {
        return getAllWifiNetworks().find { it.SSID == ssid }
    }

    // Get formatted Wi-Fi information (signal strength and MAC address)
    fun getFormattedWifiInfo(scanResult: ScanResult): String {
        return "SSID: ${scanResult.SSID}\nSignal Strength: ${scanResult.level} dBm\nMAC Address: ${scanResult.BSSID}"
    }
}
