-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keep class ceui.pixiv.shaftapi.ShaftHmac { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
