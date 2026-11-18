package com.lojia.shiftreport

import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.util.BackupEmployee
import com.lojia.shiftreport.util.EmployeeRestoreAction
import com.lojia.shiftreport.util.planEmployeeRestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage20DBackupEmployeeUnitTest {

    @Test
    fun planEmployeeRestore_mapsNameMatchToExistingEmployee() {
        val existing = listOf(
            Employee(id = "local-1", name = "Rahim Uddin", monthlySalaryMinor = 250000L)
        )
        val backup = listOf(
            BackupEmployee(id = "bak-1", name = "  rAhIm uDdIn  ", monthlySalaryMinor = 100000L)
        )

        val steps = planEmployeeRestore(
            backup = backup,
            existing = existing,
            isIdFree = { true },
            newId = { "unused-uuid" }
        )

        assertEquals(1, steps.size)
        assertEquals("bak-1", steps[0].backupId)
        assertEquals(EmployeeRestoreAction.MAP_EXISTING, steps[0].action)
        assertEquals("local-1", steps[0].targetId)
    }

    @Test
    fun planEmployeeRestore_insertsWhenNoNameMatch() {
        val existing = listOf(
            Employee(id = "local-1", name = "Rahim", monthlySalaryMinor = 200000L)
        )
        val backup = listOf(
            BackupEmployee(id = "bak-2", name = "Karim", monthlySalaryMinor = 180000L)
        )

        val steps = planEmployeeRestore(
            backup = backup,
            existing = existing,
            isIdFree = { true },
            newId = { "generated-uuid" }
        )

        assertEquals(1, steps.size)
        assertEquals("bak-2", steps[0].backupId)
        assertEquals(EmployeeRestoreAction.INSERT, steps[0].action)
        assertEquals("bak-2", steps[0].targetId)
    }

    @Test
    fun planEmployeeRestore_doesNotOverwriteExistingSalary() {
        val existing = listOf(
            Employee(
                id = "local-1",
                name = "Sara",
                monthlySalaryMinor = 300000L,
                phone = "555-0101",
                isActive = false
            )
        )
        val backup = listOf(
            BackupEmployee(
                id = "bak-1",
                name = "sara",
                monthlySalaryMinor = 999999L,
                phone = "555-9999",
                isActive = true
            )
        )

        // Ensure EmployeeRestoreAction enum has no UPDATE action
        assertFalse(EmployeeRestoreAction.entries.any { it.name.contains("UPDATE", ignoreCase = true) })

        val steps = planEmployeeRestore(
            backup = backup,
            existing = existing,
            isIdFree = { true },
            newId = { "unused-uuid" }
        )

        assertEquals(1, steps.size)
        assertEquals(EmployeeRestoreAction.MAP_EXISTING, steps[0].action)
        assertEquals("local-1", steps[0].targetId)
        assertEquals(300000L, existing[0].monthlySalaryMinor)
        assertEquals("555-0101", existing[0].phone)
        assertFalse(existing[0].isActive)
    }

    @Test
    fun planEmployeeRestore_keepsBackupIdWhenFreeOtherwiseNewId() {
        val backup = listOf(
            BackupEmployee(id = "free-id", name = "Amina", monthlySalaryMinor = 150000L),
            BackupEmployee(id = "taken-id", name = "Bilal", monthlySalaryMinor = 160000L)
        )

        val steps = planEmployeeRestore(
            backup = backup,
            existing = emptyList(),
            isIdFree = { id -> id == "free-id" },
            newId = { "fresh-uuid-99" }
        )

        assertEquals(2, steps.size)
        assertEquals(EmployeeRestoreAction.INSERT, steps[0].action)
        assertEquals("free-id", steps[0].targetId)
        assertEquals(EmployeeRestoreAction.INSERT, steps[1].action)
        assertEquals("fresh-uuid-99", steps[1].targetId)
    }

    @Test
    fun planEmployeeRestore_skipsBlankName() {
        val backup = listOf(
            BackupEmployee(id = "bak-empty", name = "", monthlySalaryMinor = 100000L),
            BackupEmployee(id = "bak-spaces", name = "   \t  ", monthlySalaryMinor = 100000L)
        )

        val steps = planEmployeeRestore(
            backup = backup,
            existing = emptyList(),
            isIdFree = { true },
            newId = { "unused-uuid" }
        )

        assertEquals(2, steps.size)
        assertEquals("bak-empty", steps[0].backupId)
        assertEquals(EmployeeRestoreAction.SKIP_BLANK, steps[0].action)
        assertNull(steps[0].targetId)
        assertEquals("bak-spaces", steps[1].backupId)
        assertEquals(EmployeeRestoreAction.SKIP_BLANK, steps[1].action)
        assertNull(steps[1].targetId)
    }

    @Test
    fun planEmployeeRestore_skipsSecondDuplicateName() {
        val backup = listOf(
            BackupEmployee(id = "bak-1", name = "Nadia", monthlySalaryMinor = 140000L),
            BackupEmployee(id = "bak-2", name = "  nadia ", monthlySalaryMinor = 220000L)
        )

        val steps = planEmployeeRestore(
            backup = backup,
            existing = emptyList(),
            isIdFree = { true },
            newId = { "unused-uuid" }
        )

        assertEquals(2, steps.size)
        assertEquals(EmployeeRestoreAction.INSERT, steps[0].action)
        assertEquals("bak-1", steps[0].targetId)
        assertEquals("bak-2", steps[1].backupId)
        assertEquals(EmployeeRestoreAction.SKIP_DUPLICATE, steps[1].action)
        assertNull(steps[1].targetId)
    }

    @Test
    fun planEmployeeRestore_oldBackupWithoutEmployeesIsNoOp() {
        val existing = listOf(
            Employee(id = "local-1", name = "Existing Staff", monthlySalaryMinor = 250000L)
        )

        val steps = planEmployeeRestore(
            backup = emptyList(),
            existing = existing,
            isIdFree = { true },
            newId = { "unused-uuid" }
        )

        assertTrue(steps.isEmpty())
    }
}
