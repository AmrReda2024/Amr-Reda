package com.amrreda.downloader.extract

import java.net.URI

/**
 * Social / video platforms handled by the yt-dlp engine.
 * [cookieUrls] are the origins whose browser cookies are passed to yt-dlp when the user is signed in;
 * [sessionCookie] is the cookie that proves a signed-in session on that platform.
 */
enum class Platform(
    val displayName: String,
    private val hosts: List<String>,
    val homeUrl: String,
    val loginUrl: String,
    val cookieUrls: List<String>,
    val cookieDomain: String,
    val sessionCookie: String,
) {
    YOUTUBE(
        "YouTube", listOf("youtube.com", "youtu.be", "youtube-nocookie.com"),
        "https://m.youtube.com", "https://accounts.google.com/ServiceLogin?service=youtube&continue=https://m.youtube.com",
        listOf("https://www.youtube.com", "https://m.youtube.com", "https://youtube.com", "https://accounts.google.com"),
        ".youtube.com", "SAPISID",
    ),
    INSTAGRAM(
        "Instagram", listOf("instagram.com", "instagr.am"),
        "https://www.instagram.com", "https://www.instagram.com/accounts/login/",
        listOf("https://www.instagram.com", "https://instagram.com"),
        ".instagram.com", "sessionid",
    ),
    FACEBOOK(
        "Facebook", listOf("facebook.com", "fb.watch", "fb.com"),
        "https://m.facebook.com", "https://m.facebook.com/login/",
        listOf("https://www.facebook.com", "https://m.facebook.com", "https://facebook.com"),
        ".facebook.com", "c_user",
    ),
    TIKTOK(
        "TikTok", listOf("tiktok.com"),
        "https://www.tiktok.com", "https://www.tiktok.com/login",
        listOf("https://www.tiktok.com", "https://tiktok.com"),
        ".tiktok.com", "sessionid",
    ),
    X(
        "X (Twitter)", listOf("x.com", "twitter.com", "t.co"),
        "https://x.com", "https://x.com/i/flow/login",
        listOf("https://x.com", "https://twitter.com"),
        ".x.com", "auth_token",
    );

    fun matchesHost(host: String): Boolean = hosts.any { host == it || host.endsWith(".$it") }

    companion object {
        fun detect(url: String?): Platform? {
            val host = hostOf(url) ?: return null
            return entries.firstOrNull { it.matchesHost(host) }
        }

        fun hostOf(url: String?): String? = runCatching {
            URI(url!!.trim()).host?.lowercase()?.removePrefix("www.")
        }.getOrNull()

        /** Sites that are not in [Platform] but are well supported by yt-dlp's extractors. */
        private val OTHER_VIDEO_HOSTS = listOf(
            "vimeo.com", "dailymotion.com", "reddit.com", "redd.it", "soundcloud.com", "twitch.tv",
            "pinterest.com", "pin.it", "snapchat.com", "threads.net", "linkedin.com", "bilibili.com",
            "rumble.com", "streamable.com", "tumblr.com", "vk.com", "ok.ru", "bandcamp.com", "mixcloud.com",
        )

        /** True when the link should go straight to the video engine instead of the plain file downloader. */
        fun isVideoSite(url: String?): Boolean {
            val host = hostOf(url) ?: return false
            return detect(url) != null || OTHER_VIDEO_HOSTS.any { host == it || host.endsWith(".$it") }
        }
    }
}
