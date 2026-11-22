package com.lojia.shiftreport.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lojia.shiftreport.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.MoneyFormat
import com.lojia.shiftreport.util.MoneyInput
import com.lojia.shiftreport.util.classifyMoneyInput
import com.lojia.shiftreport.util.isValidMonthlySalary
import com.lojia.shiftreport.util.normalizeRestoreName
import kotlinx.coroutines.launch
import java.util.Locale

internal fun isDuplicateEmployeeName(
    employeeId: String?,
    candidateName: String,
    existingEmployees: List<Employee>
): Boolean {
    val normalizedCandidate = normalizeRestoreName(candidateName)
    if (normalizedCandidate.isEmpty()) return false
    return existingEmployees.any {
        it.id != employeeId && normalizeRestoreName(it.name) == normalizedCandidate
    }
}

@Composable
fun EmployeeManagementSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember(context) { AppDatabase.getInstance(context) }
    val employeeDao = remember(db) { db.employeeDao() }
    val prefs = remember(context) { PreferencesRepository.getInstance(context) }
    val adminId = remember(prefs) { prefs.getSavedUsername().ifBlank { "admin" } }

    val employees by employeeDao.getAll().collectAsState(initial = emptyList())
    val businessProfile by db.reportDao().getBusinessProfile().collectAsState(initial = null)
    val activeCurrency = businessProfile?.currency?.ifBlank {
        com.lojia.shiftreport.util.MoneyFormat.DEFAULT_CURRENCY_CODE
    } ?: com.lojia.shiftreport.util.MoneyFormat.DEFAULT_CURRENCY_CODE
    var searchQuery by remember { mutableStateOf("") }

    var showAdminAuthForAdd by remember { mutableStateOf(false) }
    var employeePendingEditAuth by remember { mutableStateOf<Employee?>(null) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<Employee?>(null) }

    val activeCount = remember(employees) { employees.count { it.isActive } }
    val inactiveCount = remember(employees) { employees.count { !it.isActive } }

    val filteredEmployees = remember(employees, searchQuery) {
        if (searchQuery.isBlank()) {
            employees
        } else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            employees.filter {
                it.name.lowercase(Locale.getDefault()).contains(q) ||
                    (it.phone?.lowercase(Locale.getDefault())?.contains(q) == true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundLight)
    ) {
        // 1. HEADER - Top Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .depthCard(shape = RoundedCornerShape(Dimens.RadiusLg))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.nav_employee),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackgroundLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.employees_count_format, employees.size, activeCount),
                        fontSize = 14.sp,
                        color = OnSurfaceVariantLight
                    )
                }

                Button(
                    onClick = { showAdminAuthForAdd = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier.depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.empty_employee_add_button),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. STATS CARDS - 3 Cards Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmployeeStatCard(
                icon = Icons.Default.People,
                iconTint = PrimaryBlueLight,
                count = employees.size.toString(),
                label = stringResource(R.string.total),
                index = 0,
                modifier = Modifier.weight(1f)
            )
            EmployeeStatCard(
                icon = Icons.Default.CheckCircle,
                iconTint = SuccessGreen,
                count = activeCount.toString(),
                label = stringResource(R.string.active),
                index = 1,
                modifier = Modifier.weight(1f)
            )
            EmployeeStatCard(
                icon = Icons.Default.Block,
                iconTint = WarningOrange,
                count = inactiveCount.toString(),
                label = stringResource(R.string.inactive),
                index = 2,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. SEARCH BAR
        LojiaTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = stringResource(R.string.employee_search_hint),
                    color = OnSurfaceVariantLight,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = OnSurfaceVariantLight
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. EMPLOYEE LIST OR 5. EMPTY STATE
        if (filteredEmployees.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .depthCard(shape = RoundedCornerShape(Dimens.RadiusLg))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = TextDisabledColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (employees.isEmpty()) stringResource(R.string.empty_employee_title) else stringResource(R.string.empty_employee_no_match),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackgroundLight,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.empty_employee_hint),
                        fontSize = 14.sp,
                        color = OnSurfaceVariantLight,
                        textAlign = TextAlign.Center
                    )
                    if (employees.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAdminAuthForAdd = true },
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                contentColor = PureWhite,
                                disabledContentColor = OnSurfaceVariantLight
                            ),
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            modifier = Modifier.depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.empty_employee_add_button),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredEmployees.forEachIndexed { index, emp ->
                    key(emp.id) {
                        EmployeeItemCard(
                            employee = emp,
                            activeCurrency = activeCurrency,
                            index = index,
                            onEditClick = { employeePendingEditAuth = emp }
                        )
                    }
                }
            }
        }
    }

    if (showAdminAuthForAdd) {
        AdminAuthDialog(
            subtitle = stringResource(R.string.empty_employee_add_button),
            onDismissRequest = { showAdminAuthForAdd = false },
            onAuthSuccess = {
                showAdminAuthForAdd = false
                editingEmployee = null
                showAddEditDialog = true
            }
        )
    }

    employeePendingEditAuth?.let { target ->
        AdminAuthDialog(
            subtitle = stringResource(R.string.employee_edit_subtitle_format, target.name),
            onDismissRequest = { employeePendingEditAuth = null },
            onAuthSuccess = {
                employeePendingEditAuth = null
                editingEmployee = target
                showAddEditDialog = true
            }
        )
    }

    if (showAddEditDialog) {
        val currentEditing = editingEmployee
        AddEditEmployeeDialog(
            existing = currentEditing,
            existingEmployees = employees,
            activeCurrency = activeCurrency,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, salaryMinor, phone, isActive ->
                coroutineScope.launch {
                    if (!isValidMonthlySalary(salaryMinor)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.employee_salary_required),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    val currentAll = employeeDao.getAllOnce()
                    val duplicateExists = isDuplicateEmployeeName(
                        employeeId = currentEditing?.id,
                        candidateName = name,
                        existingEmployees = currentAll
                    )
                    if (duplicateExists) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.employee_name_exists_error),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    val emp = currentEditing?.copy(
                        name = name,
                        monthlySalaryMinor = salaryMinor,
                        phone = phone,
                        isActive = isActive
                    ) ?: Employee(
                        name = name,
                        monthlySalaryMinor = salaryMinor,
                        phone = phone,
                        isActive = isActive,
                        createdByAdminId = adminId
                    )
                    employeeDao.insert(emp)
                    showAddEditDialog = false
                    Toast.makeText(
                        context,
                        context.getString(
                            if (currentEditing != null) R.string.employee_updated_toast
                            else R.string.employee_saved_toast
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

@Composable
private fun EmployeeStatCard(
    icon: ImageVector,
    iconTint: Color,
    count: String,
    label: String,
    index: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .cardRise(index)
            .depthCard(shape = RoundedCornerShape(Dimens.RadiusLg))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = count,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackgroundLight
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = OnSurfaceVariantLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmployeeItemCard(
    employee: Employee,
    activeCurrency: String,
    index: Int = 0,
    onEditClick: () -> Unit
) {
    val initial = employee.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardRise(index)
            .depthCard(shape = RoundedCornerShape(Dimens.RadiusLg))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Full 48dp Circle Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainerLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlueLight
                )
            }

            // Middle: Name + Status Chip, Salary, Phone
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = employee.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackgroundLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (employee.isActive) SuccessContainer else SurfaceVariantLight
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (employee.isActive) stringResource(R.string.active) else stringResource(R.string.inactive),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (employee.isActive) SuccessTextDark else OnSurfaceVariantLight
                        )
                    }
                }

                Text(
                    text = stringResource(
                        R.string.employee_salary_per_month_format,
                        com.lojia.shiftreport.util.MoneyFormat.formatMinor(employee.monthlySalaryMinor, activeCurrency)
                    ),
                    fontSize = 14.sp,
                    color = OnSurfaceVariantLight
                )

                if (!employee.phone.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = employee.phone,
                            fontSize = 13.sp,
                            color = OnSurfaceVariantLight
                        )
                    }
                }
            }

            // Right: Edit IconButton (48dp touch target)
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DepthTokens.CardAccentGradientStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.employee_edit_label),
                        tint = PrimaryBlueLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditEmployeeDialog(
    existing: Employee?,
    existingEmployees: List<Employee>,
    activeCurrency: String,
    onDismiss: () -> Unit,
    onSave: (name: String, salaryMinor: Long, phone: String?, isActive: Boolean) -> Unit
) {
    val activeDigits = MoneyFormat.minorUnitDigits(activeCurrency)
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var salaryText by remember {
        mutableStateOf(
            existing?.monthlySalaryMinor?.let {
                MoneyFormat.toPlainDecimal(it, activeDigits)
            } ?: ""
        )
    }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

    val isDuplicateName = remember(name, existingEmployees, existing) {
        isDuplicateEmployeeName(
            employeeId = existing?.id,
            candidateName = name,
            existingEmployees = existingEmployees
        )
    }

    val salaryClass = classifyMoneyInput(salaryText, activeDigits)
    val salaryMinor = (salaryClass as? MoneyInput.Valid)?.minor
    val isSalaryInvalid = salaryClass is MoneyInput.Invalid
    val isSalaryZero = salaryClass is MoneyInput.Valid && !isValidMonthlySalary(salaryClass.minor)
    val canSave = name.isNotBlank() && !isDuplicateName && salaryMinor != null && salaryMinor > 0L

    LojiaDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(Dimens.DialogPaddingComfortable),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existing != null) stringResource(R.string.employee_edit_label) else stringResource(R.string.employee_add_new_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )

            LojiaTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.employee_name_label)) },
                placeholder = { Text(stringResource(R.string.employee_name_hint)) },
                isError = isDuplicateName,
                supportingText = if (isDuplicateName) {
                    { Text(stringResource(R.string.employee_name_exists_error)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LojiaTextField(
                value = salaryText,
                onValueChange = { salaryText = it },
                label = { Text(stringResource(R.string.monthly_salary_label)) },
                placeholder = { Text(stringResource(R.string.employee_salary_hint)) },
                isError = isSalaryInvalid || isSalaryZero,
                supportingText = when {
                    isSalaryInvalid -> {
                        { Text(stringResource(R.string.valid_amount_error)) }
                    }
                    isSalaryZero -> {
                        { Text(stringResource(R.string.employee_salary_required)) }
                    }
                    else -> null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (activeDigits == 0) KeyboardType.Number else KeyboardType.Decimal
                ),
                suffix = { Text(activeCurrency) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LojiaTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.phone_optional_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (existing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.active_status_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }

            val confirmAction = @Composable {
                Button(
                    onClick = {
                        if (salaryClass !is MoneyInput.Valid) return@Button
                        val sMinor = salaryClass.minor
                        if (sMinor <= 0L) return@Button
                        onSave(name.trim(), sMinor, phone.trim().ifBlank { null }, isActive)
                    },
                    enabled = canSave,
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthRaisedButton(enabled = canSave, cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
            val dismissAction = @Composable {
                OutlinedButton(
                    onClick = onDismiss,
                    elevation = null,
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { dismissAction() }
                Box(modifier = Modifier.weight(1f)) { confirmAction() }
            }
        }
    }
}
