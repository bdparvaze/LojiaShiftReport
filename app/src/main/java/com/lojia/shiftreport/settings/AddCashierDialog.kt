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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag

/**
 * Standard Professional Add Cashier Modal Dialog - Confined with Admin Password verification
 */
@Composable
fun AddCashierDialog(
    userProfile: UserProfile?,
    onDismissRequest: () -> Unit,
    onConfirmAdd: (name: String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val passwordRequiredMsg = stringResource(R.string.password_required)
    val incorrectPasswordMsg = stringResource(R.string.incorrect_password)

    LojiaDialog(
        onDismissRequest = onDismissRequest,
        maxWidth = Dimens.DialogMaxWidth,
        shape = RoundedCornerShape(Dimens.DialogCornerRadius),
        containerColor = SurfaceLight,
        border = BorderStroke(1.dp, OutlineLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // =============================================================
            // 1. HEADER (Solid PrimaryIndigoDark — no gradient)
            // =============================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryIndigoDark)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryContainerLight)
                            .border(1.dp, OutlineLight, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PointOfSale,
                            contentDescription = null,
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.add_cashier_4),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = stringResource(R.string.add_cashiers_here_to),
                            fontSize = 12.sp,
                            color = PrimaryContainerLight
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainerLight)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cancel_18),
                        tint = PrimaryIndigoDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // =============================================================
                // 2. FORM FIELDS
                // =============================================================
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // --- Cashier Name ---
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.cashier_name),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceLight
                        )

                        LojiaTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                if (isError && it.isNotBlank()) isError = false
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
                                    tint = if (isError) ErrorRedLight else PrimaryIndigoLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            isError = isError,
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigoLight,
                                unfocusedBorderColor = OutlineLight,
                                focusedContainerColor = BackgroundLight,
                                unfocusedContainerColor = BackgroundLight,
                                errorBorderColor = ErrorRedLight
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("input_cashier_name")
                        )

                        if (isError) {
                            Surface(
                                color = ErrorContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.cashier_name_required),
                                    fontSize = 11.sp,
                                    color = ErrorRedLight,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // --- Password Confirmation ---
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (passwordError != null) ErrorRedLight else PrimaryIndigoLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.confirm_with_password),
                                fontSize = 12.sp,
                                  fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceLight
                            )
                        }

                        LojiaTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                if (passwordError != null) passwordError = null
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.confirm_with_password),
                                    fontSize = 13.sp,
                                    color = TextHintColor
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (passwordError != null) ErrorRedLight else PrimaryIndigoLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                val desc = if (passwordVisible) "Hide password" else "Show password"
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = icon, contentDescription = desc, modifier = Modifier.size(18.dp))
                                }
                            },
                            isError = passwordError != null,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigoLight,
                                unfocusedBorderColor = OutlineLight,
                                focusedContainerColor = BackgroundLight,
                                unfocusedContainerColor = BackgroundLight,
                                errorBorderColor = ErrorRedLight
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("input_confirm_password")
                        )

                        if (passwordError != null) {
                            Surface(
                                color = ErrorContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = passwordError ?: "",
                                    fontSize = 11.sp,
                                    color = ErrorRedLight,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // =============================================================
                // 3. ACTIONS
                // =============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryIndigoLight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceLight,
                            contentColor = PrimaryIndigoLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.ButtonHeightStandard)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel_18),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            val cleanName = nameInput.trim()
                            val cleanPassword = passwordInput
                            var hasError = false
                            if (cleanName.isBlank()) {
                                isError = true
                                hasError = true
                            }
                            if (cleanPassword.isBlank()) {
                                passwordError = passwordRequiredMsg
                                hasError = true
                            } else {
                                val verified = SecurityUtils.verifySecretWithUpgrade(cleanPassword, userProfile?.passwordHash.orEmpty()).isMatch
                                if (!verified) {
                                    passwordError = incorrectPasswordMsg
                                    hasError = true
                                }
                            }
                            if (!hasError) {
                                onConfirmAdd(cleanName)
                            }
                        },
                        enabled = nameInput.isNotBlank() && passwordInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryIndigoLight,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(Dimens.ButtonHeightStandard).testTag("btn_confirm_add_cashier")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.add_cashier_4),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
