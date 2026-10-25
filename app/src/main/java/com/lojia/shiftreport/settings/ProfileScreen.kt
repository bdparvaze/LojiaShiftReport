package com.lojia.shiftreport.settings

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.data.UpdateBusinessRequest
import com.lojia.shiftreport.data.UserProfile
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.ui.common.LojiaOutlinedButton
import com.lojia.shiftreport.ui.common.LojiaPrimaryButton
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.PdfReportGenerator
import com.lojia.shiftreport.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Material 3 ProfileScreen for Lojia POS & Shift Report.
 * Features:
 * 1. Matching Exact UI from User Reference (100% pixel-perfect layout).
 * 2. Top Blue App Bar with Menu/Back navigation and "Profile" title.
 * 3. Elevated Circular Avatar with initials & Camera Badge overlay.
 * 4. INLINE Expandable Editing (Accordion Style): Clicking any row or arrow expands the input fields
 *    and action buttons (Cancel & Save) directly below the row without opening external popups.
 * 5. High-contrast typography (OnSurfaceLight text) on clean containers.
 * 6. Interactive Country Selector & Phone Formatter.
 * 7. Integrated Google Maps location picker and Password strength meter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    reportViewModel: ReportViewModel,
    modifier: Modifier = Modifier,
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val userProfile by reportViewModel.userProfile.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()
    val savedAddresses by reportViewModel.savedAddresses.collectAsState()
    val addressCount = savedAddresses.size
    val navTo: (String) -> Unit = { route -> reportViewModel.selectReportSettingsMenu(route) }
    val bProfile = businessProfile ?: BusinessProfile()

    // Active expanded inline section: "name", "store_name", "vat", "address", "phone", "email", "password", or null
    var expandedSection by remember { mutableStateOf<String?>(null) }

    // Back handler: if any accordion is expanded, collapse it.
    // Otherwise, do NOT intercept back — let MainActivity handle it.
    BackHandler(enabled = expandedSection != null) {
        expandedSection = null
    }
    var showMapLocationPicker by remember { mutableStateOf(false) }

    var logoBitmap by remember(bProfile.logoUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(bProfile.logoUri) {
        val uri = bProfile.logoUri
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
                        file.outputStream().use { out -> stream.copyTo(out) }
                        withContext(Dispatchers.Main) {
                            reportViewModel.saveBusinessProfile(bProfile.copy(logoUri = file.absolutePath))
                            Toast.makeText(context, context.getString(R.string.logo_updated_msg), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Resolved permanent address & coordinates
    val permanentAddress = remember(userProfile?.address, bProfile.address) {
        val addr = userProfile?.address?.trim().orEmpty()
        if (addr.isNotBlank()) addr else bProfile.address.trim()
    }
    val currentLat = remember(userProfile?.mapLat, bProfile.mapLat) {
        if ((userProfile?.mapLat ?: 0.0) != 0.0) userProfile?.mapLat ?: 0.0 else bProfile.mapLat
    }
    val currentLng = remember(userProfile?.mapLng, bProfile.mapLng) {
        if ((userProfile?.mapLng ?: 0.0) != 0.0) userProfile?.mapLng ?: 0.0 else bProfile.mapLng
    }

    val currentAppCountry by reportViewModel.currentCountry.collectAsState()
    val rawStorePhone = remember(bProfile.phone, userProfile?.phone) {
        bProfile.phone.ifBlank { userProfile?.phone.orEmpty() }
    }
    val hasExplicitPhoneCountry = remember(rawStorePhone) {
        val clean = rawStorePhone.trim()
        clean.isNotBlank() && AppCountry.entries.any {
            clean.startsWith(it.dialCode) ||
                (clean.startsWith("+") && clean.removePrefix("+").startsWith(it.dialCode.removePrefix("+")))
        }
    }
    val (storePhoneCountry, storePhoneLocalDigits) = remember(rawStorePhone, currentAppCountry) {
        parsePhoneNumberWithCountry(rawStorePhone, currentAppCountry)
    }
    val storePhoneDisplay = remember(storePhoneCountry, storePhoneLocalDigits, rawStorePhone, hasExplicitPhoneCountry) {
        if (hasExplicitPhoneCountry && storePhoneLocalDigits.isNotBlank()) {
            "${countryCodeToFlagEmoji(storePhoneCountry.code)} ${storePhoneCountry.dialCode} $storePhoneLocalDigits"
        } else if (rawStorePhone.isNotBlank()) {
            rawStorePhone
        } else {
            "Not configured"
        }
    }

    val currentDisplayName = remember(userProfile?.fullName, userProfile?.username, bProfile.businessName) {
        userProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: userProfile?.username?.takeIf { it.isNotBlank() }
            ?: bProfile.businessName.takeIf { it.isNotBlank() }
            ?: "Demo Owner"
    }

    val initials = remember(currentDisplayName) {
        currentDisplayName
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "DO" }
    }

    val openPhotoPicker = {
        try {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: Exception) {
            Toast.makeText(context, e.localizedMessage ?: "Picker error", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PureWhite),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // =================================================================
            // 1. Top Avatar block (100.dp, PrimaryContainerLight, 2.dp border)
            // =================================================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainerLight)
                            .border(2.dp, PrimaryIndigoLight, CircleShape)
                            .clickable { openPhotoPicker() }
                            .testTag("avatar_profile_image"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap!!.asImageBitmap(),
                                contentDescription = stringResource(R.string.business_logo),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = initials,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigoLight
                            )
                        }
                    }

                    // Camera Icon Badge
                    Surface(
                        shape = CircleShape,
                        color = PrimaryIndigoLight,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(28.dp)
                            .clickable { openPhotoPicker() }
                            .testTag("btn_camera_avatar")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.cd_edit_photo),
                                tint = PureWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentDisplayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryContainerLight
                ) {
                    Text(
                        text = (userProfile?.currentRole ?: "ADMIN").uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigoDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { openPhotoPicker() },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, PrimaryIndigoLight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryIndigoLight
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.change_photo),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigoLight
                    )
                }
            }

            // =================================================================
            // 2. Clean Item List with Section Grouping & Accordion Editing
            // =================================================================
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // =============================================================
                // GROUP 1: PERSONAL INFORMATION
                // =============================================================
                ProfileSectionHeader(title = stringResource(R.string.personal_information_section))

                // -------------------------------------------------------------
                // ROW 1: Owner / Admin Name
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.Person,
                    iconTint = PrimaryIndigoLight,
                    title = stringResource(R.string.owner_admin_name_label),
                    value = userProfile?.fullName?.ifBlank { "Demo Owner" } ?: "Demo Owner",
                    valueColor = OnSurfaceLight,
                    valueFontSize = 13.sp,
                    valueFontWeight = FontWeight.Medium,
                    isExpanded = expandedSection == "name",
                    onToggle = {
                        expandedSection = if (expandedSection == "name") null else "name"
                    },
                    testTag = "row_owner_name"
                ) {
                    var tempName by remember(userProfile?.fullName) {
                        mutableStateOf(userProfile?.fullName ?: "")
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileDialogTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = stringResource(R.string.owner_admin_full_name_label),
                            placeholder = "e.g. Demo Owner",
                            testTag = "input_edit_full_name"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = stringResource(R.string.cancel),
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = stringResource(R.string.save),
                                onClick = {
                                    val current = userProfile ?: UserProfile()
                                    reportViewModel.saveUserProfile(current.copy(fullName = tempName.trim()))
                                    expandedSection = null
                                    Toast.makeText(context, context.getString(R.string.toast_owner_name_updated), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // ROW 2: Store Contact Email
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.Email,
                    iconTint = PrimaryIndigoLight,
                    title = stringResource(R.string.store_contact_email_label),
                    value = bProfile.email.ifBlank { userProfile?.email.orEmpty() }.ifBlank { "store@example.com" },
                    valueColor = PrimaryIndigoLight,
                    valueFontSize = 13.sp,
                    valueFontWeight = FontWeight.Medium,
                    isExpanded = expandedSection == "email",
                    onToggle = {
                        expandedSection = if (expandedSection == "email") null else "email"
                    },
                    testTag = "row_store_email"
                ) {
                    var tempEmail by remember(bProfile.email, userProfile?.email) {
                        mutableStateOf(bProfile.email.ifBlank { userProfile?.email.orEmpty() })
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileDialogTextField(
                            value = tempEmail,
                            onValueChange = { tempEmail = it },
                            label = stringResource(R.string.contact_email_address_label),
                            placeholder = "e.g. store@example.com",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            testTag = "input_edit_email"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = stringResource(R.string.cancel),
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = stringResource(R.string.save),
                                onClick = {
                                    val cleanMail = tempEmail.trim()
                                    val current = userProfile ?: UserProfile()
                                    reportViewModel.saveUserProfile(current.copy(email = cleanMail))
                                    reportViewModel.saveBusinessProfile(bProfile.copy(email = cleanMail))
                                    reportViewModel.updateBusiness(UpdateBusinessRequest(email = cleanMail))
                                    expandedSection = null
                                    Toast.makeText(context, context.getString(R.string.toast_email_updated), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // =============================================================
                // GROUP 2: STORE DETAILS
                // =============================================================
                ProfileSectionHeader(title = stringResource(R.string.store_details_section))

                // -------------------------------------------------------------
                // ROW 3: Store / Business Name
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.Store,
                    iconTint = OnSurfaceVariantLight,
                    title = stringResource(R.string.store_business_name),
                    value = bProfile.businessName.ifBlank { "Demo Store (Debug)" },
                    valueColor = OnSurfaceLight,
                    valueFontSize = 13.sp,
                    valueFontWeight = FontWeight.Medium,
                    isExpanded = expandedSection == "store_name",
                    onToggle = {
                        expandedSection = if (expandedSection == "store_name") null else "store_name"
                    },
                    testTag = "row_store_name"
                ) {
                    var tempStoreName by remember(bProfile.businessName) {
                        mutableStateOf(bProfile.businessName)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileDialogTextField(
                            value = tempStoreName,
                            onValueChange = { tempStoreName = it },
                            label = stringResource(R.string.store_business_name),
                            placeholder = "e.g. Demo Store (Debug)",
                            testTag = "input_edit_store_name"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = stringResource(R.string.cancel),
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = stringResource(R.string.save),
                                onClick = {
                                    val cleanName = tempStoreName.trim()
                                    reportViewModel.saveBusinessProfile(bProfile.copy(businessName = cleanName))
                                    reportViewModel.updateBusiness(UpdateBusinessRequest(businessName = cleanName))
                                    expandedSection = null
                                    Toast.makeText(context, context.getString(R.string.toast_store_name_updated), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // ROW 4: Tax Registration / VAT ID
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.ReceiptLong,
                    iconTint = OnSurfaceVariantLight,
                    title = stringResource(R.string.tax_vat_registration_number),
                    value = bProfile.vatNumber.ifBlank { stringResource(R.string.not_set) },
                    valueColor = OnSurfaceVariantLight,
                    valueFontSize = 12.sp,
                    valueFontFamily = FontFamily.Monospace,
                    isExpanded = expandedSection == "vat",
                    onToggle = {
                        expandedSection = if (expandedSection == "vat") null else "vat"
                    },
                    testTag = "row_vat_number"
                ) {
                    var tempVat by remember(bProfile.vatNumber) {
                        mutableStateOf(bProfile.vatNumber)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileDialogTextField(
                            value = tempVat,
                            onValueChange = { tempVat = it },
                            label = stringResource(R.string.tax_vat_registration_number),
                            placeholder = "e.g. 310000000000003",
                            testTag = "input_edit_vat_id"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = stringResource(R.string.cancel),
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = stringResource(R.string.save),
                                onClick = {
                                    val cleanVat = tempVat.trim()
                                    reportViewModel.saveBusinessProfile(bProfile.copy(vatNumber = cleanVat))
                                    expandedSection = null
                                    Toast.makeText(context, context.getString(R.string.toast_vat_id_updated), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // Saved Addresses Row
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.LocationOn,
                    label = stringResource(R.string.saved_addresses_title),
                    currentValue = stringResource(R.string.saved_addresses_count_format, addressCount),
                    onEditClick = { navTo("settings/profile/addresses") }
                )

                // -------------------------------------------------------------
                // ROW 5: Permanent Store Address (Google Maps)
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Filled.Place,
                    iconTint = OnSurfaceVariantLight,
                    title = stringResource(R.string.permanent_store_address_label),
                    value = if (permanentAddress.isNotBlank()) permanentAddress else "5000 Vista Del Lago Rd, Ukia...",
                    valueColor = OnSurfaceVariantLight,
                    valueFontSize = 12.sp,
                    valueMaxLines = 2,
                    isExpanded = expandedSection == "address",
                    onToggle = {
                        expandedSection = if (expandedSection == "address") null else "address"
                    },
                    testTag = "row_permanent_store_address"
                ) {
                    var tempAddr by remember(permanentAddress) { mutableStateOf(permanentAddress) }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileDialogTextField(
                            value = tempAddr,
                            onValueChange = { tempAddr = it },
                            label = stringResource(R.string.address),
                            placeholder = stringResource(R.string.permanent_store_address_placeholder),
                            testTag = "txt_permanent_address_field"
                        )

                        // Google Maps Live Picker Button
                        Surface(
                            onClick = { showMapLocationPicker = true },
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryContainerLight,
                            border = BorderStroke(1.dp, OutlineLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Map, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.select_location_on_map), fontWeight = FontWeight.SemiBold, color = PrimaryIndigoLight, fontSize = 13.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = stringResource(R.string.cancel),
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = stringResource(R.string.save),
                                onClick = {
                                    val cleanAddr = tempAddr.trim()
                                    val current = userProfile ?: UserProfile()
                                    reportViewModel.saveUserProfile(current.copy(address = cleanAddr))
                                    reportViewModel.saveBusinessProfile(bProfile.copy(address = cleanAddr))
                                    reportViewModel.updateBusiness(UpdateBusinessRequest(address = cleanAddr))
                                    expandedSection = null
                                    Toast.makeText(context, context.getString(R.string.toast_address_saved), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // ROW 6: Store Contact Phone (Country Code Selector + Digits)
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.Phone,
                    iconTint = OnSurfaceVariantLight,
                    title = stringResource(R.string.store_contact_phone_label),
                    value = storePhoneDisplay,
                    valueColor = OnSurfaceVariantLight,
                    valueFontSize = 13.sp,
                    isExpanded = expandedSection == "phone",
                    onToggle = {
                        expandedSection = if (expandedSection == "phone") null else "phone"
                    },
                    testTag = "row_store_phone"
                ) {
                    var tempPhoneCountry by remember(storePhoneCountry) { mutableStateOf(storePhoneCountry) }
                    var hasUserSelectedCountry by remember(hasExplicitPhoneCountry) { mutableStateOf(hasExplicitPhoneCountry) }
                    var tempPhoneDigits by remember(storePhoneLocalDigits) { mutableStateOf(storePhoneLocalDigits) }
                    var showCountryPickerModal by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Country Code Picker Button
                            Surface(
                                onClick = { showCountryPickerModal = true },
                                shape = RoundedCornerShape(10.dp),
                                color = BackgroundLight,
                                border = BorderStroke(1.dp, OutlineLight),
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("btn_select_phone_country")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (hasUserSelectedCountry) {
                                        Text(
                                            text = countryCodeToFlagEmoji(tempPhoneCountry.code),
                                            fontSize = 18.sp,
                                            color = OnSurfaceLight
                                        )
                                    }
                                    Text(
                                        text = if (hasUserSelectedCountry) tempPhoneCountry.dialCode else "Code",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurfaceLight
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = stringResource(R.string.cd_change_country),
                                        tint = OnSurfaceVariantLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Local Phone Digits Field
                            ProfileDialogTextField(
                                value = tempPhoneDigits,
                                onValueChange = { input ->
                                    tempPhoneDigits = input.filter { it.isDigit() || it == ' ' || it == '-' }
                                },
                                label = stringResource(R.string.phone_digits_label),
                                placeholder = "500360360",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                testTag = "input_phone_digits"
                            )
                        }

                        if (tempPhoneDigits.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryContainerLight,
                                border = BorderStroke(1.dp, OutlineLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val previewPrefix = if (hasUserSelectedCountry) "${tempPhoneCountry.dialCode} " else ""
                                Text(
                                    text = stringResource(R.string.phone_preview_format, "$previewPrefix${tempPhoneDigits.trim()}"),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigoLight,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = stringResource(R.string.cancel),
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = stringResource(R.string.save),
                                onClick = {
                                    val fullFormatted = if (tempPhoneDigits.isNotBlank()) {
                                        if (hasUserSelectedCountry) {
                                            "${tempPhoneCountry.dialCode} ${tempPhoneDigits.trim()}"
                                        } else {
                                            tempPhoneDigits.trim()
                                        }
                                    } else {
                                        ""
                                    }
                                    val current = userProfile ?: UserProfile()
                                    reportViewModel.saveUserProfile(current.copy(phone = fullFormatted))
                                    reportViewModel.saveBusinessProfile(bProfile.copy(phone = fullFormatted))
                                    reportViewModel.updateBusiness(UpdateBusinessRequest(phone = fullFormatted))
                                    expandedSection = null
                                    Toast.makeText(context, context.getString(R.string.toast_phone_saved), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (showCountryPickerModal) {
                            CountryCodePickerDialog(
                                selectedCountry = tempPhoneCountry,
                                onCountrySelected = { newCountry ->
                                    tempPhoneCountry = newCountry
                                    hasUserSelectedCountry = true
                                    showCountryPickerModal = false
                                },
                                onDismiss = { showCountryPickerModal = false }
                            )
                        }
                    }
                }

                // =============================================================
                // GROUP 3: SECURITY
                // =============================================================
                ProfileSectionHeader(title = stringResource(R.string.nav_security))

                // -------------------------------------------------------------
                // ROW 7: Change Password
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.Key,
                    iconTint = ErrorRedLight,
                    title = stringResource(R.string.change_password_title),
                    value = stringResource(R.string.change_password_desc),
                    valueColor = OnSurfaceVariantLight,
                    valueFontSize = 13.sp,
                    isExpanded = expandedSection == "password",
                    onToggle = {
                        expandedSection = if (expandedSection == "password") null else "password"
                    },
                    testTag = "row_change_password"
                ) {
                    var currentPassword by remember { mutableStateOf("") }
                    var newPassword by remember { mutableStateOf("") }
                    var confirmPassword by remember { mutableStateOf("") }

                    var currentPasswordVisible by remember { mutableStateOf(false) }
                    var newPasswordVisible by remember { mutableStateOf(false) }
                    var confirmPasswordVisible by remember { mutableStateOf(false) }

                    var currentPasswordError by remember { mutableStateOf<String?>(null) }
                    var newPasswordError by remember { mutableStateOf<String?>(null) }
                    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

                    val focusManager = LocalFocusManager.current
                    val currentPasswordHash = userProfile?.passwordHash.orEmpty()

                    val hasMinLength = newPassword.length >= 6
                    val hasUpper = newPassword.any { it.isUpperCase() }
                    val hasDigit = newPassword.any { it.isDigit() }
                    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }

                    val strengthScore = listOf(hasMinLength, hasUpper, hasDigit, hasSpecial).count { it }
                    val (strengthLabel, strengthColor, strengthProgress) = when {
                        newPassword.isEmpty() -> Triple("", OutlineVariantLight, 0f)
                        strengthScore <= 1 -> Triple(stringResource(R.string.auth_weak), ErrorRedLight, 0.25f)
                        strengthScore == 2 -> Triple(stringResource(R.string.auth_fair), WarningOrange, 0.5f)
                        strengthScore == 3 -> Triple(stringResource(R.string.auth_good), InfoBlue, 0.75f)
                        else -> Triple(stringResource(R.string.auth_strong), SuccessGreen, 1f)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentPasswordHash.isNotBlank()) {
                            ProfileDialogTextField(
                                value = currentPassword,
                                onValueChange = {
                                    currentPassword = it
                                    currentPasswordError = null
                                },
                                label = stringResource(R.string.current_password_label),
                                placeholder = stringResource(R.string.placeholder_enter_current_password),
                                isError = currentPasswordError != null,
                                supportingText = {
                                    currentPasswordError?.let { Text(it, color = ErrorRedLight, fontSize = 12.sp) }
                                },
                                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                trailingIcon = {
                                    IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                        Icon(
                                            imageVector = if (currentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (currentPasswordVisible) stringResource(R.string.cd_hide_password) else stringResource(R.string.cd_show_password),
                                            tint = OnSurfaceVariantLight
                                        )
                                    }
                                },
                                testTag = "input_current_password"
                            )
                        }

                        ProfileDialogTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                newPasswordError = null
                                if (confirmPassword.isNotEmpty() && it != confirmPassword) {
                                    confirmPasswordError = context.getString(R.string.doc_passwords_do_not_match)
                                } else {
                                    confirmPasswordError = null
                                }
                            },
                            label = stringResource(R.string.new_password_label),
                            placeholder = stringResource(R.string.placeholder_enter_new_password),
                            isError = newPasswordError != null,
                            supportingText = {
                                newPasswordError?.let { Text(it, color = ErrorRedLight, fontSize = 12.sp) }
                            },
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (newPasswordVisible) stringResource(R.string.cd_hide_password) else stringResource(R.string.cd_show_password),
                                        tint = OnSurfaceVariantLight
                                    )
                                }
                            },
                            testTag = "input_new_password"
                        )

                        if (newPassword.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.password_strength_label), style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariantLight, fontSize = 11.sp))
                                    Text(strengthLabel, style = MaterialTheme.typography.labelSmall.copy(color = strengthColor, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { strengthProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                                    color = strengthColor,
                                    trackColor = OutlineVariantLight
                                )
                            }
                        }

                        ProfileDialogTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                if (it.isNotEmpty() && it != newPassword) {
                                    confirmPasswordError = context.getString(R.string.doc_passwords_do_not_match)
                                } else {
                                    confirmPasswordError = null
                                }
                            },
                            label = stringResource(R.string.confirm_password_label),
                            placeholder = stringResource(R.string.placeholder_reenter_new_password),
                            isError = confirmPasswordError != null,
                            supportingText = {
                                confirmPasswordError?.let { Text(it, color = ErrorRedLight, fontSize = 12.sp) }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) stringResource(R.string.cd_hide_password) else stringResource(R.string.cd_show_password),
                                        tint = OnSurfaceVariantLight
                                    )
                                }
                            },
                            testTag = "input_confirm_password"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = stringResource(R.string.cancel),
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = stringResource(R.string.save),
                                onClick = {
                                    var hasError = false
                                    if (currentPasswordHash.isNotBlank()) {
                                        if (currentPassword.isBlank()) {
                                            currentPasswordError = context.getString(R.string.error_current_password_required)
                                            hasError = true
                                        } else if (!SecurityUtils.verifySecret(currentPassword, currentPasswordHash)) {
                                            currentPasswordError = context.getString(R.string.error_incorrect_current_password)
                                            hasError = true
                                        }
                                    }
                                    if (newPassword.length < 6) {
                                        newPasswordError = context.getString(R.string.error_password_min_6_chars)
                                        hasError = true
                                    }
                                    if (newPassword != confirmPassword) {
                                        confirmPasswordError = context.getString(R.string.doc_passwords_do_not_match)
                                        hasError = true
                                    }
                                    if (!hasError) {
                                        val updated = (userProfile ?: UserProfile()).copy(
                                            passwordHash = SecurityUtils.hashSecret(newPassword)
                                        )
                                        reportViewModel.saveUserProfile(updated)
                                        expandedSection = null
                                        Toast.makeText(context, context.getString(R.string.password_changed_success), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Google Maps Location Picker Modal
    if (showMapLocationPicker) {
        GoogleMapsLocationPickerModal(
            initialAddress = permanentAddress,
            initialLat = currentLat,
            initialLng = currentLng,
            onLocationConfirmed = { newAddr, newLat, newLng ->
                showMapLocationPicker = false
                val current = userProfile ?: UserProfile()
                val updatedUser = current.copy(
                    address = newAddr.ifBlank { current.address },
                    mapLat = newLat,
                    mapLng = newLng
                )
                reportViewModel.saveUserProfile(updatedUser)

                val updatedBiz = bProfile.copy(
                    address = newAddr.ifBlank { bProfile.address },
                    mapLat = newLat,
                    mapLng = newLng
                )
                reportViewModel.saveBusinessProfile(updatedBiz)
                reportViewModel.updateBusiness(
                    UpdateBusinessRequest(
                        address = newAddr,
                        mapLat = newLat,
                        mapLng = newLng
                    )
                )
                Toast.makeText(context, context.getString(R.string.toast_location_saved_maps), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showMapLocationPicker = false }
        )
    }
}

/**
 * Compatible alias for existing callers
 */
@Composable
fun EnterpriseProfileScreen(
    reportViewModel: ReportViewModel,
    modifier: Modifier = Modifier,
    onLogoutClick: () -> Unit = {}
) {
    ProfileScreen(
        reportViewModel = reportViewModel,
        modifier = modifier,
        onLogoutClick = onLogoutClick
    )
}
