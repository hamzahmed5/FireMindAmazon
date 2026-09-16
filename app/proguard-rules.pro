# FireMind ProGuard rules.
# Kotlinx serialization: keep generated serializers for data models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class com.firemind.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.firemind.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.firemind.app.**$$serializer { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
