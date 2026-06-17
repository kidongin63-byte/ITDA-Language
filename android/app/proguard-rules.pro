# ITDA Language ProGuard Rules

# WebView JavaScript Bridge
-keepclassmembers class com.itda.language.ui.webview.NativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson (API 모델)
-keep class com.itda.language.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Compose (기본적으로 R8이 처리)
-dontwarn androidx.compose.**
