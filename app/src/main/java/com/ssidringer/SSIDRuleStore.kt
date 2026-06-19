package com.ssidringer

import android.content.Context
import android.content.SharedPreferences

class SSIDRuleStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<SSIDRule> {
        val json = prefs.getString(KEY_RULES, null) ?: return emptyList()
        return SSIDRule.decodeList(json)
    }

    fun getById(id: String): SSIDRule? = getAll().find { it.id == id }

    fun save(rule: SSIDRule) {
        val rules = getAll().toMutableList()
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            rules[index] = rule
        } else {
            rules.add(rule)
        }
        saveAll(rules)
    }

    fun delete(id: String) {
        val rules = getAll().toMutableList()
        rules.removeAll { it.id == id }
        saveAll(rules)
    }

    fun saveAll(rules: List<SSIDRule>) {
        prefs.edit().putString(KEY_RULES, SSIDRule.encodeList(rules)).apply()
    }

    companion object {
        private const val PREFS_NAME = "ssid_ringer_prefs"
        private const val KEY_RULES = "ssid_rules"
    }
}
