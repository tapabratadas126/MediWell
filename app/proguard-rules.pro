# Add project specific ProGuard rules here.
# You can control the set of applied keep options by using the -keep, -keepclassmembers,
# and other keep rules.

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Supabase and Ktor models
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class kotlinx.serialization.json.** { *; }
-keepclassmembers class * {
    companion object *;
}
