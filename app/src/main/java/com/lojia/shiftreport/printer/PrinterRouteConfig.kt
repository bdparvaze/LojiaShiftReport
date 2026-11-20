package com.lojia.shiftreport.printer

data class DrawerSettingsDestination(
    val sectionKey: String,
    val titleResName: String
)

object PrinterNavigationConfig {
    const val SECTION_BACKUP = "backup"
    const val SECTION_PRINTER = "printer"
    const val SECTION_PRINTER_ALIAS = "thermal_printer"

    val drawerSettingsOrder: List<String> = listOf(
        "cashiers",
        "employees",
        "profile",
        "security",
        "language",
        SECTION_BACKUP,
        SECTION_PRINTER,
        "support",
        "about"
    )

    fun drawerHasPrinterNearBackup(): Boolean {
        val backupIdx = drawerSettingsOrder.indexOf(SECTION_BACKUP)
        val printerIdx = drawerSettingsOrder.indexOf(SECTION_PRINTER)
        return backupIdx >= 0 && printerIdx == backupIdx + 1
    }

    fun resolvePreviewPrinterSettingsTarget(): String = SECTION_PRINTER

    fun isPrinterSection(section: String?): Boolean =
        section == SECTION_PRINTER || section == SECTION_PRINTER_ALIAS
}
