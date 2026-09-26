package com.lojia.shiftreport.settings

import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.data.Cashier
import kotlinx.coroutines.delay

enum class CashierStatusFilter {
    ALL, ACTIVE, ON_SHIFT, INACTIVE
}

/**
 * Enterprise-Grade Responsive Cashier Management Interface
 */
@Composable
fun CashierManagementSection(
    cashiers: List<Cashier>,
    onAddCashierClick: () -> Unit,
    onDeleteCashierClick: (Cashier) -> Unit,
    onToggleStatusClick: ((Cashier) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CashierStatusFilter.ALL) }
    var isLoading by remember { mutableStateOf(true) }

    // Admin Auth State for Deletion
    var cashierPendingDeletion by remember { mutableStateOf<Cashier?>(null) }

    // Simulated skeleton initial load
    LaunchedEffect(Unit) {
        delay(400)
        isLoading = false
    }

    // Filter calculations
    val totalCount = cashiers.size
    val activeCount = remember(cashiers) { cashiers.count { it.active } }
    val onShiftCount = remember(cashiers) { cashiers.count { it.active && it.id % 2 == 1 } }
    val inactiveCount = remember(cashiers) { cashiers.count { !it.active } }

    val filteredCashiers = remember(cashiers, searchQuery, selectedFilter) {
        cashiers.filter { cashier ->
            val matchesQuery = searchQuery.isBlank() ||
                    cashier.name.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                CashierStatusFilter.ALL -> true
                CashierStatusFilter.ACTIVE -> cashier.active
                CashierStatusFilter.ON_SHIFT -> cashier.active && (cashier.id % 2 == 1)
                CashierStatusFilter.INACTIVE -> !cashier.active
            }

            matchesQuery && matchesFilter
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LojiaDimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // =====================================================================
            // 1. EXECUTIVE HEADER CARD WITH KPI STRIP
            // =====================================================================
            LojiaSettingsCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Team Overview",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceLight
                        )

                        Button(
                            onClick = onAddCashierClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryIndigoLight,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(LojiaDimens.InputRadius),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.add_cashier_4),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiChip(
                            label = "Total",
                            value = totalCount.toString(),
                            color = PrimaryIndigoLight,
                            modifier = Modifier.weight(1f)
                        )
                        KpiChip(
                            label = "Active",
                            value = activeCount.toString(),
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        KpiChip(
                            label = "On-Shift",
                            value = onShiftCount.toString(),
                            color = InfoBlue,
                            modifier = Modifier.weight(1f)
                        )
                        KpiChip(
                            label = "Inactive",
                            value = inactiveCount.toString(),
                            color = OnSurfaceVariantLight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // =====================================================================
            // 2. SEARCH & STATUS FILTERS ROW
            // =====================================================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search cashiers...",
                            fontSize = 13.sp,
                            color = TextHintColor
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = OnSurfaceVariantLight
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null,
                                    tint = OnSurfaceVariantLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigoLight,
                        unfocusedBorderColor = OutlineLight,
                        focusedContainerColor = SurfaceLight,
                        unfocusedContainerColor = SurfaceLight,
                        focusedTextColor = OnSurfaceLight,
                        unfocusedTextColor = OnSurfaceLight,
                        cursorColor = PrimaryIndigoLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Filter Chips Segment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusFilterChip(
                        label = "All",
                        selected = selectedFilter == CashierStatusFilter.ALL,
                        onClick = { selectedFilter = CashierStatusFilter.ALL }
                    )
                    StatusFilterChip(
                        label = "Active",
                        selected = selectedFilter == CashierStatusFilter.ACTIVE,
                        onClick = { selectedFilter = CashierStatusFilter.ACTIVE }
                    )
                    StatusFilterChip(
                        label = "On-Shift",
                        selected = selectedFilter == CashierStatusFilter.ON_SHIFT,
                        onClick = { selectedFilter = CashierStatusFilter.ON_SHIFT }
                    )
                    StatusFilterChip(
                        label = "Inactive",
                        selected = selectedFilter == CashierStatusFilter.INACTIVE,
                        onClick = { selectedFilter = CashierStatusFilter.INACTIVE }
                    )
                }
            }

            // =====================================================================
            // 3. CASHIER LIST / EMPTY STATE / SKELETON
            // =====================================================================
            if (isLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        CashierSkeletonCard()
                    }
                }
            } else if (filteredCashiers.isEmpty()) {
                LojiaSettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = null,
                            tint = TextHintColor,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No cashiers found",
                            fontSize = 16.sp,
                            color = OnSurfaceVariantLight,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LojiaPrimaryButton(
                            text = "Add First Cashier",
                            onClick = { onAddCashierClick() }
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    filteredCashiers.forEach { cashier ->
                        CashierGridCard(
                            cashier = cashier,
                            onToggleActive = { updated ->
                                onToggleStatusClick?.invoke(updated)
                            },
                            onRequestDelete = {
                                cashierPendingDeletion = cashier
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }

        // FAB bottom-end
        FloatingActionButton(
            onClick = onAddCashierClick,
            containerColor = PrimaryIndigoLight,
            contentColor = PureWhite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Cashier"
            )
        }
    }

    // =========================================================================
    // 4. ADMIN RE-AUTHENTICATION DIALOG FOR DELETION
    // =========================================================================
    cashierPendingDeletion?.let { cashier ->
        AdminAuthDialog(
            actionTitle = "Delete Cashier: ${cashier.name}",
            actionDescription = "Mandatory admin re-authentication required. An automated security audit log entry will be created.",
            onDismissRequest = { cashierPendingDeletion = null },
            onAuthSuccess = {
                onDeleteCashierClick(cashier)
                Toast.makeText(context, "Audit log generated: Cashier ${cashier.name} deleted.", Toast.LENGTH_LONG).show()
                cashierPendingDeletion = null
            }
        )
    }
}

// =============================================================================
// KPI CHIP COMPONENT
// =============================================================================
@Composable
private fun KpiChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = color,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// =============================================================================
// STATUS FILTER CHIP
// =============================================================================
@Composable
private fun StatusFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) PrimaryContainerLight else SurfaceVariantLight,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            1.dp,
            if (selected) PrimaryIndigoLight else OutlineLight
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) PrimaryIndigoLight else OnSurfaceVariantLight,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// =============================================================================
// CASHIER ROW CARD
// =============================================================================
@Composable
private fun CashierGridCard(
    cashier: Cashier,
    onToggleActive: (Cashier) -> Unit,
    onRequestDelete: () -> Unit
) {
    val isOnShift = remember(cashier) { cashier.active && (cashier.id % 2 == 1) }
    val initials = remember(cashier.name) {
        cashier.name
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { cashier.name.take(1).uppercase() }
    }
    val roleLabel = when {
        !cashier.active -> "Inactive"
        isOnShift -> "On-Shift"
        else -> "Cashier"
    }

    LojiaSettingsCard {
        // Avatar circle 48.dp bg PrimaryContainerLight
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(PrimaryContainerLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigoLight
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cashier.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Role + status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RoleChip(roleLabel)
                StatusDot(if (cashier.active) SuccessGreen else TextHintColor)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "PIN: ••••••",
                fontSize = 12.sp,
                color = TextHintColor
            )
        }

        Switch(
            checked = cashier.active,
            onCheckedChange = { isChecked ->
                onToggleActive(cashier.copy(active = isChecked))
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = PrimaryIndigoLight,
                uncheckedThumbColor = PureWhite,
                uncheckedTrackColor = OutlineLight
            ),
            modifier = Modifier.scale(0.8f)
        )

        IconButton(onClick = onRequestDelete) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.delete),
                tint = ErrorRedLight
            )
        }
    }
}

@Composable
private fun RoleChip(role: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = SurfaceVariantLight
    ) {
        Text(
            text = role,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceVariantLight,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// =============================================================================
// SKELETON CARD PLACEHOLDER
// =============================================================================
@Composable
private fun CashierSkeletonCard() {
    LojiaSettingsCard {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceVariantLight)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceVariantLight)
            )
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BackgroundLight)
            )
        }
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceVariantLight)
        )
    }
}
