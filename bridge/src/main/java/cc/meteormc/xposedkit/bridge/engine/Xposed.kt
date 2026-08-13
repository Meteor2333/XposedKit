package cc.meteormc.xposedkit.bridge.engine

import android.os.Bundle
import cc.meteormc.xposedkit.bridge.HookedProcess
import cc.meteormc.xposedkit.bridge.HotReloadCallback
import cc.meteormc.xposedkit.bridge.HotReloadResult
import cc.meteormc.xposedkit.bridge.RequestScopeCallback
import cc.meteormc.xposedkit.bridge.RequestScopeResult

internal object Xposed : BaseEngine() {
    override fun init() {
        TODO("Not yet implemented")
    }

    override fun detect(): Boolean {
        TODO("Not yet implemented")
    }

    override fun requestScope(packages: Collection<String>, callback: RequestScopeCallback) {
        callback.onResult(
            RequestScopeResult(
                false,
                emptyList(),
                "Xposed engine does not support scope requests"
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
                "Xposed engine does not support hot reload"
            )
        )
    }
}