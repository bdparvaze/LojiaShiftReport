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
import java.io.File
import kotlin.coroutines.resume

object OcrTextExtractor {

    private const val TAG = "OcrTextExtractor"

    /**
     * Extracts text from a list of scanned page image files.
     * Uses [OcrImagePreprocessor] to enhance contrast for delicate scripts (such as Bengali/Bangla)
     * and structures output cleanly across multi-page scans.
     */
    suspend fun extractTextFromImages(context: Context, imagePaths: List<String>): String = withContext(Dispatchers.IO) {
        if (imagePaths.isEmpty()) return@withContext ""

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val sb = StringBuilder()

        for ((index, path) in imagePaths.withIndex()) {
            try {
                val file = File(path)
                if (!file.exists()) continue

                // 1. First try with contrast-enhanced Bitmap for maximum character edge clarity
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

                // 2. Fallback to raw file InputImage if bitmap processing yielded empty result
                if (visionText == null || visionText.text.isBlank()) {
                    val rawImage = InputImage.fromFilePath(context, Uri.fromFile(file))
                    visionText = processImage(recognizer, rawImage)
                }

                if (visionText != null && visionText.text.isNotBlank()) {
                    if (imagePaths.size > 1) {
                        sb.append("--- Page ${index + 1} ---\n\n")
                    }
                    val formattedPageText = formatRecognizedText(visionText)
                    sb.append(formattedPageText).append("\n\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting text from image $path", e)
            }
        }

        try {
            recognizer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing recognizer", e)
        }

        return@withContext sb.toString().trim()
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
     * common numerals, currency symbols (৳, $, €, £), and punctuation.
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
