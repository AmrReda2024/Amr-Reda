package com.amrreda.downloader

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.amrreda.downloader.data.AppDatabase
import com.amrreda.downloader.data.AppSettings
import com.amrreda.downloader.engine.DownloadEngine
import com.amrreda.downloader.engine.Notifications
import com.amrreda.downloader.engine.YtDlpEngine
import com.amrreda.downloader.extract.MediaExtractor

class DownloaderApp : Application(), ImageLoaderFactory {
    lateinit var database: AppDatabase
        private set
    lateinit var settings: AppSettings
        private set
    lateinit var engine: DownloadEngine
        private set
    lateinit var extractor: MediaExtractor
        private set
    lateinit var video: YtDlpEngine
        private set

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        database = AppDatabase.create(this)
        settings = AppSettings(this)
        video = YtDlpEngine(this).also { it.start() }
        engine = DownloadEngine(this, database.downloads(), settings, video)
        extractor = MediaExtractor(engine.client)
    }

    // Lets Coil render thumbnails for downloaded videos as well as images.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
