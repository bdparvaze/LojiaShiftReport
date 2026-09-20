package com.lojia.pos.pos

import com.lojia.pos.ui.common.LojiaTextField
import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.pos.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*



import androidx.compose.foundation.background


import androidx.compose.foundation.border


import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn


import androidx.compose.foundation.lazy.items


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.filled.*


import androidx.compose.material.icons.outlined.*


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.draw.clip


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.platform.testTag


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.text.input.KeyboardType


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp

import com.lojia.pos.data.AppLanguage

import com.lojia.pos.data.POSProduct



import androidx.compose.ui.res.stringResource
import com.lojia.pos.ui.common.rememberTranslatedString


@Composable
fun InventoryScreen(
    posViewModel: PosViewModel,
    reportViewModel: ReportViewModel,
    language: AppLanguage
) {
    val products by posViewModel.products.collectAsState()
    val categories by posViewModel.categories.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()

        val rawCurrency = businessProfile?.currency ?: "SAR"
    val currency = if (rawCurrency == "SAR") stringResource(R.string.currency_unit) else rawCurrency

    var showAddDialog by remember { mutableStateOf(false) }
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var inventorySearchQuery by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<POSProduct?>(null) }
    var adjustingProduct by remember { mutableStateOf<POSProduct?>(null) }
    var scannedBarcodeForNewItem by remember { mutableStateOf("") }

    val displayedProducts = remember(products, inventorySearchQuery) {
        if (inventorySearchQuery.isBlank()) products
        else products.filter {
            it.name.contains(inventorySearchQuery, ignoreCase = true) ||
            it.barcode.contains(inventorySearchQuery, ignoreCase = true) ||
            it.id.toString() == inventorySearchQuery.trim()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddDialog = true
                },
                containerColor = PrimaryIndigo,
                contentColor = PureWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_product))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(
                    title = stringResource(R.string.inventory_title),
                    icon = Icons.Default.Inventory2
                )
            }

            // Search and Barcode Scan Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LojiaTextField(
                        value = inventorySearchQuery,
                        onValueChange = { inventorySearchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_products)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryIndigo) },
                        trailingIcon = {
                            if (inventorySearchQuery.isNotBlank()) {
                                IconButton(onClick = { inventorySearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_text))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_search_input")
                    )

                    FilledIconButton(
                        onClick = { showBarcodeScannerDialog = true },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("inventory_scan_barcode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.barcode_scanner),
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Quick stock summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = stringResource(R.string.total_skus),
                        value = "${products.size}",
                        icon = Icons.Default.Category,
                        iconColor = PrimaryIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    val lowStockCount = products.count { it.stockQuantity <= it.minStockAlert }
                    MetricStatCard(
                        title = stringResource(R.string.low_stock_warning),
                        value = "$lowStockCount",
                        icon = Icons.Default.WarningAmber,
                        iconColor = if (lowStockCount > 0) AccentRose else AccentEmerald,
                        iconBgColor = if (lowStockCount > 0) AccentRose.copy(alpha = 0.12f) else AccentEmerald.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            items(displayedProducts) { product ->
                InventoryItemRow(
                    product = product,
                    currency = currency,
                    language = language,
                    onEdit = {
                        editingProduct = product
                        showAddDialog = true
                    },
                    onAdjustStock = {
                        adjustingProduct = product
                    },
                    onDelete = {
                        posViewModel.deleteProduct(product)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }
        var price by remember { mutableStateOf(editingProduct?.price?.toString() ?: "") }
        var costPrice by remember { mutableStateOf(editingProduct?.costPrice?.toString() ?: "") }
        var stock by remember { mutableStateOf(editingProduct?.stockQuantity?.toString() ?: "100") }
        var barcode by remember { mutableStateOf(editingProduct?.barcode?.takeIf { it.isNotBlank() } ?: scannedBarcodeForNewItem) }
        var selectedCatId by remember { mutableStateOf(editingProduct?.categoryId ?: (categories.firstOrNull()?.id ?: 0)) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                scannedBarcodeForNewItem = ""
            },
            title = {
                Text(if (editingProduct == null) stringResource(R.string.add_product) else stringResource(R.string.edit_product))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormInputField(
                        value = name,
                        onValueChange = { name = it },
                        label = stringResource(R.string.product_name)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormInputField(
                            value = price,
                            onValueChange = { price = it },
                            label = stringResource(R.string.price),
                            suffixText = currency,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        FormInputField(
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            label = stringResource(R.string.cost_price),
                            suffixText = currency,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormInputField(
                            value = stock,
                            onValueChange = { stock = it },
                            label = stringResource(R.string.stock_quantity),
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        FormInputField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = stringResource(R.string.barcode),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val prod = (editingProduct ?: POSProduct(name = name, categoryId = selectedCatId, price = price.toDoubleOrNull() ?: 0.0)).copy(
                                name = name,
                                price = price.toDoubleOrNull() ?: 0.0,
                                costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                                stockQuantity = stock.toDoubleOrNull() ?: 0.0,
                                barcode = barcode,
                                categoryId = selectedCatId
                            )
                            posViewModel.addOrUpdateProduct(prod)
                            showAddDialog = false
                            scannedBarcodeForNewItem = ""
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    scannedBarcodeForNewItem = ""
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Adjust Stock Dialog
    if (adjustingProduct != null) {
        val prod = adjustingProduct!!
        var adjustAmount by remember { mutableStateOf("") }
        var isAdd by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { adjustingProduct = null },
            title = { Text(stringResource(R.string.adjust_stock_product_fmt, prod.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.current_stock_fmt, prod.stockQuantity.toString(), prod.unit))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isAdd,
                            onClick = { isAdd = true },
                            label = { Text(stringResource(R.string.restock)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isAdd,
                            onClick = { isAdd = false },
                            label = { Text(stringResource(R.string.deduct)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FormInputField(
                        value = adjustAmount,
                        onValueChange = { adjustAmount = it },
                        label = rememberTranslatedString("Quantity"),
                        keyboardType = KeyboardType.Number
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = adjustAmount.toDoubleOrNull() ?: 0.0
                        if (qty > 0) {
                            val delta = if (isAdd) qty else -qty
                            posViewModel.adjustStock(
                                productId = prod.id,
                                productName = prod.name,
                                quantityChange = delta,
                                reason = if (isAdd) "Manual Restock" else "Manual Deduction",
                                username = "Admin"
                            )
                        }
                        adjustingProduct = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { adjustingProduct = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showBarcodeScannerDialog) {
        BarcodeScannerDialog(
            availableProducts = products,
            currentLanguage = language,
            onBarcodeScanned = { scanned ->
                val existingProduct = products.find { it.barcode == scanned || it.id.toString() == scanned }
                if (existingProduct != null) {
                    editingProduct = existingProduct
                    showAddDialog = true
                } else {
                    editingProduct = null
                    scannedBarcodeForNewItem = scanned
                    showAddDialog = true
                }
                showBarcodeScannerDialog = false
            },
            onDismiss = { showBarcodeScannerDialog = false }
        )
    }
}

@Composable
fun InventoryItemRow(
    product: POSProduct,
    currency: String,
    language: AppLanguage,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onDelete: () -> Unit
) {
    SolidCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.price_2f_s).format(product.price, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryIndigo,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.stock_0f_s).format(product.stockQuantity, product.unit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (product.stockQuantity <= product.minStockAlert) AccentRose else TextSecondaryLight,
                        fontWeight = if (product.stockQuantity <= product.minStockAlert) FontWeight.Bold else FontWeight.Normal
                    )
                }
                if (product.barcode.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.barcode_label_fmt, product.barcode),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiaryLight
                    )
                }
            }

            Row {
                IconButton(onClick = onAdjustStock) {
                    Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.adjust_stock), tint = PrimaryIndigo)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_item), tint = TextSecondaryLight)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.delete), tint = AccentRose)
                }
            }
        }
    }
}
