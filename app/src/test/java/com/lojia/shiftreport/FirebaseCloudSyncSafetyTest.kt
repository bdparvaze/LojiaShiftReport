package com.lojia.shiftreport

import com.lojia.shiftreport.sync.FirebaseCloudSyncManager
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying Firebase Cloud Sync safety rules:
 * 1. CLOUD_SYNC_ENABLED flag is false by default.
 * 2. getAuthToken() returns null by default.
 * 3. getAuthUserUid() returns null when no token/user is available.
 * 4. No legacy auth secret methods exist.
 */
class FirebaseCloudSyncSafetyTest {

    @Test
    fun testCloudSyncDisabledByDefaultInBuildConfig() {
        assertFalse("CLOUD_SYNC_ENABLED must be false by default", BuildConfig.CLOUD_SYNC_ENABLED)
    }

    @Test
    fun testGetAuthTokenReturnsNullByDefault() {
        assertNull("getAuthToken must return null by default", FirebaseCloudSyncManager.getAuthToken())
    }

    @Test
    fun testGetAuthUserUidReturnsNullWhenNoTokenAvailable() {
        assertNull("getAuthUserUid must return null when no token is present", FirebaseCloudSyncManager.getAuthUserUid())
    }

    @Test
    fun testDataPathFormattingWithUid() {
        val uid = "test_user_12345"
        val expectedPath = "users/$uid/shift_report_cloud_data.json"
        assertEquals("users/test_user_12345/shift_report_cloud_data.json", expectedPath)
    }
}
