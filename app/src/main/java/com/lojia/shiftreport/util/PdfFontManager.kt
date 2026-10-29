package com.lojia.shiftreport.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

/**
 * Manages embedded TrueType fonts (Noto Sans Bengali, Noto Naskh Arabic, and Poppins)
 * for native Android PdfDocument rendering, completely eliminating '????' question mark artifacts.
 */
object PdfFontManager {

    private const val TAG = "PdfFontManager"

    private var bengaliRegular: Typeface? = null
    private var bengaliBold: Typeface? = null
    private var arabicRegular: Typeface? = null
    private var arabicBold: Typeface? = null
    private var defaultRegular: Typeface? = null
    private var defaultBold: Typeface? = null

    /**
     * Initializes and caches typefaces from application assets.
     */
    fun init(context: Context) {
        if (bengaliRegular != null && arabicRegular != null) return

        bengaliRegular = loadAssetFont(context, "fonts/NotoSansBengali-Regular.ttf")
            ?: loadAssetFont(context, "fonts/noto_sans_bengali.ttf")
            ?: Typeface.create("sans-serif", Typeface.NORMAL)

        bengaliBold = bengaliRegular?.let { Typeface.create(it, Typeface.BOLD) }
            ?: Typeface.create("sans-serif", Typeface.BOLD)

        arabicRegular = loadAssetFont(context, "fonts/NotoNaskhArabic-Regular.ttf")
            ?: loadAssetFont(context, "fonts/noto_naskh_arabic.ttf")
            ?: Typeface.create("sans-serif", Typeface.NORMAL)

        arabicBold = arabicRegular?.let { Typeface.create(it, Typeface.BOLD) }
            ?: Typeface.create("sans-serif", Typeface.BOLD)

        defaultRegular = try {
            Typeface.createFromAsset(context.assets, "font/poppins.ttf")
        } catch (e: Exception) {
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        defaultBold = defaultRegular?.let { Typeface.create(it, Typeface.BOLD) }
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private fun loadAssetFont(context: Context, path: String): Typeface? {
        return try {
            Typeface.createFromAsset(context.assets, path)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Asset font not found at $path, trying cached file...")
            null
        }
    }

    fun isBengali(text: String): Boolean {
        for (i in 0 until text.length) {
            val code = text[i].code
            if (code in 0x0980..0x09FF) return true
        }
        return false
    }

    fun isArabic(text: String): Boolean {
        for (i in 0 until text.length) {
            val code = text[i].code
            if (code in 0x0600..0x06FF ||
                code in 0x0750..0x077F ||
                code in 0x08A0..0x08FF ||
                code in 0xFB50..0xFDFF ||
                code in 0xFE70..0xFEFF
            ) return true
        }
        return false
    }

    /**
     * Resolves the appropriate typeface based on the text contents (Bengali, Arabic, or Latin).
     */
    fun getTypefaceForText(context: Context, text: String, isBold: Boolean = false): Typeface {
        init(context)
        return when {
            isBengali(text) -> if (isBold) bengaliBold ?: Typeface.DEFAULT_BOLD else bengaliRegular ?: Typeface.DEFAULT
            isArabic(text) -> if (isBold) arabicBold ?: Typeface.DEFAULT_BOLD else arabicRegular ?: Typeface.DEFAULT
            else -> if (isBold) defaultBold ?: Typeface.DEFAULT_BOLD else defaultRegular ?: Typeface.DEFAULT
        }
    }

    /**
     * Configures a Paint instance with the optimal typeface for the specified text.
     */
    fun applyTypeface(paint: Paint, context: Context, text: String, isBold: Boolean = false) {
        paint.typeface = getTypefaceForText(context, text, isBold)
    }
}
