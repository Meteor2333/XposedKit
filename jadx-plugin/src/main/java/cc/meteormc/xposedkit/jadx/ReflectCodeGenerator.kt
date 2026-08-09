package cc.meteormc.xposedkit.jadx

import jadx.api.metadata.ICodeNodeRef
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.FieldNode
import jadx.core.dex.nodes.MethodNode
import org.slf4j.LoggerFactory

object ReflectCodeGenerator {
    private val LOG = LoggerFactory.getLogger(ReflectCodeGenerator::class.java)

    fun isSupportedNode(node: ICodeNodeRef): Boolean {
        return node is ClassNode || node is MethodNode || node is FieldNode
    }

    fun generate(node: ICodeNodeRef): String {
        return when (node) {
            is ClassNode -> generateClass(node)
            is MethodNode -> generateMethod(node)
            is FieldNode -> generateField(node)
            else -> {
                LOG.warn("Unsupported node type: ${node.javaClass.simpleName}, this shouldn't happen")
                "Unsupported node!"
            }
        }
    }

    private fun generateClass(node: ClassNode): String {
        return node.rawName().createLambda("")
    }

    private fun generateMethod(node: MethodNode): String {
        val name = node.name
        val params = node.argTypes
        val clazz = node.declaringClass

        fun buildParams() = params.joinToString(",\n", "\n", "\n") {
            if (it.isPrimitive) {
                "${it.primitiveType.longName.capitalize()}::class.javaPrimitiveType!!"
            } else if (it.isArray) {
                var type = it.arrayElement
                if (type.isPrimitive) {
                    // 基本类型数组 直接用kt提供的数组类
                    "${type.primitiveType.longName.capitalize()}Array::class.java"
                } else {
                    var count = 1
                    while (type.isArray) {
                        count++
                        type = type.arrayElement
                    }

                    val typeName = if (type.isPrimitive) {
                        type.primitiveType.shortName
                    } else if (type.isGeneric) {
                        it.`object`
                    } else if (type.isGenericType) {
                        "java.lang.Object"
                    } else {
                        type.toString()
                    }

                    // 对象数组或多维数组 需要反射获取
                    "${"${"[".repeat(count)}L$typeName;".quote()}.clazz"
                }
            } else if (it.isGeneric) {
                it.`object`.formatReference(true)
            } else if (it.isGenericType) {
                "Any::class.java"
            } else {
                it.toString().formatReference(true)
            }
        }

        if (node.isConstructor) {
            // 构造器直接通过参数匹配
            return clazz.rawName().createLambda(
                "constructor(${buildParams()})"
            )
        }

        val methods = clazz.methods.toMutableList()
        clazz.visitParentClasses {
            methods.addAll(it.methods)
        }
        val innerCode = if (params.isNotEmpty() && methods.any { it != node && it.name == name && it.argTypes != params }) {
            // 如果存在同名的且非重写的重载方法，则额外使用参数类型来区分
            "\n${name.quote()},${buildParams()}"
        } else {
            // 否则直接使用方法名称匹配即可
            name.quote()
        }

        return clazz.rawName().createLambda(
            "method($innerCode)"
        )
    }

    private fun generateField(node: FieldNode): String {
        return node.declaringClass.rawName().createLambda(
            "field(${node.name.quote()})"
        )
    }

    private fun ClassNode.rawName(): String {
        // 获取原始类名
        return classInfo.makeRawFullName()
    }

    private fun String.capitalize(): String {
        return replaceFirstChar { s -> s.titlecase() }
    }

    private fun String.quote(): String {
        val str = this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
        // 直接使用插值前缀来转义$
        return "${if (contains('$')) "$$" else ""}\"$str\""
    }

    private fun String.isBootClass(): Boolean {
        return startsWith("java.") || startsWith("android.")
    }

    private fun String.createLambda(code: String): String {
        return """
            ${formatReference(false)}.reflect {
                $code
            }
        """.trimIndent()
    }

    private fun String.formatReference(toClass: Boolean): String {
        val suffix: String
        // 从BootClassLoader加载的类可以直接引用类型
        return if (isBootClass()) {
            suffix = ".java"
            if (this == "java.lang.Object") {
                "Any"
            } else {
                // 暂时选择去除包名
                split('.').last()
            } + "::class"
        } else {
            suffix = ".clazz!!"
            quote()
        } + if (toClass) {
            suffix
        } else {
            ""
        }
    }
}