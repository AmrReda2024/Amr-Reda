package com.amrreda.downloader.ui

import java.text.DateFormat
import java.util.Date
import java.util.Locale

object Format {
    fun bytes(b: Long): String {
        if (b < 0) return "?"
        if (b < 1024) return "$b B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var v = b / 1024.0
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return String.format(Locale.US, if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
    }

    fun speed(bps: Long): String = bytes(bps) + "/s"

    fun eta(remaining: Long, bps: Long): String {
        if (bps <= 0 || remaining <= 0) return ""
        val s = remaining / bps
        return when {
            s >= 3600 -> String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
            else -> String.format(Locale.US, "%d:%02d", s / 60, s % 60)
        }
    }

    fun date(ms: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))
}
