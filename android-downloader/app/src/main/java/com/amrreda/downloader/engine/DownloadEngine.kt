package com.amrreda.downloader.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.webkit.CookieManager
import com.amrreda.downloader.data.AppSettings
import com.amrreda.downloader.data.DownloadDao
import com.amrreda.downloader.data.DownloadEntity
import com.amrreda.downloader.data.DownloadKind
import com.amrreda.downloader.data.DownloadStatus
import com.amrreda.downloader.data.FileCategory
import com.amrreda.downloader.service.DownloadService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Multi-connection download manager:
 *  - persistent queue (Room) that survives app restarts
 *  - configurable parallel downloads
 *  - pause / resume using HTTP Range requests (continues from the last byte)
 *  - Wi-Fi-only mode
 *  - forwards browser cookies / referer so links opened in the in-app browser keep working
 */
class DownloadEngine(
    private val context: Context,
    private val dao: DownloadDao,
    private val settings: AppSettings,
    private val video: YtDlpEngine,
) {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val mutex = Mutex()
    private val tempDir: File by lazy {
        File(context.getExternalFilesDir(null) ?: context.filesDir, "partial").apply { mkdirs() }
    }

    init {
        scope.launch {
            dao.requeueInterrupted()
            pump()
        }
        merge(settings.maxConcurrent.drop(1), settings.wifiOnly.drop(1))
            .onEach { pump() }
            .launchIn(scope)
        runCatching {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { kick() }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { kick() }
            })
        }
    }

    fun kick() { scope.launch { pump() } }

    fun enqueue(url: String, fileName: String? = null, referer: String? = null, userAgent: String? = null) {
        scope.launch {
            val name = FileNames.sanitize(fileName?.takeIf { it.isNotBlank() } ?: FileNames.fromUrl(url))
            val mime = FileCategory.mimeFromName(name)
            dao.insert(
                DownloadEntity(
                    url = url.trim(),
                    fileName = name,
                    nameFromUser = !fileName.isNullOrBlank(),
                    mimeType = mime,
                    category = FileCategory.of(mime, name).name,
                    referer = referer,
                    userAgent = userAgent,
                )
            )
            pump()
        }
    }

    /** Queues a video/audio download from YouTube, Instagram, Facebook, etc. through yt-dlp. */
    fun enqueueVideo(details: VideoDetails, option: QualityOption) {
        scope.launch {
            val ext = option.audioFormat ?: "mp4"
            val base = FileNames.sanitize(details.title).take(90)
            dao.insert(
                DownloadEntity(
                    url = details.url,
                    fileName = "$base.$ext",
                    mimeType = FileCategory.mimeFromName("x.$ext"),
                    category = (if (option.isAudio) FileCategory.AUDIO else FileCategory.VIDEO).name,
                    totalBytes = option.estimatedBytes,
                    kind = DownloadKind.VIDEO.name,
                    title = details.title,
                    thumbnailUrl = details.thumbnail,
                    formatSelector = option.format,
                    formatSort = option.sort,
                    audioFormat = option.audioFormat,
                    qualityLabel = option.label,
                    platform = details.platform?.name,
                )
            )
            pump()
        }
    }

    fun pause(id: Long) {
        scope.launch {
            jobs.remove(id)?.cancelAndJoin()
            val e = dao.get(id) ?: return@launch
            if (e.statusEnum == DownloadStatus.RUNNING || e.statusEnum == DownloadStatus.QUEUED) {
                dao.update(e.copy(status = DownloadStatus.PAUSED.name, speedBps = 0, downloadedBytes = partialSize(e)))
            }
            pump()
        }
    }

    fun resume(id: Long) {
        scope.launch {
            val e = dao.get(id) ?: return@launch
            if (e.statusEnum in setOf(DownloadStatus.PAUSED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)) {
                dao.update(e.copy(status = DownloadStatus.QUEUED.name, error = null))
            }
            pump()
        }
    }

    fun cancel(id: Long) {
        scope.launch {
            jobs.remove(id)?.cancelAndJoin()
            val e = dao.get(id) ?: return@launch
            deletePartial(e)
            dao.update(e.copy(status = DownloadStatus.CANCELLED.name, downloadedBytes = 0, speedBps = 0, tempPath = null))
            pump()
        }
    }

    fun delete(id: Long, deleteFile: Boolean) {
        scope.launch {
            jobs.remove(id)?.cancelAndJoin()
            val e = dao.get(id) ?: return@launch
            deletePartial(e)
            if (deleteFile) StorageSaver.delete(context, e.contentUri)
            dao.delete(id)
            pump()
        }
    }

    fun pauseAll() {
        scope.launch {
            (dao.getByStatus(DownloadStatus.RUNNING.name) + dao.getByStatus(DownloadStatus.QUEUED.name)).forEach { pause(it.id) }
        }
    }

    fun resumeAll() {
        scope.launch {
            (dao.getByStatus(DownloadStatus.PAUSED.name) + dao.getByStatus(DownloadStatus.FAILED.name)).forEach { resume(it.id) }
        }
    }

    fun clearFinished() { scope.launch { dao.clearFinished() } }

    private fun partialSize(e: DownloadEntity): Long {
        val f = e.tempPath?.let { File(it) }?.takeIf { it.exists() } ?: return 0L
        return if (f.isDirectory) e.downloadedBytes else f.length()
    }

    private fun deletePartial(e: DownloadEntity) {
        e.tempPath?.let { File(it).deleteRecursively() }
    }

    private fun networkAllowed(): Boolean {
        if (!settings.wifiOnly.value) return true
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private suspend fun pump() = mutex.withLock {
        if (!networkAllowed()) return@withLock
        val max = settings.maxConcurrent.value
        for (e in dao.getByStatus(DownloadStatus.QUEUED.name)) {
            if (jobs.size >= max) break
            if (jobs.containsKey(e.id)) continue
            start(e.id)
        }
        if (jobs.isNotEmpty()) DownloadService.start(context)
    }

    private fun start(id: Long) {
        val job = scope.launch(start = CoroutineStart.LAZY) { runDownload(id) }
        jobs[id] = job
        job.invokeOnCompletion {
            jobs.remove(id, job)
            kick()
        }
        job.start()
    }

    @OptIn(InternalCoroutinesApi::class)
    private suspend fun runDownload(id: Long) {
        var e = dao.get(id) ?: return
        if (e.isVideoKind) return runVideoDownload(e)
        val temp = e.tempPath?.let(::File) ?: File(tempDir, "dl_$id.part")
        var existing = if (temp.exists()) temp.length() else 0L
        e = e.copy(status = DownloadStatus.RUNNING.name, tempPath = temp.absolutePath, error = null, downloadedBytes = existing)
        dao.update(e)

        try {
            fun request(userAgent: String) = Request.Builder()
                .url(e.url)
                .header("User-Agent", userAgent)
                .header("Accept", "*/*")
                .apply {
                    e.referer?.let { header("Referer", it) }
                    cookieFor(e.url)?.let { header("Cookie", it) }
                    if (existing > 0) header("Range", "bytes=$existing-")
                }
                .build()

            var call = client.newCall(request(e.userAgent ?: DEFAULT_UA))
            // Abort the socket immediately when the user pauses/cancels.
            val handle = currentCoroutineContext().job.invokeOnCompletion(onCancelling = true, invokeImmediately = true) { call.cancel() }
            try {
                var first = call.execute()
                if (first.code == 403) {
                    // Some CDNs challenge browser-like clients (e.g. Cloudflare bot checks); retry once as a plain client.
                    first.close()
                    call = client.newCall(request(FALLBACK_UA))
                    first = call.execute()
                }
                first.use { resp ->
                    if (resp.code == 416 && existing > 0) {
                        temp.delete()
                        throw IOException("Server rejected resume; tap retry to restart")
                    }
                    if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                    val body = resp.body ?: throw IOException("Empty response")
                    val append = existing > 0 && resp.code == 206
                    if (!append) existing = 0
                    val bodyLen = body.contentLength()
                    val total = when {
                        resp.code == 206 -> parseRangeTotal(resp.header("Content-Range")) ?: if (bodyLen >= 0) bodyLen + existing else -1
                        else -> bodyLen
                    }
                    val serverMime = body.contentType()?.let { "${it.type}/${it.subtype}" }
                    val mime = when {
                        serverMime == null || serverMime == "application/octet-stream" || serverMime == "binary/octet-stream" -> e.mimeType ?: serverMime
                        else -> serverMime
                    }
                    val name = if (e.nameFromUser) e.fileName
                    else FileNames.fromResponse(resp.request.url.toString(), resp.header("Content-Disposition"), mime, e.fileName)
                    val finalMime = if (mime == null || mime == "application/octet-stream") FileCategory.mimeFromName(name) ?: mime else mime
                    e = e.copy(
                        fileName = name, mimeType = finalMime, category = FileCategory.of(finalMime, name).name,
                        totalBytes = total, downloadedBytes = existing,
                    )
                    dao.update(e)

                    FileOutputStream(temp, append).use { out ->
                        body.byteStream().use { input ->
                            val buf = ByteArray(64 * 1024)
                            var downloaded = existing
                            var lastTime = SystemClock.elapsedRealtime()
                            var lastBytes = downloaded
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                downloaded += n
                                val now = SystemClock.elapsedRealtime()
                                if (now - lastTime >= 500) {
                                    val speed = (downloaded - lastBytes) * 1000 / (now - lastTime)
                                    dao.updateProgress(id, downloaded, speed)
                                    lastTime = now
                                    lastBytes = downloaded
                                }
                            }
                            out.flush()
                            existing = downloaded
                        }
                    }
                    if (total > 0 && existing < total) throw IOException("Connection closed early – tap retry to resume")
                }
            } finally {
                handle.dispose()
            }

            val uri = StorageSaver.publish(context, temp, e.fileName, e.mimeType, e.categoryEnum)
            temp.delete()
            val done = e.copy(
                status = DownloadStatus.COMPLETED.name,
                downloadedBytes = existing,
                totalBytes = if (e.totalBytes > 0) e.totalBytes else existing,
                speedBps = 0,
                contentUri = uri.toString(),
                tempPath = null,
                completedAt = System.currentTimeMillis(),
            )
            dao.update(done)
            Notifications.notifyCompleted(context, done)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            if (!currentCoroutineContext().isActive) throw CancellationException("cancelled")
            val failed = (dao.get(id) ?: e).copy(
                status = DownloadStatus.FAILED.name,
                error = t.message ?: t.javaClass.simpleName,
                speedBps = 0,
                downloadedBytes = if (temp.exists()) temp.length() else 0,
            )
            dao.update(failed)
            Notifications.notifyFailed(context, failed)
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    private suspend fun runVideoDownload(start: DownloadEntity) {
        val id = start.id
        val workDir = File(tempDir, "video_$id")
        var e = start.copy(status = DownloadStatus.RUNNING.name, tempPath = workDir.absolutePath, error = null)
        dao.update(e)
        val processId = "dl_$id"
        val handle = currentCoroutineContext().job.invokeOnCompletion(onCancelling = true, invokeImmediately = true) {
            video.cancel(processId)
        }
        try {
            var lastWrite = 0L
            val output = video.downloadWithRefresh(
                url = e.url, workDir = workDir,
                format = e.formatSelector ?: "bv*+ba/b", sort = e.formatSort, audioFormat = e.audioFormat,
                processId = processId,
            ) { p ->
                // Note: for separate video+audio streams yt-dlp reports each stream from 0 to 100%.
                val now = SystemClock.elapsedRealtime()
                if (now - lastWrite >= 500) {
                    lastWrite = now
                    val total = if (p.totalBytes > 0) p.totalBytes else -1
                    val done = if (total > 0) (total * p.percent / 100f).toLong() else 0L
                    kotlinx.coroutines.runBlocking {
                        val cur = dao.get(id) ?: return@runBlocking
                        if (cur.statusEnum != DownloadStatus.RUNNING) return@runBlocking
                        e = cur.copy(totalBytes = total, downloadedBytes = done, speedBps = p.speedBps.coerceAtLeast(0))
                        dao.update(e)
                    }
                }
            }
            currentCoroutineContext().ensureActive()
            val name = FileNames.sanitize(output.name.replace(Regex("""\s\[[A-Za-z0-9_-]{5,}]"""), ""))
            val mime = FileCategory.mimeFromName(name) ?: e.mimeType
            val category = FileCategory.of(mime, name)
            val uri = StorageSaver.publish(context, output, name, mime, category)
            val size = output.length()
            workDir.deleteRecursively()
            val done = (dao.get(id) ?: e).copy(
                status = DownloadStatus.COMPLETED.name, fileName = name, mimeType = mime, category = category.name,
                totalBytes = size, downloadedBytes = size, speedBps = 0, error = null,
                contentUri = uri.toString(), tempPath = null, completedAt = System.currentTimeMillis(),
            )
            dao.update(done)
            Notifications.notifyCompleted(context, done)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            if (!currentCoroutineContext().isActive) throw CancellationException("cancelled")
            val failed = (dao.get(id) ?: e).copy(
                status = DownloadStatus.FAILED.name, error = t.message ?: t.javaClass.simpleName, speedBps = 0,
            )
            dao.update(failed)
            Notifications.notifyFailed(context, failed)
        } finally {
            handle.dispose()
        }
    }

    private fun parseRangeTotal(header: String?): Long? =
        header?.substringAfterLast('/', "")?.trim()?.takeIf { it != "*" }?.toLongOrNull()

    private fun cookieFor(url: String): String? =
        runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()?.takeIf { it.isNotBlank() }

    companion object {
        const val FALLBACK_UA = "SmartDownloader/2.0 (Android)"
        const val DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    }
}
