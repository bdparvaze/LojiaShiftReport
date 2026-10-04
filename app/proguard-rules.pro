# ProGuard rules for Lojia Shift Report
# Narrow keep rules: keep Room entities, DAOs, and data classes in com.lojia.shiftreport.data
-keep class com.lojia.shiftreport.data.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.lojia.shiftreport.scanner.ScannedDocument { *; }
-keep class com.lojia.shiftreport.scanner.DocumentScannerDao { *; }
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
