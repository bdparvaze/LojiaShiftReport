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

    @Query("SELECT * FROM advance_transactions WHERE isVoid = 0 ORDER BY createdAt DESC")
    fun getAllActive(): Flow<List<EmployerAdvanceTransaction>>

    @Query("SELECT * FROM advance_transactions ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<EmployerAdvanceTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(advances: List<EmployerAdvanceTransaction>)

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM advance_transactions WHERE isVoid = 0 AND createdAt BETWEEN :start AND :end")
    suspend fun getTotalInRange(start: Long, end: Long): Long

    @Query("SELECT COUNT(*) FROM advance_transactions")
    fun observeAdvanceCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM advance_transactions")
    suspend fun getAdvanceCount(): Int
}
