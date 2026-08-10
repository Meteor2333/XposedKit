package cc.meteormc.xposedkit.impl

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.ParcelFileDescriptor
import android.util.Log
import cc.meteormc.xposedkit.XLog
import cc.meteormc.xposedkit.XposedInterface
import cc.meteormc.xposedkit.XposedKit
import cc.meteormc.xposedkit.hook.HookHandle
import cc.meteormc.xposedkit.hook.HookType
import cc.meteormc.xposedkit.hook.InvokeCallback
import cc.meteormc.xposedkit.hook.InvokeInfo
import cc.meteormc.xposedkit.param.PackageLoadedParam
import cc.meteormc.xposedkit.param.ProcessLoadedParam
import cc.meteormc.xposedkit.param.SystemServerStartingParam
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.FileNotFoundException
import java.lang.reflect.Member
import java.util.concurrent.CopyOnWriteArrayList

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
    override val moduleAppInfo: ApplicationInfo
        get() = XposedKit.modulePackageInfo.applicationInfo

    private data class HookedMember(
        val member: Member,
        val unhook: XC_MethodHook.Unhook,
        val handles: MutableList<HookHandle>
    )

    private val hookedMembers = HashMap<Member, HookedMember>()
    private val xcallback = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            runCallback(HookType.BEFORE, param)
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            runCallback(HookType.AFTER, param)
        }

        private fun runCallback(type: HookType, param: MethodHookParam) {
            val member = param.method
            val target = hookedMembers[member] ?: return
            val info = InvokeInfo(
                member,
                param.thisObject,
                param.args,
                param.result,
                param.throwable
            )
            runCatching {
                for (handle in target.handles) {
                    if (handle.type != type) continue
                    handle.callback(info)
                }
            }.onFailure {
                param.throwable = it
            }
            if (info.hasChanged) {
                param.result = info.result
            }
        }
    }

    override fun hook(
        member: Member,
        type: HookType,
        priority: Int,
        callback: InvokeCallback
    ): HookHandle {
        val target = hookedMembers.getOrPut(member) {
            HookedMember(
                member,
                XposedBridge.hookMethod(member, xcallback),
                CopyOnWriteArrayList()
            )
        }

        val handle = HookHandle(
            member,
            type,
            priority,
            callback
        ) {
            val handles = target.handles
            handles.removeIf { it.callback == callback }
             if (handles.isEmpty()) {
                 hookedMembers.remove(member)
                 // 由于Xposed内部的unhook也只是移除列表中的回调
                 // 也并没有真正的取消hook 所以这个操作暂时看上去没什么意义
                 // target.unhook.unhook()
             }
        }

        val handles = target.handles
        val insertIndex = handles.indexOfFirst { it.priority < priority }.takeIf { it >= 0 } ?: handles.size
        handles.add(insertIndex, handle)
        return handle
    }

    override fun getRemotePrefs(name: String): SharedPreferences {
        return XSharedPreferences(
            XposedKit.modulePackageName,
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
        val level = when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> ""
        }
        val values = mapOf(
            "level" to level,
            "level_short" to (level.firstOrNull() ?: "").toString(),
            "module_package" to XposedKit.modulePackageName,
            "tag" to tag,
            "message" to msg
        )
        var formated = "%(\\w+)%".toRegex().replace(XLog.pattern) {
            values[it.groupValues[1]] ?: it.value
        }
        if (tr != null) {
            formated += "\n${Log.getStackTraceString(tr)}"
        }

        XposedBridge.log(formated)
    }

    override fun initZygote(param: IXposedHookZygoteInit.StartupParam) {
        moduleSource = param.modulePath
    }

    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        if (param.packageName == XposedKit.modulePackageName) {
            return
        }

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