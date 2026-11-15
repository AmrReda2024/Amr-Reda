package com.amrreda.downloader.extract

import com.amrreda.downloader.data.FileCategory
import com.amrreda.downloader.engine.DownloadEngine
import com.amrreda.downloader.engine.FileNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

data class MediaItem(
    val url: String,
    val category: FileCategory,
    val name: String,
    val source: String = "",
)

sealed interface ExtractResult {
    /** The link itself is a file (video, image, pdf...). */
    data class DirectFile(val item: MediaItem, val sizeBytes: Long) : ExtractResult
    /** The link is a web page; these are the downloadable files found inside it. */
    data class Page(val title: String, val items: List<MediaItem>) : ExtractResult
}

/**
 * Analyses any public link: if it is a file, returns it directly; if it is a web page,
 * scans the HTML (img / video / audio / source tags, srcset, Open Graph & Twitter cards,
 * and links to media/document files) and returns every downloadable item found.
 */
class MediaExtractor(private val client: OkHttpClient) {

    suspend fun analyze(url: String): ExtractResult = withContext(Dispatchers.IO) {
        fun request(ua: String) = Request.Builder().url(url)
            .header("User-Agent", ua)
            .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
            .build()
        var response = client.newCall(request(DownloadEngine.DEFAULT_UA)).execute()
        if (response.code == 403) {
            response.close()
            response = client.newCall(request(DownloadEngine.FALLBACK_UA)).execute()
        }
        response.use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("Empty response")
            val type = body.contentType()
            val mime = type?.let { "${it.type}/${it.subtype}" }
            val finalUrl = resp.request.url.toString()
            val isHtml = mime == null || mime.contains("html") || mime.contains("xml") && !mime.contains("svg")
            if (!isHtml) {
                val name = FileNames.fromResponse(finalUrl, resp.header("Content-Disposition"), mime, FileNames.fromUrl(finalUrl))
                return@use ExtractResult.DirectFile(
                    MediaItem(finalUrl, FileCategory.of(mime, name), name, "direct"),
                    body.contentLength(),
                )
            }
            // Cap page size to protect memory on huge pages.
            val bytes = body.source().let { src ->
                src.request(MAX_HTML_BYTES)
                src.buffer.snapshot().toByteArray().let { if (it.size > MAX_HTML_BYTES) it.copyOf(MAX_HTML_BYTES.toInt()) else it }
            }
            val charset = type?.charset() ?: Charsets.UTF_8
            val doc = Jsoup.parse(String(bytes, charset), finalUrl)
            ExtractResult.Page(doc.title().ifBlank { finalUrl }, extractFrom(doc))
        }
    }

    fun extractFrom(doc: Document): List<MediaItem> {
        val found = LinkedHashMap<String, MediaItem>()

        fun add(raw: String?, hint: FileCategory?, source: String) {
            if (raw.isNullOrBlank()) return
            val abs = raw.trim()
            if (!abs.startsWith("http://") && !abs.startsWith("https://")) return
            if (found.containsKey(abs)) return
            val name = FileNames.fromUrl(abs)
            var cat = FileCategory.of(null, name)
            if (cat == FileCategory.OTHER && hint != null) cat = hint
            found[abs] = MediaItem(abs, cat, name, source)
        }

        fun absAttr(el: org.jsoup.nodes.Element, attr: String) = el.absUrl(attr).ifBlank { null }

        fun largestFromSrcset(el: org.jsoup.nodes.Element): String? {
            val srcset = el.attr("srcset").ifBlank { el.attr("data-srcset") }
            if (srcset.isBlank()) return null
            val best = srcset.split(',').mapNotNull { part ->
                val bits = part.trim().split(Regex("\\s+"))
                val u = bits.getOrNull(0) ?: return@mapNotNull null
                val w = bits.getOrNull(1)?.dropLast(1)?.toFloatOrNull() ?: 1f
                u to w
            }.maxByOrNull { it.second }?.first ?: return null
            return runCatching { java.net.URL(java.net.URL(el.baseUri()), best).toString() }.getOrNull()
        }

        // Social / Open Graph metadata usually holds the main video & image of the page.
        doc.select("meta[property=og:video], meta[property=og:video:url], meta[property=og:video:secure_url], meta[name=twitter:player:stream]")
            .forEach { add(it.absUrl("content"), FileCategory.VIDEO, "og:video") }
        doc.select("meta[property=og:audio], meta[property=og:audio:url]")
            .forEach { add(it.absUrl("content"), FileCategory.AUDIO, "og:audio") }
        doc.select("meta[property=og:image], meta[property=og:image:url], meta[name=twitter:image], meta[name=twitter:image:src]")
            .forEach { add(it.absUrl("content"), FileCategory.IMAGE, "og:image") }

        doc.select("video").forEach { v ->
            add(absAttr(v, "src"), FileCategory.VIDEO, "video")
            v.select("source").forEach { add(absAttr(it, "src"), FileCategory.VIDEO, "video") }
            add(absAttr(v, "poster"), FileCategory.IMAGE, "poster")
        }
        doc.select("audio").forEach { a ->
            add(absAttr(a, "src"), FileCategory.AUDIO, "audio")
            a.select("source").forEach { add(absAttr(it, "src"), FileCategory.AUDIO, "audio") }
        }
        doc.select("picture source").forEach { add(largestFromSrcset(it), FileCategory.IMAGE, "picture") }
        doc.select("img").forEach { img ->
            add(largestFromSrcset(img), FileCategory.IMAGE, "img")
            add(absAttr(img, "src"), FileCategory.IMAGE, "img")
            add(absAttr(img, "data-src"), FileCategory.IMAGE, "img")
            add(absAttr(img, "data-original"), FileCategory.IMAGE, "img")
        }
        doc.select("a[href]").forEach { a ->
            val href = a.absUrl("href")
            if (FileCategory.extensionOf(href) in FileCategory.MEDIA_AND_FILE_EXT) add(href, null, "link")
        }
        doc.select("embed[src], object[data], iframe[src]").forEach { el ->
            val u = el.absUrl("src").ifBlank { el.absUrl("data") }
            if (FileCategory.extensionOf(u) in FileCategory.MEDIA_AND_FILE_EXT) add(u, null, "embed")
        }

        // Most useful first: videos, audio, images, documents, then everything else.
        val order = listOf(FileCategory.VIDEO, FileCategory.AUDIO, FileCategory.IMAGE, FileCategory.DOCUMENT,
            FileCategory.ARCHIVE, FileCategory.APK, FileCategory.OTHER)
        return found.values.sortedBy { order.indexOf(it.category) }
    }

    private companion object {
        const val MAX_HTML_BYTES = 4L * 1024 * 1024
    }
}
