package cc.meteormc.xposedkit.impl

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.ParcelFileDescriptor
import cc.meteormc.xposedkit.XposedInterface
import cc.meteormc.xposedkit.XposedKit
import cc.meteormc.xposedkit.param.HotReloadParam
import cc.meteormc.xposedkit.param.PackageLoadedParam
import cc.meteormc.xposedkit.param.ProcessLoadedParam
import cc.meteormc.xposedkit.param.SystemServerStartingParam
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedInterface as LSPInterface
import io.github.libxposed.api.XposedModule as LSPModule

class LSPosed : XposedInterface, LSPModule {
    constructor() : super()

    constructor(base: LSPInterface, param: XposedModuleInterface.ModuleLoadedParam) : super(base, param) {
        onModuleLoaded(param)
    }

    override val apiVer: Int
        get() = runCatching {
            apiVersion
        }.getOrElse {
            // The method is not implemented, which means the API version is 100 or below.
            @Suppress("DEPRECATION") API
        }
    override val frameworkLabel: String
        get() = frameworkName
    override val frameworkVer: String
        get() = frameworkVersion
    override val frameworkVerCode: Long
        get() = frameworkVersionCode
    override val moduleSource: String
        get() = moduleAppInfo.sourceDir
    override val moduleAppInfo: ApplicationInfo
        get() = runCatching {
            moduleApplicationInfo
        }.getOrElse {
            applicationInfo
        }

    override fun getRemotePrefs(name: String): SharedPreferences {
        return runCatching {
            getRemotePreferences(name)
        }.getOrElse {
            getSharedPreferences(name, Context.MODE_PRIVATE)
        }
    }

    override fun getRemoteFile(name: String): ParcelFileDescriptor {
        return runCatching {
            openRemoteFile(name)
        }.getOrElse {
            openFileInput(name).use { input ->
                ParcelFileDescriptor.dup(input.getFD())
            }
        }
    }

    override fun getRemoteFiles(): List<String> {
        return runCatching {
            listRemoteFiles()
        }.getOrElse {
            fileList()
        }.toList()
    }

    override fun printLog(
        priority: Int,
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        runCatching {
            if (tr != null) {
                log(priority, tag, msg, tr)
            } else {
                log(priority, tag, msg)
            }
        }.onFailure {
            if (tr != null) {
                log(formatLog(priority, tag, msg), tr)
            } else {
                log(formatLog(priority, tag, msg))
            }
        }
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        XposedKit.init(this)
        if (param.isSystemServer) return
        val processParam = ProcessLoadedParam(param.processName)
        XposedKit.mount { onProcessLoaded(processParam) }
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        val classLoader = try {
            @Suppress("DEPRECATION")
            param.classLoader
        } catch (e: NoSuchMethodError) {
            // API 101+
            return
        }

        if (param.isFirstPackage) {
            XposedKit.prepare()
        }

        val packageParam = PackageLoadedParam(
            param.packageName,
            classLoader,
            param.applicationInfo,
            null,
            param.isFirstPackage
        )
        XposedKit.mount { onPackageLoaded(packageParam) }
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (param.isFirstPackage) {
            XposedKit.prepare()
        }

        val packageParam = PackageLoadedParam(
            param.packageName,
            param.classLoader,
            param.applicationInfo,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) param.appComponentFactory else null,
            param.isFirstPackage
        )
        XposedKit.mount { onPackageLoaded(packageParam) }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in API101+")
    override fun onSystemServerLoaded(param: XposedModuleInterface.SystemServerLoadedParam) {
        val systemParam = SystemServerStartingParam(param.classLoader)
        XposedKit.mount { onSystemServerStarting(systemParam) }
    }

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        val systemParam = SystemServerStartingParam(param.classLoader)
        XposedKit.mount { onSystemServerStarting(systemParam) }
    }

    override fun onHotReloading(param: XposedModuleInterface.HotReloadingParam): Boolean {
        val reloadParam = HotReloadParam(
            param.extras,
            null
        )
        val approve = XposedKit.mount { onHotReloadOld(reloadParam) }
        if (reloadParam.savedInstanceState != null) {
            param.setSavedInstanceState(reloadParam.savedInstanceState)
        }
        return approve
    }

    override fun onHotReloaded(param: XposedModuleInterface.HotReloadedParam) {
        val reloadParam = HotReloadParam(
            param.extras,
            param.savedInstanceState
        )
        XposedKit.mount { onHotReloadNew(reloadParam) }
    }
}