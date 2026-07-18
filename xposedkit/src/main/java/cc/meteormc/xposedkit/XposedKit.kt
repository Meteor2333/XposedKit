package cc.meteormc.xposedkit

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.content.res.XModuleResources
import android.os.ParcelFileDescriptor
import android.util.Log
import cc.meteormc.xposedkit.hook.InvokeCallback
import cc.meteormc.xposedkit.hook.InvokeInfo
import cc.meteormc.xposedkit.provider.RemoteFileProvider
import cc.meteormc.xposedkit.provider.RemotePreferencesProvider
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Member
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

object XposedKit {
    const val TAG = "XposedKit"

    internal var impl: XposedInterface? = null
    private val attachedApplications = WeakHashMap<String, Application>()
    private val appAttachListeners = ConcurrentHashMap<String, MutableSet<(Application) -> Unit>>()

    internal fun init(impl: XposedInterface) {
        this.impl = impl
    }

    internal fun prepare() {
        hookAfter(Application::class.reflect.method("attach")!!) {
            val application = it.instance<Application>()
            attachedApplications[application.packageName] = application

            val listeners = appAttachListeners.remove(application.packageName) ?: return@hookAfter
            synchronized(listeners) {
                listeners.toList()
            }.forEach { listener ->
                listener(application)
            }
        }
    }

    internal fun <T> mount(block: XposedModule.() -> T): T {
        return block(moduleInstance)
    }

    val available
        get() = impl != null

    val apiVersion
        get() = impl!!.apiVer

    val frameworkName
        get() = impl!!.frameworkLabel

    val frameworkVersion
        get() = impl!!.frameworkVer

    val frameworkVersionCode
        get() = impl!!.frameworkVerCode

    val moduleSource
        get() = impl!!.moduleSource

    val moduleAppInfo
        get() = impl?.moduleAppInfo

    val modulePackage
        get() = moduleInstance.modulePackage

    val moduleInstance by lazy {
        val classLoader = javaClass.classLoader!!
        // ServiceLoader不能直接适配 kotlin 的单例类, 所以改为自己实现
        // val services = ServiceLoader.load(XposedModule::class.java, classLoader)
        val services = classLoader
            .getResourceAsStream("META-INF/services/${XposedModule::class.java.name}")
            .bufferedReader()
            .readLines()

        if (services.isEmpty()) {
            throw IllegalStateException("No XposedModule implementation found!")
        }

        var result: XposedModule? = null
        for (service in services) {
            if (result != null) {
                Log.w(TAG, "Multiple XposedModule implementations found, ignoring $service")
                continue
            }

            val reflect = classLoader.typedReflect<XposedModule>(service)
            if (reflect == null) {
                Log.w(TAG, "XposedModule implementation $service not found, skipping it")
                continue
            }

            result = reflect.singleton ?: reflect.constructor()?.new()
            if (result == null) {
                Log.w(TAG, "XposedModule implementation $service does not have a no-arg constructor, skipping it")
            }
        }

        result ?: throw IllegalStateException("No valid XposedModule implementation found!")
    }

    val remotePreferences by lazy {
        object : RemotePreferencesProvider {
            override fun get(name: String): SharedPreferences {
                return impl?.getRemotePrefs(name) ?: throw IllegalStateException("XposedInterface is not initialized!")
            }
        }
    }

    val remoteFile by lazy {
        object : RemoteFileProvider {
            override fun get(name: String): ParcelFileDescriptor {
                return impl?.getRemoteFile(name) ?: throw IllegalStateException("XposedInterface is not initialized!")
            }

            override fun files(): List<String> {
                return impl?.getRemoteFiles().orEmpty()
            }
        }
    }

    fun hookBefore(member: Member, callback: InvokeCallback): XC_MethodHook.Unhook {
        return XposedBridge.hookMethod(
            member,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    callback(
                        InvokeInfo(
                            param.method,
                            param.thisObject,
                            param.args,
                            param.result,
                            param.throwable
                        )
                    )
                }
            }
        )
    }

    fun hookAfter(member: Member, callback: InvokeCallback): XC_MethodHook.Unhook {
        return XposedBridge.hookMethod(
            member,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    callback(
                        InvokeInfo(
                            param.method,
                            param.thisObject,
                            param.args,
                            param.result,
                            param.throwable
                        )
                    )
                }
            }
        )
    }

    fun getModuleResources(context: Context): Resources {
        return XModuleResources.createInstance(moduleSource, null)
    }

    fun registerAppAttachListener(packageName: String, listener: (Application) -> Unit) {
        val attached = attachedApplications[packageName]
        if (attached != null) {
            listener(attached)
            return
        }

        val listeners = appAttachListeners.getOrPut(packageName) { mutableSetOf() }
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun unregisterAppAttachListeners(packageName: String) {
        appAttachListeners.remove(packageName)
    }
}