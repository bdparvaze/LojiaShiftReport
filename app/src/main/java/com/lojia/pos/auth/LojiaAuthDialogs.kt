package com.lojia.pos.auth

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
import com.lojia.pos.BuildConfig
import com.lojia.pos.data.UserProfile

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
        LojiaStrings.get("sq4", isBn)
    }
    val actualAnswer = userProfile?.securityAnswer.orEmpty().ifEmpty { "School" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isBn) "পাসওয়ার্ড উদ্ধার" else "Password Recovery",
                fontWeight = FontWeight.Bold,
                color = LojiaColors.P600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isBn) "নিরাপত্তা প্রশ্নের উত্তর দিয়ে অ্যাকাউন্ট আনলক করুন:" else "Answer security question to unlock your account:",
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
                            text = if (isBn) "প্রশ্ন:" else "Question:",
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
                        label = if (isBn) "উত্তর" else "Answer",
                        placeholder = if (isBn) "উত্তর লিখুন" else "Enter answer",
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
                                text = if (isBn) "উত্তর সঠিক!" else "Identity Verified!",
                                fontSize = 12.sp,
                                color = LojiaColors.G500,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBn) "অ্যাকাউন্ট পুনরুদ্ধার সফল হয়েছে। এগিয়ে যেতে নিচের বোতাম চাপুন।" else "Security verification successful. Click below to continue.",
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
            if (!isAnswerCorrect) {
                TextButton(
                    onClick = {
                        if (actualAnswer.isNotBlank() && recoveryAnswer.trim().equals(actualAnswer.trim(), ignoreCase = true)) {
                            isAnswerCorrect = true
                        } else {
                            recoveryError = if (isBn) "ভুল উত্তর! আবার চেষ্টা করুন।" else "Incorrect answer! Please try again."
                        }
                    }
                ) {
                    Text(if (isBn) "যাচাই করুন" else "Verify", color = LojiaColors.P500, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    onClick = {
                        onDismiss()
                        onAuthenticated()
                    }
                ) {
                    Text(if (isBn) "লগইন সম্পন্ন করুন" else "Complete Login", color = LojiaColors.G500, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "বাতিল" else "Cancel", color = LojiaColors.N500)
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
                    text = if (isBn) "ভাষা পরিবর্তন করুন" else "Change Language",
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
                            text = "বাংলা (Bengali)",
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
                            text = "English",
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
                Text(if (isBn) "বাতিল" else "Cancel", color = LojiaColors.N500)
            }
        }
    )
}
