package com.amrreda.downloader.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amrreda.downloader.R
import com.amrreda.downloader.data.FileCategory
import com.amrreda.downloader.engine.FileNames
import com.amrreda.downloader.extract.MediaItem
import com.amrreda.downloader.extract.Platform
import com.amrreda.downloader.ui.MainViewModel
import org.json.JSONArray
import org.json.JSONTokener
import java.net.URLEncoder

/** JavaScript that lists the media currently present in the page's DOM. */
private const val SCAN_JS = """
(function(){
  var out=[];
  function add(u,t){ if(u && typeof u==='string' && u.indexOf('blob:')!==0 && u.indexOf('data:')!==0) out.push([u,t]); }
  var vids=document.querySelectorAll('video');
  for(var i=0;i<vids.length;i++){ var v=vids[i]; add(v.currentSrc||v.src,'VIDEO');
    var s=v.querySelectorAll('source'); for(var j=0;j<s.length;j++) add(s[j].src,'VIDEO'); }
  var auds=document.querySelectorAll('audio');
  for(var i=0;i<auds.length;i++){ var a=auds[i]; add(a.currentSrc||a.src,'AUDIO');
    var s=a.querySelectorAll('source'); for(var j=0;j<s.length;j++) add(s[j].src,'AUDIO'); }
  var imgs=document.querySelectorAll('img');
  for(var i=0;i<imgs.length;i++){ var im=imgs[i]; if((im.naturalWidth||im.width)>=120) add(im.currentSrc||im.src,'IMAGE'); }
  var metas=document.querySelectorAll('meta[property^="og:video"],meta[property="og:image"],meta[name="twitter:image"]');
  for(var i=0;i<metas.length;i++){ var p=(metas[i].getAttribute('property')||metas[i].getAttribute('name')||''); add(metas[i].content, p.indexOf('video')>=0?'VIDEO':'IMAGE'); }
  var links=document.querySelectorAll('a[href]');
  for(var i=0;i<links.length;i++){ var h=links[i].href; if(/\.(mp4|webm|mkv|mov|m4v|3gp|mp3|m4a|aac|ogg|wav|flac|pdf|docx?|xlsx?|pptx?|zip|rar|7z|apk|jpe?g|png|gif|webp)(\?|#|$)/i.test(h)) add(h,''); }
  return JSON.stringify(out);
})()
"""

