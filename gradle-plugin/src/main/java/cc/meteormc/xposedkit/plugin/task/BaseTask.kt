package cc.meteormc.xposedkit.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.util.Properties

abstract class BaseTask : DefaultTask() {
    @get:InputFile
    abstract val metadataInput: RegularFileProperty

    @TaskAction
    abstract fun execute()

    protected fun parseMetadata(): Properties {
        val metadata = Properties()
        metadataInput.get().asFile.bufferedReader().use { stream ->
            metadata.load(stream)
        }
        return metadata
    }
}