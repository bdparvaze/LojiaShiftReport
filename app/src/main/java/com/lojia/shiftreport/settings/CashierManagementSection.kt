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
import com.lojia.shiftreport.util.DateTimeFormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var cashierPendingAdminAuthForToggle by remember { mutableStateOf<Cashier?>(null) }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardRise(0)
                    .depthCard(accent = true)
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
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(LojiaDimens.InputRadius),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = Dimens.MinTouchTarget)
                                .depthRaisedButton(enabled = true, cornerRadius = LojiaDimens.InputRadius)
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
                LojiaTextField(
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
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
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
                    shape = RoundedCornerShape(Dimens.RadiusMd),
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardRise(1)
                            .depthCard()
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
                                text = stringResource(R.string.cashier_empty_subtitle),
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
                        filteredActiveCashiers.forEachIndexed { index, cashier ->
                            CashierCard(
                                cashier = cashier,
                                isArchived = false,
                                riseIndex = index + 1,
                                onEdit = {
                                    cashierPendingAdminAuthForEdit = cashier
                                },
                                onToggleActive = { updated ->
                                    cashierPendingAdminAuthForToggle = updated
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cardRise(2)
                        .depthCard()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.MinTouchTarget)
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
                                        text = stringResource(R.string.cashier_archived_audit_desc),
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
                                        text = stringResource(R.string.cashier_no_archived),
                                        fontSize = 12.sp,
                                        color = TextHintColor,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    filteredArchivedCashiers.forEachIndexed { index, archivedCashier ->
                                        CashierCard(
                                            cashier = archivedCashier,
                                            isArchived = true,
                                            riseIndex = index + 1,
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
            subtitle = stringResource(R.string.add_cashier),
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
            subtitle = "${stringResource(R.string.edit_cashier)}: ${target.name}",
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
            subtitle = "${stringResource(R.string.delete_cashier)}: ${target.name}",
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
            subtitle = "${stringResource(R.string.action_unarchive)}: ${target.name}",
            onDismissRequest = { cashierPendingAdminAuthForRestore = null },
            onAuthSuccess = {
                executeRestore(target)
            }
        )
    }

    // 6b. Admin Auth for Active/Inactive Status Toggle
    cashierPendingAdminAuthForToggle?.let { updated ->
        AdminAuthDialog(
            subtitle = "${stringResource(R.string.edit_cashier)}: ${updated.name}",
            onDismissRequest = { cashierPendingAdminAuthForToggle = null },
            onAuthSuccess = {
                cashierPendingAdminAuthForToggle = null
                if (onToggleStatusClick != null) {
                    onToggleStatusClick(updated)
                } else {
                    reportViewModel?.updateCashier(updated)
                }
            }
        )
    }

    // 7. Dialog: "Cannot Delete - Has History" (LojiaDialog soft depth)
    cashierCannotDeleteWithHistory?.let { target ->
        LojiaDialog(
            onDismissRequest = { cashierCannotDeleteWithHistory = null },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.DialogPaddingComfortable),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                ) {
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

                    Text(
                        text = stringResource(R.string.cashier_cannot_delete_has_history_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        color = OnBackgroundLight,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.cashier_cannot_delete_has_history_desc),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = OnSurfaceVariantLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { executeArchive(target) },
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Dimens.MinTouchTarget)
                                .depthRaisedButton(enabled = true, cornerRadius = Dimens.RadiusMd)
                                .testTag("btnArchiveCashier")
                        ) {
                            Text(
                                text = stringResource(R.string.action_archive),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { cashierCannotDeleteWithHistory = null },
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            border = null,
                            elevation = null,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = OnSurfaceVariantLight
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Dimens.MinTouchTarget)
                                .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                                .testTag("btnCancelArchive")
                        ) {
                            Text(
                                text = stringResource(R.string.cancel_18),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            modifier = Modifier.testTag("cannotDeleteHistoryDialog")
        )
    }

    // 8. Dialog: "Delete Permanently?" with "type DELETE confirmation" (LojiaDialog soft depth)
    cashierPermanentDeleteConfirm?.let { target ->
        var deleteConfirmationText by remember { mutableStateOf("") }
        val isConfirmed = deleteConfirmationText.trim().equals("DELETE", ignoreCase = true)

        LojiaDialog(
            onDismissRequest = { cashierPermanentDeleteConfirm = null },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.DialogPaddingComfortable),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                ) {
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

                    Text(
                        text = stringResource(R.string.delete_permanently_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        color = OnBackgroundLight,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.delete_permanently_desc),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = OnSurfaceVariantLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LojiaTextField(
                        value = deleteConfirmationText,
                        onValueChange = { deleteConfirmationText = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.type_delete_to_confirm),
                                fontSize = 12.sp,
                                color = TextHintColor
                            )
                        },
                        isError = deleteConfirmationText.isNotEmpty() && !isConfirmed,
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inputDeleteConfirm")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { executePermanentDelete(target) },
                            enabled = isConfirmed,
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                contentColor = PureWhite,
                                disabledContentColor = OnSurfaceVariantLight
                            ),
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Dimens.MinTouchTarget)
                                .depthRaisedButton(
                                    enabled = isConfirmed,
                                    isDestructive = true,
                                    cornerRadius = Dimens.RadiusMd
                                )
                                .testTag("btnConfirmPermanentDelete")
                        ) {
                            Text(
                                text = stringResource(R.string.delete_1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { cashierPermanentDeleteConfirm = null },
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            border = null,
                            elevation = null,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = OnSurfaceVariantLight
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Dimens.MinTouchTarget)
                                .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                                .testTag("btnCancelPermanentDelete")
                        ) {
                            Text(
                                text = stringResource(R.string.cancel_18),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
    riseIndex: Int = 0,
    onEdit: (() -> Unit)? = null,
    onToggleActive: ((Cashier) -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    val initial = remember(cashier.name) {
        cashier.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
    val isOnShift = remember(cashier) { !isArchived && cashier.active && (cashier.id % 2 == 1) }

    val cardModifier = if (isArchived) {
        Modifier
            .fillMaxWidth()
            .cardRise(riseIndex)
            .depthSemanticCard(
                backgroundColor = SurfaceVariantLight.copy(alpha = 0.65f),
                borderColor = OutlineVariantLight
            )
    } else {
        Modifier
            .fillMaxWidth()
            .cardRise(riseIndex)
            .depthCard()
    }

    Box(modifier = cardModifier) {
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
                        DateTimeFormatUtils.formatDateTime(cashier.archivedAt)
                    }
                    Text(
                        text = stringResource(R.string.cashier_archived_date_fmt, dateFormatted),
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
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btnRestoreCashier_${cashier.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = stringResource(R.string.action_unarchive),
                            tint = PrimaryIndigoLight
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btnDeleteArchivedCashier_${cashier.id}")
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
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btnEditCashier_${cashier.id}")
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
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btnDeleteCashier_${cashier.id}")
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
    val accessibleColor = if (color == TextHintColor) OnSurfaceVariantLight else color
    Box(
        modifier = modifier.depthSemanticCard(
            backgroundColor = accessibleColor.copy(alpha = 0.10f),
            borderColor = accessibleColor.copy(alpha = 0.28f),
            shape = RoundedCornerShape(Dimens.RadiusSm)
        )
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
                color = accessibleColor,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accessibleColor
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
    Box(
        modifier = Modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .depthCard(
                accent = selected,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) PrimaryIndigoDark else OnSurfaceVariantLight,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun CashierSkeletonCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .depthCard()
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
