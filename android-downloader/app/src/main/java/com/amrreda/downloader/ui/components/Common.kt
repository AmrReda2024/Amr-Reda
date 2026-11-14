package com.amrreda.downloader.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.amrreda.downloader.R
import com.amrreda.downloader.data.FileCategory
import com.amrreda.downloader.engine.Notifications

fun FileCategory.icon(): ImageVector = when (this) {
    FileCategory.VIDEO -> Icons.Filled.Movie
    FileCategory.IMAGE -> Icons.Filled.Image
    FileCategory.AUDIO -> Icons.Filled.AudioFile
    FileCategory.DOCUMENT -> Icons.Filled.Description
    FileCategory.ARCHIVE -> Icons.Filled.Archive
    FileCategory.APK -> Icons.Filled.Android
    FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
}

fun FileCategory.label(): Int = when (this) {
    FileCategory.VIDEO -> R.string.cat_video
    FileCategory.IMAGE -> R.string.cat_image
    FileCategory.AUDIO -> R.string.cat_audio
    FileCategory.DOCUMENT -> R.string.cat_document
    FileCategory.ARCHIVE -> R.string.cat_archive
    FileCategory.APK -> R.string.cat_apk
    FileCategory.OTHER -> R.string.cat_other
}

/** Thumbnail for images/videos (local file or remote URL) with a category icon fallback. */
@Composable
fun Thumb(model: Any?, category: FileCategory, size: Dp = 52.dp) {
    val shape = RoundedCornerShape(10.dp)
    // Remote videos are never loaded for thumbnails (that would download the whole file twice).
    val isLocal = model is String && (model.startsWith("content://") || model.startsWith("file://"))
    val showImage = model != null && (category == FileCategory.IMAGE || (category == FileCategory.VIDEO && isLocal))
    Box(
        Modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val fallback = @Composable {
            Icon(category.icon(), null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        if (showImage) {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
                loading = { fallback() },
                error = { fallback() },
            )
        } else fallback()
    }
}

object FileActions {
    fun open(context: Context, uri: String?, mime: String?) {
        if (uri == null) return
        try {
            context.startActivity(
                Intent.createChooser(Notifications.openFileIntent(Uri.parse(uri), mime), null)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_app_to_open, Toast.LENGTH_SHORT).show()
        }
    }

    fun share(context: Context, uri: String?, mime: String?) {
        if (uri == null) return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun copyLink(context: Context, url: String) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("url", url))
        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
    }
}
