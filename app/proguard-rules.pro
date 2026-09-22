# R8 rules for the release build.
#
# Only what this app actually needs. Everything here has a reason written
# next to it, because a keep rule nobody understands is one nobody dares
# remove.

# --- kotlinx.serialization ---------------------------------------------------
# Serializers are generated as companion objects and looked up by name. R8
# does not see those lookups, and without these the release build crashes
# the first time it parses JSON — which is the menu, so: immediately.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.rimagwinya.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.rimagwinya.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rimagwinya.app.**$$serializer { *; }
-keep class kotlinx.serialization.** { *; }

# --- Retrofit / OkHttp -------------------------------------------------------
# Retrofit reads the annotations and the generic return types of the API
# interfaces through reflection at runtime.
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation interface com.rimagwinya.app.data.remote.**
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- supabase-kt and Ktor ----------------------------------------------------
# The session and the realtime payloads are serialized the same way, and
# Ktor picks its engine reflectively.
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# --- Room --------------------------------------------------------------------
-keep class com.rimagwinya.app.core.database.** { *; }

# --- Firebase Messaging ------------------------------------------------------
# The service is found by name from the manifest.
-keep class com.rimagwinya.app.notifications.RimagwinyaMessagingService { *; }

# --- Our own models ----------------------------------------------------------
# DTOs and domain models are serialized or read reflectively by the above.
-keep class com.rimagwinya.app.domain.model.** { *; }
-keep class com.rimagwinya.app.domain.pricing.Selection { *; }

# --- Keep line numbers in crash reports, hide the file names -----------------
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
