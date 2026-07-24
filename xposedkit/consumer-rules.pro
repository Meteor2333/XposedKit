-keep,allowoptimization,allowobfuscation class cc.meteormc.xposedkit.** { *; }
-keep,allowoptimization,allowobfuscation class * extends cc.meteormc.xposedkit.XposedModule

-dontwarn android.app.AndroidAppHelper
-dontwarn android.content.res.XModuleResources
-dontwarn android.content.res.XResForwarder
-dontwarn android.content.res.XResources
-dontwarn de.robv.android.xposed.**
-dontwarn io.github.libxposed.**