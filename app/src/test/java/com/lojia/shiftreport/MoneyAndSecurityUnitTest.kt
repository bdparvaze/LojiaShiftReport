package com.lojia.shiftreport

import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for critical financial calculations (MoneyFormat)
 * and credential security hashing/upgrades (SecurityUtils).
 */
class MoneyAndSecurityUnitTest {

    // ----------------------------------------------------------------------
    // 1. MONEYFORMAT TESTS (Long-based Minor Unit API)
    // ----------------------------------------------------------------------

    @Test
    fun moneyFormat_addition_preventsFloatingPointDrift() {
        // 0.1 + 0.2 -> 10 + 20 = 30 minor units
        val val1 = MoneyFormat.toMinorUnits(0.1)
        val val2 = MoneyFormat.toMinorUnits(0.2)
        val result = MoneyFormat.addMinor(val1, val2)
        assertEquals(30L, result)
        assertEquals(0.30, MoneyFormat.toMajorUnits(result), 0.0001)

        val multiAdd = MoneyFormat.addMinor(
            MoneyFormat.toMinorUnits(100.05),
            MoneyFormat.toMinorUnits(50.10),
            MoneyFormat.toMinorUnits(25.00)
        )
        assertEquals(17515L, multiAdd)
    }

    @Test
    fun moneyFormat_subtractionAndMultiplication_roundsHalfUp() {
        val sub = MoneyFormat.subtractMinor(
            MoneyFormat.toMinorUnits(100.05),
            MoneyFormat.toMinorUnits(5.05)
        )
        assertEquals(9500L, sub)

        val mul = MoneyFormat.multiplyMinor(
            MoneyFormat.toMinorUnits(12.50),
            3.0
        )
        assertEquals(3750L, mul)
    }

    @Test
    fun moneyFormat_calculateExpectedCash_correct() {
        // Expected Cash = Starting Cash + Cash Sales + Pay In - Pay Out - Returns
        val expected = MoneyFormat.calculateExpectedCashMinor(
            startingCash = 20000L,
            cashSales = 10050L,
            payIn = 5025L,
            payOut = 2025L,
            salesReturns = 500L
        )
        assertEquals(32550L, expected)
    }

    @Test
    fun moneyFormat_calculateVariance_handlesShortageAndOverage() {
        val expected = 30000L

        // Actual count is 295.00 -> Shortage of -5.00
        val shortage = MoneyFormat.calculateVarianceMinor(29500L, expected)
        assertEquals(-500L, shortage)

        // Actual count is 310.00 -> Excess of +10.00
        val overage = MoneyFormat.calculateVarianceMinor(31000L, expected)
        assertEquals(1000L, overage)

        // Actual count is 300.00 -> Balanced (0)
        val balanced = MoneyFormat.calculateVarianceMinor(30000L, expected)
        assertEquals(0L, balanced)
    }

    @Test
    fun moneyFormat_calculateTax_inclusiveAndExclusive() {
        // Exclusive tax: 100.00 (10000L) at 15% = 1500L
        val taxExclusive = MoneyFormat.calculateTaxMinor(10000L, 15.0, isTaxInclusive = false)
        assertEquals(1500L, taxExclusive)

        // Inclusive tax: 115.00 (11500L) at 15% tax-inclusive = 115 * 15 / 115 = 1500L
        val taxInclusive = MoneyFormat.calculateTaxMinor(11500L, 15.0, isTaxInclusive = true)
        assertEquals(1500L, taxInclusive)
    }

