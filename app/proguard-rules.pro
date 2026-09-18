# ============================================================================
# Noor Pro / Deen — R8 / ProGuard keep rules
# ============================================================================

# Keep useful attributes for reflection, generics, and readable crash traces.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes *Annotation*
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ----------------------------------------------------------------------------
# App data models — serialized via Moshi (network JSON) and Firebase Firestore
# (reflection on POJOs via toObject()). Keep them intact to be safe.
# ----------------------------------------------------------------------------
-keep class com.noorpro.app.data.** { *; }
-keepclassmembers class com.noorpro.app.data.** { *; }
# BuildConfig (read at runtime)
-keep class com.example.BuildConfig { *; }

# ----------------------------------------------------------------------------
# Moshi
# ----------------------------------------------------------------------------
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier @interface *
-keepnames @com.squareup.moshi.JsonClass class *
-keep,allowobfuscation @com.squareup.moshi.JsonClass class *
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
    **[] values();
}
# Generated Moshi adapters (KSP codegen)
-keep class **JsonAdapter {
    <init>(...);
    <fields>;
}
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker

# ----------------------------------------------------------------------------
# Retrofit
# ----------------------------------------------------------------------------
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# ----------------------------------------------------------------------------
# OkHttp / Okio
# ----------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ----------------------------------------------------------------------------
# Firebase / Firestore
# (Firebase ships consumer rules; these protect app POJOs used with Firestore.)
# ----------------------------------------------------------------------------
-keepattributes *Annotation*
-keepclassmembers class com.example.** {
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
}
-keepclassmembers class com.noorpro.app.** {
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ----------------------------------------------------------------------------
# Kotlin / Coroutines
# ----------------------------------------------------------------------------
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.**

# ----------------------------------------------------------------------------
# Enums (used in when-expressions and serialization)
# ----------------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
