package com.amrreda.downloader.ui

import android.content.Context
import com.amrreda.downloader.R

/** Converts raw engine errors ("HTTP 404", timeouts...) into clear, localized messages. */
object ErrorText {
    private val HTTP = Regex("HTTP (\\d{3})")

    fun describe(context: Context, raw: String?): String {
        val text = raw.orEmpty()
        val code = HTTP.find(text)?.groupValues?.get(1)?.toIntOrNull()
        val res = when {
            code == 401 || code == 403 -> R.string.err_forbidden
            code == 404 || code == 410 -> R.string.err_not_found
            code == 429 -> R.string.err_rate_limited
            code != null && code >= 500 -> R.string.err_server
            text.contains("Unable to resolve host", true) || text.contains("UnknownHost", true) -> R.string.err_no_internet
            text.contains("timeout", true) || text.contains("timed out", true) -> R.string.err_timeout
            text.contains("ENOSPC", true) || text.contains("No space", true) -> R.string.err_no_space
            else -> return text
        }
        return context.getString(res)
    }
}
