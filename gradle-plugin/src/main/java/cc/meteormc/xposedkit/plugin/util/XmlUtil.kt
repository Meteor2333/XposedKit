package cc.meteormc.xposedkit.plugin.util

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLInputFactory

object XmlUtil {
    private val documentFactory by lazy {
        DocumentBuilderFactory.newInstance()!!.apply {
            isNamespaceAware = true
        }
    }
    private val streamXMLFactory = XMLInputFactory.newFactory().apply {
        setProperty(XMLInputFactory.SUPPORT_DTD, false)
        setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    }

    fun newDocument() = documentFactory.newDocumentBuilder().newDocument()!!

    fun parseDocument(file: File) = documentFactory.newDocumentBuilder().parse(file)!!

    fun createXMLReader(file: File) = streamXMLFactory.createXMLStreamReader(file.bufferedReader())!!
}