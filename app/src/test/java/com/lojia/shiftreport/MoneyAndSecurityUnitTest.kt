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
    // 1. MONEYFORMAT TESTS
    // ----------------------------------------------------------------------

    @Test
    fun moneyFormat_addition_preventsFloatingPointDrift() {
        // IEEE-754 raw 0.1 + 0.2 produces 0.30000000000000004
        val result = MoneyFormat.add(0.1, 0.2)
        assertEquals(0.30, result, 0.0001)

        val multiAdd = MoneyFormat.add(100.05, 50.10, 25.00)
        assertEquals(175.15, multiAdd, 0.0001)
    }

    @Test
    fun moneyFormat_subtractionAndMultiplication_roundsHalfUp() {
        val sub = MoneyFormat.subtract(100.05, 5.05)
        assertEquals(95.00, sub, 0.0001)

        val mul = MoneyFormat.multiply(12.50, 3.0)
        assertEquals(37.50, mul, 0.0001)
    }

    @Test
    fun moneyFormat_calculateExpectedCash_correct() {
        // Expected Cash = Starting Cash + Cash Sales + Pay In - Pay Out - Returns
        val expected = MoneyFormat.calculateExpectedCash(
            startingCash = 200.00,
            cashSales = 100.50,
            payIn = 50.25,
            payOut = 20.25,
            salesReturns = 5.00
        )
        // 200.00 + 100.50 + 50.25 - 20.25 - 5.00 = 325.50
        assertEquals(325.50, expected, 0.0001)
    }

    @Test
    fun moneyFormat_calculateVariance_handlesShortageAndOverage() {
        val expected = 300.00

        // Actual count is 295.00 -> Shortage of -5.00
        val shortage = MoneyFormat.calculateVariance(295.00, expected)
        assertEquals(-5.00, shortage, 0.0001)

        // Actual count is 310.00 -> Excess of +10.00
        val overage = MoneyFormat.calculateVariance(310.00, expected)
        assertEquals(10.00, overage, 0.0001)

        // Actual count is 300.00 -> Balanced (0.00)
        val balanced = MoneyFormat.calculateVariance(300.00, expected)
        assertEquals(0.00, balanced, 0.0001)
    }

    @Test
    fun moneyFormat_calculateTax_inclusiveAndExclusive() {
        // Exclusive tax: 100.00 at 15% = 15.00
        val taxExclusive = MoneyFormat.calculateTax(100.00, 15.0, isTaxInclusive = false)
        assertEquals(15.00, taxExclusive, 0.0001)

        // Inclusive tax: 115.00 at 15% tax-inclusive = 115 * 15 / 115 = 15.00
        val taxInclusive = MoneyFormat.calculateTax(115.00, 15.0, isTaxInclusive = true)
        assertEquals(15.00, taxInclusive, 0.0001)
    }


    // ----------------------------------------------------------------------
    // 2. SECURITYUTILS TESTS
    // ----------------------------------------------------------------------

    @Test
    fun securityUtils_pbkdf2_hashAndVerify_success() {
        val pin = "123456"
        val hash = SecurityUtils.hashSecretPbkdf2(pin)

        assertTrue("Hash should start with PBKDF2 header", hash.startsWith("pbkdf2:sha256:"))
        assertTrue("PIN verification should pass", SecurityUtils.verifySecret(pin, hash))
        assertFalse("Wrong PIN verification should fail", SecurityUtils.verifySecret("654321", hash))
    }

    @Test
    fun securityUtils_legacyUpgrade_doesNotLockOut() {
        val pin = "888888"
        // Generate legacy SHA-256 hash format
        val legacyHash = SecurityUtils.hashSecret(pin)

        // Verify using upgrade helper
        val result = SecurityUtils.verifySecretWithUpgrade(pin, legacyHash)

        assertTrue("Legacy PIN match should succeed", result.isMatch)
        assertNotNull("Should propose a new upgraded PBKDF2 hash to store", result.newHashToStore)
        assertTrue("Upgraded hash should start with PBKDF2 header", result.newHashToStore!!.startsWith("pbkdf2:sha256:"))
    }

    @Test
    fun securityUtils_wrongPin_failsVerification() {
        val pin = "999999"
        val hash = SecurityUtils.hashSecretPbkdf2(pin)

        val result = SecurityUtils.verifySecretWithUpgrade("000000", hash)
        assertFalse("Verification with wrong PIN must return false", result.isMatch)
        assertNull("No upgraded hash should be produced on failed verification", result.newHashToStore)
    }
}
