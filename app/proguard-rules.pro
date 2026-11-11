# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in defaultProguardFile('proguard-android-optimize.txt')

# Keep data classes and models used in serialization or Room
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Keep Hilt generated classes
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep standard enum values for configuration matching
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
