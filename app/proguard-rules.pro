# ProGuard rules for Lojia Shift Report
# Narrow keep rules: keep Room entities, DAOs, and data classes in com.lojia.shiftreport.data
-keep class com.lojia.shiftreport.data.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-keep class com.lojia.shiftreport.scanner.ScannedDocument { *; }
-keep class com.lojia.shiftreport.scanner.DocumentScannerDao { *; }
-keep class com.lojia.shiftreport.settings.backup.** { *; }
-keep class com.lojia.shiftreport.util.DriveBackupItem { *; }
-keep class com.lojia.shiftreport.util.DriveAccountInfo { *; }
-keep class com.lojia.shiftreport.util.OfflineBackupManager$** { *; }
-dontwarn com.lojia.shiftreport.**

# Preserve generic signatures, inner classes, and runtime annotations for Room/reflection
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

# Jetpack Compose
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
-dontwarn androidx.compose.**

# Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * implements androidx.room.migration.Migration { *; }

# CameraX & ML Kit
-keep class androidx.camera.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn androidx.camera.**
-dontwarn com.google.mlkit.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# MSAL excluded / optional compile-time references (display-mask, opentelemetry, findbugs, bouncycastle)
-dontwarn com.microsoft.device.display.**
-dontwarn io.opentelemetry.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn org.bouncycastle.**
