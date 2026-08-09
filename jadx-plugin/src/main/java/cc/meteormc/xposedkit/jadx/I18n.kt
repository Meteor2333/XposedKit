package cc.meteormc.xposedkit.jadx

import jadx.core.utils.exceptions.JadxRuntimeException
import jadx.gui.utils.NLS
import java.util.Locale
import java.util.MissingResourceException
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

object I18n {
    private val SUPPORTED_LOCALES = listOf(
        Locale.ENGLISH,
        Locale.SIMPLIFIED_CHINESE,
        Locale.TRADITIONAL_CHINESE
    )

    private val languageMap = mutableMapOf<Locale, ResourceBundle>()

    init {
        for (locale in SUPPORTED_LOCALES) {
            val resName = "i18n/${locale.toLanguageTag().replace('-', '_')}.lang"
            val bundle = javaClass.classLoader.getResourceAsStream(resName)?.reader()?.use {
                try {
                    PropertyResourceBundle(it)
                } catch (e: Exception) {
                    throw JadxRuntimeException("Failed to load: $resName", e)
                }
            } ?: throw JadxRuntimeException("Locale resource not found: $resName")
            languageMap[locale] = bundle
        }
    }

    fun str(key: String): String {
        val locale = NLS.currentLocale().get() ?: Locale.getDefault()
        val language = languageMap[locale] ?: languageMap.values.first()
        return try {
            language.getString(key)
        } catch (e: MissingResourceException) {
            "%$key%"
        }
    }

    fun str(key: String, vararg args: Any?): String {
        return String.format(str(key), *args)
    }
}