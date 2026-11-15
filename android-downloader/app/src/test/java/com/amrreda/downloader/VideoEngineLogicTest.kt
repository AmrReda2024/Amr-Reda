package com.amrreda.downloader

import com.amrreda.downloader.engine.CookieExporter
import com.amrreda.downloader.engine.FormatInfo
import com.amrreda.downloader.engine.VideoFormats
import com.amrreda.downloader.extract.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoEngineLogicTest {

    @Test
    fun detectsPlatformsFromAllLinkShapes() {
        assertEquals(Platform.YOUTUBE, Platform.detect("https://www.youtube.com/watch?v=abc"))
        assertEquals(Platform.YOUTUBE, Platform.detect("https://youtu.be/abc?si=x"))
        assertEquals(Platform.YOUTUBE, Platform.detect("https://m.youtube.com/shorts/abc"))
        assertEquals(Platform.INSTAGRAM, Platform.detect("https://www.instagram.com/reel/Cxyz/"))
        assertEquals(Platform.FACEBOOK, Platform.detect("https://fb.watch/abc/"))
        assertEquals(Platform.FACEBOOK, Platform.detect("https://m.facebook.com/watch/?v=1"))
        assertEquals(Platform.TIKTOK, Platform.detect("https://vm.tiktok.com/ZM123/"))
        assertEquals(Platform.X, Platform.detect("https://twitter.com/u/status/1"))
        assertNull(Platform.detect("https://example.com/video.mp4"))
        assertNull(Platform.detect("https://notyoutube.com/watch"))
        assertTrue(Platform.isVideoSite("https://vimeo.com/123"))
        assertFalse(Platform.isVideoSite("https://upload.wikimedia.org/a.png"))
    }

    private val youtubeLike = listOf(
        FormatInfo("18", "mp4", 360, "avc1.42001E", "mp4a.40.2", 5_000_000),
        FormatInfo("136", "mp4", 720, "avc1.4d401f", "none", 20_000_000),
        FormatInfo("137", "mp4", 1080, "avc1.640028", "none", 40_000_000),
        FormatInfo("248", "webm", 1080, "vp9", "none", 30_000_000),
        FormatInfo("313", "webm", 2160, "vp9", "none", 150_000_000),
        FormatInfo("140", "m4a", 0, "none", "mp4a.40.2", 3_000_000),
        FormatInfo("251", "webm", 0, "none", "opus", 2_800_000),
        FormatInfo("sb0", "mhtml", 90, "none", "none", 0),
    )

    @Test
    fun buildsResolutionAndAudioOptions() {
        val opts = VideoFormats.buildOptions(youtubeLike, 200)
        assertEquals(listOf("2160p", "1080p", "720p", "360p", "MP3", "M4A"), opts.map { it.label })
        val p1080 = opts.first { it.label == "1080p" }
        assertEquals("bv*+ba/b", p1080.format)
        assertTrue(p1080.sort!!.startsWith("res:1080"))
        assertEquals(43_000_000L, p1080.estimatedBytes) // mp4 video + best audio
        assertEquals(5_000_000L, opts.first { it.label == "360p" }.estimatedBytes) // already has audio
        assertEquals("mp3", opts.first { it.label == "MP3" }.audioFormat)
    }

    @Test
    fun facebookStyleProgressiveFormatsWithoutHeights() {
        val fb = listOf(
            FormatInfo("sd", "mp4", 0, "h264", "aac", 0, note = "sd"),
            FormatInfo("hd", "mp4", 0, "h264", "aac", 0, note = "hd"),
        )
        val labels = VideoFormats.buildOptions(fb).map { it.label }
        assertEquals(listOf("BEST", "HD", "SD", "MP3", "M4A"), labels)
    }

    @Test
    fun facebookRealWorldFormatsWithUnknownCodecs() {
        // Exactly what yt-dlp returns for a public Facebook video today: ids only, everything else null.
        val fb = listOf(FormatInfo("sd", "mp4", 0, null, null, 0), FormatInfo("hd", "mp4", 0, null, null, 0))
        val opts = VideoFormats.buildOptions(fb, 74)
        assertEquals(listOf("BEST", "HD", "SD", "MP3", "M4A"), opts.map { it.label })
        assertEquals("hd", opts.first { it.label == "HD" }.format)
    }

    @Test
    fun snapsOddHeights() {
        assertEquals(1080, VideoFormats.standardHeight(1012))
        assertEquals(720, VideoFormats.standardHeight(718))
        assertEquals(480, VideoFormats.standardHeight(480))
        assertEquals(2160, VideoFormats.standardHeight(2160))
        assertEquals(720, VideoFormats.standardHeight(800)) // 1920x800 letterbox: -S res:720 then picks <= 720
        assertEquals(100, VideoFormats.standardHeight(100))
    }

    @Test
    fun parsesYtDlpProgressLines() {
        val p = VideoFormats.parseProgress("[download]  42.5% of ~ 10.00MiB at  2.00MiB/s ETA 00:07 (frag 3/10)")
        assertNotNull(p); p!!
        assertEquals(42.5f, p.percent, 0.01f)
        assertEquals(10L * 1024 * 1024, p.totalBytes)
        assertEquals(2L * 1024 * 1024, p.speedBps)
        assertEquals(7L, p.etaSec)
        val done = VideoFormats.parseProgress("[download] 100% of   15.30MiB in 00:00:05 at 3.02MiB/s")!!
        assertEquals(100f, done.percent, 0.01f)
        assertNull(VideoFormats.parseProgress("[youtube] abc: Downloading webpage"))
    }

    @Test
    fun classifiesErrorsIntoActionableHints() {
        assertEquals(VideoFormats.ErrorHint.SIGN_IN,
            VideoFormats.friendlyError("ERROR: [youtube] x: Sign in to confirm you’re not a bot").second)
        assertEquals(VideoFormats.ErrorHint.SIGN_IN,
            VideoFormats.friendlyError("ERROR: [Instagram] x: Requested content is not available, rate-limit reached or login required").second)
        assertEquals(VideoFormats.ErrorHint.UNSUPPORTED,
            VideoFormats.friendlyError("ERROR: Unsupported URL: https://example.com").second)
        assertEquals("Unsupported URL: https://example.com",
            VideoFormats.friendlyError("WARNING: x\nERROR: Unsupported URL: https://example.com").first)
    }

    @Test
    fun exportsNetscapeCookies() {
        val parsed = CookieExporter.parseCookieHeader("sessionid=abc; csrftoken=x=y; bad; ds_user_id=1")
        assertEquals(listOf("sessionid" to "abc", "csrftoken" to "x=y", "ds_user_id" to "1"), parsed)
        val txt = CookieExporter.toNetscape("instagram.com", parsed, 1_000)
        val lines = txt.trim().lines()
        assertEquals("# Netscape HTTP Cookie File", lines[0])
        assertEquals(".instagram.com\tTRUE\t/\tTRUE\t31537000\tsessionid\tabc", lines[1])
        assertEquals(4, lines.size)
    }
}
