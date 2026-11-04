package com.lojia.shiftreport.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.AuthPinConstants
import com.lojia.shiftreport.auth.SixDigitPinInput
import com.lojia.shiftreport.data.Cashier
import com.lojia.shiftreport.data.UserProfile
import com.lojia.shiftreport.ui.theme.*

/**
 * Enterprise Add/Edit Cashier Modal Dialog (Foodics POS Standard)
 * Material 3 AlertDialog with 28dp corner radius and SixDigitPinInput.
 */
@Composable
fun AddCashierDialog(
    userProfile: UserProfile? = null,
    existingCashier: Cashier? = null,
    onDismissRequest: () -> Unit,
    onConfirmAdd: (name: String) -> Unit,
    onConfirmSave: ((name: String, pin: String, role: String) -> Unit)? = null
) {
    var nameInput by remember { mutableStateOf(existingCashier?.name.orEmpty()) }
    var pinInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(existingCashier?.role ?: "CASHIER") }
    var isNameError by remember { mutableStateOf(false) }

    val isEditing = existingCashier != null

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(28.dp),
        containerColor = SurfaceLight,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Outlined.PointOfSale else Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(if (isEditing) R.string.edit_cashier else R.string.add_cashier_4),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackgroundLight
                    )
                    Text(
                        text = stringResource(R.string.cashier_management_desc),
                        fontSize = 11.5.sp,
                        color = OnSurfaceVariantLight
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.cashier_name),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            if (isNameError && it.isNotBlank()) isNameError = false
                        },
                        placeholder = {
                            Text(
                                stringResource(R.string.eg_john_doe_sarah_1),
                                fontSize = 13.sp,
                                color = TextHintColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = if (isNameError) ErrorRedLight else PrimaryIndigoLight,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        isError = isNameError,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = OutlineLight,
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            errorBorderColor = ErrorRedLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_cashier_name")
                    )

                    if (isNameError) {
                        Text(
                            text = stringResource(R.string.cashier_name_required),
                            fontSize = 11.sp,
                            color = ErrorRedLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Role Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.cashier_role),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CASHIER" to R.string.cashier, "ADMIN" to R.string.administrator).forEach { (roleKey, strRes) ->
                            val selected = selectedRole.equals(roleKey, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) PrimaryContainerLight else SurfaceVariantLight,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) PrimaryIndigoLight else OutlineLight
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedRole = roleKey }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Badge,
                                        contentDescription = null,
                                        tint = if (selected) PrimaryIndigoLight else OnSurfaceVariantLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = stringResource(strRes),
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) PrimaryIndigoLight else OnSurfaceVariantLight
                                    )
                                }
                            }
                        }
                    }
                }

                // 6-digit PIN Configuration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.cashier_pin),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SixDigitPinInput(
                        pin = pinInput,
                        onPinChange = { pinInput = it },
                        isMasked = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cashier_six_digit_pin_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = nameInput.trim()
                    if (clean.isBlank()) {
                        isNameError = true
                    } else {
                        if (onConfirmSave != null) {
                            onConfirmSave(clean, pinInput.trim(), selectedRole)
                        } else {
                            onConfirmAdd(clean)
                        }
                        onDismissRequest()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btnSaveCashier")
            ) {
                Text(stringResource(if (isEditing) R.string.save_5 else R.string.add_cashier))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("btnCancelCashier")
            ) {
                Text(stringResource(R.string.cancel_18))
            }
        },
        modifier = Modifier.testTag("addCashierDialog")
    )
}
