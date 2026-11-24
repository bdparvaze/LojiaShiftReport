package com.lojia.shiftreport

import com.lojia.shiftreport.ui.theme.DepthMotionTokens
import com.lojia.shiftreport.ui.theme.DepthTokens
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.FlatBorder
import com.lojia.shiftreport.ui.theme.FlatDestructivePressed
import com.lojia.shiftreport.ui.theme.FlatInsetBorder
import com.lojia.shiftreport.ui.theme.FlatPrimaryPressed
import com.lojia.shiftreport.ui.theme.PrimaryBlueLight
import com.lojia.shiftreport.ui.theme.PureBlack
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.TextHintColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Stage29AFlatUiUnitTest {

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
    fun lojiaTextField_passesNullLabelToDecorationBox_andUsesSingleHintOnly() {
        val appDir = resolveAppModuleDir()
        val textFieldSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/common/LojiaTextField.kt").readText()

        val singleFieldBody = textFieldSrc
            .substringAfter("fun LojiaTextField(")
            .substringBefore("fun LojiaMultilineTextField(")
        val decorationCall = singleFieldBody.substringAfter("TextFieldDefaults.DecorationBox(")

        assertTrue(
            "LojiaTextField must compute a single effectiveHint = placeholder ?: label",
            singleFieldBody.contains("val effectiveHint: (@Composable () -> Unit)? = placeholder ?: label")
        )
        assertTrue(
            "LojiaTextField must pass label = null into TextFieldDefaults.DecorationBox",
            Regex("""\blabel\s*=\s*null\s*,""").containsMatchIn(decorationCall)
        )
        assertTrue(
            "LojiaTextField must pass placeholder = styledPlaceholder into TextFieldDefaults.DecorationBox",
            Regex("""\bplaceholder\s*=\s*styledPlaceholder\s*,""").containsMatchIn(decorationCall)
        )
        assertFalse(
            "LojiaTextField must never switch to contentPaddingWithLabel()",
            decorationCall.contains("contentPaddingWithLabel")
        )
        assertTrue(
            "LojiaMultilineTextField must delegate centrally to LojiaTextField",
            textFieldSrc.substringAfter("fun LojiaMultilineTextField(").contains("LojiaTextField(")
        )
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

    @Test
    fun depthCardAndDepthRaisedButton_doNotReferenceGradientOrLipTokens() {
        val appDir = resolveAppModuleDir()
        val depthCompSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/theme/DepthComponents.kt").readText()

        val cardFnBody = depthCompSrc
            .substringAfter("fun Modifier.depthCard(")
            .substringBefore("fun Modifier.depthSemanticCard(")
        val buttonFnBody = depthCompSrc
            .substringAfter("fun Modifier.depthRaisedButton(")
            .substringBefore("fun Modifier.depthSecondaryButton(")
        val headerFnBody = depthCompSrc
            .substringAfter("fun LojiaGradientHeader(")
            .substringBefore("fun Modifier.cardRise(")

        val forbiddenTokenPattern = Regex("""gradient|\blip|(?-i:Lip)""", RegexOption.IGNORE_CASE)
        assertFalse(
            "depthCard must not reference gradient or lip tokens",
            forbiddenTokenPattern.containsMatchIn(cardFnBody)
        )
        assertFalse(
            "depthRaisedButton must not reference gradient or lip tokens",
            forbiddenTokenPattern.containsMatchIn(buttonFnBody)
        )
        assertFalse(
            "LojiaGradientHeader must render flat solid color without gradient",
            headerFnBody.contains("Gradient", ignoreCase = true)
        )

        // Verify flat design tokens & motion rules
        assertEquals(FlatBorder, DepthTokens.RaisedCardBorderColor)
        assertEquals(FlatInsetBorder, DepthTokens.InsetFieldBorderColor)
        assertEquals(PureBlack, DepthTokens.FlatCardShadowColor)
        assertEquals(0.06f, DepthTokens.FLAT_CARD_SHADOW_ALPHA, 1e-6f)
        assertEquals(PrimaryBlueLight, DepthTokens.FlatPrimaryButtonFill)
        assertEquals(FlatPrimaryPressed, DepthTokens.FlatPrimaryButtonPressedFill)
        assertEquals(FlatDestructivePressed, DepthTokens.FlatDestructiveButtonPressedFill)
        assertEquals(TextHintColor, DepthTokens.InsetPlaceholderColor)
        assertEquals(PureWhite, DepthTokens.CardGradientStart)
        assertEquals(1.0f, DepthMotionTokens.BUTTON_PRESSED_SCALE, 1e-6f)
        assertEquals(androidx.compose.ui.unit.Dp(0f), Dimens.ButtonPressedTranslationY)
        assertEquals(androidx.compose.ui.unit.Dp(0f), Dimens.CardRiseTranslationY)
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
        assertEquals(enKeys.size, bnKeys.size)
        assertEquals(enKeys.size, arKeys.size)
    }
}
