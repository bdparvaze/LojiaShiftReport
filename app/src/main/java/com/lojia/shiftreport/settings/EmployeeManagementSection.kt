package com.lojia.shiftreport.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

    val depthStyle = LocalDepthStyle.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(depthStyle.reportPageBackground)
    ) {
        // 1. HEADER - Top Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd)
                .depthCard(cornerRadius = Dimens.CardCornerRadius)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.ReportCardInnerPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = Dimens.SpacingMd)) {
                    Text(
                        text = stringResource(R.string.nav_employee),
                        fontSize = DepthTypeTokens.TopBarTitleSize,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryReport,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                    Text(
                        text = stringResource(R.string.employees_count_format, employees.size, activeCount),
                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
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
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    modifier = Modifier.depthRaisedButton(
                        cornerRadius = Dimens.RadiusReportInput,
                        minHeight = Dimens.ButtonHeightPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSm)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text(
                        text = stringResource(R.string.empty_employee_add_button),
                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.ReportGridGap))

        // 2. STATS CARDS - 3 Cards Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ReportGridGap)
        ) {
            EmployeeStatCard(
                icon = Icons.Default.People,
                iconTint = PrimaryBlueLight,
                tileColors = listOf(PureWhite, BadgeBlueBg),
                count = employees.size.toString(),
                label = stringResource(R.string.total),
                index = 0,
                modifier = Modifier.weight(1f)
            )
            EmployeeStatCard(
                icon = Icons.Default.CheckCircle,
                iconTint = BadgeGreenText,
                tileColors = listOf(PureWhite, BadgeGreenBg),
                count = activeCount.toString(),
                label = stringResource(R.string.active),
                index = 1,
                modifier = Modifier.weight(1f)
            )
            EmployeeStatCard(
                icon = Icons.Default.Block,
                iconTint = BadgeAmberAccent,
                tileColors = listOf(PureWhite, BadgeAmberBg),
                count = inactiveCount.toString(),
                label = stringResource(R.string.inactive),
                index = 2,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingLg))

        // 3. SEARCH BAR
        LojiaTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = stringResource(R.string.employee_search_hint),
                    color = depthStyle.reportFieldPlaceholderColor,
                    fontSize = DepthTypeTokens.InsetPlaceholderSize
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = OnSurfaceVariantLight,
                    modifier = Modifier.size(Dimens.IconField)
                )
            },
            shape = RoundedCornerShape(Dimens.RadiusReportInput),
            minHeight = Dimens.TextFieldHeightReport,
            borderColorOverride = depthStyle.reportFieldBorderColor,
            topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
            placeholderColor = depthStyle.reportFieldPlaceholderColor,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg)
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingLg))

        // 4. EMPLOYEE LIST OR 5. EMPTY STATE
        if (filteredEmployees.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm)
                    .depthCard(cornerRadius = Dimens.CardCornerRadius)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpacingHuge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.EmployeeAvatarTileSize + Dimens.SpacingLg)
                            .depthRaisedIconTile(
                                gradientColors = listOf(PureWhite, BadgeBlueBg),
                                cornerRadius = Dimens.RadiusEmployeeAvatarTile
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = PrimaryBlueLight,
                            modifier = Modifier.size(Dimens.IconLg)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.SpacingLg))
                    Text(
                        text = if (employees.isEmpty()) stringResource(R.string.empty_employee_title) else stringResource(R.string.empty_employee_no_match),
                        fontSize = DepthTypeTokens.EmptyStateTitleSize,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryReport,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    Text(
                        text = stringResource(R.string.empty_employee_hint),
                        fontSize = DepthTypeTokens.ListRowSubtitleSize,
                        color = OnSurfaceVariantLight,
                        textAlign = TextAlign.Center
                    )
                    if (employees.isEmpty()) {
                        Spacer(modifier = Modifier.height(Dimens.SpacingLg))
                        Button(
                            onClick = { showAdminAuthForAdd = true },
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                contentColor = PureWhite,
                                disabledContentColor = OnSurfaceVariantLight
                            ),
                            shape = RoundedCornerShape(Dimens.RadiusReportInput),
                            modifier = Modifier.depthRaisedButton(
                                cornerRadius = Dimens.RadiusReportInput,
                                minHeight = Dimens.ButtonHeightReportPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.IconSm)
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text(
                                text = stringResource(R.string.empty_employee_add_button),
                                fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
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
                    .padding(horizontal = Dimens.SpacingLg)
                    .padding(bottom = Dimens.SpacingXxl),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
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
    tileColors: List<Color>,
    count: String,
    label: String,
    index: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 80.dp)
            .cardRise(index)
            .depthCard(cornerRadius = Dimens.CardCornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.SectionIconTileSize)
                    .depthRaisedIconTile(
                        gradientColors = tileColors,
                        cornerRadius = Dimens.RadiusSectionIconTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(Dimens.IconSm)
                )
            }
            Column {
                Text(
                    text = count,
                    fontSize = DepthTypeTokens.StatCountSize,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryReport
                )
                Text(
                    text = label,
                    fontSize = DepthTypeTokens.ListRowSubtitleSize,
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
            .depthCard(cornerRadius = Dimens.CardCornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.ReportCardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: 44dp SH1 Gradient Avatar Tile
            Box(
                modifier = Modifier
                    .size(Dimens.EmployeeAvatarTileSize)
                    .depthRaisedIconTile(
                        gradientColors = listOf(PureWhite, BadgeBlueBg),
                        cornerRadius = Dimens.RadiusEmployeeAvatarTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = DepthTypeTokens.StatCountSize,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlueLight
                )
            }

            // Middle: Name + Status Chip, Salary, Phone
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Dimens.SettingsIconTitleGap, end = Dimens.SpacingSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = employee.name,
                        fontSize = DepthTypeTokens.ListRowTitleSize,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryReport,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.RadiusFull))
                            .background(
                                if (employee.isActive) CashWellBg else SurfaceVariantLight
                            )
                            .padding(horizontal = Dimens.ReportGridGap, vertical = Dimens.SpacingXxs)
                    ) {
                        Text(
                            text = if (employee.isActive) stringResource(R.string.active) else stringResource(R.string.inactive),
                            fontSize = DepthTypeTokens.ChipLabelSize,
                            fontWeight = FontWeight.SemiBold,
                            color = if (employee.isActive) BadgeBlueText else OnSurfaceVariantLight
                        )
                    }
                }

                Text(
                    text = stringResource(
                        R.string.employee_salary_per_month_format,
                        com.lojia.shiftreport.util.MoneyFormat.formatMinor(employee.monthlySalaryMinor, activeCurrency)
                    ),
                    fontSize = DepthTypeTokens.ListRowSubtitleSize,
                    color = OnSurfaceVariantLight
                )

                if (!employee.phone.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(Dimens.IconXs)
                        )
                        Text(
                            text = employee.phone,
                            fontSize = DepthTypeTokens.ListRowSubtitleSize,
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
                        .size(Dimens.SerialBadgeSize)
                        .depthRaisedIconTile(
                            gradientColors = listOf(PureWhite, CashWellBg),
                            cornerRadius = Dimens.RadiusActionIconTile
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.employee_edit_label),
                        tint = PrimaryBlueLight,
                        modifier = Modifier.size(Dimens.IconSm)
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

    val depthStyle = LocalDepthStyle.current

    LojiaDialog(
        onDismissRequest = onDismiss,
        size = com.lojia.shiftreport.ui.common.DialogSize.Medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Text(
                text = if (existing != null) stringResource(R.string.employee_edit_label) else stringResource(R.string.employee_add_new_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryReport
            )

            val employeeNameLabel = stringResource(R.string.employee_name_label)
            LojiaTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(employeeNameLabel) },
                contentDescription = employeeNameLabel,
                placeholder = { Text(stringResource(R.string.employee_name_hint)) },
                isError = isDuplicateName,
                supportingText = if (isDuplicateName) {
                    { Text(stringResource(R.string.employee_name_exists_error)) }
                } else null,
                shape = RoundedCornerShape(Dimens.RadiusReportInput),
                minHeight = Dimens.TextFieldHeightReport,
                borderColorOverride = depthStyle.reportFieldBorderColor,
                topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val monthlySalaryLabel = stringResource(R.string.monthly_salary_label)
            LojiaTextField(
                value = salaryText,
                onValueChange = { salaryText = it },
                label = { Text(monthlySalaryLabel) },
                contentDescription = monthlySalaryLabel,
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
                shape = RoundedCornerShape(Dimens.RadiusReportInput),
                minHeight = Dimens.TextFieldHeightReport,
                borderColorOverride = depthStyle.reportFieldBorderColor,
                topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val phoneOptionalLabel = stringResource(R.string.phone_optional_label)
            LojiaTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(phoneOptionalLabel) },
                contentDescription = phoneOptionalLabel,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(Dimens.RadiusReportInput),
                minHeight = Dimens.TextFieldHeightReport,
                borderColorOverride = depthStyle.reportFieldBorderColor,
                topShadowAlphaOverride = depthStyle.reportFieldInsetAlpha,
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
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryReport
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
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthRaisedButton(
                            enabled = canSave,
                            cornerRadius = Dimens.RadiusReportInput,
                            minHeight = Dimens.ButtonHeightReportPrimary
                        )
                ) {
                    Text(
                        stringResource(R.string.save),
                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                        fontWeight = FontWeight.SemiBold
                    )
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
                    shape = RoundedCornerShape(Dimens.RadiusReportInput),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthSecondaryButton(
                            cornerRadius = Dimens.RadiusReportInput,
                            minHeight = Dimens.ButtonHeightReportPrimary
                        )
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        fontSize = DepthTypeTokens.PrimaryCtaSmallSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.SpacingXs),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { dismissAction() }
                Box(modifier = Modifier.weight(1f)) { confirmAction() }
            }
        }
    }
}
