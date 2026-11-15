-dontwarn org.jspecify.annotations.**
-dontwarn com.google.re2j.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }

# yt-dlp wrapper: JSON is mapped by Jackson through reflection.
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn org.brotli.dec.**
-dontwarn com.github.luben.zstd.**
-dontwarn java.beans.**
# Commons Compress builds its ZIP extra-field registry by reflection (used to unpack Python on first launch).
-keep class org.apache.commons.compress.** { *; }
-keep class org.apache.commons.io.** { *; }
