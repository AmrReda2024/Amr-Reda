package com.amrreda.downloader.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _maxConcurrent = MutableStateFlow(prefs.getInt(KEY_MAX, 3))
    val maxConcurrent: StateFlow<Int> = _maxConcurrent.asStateFlow()

    private val _wifiOnly = MutableStateFlow(prefs.getBoolean(KEY_WIFI, false))
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    fun setMaxConcurrent(value: Int) {
        val v = value.coerceIn(1, 6)
        prefs.edit().putInt(KEY_MAX, v).apply()
        _maxConcurrent.value = v
    }

    fun setWifiOnly(value: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI, value).apply()
        _wifiOnly.value = value
    }

    private companion object {
        const val KEY_MAX = "max_concurrent"
        const val KEY_WIFI = "wifi_only"
    }
}
