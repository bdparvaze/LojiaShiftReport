package com.lojia.shiftreport.settings

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
    com.lojia.shiftreport.ui.common.LojiaDialog(
        onDismissRequest = { /* Modal must be chosen by user */ },
        size = com.lojia.shiftreport.ui.common.DialogSize.Small,
        shape = RoundedCornerShape(Dimens.DialogCornerRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLg)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.PinDialogIconSize)
                    .clip(CircleShape)
                    .background(PrimaryContainerLight)
                    .border(Dimens.BorderThin, OutlineLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(Dimens.IconLg)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Text(
                    text = stringResource(R.string.preserve_data_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.preserve_data_dialog_desc),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondaryLight,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
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
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconMd)
                        )
                        Text(
                            text = stringResource(R.string.btn_keep_data),
                            style = MaterialTheme.typography.labelLarge,
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
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconMd)
                        )
                        Text(
                            text = stringResource(R.string.btn_delete_everything),
                            style = MaterialTheme.typography.labelLarge,
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
    com.lojia.shiftreport.ui.common.LojiaDialog(
        onDismissRequest = {}, // Non-dismissible
        size = com.lojia.shiftreport.ui.common.DialogSize.Small,
        shape = RoundedCornerShape(Dimens.DialogCornerRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLg)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.PinDialogIconSize)
                    .clip(CircleShape)
                    .background(PrimaryContainerLight)
                    .border(Dimens.BorderThin, OutlineLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(Dimens.IconLg)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Text(
                    text = stringResource(R.string.restore_data_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.restore_data_dialog_desc),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondaryLight,
                    textAlign = TextAlign.Center
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
                            .padding(top = Dimens.SpacingXs)
                            .depthSemanticCard(
                                backgroundColor = BackgroundLight,
                                borderColor = OutlineLight,
                                shape = RoundedCornerShape(Dimens.RadiusSm)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.CardPadding),
                            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(Dimens.IconSm)
                                )
                                Text(
                                    text = backupFile.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceLight,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = preview,
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryIndigoLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
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
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.IconMd)
                            )
                            Text(
                                text = stringResource(R.string.btn_restore_from_backup),
                                style = MaterialTheme.typography.labelLarge,
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
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.IconMd)
                            )
                            Text(
                                text = stringResource(R.string.btn_browse_backups),
                                style = MaterialTheme.typography.labelLarge,
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
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondaryLight
                    )
                }
            }
        }
    }
}
