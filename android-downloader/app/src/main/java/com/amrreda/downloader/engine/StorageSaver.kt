package com.amrreda.downloader.engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.amrreda.downloader.data.FileCategory
import java.io.File
import java.io.IOException

/**
 * Moves a finished file into public storage:
 *   Download/SmartDownloader/<Videos|Images|Audio|Documents|...>/
 * Android 10+ uses MediaStore (no storage permission needed); older versions write directly.
 * Images, videos and music in these folders appear automatically in the Gallery / music apps.
 */
object StorageSaver {
    const val ROOT_FOLDER = "SmartDownloader"

    fun publish(context: Context, source: File, name: String, mime: String?, category: FileCategory): Uri {
        val mimeType = mime ?: "application/octet-stream"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$ROOT_FOLDER/${category.folder}")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create file in Downloads")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    source.inputStream().use { it.copyTo(out, 256 * 1024) }
                } ?: throw IOException("Could not open output stream")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (t: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw t
            }
            uri
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$ROOT_FOLDER/${category.folder}")
            if (!dir.exists() && !dir.mkdirs()) throw IOException("Cannot create ${dir.path}")
            val dest = uniqueFile(dir, name)
            if (!source.renameTo(dest)) {
                source.copyTo(dest, overwrite = true)
            }
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mimeType), null)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
        }
    }

    fun delete(context: Context, uri: String?) {
        if (uri.isNullOrBlank()) return
        runCatching { context.contentResolver.delete(Uri.parse(uri), null, null) }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var file = File(dir, name)
        if (!file.exists()) return file
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        var i = 1
        while (file.exists()) {
            file = File(dir, "$base ($i)$ext")
            i++
        }
        return file
    }
}
