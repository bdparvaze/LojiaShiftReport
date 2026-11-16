package com.lojia.shiftreport.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.common.LojiaDimens
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.PasswordSetupResult
import com.lojia.shiftreport.util.checkPasswordSetup
import com.lojia.shiftreport.util.getOrCreateDeviceKek
import com.lojia.shiftreport.util.saveVault
import com.lojia.shiftreport.util.seal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupPasswordCard(
    hasBackupPassword: Boolean,
    onManagePasswordClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(LojiaDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigoLight.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.backup_pw_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                        Text(
                            text = stringResource(R.string.backup_pw_card_desc),
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasBackupPassword) SuccessContainer else WarningContainer
                ) {
                    Text(
                        text = stringResource(
                            if (hasBackupPassword) R.string.backup_pw_status_set
                            else R.string.backup_pw_status_not_set
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBackupPassword) SuccessGreen else WarningOrange,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("badge_backup_password_status")
                    )
                }
            }

            Button(
                onClick = onManagePasswordClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_backup_password_manage")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (hasBackupPassword) R.string.backup_pw_action_change
                        else R.string.backup_pw_action_set
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun BackupPasswordSetupDialog(
    isChange: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Ephemeral Compose state only (cleared immediately on submit or dismiss; never persisted)
    var newPasswordText by remember { mutableStateOf("") }
    var confirmPasswordText by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val clearInputs = {
        newPasswordText = ""
        confirmPasswordText = ""
        newPasswordVisible = false
        confirmPasswordVisible = false
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                clearInputs()
                onDismiss()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = null,
                tint = PrimaryIndigoLight,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(
                    if (isChange) R.string.backup_pw_dialog_change_title
                    else R.string.backup_pw_dialog_set_title
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarningContainer,
                    border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.backup_pw_no_recovery_warning),
                            fontSize = 12.sp,
                            color = OnSurfaceLight,
                            lineHeight = 16.sp
                        )
                    }
                }

                if (isChange) {
                    Text(
                        text = stringResource(R.string.backup_pw_older_backups_notice),
                        fontSize = 12.sp,
                        color = TextSecondaryLight,
                        lineHeight = 16.sp
                    )
                }

                OutlinedTextField(
                    value = newPasswordText,
                    onValueChange = {
                        newPasswordText = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.backup_pw_new_label)) },
                    singleLine = true,
                    enabled = !isSaving,
                    visualTransformation = if (newPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false
                    ),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = stringResource(R.string.backup_pw_new_label)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_backup_pw_new")
                )

                OutlinedTextField(
                    value = confirmPasswordText,
                    onValueChange = {
                        confirmPasswordText = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.backup_pw_confirm_label)) },
                    singleLine = true,
                    enabled = !isSaving,
                    visualTransformation = if (confirmPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false
                    ),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = stringResource(R.string.backup_pw_confirm_label)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_backup_pw_confirm")
                )

                if (!errorMessage.isNullOrEmpty()) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = ErrorRedLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newChars = newPasswordText.toCharArray()
                    val confirmChars = confirmPasswordText.toCharArray()
                    val validation = checkPasswordSetup(newChars, confirmChars)
                    if (validation != PasswordSetupResult.OK) {
                        newChars.fill('\u0000')
                        confirmChars.fill('\u0000')
                        errorMessage = when (validation) {
                            PasswordSetupResult.BLANK -> context.getString(R.string.backup_pw_error_blank)
                            PasswordSetupResult.TOO_SHORT -> context.getString(R.string.backup_pw_error_too_short)
                            PasswordSetupResult.MISMATCH -> context.getString(R.string.backup_pw_error_mismatch)
                            PasswordSetupResult.OK -> null
                        }
                        return@Button
                    }

                    clearInputs()
                    isSaving = true
                    coroutineScope.launch {
                        val outcome = withContext(Dispatchers.IO) {
                            try {
                                val deviceKek = getOrCreateDeviceKek()
                                if (deviceKek == null) {
                                    "DEVICE_KEY"
                                } else {
                                    val blob = seal(newChars, deviceKek)
                                    if (saveVault(context, blob)) "OK" else "SAVE_FAILED"
                                }
                            } finally {
                                newChars.fill('\u0000')
                                confirmChars.fill('\u0000')
                            }
                        }
                        isSaving = false
                        when (outcome) {
                            "OK" -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.backup_pw_saved_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSaved()
                            }
                            "DEVICE_KEY" -> {
                                errorMessage = context.getString(R.string.backup_pw_error_device_key)
                            }
                            else -> {
                                errorMessage = context.getString(R.string.backup_pw_error_save_failed)
                            }
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                modifier = Modifier.testTag("btn_backup_pw_save")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PureWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.backup_pw_save_button), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!isSaving) {
                        clearInputs()
                        onDismiss()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.testTag("btn_backup_pw_cancel")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun BackupPasswordRequiredDialog(
    onDismiss: () -> Unit,
    onSetPasswordClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.backup_pw_set_first_title),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Text(
                text = stringResource(R.string.backup_pw_set_first_message),
                fontSize = 13.sp,
                color = TextSecondaryLight,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onSetPasswordClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                modifier = Modifier.testTag("btn_backup_pw_gate_set")
            ) {
                Text(stringResource(R.string.backup_pw_action_set), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_backup_pw_gate_cancel")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun BackupRestorePasswordDialog(
    fileName: String?,
    isRestoring: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmitPassword: (String) -> Unit
) {
    // Ephemeral Compose state only (cleared immediately on submit or dismiss; never persisted)
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isRestoring) {
                passwordInput = ""
                passwordVisible = false
                onDismiss()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = PrimaryIndigoLight,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.backup_pw_restore_title),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.backup_pw_restore_prompt),
                    fontSize = 13.sp,
                    color = TextSecondaryLight,
                    lineHeight = 18.sp
                )
                if (!fileName.isNullOrBlank()) {
                    Text(
                        text = fileName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryIndigoLight
                    )
                }

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text(stringResource(R.string.backup_pw_enter_label)) },
                    singleLine = true,
                    enabled = !isRestoring,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = stringResource(R.string.backup_pw_enter_label)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_restore_backup_pw")
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = ErrorRedLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val submitted = passwordInput
                    passwordInput = ""
                    passwordVisible = false
                    onSubmitPassword(submitted)
                },
                enabled = !isRestoring && passwordInput.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                modifier = Modifier.testTag("btn_unlock_restore_backup")
            ) {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PureWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.backup_pw_unlock_restore_button),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!isRestoring) {
                        passwordInput = ""
                        passwordVisible = false
                        onDismiss()
                    }
                },
                enabled = !isRestoring,
                modifier = Modifier.testTag("btn_cancel_restore_pw")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