    @Test
    fun moneyFormat_roundTrip_toMinorAndMajor() {
        // 0.01 -> 1L -> 0.01
        val m1 = MoneyFormat.toMinorUnits(0.01)
        assertEquals(1L, m1)
        assertEquals(0.01, MoneyFormat.toMajorUnits(m1), 0.0001)

        // 0.1 + 0.2 -> 0.30000000000000004 -> 30L -> 0.3
        val m2 = MoneyFormat.toMinorUnits(0.1 + 0.2)
        assertEquals(30L, m2)
        assertEquals(0.3, MoneyFormat.toMajorUnits(m2), 0.0001)

        // 1234.565 -> rounds HALF_UP to 1234.57 -> 123457L -> 1234.57
        val m3 = MoneyFormat.toMinorUnits(1234.565)
        assertEquals(123457L, m3)
        assertEquals(1234.57, MoneyFormat.toMajorUnits(m3), 0.0001)
    }

    @Test
    fun moneyFormat_multiplyMinor_roundingHalfUp() {
        // 100L * 0.125 = 12.5 -> rounds HALF_UP to 13L
        val r1 = MoneyFormat.multiplyMinor(100L, 0.125)
        assertEquals(13L, r1)

        // 100L * 0.124 = 12.4 -> rounds HALF_UP to 12L
        val r2 = MoneyFormat.multiplyMinor(100L, 0.124)
        assertEquals(12L, r2)
    }

    @Test
    fun moneyFormat_calculateTaxMinor_matrix() {
        val rates = doubleArrayOf(0.0, 5.0, 15.0, 8.25)
        val amounts = longArrayOf(1L, 99L, 100L, 12345L)

        for (rate in rates) {
            for (amount in amounts) {
                // Exclusive
                val taxExc = MoneyFormat.calculateTaxMinor(amount, rate, isTaxInclusive = false)
                val expectedExc = java.math.BigDecimal.valueOf(amount)
                    .multiply(java.math.BigDecimal.valueOf(rate))
                    .divide(java.math.BigDecimal.valueOf(100.0), 4, java.math.RoundingMode.HALF_UP)
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .toLong()
                assertEquals("Tax exclusive mismatch for amount=$amount, rate=$rate", expectedExc, taxExc)

                // Inclusive
                val taxInc = MoneyFormat.calculateTaxMinor(amount, rate, isTaxInclusive = true)
                val expectedInc = if (rate <= 0.0) 0L else {
                    java.math.BigDecimal.valueOf(amount)
                        .multiply(java.math.BigDecimal.valueOf(rate))
                        .divide(java.math.BigDecimal.valueOf(100.0 + rate), 4, java.math.RoundingMode.HALF_UP)
                        .setScale(0, java.math.RoundingMode.HALF_UP)
                        .toLong()
                }
                assertEquals("Tax inclusive mismatch for amount=$amount, rate=$rate", expectedInc, taxInc)
            }
        }
    }

    @Test
    fun shiftReport_calculationsWithSampleJson() {
        // Setup a ShiftReport with some mock JSON values
        val sampleReport = com.lojia.shiftreport.data.ShiftReport(
            cashierName = "John Doe",
            openingCash = 5000L,
            grossCash = 10000L,
            madaPayments = 15000L,
            digitalWallet = 5000L,
            totalDiscounts = 2000L,
            salesReturns = 1000L,
            dueCreditEntriesJson = """
                [
                    {"id": "1", "customerName": "Alice", "amount": 3000, "note": "Pending Payment", "phone": "123456"},
                    {"id": "2", "customerName": "Bob", "amount": 2000, "note": "Credit Sale", "phone": "654321"}
                ]
            """.trimIndent(),
            previousDueCollectionsJson = """
                [
                    {"id": "3", "customerName": "Charlie", "amount": 1500, "paymentMode": "CASH", "note": "Previous Balance"},
                    {"id": "4", "customerName": "David", "amount": 2500, "paymentMode": "BANK", "note": "Wire Transfer"}
                ]
            """.trimIndent()
        )

        // grossSales = grossCash (10000L) + madaPayments (15000L) + digitalWallet (5000L) = 30000L
        assertEquals(30000L, sampleReport.grossSales)
        assertEquals(30000L, sampleReport.totalSales)

        // netSales = grossSales (30000L) - totalDiscounts (2000L) - salesReturns (1000L) = 27000L
        assertEquals(27000L, sampleReport.netSales)

        // dueCreditEntriesList size = 2, total amount = 3000 + 2000 = 5000L
        assertEquals(2, sampleReport.dueCreditEntriesList.size)
        assertEquals(5000L, sampleReport.totalDueCredit)

        // previousDueCollectionsList size = 2
        assertEquals(2, sampleReport.previousDueCollectionsList.size)
        // CASH collections = 1500L
        assertEquals(1500L, sampleReport.totalPreviousDueCash)
        // BANK collections = 2500L
        assertEquals(2500L, sampleReport.totalPreviousDueBank)
        // totalDueCollected = 1500 + 2500 = 4000L
        assertEquals(4000L, sampleReport.totalDueCollected)
    }


