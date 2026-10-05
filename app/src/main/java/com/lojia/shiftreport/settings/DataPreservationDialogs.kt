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
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.theme.*
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
        shape = RoundedCornerShape(Dimens.DialogCornerRadius),
        containerColor = SurfaceLight,
        border = BorderStroke(1.dp, OutlineLight)
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
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigoLight,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeightStandard)
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
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ErrorRedLight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRedLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeightStandard)
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
    onRestore: () -> Unit,
    onBrowseBackups: (() -> Unit)? = null,
    onStartFresh: () -> Unit,
    onDismiss: () -> Unit
) {
    LojiaDialog(
        onDismissRequest = onDismiss,
        maxWidth = Dimens.DialogSmallMaxWidth,
        shape = RoundedCornerShape(Dimens.DialogCornerRadius),
        containerColor = SurfaceLight,
        border = BorderStroke(1.dp, OutlineLight)
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
                    Surface(
                        color = BackgroundLight,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, OutlineLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
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
                                fontSize = 11.sp,
                                color = OnSurfaceLight,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (backupFile != null) {
                    Button(
                        onClick = onRestore,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryIndigoLight,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ButtonHeightStandard)
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

                if (onBrowseBackups != null) {
                    OutlinedButton(
                        onClick = onBrowseBackups,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryIndigoLight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryIndigoLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ButtonHeightStandard)
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

                OutlinedButton(
                    onClick = onStartFresh,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, OutlineLight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OnSurfaceLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeightStandard)
                        .testTag("btn_start_fresh")
                ) {
                    Text(
                        text = stringResource(R.string.btn_start_fresh),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_cancel_restore")
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        fontSize = 13.sp,
                        color = TextSecondaryLight
                    )
                }
            }
        }
    }
}
