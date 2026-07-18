package cc.meteormc.xposedkit.plugin

import cc.meteormc.xposedkit.plugin.task.GenerateManifestTask
import com.android.build.api.variant.AndroidComponentsExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Locale

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

//        val tasks = target.tasks
//        tasks.register<Copy>("copyProcessorDebugRes") {
//            dependsOn("kspDebugKotlin")
//            from(buildDir.dir("generated/ksp/debug/resources/res"))
//            into(buildDir.dir("generated/processor-res/debug"))
//        }
//
//        tasks.register<Copy>("copyProcessorReleaseRes") {
//            dependsOn("kspReleaseKotlin")
//            from(buildDir.dir("generated/ksp/release/resources/res"))
//            into(buildDir.dir("generated/processor-res/release"))
//        }
//
//        tasks.named("preBuild") {
//            dependsOn("copyProcessorDebugRes")
//            dependsOn("copyProcessorReleaseRes")
//        }

//        val android = target.extensions.findByType(CommonExtension::class.java)
//            ?: throw IllegalStateException("Android extension not found!")
//        android.sourceSets.getByName("main") {
//            fun addResSource(path: String) {
//                res.directories.add(buildDir.dir(path).get().asFile.path)
//            }
//
//            val buildTypes = android.buildTypes
//            buildTypes.named("debug") {
//                addResSource("generated/processor-res/debug")
//            }
//
//            buildTypes.named("release") {
//                addResSource("generated/processor-res/release")
//            }
//        }

        val manifestDir = buildDir.dir("generated/res/manifest").get()
        val metadataFile = manifestDir.file("metadata.properties")
        val ksp = target.extensions.findByType(KspExtension::class.java)
            ?: throw IllegalStateException("KSP extension not found!")
        ksp.arg("metadataOutput", metadataFile.asFile.absolutePath)

        val tasks = target.tasks
        val androidComponents = target.extensions.findByType(AndroidComponentsExtension::class.java)
            ?: throw IllegalStateException("Android components extension not found!")
        androidComponents.onVariants { variant ->
            val variantName = variant.name
            val manifests = variant.sources.manifests
            val manifestFile = manifestDir.file("$variantName/AndroidManifest.xml")
            val capitalizeName = variantName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT)
                else it.toString()
            }
            val task = tasks.register(
                "generate${capitalizeName}XposedMetadataManifest",
                GenerateManifestTask::class.java
            ) {
                dependsOn("ksp${capitalizeName}Kotlin")
                mainManifest.set(
                    project.layout.projectDirectory.file("src/main/AndroidManifest.xml")
                )
                metadataInput.set(metadataFile)
                output.set(manifestFile)
            }

            manifests.addGeneratedManifestFile(
                task,
                GenerateManifestTask::output
            )
        }
    }
}