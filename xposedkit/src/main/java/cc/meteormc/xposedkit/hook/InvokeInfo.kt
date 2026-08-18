package cc.meteormc.xposedkit.hook

import cc.meteormc.xposedkit.call
import cc.meteormc.xposedkit.callOriginal
import cc.meteormc.xposedkit.callSpecial
import cc.meteormc.xposedkit.reflect
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method

@Suppress("UNCHECKED_CAST")
class InvokeInfo(
    val member: Member,
    val instance: Any?,
    val args: Array<Any?>,
    result: Any?,
    val exception: Throwable?
) {
    internal var hasChanged = false
    var result = result
        set(value) {
            field = value
            hasChanged = true
        }

    fun <T> instance() = this.instance as T

    fun booleanArg(index: Int = 0) = argByGenerics<Boolean>(index)
    fun byteArg(index: Int = 0) = argByGenerics<Byte>(index)
    fun charArg(index: Int = 0) = argByGenerics<Char>(index)
    fun doubleArg(index: Int = 0) = argByGenerics<Double>(index)
    fun floatArg(index: Int = 0) = argByGenerics<Float>(index)
    fun intArg(index: Int = 0) = argByGenerics<Int>(index)
    fun longArg(index: Int = 0) = argByGenerics<Long>(index)
    fun shortArg(index: Int = 0) = argByGenerics<Short>(index)
    fun stringArg(index: Int = 0) = argByGenerics<String>(index)
    fun <T> argByClass(type: Class<T>, index: Int = 0) = this.args.filterIsInstance(type)[index]
    inline fun <reified T> argByGenerics(index: Int = 0) = this.args.filterIsInstance<T>()[index]

    fun booleanArg(value: Boolean, index: Int = 0) = argByGenerics<Boolean>(value, index)
    fun byteArg(value: Byte, index: Int = 0) = argByGenerics<Byte>(value, index)
    fun charArg(value: Char, index: Int = 0) = argByGenerics<Char>(value, index)
    fun doubleArg(value: Double, index: Int = 0) = argByGenerics<Double>(value, index)
    fun floatArg(value: Float, index: Int = 0) = argByGenerics<Float>(value, index)
    fun intArg(value: Int, index: Int = 0) = argByGenerics<Int>(value, index)
    fun longArg(value: Long, index: Int = 0) = argByGenerics<Long>(value, index)
    fun shortArg(value: Short, index: Int = 0) = argByGenerics<Short>(value, index)
    fun stringArg(value: String, index: Int = 0) = argByGenerics<String>(value, index)
    fun <T> argByClass(value: Any, type: Class<T>, index: Int = 0) = this.args.withIndex()
        .filter { type.isInstance(it.value) }[index]
        .index
        .let { args[it] = value }
    inline fun <reified T> argByGenerics(value: Any, index: Int = 0) = this.args.withIndex()
        .filter { it.value is T }[index]
        .index
        .let { args[it] = value }

    val booleanResult
        get() = this.result as Boolean
    val byteResult
        get() = this.result as Byte
    val charResult
        get() = this.result as Char
    val doubleResult
        get() = this.result as Double
    val floatResult
        get() = this.result as Float
    val intResult
        get() = this.result as Int
    val longResult
        get() = this.result as Long
    val shortResult
        get() = this.result as Short
    val stringResult
        get() = this.result as String
    fun <T> result() = this.result as T

    fun <T : Throwable> exception() = this.exception as T

    fun doNothing() {
        result = null
    }

    fun <T> callSuper(): T {
        return callSuper(*args)
    }

    fun <T> callSuper(vararg args: Any?): T {
        val superclass = member.declaringClass.superclass?.reflect
            ?: throw IllegalArgumentException("The declaring class of the member has no superclass")
        return when (member) {
            is Constructor<*> -> {
                val ctor = superclass.constructor(*member.parameterTypes)
                    ?: throw IllegalArgumentException("No matching constructor found in superclass")
                ctor.call(instance(), *args)
            }
            is Method -> {
                val method = superclass.method(member.name, *member.parameterTypes)
                    ?: throw IllegalArgumentException("No matching method found in superclass")
                method.callSpecial(instance(), *args)
            }
            else -> {
                throw IllegalArgumentException("Unsupported member type: ${member::class.java.name}")
            }
        }
    }

    fun <T> callOriginal(): T {
        return callOriginal(*args)
    }

    fun <T> callOriginal(vararg args: Any?): T {
        return when (member) {
            is Constructor<*> -> {
                member.callOriginal(instance, *args) as T
            }
            is Method -> {
                member.callOriginal(instance, *args)
            }
            else -> {
                throw IllegalArgumentException("Unsupported member type: ${member::class.java.name}")
            }
        }
    }

    override fun equals(other: Any?) = other is InvokeInfo && this.member == other.member

    override fun hashCode() = this.member.hashCode()

    override fun toString(): String {
        return "InvokeInfo(member=$member, instance=$instance, args=${args.contentToString()}, result=$result, exception=$exception, hasChanged=$hasChanged)"
    }
}