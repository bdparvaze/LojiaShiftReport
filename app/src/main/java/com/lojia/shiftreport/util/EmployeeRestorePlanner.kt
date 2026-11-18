package com.lojia.shiftreport.util

import com.lojia.shiftreport.data.Employee

data class BackupEmployee(
    val id: String,
    val name: String,
    val monthlySalaryMinor: Long,
    val phone: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val createdByAdminId: String = ""
)

enum class EmployeeRestoreAction {
    MAP_EXISTING,
    INSERT,
    SKIP_BLANK,
    SKIP_DUPLICATE
}

data class EmployeeRestoreStep(
    val backupId: String,
    val action: EmployeeRestoreAction,
    val targetId: String?
)

/**
 * Pure planning function for employee restore matching.
 * - Blank normalized names are skipped (SKIP_BLANK).
 * - Duplicate normalized names within the same backup are skipped (SKIP_DUPLICATE).
 * - Matches to existing local employees by normalized name reuse the existing id without overwriting (MAP_EXISTING).
 * - Unmatched employees insert with backup id if free, otherwise [newId] (INSERT).
 */
fun planEmployeeRestore(
    backup: List<BackupEmployee>,
    existing: List<Employee>,
    isIdFree: (String) -> Boolean,
    newId: () -> String
): List<EmployeeRestoreStep> {
    if (backup.isEmpty()) return emptyList()

    val existingByNormalizedName = mutableMapOf<String, String>()
    val allocatedIds = mutableSetOf<String>()
    for (emp in existing) {
        allocatedIds.add(emp.id)
        val norm = normalizeRestoreName(emp.name)
        if (norm.isNotEmpty() && !existingByNormalizedName.containsKey(norm)) {
            existingByNormalizedName[norm] = emp.id
        }
    }

    val seenBackupNames = mutableSetOf<String>()
    val steps = ArrayList<EmployeeRestoreStep>(backup.size)

    for (item in backup) {
        val norm = normalizeRestoreName(item.name)
        if (norm.isEmpty()) {
            steps.add(
                EmployeeRestoreStep(
                    backupId = item.id,
                    action = EmployeeRestoreAction.SKIP_BLANK,
                    targetId = null
                )
            )
            continue
        }

        if (!seenBackupNames.add(norm)) {
            steps.add(
                EmployeeRestoreStep(
                    backupId = item.id,
                    action = EmployeeRestoreAction.SKIP_DUPLICATE,
                    targetId = null
                )
            )
            continue
        }

        val existingId = existingByNormalizedName[norm]
        if (existingId != null) {
            steps.add(
                EmployeeRestoreStep(
                    backupId = item.id,
                    action = EmployeeRestoreAction.MAP_EXISTING,
                    targetId = existingId
                )
            )
            continue
        }

        val chosenId = if (item.id.isNotBlank() && !allocatedIds.contains(item.id) && isIdFree(item.id)) {
            item.id
        } else {
            newId()
        }
        allocatedIds.add(chosenId)
        steps.add(
            EmployeeRestoreStep(
                backupId = item.id,
                action = EmployeeRestoreAction.INSERT,
                targetId = chosenId
            )
        )
    }

    return steps
}