    // ----------------------------------------------------------------------
    // 2. SECURITYUTILS TESTS
    // ----------------------------------------------------------------------

    @Test
    fun securityUtils_newHash_verifiesSuccessfully() {
        val pin = "123456"
        val hash = SecurityUtils.hashSecret(pin)

        assertTrue("New hash must use PBKDF2 with 210000 iterations", hash.startsWith("pbkdf2:sha256:210000:"))
        val result = SecurityUtils.verifySecretWithUpgrade(pin, hash)
        assertTrue("Verification with correct PIN must pass", result.isMatch)
        assertNull("No upgrade needed for fresh 210000 iterations hash", result.newHashToStore)
    }

    @Test
    fun securityUtils_wrongPin_failsVerification() {
        val pin = "123456"
        val hash = SecurityUtils.hashSecret(pin)

        val result = SecurityUtils.verifySecretWithUpgrade("654321", hash)
        assertFalse("Verification with wrong PIN must fail", result.isMatch)
        assertNull("No upgrade hash generated when PIN does not match", result.newHashToStore)
    }

    @Test
    fun securityUtils_oldLegacySha256Hash_verifiesAndReturnsUpgradedHash() {
        val pin = "888888"
        val legacyHash = SecurityUtils.legacyHashSecret(pin)

        val result = SecurityUtils.verifySecretWithUpgrade(pin, legacyHash)
        assertTrue("Old legacy SHA-256 hash must verify", result.isMatch)
        assertNotNull("Must return an upgraded PBKDF2 hash", result.newHashToStore)
        assertTrue("Upgraded hash must be PBKDF2 with 210000 iterations", result.newHashToStore!!.startsWith("pbkdf2:sha256:210000:"))
    }

    @Test
    fun securityUtils_old10000IterationPbkdf2Hash_verifiesAndReturnsUpgradedHash() {
        val pin = "555555"
        val old10kHash = SecurityUtils.hashSecretPbkdf2(pin, iterations = 10000)
        assertTrue("Pre-condition: must have 10000 iterations", old10kHash.startsWith("pbkdf2:sha256:10000:"))

        val result = SecurityUtils.verifySecretWithUpgrade(pin, old10kHash)
        assertTrue("Old 10000-iteration PBKDF2 hash must verify successfully", result.isMatch)
        assertNotNull("Must upgrade low-iteration PBKDF2 hash to 210000 iterations", result.newHashToStore)
        assertTrue("Upgraded hash must be 210000 iterations", result.newHashToStore!!.startsWith("pbkdf2:sha256:210000:"))
    }

    @Test
    fun securityUtils_twoHashesOfSamePin_differDueToRandomSalt() {
        val pin = "4321"
        val hash1 = SecurityUtils.hashSecret(pin)
        val hash2 = SecurityUtils.hashSecret(pin)

        assertNotEquals("Two hashes of the same PIN must differ due to unique random salts", hash1, hash2)
        assertTrue("First hash must verify PIN", SecurityUtils.verifySecret(pin, hash1))
        assertTrue("Second hash must verify PIN", SecurityUtils.verifySecret(pin, hash2))
    }
}
