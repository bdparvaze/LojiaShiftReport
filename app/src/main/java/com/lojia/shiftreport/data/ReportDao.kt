package com.lojia.shiftreport.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM shift_reports ORDER BY dateInMillis DESC, id DESC")
    fun getAllShiftReports(): Flow<List<ShiftReport>>

    @Query("SELECT * FROM shift_reports ORDER BY dateInMillis DESC, id DESC")
    suspend fun getAllShiftReportsList(): List<ShiftReport>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftReports(reports: List<ShiftReport>)

    @Query("SELECT * FROM shift_reports WHERE id = :id LIMIT 1")
    suspend fun getShiftReportById(id: Int): ShiftReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftReport(report: ShiftReport): Long

    @Delete
    suspend fun deleteShiftReport(report: ShiftReport)

    // Shift Sessions (Live cash drawer & open/close shifts)
    @Query("SELECT * FROM shift_sessions ORDER BY openedAt DESC")
    fun getAllShiftSessions(): Flow<List<ShiftSession>>

    @Query("SELECT * FROM shift_sessions ORDER BY openedAt DESC")
    suspend fun getAllShiftSessionsList(): List<ShiftSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftSessions(sessions: List<ShiftSession>)

    @Query("SELECT * FROM shift_sessions WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun getActiveShiftSession(): Flow<ShiftSession?>

    @Query("SELECT * FROM shift_sessions WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getActiveShiftSessionOnce(): ShiftSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftSession(session: ShiftSession): Long

    @Update
    suspend fun updateShiftSession(session: ShiftSession)

    // Cash Movements (Pay In / Pay Out)
    @Query("SELECT * FROM cash_movements WHERE shiftSessionId = :sessionId ORDER BY timestamp DESC")
    fun getCashMovementsForSession(sessionId: Int): Flow<List<CashMovement>>

    @Query("SELECT * FROM cash_movements ORDER BY timestamp DESC LIMIT 50")
    fun getAllCashMovements(): Flow<List<CashMovement>>

    @Query("SELECT * FROM cash_movements ORDER BY timestamp DESC")
    suspend fun getAllCashMovementsList(): List<CashMovement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMovements(movements: List<CashMovement>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMovement(movement: CashMovement): Long

    // Draft Reports
    @Query("SELECT * FROM draft_reports WHERE id = 1 LIMIT 1")
    suspend fun getDraftReport(): DraftReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraftReport(draft: DraftReport)

    @Query("DELETE FROM draft_reports WHERE id = 1")
    suspend fun clearDraftReport()

    // Cashiers
    @Query("SELECT * FROM cashiers ORDER BY name ASC")
    fun getAllCashiers(): Flow<List<Cashier>>

    @Query("SELECT * FROM cashiers ORDER BY name ASC")
    suspend fun getAllCashiersList(): List<Cashier>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashiers(cashiers: List<Cashier>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashier(cashier: Cashier): Long

    @Delete
    suspend fun deleteCashier(cashier: Cashier)

    // Users & Auth
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY username ASC")
    suspend fun getAllUsersList(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE pin = :pin LIMIT 1")
    suspend fun getUserByPin(pin: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    // Shop Receipt Config
    @Query("SELECT * FROM shop_receipt_config WHERE id = 1 LIMIT 1")
    fun getReceiptConfig(): Flow<ShopReceiptConfig?>

    @Query("SELECT * FROM shop_receipt_config WHERE id = 1 LIMIT 1")
    suspend fun getReceiptConfigOnce(): ShopReceiptConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReceiptConfig(config: ShopReceiptConfig)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAuditLogsList(): List<AuditLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long

    // Business Profile
    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    fun getBusinessProfile(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    suspend fun getBusinessProfileOnce(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBusinessProfile(profile: BusinessProfile)

    // App Settings
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSetting>>

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsList(): List<AppSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<AppSetting>)

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    fun observeSetting(key: String): Flow<String?>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSetting)

    @Query("SELECT * FROM cashiers WHERE name = :name LIMIT 1")
    suspend fun getCashierByName(name: String): Cashier?

    @Query("SELECT COUNT(*) FROM cashiers WHERE active = 1")
    suspend fun getCashierCount(): Int
}
