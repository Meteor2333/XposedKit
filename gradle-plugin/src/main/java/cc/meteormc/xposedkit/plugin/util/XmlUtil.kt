package cc.meteormc.xposedkit.plugin.util

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object XmlUtil {
    private val documentFactory by lazy {
        DocumentBuilderFactory.newInstance()!!.apply {
            isNamespaceAware = true
        }
    }

    fun newDocument() = documentFactory.newDocumentBuilder().newDocument()!!

    fun parseDocument(file: File) = documentFactory.newDocumentBuilder().parse(file)!!
}