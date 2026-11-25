package com.lojia.shiftreport

import androidx.compose.ui.graphics.Color
import com.lojia.shiftreport.ui.theme.DepthStyle
import com.lojia.shiftreport.ui.theme.DepthTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Stage26EParityUnitTest {

    private fun resolveAppModuleDir(): File {
        val candidates = listOf(File("."), File("app"), File(".."), File("../app"))
        return candidates.firstOrNull { dir ->
            File(dir, "src/main/java/com/lojia/shiftreport/ui/theme/DepthComponents.kt").isFile
        } ?: error("Unable to locate app module directory from ${File(".").absolutePath}")
    }

    @Test
    fun stage26E_authRootsAndSystemBarsUsePageBackgroundEdgeToEdge() {
        assertEquals(Color(0xFFF4F7FD), DepthTokens.PageBackground)
        assertEquals(Color(0xFFF4F7FD), DepthStyle().pageBackground)

        val appDir = resolveAppModuleDir()

        val colorsXml = File(appDir, "src/main/res/values/colors.xml").readText()
        assertTrue(
            "colors.xml background must be #FFF4F7FD",
            colorsXml.contains("""<color name="background">#FFF4F7FD</color>""")
        )

        val stylesXml = File(appDir, "src/main/res/values/styles.xml").readText()
        assertTrue(
            "styles.xml must set transparent statusBarColor for edge-to-edge",
            stylesXml.contains("""<item name="android:statusBarColor">@android:color/transparent</item>""")
        )
        assertTrue(
            "styles.xml must set transparent navigationBarColor for edge-to-edge",
            stylesXml.contains("""<item name="android:navigationBarColor">@android:color/transparent</item>""")
        )

        val mainActivitySrc = File(appDir, "src/main/java/com/lojia/shiftreport/MainActivity.kt").readText()
        assertTrue(
            "MainActivity must pass isLightSystemBarCanvas = !isAuthenticated to LojiaTheme",
            mainActivitySrc.contains("isLightSystemBarCanvas = !isAuthenticated")
        )

        val themeSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/theme/Theme.kt").readText()
        assertTrue(
            "Theme.kt must update window background drawable via SideEffect",
            themeSrc.contains("setBackgroundDrawable")
        )
        assertTrue(
            "Theme.kt must toggle light status bar icons for light auth canvas",
            themeSrc.contains("isAppearanceLightStatusBars = isLightSystemBarCanvas")
        )
        assertTrue(
            "Theme.kt must disable navigation bar contrast scrim on API 29+",
            themeSrc.contains("isNavigationBarContrastEnforced = false")
        )

        val authRootFiles = listOf(
            "src/main/java/com/lojia/shiftreport/auth/BiometricManager.kt",
            "src/main/java/com/lojia/shiftreport/auth/QuickPinScreen.kt",
            "src/main/java/com/lojia/shiftreport/auth/LojiaRegisterScreen.kt"
        )
        val forbiddenBgPatterns = listOf(
            ".background(Color.White)",
            ".background(PureWhite)",
            ".background(SurfaceLight)",
            ".background(MaterialTheme.colorScheme.background)"
        )
        authRootFiles.forEach { relPath ->
            val src = File(appDir, relPath).readText()
            assertTrue(
                "$relPath must paint root with pageBackground",
                src.contains("pageBackground")
            )
            forbiddenBgPatterns.forEach { forbidden ->
                assertFalse(
                    "$relPath must not overwrite canvas with $forbidden",
                    src.contains(forbidden)
                )
            }
        }

        val adminDialogSrc = File(appDir, "src/main/java/com/lojia/shiftreport/auth/AdminAuthDialog.kt").readText()
        assertTrue(
            "AdminAuthDialog must delegate dialog presentation to LojiaDialog with DialogSize.Small",
            adminDialogSrc.contains("LojiaDialog(") &&
                adminDialogSrc.contains("size = DialogSize.Small")
        )
        assertFalse(
            "AdminAuthDialog must not use a custom OverlayScrim backdrop",
            adminDialogSrc.contains("OverlayScrim")
        )
    }

    @Test
    fun stage26E_lipColorsAreOpaqueAndDistinctFromFaceAndBackground() {
        assertEquals(1.0f, DepthTokens.KeypadButtonLipColor.alpha, 1e-6f)
        assertEquals(1.0f, DepthTokens.RaisedButtonLipColor.alpha, 1e-6f)
        assertEquals(1.0f, DepthTokens.SecondaryButtonLipColor.alpha, 1e-6f)

        assertEquals(1.0f, DepthTokens.RAISED_CARD_BORDER_ALPHA, 1e-6f)
        assertEquals(1.0f, DepthTokens.INSET_FIELD_BORDER_ALPHA, 1e-6f)
        assertEquals(0.0f, DepthTokens.RAISED_BUTTON_LIP_ALPHA, 1e-6f)
        assertEquals(0.0f, DepthTokens.SECONDARY_BUTTON_LIP_ALPHA, 1e-6f)

        assertNotEquals(DepthTokens.KeypadButtonLipColor, DepthTokens.KeypadButtonGradientEnd)
        assertNotEquals(DepthTokens.KeypadButtonLipColor, DepthTokens.PageBackground)
        assertEquals(DepthTokens.RaisedButtonLipColor, DepthTokens.BrandButtonGradientEnd)
        assertEquals(DepthTokens.SecondaryButtonLipColor, DepthTokens.SecondaryButtonGradientEnd)
    }

    @Test
    fun stage26E_depthComponentsDrawOrderAndSigmaVerified() {
        val appDir = resolveAppModuleDir()
        val depthCompSrc = File(appDir, "src/main/java/com/lojia/shiftreport/ui/theme/DepthComponents.kt").readText()

        assertTrue(
            "drawSoftDropShadow must use exact Gaussian sigma = blurPx / 2f",
            depthCompSrc.contains("val sigmaPx = (blurPx / 2f).coerceAtLeast(0.5f)")
        )
        assertFalse(
            "drawWithContent must not paint over inset/card borders",
            depthCompSrc.contains("drawWithContent")
        )

        val insetSection = depthCompSrc.substringAfter("fun Modifier.depthInset(").substringBefore("fun Modifier.depthRaisedButton(")
        val insetFillIdx = insetSection.indexOf("drawOutline(")
        val insetBorderIdx = insetSection.indexOf("drawInsetShapeBorder(")
        assertTrue("depthInset must draw flat fill before crisp perimeter border", insetFillIdx in 0 until insetBorderIdx)
        assertFalse("depthInset must not draw bottom highlight in flat UI", insetSection.contains("insetBottomHighlightColor"))

        val keypadSection = depthCompSrc.substringAfter("fun Modifier.depthKeypadKey(").substringBefore("fun Modifier.depthLogoTile(")
        val shadowIdx = keypadSection.indexOf("drawSoftDropShadow(")
        val clipIdx = keypadSection.indexOf(".clip(shape)")
        val faceDrawIdx = keypadSection.indexOf("drawOutline(")
        val borderDrawIdx = keypadSection.indexOf("drawInsetShapeBorder(")
        assertTrue("depthKeypadKey drop shadow must draw before .clip(shape)", shadowIdx in 0 until clipIdx)
        assertTrue("depthKeypadKey flat fill must draw after .clip(shape)", clipIdx in 0 until faceDrawIdx)
        assertTrue("depthKeypadKey flat border must draw after fill", faceDrawIdx in 0 until borderDrawIdx)
        assertFalse("depthKeypadKey must not draw 3D lip in flat UI", keypadSection.contains("keypadLipColor"))
    }
}
