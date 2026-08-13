package cc.meteormc.xposedkit.bridge.engine

import android.os.Bundle
import cc.meteormc.xposedkit.bridge.FrameworkProperty
import cc.meteormc.xposedkit.bridge.HookedProcess
import cc.meteormc.xposedkit.bridge.HotReloadCallback
import cc.meteormc.xposedkit.bridge.HotReloadResult
import cc.meteormc.xposedkit.bridge.RequestScopeCallback
import cc.meteormc.xposedkit.bridge.RequestScopeResult
import cc.meteormc.xposedkit.bridge.XposedKitBridge
import io.github.libxposed.service.HookedTarget
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import io.github.libxposed.service.HotReloadResult as LSPHotReloadResult
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicReference

internal object LSPosed : BaseEngine(), XposedServiceHelper.OnServiceListener {
    private val service = AtomicReference<XposedService>()

    override val apiVersion: Int
        get() = useService { apiVersion }
    override val frameworkName: String
        get() = useService { frameworkName }
    override val frameworkVersion: String
        get() = useService { frameworkVersion }
    override val frameworkVersionCode: Long
        get() = useService { frameworkVersionCode }
    override val frameworkProperties: EnumMap<FrameworkProperty, Boolean>
        get() {
            val prop = useService { frameworkProperties }
            return EnumMap<FrameworkProperty, Boolean>(FrameworkProperty::class.java).apply {
                put(FrameworkProperty.CAPABILITY_SYSTEM, prop and XposedService.PROP_CAP_SYSTEM != 0L)
                put(FrameworkProperty.CAPABILITY_REMOTE, prop and XposedService.PROP_CAP_REMOTE != 0L)
                put(FrameworkProperty.API_PROTECTION, prop and XposedService.PROP_RT_API_PROTECTION != 0L)
            }
        }
    override val scopes: Collection<String>
        get() = useService { scope }
    override val hookedProcess: Collection<HookedProcess>
        get() = useService {
            processMapping = runningTargets.associateBy { it.toProcess() }
            processMapping.keys
        }

    private var processMapping: Map<HookedProcess, HookedTarget> = emptyMap()

    override fun init() {
        XposedServiceHelper.registerListener(this)
    }

    override fun detect(): Boolean {
        return service.get() != null
    }

    override fun requestScope(packages: Collection<String>, callback: RequestScopeCallback) {
        useService {
            requestScope(packages.toList(), object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) {
                    callback.onResult(RequestScopeResult(true, approved, "Success"))
                }

                override fun onScopeRequestFailed(message: String) {
                    callback.onResult(RequestScopeResult(false, emptyList(), message))
                }
            })

            return
        }
    }

    override fun removeScope(packages: Collection<String>): Boolean {
        useService { removeScope(packages.toList()) }
        return true
    }

    override fun hotReloadModule(process: HookedProcess, extras: Bundle?, callback: HotReloadCallback) {
        val target = processMapping[process] ?: return callback.onResult(
            HotReloadResult(
                process,
                HotReloadResult.Status.FAILED,
                "Process not found"
            )
        )
        useService {
            hotReloadModule(target, extras) { target, result ->
                val status = when (result.status) {
                    LSPHotReloadResult.Status.SUCCEEDED -> HotReloadResult.Status.SUCCEEDED
                    LSPHotReloadResult.Status.FAILED -> HotReloadResult.Status.FAILED
                    LSPHotReloadResult.Status.UNSUPPORTED -> HotReloadResult.Status.UNSUPPORTED
                    LSPHotReloadResult.Status.IN_PROGRESS -> HotReloadResult.Status.IN_PROGRESS
                    LSPHotReloadResult.Status.PROCESS_DIED -> HotReloadResult.Status.PROCESS_DIED
                }
                callback.onResult(
                    HotReloadResult(process, status, result.message)
                )
            }
        }
    }

    override fun onServiceBind(svc: XposedService) {
        XposedKitBridge.setEngine(this)
        service.set(svc)
    }

    override fun onServiceDied(svc: XposedService) {
        XposedKitBridge.setEngine(Default)
        service.set(null)
    }

    private inline fun <T> useService(block: XposedService.() -> T): T {
        return service.get().let(block)
    }

    private fun HookedTarget.toProcess(): HookedProcess {
        return HookedProcess(
            this@LSPosed,
            uid,
            pid,
            processName
        ).apply {
            state = when (this@toProcess.state) {
                HookedTarget.State.UP_TO_DATE -> HookedProcess.State.LATEST
                HookedTarget.State.STALE -> HookedProcess.State.OUTDATED
                HookedTarget.State.RELOADING -> HookedProcess.State.RELOADING
                HookedTarget.State.FAILED -> HookedProcess.State.DIRTY
            }
            moduleVersionCode = loadedVersionCode
        }
    }
}