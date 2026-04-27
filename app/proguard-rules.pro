##################################################
# 🔹 Debugging (keep stack traces readable)
##################################################
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

##################################################
# 🔹 Hilt / Dagger (CRITICAL)
##################################################
-keep class javax.inject.** { *; }
-keep class dagger.hilt.** { *; }

# Hilt generated classes
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Factories & Injectors
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **_HiltModules_* { *; }

# ViewModel (used with Hilt)
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep annotations (VERY IMPORTANT)
-keepattributes *Annotation*

##################################################
# 🔹 Jetpack Compose (SAFE + minimal)
##################################################
# Required for Compose runtime
-keep class kotlin.Metadata { *; }

# Suppress warnings
-dontwarn androidx.compose.**

##################################################
# 🔹 Kotlin Coroutines
##################################################
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

##################################################
# 🔹 Kotlin Serialization
##################################################
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

##################################################
# 🔹 LibSu (Root library)
##################################################
-keep class com.topjohnwu.superuser.** { *; }
-keep class com.topjohnwu.superuser.internal.** { *; }

##################################################
# 🔹 WPS Connection Library
##################################################
-keep class com.github.fulvius31.** { *; }

##################################################
# 🔹 Your App Logic (reflection-heavy)
##################################################
# Algorithms
-keep class sangiorgi.wps.opensource.algorithm.** { *; }
-keep class sangiorgi.wps.opensource.algorithm.strategy.** { *; }
-keep class sangiorgi.wps.opensource.algorithm.impl.** { *; }

# Connection + callbacks
-keep class sangiorgi.wps.opensource.connection.models.** { *; }
-keep class sangiorgi.wps.opensource.connection.ConnectionUpdateCallback { *; }

# Domain models
-keep class sangiorgi.wps.opensource.domain.models.** { *; }

# Data models
-keep class sangiorgi.wps.opensource.data.models.** { *; }

##################################################
# 🔹 Android Components
##################################################
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }

##################################################
# 🔹 BuildConfig
##################################################
-keep class sangiorgi.wps.opensource.BuildConfig { *; }

##################################################
# 🔹 Parcelable
##################################################
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

##################################################
# 🔹 Enums
##################################################
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

##################################################
# 🔹 Commons Lang
##################################################
-keep class org.apache.commons.lang3.** { *; }

##################################################
# 🔹 Warnings cleanup
##################################################
-dontwarn kotlinx.coroutines.**
-dontwarn dagger.hilt.**
-dontwarn org.apache.commons.lang3.**
-dontwarn java.lang.invoke.**
