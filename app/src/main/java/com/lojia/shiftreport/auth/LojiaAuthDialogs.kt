package com.lojia.shiftreport.auth

import com.lojia.shiftreport.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.BuildConfig
import com.lojia.shiftreport.data.UserProfile

@Composable
fun LojiaPasswordRecoveryDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    userProfile: UserProfile?,
    isBn: Boolean,
    onAuthenticated: () -> Unit
) {
    if (!showDialog) return

    var recoveryAnswer by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    var isAnswerCorrect by remember { mutableStateOf(false) }

    val question = userProfile?.securityQuestion.orEmpty().ifEmpty {
        stringResource(R.string.auth_sq4)
    }
    val actualAnswer = userProfile?.securityAnswer.orEmpty().ifEmpty { "School" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.auth_password_recovery_title),
                fontWeight = FontWeight.Bold,
                color = LojiaColors.P600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.auth_answer_security_question),
                    fontSize = 12.sp,
                    color = LojiaColors.N600
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LojiaColors.P50,
                    border = BorderStroke(1.dp, LojiaColors.P100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(R.string.auth_question_label),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LojiaColors.P600
                        )
                        Text(
                            text = question,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = LojiaColors.N900
                        )
                    }
                }

                if (!isAnswerCorrect) {
                    LojiaInputField(
                        value = recoveryAnswer,
                        onValueChange = {
                            recoveryAnswer = it
                            recoveryError = null
                        },
                        label = stringResource(R.string.auth_answer_label),
                        placeholder = stringResource(R.string.auth_enter_answer_placeholder),
                        errorMessage = recoveryError,
                        validationState = if (recoveryError != null) FieldValidationState.ERROR else FieldValidationState.DEFAULT,
                        testTag = "recoveryAnswer"
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LojiaColors.OkBg,
                        border = BorderStroke(1.dp, LojiaColors.G200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.auth_identity_verified),
                                fontSize = 12.sp,
                                color = LojiaColors.G500,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.auth_recovery_success_msg),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = LojiaColors.N900
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val incorrectAnswerError = stringResource(R.string.auth_incorrect_answer)
            if (!isAnswerCorrect) {
                TextButton(
                    onClick = {
                        if (actualAnswer.isNotBlank() && recoveryAnswer.trim().equals(actualAnswer.trim(), ignoreCase = true)) {
                            isAnswerCorrect = true
                        } else {
                            recoveryError = incorrectAnswerError
                        }
                    }
                ) {
                    Text(stringResource(R.string.auth_verify_btn), color = LojiaColors.P500, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    onClick = {
                        onDismiss()
                        onAuthenticated()
                    }
                ) {
                    Text(stringResource(R.string.auth_complete_login_btn), color = LojiaColors.G500, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = LojiaColors.N500)
            }
        }
    )
}

@Composable
fun LojiaLanguageDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    isBn: Boolean,
    onSelectLanguage: (Boolean) -> Unit
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = LojiaColors.P600,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.auth_change_language_title),
                    fontWeight = FontWeight.Bold,
                    color = LojiaColors.P600,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = {
                        onSelectLanguage(true)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isBn) LojiaColors.P50 else Color.White,
                    border = BorderStroke(1.5.dp, if (isBn) LojiaColors.P500 else LojiaColors.N200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.lang_bengali),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBn) LojiaColors.P600 else Color.Black
                        )
                        if (isBn) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = LojiaColors.P600,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        onSelectLanguage(false)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isBn) LojiaColors.P50 else Color.White,
                    border = BorderStroke(1.5.dp, if (!isBn) LojiaColors.P500 else LojiaColors.N200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.lang_english),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!isBn) LojiaColors.P600 else Color.Black
                        )
                        if (!isBn) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = LojiaColors.P600,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = LojiaColors.N500)
            }
        }
    )
}
