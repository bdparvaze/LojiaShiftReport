package com.lojia.pos.scanner

import android.content.Context
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfProtector {

    private const val TAG = "PdfProtector"
    private var isInitialized = false

    private fun initIfNeeded(context: Context) {
        if (!isInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize PDFBoxResourceLoader", e)
            }
        }
    }

    /**
     * Encrypts the [sourceFile] using 128-bit key standard protection policy with [password].
     * Writes output to [destinationFile].
     * Does not modify or overwrite [sourceFile].
     */
    suspend fun createPasswordProtectedPdf(
        context: Context,
        sourceFile: File,
        destinationFile: File,
        password: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Source PDF file does not exist"))
            }

            initIfNeeded(context)

            destinationFile.parentFile?.let { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }

            PDDocument.load(sourceFile).use { document ->
                // Access permissions: allow printing, screen reader, but standard user access requires password
                val accessPermission = AccessPermission()
                accessPermission.setCanPrint(true)
                accessPermission.setCanExtractContent(true)

                // 128-bit AES encryption
                val keyLength = 128
                val protectionPolicy = StandardProtectionPolicy(password, password, accessPermission).apply {
                    setEncryptionKeyLength(keyLength)
                }

                document.protect(protectionPolicy)
                document.save(destinationFile)
            }

            if (destinationFile.exists() && destinationFile.length() > 0) {
                Result.success(destinationFile)
            } else {
                Result.failure(IllegalStateException("Failed to write protected PDF output file"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating password protected PDF", e)
            Result.failure(e)
        }
    }
}
