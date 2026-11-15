# Smart Downloader — المُنزِّل الذكي

A complete Android app (Kotlin + Jetpack Compose) for downloading **videos and audio from YouTube, Instagram, Facebook, TikTok, X and 1000+ sites**, plus files and images from any link or web page.
تطبيق أندرويد متكامل لتنزيل الفيديوهات والصوتيات من يوتيوب وإنستجرام وفيسبوك وتيك توك وX وأكثر من 1000 موقع، إضافة إلى الملفات والصور من أي رابط أو صفحة ويب.

## Features / المميزات

| | English | العربية |
|---|---|---|
| 🎬 | **YouTube / Instagram / Facebook / TikTok / X** + 1000 sites via the embedded open-source **yt-dlp** and **FFmpeg**: quality picker (144p–4K) with sizes, MP3/M4A audio extraction, auto-merged MP4 | **يوتيوب وإنستجرام وفيسبوك وتيك توك وX** وأكثر من 1000 موقع: اختيار الجودة من 144p حتى 4K مع الحجم، واستخراج الصوت MP3/M4A |
| 🔐 | **Accounts**: sign in on the platform's own page inside the app for private / login-only content (Instagram, age-restricted YouTube) | **الحسابات**: تسجيل الدخول من صفحة المنصة نفسها للمحتوى الخاص |
| 🔄 | Video engine updates itself every few days (sites change often) + manual "Update" button | محرّك الفيديو يحدّث نفسه تلقائيًا |
| 📋 | Detects a copied link and offers to download it | يكتشف الرابط المنسوخ ويعرض تنزيله |
| ⬇️ | Download any direct link, optional custom file name; automatic retry when a CDN blocks browser-like clients | تنزيل أي رابط مباشر مع إمكانية تسمية الملف |
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

* **From GitHub:** *Actions* tab → *Android Downloader APK* → latest run → `SmartDownloader-apk` artifact → install `app-arm64-v8a-release.apk` (almost all phones since 2017) or `app-armeabi-v7a-release.apk` (older 32-bit phones).
* **Build locally:** `cd android-downloader && ./gradlew assembleRelease` → `app/build/outputs/apk/release/`.

Requires Android 7.0 (API 24) or newer. One APK per processor type keeps the download at ~50 MB (the video engine ships Python, yt-dlp and FFmpeg).

### Signing (so updates install over the previous version)
Android only installs an update signed with the same key. Add the app's keystore as repository secret `SIGNING_KEYSTORE_BASE64` (`base64 -w0 smartdownloader-signing.keystore`); optional secrets `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` default to the Android debug-key values. Keep the keystore private and backed up.

## Architecture

```
app/src/main/java/com/amrreda/downloader/
├── DownloaderApp.kt          service locator: DB, settings, engine, extractor, Coil
├── data/                     Room entity/DAO, FileCategory, AppSettings
├── engine/DownloadEngine.kt  queue, concurrency, Range resume, cookies/referer, Wi-Fi gate
├── engine/StorageSaver.kt    MediaStore (Android 10+) / public Downloads (older)
├── engine/YtDlpEngine.kt     yt-dlp + FFmpeg: init, auto-update, video info, downloads
├── engine/VideoFormats.kt    quality options, progress parsing, error classification
├── engine/CookieExporter.kt  in-app sign-in cookies → Netscape cookies.txt for yt-dlp
├── extract/Platform.kt       YouTube / Instagram / Facebook / TikTok / X detection
├── extract/MediaExtractor.kt HTML media discovery (Jsoup)
├── service/DownloadService   foreground service + progress notification
└── ui/                       Compose screens: Downloads, Browser, Files, Settings
```

## Limits & responsible use / حدود الاستخدام

* DRM-protected content (paid streaming services) is not supported.
* Download only content you own or are authorised to save. YouTube, Instagram and Facebook terms restrict downloading outside their own features; copyright law applies to the content itself.
* Licensing: the embedded youtubedl-android library is GPL-3.0, so distributing this app requires making its source available under GPL-compatible terms. Apps of this kind are not accepted on Google Play; distribute the APK directly.
