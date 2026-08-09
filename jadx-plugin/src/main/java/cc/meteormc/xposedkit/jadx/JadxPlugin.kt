package cc.meteormc.xposedkit.jadx

import cc.meteormc.xposedkit.jadx.util.FontUtil
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.api.plugins.JadxPluginInfoBuilder
import jadx.core.utils.GsonUtils
import jadx.gui.settings.JadxSettingsData
import jadx.gui.utils.files.JadxFiles
import jadx.plugins.tools.data.JadxInstalledPlugins
import jadx.plugins.tools.utils.PluginFiles
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate

class JadxPlugin : JadxPlugin {
    companion object {
        private val LOG = LoggerFactory.getLogger(JadxPlugin::class.java)

        const val PLUGIN_ID = "xposedkit-extension"

        private const val ASCII_ART = """
               _  __                           ____ __ _ __ 
              | |/ /____  ____  ________  ____/ / //_/(_) /_
              |   // __ \/ __ \/ ___/ _ \/ __  / ,<  / / __/
             /   |/ /_/ / /_/ (__  )  __/ /_/ / /| |/ / /_  
            /_/|_/ .___/\____/____/\___/\__,_/_/ |_/_/\__/  
                /_/                                         
        """

        private const val ASCII_ART_3D = """
             __   __                                     __  __  __      __      
            /\ \ /\ \                                   /\ \/\ \/\ \  __/\ \__   
            \ `\`\/'/'  _____     ___     ____     __   \_\ \ \ \/'/'/\_\ \ ,_\  
             `\/ > <   /\ '__`\  / __`\  /',__\  /'__`\ /'_` \ \ , < \/\ \ \ \/  
                \/'/\`\\ \ \L\ \/\ \L\ \/\__, `\/\  __//\ \L\ \ \ \\`\\ \ \ \ \_ 
                /\_\\ \_\ \ ,__/\ \____/\/\____/\ \____\ \___,_\ \_\ \_\ \_\ \__\
                \/_/ \/_/\ \ \/  \/___/  \/___/  \/____/\/__,_ /\/_/\/_/\/_/\/__/
                          \ \_\                                                  
                           \/_/                                                  
        """

        private const val ASCII_ART_MONOSPACE = """
            ##   ##  #####     ####       ####   #####   #####     ##    ##  ##  ######
             ## ##   ##   ##  ##     ##  ##          ##         ##    ##   ##  ##    ##      ##
              ###     #####   ##     ##     ###     ####    ##     ##  ####      ##      ##
             ## ##   ##         ##     ##          ##  ##         ##    ##   ##  ##    ##      ##
            ##   ##  ##           ####      ####     #####  #####     ##    ##  ##      ##
        """
    }

    override fun getPluginInfo(): JadxPluginInfo = JadxPluginInfoBuilder
        .pluginId(PLUGIN_ID)
        .name(I18n.str("plugin.name"))
        .description(I18n.str("plugin.description") + "\n\n\n${getAsciiArt()}")
        .homepage("https://github.com/Meteor2333/XposedKit")
        .build()

    override fun init(context: JadxPluginContext) {
        val gui = context.guiContext
        if (gui == null) {
            LOG.error("Only supports Jadx GUI")
            return
        }

        context.registerOptions(PluginOptions)
        gui.addPopupMenuAction(I18n.str("popup.copy-reflect"), {
            ReflectCodeGenerator.isSupportedNode(it)
        }, PluginOptions.copyReflectShortcut) { node ->
            val code = ReflectCodeGenerator.generate(node)
            gui.copyToClipboard(code)
        }

        refreshPluginDetail()
    }

    private fun getAsciiArt(): String {
        try {
            if (!Files.exists(JadxFiles.GUI_CONF)) {
                throw FileNotFoundException("Jadx GUI configuration file not found: ${JadxFiles.GUI_CONF}")
            }

            val settings = Files.newBufferedReader(JadxFiles.GUI_CONF).use {
                GsonUtils.defaultGsonBuilder()
                    .registerTypeAdapter(
                        Path::class.java,
                        object : JsonSerializer<Path>, JsonDeserializer<Path> {
                            override fun serialize(
                                src: Path,
                                typeOfSrc: Type,
                                context: JsonSerializationContext
                            ): JsonElement {
                                return JsonPrimitive(src.toString())
                            }

                            override fun deserialize(
                                json: JsonElement,
                                typeOfT: Type,
                                context: JsonDeserializationContext
                            ): Path {
                                return Paths.get(json.asString)
                            }
                        }
                    )
                    .create()
                    .fromJson(it, JadxSettingsData::class.java)
            }

            var art = ASCII_ART

            // 不知道为什么要做这个 可能是太闲了
            val now = LocalDate.now()
            if (now.monthValue == 4 && now.dayOfMonth == 1) {
                art = ASCII_ART_3D
            }

            val uiFont = FontUtil.loadFont(settings.uiFontStr)
            if (!FontUtil.isMonospace(uiFont)) {
                art = ASCII_ART_MONOSPACE
            }

            return art.trimIndent().prependIndent(" ".repeat(6))
        } catch (t: Throwable) {
            LOG.warn("Failed to load ASCII art", t)
            return ""
        }
    }

    private fun refreshPluginDetail() {
        try {
            val gson = GsonUtils.buildGson()
            val plugins = Files.newBufferedReader(PluginFiles.PLUGINS_JSON).use {
                gson.fromJson(it, JadxInstalledPlugins::class.java)
            }

            val currentPlugin = pluginInfo
            for (plugin in plugins.installed) {
                if (plugin.pluginId != currentPlugin.pluginId) continue
                plugin.name = currentPlugin.name
                plugin.description = currentPlugin.description
            }

            Files.newBufferedWriter(PluginFiles.PLUGINS_JSON).use {
                gson.toJson(plugins, it)
            }
        } catch (t: Throwable) {
            LOG.warn("Failed to refresh plugin detail, but you can ignore it", t)
        }
    }
}