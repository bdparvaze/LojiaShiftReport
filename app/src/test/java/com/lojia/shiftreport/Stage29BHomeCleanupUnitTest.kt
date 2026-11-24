package com.lojia.shiftreport

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage29BHomeCleanupUnitTest {

    private fun appFile(relativePath: String): File {
        val direct = File(relativePath)
        return if (direct.exists()) direct else File("app", relativePath)
    }

    private fun parseStringValues(relativePath: String): Map<String, String> {
        val file = appFile(relativePath)
        assertTrue("Expected file to exist: $relativePath", file.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val map = LinkedHashMap<String, String>(nodes.length)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            map[name] = node.textContent.orEmpty()
        }
        return map
    }

    @Test
    fun removedQuickActionsKeysTokensAndComposablesHaveZeroReferences() {
        val removedIdentifiers = listOf(
            "HomeQuickActionsCard",
            "HomeQuickActionItem",
            "home_quick_actions_card",
            "home_quick_action",
            "home_quick_actions",
            "home_action_new_report",
            "home_action_add_due",
            "HomeQuickActionTileSize",
            "HomeQuickActionTileRadius",
            "HomeQuickActionLabelSize",
            "onOpenAddDueClick"
        )

        val scanRoots = listOf(
            appFile("src/main/java"),
            appFile("src/main/res")
        )
        val violations = mutableListOf<String>()
        scanRoots.forEach { root ->
            root.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
                .forEach { file ->
                    val rel = file.relativeTo(appFile("src")).path.replace('\\', '/')
                    val text = file.readText()
                    removedIdentifiers.forEach { token ->
                        if (text.contains(token)) {
                            violations.add("$rel contains $token")
                        }
                    }
                }
        }

        assertTrue(
            "Found leftover references to removed Quick Actions artifacts:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun viewDetailsLinkAndAnalyticsNavigationAreWiredForOwnerChart() {
        val homeSrc = appFile("src/main/java/com/lojia/shiftreport/report/HomeDashboardScreen.kt").readText()
        assertTrue(homeSrc.contains("if (summary.showOwnerOnlyParts)"))
        assertTrue(homeSrc.contains("R.string.home_view_details"))
        assertTrue(homeSrc.contains("testTag(\"home_chart_view_details\")"))
        assertTrue(homeSrc.contains("Dimens.MinTouchTarget"))
        assertTrue(homeSrc.contains("PrimaryBlueLight"))
        assertTrue(homeSrc.contains("onViewDetailsClick = onOpenAnalyticsClick"))

        val mainActivitySrc = appFile("src/main/java/com/lojia/shiftreport/MainActivity.kt").readText()
        assertTrue(
            "MainActivity must navigate to existing Analytics destination on Home View details tap",
            mainActivitySrc.contains("onNavigateToAnalytics = {") &&
                mainActivitySrc.contains("navigateTo(AppNavState.ShiftReportState.Analytics)")
        )
    }

    @Test
    fun stringResourceCountsAndKeysMatchAcrossEnBnAr() {
        val enMap = parseStringValues("src/main/res/values/strings.xml")
        val bnMap = parseStringValues("src/main/res/values-bn/strings.xml")
        val arMap = parseStringValues("src/main/res/values-ar/strings.xml")

        assertEquals("values vs values-bn key count mismatch", enMap.size, bnMap.size)
        assertEquals("values vs values-ar key count mismatch", enMap.size, arMap.size)
        assertEquals("values vs values-bn key set mismatch", enMap.keys, bnMap.keys)
        assertEquals("values vs values-ar key set mismatch", enMap.keys, arMap.keys)

        assertEquals("View details", enMap["home_view_details"])
        assertEquals("বিস্তারিত দেখুন", bnMap["home_view_details"])
        assertEquals("عرض التفاصيل", arMap["home_view_details"])

        assertFalse(enMap.containsKey("home_quick_actions"))
        assertFalse(enMap.containsKey("home_action_new_report"))
        assertFalse(enMap.containsKey("home_action_add_due"))
    }

    @Test
    fun zeroColorLiteralOutsideUiTheme() {
        val mainJavaDir = appFile("src/main/java")
        val violations = mutableListOf<String>()
        mainJavaDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relPath = file.relativeTo(mainJavaDir).path.replace('\\', '/')
                if (relPath.startsWith("com/lojia/shiftreport/ui/theme/")) return@forEach
                file.readLines().forEachIndexed { idx, line ->
                    if (line.contains("Color(0x", ignoreCase = false)) {
                        violations.add("$relPath:${idx + 1}")
                    }
                }
            }

        assertTrue(
            "Found hardcoded Color(0x outside ui/theme:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
