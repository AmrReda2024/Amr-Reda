package com.amrreda.downloader.engine

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amrreda.downloader.R
import com.amrreda.downloader.data.DownloadEntity

object Notifications {
    const val CHANNEL_PROGRESS = "downloads_progress"
    const val CHANNEL_DONE = "downloads_done"
    const val PROGRESS_ID = 1001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, context.getString(R.string.channel_progress), NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, context.getString(R.string.channel_done), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun openFileIntent(uri: Uri, mime: String?): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun notifyCompleted(context: Context, entity: DownloadEntity) {
        if (!canNotify(context)) return
        val uri = entity.contentUri?.let(Uri::parse) ?: return
        val pi = PendingIntent.getActivity(
            context, entity.id.toInt(),
            Intent.createChooser(openFileIntent(uri, entity.mimeType), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.notif_completed))
            .setContentText(entity.fileName)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(2000 + entity.id.toInt(), n)
        } catch (_: SecurityException) { }
    }

    fun notifyFailed(context: Context, entity: DownloadEntity) {
        if (!canNotify(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.notif_failed))
            .setContentText(entity.fileName)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(2000 + entity.id.toInt(), n)
        } catch (_: SecurityException) { }
    }
}
