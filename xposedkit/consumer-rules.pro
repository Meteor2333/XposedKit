# R8对这种文件的支持不好 很多时候并不能成功替换 所以暂时只能keep这个类
# -adaptresourcefilecontents assets/xposed_init
-keep,allowshrinking,allowoptimization class cc.meteormc.xposedkit.impl.Xposed
-adaptresourcefilecontents META-INF/xposed/java_init.list

-keep,allowoptimization,allowobfuscation class cc.meteormc.xposedkit.** { *; }
-keep,allowoptimization,allowobfuscation class * extends cc.meteormc.xposedkit.XposedModule

-dontwarn android.app.AndroidAppHelper
-dontwarn android.content.res.XModuleResources
-dontwarn android.content.res.XResForwarder
-dontwarn android.content.res.XResources
-dontwarn de.robv.android.xposed.**
-dontwarn io.github.libxposed.**

-dontwarn android.app.ActivityThread
-dontwarn android.app.ActivityThread$ApplicationThread
-dontwarn android.app.ActivityThread$ActivityClientRecord
-dontwarn android.content.pm.PackageParser
-dontwarn android.content.pm.PackageParser$Package
-dontwarn android.content.pm.PackageParser$Activity
-dontwarn android.content.pm.PackageParser$Provider
-dontwarn android.content.pm.PackageParser$Service
-dontwarn android.content.pm.PackageParser$PackageParserException
-dontwarn android.content.res.ApkAssets