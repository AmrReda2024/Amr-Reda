package com.amrreda.downloader

import com.amrreda.downloader.data.FileCategory
import com.amrreda.downloader.engine.FileNames
import com.amrreda.downloader.extract.MediaExtractor
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorAndNamesTest {

    private val html = """
        <html><head>
          <title>Sample</title>
          <meta property="og:video" content="/media/clip.mp4">
          <meta property="og:image" content="https://cdn.example.com/cover.jpg">
        </head><body>
          <video poster="poster.png"><source src="movie.webm" type="video/webm"></video>
          <audio src="https://example.com/a/song.mp3"></audio>
          <img src="small.jpg" srcset="small.jpg 320w, big.jpg 1280w">
          <img src="data:image/png;base64,AAAA">
          <a href="/docs/report.pdf">Report</a>
          <a href="/about">About</a>
          <video src="blob:https://example.com/123"></video>
        </body></html>
    """.trimIndent()

    @Test
    fun extractsAllMediaWithAbsoluteUrls() {
        val doc = Jsoup.parse(html, "https://example.com/page/")
        val items = MediaExtractor(OkHttpClient()).extractFrom(doc)
        val urls = items.map { it.url }

        assertTrue(urls.contains("https://example.com/media/clip.mp4"))
        assertTrue(urls.contains("https://example.com/page/movie.webm"))
        assertTrue(urls.contains("https://example.com/a/song.mp3"))
        assertTrue(urls.contains("https://example.com/page/big.jpg"))      // largest srcset candidate
        assertTrue(urls.contains("https://cdn.example.com/cover.jpg"))
        assertTrue(urls.contains("https://example.com/docs/report.pdf"))
        assertFalse(urls.any { it.startsWith("data:") || it.startsWith("blob:") })
        assertFalse(urls.contains("https://example.com/about"))
        assertEquals(urls.size, urls.toSet().size)                          // no duplicates
        assertEquals(FileCategory.VIDEO, items.first().category)            // videos listed first
        assertEquals(FileCategory.DOCUMENT, items.first { it.url.endsWith(".pdf") }.category)
    }

    @Test
    fun categoriesFromMimeAndExtension() {
        assertEquals(FileCategory.VIDEO, FileCategory.of("video/mp4", null))
        assertEquals(FileCategory.IMAGE, FileCategory.of(null, "photo.JPEG"))
        assertEquals(FileCategory.AUDIO, FileCategory.of(null, "x.m4a"))
        assertEquals(FileCategory.APK, FileCategory.of("application/vnd.android.package-archive", "x"))
        assertEquals(FileCategory.ARCHIVE, FileCategory.of(null, "backup.zip"))
        assertEquals(FileCategory.DOCUMENT, FileCategory.of("application/pdf", "x"))
        assertEquals("mp4", FileCategory.extensionOf("https://a.com/v/clip.mp4?token=1#t"))
    }

    @Test
    fun fileNamesAreCleanAndHonourContentDisposition() {
        assertEquals("clip.mp4", FileNames.fromUrl("https://a.com/v/clip.mp4?x=1"))
        assertEquals("تقرير 2026.pdf", FileNames.fromUrl("https://a.com/%D8%AA%D9%82%D8%B1%D9%8A%D8%B1%202026.pdf"))
        assertEquals("a_b_c.txt", FileNames.sanitize("a/b:c.txt"))
        assertEquals("Contract Final.docx",
            FileNames.fromResponse("https://a.com/dl?id=5", "attachment; filename=\"Contract Final.docx\"", null, "dl"))
        assertEquals("عقد.pdf",
            FileNames.fromResponse("https://a.com/dl", "attachment; filename*=UTF-8''%D8%B9%D9%82%D8%AF.pdf", null, "dl"))
        assertTrue(FileNames.sanitize("x".repeat(300) + ".mp4").let { it.length <= 120 && it.endsWith(".mp4") })
    }
}
