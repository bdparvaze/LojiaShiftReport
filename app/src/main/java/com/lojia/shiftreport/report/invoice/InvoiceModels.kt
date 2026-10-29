package com.lojia.shiftreport.report.invoice

/**
 * Paper sizes supported by Foodics POS style invoice engine.
 */
enum class InvoicePaperSize(
    val widthPt: Int,
    val heightPt: Int,
    val isThermal: Boolean
) {
    THERMAL_58MM(widthPt = 164, heightPt = 600, isThermal = true),
    THERMAL_80MM(widthPt = 226, heightPt = 800, isThermal = true),
    A4(widthPt = 595, heightPt = 842, isThermal = false)
}

/**
 * Line item in POS invoice / receipt.
 */
data class InvoiceItem(
    val no: Int,
    val description: String,
    val descriptionArabic: String = "",
    val descriptionBengali: String = "",
    val qty: Double,
    val unitPrice: Double,
    val total: Double,
    val vatRate: Double = 0.15
)

/**
 * Core financial & cash reconciliation breakdown for Lojia Shift Report invoice.
 */
data class PaymentSummary(
    val grossCash: Double,
    val madaCard: Double,
    val digitalWallet: Double,
    val expenses: Double,
    val staffMeal: Double,
    val muassel: Double,
    val variance: Double,
    val netSales: Double,
    val vatRate: Double = 0.15,
    val vatAmount: Double = netSales * vatRate,
    val totalPayable: Double = netSales + vatAmount
)

/**
 * Complete multi-currency, multilingual POS Invoice data container.
 */
data class InvoiceData(
    val storeName: String,
    val storeNameArabic: String = "",
    val storeNameBengali: String = "",
    val vatNo: String,
    val invoiceNo: String,
    val date: String,
    val cashier: String,
    val storeAddress: String = "",
    val logoUri: String? = null,
    val shiftId: String = "",
    val currencyCode: String = "SAR", // SAR, BDT, USD
    val currencySymbol: String = "SAR",
    val items: List<InvoiceItem> = emptyList(),
    val payment: PaymentSummary,
    val qrData: String = "",
    val footerNoteEn: String = "Thank you for your business!",
    val footerNoteAr: String = "شكراً لزيارتكم!",
    val footerNoteBn: String = "আমাদের সাথে থাকার জন্য ধন্যবাদ!",
    val paperSize: InvoicePaperSize = InvoicePaperSize.A4
)
