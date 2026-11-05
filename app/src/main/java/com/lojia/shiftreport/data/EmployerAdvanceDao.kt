package com.lojia.shiftreport.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployerAdvanceDao {
    @Query("SELECT * FROM advance_transactions WHERE employeeId = :id AND isVoid = 0 ORDER BY createdAt DESC")
    fun getByEmployee(id: String): Flow<List<EmployerAdvanceTransaction>>

    @Query("SELECT * FROM advance_transactions WHERE employeeId = :id AND isVoid = 0 ORDER BY createdAt DESC")
    suspend fun getByEmployeeOnce(id: String): List<EmployerAdvanceTransaction>

    @Query("SELECT * FROM advance_transactions WHERE isVoid = 0 ORDER BY createdAt DESC")
    fun getAllActive(): Flow<List<EmployerAdvanceTransaction>>

    @Query("SELECT * FROM advance_transactions WHERE isVoid = 0 ORDER BY createdAt DESC")
    suspend fun getAllActiveOnce(): List<EmployerAdvanceTransaction>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM advance_transactions WHERE employeeId = :id AND isVoid = 0")
    suspend fun getTotalByEmployee(id: String): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM advance_transactions WHERE isVoid = 0 AND createdAt BETWEEN :start AND :end")
    suspend fun getTotalInRange(start: Long, end: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: EmployerAdvanceTransaction)

    @Query("UPDATE advance_transactions SET isVoid = 1 WHERE id = :id")
    suspend fun voidTransaction(id: String)
}
