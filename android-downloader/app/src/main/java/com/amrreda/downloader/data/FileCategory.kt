package com.amrreda.downloader.data

import android.webkit.MimeTypeMap

enum class FileCategory(val folder: String) {
    VIDEO("Videos"),
    IMAGE("Images"),
    AUDIO("Audio"),
    DOCUMENT("Documents"),
    ARCHIVE("Archives"),
    APK("Apps"),
    OTHER("Other");

    companion object {
        private val VIDEO_EXT = setOf("mp4", "m4v", "webm", "mkv", "mov", "avi", "3gp", "flv", "wmv", "ts", "mpeg", "mpg")
        private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "avif", "ico", "tif", "tiff")
        private val AUDIO_EXT = setOf("mp3", "m4a", "aac", "ogg", "oga", "opus", "wav", "flac", "amr", "wma")
        private val DOC_EXT = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp", "csv", "epub", "json", "xml", "html", "htm", "md")
        private val ARCHIVE_EXT = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz")

        val MEDIA_AND_FILE_EXT: Set<String> = VIDEO_EXT + IMAGE_EXT + AUDIO_EXT + DOC_EXT.minus(setOf("html", "htm", "xml", "json")) + ARCHIVE_EXT + "apk"

        fun extensionOf(nameOrUrl: String?): String {
            if (nameOrUrl.isNullOrBlank()) return ""
            val clean = nameOrUrl.substringBefore('?').substringBefore('#').substringAfterLast('/')
            return clean.substringAfterLast('.', "").lowercase().take(5)
        }

        fun mimeFromName(name: String?): String? {
            val ext = extensionOf(name)
            if (ext.isEmpty()) return null
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }

        fun of(mime: String?, name: String?): FileCategory {
            val m = mime?.lowercase().orEmpty()
            val ext = extensionOf(name)
            return when {
                m.startsWith("video/") || ext in VIDEO_EXT -> VIDEO
                m.startsWith("image/") || ext in IMAGE_EXT -> IMAGE
                m.startsWith("audio/") || ext in AUDIO_EXT -> AUDIO
                m == "application/vnd.android.package-archive" || ext == "apk" -> APK
                ext in ARCHIVE_EXT || m.contains("zip") || m.contains("rar") || m.contains("7z") || m.contains("tar") -> ARCHIVE
                ext in DOC_EXT || m.startsWith("text/") || m.contains("pdf") || m.contains("document") ||
                    m.contains("msword") || m.contains("sheet") || m.contains("presentation") || m.contains("epub") -> DOCUMENT
                else -> OTHER
            }
        }
    }
}
