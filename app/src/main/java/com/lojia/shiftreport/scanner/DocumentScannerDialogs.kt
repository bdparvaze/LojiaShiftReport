package com.lojia.shiftreport.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.common.DialogSize
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.theme.*

@Composable
fun SaveDocumentTitleDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val style = LocalDepthStyle.current
    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Text(
                text = stringResource(R.string.doc_save_title_dialog),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )
            Text(
                text = stringResource(R.string.doc_enter_title_desc),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceVariantLight
            )
            LojiaTextField(
                value = title,
                onValueChange = onTitleChange,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.doc_title_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_title_input")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = null,
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryIndigoDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = style.secondaryButtonBorderColor,
                            cornerRadius = Dimens.RadiusMd
                        )
                ) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onConfirm,
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                        .testTag("confirm_save_title_btn")
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ScannerFallbackPromptDialog(
    onCameraCapture: () -> Unit,
    onGalleryPick: () -> Unit,
    onDismiss: () -> Unit
) {
    val style = LocalDepthStyle.current
    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(Dimens.IconXl)
            )
            Text(
                text = stringResource(R.string.doc_capture_doc),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )
            Text(
                text = stringResource(R.string.doc_capture_desc),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceVariantLight
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onGalleryPick()
                    },
                    border = null,
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryIndigoDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = style.secondaryButtonBorderColor,
                            cornerRadius = Dimens.RadiusMd
                        )
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text(stringResource(R.string.doc_from_gallery), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        onDismiss()
                        onCameraCapture()
                    },
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text(stringResource(R.string.doc_open_camera), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RenameDocumentDialog(
    currentTitle: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTitle by remember { mutableStateOf(currentTitle) }
    val style = LocalDepthStyle.current

    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Text(
                text = stringResource(R.string.doc_rename_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )
            LojiaTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                singleLine = true,
                label = { Text(stringResource(R.string.doc_title_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_title_input")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = null,
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryIndigoDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = style.secondaryButtonBorderColor,
                            cornerRadius = Dimens.RadiusMd
                        )
                ) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onRename(newTitle.trim())
                        }
                    },
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DeleteDocumentConfirmDialog(
    documentTitle: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val style = LocalDepthStyle.current
    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = ErrorRedLight,
                modifier = Modifier.size(Dimens.IconXl)
            )
            Text(
                text = stringResource(R.string.doc_delete_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )
            Text(
                text = stringResource(R.string.doc_delete_confirm, documentTitle),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceVariantLight
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = null,
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryIndigoDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = style.secondaryButtonBorderColor,
                            cornerRadius = Dimens.RadiusMd
                        )
                ) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onConfirmDelete,
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(isDestructive = true, cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BatchDeleteConfirmDialog(
    count: Int,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val style = LocalDepthStyle.current
    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteSweep,
                contentDescription = null,
                tint = ErrorRedLight,
                modifier = Modifier.size(Dimens.IconXl)
            )
            Text(
                text = stringResource(R.string.doc_batch_delete_title, count),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )
            Text(
                text = stringResource(R.string.doc_batch_delete_confirm, count),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceVariantLight
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = null,
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryIndigoDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = style.secondaryButtonBorderColor,
                            cornerRadius = Dimens.RadiusMd
                        )
                ) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onConfirmDelete,
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(isDestructive = true, cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.doc_delete_all_selected), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OcrTextPreviewDialog(
    documentTitle: String,
    extractedText: String,
    isLoading: Boolean,
    onCopyText: (String) -> Unit,
    onShareWord: () -> Unit,
    onDismiss: () -> Unit
) {
    val style = LocalDepthStyle.current
    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Text(
                text = stringResource(R.string.doc_extracted_text_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Dimens.ScannerOcrDialogMaxHeight)
            ) {
                Text(
                    text = documentTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = PrimaryIndigo,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(bottom = Dimens.SpacingSm)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ScannerOcrLoadingHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                } else if (extractedText.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.MultilineFieldMinHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.doc_no_text_found),
                            color = OnSurfaceVariantLight,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = extractedText,
                            style = MaterialTheme.typography.titleMedium,
                            color = OnBackgroundLight
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (extractedText.isNotBlank()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = null,
                        elevation = null,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(
                                borderColor = style.secondaryButtonBorderColor,
                                cornerRadius = Dimens.RadiusMd
                            )
                    ) {
                        Text(stringResource(R.string.close))
                    }
                    OutlinedButton(
                        onClick = { onCopyText(extractedText) },
                        border = null,
                        elevation = null,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = PrimaryIndigoDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(
                                borderColor = style.secondaryButtonBorderColor,
                                cornerRadius = Dimens.RadiusMd
                            )
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                        Text(stringResource(R.string.copy))
                    }
                    Button(
                        onClick = onShareWord,
                        elevation = null,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                        Text(stringResource(R.string.doc_btn_share_docx))
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        elevation = null,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                    ) {
                        Text(stringResource(R.string.close), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordProtectDialog(
    documentTitle: String,
    onProtect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val style = LocalDepthStyle.current
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LojiaDialog(
        onDismissRequest = onDismiss,
        size = DialogSize.Small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Text(
                text = stringResource(R.string.doc_protect_pdf_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )

            Text(
                text = stringResource(R.string.doc_protect_pdf_desc, documentTitle),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceVariantLight
            )

            LojiaTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            LojiaTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.auth_confirm_pw)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = ErrorRedLight,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = null,
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryIndigoDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = style.secondaryButtonBorderColor,
                            cornerRadius = Dimens.RadiusMd
                        )
                ) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (password.length < 4) {
                            errorMessage = context.getString(R.string.doc_password_min_length)
                        } else if (password != confirmPassword) {
                            errorMessage = context.getString(R.string.doc_passwords_do_not_match)
                        } else {
                            onProtect(password)
                        }
                    },
                    elevation = null,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.doc_btn_protect_pdf), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
