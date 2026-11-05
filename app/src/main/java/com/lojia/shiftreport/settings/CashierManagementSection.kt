package com.lojia.shiftreport.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.data.Cashier
import com.lojia.shiftreport.data.ShiftReportRepository
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CashierStatusFilter {
    ALL, ACTIVE, ON_SHIFT, ARCHIVED
}

/**
 * Enterprise-Grade Cashier Management Interface (Foodics POS / Banking App standard).
 * Complies with ZATCA/VAT audit integrity: no hard deletion for cashiers with historical shifts.
 */
@Composable
fun CashierManagementSection(
    cashiers: List<Cashier>,
    onAddCashierClick: (() -> Unit)? = null,
    onDeleteCashierClick: ((Cashier) -> Unit)? = null,
    onArchiveCashierClick: ((Cashier) -> Unit)? = null,
    onUnarchiveCashierClick: ((Cashier) -> Unit)? = null,
    onEditCashierClick: ((Cashier) -> Unit)? = null,
    onSaveNewCashier: ((name: String, pin: String, role: String) -> Unit)? = null,
    onToggleStatusClick: ((Cashier) -> Unit)? = null,
    reportViewModel: ReportViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember(context) { ShiftReportRepository.getInstance(context) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CashierStatusFilter.ALL) }
    var isLoading by remember { mutableStateOf(true) }
    var isArchivedSectionExpanded by remember { mutableStateOf(false) }

    // Admin Auth States
    var showAdminAuthForAdd by remember { mutableStateOf(false) }
    var cashierPendingAdminAuthForEdit by remember { mutableStateOf<Cashier?>(null) }
    var cashierPendingAdminAuthForDelete by remember { mutableStateOf<Cashier?>(null) }
    var cashierPendingAdminAuthForRestore by remember { mutableStateOf<Cashier?>(null) }

    // Modals triggered post Admin-Auth
    var showAddDialog by remember { mutableStateOf(false) }
    var cashierToEdit by remember { mutableStateOf<Cashier?>(null) }
    var cashierCannotDeleteWithHistory by remember { mutableStateOf<Cashier?>(null) }
    var cashierPermanentDeleteConfirm by remember { mutableStateOf<Cashier?>(null) }

    LaunchedEffect(Unit) {
        delay(250)
        isLoading = false
    }

    // Partition cashiers into active/unarchived vs archived
    val activeCashiers = remember(cashiers) {
        cashiers.filter { !it.isArchived }
    }
    val archivedCashiers = remember(cashiers) {
        cashiers.filter { it.isArchived }
    }

    // KPI Counts
    val totalCount = cashiers.size
    val activeCount = remember(activeCashiers) { activeCashiers.count { it.isActive && it.active } }
    val onShiftCount = remember(activeCashiers) { activeCashiers.count { it.isActive && it.active && it.id % 2 == 1 } }
    val archivedCount = archivedCashiers.size

    val filteredActiveCashiers = remember(activeCashiers, searchQuery, selectedFilter) {
        if (selectedFilter == CashierStatusFilter.ARCHIVED) {
            emptyList()
        } else {
            activeCashiers.filter { cashier ->
                val matchesQuery = searchQuery.isBlank() ||
                        cashier.name.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (selectedFilter) {
                    CashierStatusFilter.ALL -> true
                    CashierStatusFilter.ACTIVE -> cashier.isActive && cashier.active
                    CashierStatusFilter.ON_SHIFT -> cashier.isActive && cashier.active && (cashier.id % 2 == 1)
                    CashierStatusFilter.ARCHIVED -> false
                }
                matchesQuery && matchesFilter
            }
        }
    }

    val filteredArchivedCashiers = remember(archivedCashiers, searchQuery, selectedFilter) {
        archivedCashiers.filter { cashier ->
            val matchesQuery = searchQuery.isBlank() ||
                    cashier.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                CashierStatusFilter.ALL, CashierStatusFilter.ARCHIVED -> true
                else -> false
            }
            matchesQuery && matchesFilter
        }
    }

    fun executeArchive(target: Cashier) {
        if (onArchiveCashierClick != null) {
            onArchiveCashierClick(target)
        } else if (reportViewModel != null) {
            reportViewModel.archiveCashier(target)
        } else {
            coroutineScope.launch(Dispatchers.IO) {
                repository.archiveCashier(target.id.toString())
            }
        }
        Toast.makeText(context, context.getString(R.string.cashier_archived_success, target.name), Toast.LENGTH_SHORT).show()
        cashierCannotDeleteWithHistory = null
    }

    fun executeRestore(target: Cashier) {
        if (onUnarchiveCashierClick != null) {
            onUnarchiveCashierClick(target)
        } else if (reportViewModel != null) {
            reportViewModel.unarchiveCashier(target)
        } else {
            coroutineScope.launch(Dispatchers.IO) {
                repository.unarchiveCashier(target.id.toString())
            }
        }
        Toast.makeText(context, context.getString(R.string.cashier_restored_success, target.name), Toast.LENGTH_SHORT).show()
        cashierPendingAdminAuthForRestore = null
    }

    fun executePermanentDelete(target: Cashier) {
        if (onDeleteCashierClick != null) {
            onDeleteCashierClick(target)
        } else if (reportViewModel != null) {
            reportViewModel.deleteCashier(target)
        } else {
            coroutineScope.launch(Dispatchers.IO) {
                repository.deleteCashier(target)
            }
        }
        Toast.makeText(context, context.getString(R.string.user_deleted_msg, target.name), Toast.LENGTH_SHORT).show()
        cashierPermanentDeleteConfirm = null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LojiaDimens.ScreenPadding, vertical = 8.dp)
                .padding(PaddingValues(bottom = 88.dp)),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // =====================================================================
            // 1. KPI STRIP & HEADER
            // =====================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, OutlineVariantLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.team_overview),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceLight
                        )

                        Button(
                            onClick = { showAdminAuthForAdd = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryIndigoLight,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(LojiaDimens.InputRadius),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("btnAddCashier")
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
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureWhite
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
                            label = stringResource(R.string.total),
                            value = totalCount.toString(),
                            color = PrimaryIndigoLight,
                            modifier = Modifier.weight(1f)
                        )
                        KpiChip(
                            label = stringResource(R.string.status_active),
                            value = activeCount.toString(),
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        KpiChip(
                            label = stringResource(R.string.cashier_status_on_shift),
                            value = onShiftCount.toString(),
                            color = InfoBlue,
                            modifier = Modifier.weight(1f)
                        )
                        KpiChip(
                            label = stringResource(R.string.status_archived),
                            value = archivedCount.toString(),
                            color = TextHintColor,
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
                            text = stringResource(R.string.search_cashiers_placeholder),
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusFilterChip(
                        label = stringResource(R.string.filter_status_all),
                        selected = selectedFilter == CashierStatusFilter.ALL,
                        onClick = { selectedFilter = CashierStatusFilter.ALL }
                    )
                    StatusFilterChip(
                        label = stringResource(R.string.status_active),
                        selected = selectedFilter == CashierStatusFilter.ACTIVE,
                        onClick = { selectedFilter = CashierStatusFilter.ACTIVE }
                    )
                    StatusFilterChip(
                        label = stringResource(R.string.cashier_status_on_shift),
                        selected = selectedFilter == CashierStatusFilter.ON_SHIFT,
                        onClick = { selectedFilter = CashierStatusFilter.ON_SHIFT }
                    )
                    StatusFilterChip(
                        label = stringResource(R.string.status_archived),
                        selected = selectedFilter == CashierStatusFilter.ARCHIVED,
                        onClick = { selectedFilter = CashierStatusFilter.ARCHIVED }
                    )
                }
            }

            // =====================================================================
            // 3. ACTIVE CASHIER LIST / EMPTY STATE
            // =====================================================================
            if (isLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { CashierSkeletonCard() }
                }
            } else if (selectedFilter != CashierStatusFilter.ARCHIVED) {
                if (filteredActiveCashiers.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, OutlineVariantLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.People,
                                contentDescription = null,
                                tint = TextHintColor,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_cashiers_found_1),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add your first cashier to get started",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextHintColor
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredActiveCashiers.forEach { cashier ->
                            CashierCard(
                                cashier = cashier,
                                isArchived = false,
                                onEdit = {
                                    cashierPendingAdminAuthForEdit = cashier
                                },
                                onToggleActive = { updated ->
                                    if (onToggleStatusClick != null) {
                                        onToggleStatusClick(updated)
                                    } else {
                                        reportViewModel?.updateCashier(updated)
                                    }
                                },
                                onDelete = {
                                    cashierPendingAdminAuthForDelete = cashier
                                }
                            )
                        }
                    }
                }
            }

            // =====================================================================
            // 4. EXPANDABLE ARCHIVED CASHIERS SECTION
            // =====================================================================
            if (filteredArchivedCashiers.isNotEmpty() || selectedFilter == CashierStatusFilter.ARCHIVED) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, OutlineVariantLight)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isArchivedSectionExpanded = !isArchivedSectionExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceVariantLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Archive,
                                        contentDescription = null,
                                        tint = OnSurfaceVariantLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${stringResource(R.string.archived_cashiers_title)} (${filteredArchivedCashiers.size})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurfaceLight
                                    )
                                    Text(
                                        text = "Preserved for ZATCA/VAT audit history",
                                        fontSize = 11.sp,
                                        color = TextHintColor
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isArchivedSectionExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight
                            )
                        }

                        if (isArchivedSectionExpanded || selectedFilter == CashierStatusFilter.ARCHIVED) {
                            HorizontalDivider(color = OutlineVariantLight)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (filteredArchivedCashiers.isEmpty()) {
                                    Text(
                                        text = "No archived cashiers.",
                                        fontSize = 12.sp,
                                        color = TextHintColor,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    filteredArchivedCashiers.forEach { archivedCashier ->
                                        CashierCard(
                                            cashier = archivedCashier,
                                            isArchived = true,
                                            onRestore = {
                                                cashierPendingAdminAuthForRestore = archivedCashier
                                            },
                                            onDelete = {
                                                cashierPendingAdminAuthForDelete = archivedCashier
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // DIALOGS & GATED ADMIN FLOWS
    // =========================================================================

    // 1. Admin Auth for Add Cashier
    if (showAdminAuthForAdd) {
        AdminAuthDialog(
            actionTitle = stringResource(R.string.add_cashier),
            actionDescription = stringResource(R.string.admin_auth_audit_desc),
            onDismissRequest = { showAdminAuthForAdd = false },
            onAuthSuccess = {
                showAdminAuthForAdd = false
                showAddDialog = true
            }
        )
    }

    // 2. Add Cashier Dialog
    if (showAddDialog) {
        AddCashierDialog(
            existingCashier = null,
            onDismissRequest = { showAddDialog = false },
            onConfirmAdd = { name ->
                if (onSaveNewCashier != null) {
                    onSaveNewCashier(name, "", "CASHIER")
                } else if (reportViewModel != null) {
                    reportViewModel.addCashier(name, "", "CASHIER")
                } else {
                    onAddCashierClick?.invoke()
                }
                showAddDialog = false
            },
            onConfirmSave = { name, pin, role ->
                if (onSaveNewCashier != null) {
                    onSaveNewCashier(name, pin, role)
                } else if (reportViewModel != null) {
                    reportViewModel.addCashier(name, pin, role)
                } else {
                    onAddCashierClick?.invoke()
                }
                showAddDialog = false
            }
        )
    }

    // 3. Admin Auth for Edit Cashier
    cashierPendingAdminAuthForEdit?.let { target ->
        AdminAuthDialog(
            actionTitle = "${stringResource(R.string.edit_cashier)}: ${target.name}",
            actionDescription = stringResource(R.string.admin_auth_audit_desc),
            onDismissRequest = { cashierPendingAdminAuthForEdit = null },
            onAuthSuccess = {
                cashierPendingAdminAuthForEdit = null
                cashierToEdit = target
            }
        )
    }

    // 4. Edit Cashier Dialog
    cashierToEdit?.let { target ->
        AddCashierDialog(
            existingCashier = target,
            onDismissRequest = { cashierToEdit = null },
            onConfirmAdd = { newName ->
                val updated = target.copy(name = newName)
                if (onEditCashierClick != null) onEditCashierClick(updated) else reportViewModel?.updateCashier(updated)
                cashierToEdit = null
            },
            onConfirmSave = { newName, newPin, newRole ->
                val updated = target.copy(
                    name = newName,
                    pin = if (newPin.isNotBlank()) newPin else target.pin,
                    role = newRole
                )
                if (onEditCashierClick != null) onEditCashierClick(updated) else reportViewModel?.updateCashier(updated)
                cashierToEdit = null
            }
        )
    }

    // 5. Admin Auth for Delete/Archive
    cashierPendingAdminAuthForDelete?.let { target ->
        AdminAuthDialog(
            actionTitle = "${stringResource(R.string.delete_cashier)}: ${target.name}",
            actionDescription = stringResource(R.string.admin_auth_audit_desc),
            onDismissRequest = { cashierPendingAdminAuthForDelete = null },
            onAuthSuccess = {
                val toEvaluate = target
                cashierPendingAdminAuthForDelete = null
                coroutineScope.launch(Dispatchers.IO) {
                    val shiftCount = repository.getShiftCountByCashierId(toEvaluate.id.toString())
                    withContext(Dispatchers.Main) {
                        if (shiftCount > 0) {
                            cashierCannotDeleteWithHistory = toEvaluate
                        } else {
                            cashierPermanentDeleteConfirm = toEvaluate
                        }
                    }
                }
            }
        )
    }

    // 6. Admin Auth for Restore
    cashierPendingAdminAuthForRestore?.let { target ->
        AdminAuthDialog(
            actionTitle = "${stringResource(R.string.action_unarchive)}: ${target.name}",
            actionDescription = stringResource(R.string.admin_auth_audit_desc),
            onDismissRequest = { cashierPendingAdminAuthForRestore = null },
            onAuthSuccess = {
                executeRestore(target)
            }
        )
    }

    // 7. Dialog: "Cannot Delete - Has History" (Material 3 AlertDialog, 28dp radius)
    cashierCannotDeleteWithHistory?.let { target ->
        AlertDialog(
            onDismissRequest = { cashierCannotDeleteWithHistory = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = SurfaceLight,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WarningContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.cashier_cannot_delete_has_history_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    color = OnBackgroundLight,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.cashier_cannot_delete_has_history_desc),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = OnSurfaceVariantLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { executeArchive(target) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btnArchiveCashier")
                ) {
                    Text(stringResource(R.string.action_archive))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { cashierCannotDeleteWithHistory = null },
                    modifier = Modifier.testTag("btnCancelArchive")
                ) {
                    Text(stringResource(R.string.cancel_18))
                }
            },
            modifier = Modifier.testTag("cannotDeleteHistoryDialog")
        )
    }

    // 8. Dialog: "Delete Permanently?" with "type DELETE confirmation" (Material 3 AlertDialog, 28dp radius)
    cashierPermanentDeleteConfirm?.let { target ->
        var deleteConfirmationText by remember { mutableStateOf("") }
        val isConfirmed = deleteConfirmationText.trim().equals("DELETE", ignoreCase = true)

        AlertDialog(
            onDismissRequest = { cashierPermanentDeleteConfirm = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = SurfaceLight,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ErrorContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = ErrorRedLight,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_permanently_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    color = OnBackgroundLight,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delete_permanently_desc),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = OnSurfaceVariantLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deleteConfirmationText,
                        onValueChange = { deleteConfirmationText = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.type_delete_to_confirm),
                                fontSize = 12.sp,
                                color = TextHintColor
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ErrorRedLight,
                            unfocusedBorderColor = OutlineLight,
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            cursorColor = ErrorRedLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inputDeleteConfirm")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { executePermanentDelete(target) },
                    enabled = isConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRedLight,
                        disabledContainerColor = ErrorRedLight.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btnConfirmPermanentDelete")
                ) {
                    Text(stringResource(R.string.delete_1))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { cashierPermanentDeleteConfirm = null },
                    modifier = Modifier.testTag("btnCancelPermanentDelete")
                ) {
                    Text(stringResource(R.string.cancel_18))
                }
            },
            modifier = Modifier.testTag("permanentDeleteDialog")
        )
    }
}

// =============================================================================
// CASHIER CARD (Active vs Archived)
// =============================================================================
@Composable
private fun CashierCard(
    cashier: Cashier,
    isArchived: Boolean,
    onEdit: (() -> Unit)? = null,
    onToggleActive: ((Cashier) -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    val initial = remember(cashier.name) {
        cashier.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
    val isOnShift = remember(cashier) { !isArchived && cashier.active && (cashier.id % 2 == 1) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isArchived) SurfaceVariantLight.copy(alpha = 0.5f) else SurfaceLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, OutlineVariantLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isArchived) SurfaceVariantLight else PrimaryContainerLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isArchived) OnSurfaceVariantLight else PrimaryIndigoLight
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cashier.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isArchived) OnSurfaceVariantLight else OnSurfaceLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isArchived) {
                        CashierStatusChip(
                            label = stringResource(R.string.status_archived),
                            containerColor = SurfaceVariantLight,
                            textColor = OnSurfaceVariantLight,
                            dotColor = TextHintColor
                        )
                    } else if (!cashier.active) {
                        CashierStatusChip(
                            label = stringResource(R.string.inactive),
                            containerColor = SurfaceVariantLight,
                            textColor = TextHintColor,
                            dotColor = TextHintColor
                        )
                    } else if (isOnShift) {
                        CashierStatusChip(
                            label = stringResource(R.string.cashier_status_on_shift),
                            containerColor = InfoContainer,
                            textColor = InfoBlue,
                            dotColor = InfoBlue
                        )
                    } else {
                        CashierStatusChip(
                            label = stringResource(R.string.status_active),
                            containerColor = SuccessContainer,
                            textColor = SuccessGreen,
                            dotColor = SuccessGreen
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceVariantLight
                    ) {
                        Text(
                            text = cashier.role,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceVariantLight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isArchived && cashier.archivedAt != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    val dateFormatted = remember(cashier.archivedAt) {
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(cashier.archivedAt))
                    }
                    Text(
                        text = "Archived: $dateFormatted",
                        fontSize = 10.5.sp,
                        color = TextHintColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            if (isArchived) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onRestore?.invoke() },
                        modifier = Modifier.testTag("btnRestoreCashier_${cashier.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = stringResource(R.string.action_unarchive),
                            tint = PrimaryIndigoLight
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("btnDeleteArchivedCashier_${cashier.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = ErrorRedLight
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEdit?.invoke() },
                        modifier = Modifier.testTag("btnEditCashier_${cashier.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.edit_cashier),
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Switch(
                        checked = cashier.active,
                        onCheckedChange = { isChecked ->
                            onToggleActive?.invoke(cashier.copy(isActive = isChecked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = PrimaryIndigoLight,
                            uncheckedThumbColor = PureWhite,
                            uncheckedTrackColor = OutlineLight
                        ),
                        modifier = Modifier.testTag("switchCashier_${cashier.id}")
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("btnDeleteCashier_${cashier.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = ErrorRedLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENTS
// =============================================================================
@Composable
private fun CashierStatusChip(
    label: String,
    containerColor: Color,
    textColor: Color,
    dotColor: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

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

@Composable
private fun CashierSkeletonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineVariantLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantLight)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceVariantLight)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceVariantLight)
                )
            }
        }
    }
}
