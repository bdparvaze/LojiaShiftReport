package com.lojia.shiftreport

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lojia.shiftreport.data.AppDatabase
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration test verifying Room schema migration from version 14 to version 15:
 * - shift_sessions: startingCash converted from REAL (e.g. 12.5) to INTEGER minor units (1250)
 * - shift_reports & draft_reports: JSON columns containing decimal amounts converted to minor units
 *
 * NOTE: Running this test requires the version 14 schema file in test assets:
 * app/schemas/com.lojia.shiftreport.data.AppDatabase/14.json
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration14To15() {
        // 1. Create database at version 14 with legacy schema data
        var db = helper.createDatabase(TEST_DB, 14)

        // Insert version 14 shift_sessions row with startingCash as REAL (12.5)
        db.execSQL(
            """
            INSERT INTO `shift_sessions` (
                `cashierName`, `shiftName`, `status`, `openedAt`, `closedAt`,
                `startingCash`, `cashSales`, `cardSales`, `digitalSales`,
                `totalDiscounts`, `salesReturns`, `totalPayIn`, `totalPayOut`,
                `expectedCash`, `actualCashCount`, `variance`, `notes`,
                `isLocked`, `managerSignedBy`, `managerSignTime`
            ) VALUES (
                'Zayd Ali', 'Morning', 'CLOSED', 1700000000000, 1700028800000,
                12.5, 100.0, 50.0, 25.0,
                5.0, 2.0, 10.0, 5.0,
                115.5, 115.5, 0.0, 'Session notes',
                1, 'Admin', 1700028900000
            )
            """.trimIndent()
        )

        // Insert version 14 shift_reports row with decimal amounts in JSON columns
        db.execSQL(
            """
            INSERT INTO `shift_reports` (
                `cashierName`, `shift`, `dateInMillis`, `openingCash`, `closingCash`,
                `grossCash`, `madaPayments`, `digitalWallet`, `totalDiscounts`, `salesReturns`,
                `staffMealsCount`, `totalExpenses`, `muasselQty`, `outdoorShishaQty`,
                `dueCreditEntriesJson`, `previousDueCollectionsJson`, `staffAdvancesJson`,
                `unpaidBillsJson`, `purchasedItemsJson`, `notes`, `isLocked`,
                `managerSignedBy`, `managerSignTime`
            ) VALUES (
                'Zayd Ali', 'Morning', 1700000000000, 10.0, 50.0,
                100.0, 50.0, 20.0, 5.0, 2.0,
                1, 10.0, 1.0, 0.0,
                '[{"id":"due1","customerName":"Customer A","amount":12.5}]',
                '[{"id":"prev1","customerName":"Customer B","amount":7.25}]',
                '[{"id":"adv1","staffName":"Staff C","amount":15.0}]',
                '[{"id":"unpaid1","tableOrOrderRef":"Table 1","amount":25.5}]',
                '[{"id":"item1","itemName":"Coffee Beans","quantity":2.0,"unitPrice":5.5,"totalAmount":11.0}]',
                'Report Notes', 1, 'Admin', 1700028900000
            )
            """.trimIndent()
        )

        // Insert version 14 draft_reports row
        db.execSQL(
            """
            INSERT INTO `draft_reports` (
                `id`, `cashierName`, `shift`, `dateInMillis`, `openingCash`, `closingCash`,
                `grossCash`, `madaPayments`, `digitalWallet`, `totalDiscounts`, `salesReturns`,
                `staffMealsCount`, `totalExpenses`, `muasselQty`, `outdoorShishaQty`,
                `dueCreditEntriesJson`, `previousDueCollectionsJson`, `staffAdvancesJson`,
                `unpaidBillsJson`, `purchasedItemsJson`, `notes`
            ) VALUES (
                1, 'Zayd Ali', 'Morning', 1700000000000, 10.0, 50.0,
                100.0, 50.0, 20.0, 5.0, 2.0,
                1, 10.0, 1.0, 0.0,
                '[{"id":"due1","customerName":"Customer A","amount":12.5}]',
                '[{"id":"prev1","customerName":"Customer B","amount":7.25}]',
                '[{"id":"adv1","staffName":"Staff C","amount":15.0}]',
                '[{"id":"unpaid1","tableOrOrderRef":"Table 1","amount":25.5}]',
                '[{"id":"item1","itemName":"Coffee Beans","quantity":2.0,"unitPrice":5.5,"totalAmount":11.0}]',
                'Draft Notes'
            )
            """.trimIndent()
        )

        db.close()

        // 2. Run MIGRATION_14_15 and validate schema
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 15, true, AppDatabase.MIGRATION_14_15)

        // 3. Assert startingCash in shift_sessions is converted to minor units (12.5 -> 1250)
        val sessionCursor = migratedDb.query("SELECT startingCash FROM shift_sessions WHERE cashierName = 'Zayd Ali'")
        try {
            sessionCursor.moveToFirst()
            val startingCash = sessionCursor.getLong(0)
            assertEquals(1250L, startingCash)
        } finally {
            sessionCursor.close()
        }

        // 4. Assert JSON amounts in shift_reports are converted to minor units
        val reportCursor = migratedDb.query(
            "SELECT dueCreditEntriesJson, previousDueCollectionsJson, staffAdvancesJson, unpaidBillsJson, purchasedItemsJson FROM shift_reports WHERE cashierName = 'Zayd Ali'"
        )
        try {
            reportCursor.moveToFirst()
            val dueCreditJson = reportCursor.getString(0)
            val prevDueJson = reportCursor.getString(1)
            val staffAdvJson = reportCursor.getString(2)
            val unpaidBillsJson = reportCursor.getString(3)
            val purchasedJson = reportCursor.getString(4)

            val dueCreditArr = JSONArray(dueCreditJson)
            assertEquals(1250L, dueCreditArr.getJSONObject(0).getLong("amount"))

            val prevDueArr = JSONArray(prevDueJson)
            assertEquals(725L, prevDueArr.getJSONObject(0).getLong("amount"))

            val staffAdvArr = JSONArray(staffAdvJson)
            assertEquals(1500L, staffAdvArr.getJSONObject(0).getLong("amount"))

            val unpaidArr = JSONArray(unpaidBillsJson)
            assertEquals(2550L, unpaidArr.getJSONObject(0).getLong("amount"))

            val purchasedArr = JSONArray(purchasedJson)
            assertEquals(550L, purchasedArr.getJSONObject(0).getLong("unitPrice"))
            assertEquals(1100L, purchasedArr.getJSONObject(0).getLong("totalAmount"))
        } finally {
            reportCursor.close()
        }

        // 5. Assert JSON amounts in draft_reports are converted to minor units
        val draftCursor = migratedDb.query(
            "SELECT dueCreditEntriesJson, purchasedItemsJson FROM draft_reports WHERE id = 1"
        )
        try {
            draftCursor.moveToFirst()
            val draftDueCreditJson = draftCursor.getString(0)
            val draftPurchasedJson = draftCursor.getString(1)

            val draftDueCreditArr = JSONArray(draftDueCreditJson)
            assertEquals(1250L, draftDueCreditArr.getJSONObject(0).getLong("amount"))

            val draftPurchasedArr = JSONArray(draftPurchasedJson)
            assertEquals(550L, draftPurchasedArr.getJSONObject(0).getLong("unitPrice"))
            assertEquals(1100L, draftPurchasedArr.getJSONObject(0).getLong("totalAmount"))
        } finally {
            draftCursor.close()
        }

        migratedDb.close()
    }
}
