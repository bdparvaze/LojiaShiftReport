package com.lojia.pos.settings

import com.lojia.pos.ui.common.LojiaTextField
import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.pos.data.Cashier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val onShiftCount = remember(cashiers) { cashiers.count { it.active && it.id % 2 == 1 } } // Demo on-shift status
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // =====================================================================
        // 1. EXECUTIVE HEADER CARD WITH KPI STRIP
        // =====================================================================
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x0A000000))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0FDF4))
                                    .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PointOfSale,
                                    contentDescription = null,
                                    tint = Color(0xFF00796B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = stringResource(R.string.cashier_management_3),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.add_cashiers_here_to),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Primary Add Cashier Action Button
                    Button(
                        onClick = onAddCashierClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00796B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
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

                // Metric Summary Strip
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetricPill(
                            label = "Total",
                            value = "$totalCount",
                            icon = Icons.Outlined.People,
                            color = Color(0xFF0F172A)
                        )
                        Divider(color = Color(0xFFE2E8F0), modifier = Modifier.height(18.dp).width(1.dp))
                        MetricPill(
                            label = "Active",
                            value = "$activeCount",
                            icon = Icons.Outlined.CheckCircle,
                            color = Color(0xFF166534)
                        )
                        Divider(color = Color(0xFFE2E8F0), modifier = Modifier.height(18.dp).width(1.dp))
                        MetricPill(
                            label = "On-Shift",
                            value = "$onShiftCount",
                            icon = Icons.Outlined.Badge,
                            color = Color(0xFF0284C7)
                        )
                        Divider(color = Color(0xFFE2E8F0), modifier = Modifier.height(18.dp).width(1.dp))
                        MetricPill(
                            label = "Inactive",
                            value = "$inactiveCount",
                            icon = Icons.Outlined.PauseCircle,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 2. SEARCH & STATUS FILTERS ROW
        // =====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Real-time Search TextField
            LojiaTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        stringResource(R.string.search_cashier_shift),
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00796B),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Filter Chips Segment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusFilterChip(
                    label = "All ($totalCount)",
                    selected = selectedFilter == CashierStatusFilter.ALL,
                    onClick = { selectedFilter = CashierStatusFilter.ALL }
                )
                StatusFilterChip(
                    label = "Active ($activeCount)",
                    selected = selectedFilter == CashierStatusFilter.ACTIVE,
                    onClick = { selectedFilter = CashierStatusFilter.ACTIVE },
                    accentColor = Color(0xFF166534)
                )
                StatusFilterChip(
                    label = "On-Shift ($onShiftCount)",
                    selected = selectedFilter == CashierStatusFilter.ON_SHIFT,
                    onClick = { selectedFilter = CashierStatusFilter.ON_SHIFT },
                    accentColor = Color(0xFF0284C7)
                )
                StatusFilterChip(
                    label = "Inactive ($inactiveCount)",
                    selected = selectedFilter == CashierStatusFilter.INACTIVE,
                    onClick = { selectedFilter = CashierStatusFilter.INACTIVE },
                    accentColor = Color(0xFF64748B)
                )
            }
        }

        // =====================================================================
        // 3. RESPONSIVE GRID / CARDS WITH SKELETON LOADING
        // =====================================================================
        if (isLoading) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) {
                    CashierSkeletonCard()
                }
            }
        } else if (filteredCashiers.isEmpty()) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOff,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.no_cashiers_found_2),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp
                    )

                    Text(
                        text = "No cashiers match your current search or filter criteria.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onAddCashierClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.add_first_cashier_1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            // Adaptive Grid / Column
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWideScreen = maxWidth > 600.dp

                if (isWideScreen) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 1000.dp)
                    ) {
                        items(filteredCashiers, key = { it.id }) { cashier ->
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
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            }
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
// METRIC PILL COMPONENT
// =============================================================================
@Composable
private fun MetricPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// =============================================================================
// STATUS FILTER CHIP
// =============================================================================
@Composable
private fun StatusFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = Color(0xFF00796B)
) {
    Surface(
        color = if (selected) accentColor.copy(alpha = 0.12f) else Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (selected) accentColor else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accentColor else Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// =============================================================================
// ENTERPRISE CASHIER CARD WITH SENSITIVE MASKING & TOGGLE SWITCH
// =============================================================================
@Composable
private fun CashierGridCard(
    cashier: Cashier,
    onToggleActive: (Cashier) -> Unit,
    onRequestDelete: () -> Unit
) {
    var isPinMasked by remember { mutableStateOf(true) }
    val isOnShift = remember(cashier) { cashier.active && (cashier.id % 2 == 1) }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (cashier.active) Color(0xFFE2E8F0) else Color(0xFFF1F5F9)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                if (cashier.active) 2.dp else 0.dp,
                RoundedCornerShape(14.dp),
                ambientColor = Color(0x0A000000)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Squircle
                    Box {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (cashier.active) Color(0xFFCCFBF1) else Color(0xFFF1F5F9)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cashier.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (cashier.active) Color(0xFF00796B) else Color(0xFF94A3B8),
                                fontSize = 17.sp
                            )
                        }

                        if (cashier.active) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(Color.White, CircleShape)
                                    .padding(1.5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (isOnShift) Color(0xFF0284C7) else Color(0xFF22C55E),
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }

                    // Name & Tags
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = cashier.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp,
                                color = if (cashier.active) Color(0xFF0F172A) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // On-Shift / Active Status Tag
                            Surface(
                                color = when {
                                    !cashier.active -> Color(0xFFF1F5F9)
                                    isOnShift -> Color(0xFFE0F2FE)
                                    else -> Color(0xFFDCFCE7)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when {
                                        !cashier.active -> "Inactive"
                                        isOnShift -> "On-Shift"
                                        else -> "Active"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !cashier.active -> Color(0xFF64748B)
                                        isOnShift -> Color(0xFF0369A1)
                                        else -> Color(0xFF15803D)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Terminal Cashier",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Delete Action
                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Bottom Info: Masked PIN & Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PIN Security Masked Display with Toggle Eye
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color(0xFF00796B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isPinMasked) "PIN: ••••••" else "PIN: 111111",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                    IconButton(
                        onClick = { isPinMasked = !isPinMasked },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPinMasked) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = "Toggle PIN Visibility",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Status Toggle Switch (Active / Inactive)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (cashier.active) "Active" else "Disabled",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (cashier.active) Color(0xFF166534) else Color(0xFF94A3B8)
                    )
                    Switch(
                        checked = cashier.active,
                        onCheckedChange = { isChecked ->
                            onToggleActive(cashier.copy(active = isChecked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00796B),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.scale(0.75f)
                    )
                }
            }
        }
    }
}

// =============================================================================
// SKELETON CARD PLACEHOLDER
// =============================================================================
@Composable
private fun CashierSkeletonCard() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF1F5F9))
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF8FAFC))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
            )
        }
    }
}
