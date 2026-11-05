package com.lojia.shiftreport.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.data.Employee
import com.lojia.shiftreport.data.PreferencesRepository
import kotlinx.coroutines.launch
import java.util.Locale

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
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Employees (${employees.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Unlimited payroll & advance roster",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAdminAuthForAdd = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Employee")
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search employee by name or phone...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredEmployees.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = if (employees.isEmpty()) "Add your first employee" else "No matching employees",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (employees.isEmpty()) {
                        Button(onClick = { showAdminAuthForAdd = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Employee")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredEmployees, key = { it.id }) { emp ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (emp.isActive) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emp.name.take(1).uppercase(Locale.getDefault()),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = emp.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!emp.isActive) {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text("Inactive", fontSize = 10.sp) },
                                                modifier = Modifier.height(22.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = String.format(Locale.US, "Salary: %.2f SAR", emp.monthlySalary),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!emp.phone.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Phone,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = emp.phone,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = { employeePendingEditAuth = emp }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Employee",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Admin Auth for Add
    if (showAdminAuthForAdd) {
        AdminAuthDialog(
            onDismissRequest = { showAdminAuthForAdd = false },
            onAuthSuccess = {
                showAdminAuthForAdd = false
                editingEmployee = null
                showAddEditDialog = true
            }
        )
    }

    // Admin Auth for Edit
    employeePendingEditAuth?.let { targetEmp ->
        AdminAuthDialog(
            onDismissRequest = { employeePendingEditAuth = null },
            onAuthSuccess = {
                employeePendingEditAuth = null
                editingEmployee = targetEmp
                showAddEditDialog = true
            }
        )
    }

    // Add / Edit Employee Dialog
    if (showAddEditDialog) {
        var name by remember(editingEmployee) { mutableStateOf(editingEmployee?.name ?: "") }
        var salaryText by remember(editingEmployee) {
            mutableStateOf(editingEmployee?.monthlySalary?.toString() ?: "")
        }
        var phone by remember(editingEmployee) { mutableStateOf(editingEmployee?.phone ?: "") }
        var isActive by remember(editingEmployee) { mutableStateOf(editingEmployee?.isActive ?: true) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (editingEmployee == null) "Add Employee" else "Edit Employee",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMsg = null },
                        label = { Text("Employee Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = salaryText,
                        onValueChange = { salaryText = it; errorMsg = null },
                        label = { Text("Monthly Salary (SAR) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (editingEmployee != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Employee", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                    }
                    if (errorMsg != null) {
                        Text(
                            text = errorMsg ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val salary = salaryText.trim().toDoubleOrNull()
                        if (trimmedName.isEmpty()) {
                            errorMsg = "Employee name is required"
                            return@Button
                        }
                        if (salary == null || salary < 0.0) {
                            errorMsg = "Enter a valid monthly salary"
                            return@Button
                        }
                        coroutineScope.launch {
                            val existing = editingEmployee
                            if (existing == null) {
                                employeeDao.insert(
                                    Employee(
                                        name = trimmedName,
                                        monthlySalary = salary,
                                        phone = phone.trim().ifBlank { null },
                                        isActive = true,
                                        createdByAdminId = adminId
                                    )
                                )
                                Toast.makeText(context, "Employee added", Toast.LENGTH_SHORT).show()
                            } else {
                                employeeDao.update(
                                    existing.copy(
                                        name = trimmedName,
                                        monthlySalary = salary,
                                        phone = phone.trim().ifBlank { null },
                                        isActive = isActive
                                    )
                                )
                                Toast.makeText(context, "Employee updated", Toast.LENGTH_SHORT).show()
                            }
                            showAddEditDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
