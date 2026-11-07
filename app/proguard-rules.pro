# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to rules stored
# in /android-sdk/tools/proguard/proguard-android.txt
# and can be altered by the build system.

# Keep Nearby Connections classes used reflectively by Google Play services.
-keep class com.google.android.gms.nearby.connection.** { *; }

# Preserve model classes used for transfer history JSON.
-keep class com.example.swiftshare.domain.TransferRecord { *; }

-keep class com.example.swiftshare.domain.FileItem { *; }
