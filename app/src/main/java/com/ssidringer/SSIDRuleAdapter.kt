package com.ssidringer

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SSIDRuleAdapter(
    private val onEdit: (SSIDRule) -> Unit,
    private val onDelete: (SSIDRule) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onToggleEnabled: (SSIDRule, Boolean) -> Unit
) : RecyclerView.Adapter<SSIDRuleAdapter.ViewHolder>() {

    private val rules = mutableListOf<SSIDRule>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ssid_rule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rules[position], position)
    }

    override fun getItemCount(): Int = rules.size

    fun submitList(list: List<SSIDRule>) {
        rules.clear()
        rules.addAll(list)
        notifyDataSetChanged()
    }

    fun getRules(): List<SSIDRule> = rules.toList()

    fun moveItem(from: Int, to: Int) {
        val item = rules.removeAt(from)
        rules.add(to, item)
        notifyItemMoved(from, to)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dragHandle: View = itemView.findViewById(R.id.drag_handle)
        private val ruleNumber: TextView = itemView.findViewById(R.id.rule_number)
        private val ssidText: TextView = itemView.findViewById(R.id.ssid_text)
        private val triggerText: TextView = itemView.findViewById(R.id.trigger_text)
        private val modeText: TextView = itemView.findViewById(R.id.mode_text)
        private val ruleSwitch: SwitchMaterial = itemView.findViewById(R.id.rule_switch)
        private val editBtn: MaterialButton = itemView.findViewById(R.id.edit_button)
        private val deleteBtn: MaterialButton = itemView.findViewById(R.id.delete_button)

        fun bind(rule: SSIDRule, position: Int) {
            ruleNumber.text = (position + 1).toString()
            ssidText.text = rule.ssid
            triggerText.text = SSIDRule.triggerName(rule.trigger)
            modeText.text = SSIDRule.ringerModeName(rule.ringerMode)
            ruleSwitch.isChecked = rule.enabled
            val alpha = if (rule.enabled) 1.0f else 0.5f
            itemView.alpha = alpha

            editBtn.setOnClickListener { onEdit(rule) }
            deleteBtn.setOnClickListener { onDelete(rule) }
            ruleSwitch.setOnCheckedChangeListener(null)
            ruleSwitch.setOnCheckedChangeListener { _, isChecked ->
                onToggleEnabled(rule, isChecked)
            }
            dragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }
        }
    }
}
