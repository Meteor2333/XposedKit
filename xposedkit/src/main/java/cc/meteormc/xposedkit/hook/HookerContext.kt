package cc.meteormc.xposedkit.hook

import cc.meteormc.xposedkit.Reflect
import cc.meteormc.xposedkit.XposedKit
import cc.meteormc.xposedkit.reflect
import cc.meteormc.xposedkit.typedReflect
import java.lang.reflect.Member

open class HookerContext(
    open val classLoader: ClassLoader,
    protected val handles: MutableMap<Member, MutableList<HookHandle>> = mutableMapOf()
) {
    val String.clazz: Class<*>?
        get() = reflect?.type
    val String.reflect: Reflect<*>?
        get() = classLoader.reflect(this)

    fun <R> String.reflect(block: Reflect<*>.() -> R): R? {
        return classLoader.reflect(this, block)
    }

    fun <T : Any, R> String.typedReflect(block: Reflect<T>.() -> R): R? {
        return classLoader.typedReflect(this, block)
    }

    fun Member.hook(type: HookType, priority: Int = InvokeCallback.PRIORITY_NORMAL, callback: InvokeCallback): Member {
        val handle = XposedKit.impl.hook(this, type, priority, callback)
        handles.getOrPut(this) { mutableListOf() } += handle
        return this
    }

    fun Member.hookBefore(priority: Int = InvokeCallback.PRIORITY_NORMAL, callback: InvokeCallback): Member {
        return hook(HookType.BEFORE, priority, callback)
    }

    fun Member.hookAfter(priority: Int = InvokeCallback.PRIORITY_NORMAL, callback: InvokeCallback): Member {
        return hook(HookType.AFTER, priority, callback)
    }

    fun <T : Iterable<Member>> T.hook(type: HookType, callback: InvokeCallback): T {
        forEach { it.hook(type, InvokeCallback.PRIORITY_NORMAL, callback) }
        return this
    }

    fun <T : Iterable<Member>> T.hookBefore(callback: InvokeCallback): T {
        return hook(HookType.BEFORE, callback)
    }

    fun <T : Iterable<Member>> T.hookAfter(callback: InvokeCallback): T {
        return hook(HookType.AFTER, callback)
    }
}