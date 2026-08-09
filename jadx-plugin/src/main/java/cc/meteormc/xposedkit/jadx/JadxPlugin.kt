package cc.meteormc.xposedkit.jadx

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.api.plugins.JadxPluginInfoBuilder
import org.slf4j.LoggerFactory

class JadxPlugin : JadxPlugin {
    companion object {
        private val LOG = LoggerFactory.getLogger(JadxPlugin::class.java)

        const val PLUGIN_ID = "xposedkit-extension"
    }

    override fun getPluginInfo(): JadxPluginInfo = JadxPluginInfoBuilder
        .pluginId(PLUGIN_ID)
        .name(I18n.str("plugin.name"))
        .description(I18n.str("plugin.description"))
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
    }
}