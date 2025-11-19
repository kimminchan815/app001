# Add project specific ProGuard rules here.
-keep class com.example.proximityalert.** { *; }
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}
