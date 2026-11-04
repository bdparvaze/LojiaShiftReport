package com.lojia.shiftreport.data

import android.content.Context
import com.lojia.shiftreport.util.SecurityUtils
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface and implementation providing an abstraction layer
 * over Room Database operations for daily Shift Reports.
 */
class ShiftReportRepository(private val reportDao: ReportDao) {

    val allReports: Flow<List<ShiftReport>> = reportDao.getAllShiftReports()

    suspend fun getReportById(id: Int): ShiftReport? {
        return reportDao.getShiftReportById(id)
    }

    suspend fun insertReport(report: ShiftReport): Long {
        return reportDao.insertShiftReport(report)
    }

    suspend fun deleteReport(report: ShiftReport) {
        reportDao.deleteShiftReport(report)
    }

    suspend fun getDraft(): DraftReport? {
        return reportDao.getDraftReport()
    }

    suspend fun saveDraft(draft: DraftReport) {
        reportDao.saveDraftReport(draft)
    }

    suspend fun clearDraft() {
        reportDao.clearDraftReport()
    }

    suspend fun getActiveCashiers(): List<Cashier> {
        return reportDao.getActiveCashiers()
    }

    fun observeActiveCashiers(): Flow<List<Cashier>> {
        return reportDao.observeActiveCashiers()
    }

    suspend fun getArchivedCashiers(): List<Cashier> {
        return reportDao.getArchivedCashiers()
    }

    suspend fun getCashierById(id: String): Cashier? {
        return reportDao.getCashierById(id)
    }

    suspend fun archiveCashier(id: String, timestamp: Long = System.currentTimeMillis()) {
        reportDao.archiveCashier(id, timestamp)
    }

    suspend fun unarchiveCashier(id: String) {
        reportDao.unarchiveCashier(id)
    }

    suspend fun deleteCashier(cashier: Cashier) {
        reportDao.deleteCashier(cashier)
    }

    /**
     * Requirement 4: Query shift_reports for cashier shift history before allowing delete.
     */
    suspend fun getShiftCountByCashierId(cashierId: String): Int {
        val cashier = reportDao.getCashierById(cashierId)
        val name = cashier?.name.orEmpty()
        return if (name.isNotBlank()) {
            reportDao.getShiftCountByCashierIdOrName(cashierId, name)
        } else {
            reportDao.getShiftCountByCashierId(cashierId)
        }
    }

    suspend fun verifyAdminCredentials(context: Context, enteredPinOrPassword: String): Boolean {
        val trimmed = enteredPinOrPassword.trim()
        if (trimmed.isEmpty()) return false
        val profile = reportDao.getUserProfileOnce() ?: return false
        val storedPin = profile.pin
        val storedHash = profile.passwordHash

        val pinResult = if (storedPin.isNotEmpty()) SecurityUtils.verifySecretWithUpgrade(trimmed, storedPin) else null
        if (pinResult?.isMatch == true) {
            if (pinResult.newHashToStore != null) {
                reportDao.saveUserProfile(profile.copy(pin = pinResult.newHashToStore))
            }
            return true
        }

        val passResult = if (storedHash.isNotEmpty()) SecurityUtils.verifySecretWithUpgrade(trimmed, storedHash) else null
        if (passResult?.isMatch == true) {
            if (passResult.newHashToStore != null) {
                reportDao.saveUserProfile(profile.copy(passwordHash = passResult.newHashToStore))
            }
            return true
        }

        return false
    }

    companion object {
        @Volatile
        private var INSTANCE: ShiftReportRepository? = null

        fun getInstance(context: Context): ShiftReportRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = ShiftReportRepository(db.reportDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
