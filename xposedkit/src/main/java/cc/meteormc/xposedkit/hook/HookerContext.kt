package cc.meteormc.xposedkit.hook

import cc.meteormc.xposedkit.Reflect
import cc.meteormc.xposedkit.XposedKit
import cc.meteormc.xposedkit.reflect
import cc.meteormc.xposedkit.typedReflect
import java.lang.reflect.Member

open class HookerContext(
    open val classLoader: ClassLoader
) {
    val String.clazz: Class<*>?
        get() = reflect?.delegate
    val String.reflect: Reflect<*>?
        get() = classLoader.reflect(this)

    fun <R> String.reflect(block: Reflect<*>.() -> R): R? {
        return classLoader.reflect(this, block)
    }

    fun <T : Any, R> String.typedReflect(block: Reflect<T>.() -> R): R? {
        return classLoader.typedReflect(this, block)
    }

    fun Member.hookBefore(callback: InvokeCallback): Member {
        XposedKit.hookBefore(this, callback)
        return this
    }

    fun Member.hookAfter(callback: InvokeCallback): Member {
        XposedKit.hookAfter(this, callback)
        return this
    }
}