package cc.meteormc.xposedkit.bridge

import android.os.Bundle
import cc.meteormc.xposedkit.bridge.engine.BaseEngine

@ConsistentCopyVisibility
data class HookedProcess internal constructor(
    private val engine: BaseEngine,
    val uid: Int,
    val pid: Int,
    val processName: String
) {
    var state: State = State.LATEST
        internal set
    var moduleVersionCode: Long = -1L
        internal set

    enum class State {
        LATEST,
        OUTDATED,
        RELOADING,
        DIRTY
    }

    fun hotReload(extras: Bundle? = null, callback: HotReloadCallback) {
        engine.hotReloadModule(this, extras, callback)
    }
}

fun interface HotReloadCallback {
    fun onResult(result: HotReloadResult)
}

class HotReloadResult(
    val process: HookedProcess,
    val status: Status,
    val message: String?
) {
    enum class Status {
        SUCCEEDED,
        FAILED,
        UNSUPPORTED,
        IN_PROGRESS,
        PROCESS_DIED
    }
}