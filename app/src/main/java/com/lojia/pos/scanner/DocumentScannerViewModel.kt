package com.lojia.pos.scanner

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.lojia.pos.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val dao = database.documentScannerDao()

    val searchQuery = MutableStateFlow("")

    val documents: StateFlow<List<ScannedDocument>> = combine(
        dao.getAllDocuments(),
        searchQuery
    ) { docs, query ->
        if (query.isBlank()) {
            docs
        } else {
            docs.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun saveScannedResult(result: GmsDocumentScanningResult, customTitle: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pdfUri = result.pdf?.uri ?: return@launch
                val pageCount = result.pdf?.pageCount ?: result.pages?.size ?: 1

                val context = getApplication<Application>().applicationContext
                val docsDir = File(context.filesDir, "scanned_documents").apply { if (!exists()) mkdirs() }
                val thumbsDir = File(docsDir, "thumbnails").apply { if (!exists()) mkdirs() }

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val title = customTitle?.ifBlank { null } ?: "Scan_$timeStamp"
                val destFile = File(docsDir, "${title.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}_$timeStamp.pdf")

                context.contentResolver.openInputStream(pdfUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val docPagesDir = File(docsDir, "pages_$timeStamp").apply { if (!exists()) mkdirs() }
                val pageImagePaths = mutableListOf<String>()

                result.pages?.forEachIndexed { index, page ->
                    val pageUri = page.imageUri
                    if (pageUri != null) {
                        val pageFile = File(docPagesDir, "page_${index + 1}.jpg")
                        try {
                            context.contentResolver.openInputStream(pageUri)?.use { input ->
                                FileOutputStream(pageFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            if (pageFile.exists() && pageFile.length() > 0) {
                                pageImagePaths.add(pageFile.absolutePath)
                            }
                        } catch (e: Exception) {
                            Log.e("DocumentScannerVM", "Failed to save page image $index", e)
                        }
                    }
                }

                var thumbPath = ""
                val firstPagePath = pageImagePaths.firstOrNull()
                if (firstPagePath != null) {
                    val thumbFile = File(thumbsDir, "thumb_$timeStamp.jpg")
                    try {
                        File(firstPagePath).copyTo(thumbFile, overwrite = true)
                        if (thumbFile.exists() && thumbFile.length() > 0) {
                            thumbPath = thumbFile.absolutePath
                        }
                    } catch (e: Exception) {
                        Log.e("DocumentScannerVM", "Failed to copy thumbnail", e)
                    }
                }

                // Perform OCR immediately
                val extractedText = if (pageImagePaths.isNotEmpty()) {
                    OcrTextExtractor.extractTextFromImages(context, pageImagePaths)
                } else ""

                // Generate DOCX file if text extracted
                var docxPath = ""
                if (extractedText.isNotBlank()) {
                    val docxDir = File(docsDir, "docx").apply { if (!exists()) mkdirs() }
                    val docxFile = File(docxDir, "${title.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}_$timeStamp.docx")
                    val success = DocxExporter.createDocxFile(docxFile, title, extractedText)
                    if (success && docxFile.exists()) {
                        docxPath = docxFile.absolutePath
                    }
                }

                val doc = ScannedDocument(
                    title = title,
                    pdfUriPath = destFile.absolutePath,
                    pageCount = pageCount,
                    fileSizeBytes = destFile.length(),
                    createdAtMillis = System.currentTimeMillis(),
                    thumbnailPath = thumbPath,
                    ocrText = extractedText,
                    docxUriPath = docxPath
                )

                dao.insertDocument(doc)
            } catch (e: Exception) {
                Log.e("DocumentScannerVM", "Error saving scanned document", e)
            }
        }
    }

    fun extractTextAndGenerateDocx(document: ScannedDocument, onComplete: (String, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val docsDir = File(context.filesDir, "scanned_documents")
            
            var text = document.ocrText
            var docxPath = document.docxUriPath

            if (text.isBlank()) {
                // Find saved page images folder
                val pdfName = File(document.pdfUriPath).nameWithoutExtension
                val timeStamp = pdfName.substringAfterLast("_")
                val pagesDir = File(docsDir, "pages_$timeStamp")
                val pageFiles = pagesDir.listFiles()?.filter { it.extension.lowercase() == "jpg" }?.sortedBy { it.name } ?: emptyList()
                
                if (pageFiles.isNotEmpty()) {
                    text = OcrTextExtractor.extractTextFromImages(context, pageFiles.map { it.absolutePath })
                }
            }

            if (text.isNotBlank() && docxPath.isBlank()) {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val docxDir = File(docsDir, "docx").apply { if (!exists()) mkdirs() }
                val docxFile = File(docxDir, "${document.title.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}_$timeStamp.docx")
                val success = DocxExporter.createDocxFile(docxFile, document.title, text)
                if (success && docxFile.exists()) {
                    docxPath = docxFile.absolutePath
                }
            }

            if (text != document.ocrText || docxPath != document.docxUriPath) {
                dao.updateOcrAndDocx(document.id, text, docxPath)
            }

            withContext(Dispatchers.Main) {
                onComplete(text, docxPath)
            }
        }
    }

    fun shareDocx(context: Context, document: ScannedDocument) {
        try {
            val file = File(document.docxUriPath)
            if (!file.exists()) {
                Toast.makeText(context, "Word file not generated yet. Extracting text first...", Toast.LENGTH_SHORT).show()
                extractTextAndGenerateDocx(document) { text, path ->
                    if (path.isNotBlank() && File(path).exists()) {
                        shareDocxFileInternal(context, File(path))
                    } else {
                        Toast.makeText(context, "Could not generate Word document.", Toast.LENGTH_SHORT).show()
                    }
                }
                return
            }
            shareDocxFileInternal(context, file)
        } catch (e: Exception) {
            Log.e("DocumentScannerVM", "Error sharing docx", e)
            Toast.makeText(context, "Could not share Word file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareDocxFileInternal(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Word (.docx)"))
    }

    fun renameDocument(document: ScannedDocument, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.updateDocumentTitle(document.id, newTitle.trim())
            } catch (e: Exception) {
                Log.e("DocumentScannerVM", "Error renaming document", e)
            }
        }
    }

    fun deleteDocument(document: ScannedDocument) {
        deleteDocuments(listOf(document))
    }

    fun deleteDocuments(documents: List<ScannedDocument>) {
        if (documents.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val docsDir = File(context.filesDir, "scanned_documents")

            for (document in documents) {
                try {
                    // Delete PDF file
                    val pdfFile = File(document.pdfUriPath)
                    if (pdfFile.exists()) {
                        pdfFile.delete()
                    }

                    // Delete thumbnail
                    if (document.thumbnailPath.isNotBlank()) {
                        val thumbFile = File(document.thumbnailPath)
                        if (thumbFile.exists()) {
                            thumbFile.delete()
                        }
                    }

                    // Delete DOCX file if generated
                    if (document.docxUriPath.isNotBlank()) {
                        val docxFile = File(document.docxUriPath)
                        if (docxFile.exists()) {
                            docxFile.delete()
                        }
                    }

                    // Delete page image directory
                    val pdfName = File(document.pdfUriPath).nameWithoutExtension
                    val timeStamp = pdfName.substringAfterLast("_")
                    val pagesDir = File(docsDir, "pages_$timeStamp")
                    if (pagesDir.exists() && pagesDir.isDirectory) {
                        pagesDir.deleteRecursively()
                    }
                } catch (e: Exception) {
                    Log.e("DocumentScannerVM", "Error deleting files for doc ${document.id}", e)
                }
            }

            try {
                dao.deleteDocumentsByIds(documents.map { it.id })
            } catch (e: Exception) {
                Log.e("DocumentScannerVM", "Error deleting documents from database", e)
            }
        }
    }

    fun protectPdfWithPassword(
        context: Context,
        document: ScannedDocument,
        password: String,
        onResult: (Result<File>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sourceFile = File(document.pdfUriPath)
            if (!sourceFile.exists()) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(FileNotFoundException("Original PDF file not found")))
                }
                return@launch
            }

            val docsDir = File(context.filesDir, "scanned_documents/protected")
            val sanitizedTitle = document.title.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val timeStamp = System.currentTimeMillis()
            val destinationFile = File(docsDir, "${sanitizedTitle}_protected_${timeStamp}.pdf")

            val result = PdfProtector.createPasswordProtectedPdf(context, sourceFile, destinationFile, password)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun shareProtectedPdf(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "Protected PDF file not found", Toast.LENGTH_SHORT).show()
                return
            }
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
            context.startActivity(Intent.createChooser(shareIntent, "Share Protected PDF"))
        } catch (e: Exception) {
            Log.e("DocumentScannerVM", "Error sharing protected PDF", e)
            Toast.makeText(context, "Could not share protected PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun viewProtectedPdf(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "Protected PDF file not found", Toast.LENGTH_SHORT).show()
                return
            }
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
            context.startActivity(Intent.createChooser(viewIntent, "Open Protected PDF"))
        } catch (e: ActivityNotFoundException) {
            Log.e("DocumentScannerVM", "No PDF viewer app found", e)
            Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("DocumentScannerVM", "Error opening protected PDF", e)
            Toast.makeText(context, "Could not open protected PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDocument(context: Context, document: ScannedDocument) {
        try {
            val file = File(document.pdfUriPath)
            if (!file.exists()) {
                Toast.makeText(context, "Document file not found", Toast.LENGTH_SHORT).show()
                return
            }

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
            Toast.makeText(context, "Could not share document: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun viewDocument(context: Context, document: ScannedDocument) {
        try {
            val file = File(document.pdfUriPath)
            if (!file.exists()) {
                Toast.makeText(context, "Document file not found", Toast.LENGTH_SHORT).show()
                return
            }

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
        } catch (e: ActivityNotFoundException) {
            Log.e("DocumentScannerVM", "No PDF viewer app installed", e)
            Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("DocumentScannerVM", "Error opening document", e)
            Toast.makeText(context, "Could not open document: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
