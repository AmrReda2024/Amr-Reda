package com.amrreda.downloader.engine

import android.webkit.CookieManager
import com.amrreda.downloader.extract.Platform
import java.io.File

/**
 * Bridges the in-app browser and yt-dlp: when the user signs in to Instagram / YouTube / Facebook
 * inside the app, the session cookies live in the WebView cookie store. yt-dlp reads cookies from a
 * Netscape "cookies.txt" file, so we export them into a private file for each request.
 * The file lives in app-private storage and is deleted after the request.
 */
object CookieExporter {

    /** Parses the "a=1; b=2" string returned by CookieManager. */
    fun parseCookieHeader(header: String?): List<Pair<String, String>> =
        header.orEmpty().split(';').mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null else part.substring(0, idx).trim() to part.substring(idx + 1).trim()
        }.filter { it.first.isNotEmpty() }

    /** Builds a Netscape cookie file body. Pure function so it can be unit tested. */
    fun toNetscape(domain: String, cookies: List<Pair<String, String>>, nowSeconds: Long): String {
        val expiry = nowSeconds + 365L * 24 * 3600
        val d = if (domain.startsWith(".")) domain else ".$domain"
        return buildString {
            append("# Netscape HTTP Cookie File\n")
            cookies.distinctBy { it.first }.forEach { (name, value) ->
                append(d).append('\t').append("TRUE").append('\t').append("/").append('\t')
                    .append("TRUE").append('\t').append(expiry).append('\t')
                    .append(name).append('\t').append(value).append('\n')
            }
        }
    }

    private fun cookiesFor(urls: List<String>): List<Pair<String, String>> {
        val cm = runCatching { CookieManager.getInstance() }.getOrNull() ?: return emptyList()
        return urls.flatMap { parseCookieHeader(cm.getCookie(it)) }.distinctBy { it.first }
    }

    fun isSignedIn(platform: Platform): Boolean =
        cookiesFor(platform.cookieUrls).any { it.first == platform.sessionCookie && it.second.isNotEmpty() }

    /** Writes a cookie file for [url] into [dir]; returns null when there are no cookies to pass. */
    fun writeFor(url: String, dir: File): File? {
        val platform = Platform.detect(url)
        val (domain, urls) = if (platform != null) {
            platform.cookieDomain to platform.cookieUrls
        } else {
            val host = Platform.hostOf(url) ?: return null
            ".$host" to listOf("https://$host", "https://www.$host")
        }
        val cookies = cookiesFor(urls)
        if (cookies.isEmpty()) return null
        dir.mkdirs()
        val file = File(dir, "cookies_${System.nanoTime()}.txt")
        file.writeText(toNetscape(domain, cookies, System.currentTimeMillis() / 1000))
        return file
    }

    /** Signs the user out of [platform] inside the app by expiring its cookies. */
    fun signOut(platform: Platform) {
        val cm = runCatching { CookieManager.getInstance() }.getOrNull() ?: return
        platform.cookieUrls.forEach { url ->
            parseCookieHeader(cm.getCookie(url)).forEach { (name, _) ->
                val expired = "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/"
                cm.setCookie(url, expired)
                cm.setCookie(url, "$expired; Domain=${platform.cookieDomain}")
            }
        }
        cm.flush()
    }
}
