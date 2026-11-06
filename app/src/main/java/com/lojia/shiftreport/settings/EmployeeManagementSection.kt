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
import kotlinx.coroutines.launch
import java.util.Locale

private val FoodicsBlue = Color(0xFF2563EB)
private val FoodicsBg = Color(0xFFF9FAFB)
private val FoodicsTextBlack = Color(0xFF111827)
private val FoodicsTextGray = Color(0xFF6B7280)
private val FoodicsBorderLight = Color(0xFFE5E7EB)
private val FoodicsGreen = Color(0xFF10B981)
private val FoodicsOrange = Color(0xFFF59E0B)

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
            .background(FoodicsBg)
    ) {
        // 1. HEADER - Top Section
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
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
                        text = "Employee Management",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FoodicsTextBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${employees.size} employees • $activeCount active",
                        fontSize = 14.sp,
                        color = FoodicsTextGray
                    )
                }

                Button(
                    onClick = { showAdminAuthForAdd = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FoodicsBlue,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Employee",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. STATS CARDS - 3 Cards Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmployeeStatCard(
                icon = Icons.Default.People,
                iconTint = FoodicsBlue,
                count = employees.size.toString(),
                label = "Total",
                modifier = Modifier.weight(1f)
            )
            EmployeeStatCard(
                icon = Icons.Default.CheckCircle,
                iconTint = FoodicsGreen,
                count = activeCount.toString(),
                label = "Active",
                modifier = Modifier.weight(1f)
            )
            EmployeeStatCard(
                icon = Icons.Default.Block,
                iconTint = FoodicsOrange,
                count = inactiveCount.toString(),
                label = "Inactive",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search by name or phone...",
                    color = FoodicsTextGray,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = FoodicsTextGray
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = FoodicsBlue,
                unfocusedBorderColor = FoodicsBorderLight
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. EMPLOYEE LIST OR 5. EMPTY STATE
        if (filteredEmployees.isEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                        tint = Color(0xFFD1D5DB),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (employees.isEmpty()) "No employees yet" else "No matching employees",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FoodicsTextBlack,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add your first employee with monthly salary to track advances",
                        fontSize = 14.sp,
                        color = FoodicsTextGray,
                        textAlign = TextAlign.Center
                    )
                    if (employees.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAdminAuthForAdd = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FoodicsBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Employee",
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
                filteredEmployees.forEach { emp ->
                    key(emp.id) {
                        EmployeeItemCard(
                            employee = emp,
                            onEditClick = { employeePendingEditAuth = emp }
                        )
                    }
                }
            }
        }
    }

    if (showAdminAuthForAdd) {
        AdminAuthDialog(
            subtitle = "Add Employee",
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
            subtitle = "Edit Employee: ${target.name}",
            onDismissRequest = { employeePendingEditAuth = null },
            onAuthSuccess = {
                employeePendingEditAuth = null
                editingEmployee = target
                showAddEditDialog = true
            }
        )
    }

    if (showAddEditDialog) {
        AddEditEmployeeDialog(
            existing = editingEmployee,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, salary, phone, isActive ->
                coroutineScope.launch {
                    val emp = editingEmployee?.copy(
                        name = name,
                        monthlySalary = salary,
                        phone = phone,
                        isActive = isActive
                    ) ?: Employee(
                        name = name,
                        monthlySalary = salary,
                        phone = phone,
                        isActive = isActive,
                        createdByAdminId = adminId
                    )
                    employeeDao.insert(emp)
                    showAddEditDialog = false
                    Toast.makeText(
                        context,
                        if (editingEmployee != null) "Employee updated" else "Employee saved",
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
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.height(80.dp)
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
                    color = FoodicsTextBlack
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = FoodicsTextGray,
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
    onEditClick: () -> Unit
) {
    val initial = employee.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, FoodicsBorderLight),
        modifier = Modifier.fillMaxWidth()
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
                    .background(Color(0xFFDBEAFE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FoodicsBlue
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
                        color = FoodicsTextBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (employee.isActive) Color(0xFFD1FAE5) else Color(0xFFF3F4F6)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (employee.isActive) "Active" else "Inactive",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (employee.isActive) Color(0xFF065F46) else FoodicsTextGray
                        )
                    }
                }

                Text(
                    text = String.format(Locale.US, "SAR %,.2f / month", employee.monthlySalary),
                    fontSize = 14.sp,
                    color = FoodicsTextGray
                )

                if (!employee.phone.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = FoodicsTextGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = employee.phone,
                            fontSize = 13.sp,
                            color = FoodicsTextGray
                        )
                    }
                }
            }

            // Right: Edit IconButton
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Employee",
                    tint = FoodicsBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddEditEmployeeDialog(
    existing: Employee?,
    onDismiss: () -> Unit,
    onSave: (name: String, salary: Double, phone: String?, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var salaryText by remember {
        mutableStateOf(
            existing?.monthlySalary?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: ""
        )
    }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

    val salaryVal = salaryText.toDoubleOrNull()
    val canSave = name.isNotBlank() && salaryVal != null && salaryVal > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing != null) "Edit Employee" else "Add New Employee",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Employee Name *") },
                    placeholder = { Text("e.g., Rahim") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it },
                    label = { Text("Monthly Salary (SAR) *") },
                    placeholder = { Text("e.g., 1500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("SAR") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (existing != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Status",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = salaryText.toDoubleOrNull() ?: return@Button
                    onSave(name.trim(), s, phone.trim().ifBlank { null }, isActive)
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = FoodicsBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
