@file:Suppress("UNCHECKED_CAST")

package cc.meteormc.xposedkit

import cc.meteormc.xposedkit.util.Primitives
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.WeakHashMap
import kotlin.reflect.KClass

private val cache = WeakHashMap<Class<*>, Reflect<*>>()

val <T : Any> Class<T>.reflect: Reflect<T>
    get() = cache.getOrPut(this) { Reflect(this) } as Reflect<T>

fun <T : Any, R> Class<T>.reflect(block: Reflect<T>.() -> R): R {
    return reflect.run(block)
}

val <T : Any> KClass<T>.reflect: Reflect<T>
    get() = this.java.reflect

fun <T : Any, R> KClass<T>.reflect(block: Reflect<T>.() -> R): R {
    return this.java.reflect(block)
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
        it.reflect
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
    private val constructorCache = mutableMapOf<String, Constructor<*>?>()
    private val methodCache = mutableMapOf<String, Method?>()
    private val fieldCache = mutableMapOf<String, Field?>()

    val singleton by lazy {
        runCatching {
            delegate.getDeclaredField("INSTANCE")
        }.getOrNull()?.takeIf {
            Modifier.isStatic(it.modifiers)
        }?.get<T>(null)
    }

    fun constructor(vararg paramTypes: Class<*>) = constructorCache.getOrPut(getParametersString(*paramTypes)) {
        runCatching {
            delegate.getDeclaredConstructor(*paramTypes).setAccessible()
        }.getOrNull()
    } as? Constructor<T>

    val constructors
        get() = delegate.constructors.setAccessible()

    val declaredConstructors
        get() = delegate.declaredConstructors.setAccessible()

    fun method(name: String, vararg paramTypes: Class<*>) = methodCache.getOrPut(name + getParametersString(*paramTypes)) {
        firstRecursive {
            runCatching {
                it.getDeclaredMethod(name, *paramTypes).setAccessible()
            }.getOrNull()
        }
    }

    fun method(name: String) = methodCache.getOrPut(name) {
        method(name, *emptyArray<Class<*>>())?.let {
            return@getOrPut it
        }

        // 先按继承层级取最优先匹配的类
        // 再在该类的重载中取唯一的方法
        // 并忽略父类声明的同名方法
        methods(name)
            .groupBy { it.declaringClass }
            .entries
            .firstOrNull()
            ?.value
            ?.singleOrNull()
    }

    fun methods(name: String): List<Method> {
        val exists = mutableSetOf<String>()
        return findRecursive {
            it.declaredMethods.filter { method ->
                // 仅保留继承链中最先匹配到的类的方法（优先子类）
                // 忽略父类被覆盖的方法
                name.contentEquals(method.name) && exists.add(method.signature())
            }
        }.flatten().setAccessible()
    }

    val methods
        get() = delegate.methods.setAccessible()

    val declaredMethods
        get() = delegate.declaredMethods.setAccessible()

    fun field(name: String) = fieldCache.getOrPut(name) {
        firstRecursive {
            runCatching {
                it.getDeclaredField(name).setAccessible()
            }.getOrNull()
        }
    }

    fun fields(type: Class<*>) = findRecursive {
        it.declaredFields.filter { field ->
            type.isAssignableFrom(field.type)
        }
    }.flatten().setAccessible()

    val fields
        get() = delegate.fields.setAccessible()

    val declaredFields
        get() = delegate.declaredFields.setAccessible()

    private fun getParametersString(vararg clazzes: Class<*>): String {
        return "(${clazzes.joinToString(",") { it.name }})"
    }

    private inline fun <R> firstRecursive(func: (clazz: Class<*>) -> R?): R? {
        var superClass: Class<*> = delegate
        do {
            func(superClass)?.let { return it }
        } while ((superClass.getSuperclass()?.also { superClass = it }) != null)
        return null
    }

    private inline fun <R> findRecursive(func: (clazz: Class<*>) -> R?): List<R> {
        val result = mutableListOf<R>()
        var superClass: Class<*> = delegate
        do {
            func(superClass)?.let { result.add(it) }
        } while ((superClass.getSuperclass()?.also { superClass = it }) != null)
        return result
    }
}

fun Class<*>.descriptor(): String {
    return when {
        isPrimitive -> Primitives.getAbbreviation(name)!!
        isArray -> "[" + componentType!!.descriptor()
        else -> "L${name.replace('.', '/')};"
    }
}

fun Member.descriptor(): String {
    var parameterTypes: Array<Class<*>>
    var returnType: Class<*>
    when (this) {
        is Constructor<*> -> {
            parameterTypes = this.parameterTypes
            returnType = Void::class.javaPrimitiveType!!
        }
        is Method -> {
            parameterTypes = this.parameterTypes
            returnType = this.returnType
        }
        else -> {
            throw IllegalArgumentException("Unsupported member type: ${this::class.java.name}")
        }
    }
    return "(${parameterTypes.joinToString("") { it.descriptor() }})${returnType.descriptor()}"
}

fun Member.signature(): String {
    var name: String
    var parameterTypes: Array<Class<*>>
    when (this) {
        is Constructor<*> -> {
            name = "<init>"
            parameterTypes = this.parameterTypes
        }
        is Method -> {
            name = this.name
            parameterTypes = this.parameterTypes
        }
        else -> {
            throw IllegalArgumentException("Unsupported member type: ${this::class.java.name}")
        }
    }
    return "$name(${parameterTypes.joinToString(",") { it.name }})"
}

fun <T> Constructor<T>.new(vararg args: Any?): T {
    return this.setAccessible().newInstance(*args)
}

fun <T> Method.call(obj: Any?, vararg args: Any?): T {
    return this.setAccessible().invoke(obj, *args) as T
}

fun <T> Field.get(obj: Any?): T {
    return this.setAccessible()[obj] as T
}

fun <T : AccessibleObject> T.setAccessible(): T {
    isAccessible = true
    return this
}

fun <T : AccessibleObject> Array<T>.setAccessible(): List<T> {
    return this.map { it.setAccessible() }
}

fun <T : AccessibleObject> Iterable<T>.setAccessible(): List<T> {
    return this.map { it.setAccessible() }
}