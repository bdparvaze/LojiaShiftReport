package com.lojia.shiftreport

import com.lojia.shiftreport.util.mergePinHash
import com.lojia.shiftreport.util.resolveRestoredIsActive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage6RestoreSafetyUnitTest {

    @Test
    fun mergePinHash_backupNull_keepsLocal() {
        assertEquals("abc", mergePinHash(localHash = "abc", backupHash = null))
    }

    @Test
    fun mergePinHash_backupEmpty_keepsLocal() {
        assertEquals("abc", mergePinHash(localHash = "abc", backupHash = ""))
    }

    @Test
    fun mergePinHash_backupBlankSpaces_keepsLocal() {
        assertEquals("abc", mergePinHash(localHash = "abc", backupHash = "   "))
    }

    @Test
    fun mergePinHash_localNullAndBackupPresent_usesBackup() {
        assertEquals("xyz", mergePinHash(localHash = null, backupHash = "xyz"))
    }

    @Test
    fun mergePinHash_bothPresent_usesBackup() {
        assertEquals("xyz", mergePinHash(localHash = "abc", backupHash = "xyz"))
    }

    @Test
    fun mergePinHash_bothNull_returnsNull() {
        assertNull(mergePinHash(localHash = null, backupHash = null))
    }

    @Test
    fun resolveRestoredIsActive_existingLocalPreserved() {
        assertTrue(resolveRestoredIsActive(localIsActive = true, backupIsActive = false))
        assertFalse(resolveRestoredIsActive(localIsActive = false, backupIsActive = true))
    }

    @Test
    fun resolveRestoredIsActive_newRecordUsesBackupOrDefaultsTrue() {
        assertFalse(resolveRestoredIsActive(localIsActive = null, backupIsActive = false))
        assertTrue(resolveRestoredIsActive(localIsActive = null, backupIsActive = true))
        assertTrue(resolveRestoredIsActive(localIsActive = null, backupIsActive = null))
    }
}
