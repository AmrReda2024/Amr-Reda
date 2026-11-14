# Smart Downloader — المُنزِّل الذكي

A complete Android app (Kotlin + Jetpack Compose) for downloading **files, images, videos and audio** — from direct links or from any web page.
تطبيق أندرويد متكامل لتنزيل الملفات والصور والفيديوهات والصوتيات من الروابط المباشرة أو من أي صفحة ويب.

## Features / المميزات

| | English | العربية |
|---|---|---|
| ⬇️ | Download any direct link, optional custom file name | تنزيل أي رابط مباشر مع إمكانية تسمية الملف |
| 🔍 | **Find media**: scans a web page (video/audio/img/srcset/Open Graph/links) and lists everything downloadable, with multi-select | **استخراج الوسائط** من أي صفحة ويب مع تحديد متعدد |
| 🌐 | **In-app browser** that detects videos as they play, a floating "Media" button, long-press any image/link to download, and site-triggered downloads | **متصفح مدمج** يلتقط الفيديوهات أثناء تشغيلها، وضغطة مطوّلة على أي صورة أو رابط لتنزيله |
| ⏯️ | Pause / resume (HTTP Range — continues from the last byte), retry, cancel | إيقاف مؤقت واستئناف من نفس النقطة، وإعادة المحاولة |
| ⚡ | Parallel downloads (1–6), live speed, %, ETA | تنزيلات متزامنة مع السرعة والنسبة والوقت المتبقي |
| 🔔 | Background downloads with a progress notification + "Pause all"; completion notifications | التنزيل في الخلفية مع إشعارات |
| 📁 | Files tab with categories (Videos, Images, Audio, Documents, Archives, Apps), search, thumbnails, open/share/delete | تبويب الملفات مصنّف مع بحث ومعاينة وفتح ومشاركة وحذف |
| 📤 | Share a link to the app from WhatsApp, Chrome, etc. — it is analysed automatically | مشاركة أي رابط للتطبيق من أي تطبيق آخر |
| 📶 | Wi-Fi-only mode; queue survives restarts and resumes automatically | وضع Wi-Fi فقط، والقائمة تُستأنف تلقائيًا بعد إعادة التشغيل |
| 🌙 | Arabic (RTL) + English, light/dark, Material You colours | عربي وإنجليزي، وضع ليلي |

Files are saved to `Download/SmartDownloader/<Videos|Images|Audio|Documents|…>/`; photos and videos also show up in the Gallery.

## Install / التثبيت

* **From GitHub:** open the *Actions* tab → *Android Downloader APK* → latest run → download the `SmartDownloader-apk` artifact, unzip, and install `app-release.apk` (allow "Install unknown apps").
* **Build locally:** `cd android-downloader && ./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.

Requires Android 7.0 (API 24) or newer. The release build is signed with the debug key so it installs directly; create your own keystore before publishing to Google Play.

## Architecture

```
app/src/main/java/com/amrreda/downloader/
├── DownloaderApp.kt          service locator: DB, settings, engine, extractor, Coil
├── data/                     Room entity/DAO, FileCategory, AppSettings
├── engine/DownloadEngine.kt  queue, concurrency, Range resume, cookies/referer, Wi-Fi gate
├── engine/StorageSaver.kt    MediaStore (Android 10+) / public Downloads (older)
├── extract/MediaExtractor.kt HTML media discovery (Jsoup)
├── service/DownloadService   foreground service + progress notification
└── ui/                       Compose screens: Downloads, Browser, Files, Settings
```

## Limits & responsible use / حدود الاستخدام

* Works with content that is reachable as a normal file URL. DRM-protected streams and segmented streams (HLS `.m3u8` / DASH) are intentionally not supported.
* Download only content you own or are authorised to save; many platforms' terms prohibit downloading their content.
