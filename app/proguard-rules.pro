# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler
-keepnames class kotlinx.coroutines.test.**

# Tink & Nearby (for Step 2 readiness)
-keepclassmembers class * {
    @com.google.crypto.tink.shaded.protobuf.* <fields>;
}
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
