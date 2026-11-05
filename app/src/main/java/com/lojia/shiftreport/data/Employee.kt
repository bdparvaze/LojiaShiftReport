package com.lojia.shiftreport.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Unlimited Employee entity for Foodics + SAP Payroll standard.
 */
@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val monthlySalary: Double,
    val phone: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdByAdminId: String = ""
)

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActive(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllActiveOnce(): List<Employee>

    @Query("SELECT * FROM employees ORDER BY isActive DESC, name ASC")
    fun getAll(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: Employee)

    @Update
    suspend fun update(employee: Employee)

    @Query("UPDATE employees SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean)
}
