package com.lojia.shiftreport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Stage29DFieldWidthUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(File("."), File("app"), File(".."), File("../app"))
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/ui/common/LojiaTextField.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    private fun extractStringKeys(stringsFile: File): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile)
        val nodes = doc.getElementsByTagName("string")
        val keys = linkedSetOf<String>()
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val name = el.getAttribute("name")
            if (name.isNotBlank()) keys.add(name)
        }
        return keys
    }

    @Test
    fun lojiaTextField_decorationBox_hasDirectDecorationBoxRoot_withoutBoxColumnRowWrapper() {
        val appDir = resolveAppModuleDir()
        val src = File(appDir, "src/main/java/com/lojia/shiftreport/ui/common/LojiaTextField.kt").readText()

        val singleFieldBody = src
            .substringAfter("fun LojiaTextField(")
            .substringBefore("fun LojiaMultilineTextField(")

        val decorationBoxStart = singleFieldBody.indexOf("decorationBox = {")
        assertTrue("Expected decorationBox lambda in LojiaTextField", decorationBoxStart >= 0)

        val afterDecorationHeader = singleFieldBody
            .substring(decorationBoxStart)
            .substringAfter("innerTextField ->")
            .trimStart()

        assertTrue(
            "TextFieldDefaults.DecorationBox( must be the direct content of decorationBox so parent constraints are never relaxed",
            afterDecorationHeader.startsWith("TextFieldDefaults.DecorationBox(")
        )

        val preDecorationPrefix = afterDecorationHeader.substringBefore("TextFieldDefaults.DecorationBox(")
        assertFalse(
            "decorationBox must not wrap TextFieldDefaults.DecorationBox in Box/Column/Row",
            Regex("""\b(Box|Column|Row)\s*[\({]""").containsMatchIn(preDecorationPrefix)
        )
        assertFalse(
            "LojiaTextField must not render a zero-size drawWithContent label wrapper",
            singleFieldBody.contains("drawWithContent") || singleFieldBody.contains("SpacingNone")
        )
    }

    @Test
    fun lojiaTextFieldAndMultiline_exposeAndWireContentDescriptionSemantics() {
        val appDir = resolveAppModuleDir()
        val src = File(appDir, "src/main/java/com/lojia/shiftreport/ui/common/LojiaTextField.kt").readText()

        val singleFieldHeader = src
            .substringAfter("fun LojiaTextField(")
            .substringBefore(") {")
        val singleFieldBody = src
            .substringAfter("fun LojiaTextField(")
            .substringBefore("fun LojiaMultilineTextField(")
        val multilineSection = src.substringAfter("fun LojiaMultilineTextField(")
        val multilineHeader = multilineSection.substringBefore(") {")
        val multilineBody = multilineSection.substringAfter(") {")

        assertTrue(
            "LojiaTextField must accept contentDescription: String? = null",
            Regex("""contentDescription\s*:\s*String\?\s*=\s*null""").containsMatchIn(singleFieldHeader)
        )
        assertTrue(
            "LojiaMultilineTextField must accept contentDescription: String? = null",
            Regex("""contentDescription\s*:\s*String\?\s*=\s*null""").containsMatchIn(multilineHeader)
        )
        assertTrue(
            "LojiaMultilineTextField must forward contentDescription to LojiaTextField",
            Regex("""contentDescription\s*=\s*contentDescription""").containsMatchIn(multilineBody)
        )
        assertTrue(
            "LojiaTextField must apply semantics contentDescription on BasicTextField when non-null",
            singleFieldBody.contains("Modifier.semantics") &&
                singleFieldBody.contains("this.contentDescription = talkBackDescription")
        )
    }

    @Test
    fun otherFieldComponents_doNotWrapFullWidthChildrenInUnconstrainedBox() {
        val appDir = resolveAppModuleDir()

        val otpSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/common/OtpInputField.kt").readText()
        val otpDecBox = otpSrc.substringAfter("decorationBox = {").trimStart()
        assertTrue(
            "OtpInputField decorationBox root must be Row with fillMaxWidth",
            otpDecBox.startsWith("Row(") && otpDecBox.contains("fillMaxWidth()")
        )

        val pinSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/SixDigitPinInput.kt").readText()
        assertTrue(
            "SixDigitPinInput must lay out digit boxes inside Row(fillMaxWidth())",
            pinSrc.contains("Row(") && pinSrc.substringAfter("Row(").contains(".fillMaxWidth()")
        )

        val authCompSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/LojiaAuthComponents.kt").readText()
        val inputFieldBody = authCompSrc
            .substringAfter("fun LojiaInputField(")
            .substringBefore("fun LojiaGradientButton(")
        assertTrue(
            "LojiaInputField container Box must retain fillMaxWidth()",
            inputFieldBody.substringAfter("Box(").contains(".fillMaxWidth()")
        )
    }

    @Test
    fun stringResourceKeys_haveFullParityAcrossEnBnAr() {
        val appDir = resolveAppModuleDir()
        val enKeys = extractStringKeys(File(appDir, "src/main/res/values/strings.xml"))
        val bnKeys = extractStringKeys(File(appDir, "src/main/res/values-bn/strings.xml"))
        val arKeys = extractStringKeys(File(appDir, "src/main/res/values-ar/strings.xml"))

        assertTrue("Expected non-empty strings.xml key set", enKeys.isNotEmpty())
        assertEquals("English and Bengali string key sets must match", enKeys, bnKeys)
        assertEquals("English and Arabic string key sets must match", enKeys, arKeys)
    }

    @Test
    fun zeroColorLiteralsOutsideTheme() {
        val appDir = resolveAppModuleDir()
        val offending = File(appDir, "src/main")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.invariantSeparatorsPath.contains("/ui/theme/") }
            .filter { it.readText().contains("Color(0x") }
            .map { it.relativeTo(appDir).path }
            .toList()
        assertEquals(
            "Zero Color(0x literals allowed outside ui/theme/, found: $offending",
            emptyList<String>(),
            offending
        )
    }
}
