package com.lojia.shiftreport.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class AdvancePaymentMethod {
    CASH,
    BANK
}

enum class AdvanceTransactionType {
    ADVANCE,
    CORRECTION
}

/**
 * Per-employee advance & payroll correction ledger entry.
 * Never hard-deleted; voiding sets [isVoid] = true for audit compliance.
 */
@Entity(tableName = "advance_transactions")
data class EmployerAdvanceTransaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val employeeId: String,
    val employeeName: String,
    val shiftId: String = "",
    val amount: Double, // Positive for advance, negative for CORRECTION
    val paymentMethod: String = AdvancePaymentMethod.CASH.name, // CASH or BANK
    val type: String = AdvanceTransactionType.ADVANCE.name, // ADVANCE or CORRECTION
    val reason: String = "",
    val originalTransactionId: String? = null,
    val isVoid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val createdByAdminId: String = ""
)
