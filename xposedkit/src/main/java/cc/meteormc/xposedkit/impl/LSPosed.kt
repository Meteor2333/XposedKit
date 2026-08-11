package cc.meteormc.xposedkit.impl

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import cc.meteormc.xposedkit.XposedInterface
import cc.meteormc.xposedkit.XposedKit
import cc.meteormc.xposedkit.hook.HookHandle
import cc.meteormc.xposedkit.hook.HookType
import cc.meteormc.xposedkit.hook.InvokeCallback
import cc.meteormc.xposedkit.hook.InvokeInfo
import cc.meteormc.xposedkit.param.HotReloadParam
import cc.meteormc.xposedkit.param.PackageLoadedParam
import cc.meteormc.xposedkit.param.ProcessLoadedParam
import cc.meteormc.xposedkit.param.SystemServerStartingParam
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Method
import io.github.libxposed.api.XposedInterface as LSPInterface
import io.github.libxposed.api.XposedModule as LSPModule
import io.github.libxposed.api.XposedModuleInterface as LSPLifecycle

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun hook(
        member: Member,
        type: HookType,
        priority: Int,
        callback: InvokeCallback
    ): HookHandle {
        if (member !is Executable) {
            throw IllegalArgumentException("Member must be an Executable in LSPosed framework")
        }

        val handle = hook(member)
            .setExceptionMode(LSPInterface.ExceptionMode.PASSTHROUGH)
            .setPriority(if (priority == InvokeCallback.PRIORITY_NORMAL) PRIORITY_DEFAULT else priority)
            .intercept {
                val returnValue: Any?
                val member: Member = it.executable
                when (type) {
                    HookType.BEFORE -> {
                        val args = it.args.toTypedArray()
                        val info = InvokeInfo(
                            member,
                            it.thisObject,
                            args,
                            null,
                            null
                        )
                        callback(info)
                        returnValue = if (info.thrown != null) {
                            throw info.thrown
                        } else if (info.hasChanged) {
                            info.result
                        } else {
                            it.proceed(args)
                        }
                    }
                    HookType.AFTER -> {
                        val result = it.runCatching { proceed() }
                        val info = InvokeInfo(
                            member,
                            it.thisObject,
                            it.args.toTypedArray(),
                            result.getOrNull(),
                            result.exceptionOrNull()
                        )
                        callback(info)
                        if (info.thrown != null) {
                            throw info.thrown
                        }

                        returnValue = info.result
                    }
                }

                returnValue
            }

        return HookHandle(
            member,
            type,
            priority,
            callback
        ) {
            handle.unhook()
        }
    }

    override fun invokeOriginal(member: Member, obj: Any?, vararg args: Any?): Any? {
        return member.toInvoker()
            .setType(LSPInterface.Invoker.Type.ORIGIN)
            .invoke(obj, *args)
    }

    override fun invokeSpecial(member: Member, obj: Any, vararg args: Any?): Any? {
        return member.toInvoker().invokeSpecial(obj, *args)
    }

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

    override fun onModuleLoaded(param: LSPLifecycle.ModuleLoadedParam) {
        XposedKit.init(this)
        XposedKit.prepare()
        val processParam = ProcessLoadedParam(param.processName, param.isSystemServer)
        XposedKit.mount { onProcessLoaded(processParam) }
    }

    override fun onPackageReady(param: LSPLifecycle.PackageReadyParam) {
        val packageParam = PackageLoadedParam(
            param.packageName,
            param.classLoader,
            param.applicationInfo,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) param.appComponentFactory else null,
            param.isFirstPackage
        )
        XposedKit.mount { onPackageLoaded(packageParam) }
    }

    override fun onSystemServerStarting(param: LSPLifecycle.SystemServerStartingParam) {
        val systemParam = SystemServerStartingParam(param.classLoader)
        XposedKit.mount { onSystemServerStarting(systemParam) }
    }

    override fun onHotReloading(param: LSPLifecycle.HotReloadingParam): Boolean {
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

    override fun onHotReloaded(param: LSPLifecycle.HotReloadedParam) {
        val reloadParam = HotReloadParam(
            param.extras,
            param.savedInstanceState
        )
        XposedKit.mount { onHotReloadNew(reloadParam) }
    }

    private fun Member.toInvoker(): LSPInterface.Invoker<out LSPInterface.Invoker<*, out Executable>, out Executable> {
        return when (this) {
            is Method -> getInvoker(this)
            is Constructor<*> -> getInvoker(this)
            else -> {
                throw IllegalArgumentException("Member must be an Executable in LSPosed framework")
            }
        }
    }
}