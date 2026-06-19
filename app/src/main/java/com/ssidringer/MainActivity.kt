package com.ssidringer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var store: SSIDRuleStore
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: SSIDRuleAdapter
    private lateinit var rulesList: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var serviceStatus: TextView
    private lateinit var serviceToggle: SwitchMaterial
    private var serviceBound = false
    private var serviceIntent: Intent? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            checkPermissionsAndStartService()
        }

    private val notificationPolicyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            checkPermissionsAndStartService()
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) checkPermissionsAndStartService()
        }

    private val addEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                refreshList()
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBound = true
            updateServiceStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            updateServiceStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = SSIDRuleStore(this)
        prefs = getSharedPreferences("ssid_ringer_prefs", Context.MODE_PRIVATE)

        rulesList = findViewById(R.id.rules_list)
        emptyState = findViewById(R.id.empty_state)
        serviceStatus = findViewById(R.id.service_status)
        serviceToggle = findViewById(R.id.service_toggle)
        val fabAdd = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add)

        rulesList.layoutManager = LinearLayoutManager(this)

        val touchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                store.saveAll(adapter.getRules())
                refreshList()
            }
        }
        val itemTouchHelper = ItemTouchHelper(touchHelperCallback)
        itemTouchHelper.attachToRecyclerView(rulesList)

        adapter = SSIDRuleAdapter(
            onEdit = { rule -> openAddEdit(rule) },
            onDelete = { rule -> confirmDelete(rule) },
            onStartDrag = { viewHolder -> itemTouchHelper.startDrag(viewHolder) },
            onToggleEnabled = { rule, enabled ->
                store.save(rule.copy(enabled = enabled))
            }
        )
        rulesList.adapter = adapter

        fabAdd.setOnClickListener { openAddEdit(null) }

        serviceToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_SERVICE_ENABLED, isChecked).apply()
            if (isChecked) {
                requestAllPermissions()
            } else {
                stopService()
            }
        }

        if (prefs.getBoolean(PREF_SERVICE_ENABLED, false)) {
            serviceToggle.isChecked = true
        }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
        requestMissingPermissions()
        if (prefs.getBoolean(PREF_SERVICE_ENABLED, false) && !serviceBound) {
            startService()
        }
        updateServiceStatus()
    }

    private fun refreshList() {
        val rules = store.getAll()
        adapter.submitList(rules)
        emptyState.visibility = if (rules.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun openAddEdit(rule: SSIDRule?) {
        val intent = Intent(this, AddEditRuleActivity::class.java)
        rule?.let { intent.putExtra("rule_id", it.id) }
        addEditLauncher.launch(intent)
    }

    private fun confirmDelete(rule: SSIDRule) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Rule")
            .setMessage("Delete rule for \"${rule.ssid}\"?")
            .setPositiveButton("Delete") { _, _ ->
                store.delete(rule.id)
                refreshList()
                Snackbar.make(rulesList, "Rule deleted", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestAllPermissions() {
        val needsFineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        val needsBackgroundLocation = Build.VERSION.SDK_INT >= 30 &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED

        val needsNearby = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) !=
                PackageManager.PERMISSION_GRANTED

        val needsNotification = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED

        val needsPolicy = !isNotificationPolicyGranted()

        if (needsNotification) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        val permissionsToRequest = mutableListOf<String>()
        if (needsFineLocation) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (needsBackgroundLocation) {
            permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (needsNearby) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        if (permissionsToRequest.isNotEmpty()) {
            locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            return
        }

        if (needsPolicy) {
            notificationPolicyLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }

        startService()
    }

    private fun checkPermissionsAndStartService() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            serviceToggle.isChecked = false
            Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            serviceToggle.isChecked = false
            Toast.makeText(this, "Location permission required for WiFi scanning", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) !=
                PackageManager.PERMISSION_GRANTED) {
            serviceToggle.isChecked = false
            Toast.makeText(this, "Nearby devices permission required", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isNotificationPolicyGranted()) {
            serviceToggle.isChecked = false
            Toast.makeText(this, "Notification policy access required to change ringer", Toast.LENGTH_SHORT).show()
            return
        }
        startService()
    }

    private fun requestMissingPermissions() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        val locationPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            locationPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            locationPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= 30 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            locationPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (locationPermissions.isNotEmpty()) {
            locationPermissionLauncher.launch(locationPermissions.toTypedArray())
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) !=
                PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES))
            return
        }
        if (!isNotificationPolicyGranted()) {
            notificationPolicyLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    private fun isNotificationPolicyGranted(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun startService() {
        if (serviceIntent == null) {
            serviceIntent = Intent(this, SSIDRingerService::class.java)
            startForegroundService(serviceIntent!!)
        }
        if (!serviceBound) {
            bindService(serviceIntent!!, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        serviceStatus.text = "Monitoring Active"
    }

    private fun stopService() {
        try {
            if (serviceBound) {
                unbindService(serviceConnection)
            }
            serviceBound = false
            serviceIntent?.let { stopService(it) }
            serviceIntent = null
        } catch (_: Exception) {
            serviceBound = false
        }
        serviceStatus.text = "Monitoring Stopped"
    }

    private fun updateServiceStatus() {
        serviceStatus.text = if (serviceBound) "Monitoring Active" else "Monitoring Stopped"
    }

    companion object {
        private const val PREF_SERVICE_ENABLED = "service_enabled"
    }
}
