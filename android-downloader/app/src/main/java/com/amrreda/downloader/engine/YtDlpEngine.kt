package com.amrreda.downloader.engine

import android.content.Context
import com.amrreda.downloader.extract.Platform
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Everything the quality picker needs about one video/post. */
data class VideoDetails(
    val url: String,
    val title: String,
    val uploader: String?,
    val thumbnail: String?,
    val durationSec: Int,
    val platform: Platform?,
    val extractor: String?,
    val options: List<QualityOption>,
)

class VideoEngineException(message: String, val hint: VideoFormats.ErrorHint) : Exception(message)

/**
 * Wrapper around the embedded yt-dlp (Python) + FFmpeg binaries.
 * - initialises once in the background at app start
 * - keeps yt-dlp up to date automatically (sites change often, so a fresh extractor matters)
 * - reads video info and runs downloads with progress callbacks
 */
class YtDlpEngine(private val context: Context) {

    sealed interface State {
        data object Initializing : State
        data class Ready(val version: String?) : State
        data class Failed(val message: String) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("ytdlp", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow<State>(State.Initializing)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating.asStateFlow()

    /** Private folder for cookie files (never on shared storage). */
    private val cookieDir: File get() = File(context.filesDir, "cookies")

    fun start() {
        scope.launch {
            try {
                YoutubeDL.getInstance().init(context)
                FFmpeg.getInstance().init(context)
                _state.value = State.Ready(currentVersion() ?: bundledVersion())
            } catch (t: Throwable) {
                android.util.Log.e(TAG, "Video engine failed to start", t)
                _state.value = State.Failed(t.message ?: t.javaClass.simpleName)
                return@launch
            }
            // Refresh yt-dlp every 3 days in the background.
            val last = prefs.getLong(KEY_LAST_UPDATE, 0)
            if (System.currentTimeMillis() - last > 3L * 24 * 3600 * 1000) update()
        }
    }

    private fun currentVersion(): String? = runCatching { YoutubeDL.getInstance().versionName(context) }.getOrNull()
        ?: runCatching { YoutubeDL.getInstance().version(context) }.getOrNull()

    /** The library only records a version after an update; ask yt-dlp itself otherwise. */
    private fun bundledVersion(): String? = runCatching {
        YoutubeDL.getInstance().execute(YoutubeDLRequest(emptyList<String>()).addOption("--version")).out.trim()
    }.getOrNull()?.takeIf { it.isNotEmpty() && it.length < 40 }

    suspend fun awaitReady() {
        val s = state.first { it !is State.Initializing }
        if (s is State.Failed) throw VideoEngineException(s.message, VideoFormats.ErrorHint.UPDATE)
    }

    /** Updates yt-dlp to the latest stable release. Returns true when a new version was installed. */
    suspend fun update(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (_updating.value) return@withContext Result.success(false)
        _updating.value = true
        try {
            awaitReady()
            val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
            prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
            _state.value = State.Ready(currentVersion() ?: bundledVersion())
            Result.success(status == YoutubeDL.UpdateStatus.DONE)
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            _updating.value = false
        }
    }

    private fun withCookies(request: YoutubeDLRequest, url: String): File? {
        val file = CookieExporter.writeFor(url, cookieDir) ?: return null
        request.addOption("--cookies", file.absolutePath)
        return file
    }

    suspend fun fetchDetails(url: String): VideoDetails = withContext(Dispatchers.IO) {
        awaitReady()
        val request = YoutubeDLRequest(url)
            .addOption("--no-playlist")
            .addOption("--socket-timeout", 20)
        val cookieFile = withCookies(request, url)
        try {
            val info = YoutubeDL.getInstance().getInfo(request)
            val formats = info.formats.orEmpty().map {
                FormatInfo(
                    id = it.formatId ?: "",
                    ext = it.ext,
                    height = it.height,
                    vcodec = it.vcodec,
                    acodec = it.acodec,
                    fileSize = if (it.fileSize > 0) it.fileSize else it.fileSizeApproximate,
                    note = it.formatNote,
                    tbrKbps = it.tbr,
                )
            }
            VideoDetails(
                url = info.webpageUrl ?: url,
                title = info.title ?: info.fulltitle ?: url,
                uploader = info.uploader,
                thumbnail = info.thumbnail,
                durationSec = info.duration,
                platform = Platform.detect(url),
                extractor = info.extractorKey ?: info.extractor,
                options = VideoFormats.buildOptions(formats, info.duration),
            )
        } catch (t: Throwable) {
            val (msg, hint) = VideoFormats.friendlyError(t.message)
            throw VideoEngineException(msg, hint)
        } finally {
            cookieFile?.delete()
        }
    }

    /**
     * Downloads [url] into [workDir] (resuming any partial files there) and returns the finished file.
     * Blocking; cancel through [cancel] with the same [processId].
     */
    fun download(
        url: String,
        workDir: File,
        format: String,
        sort: String?,
        audioFormat: String?,
        processId: String,
        onProgress: (VideoFormats.Progress) -> Unit,
    ): File {
        workDir.mkdirs()
        val request = YoutubeDLRequest(url)
            .addOption("--no-playlist")
            .addOption("--no-mtime")
            .addOption("--continue")
            .addOption("--socket-timeout", 30)
            .addOption("--retries", 10)
            .addOption("--fragment-retries", 10)
            .addOption("-N", 4)
            .addOption("-f", format)
            .addOption("-o", File(workDir, "%(title).90B [%(id)s].%(ext)s").absolutePath)
        if (sort != null) request.addOption("-S", sort)
        if (audioFormat != null) {
            request.addOption("-x").addOption("--audio-format", audioFormat).addOption("--audio-quality", "0")
        } else {
            request.addOption("--merge-output-format", "mp4")
        }
        val cookieFile = withCookies(request, url)
        try {
            YoutubeDL.getInstance().execute(request, processId) { _, _, line ->
                VideoFormats.parseProgress(line)?.let(onProgress)
            }
        } catch (t: Throwable) {
            if (t is YoutubeDL.CanceledException) throw t
            val (msg, _) = VideoFormats.friendlyError(t.message)
            throw java.io.IOException(msg)
        } finally {
            cookieFile?.delete()
        }
        return workDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") && !it.name.contains(".part-Frag") && !it.name.endsWith(".temp") }
            ?.maxByOrNull { it.length() }
            ?: throw java.io.IOException("Download finished but no file was produced")
    }

    fun cancel(processId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
    }

    private companion object {
        const val TAG = "YtDlpEngine"
        const val KEY_LAST_UPDATE = "last_update"
    }
}
