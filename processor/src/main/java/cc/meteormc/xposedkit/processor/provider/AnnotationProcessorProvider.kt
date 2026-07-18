package cc.meteormc.xposedkit.processor.provider

import cc.meteormc.xposedkit.processor.AnnotationProcessor
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class AnnotationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return AnnotationProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
    }
}