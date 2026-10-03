package com.lojia.shiftreport.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedAddressDao {
    @Query("SELECT * FROM saved_addresses ORDER BY isDefault DESC, createdAt DESC")
    fun getAll(): Flow<List<SavedAddress>>

    @Query("SELECT * FROM saved_addresses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavedAddress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(address: SavedAddress)

    @Delete
    suspend fun delete(address: SavedAddress)

    @Query("UPDATE saved_addresses SET isDefault = 0")
    suspend fun clearDefaults()

    @Query("UPDATE saved_addresses SET isDefault = 1 WHERE id = :id")
    suspend fun markAsDefault(id: String)

    @Query("SELECT COUNT(*) FROM saved_addresses")
    suspend fun count(): Int
}
