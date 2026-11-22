package com.lojia.shiftreport.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.OfflineBackupManager
import java.io.File

/**
 * One-time first-run dialog asking the user whether to preserve data on uninstall.
 */
@Composable
fun DataPreservationSetupDialog(
    onChoice: (preserve: Boolean) -> Unit
) {
    LojiaDialog(
        onDismissRequest = { /* Modal must be chosen by user */ },
        maxWidth = Dimens.DialogSmallMaxWidth,
        shape = RoundedCornerShape(Dimens.DialogCornerRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainerLight)
                    .border(1.dp, OutlineLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.preserve_data_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.preserve_data_dialog_desc),
                    fontSize = 13.sp,
                    color = TextSecondaryLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onChoice(true) },
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                        .testTag("btn_keep_data")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.btn_keep_data),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onChoice(false) },
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = ErrorRedLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = ErrorRedLight,
                            cornerRadius = Dimens.RadiusMd
                        )
                        .testTag("btn_delete_everything")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.btn_delete_everything),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reinstall data recovery dialog asking whether to restore previous data or start fresh.
 */
@Composable
fun DataRecoveryDialog(
    backupFile: File?,
    previewText: String? = null,
    onRestore: () -> Unit,
    onBrowseBackups: (() -> Unit)? = null,
    onStartFresh: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    LojiaDialog(
        onDismissRequest = {}, // Non-dismissible
        maxWidth = Dimens.DialogSmallMaxWidth,
        shape = RoundedCornerShape(Dimens.DialogCornerRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainerLight)
                    .border(1.dp, OutlineLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.restore_data_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.restore_data_dialog_desc),
                    fontSize = 13.sp,
                    color = TextSecondaryLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                if (backupFile != null) {
                    val summary = remember(backupFile) {
                        try {
                            if (backupFile.exists() && backupFile.canRead()) {
                                OfflineBackupManager.parseBackupSummaryFromBytes(backupFile.readBytes()).getOrNull()
                            } else null
                        } catch (_: Exception) {
                            null
                        }
                    }

                    val resolvedPreview = when {
                        !previewText.isNullOrBlank() -> previewText
                        summary != null -> stringResource(
                            R.string.backup_shifts_ready_fmt,
                            summary.shiftReportsCount,
                            summary.totalRecords
                        )
                        else -> stringResource(R.string.backup_found_label)
                    }
                    val preview = resolvedPreview

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .depthSemanticCard(
                                backgroundColor = BackgroundLight,
                                borderColor = OutlineLight,
                                shape = RoundedCornerShape(Dimens.RadiusSm)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = backupFile.name,
                                    fontSize = 12.sp,
                                    color = OnSurfaceLight,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = preview,
                                fontSize = 11.5.sp,
                                color = PrimaryIndigoLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Primary: Restore from backup
                if (backupFile != null) {
                    Button(
                        onClick = onRestore,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            contentColor = PureWhite,
                            disabledContentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                            .testTag("btn_restore_from_backup")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_restore_from_backup),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 2. Outlined: Browse Backups
                if (onBrowseBackups != null) {
                    OutlinedButton(
                        onClick = onBrowseBackups,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = PrimaryIndigoLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthSecondaryButton(
                                borderColor = PrimaryIndigoLight,
                                cornerRadius = Dimens.RadiusMd
                            )
                            .testTag("btn_browse_backups")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_browse_backups),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 3. Text Button: Start Fresh
                TextButton(
                    onClick = onStartFresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .testTag("btn_start_fresh")
                ) {
                    Text(
                        text = stringResource(R.string.btn_start_fresh),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondaryLight
                    )
                }
            }
        }
    }
}
