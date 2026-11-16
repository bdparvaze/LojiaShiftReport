package com.lojia.shiftreport

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lojia.shiftreport.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration test verifying Room schema migration from version 19 to version 20:
 * - employees.monthlySalary (REAL) -> employees.monthlySalaryMinor (INTEGER)
 * - advance_transactions.amount (REAL) -> advance_transactions.amountMinor (INTEGER)
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
    fun testMigration19To20() {
        // 1. Create database at version 19 with legacy schema data
        val db = helper.createDatabase(TEST_DB, 19)

        // Seed business_profile with USD (2 decimal digits)
        db.execSQL(
            """
            INSERT INTO `business_profile` (
                `id`, `businessName`, `vatNumber`, `phone`, `email`, `address`,
                `mapLat`, `mapLng`, `workingHours`, `currency`, `country`,
                `vatRate`, `isTaxEnabled`, `isTaxIncluded`, `logoUri`
            ) VALUES (
                1, 'Test Store', '', '', '', '',
                0.0, 0.0, '08:00 AM - 10:00 PM', 'USD', 'United States',
                0.0, 0, 1, ''
            )
            """.trimIndent()
        )

        // Seed one employee with monthlySalary = 1500.5
        db.execSQL(
            """
            INSERT INTO `employees` (
                `id`, `name`, `monthlySalary`, `phone`, `isActive`, `createdAt`, `createdByAdminId`
            ) VALUES (
                'emp-1', 'Rahim', 1500.5, NULL, 1, 1700000000000, 'admin'
            )
            """.trimIndent()
        )

        // Seed one advance transaction with amount = 250.0
        db.execSQL(
            """
            INSERT INTO `advance_transactions` (
                `id`, `employeeId`, `employeeName`, `shiftId`, `amount`,
                `paymentMethod`, `type`, `reason`, `originalTransactionId`,
                `isVoid`, `createdAt`, `createdByAdminId`
            ) VALUES (
                'adv-1', 'emp-1', 'Rahim', 'shift-1', 250.0,
                'CASH', 'ADVANCE', 'Salary advance', NULL,
                0, 1700000001000, 'admin'
            )
            """.trimIndent()
        )

        db.close()

        // 2. Run MIGRATION_19_20 and validate schema against exported 20.json
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 20, true, AppDatabase.MIGRATION_19_20)

        // 3. Assert employees.monthlySalaryMinor == 150050L
        migratedDb.query("SELECT monthlySalaryMinor FROM employees WHERE id = 'emp-1'").use { empCursor ->
            assertTrue(empCursor.moveToFirst())
            assertEquals(150050L, empCursor.getLong(0))
        }

        // 4. Assert advance_transactions.amountMinor == 25000L
        migratedDb.query("SELECT amountMinor FROM advance_transactions WHERE id = 'adv-1'").use { advCursor ->
            assertTrue(advCursor.moveToFirst())
            assertEquals(25000L, advCursor.getLong(0))
        }

        migratedDb.close()
    }
}
