package cc.meteormc.xposedkit.bridge

import cc.meteormc.xposedkit.bridge.engine.BaseEngine
import java.util.Collections

@ConsistentCopyVisibility
data class ScopeList internal constructor(
    private val engine: BaseEngine,
    // 作用域列表**快照**
    private val scopes: MutableList<String>
) : Iterable<String> {
    val size: Int
        get() = scopes.size

    override fun iterator(): Iterator<String> {
        return toList().iterator()
    }

    operator fun plusAssign(element: String) {
        add(element)
    }

    fun add(element: String) {
        add(element) { }
    }

    fun add(element: String, callback: RequestScopeCallback) {
        addAll(listOf(element), callback)
    }

    fun addAll(elements: Collection<String>, callback: RequestScopeCallback) {
        engine.requestScope(elements) {
            scopes.addAll(it.approved)
            callback.onResult(it)
        }
    }

    operator fun minusAssign(element: String) {
        remove(element)
    }

    fun remove(element: String): Boolean {
        return removeAll(listOf(element))
    }

    fun removeAll(elements: Collection<String>): Boolean {
        scopes.removeAll(elements)
        return engine.removeScope(elements)
    }

    fun retainAll(elements: Collection<String>): Boolean {
        scopes.retainAll(elements)
        return engine.removeScope(scopes.filter { it !in elements })
    }

    fun clear(): Boolean {
        scopes.clear()
        return engine.removeScope(scopes)
    }

    fun isEmpty(): Boolean {
        return scopes.isEmpty()
    }

    fun contains(element: String): Boolean {
        return scopes.contains(element)
    }

    fun containsAll(elements: Collection<String>): Boolean {
        return scopes.containsAll(elements)
    }

    fun toList(): List<String> {
        return Collections.unmodifiableList(scopes)
    }
}

fun interface RequestScopeCallback {
    fun onResult(result: RequestScopeResult)
}

class RequestScopeResult(
    val success: Boolean,
    val approved: List<String>,
    val message: String
)