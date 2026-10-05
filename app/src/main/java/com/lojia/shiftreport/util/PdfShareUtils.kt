package com.lojia.shiftreport.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.lojia.shiftreport.R
import java.io.File
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
                Toast.makeText(context, "Could not open folder: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
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
            context.startActivity(Intent.createChooser(intent, "Open PDF Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.no_pdf_viewer_found), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares the exported PDF file via Intent chooser.
     */
    fun sharePdfFile(context: Context, uri: Uri, title: String = "Shift Sales Report PDF") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
                putExtra(Intent.EXTRA_SUBJECT, "Shift Sales Report PDF")
                putExtra(Intent.EXTRA_TEXT, "Please find attached the Shift Sales Report PDF.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "Email PDF Report")
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
                    Intent.createChooser(fallbackIntent, "Share PDF Report")
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
