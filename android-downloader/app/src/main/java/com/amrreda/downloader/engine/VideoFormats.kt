package com.amrreda.downloader.engine

/** Minimal, library-independent view of one yt-dlp format (keeps the logic unit-testable). */
data class FormatInfo(
    val id: String,
    val ext: String?,
    val height: Int,
    val vcodec: String?,
    val acodec: String?,
    val fileSize: Long,
    val note: String? = null,
    val tbrKbps: Int = 0,
) {
    // A missing codec means "unknown" (Facebook's "sd"/"hd" files report none): assume a normal video file with sound.
    val hasVideo: Boolean get() = vcodec != "none" && ext !in AUDIO_EXTS && ext !in NON_MEDIA_EXTS
    val hasAudio: Boolean get() = acodec != "none" && ext !in NON_MEDIA_EXTS

    private companion object {
        val AUDIO_EXTS = setOf("m4a", "mp3", "aac", "opus", "ogg", "oga", "wav", "flac", "weba")
        val NON_MEDIA_EXTS = setOf("mhtml", "json", "vtt", "srt")
    }
}

/** One choice shown in the quality picker. */
data class QualityOption(
    val label: String,
    /** yt-dlp `-f` expression. */
    val format: String,
    /** yt-dlp `-S` sort expression, or null. */
    val sort: String? = null,
    /** Audio extraction target ("mp3", "m4a") or null for video. */
    val audioFormat: String? = null,
    val estimatedBytes: Long = -1,
    val height: Int = 0,
) {
    val isAudio: Boolean get() = audioFormat != null
}

object VideoFormats {
    private val STANDARD_HEIGHTS = listOf(2160, 1440, 1080, 720, 480, 360, 240, 144)

    /** Snaps odd heights (e.g. 1012, 718) to the nearest standard label used in the picker. */
    fun standardHeight(h: Int): Int = STANDARD_HEIGHTS.firstOrNull { it <= h * 1.1 } ?: h

    private fun bestSize(candidates: List<FormatInfo>): Long =
        candidates.filter { it.fileSize > 0 }.maxByOrNull { it.fileSize }?.fileSize ?: -1

    fun buildOptions(formats: List<FormatInfo>, durationSec: Int = 0): List<QualityOption> {
        val options = mutableListOf<QualityOption>()
        val video = formats.filter { it.hasVideo }
        val audio = formats.filter { it.hasAudio && !it.hasVideo }
        val bestAudioSize = bestSize(audio).let { if (it < 0 && durationSec > 0) durationSec * 16_000L else it }

        val heights = video.map { it.height }.filter { it > 0 }.map(::standardHeight).distinct().sortedDescending()
        if (heights.isNotEmpty()) {
            heights.forEach { h ->
                val atHeight = video.filter { standardHeight(it.height) == h }
                val preferred = atHeight.filter { it.ext == "mp4" }.ifEmpty { atHeight }
                val videoSize = bestSize(preferred)
                val size = when {
                    videoSize < 0 -> -1
                    preferred.any { it.hasAudio } -> videoSize
                    else -> videoSize + maxOf(bestAudioSize, 0)
                }
                options += QualityOption(
                    label = "${h}p",
                    format = "bv*+ba/b",
                    // Largest resolution not above h, preferring H.264/AAC MP4 for maximum phone compatibility.
                    sort = "res:$h,vcodec:h264,acodec:aac,ext:mp4:m4a",
                    estimatedBytes = size,
                    height = h,
                )
            }
        } else {
            // Sites like Facebook often expose only labelled progressive files (e.g. "hd", "sd").
            options += QualityOption("BEST", "bv*+ba/b", sort = "vcodec:h264,acodec:aac,ext:mp4:m4a",
                estimatedBytes = bestSize(video))
            video.filter { it.hasAudio }
                .distinctBy { it.id }
                // Higher quality first: known size/bitrate, then "hd" before "sd".
                .sortedWith(compareByDescending<FormatInfo> { it.fileSize }.thenByDescending { it.tbrKbps }
                    .thenByDescending { (it.note ?: it.id).contains("hd", ignoreCase = true) })
                .forEach { f ->
                    val label = (f.note?.takeIf { it.isNotBlank() } ?: f.id).uppercase()
                    if (options.none { it.label == label }) options += QualityOption(label, f.id, estimatedBytes = f.fileSize)
                }
        }
        if (audio.isNotEmpty() || video.any { it.hasAudio } || formats.isEmpty()) {
            options += QualityOption("MP3", "ba/b", audioFormat = "mp3", estimatedBytes = bestAudioSize)
            options += QualityOption("M4A", "ba[ext=m4a]/ba/b", audioFormat = "m4a", estimatedBytes = bestAudioSize)
        }
        return options
    }

    data class Progress(val percent: Float, val totalBytes: Long, val speedBps: Long, val etaSec: Long)

    private val PROGRESS_RE = Regex("""\[download]\s+([\d.]+)%(?:\s+of\s+~?\s*([\d.]+)\s*([KMGT]?i?B))?(?:.*?\bat\s+([\d.]+)\s*([KMGT]?i?B)/s)?(?:.*?ETA\s+([\d:]+))?""")

    /** Parses a yt-dlp progress line such as `[download]  42.0% of ~ 10.05MiB at  1.20MiB/s ETA 00:07`. */
    fun parseProgress(line: String): Progress? {
        val m = PROGRESS_RE.find(line) ?: return null
        val pct = m.groupValues[1].toFloatOrNull() ?: return null
        val total = toBytes(m.groupValues[2], m.groupValues[3])
        val speed = toBytes(m.groupValues[4], m.groupValues[5])
        val eta = m.groupValues[6].takeIf { it.isNotEmpty() }?.split(':')?.fold(0L) { acc, p -> acc * 60 + (p.toLongOrNull() ?: 0) } ?: -1
        return Progress(pct, total, speed, eta)
    }

    private fun toBytes(num: String, unit: String): Long {
        val v = num.toDoubleOrNull() ?: return -1
        val mult = when (unit.uppercase().replace("I", "")) {
            "KB" -> 1024.0
            "MB" -> 1024.0 * 1024
            "GB" -> 1024.0 * 1024 * 1024
            "TB" -> 1024.0 * 1024 * 1024 * 1024
            else -> 1.0
        }
        return (v * mult).toLong()
    }

    /** Turns raw yt-dlp error output into a short message plus a hint key. */
    fun friendlyError(raw: String?): Pair<String, ErrorHint> {
        val text = raw.orEmpty()
        val errorLine = text.lines().lastOrNull { it.contains("ERROR") }?.substringAfter("ERROR:")?.trim()
            ?: text.lines().lastOrNull { it.isNotBlank() }?.trim() ?: "Unknown error"
        val lower = text.lowercase()
        val hint = when {
            "sign in" in lower || "login" in lower || "log in" in lower || "cookies" in lower ||
                "not a bot" in lower || "private" in lower || "age" in lower && "confirm" in lower -> ErrorHint.SIGN_IN
            "unsupported url" in lower -> ErrorHint.UNSUPPORTED
            "429" in lower || "rate" in lower && "limit" in lower -> ErrorHint.RATE_LIMIT
            "unable to resolve" in lower || "network is unreachable" in lower || "timed out" in lower -> ErrorHint.NETWORK
            else -> ErrorHint.UPDATE
        }
        return errorLine.take(300) to hint
    }

    enum class ErrorHint { SIGN_IN, UNSUPPORTED, RATE_LIMIT, NETWORK, UPDATE }
}
