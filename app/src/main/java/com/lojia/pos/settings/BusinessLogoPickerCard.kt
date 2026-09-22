package com.lojia.pos.settings

import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lojia.pos.data.BusinessProfile
import com.lojia.pos.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PresetLogoItem(
    val key: String,
    val iconEmoji: String,
    val nameResId: Int,
    val bgColors: List<Color>
)

val PRESET_LOGOS = listOf(
    PresetLogoItem("preset:superstore", "🏪", R.string.preset_superstore, listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))),
    PresetLogoItem("preset:retail", "🛍️", R.string.preset_retail, listOf(Color(0xFF10B981), Color(0xFF059669))),
    PresetLogoItem("preset:cafe", "☕", R.string.preset_cafe, listOf(Color(0xFFB45309), Color(0xFF78350F))),
    PresetLogoItem("preset:restaurant", "🍴", R.string.preset_restaurant, listOf(Color(0xFFF97316), Color(0xFFC2410C))),
    PresetLogoItem("preset:fashion", "💎", R.string.preset_fashion, listOf(Color(0xFFEC4899), Color(0xFFBE185D))),
    PresetLogoItem("preset:pharmacy", "⚕️", R.string.preset_pharmacy, listOf(Color(0xFF06B6D4), Color(0xFF0E7490))),
    PresetLogoItem("preset:tech", "⚡", R.string.preset_tech, listOf(Color(0xFF6366F1), Color(0xFF4338CA)))
)

@Composable
fun BusinessLogoPickerCard(
    businessProfile: BusinessProfile,
    onSaveProfile: (BusinessProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPresetDialog by remember { mutableStateOf(false) }

    var logoBitmap by remember(businessProfile.logoUri) {
        mutableStateOf<Bitmap?>(null)
    }

    val updatedMsg = stringResource(R.string.logo_updated_msg)
    val removedMsg = stringResource(R.string.logo_removed_msg)

    LaunchedEffect(businessProfile.logoUri) {
        val uri = businessProfile.logoUri
        if (uri.isBlank()) {
            logoBitmap = null
        } else {
            withContext(Dispatchers.IO) {
                logoBitmap = PdfReportGenerator.loadBusinessLogoBitmap(context, uri, 300)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        val file = File(context.filesDir, "biz_logo_${System.currentTimeMillis()}.png")
                        file.outputStream().use { out ->
                            stream.copyTo(out)
                        }
                        withContext(Dispatchers.Main) {
                            onSaveProfile(businessProfile.copy(logoUri = file.absolutePath))
                            Toast.makeText(context, updatedMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val primaryBrand = Color(0xFF00796B)
    var showOptionSheet by remember { mutableStateOf(false) }

    // Change photo options dialog
    if (showOptionSheet) {
        AlertDialog(
            onDismissRequest = { showOptionSheet = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOptionSheet = false }) {
                    Text(stringResource(R.string.close), color = Color(0xFF64748B))
                }
            },
            title = {
                Text(
                    text = "Change photo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gallery Upload
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showOptionSheet = false
                                try {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.localizedMessage ?: "Gallery error", Toast.LENGTH_SHORT).show()
                                }
                            },
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = primaryBrand,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.upload_logo),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    // Preset Logos
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showOptionSheet = false
                                showPresetDialog = true
                            },
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = primaryBrand,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.preset_logos),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    // Remove Photo (if set)
                    if (businessProfile.logoUri.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showOptionSheet = false
                                    onSaveProfile(businessProfile.copy(logoUri = ""))
                                    Toast.makeText(context, removedMsg, Toast.LENGTH_SHORT).show()
                                },
                            color = Color(0xFFFEF2F2)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = stringResource(R.string.remove_logo),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFDC2626),
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    // Centered Photo Selector (Matching Image 1)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("business_logo_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large Round Avatar Circle
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
                .clickable {
                    try {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } catch (e: Exception) {
                        showOptionSheet = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (logoBitmap != null) {
                Image(
                    bitmap = logoBitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.business_logo),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                val initial = businessProfile.businessName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "L"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF94A3B8)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 46.sp
                        )
                    )
                }
            }

            // Camera Icon centered inside circle (Matching Image 1)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Change photo",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // "Change photo" Label Link (Matching Image 1)
        Text(
            text = "Change photo",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = primaryBrand,
                fontSize = 18.sp
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showOptionSheet = true }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("btn_upload_logo")
        )
    }

    if (showPresetDialog) {
        PresetLogoPickerDialog(
            currentLogoKey = businessProfile.logoUri,
            onSelectPreset = { presetKey ->
                onSaveProfile(businessProfile.copy(logoUri = presetKey))
                showPresetDialog = false
                Toast.makeText(context, updatedMsg, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showPresetDialog = false }
        )
    }
}

/**
 * Modern Card container for grouping profile items with a section title.
 */
@Composable
fun ProfileGroupCard(
    title: String,
    icon: ImageVector,
    iconTint: Color = Color(0xFF00796B),
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        letterSpacing = 0.8.sp,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                color = Color(0xFFF1F5F9),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            content()
        }
    }
}

/**
 * Executive profile detail item with stylish rounded icon container, high-contrast typography,
 * and clear interactive tap target with edit indicator.
 */
@Composable
fun ProfileDetailItemRow(
    icon: ImageVector,
    label: String,
    value: String?,
    isLastItem: Boolean = false,
    onClick: () -> Unit
) {
    val displayValue = if (value.isNullOrBlank()) "—" else value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    AutoText(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayValue,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Edit badge
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit $label",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (!isLastItem) {
            HorizontalDivider(
                color = Color(0xFFF1F5F9),
                thickness = 0.8.dp,
                modifier = Modifier.padding(start = 68.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun PresetLogoPickerDialog(
    currentLogoKey: String,
    onSelectPreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val primaryBrand = Color(0xFF00796B)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(primaryBrand.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = primaryBrand,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.choose_preset_logo),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 17.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PRESET_LOGOS) { item ->
                        val isSelected = currentLogoKey == item.key
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) primaryBrand else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectPreset(item.key) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) primaryBrand.copy(alpha = 0.08f) else Color(0xFFF8FAFC)
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .shadow(3.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(item.bgColors)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.iconEmoji,
                                            fontSize = 22.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(item.nameResId),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) primaryBrand else Color(0xFF1E293B),
                                            fontSize = 12.sp
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(primaryBrand),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

