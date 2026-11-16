package com.lojia.shiftreport.util

/**
 * Normalizes a business VAT number for PDF/receipt printing.
 * Returns null when missing or blank after trimming so callers omit the VAT line.
 */
fun vatNumberForPrint(raw: String?): String? {
    val trimmed = raw?.trim() ?: return null
    return trimmed.ifEmpty { null }
}

/**
 * Returns a single formatted header line for the VAT registration number, or emptyList()
 * when missing or blank so callers print nothing at all (no label, placeholder, or fallback).
 */
fun vatHeaderLines(vat: String?): List<String> {
    val clean = vatNumberForPrint(vat) ?: return emptyList()
    return listOf(clean)
}

const val RECEIPT_UNSPECIFIED = "Unspecified"

fun isUnspecifiedReceipt(normalizedReceipt: String): Boolean {
    return normalizedReceipt == RECEIPT_UNSPECIFIED
}

/**
 * Normalizes a due receipt number by trimming and stripping any leading "Receipt"
 * word (case-insensitive) and leading "#" characters (with whitespace around them).
 * Never strips "#" characters from the middle of a reference.
 */
fun normalizeReceiptNumber(raw: String?): String {
    var s = raw?.trim() ?: return ""
    while (true) {
        val next = s
            .replaceFirst(Regex("^receipt\\b\\s*", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^[#\\s]+"), "")
            .trim()
        if (next == s) break
        s = next
    }
    return s
}

/**
 * Pure selector for the PDF summary header VAT/telephone metadata line.
 */
enum class HeaderMetaLabelKind {
    VAT_AND_TEL,
    VAT_ONLY,
    TEL_ONLY,
    NONE
}

fun chooseHeaderMetaLabelKind(rawVat: String?, rawPhone: String?): HeaderMetaLabelKind {
    val hasVat = vatNumberForPrint(rawVat) != null
    val hasPhone = !rawPhone.isNullOrBlank()
    return when {
        hasVat && hasPhone -> HeaderMetaLabelKind.VAT_AND_TEL
        hasVat -> HeaderMetaLabelKind.VAT_ONLY
        hasPhone -> HeaderMetaLabelKind.TEL_ONLY
        else -> HeaderMetaLabelKind.NONE
    }
}

/**
 * Pure guard for demo seed insertion: demo data may only be seeded in debug builds
 * when the database has no existing records.
 */
fun shouldSeedDemoData(isDebugBuild: Boolean, databaseIsEmpty: Boolean): Boolean {
    return isDebugBuild && databaseIsEmpty
}

/**
 * Pure width-in-dots mapping for ESC/POS thermal printers (203 dpi).
 * 80 mm -> 576 dots; 58 mm, null, or any other value -> 384 dots.
 */
fun paperWidthDots(paperMm: Int?): Int = when (paperMm) {
    80 -> 576
    58 -> 384
    else -> 384
}

