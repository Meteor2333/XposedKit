package cc.meteormc.xposedkit.impl

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.ParcelFileDescriptor
import cc.meteormc.xposedkit.XposedInterface
import cc.meteormc.xposedkit.XposedKit
import cc.meteormc.xposedkit.hook.HookHandle
import cc.meteormc.xposedkit.hook.HookType
import cc.meteormc.xposedkit.hook.InvokeCallback
import cc.meteormc.xposedkit.param.HotReloadParam
import cc.meteormc.xposedkit.param.PackageLoadedParam
import cc.meteormc.xposedkit.param.ProcessLoadedParam
import cc.meteormc.xposedkit.param.SystemServerStartingParam
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Member
import io.github.libxposed.api.XposedModule as LSPModule

class LSPosed : XposedInterface, LSPModule() {
    override val apiVer: Int
        get() = apiVersion
    override val frameworkLabel: String
        get() = frameworkName
    override val frameworkVer: String
        get() = frameworkVersion
    override val frameworkVerCode: Long
        get() = frameworkVersionCode
    override val moduleSource: String
        get() = moduleAppInfo.sourceDir
    override val moduleAppInfo: ApplicationInfo
        get() = moduleApplicationInfo

    override fun getRemotePrefs(name: String): SharedPreferences {
        return getRemotePreferences(name)
    }

    override fun getRemoteFile(name: String): ParcelFileDescriptor {
        return openRemoteFile(name)
    }

    override fun getRemoteFiles(): List<String> {
        return listRemoteFiles().toList()
    }

    override fun printLog(
        priority: Int,
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        if (tr != null) {
            log(priority, tag, msg, tr)
        } else {
            log(priority, tag, msg)
        }
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        XposedKit.init(this)
        if (param.isSystemServer) return
        val processParam = ProcessLoadedParam(param.processName)
        XposedKit.mount { onProcessLoaded(processParam) }
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