private val NETWORK_MEDIA_EXT = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp", "mp3", "m4a", "aac", "ogg", "opus", "wav", "flac")

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var address by remember { mutableStateOf(vm.browserUrl) }
    var progress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var longPressUrl by remember { mutableStateOf<String?>(null) }

    fun scan(view: WebView) {
        view.evaluateJavascript(SCAN_JS) { result ->
            runCatching {
                val json = JSONTokener(result).nextValue() as? String ?: return@runCatching
                val arr = JSONArray(json)
                val items = (0 until arr.length()).mapNotNull { i ->
                    val pair = arr.getJSONArray(i)
                    toMediaItem(pair.getString(0), pair.optString(1))
                }
                vm.addBrowserMedia(items)
            }
        }
    }

    fun go(input: String) {
        val t = input.trim()
        if (t.isEmpty()) return
        val url = when {
            t.startsWith("http://") || t.startsWith("https://") -> t
            t.contains('.') && !t.contains(' ') -> "https://$t"
            else -> "https://www.google.com/search?q=" + URLEncoder.encode(t, "UTF-8")
        }
        webView?.loadUrl(url)
    }

    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { webView?.goBack() }, enabled = canGoBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.browser_hint)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { go(address) }),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                IconButton(onClick = { webView?.reload() }) { Icon(Icons.Filled.Refresh, stringResource(R.string.reload)) }
            }
            if (progress in 0..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(2.dp))

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        // Present as regular Chrome: Google refuses sign-in from user agents marked as WebView ("; wv").
                        settings.userAgentString = settings.userAgentString
                            .replace("; wv", "")
                            .replace(Regex("Version/\\d+(\\.\\d+)* "), "")
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                vm.browserMedia.clear()
                                address = url
                                vm.browserUrl = url
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                canGoBack = view.canGoBack()
                                CookieManager.getInstance().flush()
                                vm.refreshAccounts()
                                scan(view)
                                // Re-scan after lazy-loaded content appears.
                                view.postDelayed({ scan(view) }, 2500)
                            }

                            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                                canGoBack = view.canGoBack()
                                address = url
                            }

                            // Sniff media requested by the page (e.g. a video starting to play).
                            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                val u = request.url.toString()
                                if (FileCategory.extensionOf(u) in NETWORK_MEDIA_EXT) {
                                    toMediaItem(u, "")?.let { item -> view.post { vm.addBrowserMedia(listOf(item)) } }
                                }
                                return null
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress }
                        }
                        // Files the site serves as downloads go straight to our download manager.
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            val name = URLUtil.guessFileName(url, contentDisposition, mimetype)
                            vm.engine.enqueue(url, FileNames.sanitize(name), referer = this.url, userAgent = userAgent)
                            android.widget.Toast.makeText(ctx, R.string.added_to_queue, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        // Long-press any image or link to download it.
                        setOnLongClickListener {
                            val hit = hitTestResult
                            when (hit.type) {
                                WebView.HitTestResult.IMAGE_TYPE,
                                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
                                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                                    val u = hit.extra
                                    if (u != null && (u.startsWith("http://") || u.startsWith("https://"))) { longPressUrl = u; true } else false
                                }
                                else -> false
                            }
                        }
                        loadUrl(vm.browserUrl)
                        webView = this
                    }
                },
            )
        }

        Column(
            Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        if (Platform.isVideoSite(address)) {
            ExtendedFloatingActionButton(
                onClick = { vm.openVideo(webView?.url ?: address) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Download, null) },
                text = { Text(stringResource(R.string.download_this_video)) },
            )
        }
        if (vm.browserMedia.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { webView?.let { scan(it) }; showSheet = true },
                icon = {
                    BadgedBox(badge = { Badge { Text(vm.browserMedia.size.toString()) } }) { Icon(Icons.Filled.Download, null) }
                },
                text = { Text(stringResource(R.string.media_on_page)) },
            )
        }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.let { it.stopLoading(); it.destroy() }
            webView = null
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Text(stringResource(R.string.found_n, vm.browserMedia.size), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(Modifier.padding(16.dp)) {
                items(vm.browserMedia.toList(), key = { it.url }) { item ->
                    Box(Modifier.padding(vertical = 4.dp)) {
                        FoundItemRow(item, selected = false, downloadMode = true) {
                            vm.downloadFromBrowser(item, webView?.url, webView?.settings?.userAgentString)
                        }
                    }
                }
            }
        }
    }

    longPressUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { longPressUrl = null },
            title = { Text(stringResource(R.string.download_this)) },
            text = { Text(url, maxLines = 4) },
            confirmButton = {
                TextButton(onClick = {
                    vm.downloadFromBrowser(toMediaItem(url, "") ?: MediaItem(url, FileCategory.OTHER, url), webView?.url, webView?.settings?.userAgentString)
                    longPressUrl = null
                }) { Text(stringResource(R.string.download_now)) }
            },
            dismissButton = { TextButton(onClick = { longPressUrl = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun toMediaItem(url: String, hint: String): MediaItem? {
    if (!url.startsWith("http://") && !url.startsWith("https://")) return null
    val name = FileNames.fromUrl(url)
    var cat = FileCategory.of(null, name)
    if (cat == FileCategory.OTHER) cat = runCatching { FileCategory.valueOf(hint) }.getOrDefault(FileCategory.OTHER)
    return MediaItem(url, cat, name, "browser")
}
