package com.lojia.shiftreport

import com.lojia.shiftreport.auth.SecurityRecoveryVerifier
import com.lojia.shiftreport.printer.EscPosNativeProtocol
import com.lojia.shiftreport.printer.NetworkPrintCoordinator
import com.lojia.shiftreport.printer.PrinterConfigHelper
import com.lojia.shiftreport.printer.PrinterCutMode
import com.lojia.shiftreport.printer.PrinterDevice
import com.lojia.shiftreport.printer.PrinterDiscoveryManager
import com.lojia.shiftreport.printer.PrinterNavigationConfig
import com.lojia.shiftreport.printer.PrinterType
import com.lojia.shiftreport.printer.WifiPrinterManager
import com.lojia.shiftreport.util.SecurityUtils
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 24B Pure JVM Unit Tests covering:
 * - Task A: Drawer & Preview printer navigation routing
 * - Tasks B & C: Shared ESC/POS encoding, cut modes, trailing feeds, network auto-connect, per-printer settings
 * - Task D: NSD discovery list merge/loss helpers & standard raw-print port constant
 * - Task E: Hashed recovery answer verification, legacy upgrade, and rejection of empty/wrong answers
 */
class Stage24BPrinterAndAuthUnitTest {

    // ---- Task A ----

    @Test
    fun printerNavigation_drawerIncludesPrinterNearBackup_andPreviewRoutesToPrinter() {
        assertTrue(PrinterNavigationConfig.drawerHasPrinterNearBackup())
        assertEquals("printer", PrinterNavigationConfig.resolvePreviewPrinterSettingsTarget())
        assertTrue(PrinterNavigationConfig.isPrinterSection("printer"))
        assertTrue(PrinterNavigationConfig.isPrinterSection("thermal_printer"))
        assertFalse(PrinterNavigationConfig.isPrinterSection("profile"))
    }

    // ---- Tasks B & C ----

    @Test
    fun escPosProtocol_sharedEncoderProducesIdenticalBytes_andTrailingFeedsAndCutModesWork() {
        val sampleReceipt = buildString {
            append("[C]<b><font size='big'>Test Store</font></b>\n")
            append("[C]টেস্ট / العربية\n")
            append("[L]Subtotal[R]10.00\n")
            append("[C]Thank you\n")
        }
        val deterministicRasterizer: (String, Int) -> ByteArray = { line, width ->
            byteArrayOf(0x1D, 0x76, 0x30, 0x00, (width and 0xFF).toByte(), line.length.toByte(), 0x0A)
        }

        val streamA = EscPosNativeProtocol.encodeFormattedTextWithStatus(
            text = sampleReceipt,
            charsPerLine = 48,
            widthPx = 576,
            cutMode = PrinterCutMode.FULL,
            bitmapLineRasterizer = deterministicRasterizer
        )
        val streamB = EscPosNativeProtocol.encodeFormattedTextWithStatus(
            text = sampleReceipt,
            charsPerLine = 48,
            widthPx = 576,
            cutMode = PrinterCutMode.FULL,
            bitmapLineRasterizer = deterministicRasterizer
        )
        assertArrayEquals(streamA.bytes, streamB.bytes)
        assertFalse(streamA.usedTextFallback)

        val feedCount = EscPosNativeProtocol.TRAILING_LINE_FEED_COUNT
        assertTrue("Trailing line feeds should be in 3..5", feedCount in 3..5)

        // FULL cut ends with LF*feedCount + GS V 66 0
        val expectedFullSuffix = ByteArray(feedCount) { 0x0A } + byteArrayOf(0x1D, 0x56, 0x42, 0x00)
        assertArrayEquals(
            expectedFullSuffix,
            streamA.bytes.takeLast(expectedFullSuffix.size).toByteArray()
        )

        // PARTIAL cut ends with LF*feedCount + GS V 1
        val partialStream = EscPosNativeProtocol.encodeFormattedTextWithStatus(
            text = sampleReceipt,
            charsPerLine = 48,
            widthPx = 576,
            cutMode = PrinterCutMode.PARTIAL,
            bitmapLineRasterizer = deterministicRasterizer
        )
        val expectedPartialSuffix = ByteArray(feedCount) { 0x0A } + byteArrayOf(0x1D, 0x56, 0x01)
        assertArrayEquals(
            expectedPartialSuffix,
            partialStream.bytes.takeLast(expectedPartialSuffix.size).toByteArray()
        )

        // NONE emits no GS V cut sequence
        val noneStream = EscPosNativeProtocol.encodeFormattedTextWithStatus(
            text = sampleReceipt,
            charsPerLine = 48,
            widthPx = 576,
            cutMode = PrinterCutMode.NONE,
            bitmapLineRasterizer = deterministicRasterizer
        )
        val expectedNoneSuffix = ByteArray(feedCount) { 0x0A }
        assertArrayEquals(
            expectedNoneSuffix,
            noneStream.bytes.takeLast(expectedNoneSuffix.size).toByteArray()
        )
        assertFalse(containsByteSequence(noneStream.bytes, byteArrayOf(0x1D, 0x56)))
    }

