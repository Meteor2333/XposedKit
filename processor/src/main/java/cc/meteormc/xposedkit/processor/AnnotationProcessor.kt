package cc.meteormc.xposedkit.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.io.File
import java.util.Properties

class AnnotationProcessor(
    private val generator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    companion object {
        private const val SCOPES_RESOURCE_NAME = "module_scopes"

        private const val METADATA_OUTPUT_OPTION = "metadataOutput"

        private const val XPOSED_MODULE_CLASS_NAME = "cc.meteormc.xposedkit.XposedModule"
        private const val MODULE_REGISTER_CLASS_NAME = "cc.meteormc.xposedkit.ModuleRegister"
    }

    private val moduleClasses = mutableListOf<KSClassDeclaration>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        moduleClasses.addAll(
            resolver.getSymbolsWithAnnotation(MODULE_REGISTER_CLASS_NAME).filterIsInstance<KSClassDeclaration>()
        )

        return emptyList()
    }

    override fun finish() {
        processModuleAnnotation()
    }

    private fun processModuleAnnotation() {
        logger.info("processModuleAnnotation: $moduleClasses")

        if (moduleClasses.isEmpty()) {
            throw NoSuchElementException("No @ModuleRegister annotation found! Please annotate your module class with @ModuleRegister")
        }

        if (moduleClasses.size > 1) {
            throw IllegalArgumentException("Multiple @ModuleRegister annotations found! Please ensure that only one class is annotated with @ModuleRegister")
        }

        val moduleClass = moduleClasses.single()
        val dependencies = Dependencies(true, moduleClass.containingFile!!)

        generator.createNewFileByPath(
            dependencies,
            "META-INF/services/$XPOSED_MODULE_CLASS_NAME",
            ""
        ).writer().use {
            it.write(moduleClass.qualifiedName!!.asString())
        }

        val metadataOutput = options[METADATA_OUTPUT_OPTION]?.let { File(it) } ?: return
        metadataOutput.parentFile.mkdirs()
        Properties().apply {
            putAll(
                moduleClass.annotations.first {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == MODULE_REGISTER_CLASS_NAME
                }.arguments.associate {
                    it.name!!.asString() to it.value.toString()
                }
            )
        }.store(
            metadataOutput.bufferedWriter(),
            null
        )
    }
}