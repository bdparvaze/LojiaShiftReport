package com.lojia.shiftreport.settings

import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*

import androidx.core.content.FileProvider
import com.lojia.shiftreport.settings.ProfileImageCropper.ProfileImageCropperDialog
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
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.util.PdfReportGenerator
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
    PresetLogoItem("preset:superstore", "🏪", R.string.preset_superstore, listOf(PresetLogoSuperstoreStart, PresetLogoSuperstoreEnd)),
    PresetLogoItem("preset:retail", "🛍️", R.string.preset_retail, listOf(PresetLogoRetailStart, PresetLogoRetailEnd)),
    PresetLogoItem("preset:cafe", "☕", R.string.preset_cafe, listOf(PresetLogoCafeStart, PresetLogoCafeEnd)),
    PresetLogoItem("preset:restaurant", "🍴", R.string.preset_restaurant, listOf(PresetLogoRestaurantStart, PresetLogoRestaurantEnd)),
    PresetLogoItem("preset:fashion", "💎", R.string.preset_fashion, listOf(PresetLogoFashionStart, PresetLogoFashionEnd)),
    PresetLogoItem("preset:pharmacy", "⚕️", R.string.preset_pharmacy, listOf(PresetLogoPharmacyStart, PresetLogoPharmacyEnd)),
    PresetLogoItem("preset:tech", "⚡", R.string.preset_tech, listOf(PresetLogoTechStart, PresetLogoTechEnd))
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

    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var showCropperDialog by remember { mutableStateOf(false) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingCropUri = uri
            showCropperDialog = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraTempUri != null) {
            pendingCropUri = cameraTempUri
            showCropperDialog = true
        }
    }

    fun launchCamera() {
        try {
            val tempFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            cameraTempUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.logo_camera_error_fmt, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    val primaryBrand = PrimaryIndigoLight
    var showOptionSheet by remember { mutableStateOf(false) }

    // Change photo options dialog
    if (showOptionSheet) {
        LojiaDialog(
            onDismissRequest = { showOptionSheet = false },
            size = com.lojia.shiftreport.ui.common.DialogSize.Small
        ) {
            // Keep dismiss action before sheet title in source order per baseline contract
            val dismissAction = @Composable {
                OutlinedButton(
                    onClick = { showOptionSheet = false },
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.close), fontWeight = FontWeight.SemiBold)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Text(
                    text = stringResource(R.string.cd_change_photo),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    // Camera Option
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                            .depthCard(shape = RoundedCornerShape(Dimens.RadiusMd))
                            .clickable {
                                showOptionSheet = false
                                launchCamera()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.CardPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = primaryBrand,
                                modifier = Modifier.size(Dimens.IconAction)
                            )
                            Text(
                                text = stringResource(R.string.logo_take_photo_camera),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurfaceLight
                                )
                            )
                        }
                    }

                    // Gallery Upload
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                            .depthCard(shape = RoundedCornerShape(Dimens.RadiusMd))
                            .clickable {
                                showOptionSheet = false
                                try {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.localizedMessage ?: context.getString(R.string.gallery_error), Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.CardPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = primaryBrand,
                                modifier = Modifier.size(Dimens.IconAction)
                            )
                            Text(
                                text = stringResource(R.string.upload_logo),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurfaceLight
                                )
                            )
                        }
                    }

                    // Preset Logos
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                            .depthCard(shape = RoundedCornerShape(Dimens.RadiusMd))
                            .clickable {
                                showOptionSheet = false
                                showPresetDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.CardPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = primaryBrand,
                                modifier = Modifier.size(Dimens.IconAction)
                            )
                            Text(
                                text = stringResource(R.string.preset_logos),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurfaceLight
                                )
                            )
                        }
                    }

                    // Remove Photo (if set)
                    if (businessProfile.logoUri.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                                .depthSemanticCard(
                                    backgroundColor = ErrorContainer,
                                    borderColor = ErrorRedLight.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(Dimens.RadiusMd)
                                )
                                .clickable {
                                    showOptionSheet = false
                                    onSaveProfile(businessProfile.copy(logoUri = ""))
                                    Toast.makeText(context, removedMsg, Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(Dimens.CardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = ErrorRedLight,
                                    modifier = Modifier.size(Dimens.IconAction)
                                )
                                Text(
                                    text = stringResource(R.string.remove_logo),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = ErrorRedLight
                                    )
                                )
                            }
                        }
                    }
                }

                dismissAction()
            }
        }
    }

    // Centered Photo Selector (Matching Image 1)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingLg)
            .testTag("business_logo_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large Round Avatar Circle
        Box(
            modifier = Modifier
                .size(Dimens.BusinessLogoCircleSize)
                .clip(CircleShape)
                .background(OutlineVariantLight)
                .border(Dimens.AvatarBorderWidth, primaryBrand, CircleShape)
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
                        .background(PrimaryBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = DepthTypeTokens.BusinessLogoInitialSize
                        )
                    )
                }
            }

            // Camera Icon centered inside circle (Matching Image 1)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = stringResource(R.string.cd_change_photo),
                    tint = PureWhite,
                    modifier = Modifier.size(Dimens.IconXl)
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        // "Change photo" Label Link (Matching Image 1)
        Text(
            text = stringResource(R.string.cd_change_photo),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = primaryBrand
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.RadiusMd))
                .clickable { showOptionSheet = true }
                .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXs)
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

    if (showCropperDialog && pendingCropUri != null) {
        ProfileImageCropperDialog(
            imageUri = pendingCropUri!!,
            onCropSaved = { croppedUri ->
                val path = croppedUri.path ?: croppedUri.toString()
                onSaveProfile(businessProfile.copy(logoUri = path))
                PreferencesRepository.getInstance(context).saveBusinessLogoUri(path)
                Toast.makeText(context, updatedMsg, Toast.LENGTH_SHORT).show()
                showCropperDialog = false
                pendingCropUri = null
            },
            onDismiss = {
                showCropperDialog = false
                pendingCropUri = null
            }
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
    iconTint: Color = PrimaryIndigoLight,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .depthCard(shape = RoundedCornerShape(Dimens.RadiusLg))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.SpacingMd)
        ) {
            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingXs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.SectionIconTileSize)
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(Dimens.IconSm)
                    )
                }

                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariantLight,
                        letterSpacing = DepthTypeTokens.SettingsSectionHeaderTracking
                    )
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
            HorizontalDivider(
                color = OutlineVariantLight,
                thickness = Dimens.BorderThin,
                modifier = Modifier.padding(horizontal = Dimens.SpacingLg)
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))

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
            .defaultMinSize(minHeight = Dimens.ListRowMinHeight)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.ListRowPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.IconTile)
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .background(BackgroundLight)
                        .border(Dimens.BorderThin, OutlineLight, RoundedCornerShape(Dimens.RadiusMd)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(Dimens.IconMd)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextHintColor,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                    Text(
                        text = displayValue,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = OnSurfaceLight,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Edit badge
            Surface(
                color = SurfaceVariantLight,
                shape = RoundedCornerShape(Dimens.RadiusMd),
                modifier = Modifier.size(Dimens.SectionIconTileSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.cd_edit),
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(Dimens.IconXs)
                    )
                }
            }
        }

        if (!isLastItem) {
            HorizontalDivider(
                color = OutlineVariantLight,
                thickness = Dimens.RowDividerWidth,
                modifier = Modifier.padding(start = Dimens.ProfileDetailDividerInsetStart, end = Dimens.SpacingLg)
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
    val primaryBrand = PrimaryIndigoLight

    LojiaDialog(
        onDismissRequest = onDismiss,
        size = com.lojia.shiftreport.ui.common.DialogSize.Large
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.IconTile)
                            .clip(CircleShape)
                            .background(PrimaryContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = primaryBrand,
                            modifier = Modifier.size(Dimens.IconMd)
                        )
                    }
                    Text(
                        text = stringResource(R.string.choose_preset_logo),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = OnSurfaceVariantLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Dimens.PresetLogoGridMaxHeight),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                items(PRESET_LOGOS) { item ->
                    val isSelected = currentLogoKey == item.key
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.PresetLogoTileHeight)
                            .depthCard(
                                accent = isSelected,
                                shape = RoundedCornerShape(Dimens.RadiusMd)
                            )
                            .clickable { onSelectPreset(item.key) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(Dimens.SpacingSm),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.PresetLogoCircleSize)
                                        .shadow(Dimens.ShadowXs, CircleShape)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(item.bgColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.iconEmoji,
                                        fontSize = DepthTypeTokens.PresetLogoEmojiSize
                                    )
                                }
                                Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                                Text(
                                    text = stringResource(item.nameResId),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) primaryBrand else OnSurfaceLight
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
                                        .padding(Dimens.SpacingXs)
                                        .size(Dimens.IconAction)
                                        .clip(CircleShape)
                                        .background(primaryBrand),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(Dimens.IconXs)
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

