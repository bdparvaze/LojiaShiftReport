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

import androidx.compose.ui.text.style.TextOverflow



import androidx.compose.foundation.Canvas


import androidx.compose.foundation.background


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


import androidx.compose.ui.geometry.Rect


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.graphics.Path


import androidx.compose.ui.graphics.drawscope.Stroke


import androidx.compose.ui.platform.testTag


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.text.style.TextAlign


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp




import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.lojia.pos.util.TranslationRepository
import com.lojia.pos.util.UiText


/**
 * Authentic Loyverse Ticket Shape Badge with perforated notches and active item count.
 */
@Composable
fun LoyverseTicketBadge(
    itemCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 24.dp, height = 20.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidth = 1.6.dp.toPx()
            val cornerRadius = 3.dp.toPx()
            val notchRadius = 3.dp.toPx()
            val path = Path().apply {
                // Top-left corner
                moveTo(cornerRadius, 0f)
                lineTo(w - cornerRadius, 0f)
                // Top-right corner
                arcTo(
                    rect = Rect(w - cornerRadius * 2, 0f, w, cornerRadius * 2),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                // Right edge with center notch
                lineTo(w, h / 2f - notchRadius)
                arcTo(
                    rect = Rect(w - notchRadius, h / 2f - notchRadius, w + notchRadius, h / 2f + notchRadius),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false
                )
                lineTo(w, h - cornerRadius)
                // Bottom-right corner
                arcTo(
                    rect = Rect(w - cornerRadius * 2, h - cornerRadius * 2, w, h),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                // Bottom edge
                lineTo(cornerRadius, h)
                // Bottom-left corner
                arcTo(
                    rect = Rect(0f, h - cornerRadius * 2, cornerRadius * 2, h),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                // Left edge with center notch
                lineTo(0f, h / 2f + notchRadius)
                arcTo(
                    rect = Rect(-notchRadius, h / 2f - notchRadius, notchRadius, h / 2f + notchRadius),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false
                )
                lineTo(0f, cornerRadius)
                // Top-left corner
                arcTo(
                    rect = Rect(0f, 0f, cornerRadius * 2, cornerRadius * 2),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = strokeWidth)
            )
        }
        Text(
            text = "$itemCount",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    posViewModel: PosViewModel,
    reportViewModel: ReportViewModel,
    language: AppLanguage,
    onMenuClick: () -> Unit = {},
    onSaleCompleted: (POSSale) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = true) {
        posViewModel.uiToast.collect { uiText ->
            val resolvedMessage = TranslationRepository.resolve(context, uiText, language.code)
            Toast.makeText(context, resolvedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    val categories by posViewModel.categories.collectAsState()
    val filteredProducts by posViewModel.filteredProducts.collectAsState()
    val cartItems by posViewModel.cartItems.collectAsState()
    val openTickets by posViewModel.openTickets.collectAsState()
    val cartSubtotal by posViewModel.cartSubtotal.collectAsState()
    val cartVat by posViewModel.cartVat.collectAsState()
    val cartTotal by posViewModel.cartTotal.collectAsState()
    val selectedCustomerUiText by posViewModel.selectedCustomerName.collectAsState()
    val currentTicketNameUiText by posViewModel.currentTicketName.collectAsState()
    val selectedCustomer = rememberUiText(selectedCustomerUiText)
    val currentTicketName = rememberUiText(currentTicketNameUiText)
    val activeCashier by posViewModel.activeCashier.collectAsState()
    val cashiers by posViewModel.cashiers.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()

    val searchQuery by posViewModel.searchQuery.collectAsState()
    val selectedCategoryId by posViewModel.selectedCategoryId.collectAsState()

    val showCustomerDialog by posViewModel.showCustomerDialog.collectAsState()
    val showOptionsMenu by posViewModel.showOptionsMenu.collectAsState()
    val showTicketSheet by posViewModel.showTicketSheet.collectAsState()
    val showOpenTicketsDialog by posViewModel.showOpenTicketsDialog.collectAsState()
    val showCheckoutModal by posViewModel.showCheckoutModal.collectAsState()

        val rawCurrency = businessProfile?.currency ?: "SAR"
    val currency = if (rawCurrency == "SAR") rememberTranslatedString(R.string.currency_unit) else rawCurrency

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var showSaveTicketDialog by remember { mutableStateOf(false) }
    var newTicketNameInput by remember { mutableStateOf("") }
    var customerNameInput by remember { mutableStateOf(selectedCustomer) }

    val allItemsLabel = rememberTranslatedString(R.string.all_items)
    val selectedCategoryName = if (selectedCategoryId == null) {
        allItemsLabel
    } else {
        categories.find { it.id == selectedCategoryId }?.name ?: allItemsLabel
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. DUAL ACTION GREEN BUTTON CONTAINER: [ OPEN TICKETS ] | [ CHARGE \n 0.00 SAR ]
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Surface(
                        color = LoyverseHeaderButtonGreen,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Half: OPEN TICKETS
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { posViewModel.showOpenTicketsDialog.value = true }
                                    .testTag("btn_open_tickets"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (openTickets.isNotEmpty()) {
                                    AutoText(
                                        id = R.string.open_tickets_count,
                                        openTickets.size,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    AutoText(
                                        id = R.string.open_tickets,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Thin Vertical Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(Color.White.copy(alpha = 0.35f))
                            )

                            // Right Half: CHARGE \n 0.00 SAR
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        if (cartItems.isNotEmpty()) {
                                            posViewModel.showCheckoutModal.value = true
                                        } else {
                                            posViewModel.showTicketSheet.value = true
                                        }
                                    }
                                    .testTag("btn_charge"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    AutoText(
                                        id = R.string.charge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.msg_2f_s_21).format(cartTotal, currency),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. CATEGORY DROPDOWN & BARCODE / SEARCH ROW
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Category Selector Trigger
                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable { showCategoryDropdown = true }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCategoryName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = LoyverseTextDark
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.cd_select_category),
                                    tint = Color(0xFF616161),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(allItemsLabel, fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = LoyverseTopGreen) },
                                    onClick = {
                                        posViewModel.selectedCategoryId.value = null
                                        showCategoryDropdown = false
                                    }
                                )
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            posViewModel.selectedCategoryId.value = cat.id
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Right: Barcode & Search Icons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showBarcodeScannerDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("pos_barcode_scanner_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = stringResource(R.string.barcode_scanner),
                                    tint = LoyverseTopGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = { showSearchField = !showSearchField },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("pos_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search),
                                    tint = Color(0xFF616161),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Optional Search Field Row
                    if (showSearchField) {
                        LojiaTextField(
                            value = searchQuery,
                            onValueChange = { posViewModel.searchQuery.value = it },
                            placeholder = { Text(stringResource(R.string.search_items_barcode)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LoyverseTopGreen) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    posViewModel.searchQuery.value = ""
                                    showSearchField = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    // Full-width Divider Line under category row
                    HorizontalDivider(
                        color = LoyverseDividerGrey,
                        thickness = 1.dp
                    )
                }
            }

            // 3. PRODUCT ITEMS LIST (MATCHING THE SCREENSHOT EXACTLY)
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.no_items_found),
                            color = TextSecondaryLight
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts) { product ->
                        val inCart = cartItems.find { it.product.id == product.id }
                        LoyverseProductRow(
                            product = product,
                            currency = currency,
                            quantityInCart = inCart?.quantity?.toInt() ?: 0,
                            onItemClick = {
                                posViewModel.addToCart(product)
                            }
                        )
                    }
                }
            }
        }
    }

    // 4. Ticket Details / Cart Sheet
    if (showTicketSheet) {
        ModalBottomSheet(
            onDismissRequest = { posViewModel.showTicketSheet.value = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentTicketName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = stringResource(R.string.customer_label_fmt, selectedCustomer),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                        )
                    }

                    if (cartItems.isNotEmpty()) {
                        TextButton(
                            onClick = { posViewModel.clearCart() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.clear))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = LoyverseDividerGrey)
                Spacer(modifier = Modifier.height(8.dp))

                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.ticket_empty_hint),
                            color = TextSecondaryLight
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        items(cartItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = LoyverseTextDark
                                    )
                                    Text(
                                        text = stringResource(R.string.msg_2f_s_x_0f).format(item.product.price, currency, item.quantity),
                                        fontSize = 13.sp,
                                        color = TextSecondaryLight
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { posViewModel.updateCartQuantity(item.product.id, item.quantity - 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = stringResource(R.string.cd_decrease), tint = LoyverseTopGreen)
                                    }

                                    Text(
                                        text = "${item.quantity.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    IconButton(
                                        onClick = { posViewModel.updateCartQuantity(item.product.id, item.quantity + 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = stringResource(R.string.cd_increase), tint = LoyverseTopGreen)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.msg_2f_s_21).format(item.lineTotal, currency),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = LoyverseGreenDark
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = LoyverseDividerGrey)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Summary
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AutoText(id = R.string.subtotal, color = TextSecondaryLight)
                        Text(stringResource(R.string.msg_2f_s_21).format(cartSubtotal, currency), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AutoText(id = R.string.vat_15, color = TextSecondaryLight)
                        Text(stringResource(R.string.msg_2f_s_21).format(cartVat, currency), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AutoText(id = R.string.total_amount, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(stringResource(R.string.msg_2f_s_21).format(cartTotal, currency), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = LoyverseGreenDark)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions: Save Ticket / Charge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                newTicketNameInput = context.getString(R.string.ticket_number_fmt, openTickets.size + 1)
                                showSaveTicketDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            AutoText(id = R.string.save_ticket)
                        }

                        Button(
                            onClick = {
                                posViewModel.showTicketSheet.value = false
                                posViewModel.showCheckoutModal.value = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LoyverseTopGreen),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            AutoText(id = R.string.charge_now, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // 5. Open Tickets Dialog
    if (showOpenTicketsDialog) {
        AlertDialog(
            onDismissRequest = { posViewModel.showOpenTicketsDialog.value = false },
            title = {
                Text(
                    text = stringResource(R.string.open_tickets_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (openTickets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_open_tickets),
                                color = TextSecondaryLight
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(openTickets) { ticket ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            posViewModel.loadTicket(ticket)
                                            posViewModel.showOpenTicketsDialog.value = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = BgLightGrey)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(ticket.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                stringResource(R.string.items_count_and_customer_fmt, ticket.items.size, ticket.customerName),
                                                fontSize = 12.sp,
                                                color = TextSecondaryLight
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                stringResource(R.string.msg_2f_s_21).format(ticket.totalAmount, currency),
                                                fontWeight = FontWeight.Bold,
                                                color = LoyverseGreenDark
                                            )
                                            IconButton(onClick = { posViewModel.deleteOpenTicket(ticket.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { posViewModel.showOpenTicketsDialog.value = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // 6. Customer Selection / Add Dialog
    if (showCustomerDialog) {
        val walkInLabel = stringResource(R.string.walk_in_customer)
        AlertDialog(
            onDismissRequest = { posViewModel.showCustomerDialog.value = false },
            title = {
                Text(
                    text = stringResource(R.string.assign_customer),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LojiaTextField(
                        value = customerNameInput,
                        onValueChange = { customerNameInput = it },
                        label = { Text(stringResource(R.string.customer_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.quick_select), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(walkInLabel, "Table #1", "Table #2", "VIP Lounge").forEach { quickName ->
                            FilterChip(
                                selected = customerNameInput == quickName,
                                onClick = { customerNameInput = quickName },
                                label = { Text(rememberTranslatedString(quickName), fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanedInput = customerNameInput.ifBlank { walkInLabel }
                        posViewModel.selectedCustomerName.value = if (cleanedInput == walkInLabel) {
                            com.lojia.pos.util.UiText.StringResource(R.string.walk_in_customer)
                        } else {
                            com.lojia.pos.util.UiText.DynamicString(cleanedInput)
                        }
                        posViewModel.showCustomerDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoyverseTopGreen)
                ) {
                    Text(stringResource(R.string.save), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(onClick = { posViewModel.showCustomerDialog.value = false }) {
                    Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        )
    }

    // 7. Save Ticket Dialog
    if (showSaveTicketDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTicketDialog = false },
            title = { Text(stringResource(R.string.save_open_ticket)) },
            text = {
                Column {
                    LojiaTextField(
                        value = newTicketNameInput,
                        onValueChange = { newTicketNameInput = it },
                        label = { Text(stringResource(R.string.ticket_name_table)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        posViewModel.saveCurrentTicket(newTicketNameInput)
                        showSaveTicketDialog = false
                        posViewModel.showTicketSheet.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoyverseTopGreen)
                ) {
                    Text(stringResource(R.string.save), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTicketDialog = false }) {
                    Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        )
    }

    // 8. Options Menu (Dropdown)
    if (showOptionsMenu) {
        DropdownMenu(
            expanded = showOptionsMenu,
            onDismissRequest = { posViewModel.showOptionsMenu.value = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.clear_ticket)) },
                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Red) },
                onClick = {
                    posViewModel.clearCart()
                    posViewModel.showOptionsMenu.value = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.save_open_ticket)) },
                leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = LoyverseTopGreen) },
                onClick = {
                    posViewModel.showOptionsMenu.value = false
                    newTicketNameInput = context.getString(R.string.ticket_number_fmt, openTickets.size + 1)
                    showSaveTicketDialog = true
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.apply_discount)) },
                leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, tint = LoyverseTopGreen) },
                onClick = {
                    posViewModel.showOptionsMenu.value = false
                    posViewModel.showTicketSheet.value = true
                }
            )
        }
    }

    // 9. Checkout / Payment Modal
    if (showCheckoutModal) {
        AlertDialog(
            onDismissRequest = { posViewModel.showCheckoutModal.value = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.charge_amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.msg_2f_s_21).format(cartTotal, currency),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = LoyverseTopGreen
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Active Cashier Badge
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = LoyverseTopGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.cashier_label_fmt, activeCashier),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryLight
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.select_payment_method),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextSecondaryLight
                    )

                    val paymentMethods = listOf(
                        Triple("CASH", stringResource(R.string.cash_payment_opt), Icons.Default.Payments),
                        Triple("CARD", stringResource(R.string.card_payment_opt), Icons.Default.CreditCard),
                        Triple("DIGITAL", stringResource(R.string.digital_payment_opt), Icons.Default.QrCode)
                    )

                    paymentMethods.forEach { (method, label, icon) ->
                        OutlinedButton(
                            onClick = {
                                posViewModel.checkout(
                                    paymentMethod = method,
                                    cashierName = activeCashier.ifBlank { "Lojia Manager" },
                                    onComplete = { completedSale: POSSale ->
                                        posViewModel.showCheckoutModal.value = false
                                        onSaleCompleted(completedSale)
                                    }
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pay_$method")
                        ) {
                            Icon(icon, contentDescription = null, tint = LoyverseTopGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { posViewModel.showCheckoutModal.value = false }) {
                    Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        )
    }

    // 10. Barcode Scanner Live Viewfinder & Rapid SKU Entry
    if (showBarcodeScannerDialog) {
        BarcodeScannerDialog(
            availableProducts = filteredProducts,
            currentLanguage = language,
            onBarcodeScanned = { barcode ->
                posViewModel.scanBarcode(barcode)
                showBarcodeScannerDialog = false
            },
            onDismiss = { showBarcodeScannerDialog = false }
        )
    }
}

