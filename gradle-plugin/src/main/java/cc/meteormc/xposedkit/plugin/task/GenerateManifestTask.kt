package cc.meteormc.xposedkit.plugin.task

import cc.meteormc.xposedkit.plugin.util.XmlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class GenerateManifestTask : DefaultTask() {
    companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }

    @get:InputFile
    abstract val mainManifest: RegularFileProperty

    @get:InputFile
    abstract val metadataInput: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun generate() {
        val document = XmlUtil.newDocument()

        val manifest = document.createElement("manifest")
        document.appendChild(manifest)

        val application = document.createElement("application")
        manifest.appendChild(application)

        val description = parseDescription()
        if (!description.isNullOrEmpty()) {
            document.createElement("meta-data").apply {
                setAttributeNS(ANDROID_NS, "name", "xposeddescription")
                setAttributeNS(ANDROID_NS, "value", description)
                application.appendChild(this)
            }
        }

        document.createElement("meta-data").apply {
            setAttributeNS(ANDROID_NS, "name", "xposedminversion")
            setAttributeNS(ANDROID_NS, "value", "93")
            application.appendChild(this)
        }

        document.createElement("meta-data").apply {
            setAttributeNS(ANDROID_NS, "name", "xposedscope")
            setAttributeNS(ANDROID_NS, "resource", "@array/module_scopes")
            application.appendChild(this)
        }

        val outputFile = output.get().asFile
        outputFile.parentFile.mkdirs()
        TransformerFactory.newInstance()
            .newTransformer()
            .transform(
                DOMSource(document),
                StreamResult(outputFile)
            )
    }

    fun parseDescription(): String? {
        return XmlUtil.parseDocument(mainManifest.get().asFile)
            .getElementsByTagName("application")
            .item(0)
            ?.attributes
            ?.getNamedItemNS(ANDROID_NS, "description")
            ?.nodeValue
    }
}