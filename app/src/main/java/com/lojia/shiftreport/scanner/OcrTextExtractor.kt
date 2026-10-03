package com.lojia.shiftreport.scanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

object OcrTextExtractor {

    private const val TAG = "OcrTextExtractor"

    /**
     * Extracts text from a list of scanned page image files with a 15-second timeout.
     * Uses [OcrImagePreprocessor] to enhance contrast for delicate scripts (such as Bengali/Bangla)
     * and structures output cleanly across multi-page scans.
     */
    suspend fun extractTextFromImages(context: Context, imagePaths: List<String>): String {
        if (imagePaths.isEmpty()) return ""
        return withTimeoutOrNull(15_000L) {
            try {
                withContext(Dispatchers.IO) {
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    try {
                        imagePaths.mapIndexedNotNull { index, path ->
                            val text = extractTextFromImage(context, recognizer, path)
                            if (text.isNotBlank()) {
                                if (imagePaths.size > 1) "--- Page ${index + 1} ---\n\n$text" else text
                            } else {
                                null
                            }
                        }.joinToString("\n\n").trim()
                    } finally {
                        try {
                            recognizer.close()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error closing recognizer", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "OCR failed", e)
                ""
            }
        } ?: ""
    }

    private suspend fun extractTextFromImage(
        context: Context,
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        path: String
    ): String {
        return try {
            val file = File(path)
            if (!file.exists()) return ""

            var processedBitmap: Bitmap? = null
            var visionText: Text? = null

            try {
                processedBitmap = OcrImagePreprocessor.loadOptimizedBitmapForOcr(file)
                if (processedBitmap != null) {
                    val inputImage = InputImage.fromBitmap(processedBitmap, 0)
                    visionText = processImage(recognizer, inputImage)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Preprocessed recognition attempt failed for $path, falling back to raw file", e)
            } finally {
                processedBitmap?.recycle()
            }

            if (visionText == null || visionText.text.isBlank()) {
                val rawImage = InputImage.fromFilePath(context, Uri.fromFile(file))
                visionText = processImage(recognizer, rawImage)
            }

            if (visionText != null && visionText.text.isNotBlank()) {
                formatRecognizedText(visionText)
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from image $path", e)
            ""
        }
    }

    /**
     * Reconstructs text block-by-block and line-by-line to preserve layout structure
     * (e.g. invoice items, totals, addresses) and post-processes punctuation (such as Bengali Dari/Danda '।').
     */
    private fun formatRecognizedText(visionText: Text): String {
        val pageSb = StringBuilder()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineText = cleanLineText(line.text)
                if (lineText.isNotBlank()) {
                    pageSb.append(lineText).append("\n")
                }
            }
            pageSb.append("\n")
        }
        return pageSb.toString().trim()
    }

    /**
     * Cleans character artifacts while preserving both English and Bengali Unicode code points.
     * Keeps Latin (A-Z, a-z), Bengali (U+0980 to U+09FF, including vowels, matras, digits 0-9, danda),
     * common numerals, currency symbols (BDT, USD, EUR, GBP), and punctuation.
     */
    private fun cleanLineText(rawText: String): String {
        return rawText
            .replace("\\r\\n".toRegex(), "\n")
            .replace("\r", "\n")
            // Normalize multiple consecutive spaces into a single space
            .replace(" {2,}".toRegex(), " ")
            .trim()
    }

    private suspend fun processImage(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        image: InputImage
    ): Text? = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) {
                    Log.e(TAG, "Process image failed", exception)
                    continuation.resume(null)
                }
            }
    }
}
