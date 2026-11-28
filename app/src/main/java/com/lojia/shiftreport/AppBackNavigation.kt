package com.lojia.shiftreport

enum class SettingsEntry {
    FromSettingsRoot,
    FromDrawer
}

enum class BackTarget {
    CloseOverlays,
    GoToSettingsRoot,
    PopBackStack,
    GoHome,
    NotHandled
}

fun resolveBackTarget(
    isDrawerOrMenuOpen: Boolean,
    navState: AppNavState,
    settingsEntry: SettingsEntry,
    selectedSettingsMenu: String?,
    backStackIsEmpty: Boolean
): BackTarget {
    if (isDrawerOrMenuOpen) return BackTarget.CloseOverlays

    return when (navState) {
        is AppNavState.ShiftReportState.SettingsDetail -> {
            val activeMenu = selectedSettingsMenu?.takeIf { it.isNotBlank() } ?: navState.section
            val isSettingsRoot = activeMenu == "root" || activeMenu == "all" || activeMenu == "overview"
            if (!isSettingsRoot && settingsEntry == SettingsEntry.FromSettingsRoot) {
                BackTarget.GoToSettingsRoot
            } else if (!backStackIsEmpty) {
                BackTarget.PopBackStack
            } else {
                BackTarget.GoHome
            }
        }
        is AppNavState.ShiftReportState.Home -> {
            if (!backStackIsEmpty) BackTarget.PopBackStack else BackTarget.NotHandled
        }
        else -> {
            if (!backStackIsEmpty) BackTarget.PopBackStack else BackTarget.GoHome
        }
    }
}
