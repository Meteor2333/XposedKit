package cc.meteormc.xposedkit.plugin

import cc.meteormc.xposedkit.plugin.task.GenerateManifestTask
import cc.meteormc.xposedkit.plugin.task.GenerateResourcesTask
import com.android.build.api.variant.AndroidComponentsExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class XposedKitPlugin : Plugin<Project> {
    companion object {
        private const val KSP_PLUGIN_ID = "com.google.devtools.ksp"
    }

    override fun apply(target: Project) {
        val buildDir = target.layout.buildDirectory

        val plugins = target.pluginManager
        plugins.apply(KSP_PLUGIN_ID)

        val dependencies = target.dependencies
        dependencies.add("implementation", "cc.meteormc:xposedkit:1.0.0")
        dependencies.add("ksp", "cc.meteormc:xposedkit-processor:1.0.0")

        val tasks = target.tasks
        val generatedDir = buildDir.dir("generated/xposedkit").get()
        val metadataFile = generatedDir.file("metadata.properties")

        val ksp = target.extensions.findByType(KspExtension::class.java)
            ?: throw IllegalStateException("KSP extension not found!")
        ksp.arg(
            "metadataOutput",
            metadataFile.asFile.absolutePath
        )

        val androidComponents = target.extensions.findByType(AndroidComponentsExtension::class.java)
            ?: throw IllegalStateException("Android components extension not found!")
        androidComponents.onVariants { variant ->
            val sources = variant.sources
            val variantName = variant.name
            val capitalizeName = variantName.replaceFirstChar(Char::titlecaseChar)
            val variantDir = generatedDir.dir(variantName)

            sources.manifests.addGeneratedManifestFile(
                tasks.register(
                    "generateXposedKit${capitalizeName}Manifest",
                    GenerateManifestTask::class.java
                ) {
                    dependsOn("ksp${capitalizeName}Kotlin")
                    mainManifest.set(project.layout.projectDirectory.file("src/main/AndroidManifest.xml"))
                    metadataInput.set(metadataFile)
                    output.set(variantDir.file("AndroidManifest.xml"))
                },
                GenerateManifestTask::output
            )

            sources.resources!!.addGeneratedSourceDirectory(
                tasks.register(
                    "generateXposedKit${capitalizeName}Resources",
                    GenerateResourcesTask::class.java
                ) {
                    dependsOn("ksp${capitalizeName}Kotlin")
                    metadataInput.set(metadataFile)
                    output.set(variantDir.dir("resources"))
                },
                GenerateResourcesTask::output
            )
        }
    }
}