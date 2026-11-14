package com.amrreda.downloader.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.amrreda.downloader.DownloaderApp
import com.amrreda.downloader.R
import com.amrreda.downloader.data.DownloadStatus
import com.amrreda.downloader.ui.Format
import com.amrreda.downloader.ui.MainActivity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Keeps downloads alive when the app is in the background and shows an
 * ongoing notification with overall progress and a "Pause all" action.
 */
class DownloadService : LifecycleService() {

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        startInForeground(buildNotification(0, 0, -1))
        val app = application as DownloaderApp
        lifecycleScope.launch {
            app.database.downloads().observeActive()
                .debounce(700)
                .collect { list ->
                    val running = list.filter { it.statusEnum == DownloadStatus.RUNNING }
                    if (running.isEmpty()) {
                        ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collect
                    }
                    val known = running.filter { it.totalBytes > 0 }
                    val percent = if (known.isEmpty()) -1
                    else ((known.sumOf { it.downloadedBytes } * 100) / known.sumOf { it.totalBytes }).toInt()
                    val speed = running.sumOf { it.speedBps }
                    try {
                        NotificationManagerCompat.from(this@DownloadService)
                            .notify(com.amrreda.downloader.engine.Notifications.PROGRESS_ID, buildNotification(running.size, speed, percent))
                    } catch (_: SecurityException) { }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_PAUSE_ALL) {
            (application as DownloaderApp).engine.pauseAll()
        }
        return START_NOT_STICKY
    }

    // Android 15+: dataSync services have a daily time budget; stop cleanly instead of crashing.
    override fun onTimeout(startId: Int, fgsType: Int) {
        (application as DownloaderApp).engine.pauseAll()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startInForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, com.amrreda.downloader.engine.Notifications.PROGRESS_ID, notification, type)
    }

    private fun buildNotification(count: Int, speed: Long, percent: Int): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseIntent = Intent(this, DownloadService::class.java).setAction(ACTION_PAUSE_ALL)
        val pause = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            PendingIntent.getForegroundService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
        else PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)

        val text = if (count == 0) getString(R.string.notif_preparing)
        else resources.getQuantityString(R.plurals.notif_downloading, count, count) + " • " + Format.speed(speed)
        return NotificationCompat.Builder(this, com.amrreda.downloader.engine.Notifications.CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .setProgress(100, percent.coerceAtLeast(0), percent < 0)
            .addAction(0, getString(R.string.pause_all), pause)
            .build()
    }

    companion object {
        const val ACTION_PAUSE_ALL = "com.amrreda.downloader.PAUSE_ALL"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, DownloadService::class.java))
            } catch (_: Exception) {
                // Background-start restrictions: downloads still continue while the process lives.
            }
        }
    }
}
