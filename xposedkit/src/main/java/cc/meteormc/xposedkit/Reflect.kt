@file:Suppress("UNCHECKED_CAST")

package cc.meteormc.xposedkit

import cc.meteormc.xposedkit.util.Primitives
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

val <T : Any> KClass<T>.reflect: Reflect<T>
    get() = this.java.reflect

fun <T : Any, R> KClass<T>.reflect(block: Reflect<T>.() -> R): R {
    return reflect.run(block)
}

val <T : Any> Class<T>.reflect: Reflect<T>
    get() = Reflect(this)

fun <T : Any, R> Class<T>.reflect(block: Reflect<T>.() -> R): R {
    return reflect.run(block)
}

fun ClassLoader.reflect(className: String): Reflect<*>? {
    fun tryLoad(name: String): Class<*> {
        val clazz = Primitives.getAbbreviation(name)?.let {
            loadClass("[$it").componentType
        }

        if (clazz != null) return clazz
        var formatName = name.filterNot { it.isWhitespace() }
        if (formatName.endsWith("[]")) {
            val prefix = buildString {
                while (formatName.endsWith("[]")) {
                    formatName = formatName.dropLast(2)
                    append('[')
                }
            }

            formatName = prefix + (Primitives.getAbbreviation(formatName) ?: "L$formatName;")
        }

        return loadClass(formatName)
    }

    return runCatching {
        tryLoad(className)
    }.recoverCatching { ex ->
        if (ex !is ClassNotFoundException) throw ex

        val lastDot = className.lastIndexOf('.')
        if (lastDot == -1) throw ex

        val innerName = className.replaceRange(
            lastDot,
            lastDot + 1,
            "$"
        )

        tryLoad(innerName)
    }.map {
        Reflect(it)
    }.getOrNull()
}

fun <R> ClassLoader.reflect(className: String, block: Reflect<*>.() -> R): R? {
    return reflect(className)?.run(block)
}

fun <T : Any> ClassLoader.typedReflect(className: String): Reflect<T>? {
    return reflect(className) as? Reflect<T>
}

fun <T : Any, R> ClassLoader.typedReflect(className: String, block: Reflect<T>.() -> R): R? {
    return typedReflect<T>(className)?.run(block)
}

class Reflect<T : Any>(val delegate: Class<T>) {
    companion object {
        private val singletonCache = mutableMapOf<String, Any>()
        private val constructorCache = mutableMapOf<String, Constructor<*>?>()
        private val fieldCache = mutableMapOf<String, Field?>()
        private val methodCache = mutableMapOf<String, Method?>()
    }

    val singleton: T?
        get() = runCatching {
            delegate.getDeclaredField("INSTANCE")
        }.getOrNull()?.takeIf {
            Modifier.isStatic(it.modifiers)
        }?.get<T>(null)

    fun constructor(vararg paramTypes: Class<*>): Constructor<T>? {
        val fullName = "${delegate.name}(${getParametersString(*paramTypes)})"
        if (constructorCache.containsKey(fullName)) {
            return constructorCache[fullName] as? Constructor<T>
        }

        val constructor = runCatching {
            delegate.getDeclaredConstructor(*paramTypes).setAccessible()
        }.getOrNull()
        constructorCache[fullName] = constructor
        return constructor
    }

    val constructors
        get() = delegate.constructors.setAccessible()

    val declaredConstructors
        get() = delegate.declaredConstructors.setAccessible()

    fun method(name: String, vararg paramTypes: Class<*>): Method? {
        val fullName = "${delegate.name}#$name(${getParametersString(*paramTypes)})"
        if (methodCache.containsKey(fullName)) {
            return methodCache[fullName]
        }

        val method = firstRecursive {
            runCatching {
                it.getDeclaredMethod(name, *paramTypes).setAccessible()
            }.getOrNull()
        }
        methodCache[fullName] = method
        return method
    }

    fun method(name: String): Method? {
        method(name, *emptyArray<Class<*>>())?.let {
            return it
        }

        val fullName = "${delegate.name}#$name"
        if (methodCache.containsKey(fullName)) {
            return methodCache[fullName]
        }

        val method = methods(name).singleOrNull()
        methodCache[fullName] = method
        return method
    }

    fun methods(name: String): List<Method> {
        return findRecursive {
            it.declaredMethods.filter { method ->
                name.contentEquals(method.name)
            }
        }.flatten().setAccessible()
    }

    val methods
        get() = delegate.methods.setAccessible()

    val declaredMethods
        get() = delegate.declaredMethods.setAccessible()

    fun field(name: String): Field? {
        val fullName = "${delegate.name}#$name"
        if (fieldCache.containsKey(fullName)) {
            return fieldCache[fullName]
        }

        val field = firstRecursive {
            runCatching {
                it.getDeclaredField(name).setAccessible()
            }.getOrNull()
        }
        fieldCache[fullName] = field
        return field
    }

    fun fields(type: Class<*>): List<Field> {
        return findRecursive {
            it.declaredFields.filter { field ->
                type.isAssignableFrom(field.type)
            }
        }.flatten().setAccessible()
    }

    val fields
        get() = delegate.fields.setAccessible()

    val declaredFields
        get() = delegate.declaredFields.setAccessible()

    private fun getParametersString(vararg clazzes: Class<*>): String {
        return clazzes.joinToString(",") { it.name }
    }

    private fun <R> firstRecursive(func: (clazz: Class<*>) -> R?): R? {
        var superClass: Class<*> = delegate
        do {
            func(superClass)?.let { return it }
        } while ((superClass.getSuperclass()?.also { superClass = it }) != null)
        return null
    }

    private fun <R> findRecursive(func: (clazz: Class<*>) -> R?): List<R> {
        val result = mutableListOf<R>()
        var superClass: Class<*> = delegate
        do {
            func(superClass)?.let { result.add(it) }
        } while ((superClass.getSuperclass()?.also { superClass = it }) != null)
        return result
    }
}

fun <T> Constructor<T>.new(vararg args: Any?): T {
    return this.setAccessible().newInstance(*args)
}

fun <T> Method.call(obj: Any?, vararg args: Any?): T {
    return this.setAccessible().invoke(obj, *args) as T
}

operator fun <T> Field.get(obj: Any?): T {
    return this.setAccessible()[obj] as T
}

private fun <T : AccessibleObject> T.setAccessible(): T {
    isAccessible = true
    return this
}

private fun <T : AccessibleObject> Array<T>.setAccessible(): List<T> {
    return this.map { it.setAccessible() }
}

private fun <T : AccessibleObject> Iterable<T>.setAccessible(): List<T> {
    return this.map { it.setAccessible() }
}