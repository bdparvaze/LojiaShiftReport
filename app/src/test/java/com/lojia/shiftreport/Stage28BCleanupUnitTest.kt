package com.lojia.shiftreport

import androidx.compose.ui.graphics.Color
import com.lojia.shiftreport.ui.theme.SurfaceGradientEnd
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage28BCleanupUnitTest {

    private fun appFile(relativePath: String): File {
        val direct = File(relativePath)
        return if (direct.exists()) direct else File("app", relativePath)
    }

    private fun parseStringKeys(relativePath: String): List<String> {
        val file = appFile(relativePath)
        assertTrue("Expected file to exist: $relativePath", file.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val keys = ArrayList<String>(nodes.length)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            keys.add(name)
        }
        return keys
    }

    @Test
    fun noProductionSourceContainsColorLiteralOutsideUiTheme() {
        val mainJavaDir = appFile("src/main/java")
        assertTrue("Expected src/main/java directory to exist", mainJavaDir.isDirectory)

        val violations = mutableListOf<String>()
        mainJavaDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relPath = file.relativeTo(mainJavaDir).path.replace('\\', '/')
                if (relPath.startsWith("com/lojia/shiftreport/ui/theme/")) return@forEach
                file.readLines().forEachIndexed { idx, line ->
                    if (line.contains("Color(0x", ignoreCase = false)) {
                        violations.add("$relPath:${idx + 1} -> ${line.trim()}")
                    }
                }
            }

        assertTrue(
            "Found hardcoded Color(0x outside ui/theme:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun surfaceGradientEndTokenExistsWithExactHex() {
        assertEquals(Color(0xFFF5F7FD), SurfaceGradientEnd)

        val colorKt = appFile("src/main/java/com/lojia/shiftreport/ui/theme/Color.kt").readText()
        assertTrue(
            "Color.kt must declare SurfaceGradientEnd = Color(0xFFF5F7FD)",
            colorKt.contains("val SurfaceGradientEnd = Color(0xFFF5F7FD)")
        )
    }

    @Test
    fun deadShouldSeedDemoDataIsRemovedFromProductionCode() {
        val mainJavaDir = appFile("src/main/java")
        val hits = mutableListOf<String>()
        mainJavaDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                if (file.readText().contains("shouldSeedDemoData")) {
                    hits.add(file.relativeTo(mainJavaDir).path.replace('\\', '/'))
                }
            }
        assertTrue("shouldSeedDemoData should have been removed: $hits", hits.isEmpty())
    }

    @Test
    fun stringResourceKeyCountsMatchAcrossEnBnAr() {
        val enKeys = parseStringKeys("src/main/res/values/strings.xml")
        val bnKeys = parseStringKeys("src/main/res/values-bn/strings.xml")
        val arKeys = parseStringKeys("src/main/res/values-ar/strings.xml")

        assertEquals("values vs values-bn key count mismatch", enKeys.size, bnKeys.size)
        assertEquals("values vs values-ar key count mismatch", enKeys.size, arKeys.size)
        assertEquals("values vs values-bn key set mismatch", enKeys.toSet(), bnKeys.toSet())
        assertEquals("values vs values-ar key set mismatch", enKeys.toSet(), arKeys.toSet())
        assertFalse("String resources must not be empty", enKeys.isEmpty())
    }
}
