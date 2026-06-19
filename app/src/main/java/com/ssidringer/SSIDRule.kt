package com.ssidringer

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SSIDRule(
    val id: String = UUID.randomUUID().toString(),
    val ssid: String,
    val trigger: Int,
    val ringerMode: Int,
    val enabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("ssid", ssid)
        put("trigger", trigger)
        put("ringerMode", ringerMode)
        put("enabled", enabled)
    }

    companion object {
        const val IN_RANGE = 0
        const val OUT_OF_RANGE = 1
        const val RINGER = 0
        const val VIBRATE = 1
        const val SILENT = 2

        fun fromJson(json: JSONObject): SSIDRule = SSIDRule(
            id = json.getString("id"),
            ssid = json.getString("ssid"),
            trigger = json.optInt("trigger", IN_RANGE),
            ringerMode = json.optInt("ringerMode", json.optInt("inRangeMode", RINGER)),
            enabled = json.optBoolean("enabled", true)
        )

        fun encodeList(rules: List<SSIDRule>): String = JSONArray().apply {
            rules.forEach { put(it.toJson()) }
        }.toString()

        fun decodeList(json: String): List<SSIDRule> {
            val list = mutableListOf<SSIDRule>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                list.add(fromJson(array.getJSONObject(i)))
            }
            return list
        }

        fun triggerName(trigger: Int): String = when (trigger) {
            IN_RANGE -> "Comes in range"
            OUT_OF_RANGE -> "Goes out of range"
            else -> "Unknown"
        }

        fun ringerModeName(mode: Int): String = when (mode) {
            RINGER -> "Ring"
            VIBRATE -> "Vibrate"
            SILENT -> "Silent"
            else -> "Unknown"
        }
    }
}
