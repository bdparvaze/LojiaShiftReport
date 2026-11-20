package com.lojia.shiftreport.ui.common

/**
 * Pure Kotlin state holder for the top-bar navigation menu/drawer lifecycle.
 * Guarantees idempotent open transitions and consistent dismissal across
 * outside taps, back presses, and menu item selection.
 */
class TopMenuStateHolder(initialOpen: Boolean = false) {
    var isOpen: Boolean = initialOpen
        private set

    fun open(): Boolean {
        if (isOpen) return false
        isOpen = true
        return true
    }

    fun onDismissOutsideTap(): Boolean = dismiss()

    fun onDismissBackPress(): Boolean = dismiss()

    fun onSelectItem(onSelected: (() -> Unit)? = null): Boolean {
        val changed = dismiss()
        onSelected?.invoke()
        return changed
    }

    fun syncExternalState(open: Boolean) {
        isOpen = open
    }

    fun dismiss(): Boolean {
        if (!isOpen) return false
        isOpen = false
        return true
    }
}
