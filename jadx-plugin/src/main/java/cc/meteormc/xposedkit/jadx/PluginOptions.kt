package cc.meteormc.xposedkit.jadx

import cc.meteormc.xposedkit.jadx.JadxPlugin.Companion.PLUGIN_ID
import jadx.api.plugins.options.OptionType
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder
import jadx.api.plugins.options.impl.OptionBuilder
import java.util.function.Consumer
import java.util.function.Function
import javax.swing.KeyStroke

object PluginOptions : BasePluginOptionsBuilder() {
    var copyReflectShortcut: String? = null
        private set

    override fun registerOptions() {
        shortcutOption("shortcut.copy-reflect")
            .description(I18n.str("shortcut.copy-reflect"))
            .defaultValue("K")
            .setter { copyReflectShortcut = it }
    }

    override fun <T> option(name: String): OptionBuilder<T> {
        return super.option("$PLUGIN_ID.$name")
    }

    override fun <T> option(name: String, optionType: Class<T>): OptionBuilder<T> {
        return super.option("$PLUGIN_ID.$name", optionType)
    }

    override fun boolOption(name: String): OptionBuilder<Boolean> {
        return super.boolOption("$PLUGIN_ID.$name")
    }

    override fun strOption(name: String): OptionBuilder<String> {
        return super.strOption("$PLUGIN_ID.$name")
    }

    override fun intOption(name: String): OptionBuilder<Int> {
        return super.intOption("$PLUGIN_ID.$name")
    }

    override fun <E : Enum<*>> enumOption(name: String, values: Array<E>, valueOf: Function<String, E>): OptionBuilder<E> {
        return super.enumOption("$PLUGIN_ID.$name", values, valueOf)
    }

    fun shortcutOption(name: String): OptionBuilder<String?> {
        return addOption(
            object : OptionData<String?>("$PLUGIN_ID.$name") {
                private val ALIASES = mapOf(
                    "control" to "ctrl",
                    "ctl" to "ctrl",
                    "command" to "meta",
                    "cmd" to "meta",
                    "win" to "meta",
                    "windows" to "meta",
                    "option" to "alt",

                    "esc" to "ESCAPE",
                    "return" to "ENTER",
                    "del" to "DELETE",
                    "backspace" to "BACK_SPACE",
                    "pgup" to "PAGE_UP",
                    "pageup" to "PAGE_UP",
                    "pgdn" to "PAGE_DOWN",
                    "pagedown" to "PAGE_DOWN"
                )

                init {
                    type(OptionType.STRING)
                    parser { if (it == "null") null else it }
                    formatter { it.toString() }
                }

                override fun setter(setter: Consumer<String?>): OptionBuilder<String?> {
                    // 转换为 KeyStroke.getKeyStroke 支持的格式
                    return super.setter {
                        val formated: String?
                        if (it.isNullOrBlank()) {
                            formated = null
                        } else if (KeyStroke.getKeyStroke(it) != null) {
                            formated = it
                        } else {
                            val parts = it.lowercase()
                                .replace("+", " ")
                                .replace("-", " ")
                                .trim()
                                .split(Regex("\\s+"))
                                .map { part -> ALIASES[part] ?: part }
                            val key = parts.last().uppercase()
                            val modifiers = parts.dropLast(1).joinToString(" ") { t ->
                                t.lowercase()
                            }

                            val result = if (modifiers.isEmpty()) key
                            else "$modifiers $key"
                            formated = result.takeIf { s ->
                                KeyStroke.getKeyStroke(s) != null
                            } ?: defaultValue()
                        }

                        setter.accept(formated)
                    }
                }
            }
        )
    }

    private fun <T> addOption(option: OptionBuilder<T>): OptionBuilder<T> {
        @Suppress("UNCHECKED_CAST")
        return BasePluginOptionsBuilder::class.java.getDeclaredMethod(
            "addOption",
            OptionBuilder::class.java
        ).apply {
            isAccessible = true
        }.invoke(
            this,
            option
        ) as OptionBuilder<T>
    }
}