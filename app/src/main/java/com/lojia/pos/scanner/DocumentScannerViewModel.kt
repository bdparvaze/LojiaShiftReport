package com.lojia.pos.scanner

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.lojia.pos.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val dao = database.documentScannerDao()

    val documents: StateFlow<List<ScannedDocument>> = dao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveScannedResult(result: GmsDocumentScanningResult, customTitle: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pdfUri = result.pdf?.uri ?: return@launch
                val pageCount = result.pdf?.pageCount ?: result.pages?.size ?: 1

                val context = getApplication<Application>().applicationContext
                val docsDir = File(context.filesDir, "scanned_documents").apply { if (!exists()) mkdirs() }

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val title = customTitle?.ifBlank { null } ?: "Scan_$timeStamp"
                val destFile = File(docsDir, "$title.pdf")

                context.contentResolver.openInputStream(pdfUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val doc = ScannedDocument(
                    title = title,
                    pdfUriPath = destFile.absolutePath,
                    pageCount = pageCount,
                    fileSizeBytes = destFile.length(),
                    createdAtMillis = System.currentTimeMillis()
                )

                dao.insertDocument(doc)
            } catch (e: Exception) {
                Log.e("DocumentScannerVM", "Error saving scanned document", e)
            }
        }
    }

    fun deleteDocument(document: ScannedDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(document.pdfUriPath)
                if (file.exists()) {
                    file.delete()
                }
                dao.deleteDocument(document)
            } catch (e: Exception) {
                Log.e("DocumentScannerVM", "Error deleting document", e)
            }
        }
    }

    fun shareDocument(context: Context, document: ScannedDocument) {
        try {
            val file = File(document.pdfUriPath)
            if (!file.exists()) return

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
        } catch (e: Exception) {
            Log.e("DocumentScannerVM", "Error sharing document", e)
        }
    }

    fun viewDocument(context: Context, document: ScannedDocument) {
        try {
            val file = File(document.pdfUriPath)
            if (!file.exists()) return

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(viewIntent, "Open PDF"))
        } catch (e: Exception) {
            Log.e("DocumentScannerVM", "Error opening document", e)
        }
    }
}
