package com.amrreda.downloader.engine

import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import com.amrreda.downloader.data.FileCategory
import java.net.URLDecoder

object FileNames {
    private val ILLEGAL = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    fun sanitize(name: String): String {
        var n = ILLEGAL.replace(name, "_").trim().trim('.')
        if (n.isEmpty()) n = "download"
        if (n.length > 120) {
            val ext = n.substringAfterLast('.', "")
            n = if (ext.isNotEmpty() && ext.length <= 6) n.take(120 - ext.length - 1) + "." + ext else n.take(120)
        }
        return n
    }

    fun fromUrl(url: String): String {
        val lastSegment = runCatching {
            URLDecoder.decode(url.substringBefore('?').substringBefore('#').trimEnd('/').substringAfterLast('/'), "UTF-8")
        }.getOrDefault("")
        if (lastSegment.contains('.') && FileCategory.extensionOf(lastSegment).isNotEmpty()) return sanitize(lastSegment)
        return sanitize(URLUtil.guessFileName(url, null, null))
    }

    /** Builds the best name from the server response, keeping a sensible extension. */
    fun fromResponse(url: String, contentDisposition: String?, mime: String?, fallback: String): String {
        val fromHeader = contentDisposition?.let { parseContentDisposition(it) }
        var name = fromHeader ?: fromUrl(url).takeUnless { it.endsWith(".bin") || !it.contains('.') } ?: fallback
        name = sanitize(name)
        val mimeExt = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        val currentExt = FileCategory.extensionOf(name)
        if (mimeExt != null && (currentExt.isEmpty() || currentExt == "bin")) {
            name = name.removeSuffix(".bin") + "." + mimeExt
        }
        return name
    }

    private fun parseContentDisposition(cd: String): String? {
        Regex("filename\\*\\s*=\\s*(?:UTF-8|utf-8)''([^;]+)").find(cd)?.let {
            return runCatching { URLDecoder.decode(it.groupValues[1].trim('"', ' '), "UTF-8") }.getOrNull()
        }
        Regex("filename\\s*=\\s*\"?([^\";]+)\"?").find(cd)?.let { return it.groupValues[1].trim() }
        return null
    }
}
