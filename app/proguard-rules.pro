# Add project specific ProGuard rules here.
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** INSTANCE;
}
-keep class com.elocho.snooker.data.model.** { *; }

# Prevent Compose runtime/animation methods from being incorrectly stripped/optimized
# in release builds, which can cause NoSuchMethodError at runtime on some screens.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
