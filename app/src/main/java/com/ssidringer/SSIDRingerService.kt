package com.ssidringer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class SSIDRingerService : Service() {

    private lateinit var wifiManager: WifiManager
    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var scanning = false
    private val previouslySeenSsids = mutableSetOf<String>()

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                Log.d(TAG, "Scan results available, success=$success")
                if (success) {
                    checkForMatchingSSIDs()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Monitoring nearby WiFi networks"))
        try {
            unregisterReceiver(scanReceiver)
        } catch (_: IllegalArgumentException) {}
        registerReceiver(scanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        startPeriodicScan()
        Log.d(TAG, "Service started, scanning initiated")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { unregisterReceiver(scanReceiver) } catch (_: Exception) {}
        handler.removeCallbacksAndMessages(null)
        previouslySeenSsids.clear()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    private fun startPeriodicScan() {
        scanning = true
        scanNow()
        scheduleNextScan()
    }

    private fun scheduleNextScan() {
        handler.postDelayed({
            if (scanning) {
                scanNow()
                scheduleNextScan()
            }
        }, SCAN_INTERVAL_MS)
    }

    private fun scanNow() {
        try {
            wifiManager.startScan()
            Log.d(TAG, "Scan initiated")
        } catch (e: SecurityException) {
            Log.e(TAG, "Scan failed: missing location permission", e)
        }
    }

    private fun checkForMatchingSSIDs() {
        val store = SSIDRuleStore(this)
        val rules = store.getAll().filter { it.enabled }
        if (rules.isEmpty()) {
            Log.d(TAG, "No enabled rules configured")
            return
        }

        val currentlySeenSsids = getVisibleSsids()
        if (currentlySeenSsids == null) {
            Log.w(TAG, "Failed to get visible SSIDs")
            return
        }
        Log.d(TAG, "Visible SSIDs: $currentlySeenSsids, previously: $previouslySeenSsids")

        if (currentlySeenSsids.isEmpty() && previouslySeenSsids.isEmpty()) {
            Log.d(TAG, "No SSIDs seen yet, waiting for first scan")
            return
        }

        val inRangeRules = rules.filter { it.trigger == SSIDRule.IN_RANGE }
        val outOfRangeRules = rules.filter { it.trigger == SSIDRule.OUT_OF_RANGE }

        for (rule in inRangeRules) {
            val normalized = rule.ssid.trim().lowercase()
            val isNewInRange = currentlySeenSsids.contains(normalized) && !previouslySeenSsids.contains(normalized)
            Log.d(TAG, "IN_RANGE rule for '${rule.ssid}': seen=${currentlySeenSsids.contains(normalized)}, wasSeen=${previouslySeenSsids.contains(normalized)}, isNew=$isNewInRange")
            if (isNewInRange) {
                Log.i(TAG, "Firing IN_RANGE rule: '${rule.ssid}' -> ${SSIDRule.ringerModeName(rule.ringerMode)}")
                RingerModeManager.setRingerMode(this, rule.ringerMode)
            }
        }

        val anyInRangeActive = inRangeRules.any { rule ->
            currentlySeenSsids.contains(rule.ssid.trim().lowercase())
        }
        Log.d(TAG, "Any IN_RANGE active: $anyInRangeActive")

        if (!anyInRangeActive) {
            for (rule in outOfRangeRules) {
                val normalized = rule.ssid.trim().lowercase()
                val isNewOutOfRange = !currentlySeenSsids.contains(normalized) && previouslySeenSsids.contains(normalized)
                Log.d(TAG, "OUT_OF_RANGE rule for '${rule.ssid}': seen=${currentlySeenSsids.contains(normalized)}, wasSeen=${previouslySeenSsids.contains(normalized)}, isNew=$isNewOutOfRange")
                if (isNewOutOfRange) {
                    Log.i(TAG, "Firing OUT_OF_RANGE rule: '${rule.ssid}' -> ${SSIDRule.ringerModeName(rule.ringerMode)}")
                    RingerModeManager.setRingerMode(this, rule.ringerMode)
                }
            }
        }

        previouslySeenSsids.clear()
        previouslySeenSsids.addAll(currentlySeenSsids)
    }

    private fun getVisibleSsids(): Set<String>? {
        val scanResults = try {
            wifiManager.scanResults
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to get scan results: missing location permission", e)
            return null
        }

        val ssids = scanResults
            .map { stripQuotes(it.SSID) }
            .filter { it.isNotEmpty() }
            .map { it.lowercase() }
            .toMutableSet()

        val connectedInfo = wifiManager.connectionInfo
        val connectedSsid = stripQuotes(connectedInfo.ssid)
        if (connectedSsid.isNotEmpty() && connectedSsid != "<unknown ssid>") {
            ssids.add(connectedSsid.lowercase())
        }

        Log.d(TAG, "Scan results: ${ssids.size} SSIDs visible")
        return ssids
    }

    private fun stripQuotes(ssid: String): String {
        return ssid.trim('"').trim()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SSID Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors nearby WiFi networks for configured rules"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Loud Vibrate")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "SSIDRinger"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ssid_monitor"
        private const val SCAN_INTERVAL_MS = 30_000L
    }
}
