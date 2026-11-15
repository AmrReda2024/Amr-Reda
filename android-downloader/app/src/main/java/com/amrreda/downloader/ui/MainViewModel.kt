package com.amrreda.downloader.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amrreda.downloader.DownloaderApp
import com.amrreda.downloader.R
import com.amrreda.downloader.data.DownloadEntity
import com.amrreda.downloader.engine.CookieExporter
import com.amrreda.downloader.engine.QualityOption
import com.amrreda.downloader.engine.VideoDetails
import com.amrreda.downloader.engine.VideoEngineException
import com.amrreda.downloader.engine.VideoFormats
import com.amrreda.downloader.extract.ExtractResult
import com.amrreda.downloader.extract.MediaItem
import com.amrreda.downloader.extract.Platform
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as DownloaderApp
    val engine = container.engine
    val settings = container.settings
    val video = container.video

    val downloads: StateFlow<List<DownloadEntity>> =
        container.database.downloads().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Home / link analysis ----
    var urlInput by mutableStateOf("")
    var fileNameInput by mutableStateOf("")
    var analyzing by mutableStateOf(false)
        private set
    var analyzeError by mutableStateOf<String?>(null)
        private set
    var pageTitle by mutableStateOf<String?>(null)
        private set
    val foundItems = mutableStateListOf<MediaItem>()
    val selectedUrls = mutableStateListOf<String>()
    var lastAnalyzedUrl: String? = null
        private set

    // ---- Browser ----
    var browserUrl by mutableStateOf("https://www.google.com")
    val browserMedia = mutableStateListOf<MediaItem>()

    // ---- Navigation request (e.g. from a shared link) ----
    var requestedTab by mutableStateOf<Int?>(null)

    // ---- Video sites (YouTube, Instagram, Facebook, ...) via yt-dlp ----
    /** Non-null while the quality sheet is open. */
    var videoSheetUrl by mutableStateOf<String?>(null)
        private set
    var videoDetails by mutableStateOf<VideoDetails?>(null)
        private set
    var videoError by mutableStateOf<Pair<String, VideoFormats.ErrorHint>?>(null)
        private set
    private var videoJob: Job? = null

    // ---- Clipboard link suggestion ----
    var clipboardSuggestion by mutableStateOf<String?>(null)
        private set
    private var lastClipboardOffered: String? = null

    // ---- Accounts ----
    var signedIn by mutableStateOf<Set<Platform>>(emptySet())
        private set

    fun refreshAccounts() {
        signedIn = Platform.entries.filter { CookieExporter.isSignedIn(it) }.toSet()
    }

    fun signIn(platform: Platform) = openInBrowser(platform.loginUrl)

    fun signOut(platform: Platform) {
        CookieExporter.signOut(platform)
        refreshAccounts()
    }

    fun openVideo(url: String) {
        videoJob?.cancel()
        videoSheetUrl = url
        videoDetails = null
        videoError = null
        videoJob = viewModelScope.launch {
            try {
                videoDetails = video.fetchDetails(url)
            } catch (e: VideoEngineException) {
                videoError = e.message.orEmpty() to e.hint
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                videoError = (t.message ?: t.javaClass.simpleName) to VideoFormats.ErrorHint.UPDATE
            }
        }
    }

    fun retryVideo() { videoSheetUrl?.let(::openVideo) }

    /** Updates yt-dlp first, then retries the current link with the new version. */
    fun updateAndRetryVideo() {
        val url = videoSheetUrl ?: return
        videoError = null
        videoDetails = null
        videoJob?.cancel()
        videoJob = viewModelScope.launch {
            video.update()
            openVideo(url)
        }
    }

    fun closeVideoSheet() {
        videoJob?.cancel()
        videoSheetUrl = null
        videoDetails = null
        videoError = null
    }

    fun downloadVideo(option: QualityOption) {
        val details = videoDetails ?: return
        engine.enqueueVideo(details, option)
        toast(R.string.added_to_queue)
        closeVideoSheet()
        requestedTab = 0
    }

    var engineUpdateMessage by mutableStateOf<String?>(null)
        private set

    fun updateVideoEngine() {
        viewModelScope.launch {
            engineUpdateMessage = null
            val r = video.update()
            val app = getApplication<Application>()
            engineUpdateMessage = r.fold(
                onSuccess = { app.getString(if (it) R.string.engine_updated else R.string.engine_up_to_date) },
                onFailure = { app.getString(R.string.engine_update_failed, it.message ?: "") },
            )
        }
    }

    /** Called when the window gains focus (Android only allows clipboard reads then). */
    fun onClipboardText(text: String?) {
        val url = text?.let { URL_REGEX.find(it)?.value } ?: return
        if (url == lastClipboardOffered || url == urlInput || url == videoSheetUrl) return
        lastClipboardOffered = url
        clipboardSuggestion = url
    }

    fun acceptClipboard() {
        val url = clipboardSuggestion ?: return
        clipboardSuggestion = null
        urlInput = url
        smartDownload()
    }

    fun dismissClipboard() { clipboardSuggestion = null }

    private fun toast(res: Int, vararg args: Any) =
        Toast.makeText(getApplication(), getApplication<Application>().getString(res, *args), Toast.LENGTH_SHORT).show()

    fun normalizedUrl(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        val match = URL_REGEX.find(t)?.value ?: t
        val withScheme = if (match.startsWith("http://") || match.startsWith("https://")) match else "https://$match"
        return withScheme.takeIf { android.util.Patterns.WEB_URL.matcher(it).matches() || it.contains('.') }
    }

    /** "Download" button: video sites open the quality picker, anything else downloads as a file. */
    fun smartDownload() {
        val url = normalizedUrl(urlInput) ?: return toast(R.string.invalid_url)
        urlInput = url
        if (Platform.isVideoSite(url)) {
            openVideo(url)
            return
        }
        engine.enqueue(url, fileNameInput.takeIf { it.isNotBlank() })
        fileNameInput = ""
        toast(R.string.added_to_queue)
    }

    fun analyze() {
        val url = normalizedUrl(urlInput) ?: return toast(R.string.invalid_url)
        urlInput = url
        lastAnalyzedUrl = url
        if (Platform.isVideoSite(url)) {
            openVideo(url)
            return
        }
        analyzing = true
        analyzeError = null
        pageTitle = null
        foundItems.clear()
        selectedUrls.clear()
        viewModelScope.launch {
            try {
                when (val r = container.extractor.analyze(url)) {
                    is ExtractResult.DirectFile -> {
                        pageTitle = r.item.name
                        foundItems.add(r.item)
                        selectedUrls.add(r.item.url)
                    }
                    is ExtractResult.Page -> {
                        pageTitle = r.title
                        foundItems.addAll(r.items)
                        // No video in the HTML (e.g. loaded by JavaScript): quietly let yt-dlp's 1000+ extractors try.
                        if (r.items.none { it.category == com.amrreda.downloader.data.FileCategory.VIDEO }) {
                            val details = runCatching { video.fetchDetails(url) }.getOrNull()
                            if (details != null && details.options.any { !it.isAudio }) {
                                videoSheetUrl = url
                                videoDetails = details
                                videoError = null
                            } else if (r.items.isEmpty()) {
                                analyzeError = getApplication<Application>().getString(R.string.no_media_found)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                analyzeError = ErrorText.describe(getApplication(), t.message ?: t.javaClass.simpleName)
            } finally {
                analyzing = false
            }
        }
    }

    fun toggleSelected(url: String) {
        if (url in selectedUrls) selectedUrls.remove(url) else selectedUrls.add(url)
    }

    fun selectAll(all: Boolean) {
        selectedUrls.clear()
        if (all) selectedUrls.addAll(foundItems.map { it.url })
    }

    fun downloadSelected() {
        val items = foundItems.filter { it.url in selectedUrls }
        if (items.isEmpty()) return
        items.forEach { engine.enqueue(it.url, null, referer = lastAnalyzedUrl) }
        toast(R.string.added_n_to_queue, items.size)
        selectedUrls.clear()
    }

    fun downloadFromBrowser(item: MediaItem, pageUrl: String?, userAgent: String?) {
        engine.enqueue(item.url, null, referer = pageUrl, userAgent = userAgent)
        toast(R.string.added_to_queue)
    }

    fun addBrowserMedia(items: List<MediaItem>) {
        val known = browserMedia.map { it.url }.toHashSet()
        items.forEach { if (known.add(it.url)) browserMedia.add(it) }
    }

    fun openInBrowser(url: String) {
        browserUrl = url
        requestedTab = 1
    }

    fun handleSharedText(text: String?) {
        val url = text?.let { URL_REGEX.find(it)?.value } ?: return
        urlInput = url
        lastClipboardOffered = url
        requestedTab = 0
        if (Platform.isVideoSite(url)) openVideo(url) else analyze()
    }

    private companion object {
        val URL_REGEX = Regex("https?://[^\\s\"'<>]+")
    }
}
