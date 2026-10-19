# WorkManager instantiates workers by reflection.
-keep class com.websitecheck.app.work.CheckWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Manifest components are kept by AGP, but keep this explicitly for release builds.
-keep class com.websitecheck.app.work.TimeChangeReceiver { *; }

# Keep annotations used by Room and Kotlin metadata.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
