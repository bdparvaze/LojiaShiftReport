# ProGuard rules for Lojia Shift Report
-keep class com.lojia.shiftreport.** { *; }
-dontwarn com.lojia.shiftreport.**

# Jetpack Compose
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
-dontwarn androidx.compose.**

# Room Database
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
-keep class * implements androidx.room.migration.Migration

# CameraX & ML Kit
-keep class androidx.camera.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn androidx.camera.**
-dontwarn com.google.mlkit.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
