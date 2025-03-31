-dontwarn kotlinx.serialization.**
-dontwarn kotlinx.datetime.**
-dontwarn androidx.lifecycle.**
-dontwarn org.jetbrains.skiko.**
-dontwarn org.jetbrains.kotlin.**
-dontwarn kotlin.**

-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class androidx.** { *; }
-keep class org.jetbrains.** { *; }

-keepclasseswithmembers public class net.lashua.zonedit.MainKt {
    public static void main(java.lang.String[]);
}

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}