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
import com.amrreda.downloader.extract.ExtractResult
import com.amrreda.downloader.extract.MediaItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as DownloaderApp
    val engine = container.engine
    val settings = container.settings

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

    private fun toast(res: Int, vararg args: Any) =
        Toast.makeText(getApplication(), getApplication<Application>().getString(res, *args), Toast.LENGTH_SHORT).show()

    fun normalizedUrl(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        val match = URL_REGEX.find(t)?.value ?: t
        val withScheme = if (match.startsWith("http://") || match.startsWith("https://")) match else "https://$match"
        return withScheme.takeIf { android.util.Patterns.WEB_URL.matcher(it).matches() || it.contains('.') }
    }

    fun downloadDirect() {
        val url = normalizedUrl(urlInput) ?: return toast(R.string.invalid_url)
        engine.enqueue(url, fileNameInput.takeIf { it.isNotBlank() })
        fileNameInput = ""
        toast(R.string.added_to_queue)
    }

    fun analyze() {
        val url = normalizedUrl(urlInput) ?: return toast(R.string.invalid_url)
        urlInput = url
        lastAnalyzedUrl = url
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
                        if (r.items.isEmpty()) analyzeError = getApplication<Application>().getString(R.string.no_media_found)
                    }
                }
            } catch (t: Throwable) {
                analyzeError = t.message ?: t.javaClass.simpleName
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
        requestedTab = 0
        analyze()
    }

    private companion object {
        val URL_REGEX = Regex("https?://[^\\s\"'<>]+")
    }
}
