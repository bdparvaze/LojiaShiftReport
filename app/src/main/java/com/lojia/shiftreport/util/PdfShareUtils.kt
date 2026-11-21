package com.lojia.shiftreport.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import com.lojia.shiftreport.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import androidx.core.content.FileProvider

/**
 * Utility for opening and sharing generated PDF report files via Android Intents.
 */
object PdfShareUtils {

    /**
     * Resolves the app-specific visible external directory for reports.
     */
    fun getAppReportsDirectory(context: Context): File {
        val folderName = try {
            context.getString(R.string.app_reports_folder_name)
        } catch (e: Exception) {
            "Lojia Reports"
        }
        val reportsDir = context.getExternalFilesDir(null)?.resolve(folderName)
            ?: context.filesDir.resolve(folderName)
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        return reportsDir
    }

    /**
     * Resolves the app-private cache directory for temporary statement exports (never stored externally).
     */
    fun getTempStatementExportsDirectory(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun deleteTempStatementFile(file: File?) {
        if (file == null) return
        try {
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    private tailrec fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }

    /**
     * Prints an exported PDF file through the Android PrintManager / PrintDocumentAdapter system service.
     */
    fun printPdfFile(context: Context, pdfFile: File, jobName: String): Boolean {
        val hostContext = findActivity(context) ?: context
        val printManager = hostContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null || !pdfFile.exists()) {
            Toast.makeText(
                context,
                context.getString(R.string.statement_printer_unavailable_error),
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        return try {
            val adapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(pdfFile.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }
                    try {
                        FileInputStream(pdfFile).use { input ->
                            FileOutputStream(destination.fileDescriptor).use { output ->
                                val buffer = ByteArray(8192)
                                while (true) {
                                    if (cancellationSignal?.isCanceled == true) {
                                        callback.onWriteCancelled()
                                        return
                                    }
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    output.write(buffer, 0, read)
                                }
                                output.flush()
                            }
                        }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            true
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.statement_printer_unavailable_error),
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    /**
     * Opens the reports folder in the device file manager.
     */
    fun openFolderInFileManager(context: Context, folder: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                folder
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    folder
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, context.getString(R.string.could_not_open_folder, ex.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Opens the exported PDF with the system viewer or any installed PDF application.
     */
    fun openPdfFile(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.pdf_chooser_open_report)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.no_pdf_viewer_found), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares the exported PDF file via Intent chooser.
     */
    fun sharePdfFile(context: Context, uri: Uri, title: String = context.getString(R.string.pdf_email_subject)) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.pdf_chooser_share_report)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.could_not_share_pdf, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sends the exported PDF file as an email attachment using Intent.ACTION_SEND.
     */
    fun sendEmailWithPdf(context: Context, pdfUri: Uri, recipientEmail: String = "") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                if (recipientEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail.trim()))
                }
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.pdf_email_subject))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.pdf_email_body))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.pdf_chooser_email_report))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.could_not_share_pdf, e.localizedMessage ?: ""),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Shares the exported PDF file directly to WhatsApp, falling back to a generic chooser if not installed.
     */
    fun sharePdfToWhatsApp(context: Context, pdfUri: Uri, message: String = "") {
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            if (message.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, message)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(whatsappIntent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    if (message.isNotBlank()) {
                        putExtra(Intent.EXTRA_TEXT, message)
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(fallbackIntent, context.getString(R.string.pdf_chooser_share_report))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.could_not_share_pdf, e.localizedMessage ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
