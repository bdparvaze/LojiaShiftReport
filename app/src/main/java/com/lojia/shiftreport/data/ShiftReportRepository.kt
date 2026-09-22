package com.lojia.shiftreport.data

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
}
