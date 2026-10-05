# =====================================================================
# Harmonia Studio ProGuard / R8 Rules for Production Minification
# =====================================================================

# 1. Preserve Line Numbers & Attributes for stack traces and debugging
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 2. Native Audio Synthesis Engine & Timbre DSP Modules
# Keep all audio processing classes, methods, and member variables
-keep class com.example.audio.** { *; }
-keepclassmembers class com.example.audio.** { *; }
-keep enum com.example.audio.** { *; }

# 3. Android System Audio & Media APIs
# Preserve AudioTrack, AudioRecord, MediaRecorder & AudioAttributes reflection
-keep class android.media.AudioTrack { *; }
-keep class android.media.AudioRecord { *; }
-keep class android.media.AudioFormat { *; }
-keep class android.media.AudioAttributes { *; }
-keep class android.media.MediaRecorder** { *; }
-keep class android.media.MediaPlayer** { *; }
-dontwarn android.media.**

# 4. MIDI Processing, Recording & Step Sequencer Modules
# Keep all MIDI data classes, event structures, binary SMF parsers and writers
-keep class com.example.midi.** { *; }
-keepclassmembers class com.example.midi.** { *; }
-keep enum com.example.midi.** { *; }

# 5. Local Room Database, Entities & DAOs
# Ensure Room reflection, schema tables, and KSP generated classes are retained
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# 6. JSON Serialization & Deserialization (Notes, Project Interchange & Collaboration)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.example.collab.** { *; }
-keepclassmembers class com.example.collab.** { *; }

# 7. Kotlin Coroutines & Flow Asynchronous Tasks
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# 8. Kotlin Enums & Reflection Support
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 9. Jetpack Compose & ViewModel Lifecycle
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class com.example.viewmodel.** { *; }
