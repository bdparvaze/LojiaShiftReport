package com.lojia.shiftreport

import com.lojia.shiftreport.util.NativeQrCodeEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Stage 12 Unit Test: Verifies ISO/IEC 18004 Annex D version information bits
 * and top-right / bottom-left version info placement for QR versions 7 and above.
 */
class Stage12QrVersionInfoUnitTest {

    @Test
    fun versionInfoBits_belowVersion7_returnsNull() {
        assertNull(NativeQrCodeEncoder.versionInfoBits(6))
    }

    @Test
    fun versionInfoBits_standardValues_matchIso18004AnnexD() {
        assertEquals(0x07C94, NativeQrCodeEncoder.versionInfoBits(7))
        assertEquals(0x085BC, NativeQrCodeEncoder.versionInfoBits(8))
        assertEquals(0x09A99, NativeQrCodeEncoder.versionInfoBits(9))
        assertEquals(0x0A4D3, NativeQrCodeEncoder.versionInfoBits(10))
        assertEquals(0x28C69, NativeQrCodeEncoder.versionInfoBits(40))
    }

    @Test
    fun encode_version7Input_writesVersion7BlocksInTopRightAndBottomLeft() {
        // At EcLevel.M (default), version 6 holds 106 UTF-8 bytes and version 7 holds 122 UTF-8 bytes.
        // A 110-byte ASCII input therefore selects version 7 (45 x 45 matrix).
        val input = "A".repeat(110)
        val matrix = NativeQrCodeEncoder.encode(input, NativeQrCodeEncoder.EcLevel.M)

        assertEquals(45, matrix.width)
        assertEquals(45, matrix.height)

        val size = matrix.width
        var topRightBits = 0
        var bottomLeftBits = 0
        for (i in 0..17) {
            val trRow = i / 3
            val trCol = (size - 11) + (i % 3)
            if (matrix.get(trCol, trRow)) {
                topRightBits = topRightBits or (1 shl i)
            }

            val blRow = (size - 11) + (i % 3)
            val blCol = i / 3
            if (matrix.get(blCol, blRow)) {
                bottomLeftBits = bottomLeftBits or (1 shl i)
            }
        }

        assertEquals(0x07C94, topRightBits)
        assertEquals(0x07C94, bottomLeftBits)
    }

    @Test
    fun qrVersionInfo_matchesBchFormula() {
        val generator = 0x1F25
        for (version in 7..40) {
            val tableValue = NativeQrCodeEncoder.versionInfoBits(version)
            assertNotNull("Missing table entry for version $version", tableValue)
            val bits = tableValue!!
            assertEquals("Version mismatch in high 6 bits for v$version", version, bits shr 12)

            var remainder = version shl 12
            for (bit in 17 downTo 12) {
                if (((remainder shr bit) and 1) != 0) {
                    remainder = remainder xor (generator shl (bit - 12))
                }
            }
            assertEquals("BCH remainder mismatch for v$version", remainder, bits and 0xFFF)
        }
    }
}
