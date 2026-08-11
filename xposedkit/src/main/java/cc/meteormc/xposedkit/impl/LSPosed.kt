package cc.meteormc.xposedkit.impl

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import cc.meteormc.xposedkit.XLog
import cc.meteormc.xposedkit.XposedInterface
import cc.meteormc.xposedkit.XposedKit
import cc.meteormc.xposedkit.hook.HookHandle
import cc.meteormc.xposedkit.hook.HookType
import cc.meteormc.xposedkit.hook.InvokeCallback
import cc.meteormc.xposedkit.hook.InvokeInfo
import cc.meteormc.xposedkit.impl.LSPosed.HookIdentifier.Companion.toId
import cc.meteormc.xposedkit.param.HotReloadingParam
import cc.meteormc.xposedkit.param.PackageLoadedParam
import cc.meteormc.xposedkit.param.ProcessLoadedParam
import cc.meteormc.xposedkit.param.SystemServerStartingParam
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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

    private var isHotReloading = AtomicBoolean(false)
    private var previousHookHandles = ConcurrentHashMap<Member, MutableList<LSPInterface.HookHandle>>()
    private val appPackages = mutableMapOf<String, RuntimePackage>()

    private data class RuntimePackage(
        val classLoader: WeakReference<ClassLoader>,
        val appInfo: ApplicationInfo,
        val isFirstPackage: Boolean,
        var context: WeakReference<Application> = WeakReference(null)
    )

    private fun registerRuntimePackage(
        packageName: String,
        classLoader: ClassLoader,
        appInfo: ApplicationInfo,
        isFirstPackage: Boolean
    ) {
        appPackages[packageName] = RuntimePackage(
            WeakReference(classLoader),
            appInfo,
            isFirstPackage
        )
        XposedKit.registerAppAttachListener(packageName) {
            appPackages[packageName]?.context = WeakReference(it)
        }
    }

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

        val hooker = LSPInterface.Hooker {
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

        val identifier = HookIdentifier(
            type,
            if (priority == InvokeCallback.PRIORITY_NORMAL) PRIORITY_DEFAULT else priority
        )
        fun LSPInterface.HookHandle.buildHandle(): HookHandle {
            return HookHandle(
                member,
                type,
                priority,
                callback
            ) {
                this.unhook()
            }
        }

        if (isHotReloading.get()) {
            // 当热重载时 如果存在与之前相同参数的HookHandle
            // 则可以认为它们是同一个钩子（哪怕实际上可能不是同一个）
            // 所以可以直接替换
            val previousHandle = previousHookHandles[member]?.let {
                it.indexOfFirst { handle ->
                    val id = handle.id ?: return@indexOfFirst false
                    HookIdentifier.fromId(id) == identifier
                }.takeIf { idx ->
                    idx in it.indices
                }?.let { idx ->
                    it.removeAt(idx)
                }
            }

            // 如果找到了之前的HookHandle 则替换为新的Hooker
            // 如果没找到 说明可能是新的hook 那么就走下面的逻辑创建钩子
            if (previousHandle != null) {
                return previousHandle.replaceHook(hooker).buildHandle()
            }
        }

        var builder = hook(member)
            .setExceptionMode(LSPInterface.ExceptionMode.PASSTHROUGH)
            .setPriority(identifier.priority)

        if (apiVersion >= 102) {
            builder = builder.setId(identifier.toId())
        }

        return builder.intercept(hooker).buildHandle()
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
        registerRuntimePackage(
            param.packageName,
            param.classLoader,
            param.applicationInfo,
            param.isFirstPackage
        )
    }

    override fun onSystemServerStarting(param: LSPLifecycle.SystemServerStartingParam) {
        val systemParam = SystemServerStartingParam(param.classLoader)
        XposedKit.mount { onSystemServerStarting(systemParam) }
    }

    override fun onHotReloading(param: LSPLifecycle.HotReloadingParam): Boolean {
        val reloadParam = HotReloadingParam(
            param.extras,
            null
        )
        if (!XposedKit.mount { onHotReloading(reloadParam) }) {
            return false
        }

        // !!IMPORTANT
        // 下面序列化相关的代码必须严格使用非模块ClassLoader加载的类型
        // 注意克制使用kt语法 避免编译器生成kt类
        // 否则会抛异常 且无法在另一个ClassLoader中直接反序列化

        val packages = HashMap<String, Map<String, Any?>>()
        for (entry in appPackages) {
            val pkg = entry.value
            val classLoader = pkg.classLoader.get() ?: continue
            packages[entry.key] = HashMap<String, Any?>().apply {
                put("classLoader", classLoader)
                put("appInfo", pkg.appInfo)
                put("isFirstPackage", pkg.isFirstPackage)
                put("context", pkg.context.get())
            }
        }

        param.setSavedInstanceState(
            HashMap<String, Any?>().apply {
                put("extras", param.extras)
                put("savedData", reloadParam.savedData)
                put("packages", packages)
            }
        )

        // 在此处完成清理操作
        appPackages.clear()
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun onHotReloaded(param: LSPLifecycle.HotReloadedParam) {
        val state = param.savedInstanceState as Map<String, Any?>
        val savedData = state["savedData"]
        val packages = state["packages"] as? Map<String, Map<String, Any?>>?
        if (packages == null) {
            XLog.w(XposedKit.TAG, "No packages found in savedInstanceState, skipping onHotReloaded")
            return
        }

        isHotReloading.set(true)
        previousHookHandles.clear()
        previousHookHandles.putAll(
            param.oldHookHandles
                .groupBy { it.executable }
                .mapValues { it.value.toMutableList() }
        )

        XposedKit.init(this, true)
        XposedKit.mount {
            val param = ProcessLoadedParam(
                param.processName,
                param.isSystemServer,
                ProcessLoadedParam.HotReloadInfo(
                    param.extras,
                    savedData
                )
            )
            onProcessLoaded(param)
        }

        packages.forEach {
            val packageName = it.key
            val wrapper = it.value
            val classLoader = wrapper["classLoader"] as ClassLoader
            val appInfo = wrapper["appInfo"] as ApplicationInfo
            val isFirstPackage = wrapper["isFirstPackage"] as Boolean
            val context = wrapper["context"] as Application?
            if (context != null) {
                XposedKit.attachedApplications[packageName] = context
            }

            registerRuntimePackage(
                packageName,
                classLoader,
                appInfo,
                isFirstPackage
            )

            XposedKit.mount {
                val param = PackageLoadedParam(
                    packageName,
                    classLoader,
                    appInfo,
                    null,
                    isFirstPackage
                )
                onPackageLoaded(param)
            }
        }

        isHotReloading.set(false)
        previousHookHandles.values.flatten().forEach {
            // 清除剩余的没有被替换的旧Hook
            it.unhook()
        }
        previousHookHandles.clear()
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

    private data class HookIdentifier(
        val type: HookType,
        val priority: Int,
        val random: String = generateRandomString()
    ) {
        companion object {
            private val sr = SecureRandom()

            fun fromId(id: String): HookIdentifier? {
                val split = id.split(':')
                val type = runCatching { split.getOrNull(0)?.let { HookType.valueOf(it) } }.getOrNull() ?: return null
                val priority = split.getOrNull(1)?.toIntOrNull() ?: return null
                val random = split.getOrNull(2).orEmpty().ifBlank { generateRandomString() }
                return HookIdentifier(type, priority, random)
            }

            fun HookIdentifier.toId(): String {
                return "${type.name}:$priority:$random"
            }

            private fun generateRandomString(): String {
                val bytes = ByteArray(16)
                sr.nextBytes(bytes)
                return bytes.joinToString("") { "%02x".format(it) }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HookIdentifier) return false

            if (type != other.type) return false
            if (priority != other.priority) return false

            return true
        }

        override fun hashCode(): Int {
            var result = 1
            result = 31 * result + type.ordinal
            result = 31 * result + priority
            return result
        }
    }
}