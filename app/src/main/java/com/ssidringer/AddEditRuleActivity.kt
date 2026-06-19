package com.ssidringer

import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class AddEditRuleActivity : AppCompatActivity() {

    private lateinit var store: SSIDRuleStore
    private lateinit var ssidInput: EditText
    private lateinit var triggerGroup: RadioGroup
    private lateinit var modeGroup: RadioGroup
    private lateinit var triggerInRange: RadioButton
    private lateinit var triggerOutOfRange: RadioButton
    private lateinit var modeRing: RadioButton
    private lateinit var modeVibrate: RadioButton
    private lateinit var modeSilent: RadioButton
    private var existingRule: SSIDRule? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_rule)

        store = SSIDRuleStore(this)

        ssidInput = findViewById(R.id.ssid_input)
        triggerGroup = findViewById(R.id.trigger_group)
        modeGroup = findViewById(R.id.mode_group)
        triggerInRange = findViewById(R.id.trigger_in_range)
        triggerOutOfRange = findViewById(R.id.trigger_out_of_range)
        modeRing = findViewById(R.id.mode_ring)
        modeVibrate = findViewById(R.id.mode_vibrate)
        modeSilent = findViewById(R.id.mode_silent)
        val saveButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.save_button)
        val deleteButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.delete_button)

        val ruleId = intent.getStringExtra("rule_id")
        existingRule = ruleId?.let { store.getById(it) }

        existingRule?.let { rule ->
            ssidInput.setText(rule.ssid)
            (if (rule.trigger == SSIDRule.IN_RANGE) triggerInRange else triggerOutOfRange).isChecked = true
            when (rule.ringerMode) {
                SSIDRule.RINGER -> modeRing.isChecked = true
                SSIDRule.VIBRATE -> modeVibrate.isChecked = true
                SSIDRule.SILENT -> modeSilent.isChecked = true
            }
            deleteButton.isEnabled = true
        }

        saveButton.setOnClickListener { saveRule() }
        deleteButton.setOnClickListener { confirmDelete() }
    }

    private fun saveRule() {
        val ssid = ssidInput.text.toString().trim()
        if (ssid.isEmpty()) {
            Toast.makeText(this, "SSID cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val trigger = when (triggerGroup.checkedRadioButtonId) {
            triggerInRange.id -> SSIDRule.IN_RANGE
            triggerOutOfRange.id -> SSIDRule.OUT_OF_RANGE
            else -> {
                Toast.makeText(this, "Select a trigger", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val mode = when (modeGroup.checkedRadioButtonId) {
            modeRing.id -> SSIDRule.RINGER
            modeVibrate.id -> SSIDRule.VIBRATE
            modeSilent.id -> SSIDRule.SILENT
            else -> {
                Toast.makeText(this, "Select a ringer mode", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (existingRule == null) {
            val conflict = store.getAll().find {
                it.ssid.equals(ssid, ignoreCase = true) && it.trigger == trigger
            }
            if (conflict != null) {
                val triggerLabel = if (trigger == SSIDRule.IN_RANGE) "in-range" else "out-of-range"
                Toast.makeText(this, "A $triggerLabel rule for \"$ssid\" already exists", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val rule = SSIDRule(
            id = existingRule?.id ?: UUID.randomUUID().toString(),
            ssid = ssid,
            trigger = trigger,
            ringerMode = mode
        )

        store.save(rule)
        setResult(RESULT_OK)
        finish()
    }

    private fun confirmDelete() {
        val rule = existingRule ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Rule")
            .setMessage("Delete rule for \"${rule.ssid}\"?")
            .setPositiveButton("Delete") { _, _ ->
                store.delete(rule.id)
                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
