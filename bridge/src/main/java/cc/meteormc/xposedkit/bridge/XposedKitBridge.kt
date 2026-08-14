package cc.meteormc.xposedkit.bridge

import cc.meteormc.xposedkit.bridge.engine.BaseEngine
import cc.meteormc.xposedkit.bridge.engine.LSPosed
import cc.meteormc.xposedkit.bridge.engine.Xposed

object XposedKitBridge {
    private val ENGINE_LIST = listOf(
        Xposed,
        LSPosed
    )

    private var engine: BaseEngine = BaseEngine.Default
        get() {
            if (!available) {
                ENGINE_LIST.firstOrNull { it.detect() }?.let { field = it }
            }

            return field
        }

    val available
        get() = engine !is BaseEngine.Default

    val apiVersion
        get() = engine.apiVersion

    val frameworkName
        get() = engine.frameworkName

    val frameworkVersion
        get() = engine.frameworkVersion

    val frameworkVersionCode
        get() = engine.frameworkVersionCode

    val frameworkProperties
        get() = engine.frameworkProperties

    val scopes
        get() = ScopeList(engine, engine.scopes.toMutableList())

    val hookedProcesses
        get() = engine.hookedProcesses

    fun init() {
        ENGINE_LIST.forEach { it.init() }
    }

    internal fun setEngine(engine: BaseEngine) {
        if (!engine.detect()) return
        if (!ENGINE_LIST.contains(engine)) return
        this.engine = engine
    }
}