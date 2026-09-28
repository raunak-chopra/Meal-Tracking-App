# Kalo ProGuard Configuration

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Supabase
-keep class io.github.jan.supabase.** { *; }

# Ktor Client
-keep class io.ktor.** { *; }

# Health Connect
-keep class androidx.health.connect.** { *; }
-dontwarn androidx.health.connect.**

# Room Database
-keep class * extends androidx.room.RoomDatabase

# Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Google ML Kit Barcode Scanning
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Jetpack Glance
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**


