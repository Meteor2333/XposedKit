package cc.meteormc.xposedkit.impl

import android.app.AndroidAppHelper
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.util.Log
import cc.meteormc.xposedkit.XposedInterface
import cc.meteormc.xposedkit.XposedKit
import cc.meteormc.xposedkit.param.PackageLoadedParam
import cc.meteormc.xposedkit.param.ProcessLoadedParam
import cc.meteormc.xposedkit.param.SystemServerStartingParam
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.FileNotFoundException

class Xposed : XposedInterface, IXposedHookZygoteInit, IXposedHookLoadPackage {
    init {
        XposedKit.init(this)
    }

    override val apiVer: Int
        get() = XposedBridge.getXposedVersion()
    override val frameworkLabel: String
        get() = runCatching {
            XposedBridge::class.java.getDeclaredField("TAG").get(null) as String
        }.getOrDefault("Xposed").filter {
            it == ' ' || it.isLetterOrDigit()
        }
    override val frameworkVer: String
        get() = "Unknown"
    override val frameworkVerCode: Long
        get() = -1L
    override var moduleSource: String = ""
        get() = field.ifBlank { throw IllegalStateException("Module source is not set!") }
    override val moduleAppInfo
        get() = AndroidAppHelper.currentApplication()?.packageManager?.getPackageArchiveInfo(
            moduleSource,
            PackageManager.GET_META_DATA
        )?.applicationInfo

    override fun getRemotePrefs(name: String): SharedPreferences {
        return XSharedPreferences(
            XposedKit.modulePackage,
            name
        ).apply {
            if (all.isNotEmpty()) return@apply
            makeWorldReadable()
            reload()
        }
    }

    override fun getRemoteFile(name: String): ParcelFileDescriptor {
        throw FileNotFoundException("RemotePreferences is not implemented in Xposed API!")
    }

    override fun getRemoteFiles(): List<String> {
        return emptyList()
    }

    override fun printLog(
        priority: Int,
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        var formatedLog = formatLog(priority, tag, msg)
        if (tr != null) {
            formatedLog += "\n${Log.getStackTraceString(tr)}"
        }

        XposedBridge.log(formatedLog)
    }

    override fun initZygote(param: IXposedHookZygoteInit.StartupParam) {
        moduleSource = param.modulePath
    }

    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        if (param.processName == "android") {
            val systemParam = SystemServerStartingParam(param.classLoader)
            XposedKit.mount { onSystemServerStarting(systemParam) }
            return
        }

        if (param.isFirstApplication) {
            XposedKit.prepare()
            val processParam = ProcessLoadedParam(param.processName)
            XposedKit.mount { onProcessLoaded(processParam) }
        }

        val packageParam = PackageLoadedParam(
            param.packageName,
            param.classLoader,
            param.appInfo,
            null,
            param.isFirstApplication
        )
        XposedKit.mount { onPackageLoaded(packageParam) }
    }
}