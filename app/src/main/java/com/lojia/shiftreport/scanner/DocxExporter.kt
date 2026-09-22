package com.lojia.shiftreport.scanner

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxExporter {

    private const val TAG = "DocxExporter"

    suspend fun createDocxFile(
        outputFile: File,
        title: String,
        content: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                // 1. [Content_Types].xml
                addZipEntry(zos, "[Content_Types].xml", getContentTypesXml())

                // 2. _rels/.rels
                addZipEntry(zos, "_rels/.rels", getRelsXml())

                // 3. word/_rels/document.xml.rels
                addZipEntry(zos, "word/_rels/document.xml.rels", getDocumentRelsXml())

                // 4. word/document.xml
                addZipEntry(zos, "word/document.xml", getDocumentXml(title, content))
            }

            Log.d(TAG, "Docx file successfully created at ${outputFile.absolutePath}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create docx file", e)
            return@withContext false
        }
    }

    private fun addZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val writer = OutputStreamWriter(zos, Charsets.UTF_8)
        writer.write(content)
        writer.flush()
        zos.closeEntry()
    }

    private fun getContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()
    }

    private fun getRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
    }

    private fun getDocumentRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>""".trimIndent()
    }

    private fun getDocumentXml(title: String, content: String): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        sb.append("<w:body>")

        // Add Title Header
        sb.append("<w:p><w:pPr><w:jc w:val=\"left\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"36\"/></w:rPr><w:t>")
        sb.append(escapeXml(title))
        sb.append("</w:t></w:r></w:p>")

        // Spacer paragraph
        sb.append("<w:p/>")

        // Split paragraphs by newline
        val lines = content.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("--- Page")) {
                sb.append("<w:p><w:r><w:rPr><w:b/><w:color w:val=\"4A5568\"/></w:rPr><w:t>")
                sb.append(escapeXml(trimmed))
                sb.append("</w:t></w:r></w:p>")
            } else if (trimmed.isNotEmpty()) {
                sb.append("<w:p><w:r><w:t xml:space=\"preserve\">")
                sb.append(escapeXml(trimmed))
                sb.append("</w:t></w:r></w:p>")
            } else {
                sb.append("<w:p/>")
            }
        }

        sb.append("</w:body></w:document>")
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
