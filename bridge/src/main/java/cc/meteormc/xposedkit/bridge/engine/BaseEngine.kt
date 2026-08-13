package cc.meteormc.xposedkit.bridge.engine

import android.os.Bundle
import cc.meteormc.xposedkit.bridge.FrameworkProperty
import cc.meteormc.xposedkit.bridge.HookedProcess
import cc.meteormc.xposedkit.bridge.HotReloadCallback
import cc.meteormc.xposedkit.bridge.HotReloadResult
import cc.meteormc.xposedkit.bridge.RequestScopeCallback
import cc.meteormc.xposedkit.bridge.RequestScopeResult
import java.util.EnumMap

internal abstract class BaseEngine {
    object Default : BaseEngine() {
        override fun init() {

        }

        override fun detect(): Boolean {
            return false
        }

        override fun requestScope(packages: Collection<String>, callback: RequestScopeCallback) {
            callback.onResult(
                RequestScopeResult(
                    false,
                    emptyList(),
                    "No engine detected"
                )
            )
        }

        override fun removeScope(packages: Collection<String>): Boolean {
            return false
        }

        override fun hotReloadModule(process: HookedProcess, extras: Bundle?, callback: HotReloadCallback) {
            callback.onResult(
                HotReloadResult(
                    process,
                    HotReloadResult.Status.FAILED,
                    "No engine detected"
                )
            )
        }
    }

    open val apiVersion: Int
        get() = -1

    open val frameworkName: String
        get() = "Unknown"

    open val frameworkVersion: String
        get() = "Unknown"

    open val frameworkVersionCode: Long
        get() = -1L

    open val frameworkProperties: EnumMap<FrameworkProperty, Boolean>
        get() = EnumMap(FrameworkProperty::class.java)

    open val scopes: Collection<String>
        get() = emptyList()

    open val hookedProcess: Collection<HookedProcess>
        get() = emptyList()

    abstract fun init()

    abstract fun detect(): Boolean

    abstract fun requestScope(packages: Collection<String>, callback: RequestScopeCallback)

    abstract fun removeScope(packages: Collection<String>): Boolean

    abstract fun hotReloadModule(process: HookedProcess, extras: Bundle?, callback: HotReloadCallback)
}