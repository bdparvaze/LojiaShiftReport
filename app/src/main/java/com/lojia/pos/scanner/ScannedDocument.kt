package com.lojia.pos.scanner

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scanned_documents")
data class ScannedDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val pdfUriPath: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val thumbnailPath: String = "",
    val ocrText: String = "",
    val docxUriPath: String = ""
)

@Dao
interface DocumentScannerDao {
    @Query("SELECT * FROM scanned_documents ORDER BY createdAtMillis DESC")
    fun getAllDocuments(): Flow<List<ScannedDocument>>

    @Query("SELECT * FROM scanned_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Int): ScannedDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ScannedDocument): Long

    @Query("UPDATE scanned_documents SET title = :newTitle WHERE id = :id")
    suspend fun updateDocumentTitle(id: Int, newTitle: String)

    @Query("UPDATE scanned_documents SET ocrText = :ocrText, docxUriPath = :docxUriPath WHERE id = :id")
    suspend fun updateOcrAndDocx(id: Int, ocrText: String, docxUriPath: String)

    @Delete
    suspend fun deleteDocument(document: ScannedDocument)

    @Query("DELETE FROM scanned_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)

    @Query("DELETE FROM scanned_documents WHERE id IN (:ids)")
    suspend fun deleteDocumentsByIds(ids: List<Int>)
}
