package com.lojia.pos.pos

import com.lojia.pos.BuildConfig
import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.pos.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*


import android.app.Application


import androidx.lifecycle.AndroidViewModel

import com.lojia.pos.util.UiText



import androidx.lifecycle.viewModelScope


import com.lojia.pos.util.NotificationHelper


import kotlinx.coroutines.flow.*


import kotlinx.coroutines.launch

import java.text.SimpleDateFormat

import java.util.*

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val db = AppDatabase.getInstance(application)
    private val posDao = db.posDao()
    private val reportDao = db.reportDao()
    val syncManager = ConfigurationSyncManager.getInstance(application)
    val printerManager = com.lojia.pos.printer.BluetoothPrinterManager(application)

    val currentLanguage: StateFlow<AppLanguage> = syncManager.currentLanguage
    val currentCountry: StateFlow<AppCountry> = syncManager.currentCountry
    val cashiers: StateFlow<List<Cashier>> = syncManager.cashiers
    val activeCashier: StateFlow<String> = syncManager.activeCashier

    fun setActiveCashier(name: String) {
        syncManager.setActiveCashier(name)
    }

    fun setLanguage(language: AppLanguage) {
        syncManager.setLanguage(language)
    }

    fun setCountry(country: AppCountry) {
        syncManager.setCountry(country)
    }

    val categories: StateFlow<List<POSCategory>> = posDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<POSProduct>> = posDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modifiers: StateFlow<List<POSModifier>> = posDao.getAllModifiers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discounts: StateFlow<List<POSDiscount>> = posDao.getAllDiscounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesHistory: StateFlow<List<POSSale>> = posDao.getAllSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockAdjustments: StateFlow<List<POSStockAdjustment>> = posDao.getAllStockAdjustments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receiptConfig: StateFlow<ShopReceiptConfig?> = reportDao.getReceiptConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val businessProfile: StateFlow<BusinessProfile?> = reportDao.getBusinessProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // POS Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // Open Tickets State
    private val _openTickets = MutableStateFlow<List<OpenTicket>>(emptyList())
    val openTickets: StateFlow<List<OpenTicket>> = _openTickets.asStateFlow()

    var currentTicketName = MutableStateFlow<com.lojia.pos.util.UiText>(com.lojia.pos.util.UiText.StringResource(R.string.ticket_1))
    var selectedCustomerName = MutableStateFlow<com.lojia.pos.util.UiText>(com.lojia.pos.util.UiText.StringResource(R.string.walk_in_customer))

    var searchQuery = MutableStateFlow("")
    var selectedCategoryId = MutableStateFlow<Int?>(null)

    // Applied Order Discount
    var appliedDiscount = MutableStateFlow<POSDiscount?>(null)

    // Dialog & Sheet Visibility States
    val showCustomerDialog = MutableStateFlow(false)
    val showOptionsMenu = MutableStateFlow(false)
    val showTicketSheet = MutableStateFlow(false)
    val showOpenTicketsDialog = MutableStateFlow(false)
    val showCheckoutModal = MutableStateFlow(false)

    private val _uiToast = MutableSharedFlow<com.lojia.pos.util.UiText>()
    val uiToast = _uiToast.asSharedFlow()

    init {
        viewModelScope.launch {
            // Seed screenshot items in DEBUG mode if not already present
            if (BuildConfig.DEBUG) {
                val existing = posDao.getAllProductsOnce()
                if (existing.none { it.name.contains("تغيير راس") }) {
                    val catId = posDao.insertCategory(POSCategory(name = "شيشة و رؤوس", iconName = "SmokeFree", colorHex = "#4CAF50")).toInt()
                    val screenshotItems = listOf(
                        "تغيير راس بلوبيري" to "101",
                        "تغيير راس تفاحتين فاخر" to "102",
                        "تغيير راس علك مستكا" to "103",
                        "تغيير راس ليمون نعناع" to "104",
                        "تغيير راس مكس" to "105",
                        "تغيير راس عنب ساده" to "106",
                        "تغيير راس عنب توت" to "107",
                        "تغيير راس عنب نعناع" to "108",
                        "تغيير راس بطيخ نعناع" to "109",
                        "تغيير راس تفاحتين نخلة" to "110"
                    )
                    screenshotItems.forEach { (name, code) ->
                        posDao.insertProduct(
                            POSProduct(
                                name = name,
                                categoryId = catId,
                                price = 25.0,
                                costPrice = 5.0,
                                stockQuantity = 500.0,
                                minStockAlert = 20.0,
                                barcode = code,
                                unit = "head"
                            )
                        )
                    }
                }
            }
        }

        // Check for low stock items on startup and alert if necessary
        viewModelScope.launch {
            products.collect { prods ->
                val lowStock = prods.filter { it.stockQuantity <= it.minStockAlert }
                if (lowStock.isNotEmpty()) {
                    val first = lowStock.first()
                    // Notification helper ready
                }
            }
        }
    }

    fun saveCurrentTicket(customName: String? = null) {
        val items = _cartItems.value
        if (items.isEmpty()) return
        val name = if (!customName.isNullOrBlank()) customName else "Ticket #${_openTickets.value.size + 1}"
        val ticket = OpenTicket(
            name = name,
            items = items,
            customerName = selectedCustomerName.value.asString(context)
        )
        _openTickets.value = _openTickets.value + ticket
        clearCart()
        currentTicketName.value = com.lojia.pos.util.UiText.StringResource(R.string.ticket_prefix, _openTickets.value.size + 1)
    }

    fun loadTicket(ticket: OpenTicket) {
        _cartItems.value = ticket.items
        currentTicketName.value = com.lojia.pos.util.UiText.DynamicString(ticket.name)
        selectedCustomerName.value = com.lojia.pos.util.UiText.DynamicString(ticket.customerName)
        _openTickets.value = _openTickets.value.filter { it.id != ticket.id }
    }

    fun deleteOpenTicket(ticketId: String) {
        _openTickets.value = _openTickets.value.filter { it.id != ticketId }
    }

    val filteredProducts = combine(products, searchQuery, selectedCategoryId) { prods, query, catId ->
        prods.filter { prod ->
            val matchQuery = query.isBlank() || prod.name.contains(query, ignoreCase = true) || prod.barcode.contains(query)
            val matchCat = catId == null || prod.categoryId == catId
            matchQuery && matchCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartSubtotal: StateFlow<Double> = combine(_cartItems, appliedDiscount, businessProfile) { items, discount, profile ->
        val rawSubtotal = items.sumOf { it.lineTotal }
        val discounted = if (discount != null) {
            if (discount.isPercentage) {
                rawSubtotal * (1.0 - (discount.percentage / 100.0)).coerceAtLeast(0.0)
            } else {
                (rawSubtotal - discount.fixedAmount).coerceAtLeast(0.0)
            }
        } else {
            rawSubtotal
        }
        val taxEnabled = profile?.isTaxEnabled ?: false
        val taxRate = profile?.vatRate ?: 0.0
        val taxInclusive = profile?.isTaxIncluded ?: true

        val sub = if (taxEnabled && taxRate > 0.0 && taxInclusive) {
            discounted / (1.0 + (taxRate / 100.0))
        } else {
            discounted
        }
        Math.round(sub * 100.0) / 100.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartVat: StateFlow<Double> = combine(_cartItems, appliedDiscount, businessProfile) { items, discount, profile ->
        val rawSubtotal = items.sumOf { it.lineTotal }
        val discounted = if (discount != null) {
            if (discount.isPercentage) {
                rawSubtotal * (1.0 - (discount.percentage / 100.0)).coerceAtLeast(0.0)
            } else {
                (rawSubtotal - discount.fixedAmount).coerceAtLeast(0.0)
            }
        } else {
            rawSubtotal
        }
        val taxEnabled = profile?.isTaxEnabled ?: false
        val taxRate = profile?.vatRate ?: 0.0
        val taxInclusive = profile?.isTaxIncluded ?: true

        val vat = if (taxEnabled && taxRate > 0.0) {
            if (taxInclusive) {
                discounted - (discounted / (1.0 + (taxRate / 100.0)))
            } else {
                discounted * (taxRate / 100.0)
            }
        } else {
            0.0
        }
        Math.round(vat * 100.0) / 100.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotal: StateFlow<Double> = combine(_cartItems, appliedDiscount, businessProfile) { items, discount, profile ->
        val rawSubtotal = items.sumOf { it.lineTotal }
        val discounted = if (discount != null) {
            if (discount.isPercentage) {
                rawSubtotal * (1.0 - (discount.percentage / 100.0)).coerceAtLeast(0.0)
            } else {
                (rawSubtotal - discount.fixedAmount).coerceAtLeast(0.0)
            }
        } else {
            rawSubtotal
        }
        val taxEnabled = profile?.isTaxEnabled ?: false
        val taxRate = profile?.vatRate ?: 0.0
        val taxInclusive = profile?.isTaxIncluded ?: true

        val total = if (taxEnabled && taxRate > 0.0) {
            if (taxInclusive) {
                discounted
            } else {
                discounted + (discounted * (taxRate / 100.0))
            }
        } else {
            discounted
        }
        Math.round(total * 100.0) / 100.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _completedSale = MutableStateFlow<POSSale?>(null)
    val completedSale = _completedSale.asStateFlow()

    fun addToCart(product: POSProduct, selectedModifiers: List<POSModifier> = emptyList()) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id && it.selectedModifiers == selectedModifiers }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1.0)
        } else {
            current.add(CartItem(product = product, quantity = 1.0, selectedModifiers = selectedModifiers))
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(productId: Int, quantity: Double) {
        if (quantity <= 0) {
            removeFromCart(productId)
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(quantity = quantity)
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Int) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        appliedDiscount.value = null
    }

    fun scanBarcode(barcode: String) {
        viewModelScope.launch {
            val prod = posDao.getProductByBarcode(barcode.trim())
            if (prod != null) {
                addToCart(prod)
                _uiToast.emit(UiText.StringResource(R.string.toast_added_to_cart_barcode, prod.name))
            } else {
                _uiToast.emit(UiText.StringResource(R.string.toast_no_product_barcode, barcode))
            }
        }
    }

    fun checkout(paymentMethod: String, cashierName: String = "", onComplete: (POSSale) -> Unit) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        val effectiveCashier = if (cashierName.isNotBlank() && cashierName != "Cashier") {
            cashierName
        } else {
            activeCashier.value.ifBlank { "Lojia Manager" }
        }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                val invoiceNo = "INV-" + formatter.format(Date(now))

                val sub = cartSubtotal.value
                val vat = cartVat.value
                val total = cartTotal.value

                val sale = POSSale(
                    invoiceNumber = invoiceNo,
                    cashierName = effectiveCashier,
                    customerName = selectedCustomerName.value.asString(context),
                    subtotal = sub,
                    vatAmount = vat,
                    totalAmount = total,
                    paymentMethod = paymentMethod,
                    timestamp = now
                )

                val saleItems = items.map { item ->
                    POSSaleItem(
                        saleId = 0,
                        productId = item.product.id,
                        productName = item.product.name,
                        quantity = item.quantity,
                        unitPrice = item.product.price,
                        totalPrice = Math.round(item.lineTotal * 100.0) / 100.0
                    )
                }

                // Atomic transaction in Room database
                val saleId = posDao.processCheckoutTransaction(sale, saleItems)

                // Check low stock alert after deduction
                for (item in items) {
                    val updatedProd = posDao.getProductById(item.product.id)
                    if (updatedProd != null && updatedProd.stockQuantity <= updatedProd.minStockAlert) {
                        NotificationHelper.sendLowStockNotification(
                            context,
                            updatedProd.name,
                            updatedProd.stockQuantity,
                            updatedProd.minStockAlert,
                            updatedProd.unit
                        )
                    }
                }

                val curr = businessProfile.value?.currency ?: "USD"
                // Audit
                reportDao.insertAuditLog(
                    AuditLog(
                        username = effectiveCashier,
                        action = "POS_SALE",
                        details = "Processed $invoiceNo ($paymentMethod) for ${MoneyFormat.format(total, curr)}"
                    )
                )

                val finalizedSale = sale.copy(id = saleId.toInt())
                _completedSale.value = finalizedSale
                _uiToast.emit(UiText.StringResource(R.string.payment_success))

                // Optional Bluetooth thermal receipt print (never blocks sales if printer fails or unconfigured)
                printReceiptForSale(finalizedSale, items)

                clearCart()
                InventoryCheckScheduler.triggerImmediateCheck(context)
                onComplete(finalizedSale)
            } catch (e: Exception) {
                _uiToast.emit(UiText.DynamicString("Checkout failed: ${e.localizedMessage ?: "Database error"}"))
            }
        }
    }

    /**
     * Prints sale receipt to configured ESC/POS Bluetooth printer.
     * Silent failure model — POS checkout is never blocked or failed if printer is disconnected.
     */
    fun printReceiptForSale(sale: POSSale, items: List<CartItem>) {
        if (printerManager.getSavedPrinterAddress().isBlank()) return

        viewModelScope.launch {
            try {
                val biz = businessProfile.value ?: BusinessProfile()
                val rc = receiptConfig.value ?: ShopReceiptConfig()
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val dateStr = formatter.format(Date(sale.timestamp))
                val curr = biz.currency.ifBlank { "$" }

                val receiptItems: List<Pair<String, Pair<Double, Double>>> = items.map { item ->
                    Pair(item.product.name, Pair(item.quantity, item.lineTotal))
                }

                val formattedText = printerManager.buildReceiptText(
                    businessName = biz.businessName.ifBlank { "Lojia Store" },
                    businessAddress = biz.address,
                    businessPhone = biz.phone,
                    vatNumber = biz.vatNumber,
                    customHeader = rc.customHeader,
                    customFooterText = rc.customFooterText,
                    showTaxNumber = rc.showTaxNumber,
                    showCashierName = rc.showCashierName,
                    receiptId = sale.invoiceNumber,
                    dateTimeStr = dateStr,
                    cashierName = sale.cashierName,
                    customerName = sale.customerName,
                    items = receiptItems,
                    subtotal = sale.subtotal,
                    discount = 0.0,
                    tax = sale.vatAmount,
                    grandTotal = sale.totalAmount,
                    paymentMethod = sale.paymentMethod,
                    currencySymbol = curr
                )

                printerManager.printFormattedText(formattedText)
            } catch (e: Exception) {
                // Log and swallow error — printer failures must never block POS sales
                android.util.Log.e("PosViewModel", "Optional printer output failed", e)
            }
        }
    }

    fun voidSale(
        saleId: Int,
        reason: String,
        performedBy: String = "",
        isAdminUser: Boolean = false,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val userProfile = reportDao.getUserProfileOnce()
                val role = userProfile?.currentRole ?: "ADMIN"
                val isAdmin = isAdminUser || role == "ADMIN"

                if (!isAdmin) {
                    _uiToast.emit(UiText.DynamicString("Permission denied: Cashiers cannot void sales. Admin authorization required."))
                    onComplete(false)
                    return@launch
                }

                val effectiveUser = if (performedBy.isNotBlank()) performedBy else userProfile?.fullName?.ifBlank { "Admin" } ?: activeCashier.value.ifBlank { "Admin" }
                val success = posDao.processVoidSaleTransaction(
                    saleId = saleId,
                    username = effectiveUser,
                    reason = reason
                )
                if (success) {
                    _uiToast.emit(UiText.DynamicString("Sale #$saleId voided and stock restored successfully."))
                } else {
                    _uiToast.emit(UiText.DynamicString("Could not void sale #$saleId (may already be voided)."))
                }
                onComplete(success)
            } catch (e: Exception) {
                _uiToast.emit(UiText.DynamicString("Void operation failed: ${e.localizedMessage ?: "Unknown error"}"))
                onComplete(false)
            }
        }
    }

    fun addOrUpdateProduct(product: POSProduct) {
        viewModelScope.launch {
            if (product.id == 0) {
                posDao.insertProduct(product)
                _uiToast.emit(UiText.StringResource(R.string.toast_product_created, product.name))
            } else {
                posDao.updateProduct(product)
                _uiToast.emit(UiText.StringResource(R.string.toast_product_updated, product.name))
            }
        }
    }

    fun deleteProduct(product: POSProduct) {
        viewModelScope.launch {
            posDao.deleteProduct(product)
            _uiToast.emit(UiText.StringResource(R.string.toast_product_deleted))
        }
    }

    fun adjustStock(productId: Int, productName: String, quantityChange: Double, reason: String, username: String) {
        viewModelScope.launch {
            posDao.updateStock(productId, quantityChange)
            posDao.insertStockAdjustment(
                POSStockAdjustment(
                    productId = productId,
                    productName = productName,
                    quantityChange = quantityChange,
                    type = if (quantityChange >= 0) "RESTOCK" else "DEDUCTION",
                    reason = reason,
                    username = username
                )
            )
            InventoryCheckScheduler.triggerImmediateCheck(context)
            _uiToast.emit(
                UiText.StringResource(
                    R.string.toast_stock_adjusted,
                    productName,
                    if (quantityChange >= 0) "+$quantityChange" else "$quantityChange"
                )
            )
        }
    }

    fun addCategory(name: String, icon: String = "Category", color: String = "#6366F1") {
        if (name.isBlank()) return
        viewModelScope.launch {
            posDao.insertCategory(POSCategory(name = name.trim(), iconName = icon, colorHex = color))
            _uiToast.emit(UiText.StringResource(R.string.toast_category_created, name))
        }
    }

    fun deleteCategory(category: POSCategory) {
        viewModelScope.launch {
            posDao.deleteCategory(category)
        }
    }

    // Modifiers & Discounts
    fun addModifier(name: String, group: String, extraPrice: Double) {
        if (name.isBlank()) return
        viewModelScope.launch {
            posDao.insertModifier(POSModifier(name = name.trim(), optionGroup = group.trim(), extraPrice = extraPrice))
            _uiToast.emit(UiText.StringResource(R.string.toast_modifier_added, name))
        }
    }

    fun deleteModifier(modifier: POSModifier) {
        viewModelScope.launch {
            posDao.deleteModifier(modifier)
            _uiToast.emit(UiText.StringResource(R.string.toast_modifier_removed))
        }
    }

    fun addDiscount(name: String, percentage: Double, fixedAmount: Double, isPercentage: Boolean, code: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            posDao.insertDiscount(
                POSDiscount(
                    name = name.trim(),
                    percentage = percentage,
                    fixedAmount = fixedAmount,
                    isPercentage = isPercentage,
                    code = code.trim().uppercase()
                )
            )
            _uiToast.emit(UiText.StringResource(R.string.toast_discount_added, name))
        }
    }

    fun deleteDiscount(discount: POSDiscount) {
        viewModelScope.launch {
            posDao.deleteDiscount(discount)
            _uiToast.emit(UiText.StringResource(R.string.toast_discount_removed))
        }
    }

    fun saveReceiptConfig(config: ShopReceiptConfig) {
        viewModelScope.launch {
            reportDao.saveReceiptConfig(config)
            _uiToast.emit(UiText.StringResource(R.string.toast_receipt_customization_saved))
        }
    }
}
