# Keep Kotlinx Serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.moneytracker.app.data.model.**$$serializer { *; }
-keepclassmembers class com.moneytracker.app.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.moneytracker.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