    @Test
    fun escPosProtocol_encodeRasterJob_honorsAllCutModes() {
        val rasterBody = byteArrayOf(0x1B, 0x61, 0x01, 0x1D, 0x76, 0x30, 0x00, 0x0A)
        val full = EscPosNativeProtocol.encodeRasterJob(rasterBody, PrinterCutMode.FULL)
        val partial = EscPosNativeProtocol.encodeRasterJob(rasterBody, PrinterCutMode.PARTIAL)
        val none = EscPosNativeProtocol.encodeRasterJob(rasterBody, PrinterCutMode.NONE)

        assertTrue(full.takeLast(4).toByteArray().contentEquals(EscPosNativeProtocol.CUT_FULL))
        assertTrue(partial.takeLast(3).toByteArray().contentEquals(EscPosNativeProtocol.CUT_PARTIAL))
        assertFalse(containsByteSequence(none, byteArrayOf(0x1D, 0x56)))
    }

    @Test
    fun networkPrintCoordinator_autoConnectsBeforeSendingWhenDisconnected_andClosesOnOneShot() {
        val events = mutableListOf<String>()
        var connected = false
        val fakeTransport = object : NetworkPrintCoordinator.TcpPrintTransport {
            override fun isConnected(): Boolean = connected
            override fun connect(host: String, port: Int) {
                events.add("connect:$host:$port")
                connected = true
            }

            override fun writeAndFlush(bytes: ByteArray) {
                events.add("write:${bytes.size}")
            }

            override fun close() {
                events.add("close")
                connected = false
            }
        }

        val oneShotResult = NetworkPrintCoordinator.sendWithAutoConnect(
            transport = fakeTransport,
            host = "printer.local",
            port = WifiPrinterManager.DEFAULT_PORT,
            payload = byteArrayOf(0x1B, 0x40),
            closeAfterAutoConnect = true
        )
        assertTrue(oneShotResult.isSuccess)
        assertEquals(listOf("connect:printer.local:9100", "write:2", "close"), events)
        assertFalse(connected)

        // Now keep connected session open and send directly without reconnecting/closing
        events.clear()
        connected = true
        val reuseResult = NetworkPrintCoordinator.sendWithAutoConnect(
            transport = fakeTransport,
            host = "printer.local",
            port = WifiPrinterManager.DEFAULT_PORT,
            payload = byteArrayOf(0x0A),
            closeAfterAutoConnect = true
        )
        assertTrue(reuseResult.isSuccess)
        assertEquals(listOf("write:1"), events)
        assertTrue(connected)
    }

