package com.lojia.shiftreport.scanner

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.lojia.shiftreport.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.DateTimeFormatUtils
import com.lojia.shiftreport.util.calculateInSampleSize
import java.io.File
import java.util.*

@Composable
fun DocumentScannerHeaderCard(
    onScanClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraFallbackClick: () -> Unit
) {
    val style = LocalDepthStyle.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardRise(0)
            .depthCard(shape = RoundedCornerShape(Dimens.RadiusLg))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DocumentScanner,
                        contentDescription = null,
                        tint = PrimaryIndigoDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.doc_scanner_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.doc_scanner_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondaryLight,
                            fontSize = 12.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onScanClick,
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
                        .testTag("start_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_scan),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onCameraFallbackClick,
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryIndigoDark
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = style.secondaryButtonBorderColor,
                            cornerRadius = Dimens.RadiusMd
                        )
                        .testTag("camera_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.btn_camera),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onGalleryClick,
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryIndigoDark
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(
                            borderColor = style.secondaryButtonBorderColor,
                            cornerRadius = Dimens.RadiusMd
                        )
                        .testTag("import_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        tint = PrimaryIndigoDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.btn_gallery),
                        color = PrimaryIndigoDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ScannedDocumentItemCard(
    document: ScannedDocument,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onViewClick: () -> Unit,
    onSharePdfClick: () -> Unit,
    onShareDocxClick: () -> Unit,
    onExtractTextClick: () -> Unit,
    onPasswordProtectClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = remember(document.createdAtMillis) {
        DateTimeFormatUtils.formatDateTime(document.createdAtMillis)
    }
    val sizeStr = remember(document.fileSizeBytes, context) {
        val kb = document.fileSizeBytes / 1024.0
        val appLocale = DateTimeFormatUtils.getAppLocale()
        if (kb > 1024) {
            String.format(appLocale, context.getString(R.string.backup_size_mb_format), kb / 1024.0)
        } else {
            String.format(appLocale, context.getString(R.string.backup_size_kb_format), kb)
        }
    }

    val thumbnailBitmap = remember(document.thumbnailPath) {
        if (document.thumbnailPath.isNotBlank()) {
            val file = File(document.thumbnailPath)
            if (file.exists()) {
                try {
                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                    val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight)
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inJustDecodeBounds = false
                    }
                    BitmapFactory.decodeFile(file.absolutePath, decodeOptions)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .depthCard(accent = isSelected, shape = RoundedCornerShape(Dimens.RadiusMd))
            .clickable {
                if (isSelectionMode) {
                    onToggleSelect()
                } else {
                    onViewClick()
                }
            }
            .testTag("document_card_${document.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Thumbnail or Document Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariantLight),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        tint = ErrorRedLight,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryLight
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val pageStr = if (document.pageCount == 1) stringResource(R.string.doc_page_count_single, document.pageCount) else stringResource(R.string.doc_page_count_plural, document.pageCount)
                Text(
                    text = "$sizeStr • $pageStr • $dateStr",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryLight,
                        fontSize = 11.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isSelectionMode) {
                var menuExpanded by remember { mutableStateOf(false) }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.cd_options),
                            tint = TextSecondaryLight
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = PureWhite
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.doc_btn_view_pdf), color = OnSurfaceLight) },
                            leadingIcon = { Icon(Icons.Outlined.Visibility, contentDescription = null, tint = OnSurfaceVariantLight) },
                            onClick = {
                                menuExpanded = false
                                onViewClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.doc_btn_share_pdf), color = OnSurfaceLight) },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null, tint = OnSurfaceVariantLight) },
                            onClick = {
                                menuExpanded = false
                                onSharePdfClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.doc_btn_extract_text), color = OnSurfaceLight) },
                            leadingIcon = { Icon(Icons.Outlined.TextFields, contentDescription = null, tint = OnSurfaceVariantLight) },
                            onClick = {
                                menuExpanded = false
                                onExtractTextClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.doc_btn_share_docx), color = OnSurfaceLight) },
                            leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null, tint = OnSurfaceVariantLight) },
                            onClick = {
                                menuExpanded = false
                                onShareDocxClick()
                            }
                        )
                        // Disabled: output is not a valid PDF. Re-enable only with a real PDF encryption library.
                        /*
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.doc_btn_protect_pdf), color = OnSurfaceLight) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = OnSurfaceVariantLight) },
                            onClick = {
                                menuExpanded = false
                                onPasswordProtectClick()
                            }
                        )
                        */
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.doc_btn_rename), color = OnSurfaceLight) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = OnSurfaceVariantLight) },
                            onClick = {
                                menuExpanded = false
                                onRenameClick()
                            }
                        )
                        HorizontalDivider(color = OutlineVariantLight)
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.doc_btn_delete), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentScannerEmptyState(onScanClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainerLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DocumentScanner,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = stringResource(R.string.doc_no_documents_yet),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
            )
            Text(
                text = stringResource(R.string.doc_empty_import_desc),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondaryLight,
                    fontSize = 12.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onScanClick,
                elevation = null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(Dimens.RadiusMd),
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .depthRaisedButton(cornerRadius = Dimens.RadiusMd)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DocumentScanner,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.btn_scan_document),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