    @Test
    fun networkPrintCoordinator_returnsFailureWhenConnectThrows() {
        val failingTransport = object : NetworkPrintCoordinator.TcpPrintTransport {
            override fun isConnected(): Boolean = false
            override fun connect(host: String, port: Int) {
                throw IOException("Connection refused")
            }

            override fun writeAndFlush(bytes: ByteArray) {
                throw AssertionError("Should not write when connect fails")
            }

            override fun close() {}
        }

        val result = NetworkPrintCoordinator.sendWithAutoConnect(
            transport = failingTransport,
            host = "printer.local",
            port = WifiPrinterManager.DEFAULT_PORT,
            payload = byteArrayOf(0x0A),
            closeAfterAutoConnect = true
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun printerConfigHelper_resolvesPerPrinterKeysAndDefaults() {
        val netId1 = PrinterConfigHelper.buildNetworkIdentifier("10.0.0.5", 9100)
        val netId2 = PrinterConfigHelper.buildNetworkIdentifier("10.0.0.6", 9100)
        val netKey1 = PrinterConfigHelper.buildCutModeStorageKey("network", netId1)
        val netKey2 = PrinterConfigHelper.buildCutModeStorageKey("network", netId2)
        val btKey = PrinterConfigHelper.buildCutModeStorageKey("bluetooth", "AA:BB:CC:DD:EE:FF")

        assertNotEquals(netKey1, netKey2)
        assertNotEquals(netKey1, btKey)

        assertEquals(PrinterCutMode.FULL, PrinterConfigHelper.resolveCutMode(null, "network"))
        assertEquals(PrinterCutMode.NONE, PrinterConfigHelper.resolveCutMode(null, "bluetooth"))
        assertEquals(PrinterCutMode.PARTIAL, PrinterConfigHelper.resolveCutMode("PARTIAL", "bluetooth"))
        assertEquals(PrinterCutMode.NONE, PrinterConfigHelper.resolveCutMode("NONE", "network"))
    }

    // ---- Task D ----

    @Test
    fun discoveryHelpers_mergeDeduplicateAndRemoveLostPrinters() {
        assertEquals(9100, WifiPrinterManager.DEFAULT_PORT)
        val p1 = PrinterDevice("Kitchen Printer", "10.0.0.10", PrinterType.WIFI)
        assertEquals(9100, p1.port)

        val p2 = PrinterDevice("Bar Printer", "10.0.0.11", PrinterType.WIFI, 9100)
        val p1Renamed = PrinterDevice("Kitchen Thermal", "10.0.0.10", PrinterType.WIFI, 9100)
        val blankHost = PrinterDevice("Invalid", "   ", PrinterType.WIFI, 9100)

        var list = emptyList<PrinterDevice>()
        list = PrinterDiscoveryManager.mergeDiscoveredPrinter(list, p1)
        list = PrinterDiscoveryManager.mergeDiscoveredPrinter(list, p2)
        list = PrinterDiscoveryManager.mergeDiscoveredPrinter(list, p1Renamed)
        list = PrinterDiscoveryManager.mergeDiscoveredPrinter(list, blankHost)

        assertEquals(2, list.size)
        assertEquals("Kitchen Thermal", list[0].name)
        assertEquals("Bar Printer", list[1].name)

        list = PrinterDiscoveryManager.removeLostPrinter(list, "Bar Printer")
        assertEquals(1, list.size)
        assertEquals("Kitchen Thermal", list[0].name)
    }

    // ---- Task E ----

    @Test
    fun securityRecoveryVerifier_hashesNormalizedAnswer_rejectsMissingOrWrong_andUpgradesLegacy() {
        assertFalse(SecurityRecoveryVerifier.isRecoveryConfigured(null, "hash"))
        assertFalse(SecurityRecoveryVerifier.isRecoveryConfigured("Question?", ""))
        assertFalse(SecurityRecoveryVerifier.isRecoveryConfigured("", ""))
        assertTrue(SecurityRecoveryVerifier.isRecoveryConfigured("First pet?", "pbkdf2:sha256:210000:aa:bb"))

        val storedHash = SecurityRecoveryVerifier.hashNormalizedAnswer("  Fluffy  ")
        assertTrue(storedHash.startsWith("pbkdf2:sha256:"))
        assertFalse(storedHash.contains("fluffy", ignoreCase = true))

        // Case-insensitive and whitespace-trimmed match against normalized hash
        val lowerMatch = SecurityRecoveryVerifier.verifyAnswerWithUpgrade("fluffy", storedHash)
        assertTrue(lowerMatch.isMatch)
        assertNull(lowerMatch.newHashToStore)

        val upperTrimMatch = SecurityRecoveryVerifier.verifyAnswerWithUpgrade("  FLUFFY ", storedHash)
        assertTrue(upperTrimMatch.isMatch)

        // Wrong answer rejected
        val wrong = SecurityRecoveryVerifier.verifyAnswerWithUpgrade("Max", storedHash)
        assertFalse(wrong.isMatch)

        // Empty stored answer rejected (never accepts hardcoded fallback)
        assertFalse(SecurityRecoveryVerifier.verifyAnswerWithUpgrade("School", "").isMatch)
        assertFalse(SecurityRecoveryVerifier.verifyAnswerWithUpgrade("Lojia", "").isMatch)

        // Legacy plaintext answer matches case-insensitively and emits PBKDF2 upgrade hash
        val legacyPlain = SecurityRecoveryVerifier.verifyAnswerWithUpgrade("fluffy", "Fluffy")
        assertTrue(legacyPlain.isMatch)
        assertNotNull(legacyPlain.newHashToStore)
        assertTrue(legacyPlain.newHashToStore!!.startsWith("pbkdf2:sha256:"))
        assertTrue(SecurityRecoveryVerifier.verifyAnswerWithUpgrade("FLUFFY", legacyPlain.newHashToStore!!).isMatch)

        // Legacy exact-case hash matches exact casing and upgrades to normalized lowercase hash
        val legacyExactHash = SecurityUtils.hashSecret("Fluffy")
        val exactUpgrade = SecurityRecoveryVerifier.verifyAnswerWithUpgrade("Fluffy", legacyExactHash)
        assertTrue(exactUpgrade.isMatch)
        assertNotNull(exactUpgrade.newHashToStore)
        assertTrue(SecurityRecoveryVerifier.verifyAnswerWithUpgrade("fluffy", exactUpgrade.newHashToStore!!).isMatch)
    }

    // ---- Stage 24B String Resource Lockstep ----

    @Test
    fun stage24BStrings_existInAllThreeLocalesWithoutDuplicates() {
        val keys = listOf(
            "nav_printer",
            "printer_name_label",
            "printer_name_placeholder",
            "printer_paper_width_label",
            "printer_paper_width_58mm",
            "printer_paper_width_80mm",
            "printer_cut_mode_label",
            "printer_cut_mode_none",
            "printer_cut_mode_partial",
            "printer_cut_mode_full",
            "printer_error_not_connected",
            "printer_error_no_network_ip",
            "printer_error_invalid_port",
            "printer_error_no_bt_printer",
            "printer_error_bt_permission",
            "printer_error_bt_unavailable",
            "printer_error_bt_disabled",
            "printer_error_bt_device_not_found",
            "printer_error_bt_connect_failed",
            "printer_error_network_connect_failed",
            "auth_recovery_unavailable"
        )
        val localePaths = listOf(
            "src/main/res/values/strings.xml",
            "src/main/res/values-bn/strings.xml",
            "src/main/res/values-ar/strings.xml"
        )
        for (path in localePaths) {
            val direct = File(path)
            val file = if (direct.exists()) direct else File("app", path)
            assertTrue("Missing $path", file.exists())
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string")
            val map = LinkedHashMap<String, String>()
            val dupes = mutableListOf<String>()
            for (i in 0 until nodes.length) {
                val n = nodes.item(i)
                val name = n.attributes?.getNamedItem("name")?.nodeValue
                if (!name.isNullOrBlank()) {
                    if (map.containsKey(name)) dupes.add(name)
                    map[name] = n.textContent.orEmpty()
                }
            }
            assertTrue("$path has duplicate keys: $dupes", dupes.isEmpty())
            for (k in keys) {
                assertFalse("$path missing or blank key: $k", map[k].isNullOrBlank())
            }
        }
    }

    private fun containsByteSequence(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || haystack.size < needle.size) return false
        outer@ for (i in 0..(haystack.size - needle.size)) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return true
        }
        return false
    }
}
