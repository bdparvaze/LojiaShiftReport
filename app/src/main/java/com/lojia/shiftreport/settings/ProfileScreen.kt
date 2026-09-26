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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
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
    val bProfile = businessProfile ?: BusinessProfile()

    // Back navigation to return to root settings
    BackHandler {
        reportViewModel.selectReportSettingsMenu("root")
    }

    // Active expanded inline section: "name", "store_name", "vat", "address", "phone", "email", "password", or null
    var expandedSection by remember { mutableStateOf<String?>(null) }
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
    val (storePhoneCountry, storePhoneLocalDigits) = remember(rawStorePhone, currentAppCountry) {
        parsePhoneNumberWithCountry(rawStorePhone, currentAppCountry)
    }
    val storePhoneDisplay = remember(storePhoneCountry, storePhoneLocalDigits, rawStorePhone) {
        if (storePhoneLocalDigits.isNotBlank()) {
            "${countryCodeToFlagEmoji(storePhoneCountry.code)} ${storePhoneCountry.dialCode} $storePhoneLocalDigits"
        } else if (rawStorePhone.isNotBlank()) {
            rawStorePhone
        } else {
            "${countryCodeToFlagEmoji(storePhoneCountry.code)} ${storePhoneCountry.dialCode} (Not configured)"
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // =================================================================
            // 2. Top Avatar block (96.dp, PrimaryContainerLight, 2.dp border)
            // =================================================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
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
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigoLight,
                                    fontSize = 28.sp
                                )
                            )
                        }
                    }

                    // Camera Icon Badge
                    Surface(
                        shape = CircleShape,
                        color = PrimaryIndigoLight,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable { openPhotoPicker() }
                            .testTag("btn_camera_avatar")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Edit photo",
                                tint = PureWhite,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentDisplayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = userProfile?.currentRole ?: "ADMIN",
                    fontSize = 13.sp,
                    color = OnSurfaceVariantLight
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { openPhotoPicker() }) {
                    Text(
                        text = "Change Photo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryIndigoLight
                    )
                }
            }

            // =================================================================
            // 3. Clean Item List with INLINE Expandable Accordion Editing
            // =================================================================
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // -------------------------------------------------------------
                // ROW 1: Owner / Admin Name
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.Person,
                    iconTint = OnSurfaceVariantLight,
                    title = "Owner / Admin Name",
                    value = userProfile?.fullName?.ifBlank { "Demo Owner" } ?: "Demo Owner",
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
                            label = "Owner / Admin Full Name",
                            placeholder = "e.g. Demo Owner",
                            testTag = "input_edit_full_name"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = "Cancel",
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = "Save",
                                onClick = {
                                    val current = userProfile ?: UserProfile()
                                    reportViewModel.saveUserProfile(current.copy(fullName = tempName.trim()))
                                    expandedSection = null
                                    Toast.makeText(context, "Owner name updated!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // ROW 2: Store / Business Name
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.Store,
                    iconTint = OnSurfaceVariantLight,
                    title = "Store / Business Name",
                    value = bProfile.businessName.ifBlank { "Demo Store (Debug)" },
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
                            label = "Store / Business Name",
                            placeholder = "e.g. Demo Store (Debug)",
                            testTag = "input_edit_store_name"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = "Cancel",
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = "Save",
                                onClick = {
                                    val cleanName = tempStoreName.trim()
                                    reportViewModel.saveBusinessProfile(bProfile.copy(businessName = cleanName))
                                    reportViewModel.updateBusiness(UpdateBusinessRequest(businessName = cleanName))
                                    expandedSection = null
                                    Toast.makeText(context, "Store name updated!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // ROW 3: Tax Registration / VAT ID
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.ReceiptLong,
                    iconTint = OnSurfaceVariantLight,
                    title = "Tax Registration / VAT ID",
                    value = bProfile.vatNumber.ifBlank { "Not set" },
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
                            label = "Tax Registration / VAT ID",
                            placeholder = "e.g. 310000000000003",
                            testTag = "input_edit_vat_id"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = "Cancel",
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = "Save",
                                onClick = {
                                    val cleanVat = tempVat.trim()
                                    reportViewModel.saveBusinessProfile(bProfile.copy(vatNumber = cleanVat))
                                    expandedSection = null
                                    Toast.makeText(context, "Tax Registration ID updated!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // ROW 4: Permanent Store Address (Google Maps)
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Filled.Place,
                    iconTint = OnSurfaceVariantLight,
                    title = "Permanent Store Address",
                    value = if (permanentAddress.isNotBlank()) permanentAddress else "5000 Vista Del Lago Rd, Ukia...",
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
                            label = "Store Address",
                            placeholder = "Enter street address or choose from map"
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
                                Text("Select Location on Google Maps", fontWeight = FontWeight.SemiBold, color = PrimaryIndigoLight, fontSize = 13.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LojiaOutlinedButton(
                                text = "Cancel",
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = "Save",
                                onClick = {
                                    val cleanAddr = tempAddr.trim()
                                    val current = userProfile ?: UserProfile()
                                    reportViewModel.saveUserProfile(current.copy(address = cleanAddr))
                                    reportViewModel.saveBusinessProfile(bProfile.copy(address = cleanAddr))
                                    reportViewModel.updateBusiness(UpdateBusinessRequest(address = cleanAddr))
                                    expandedSection = null
                                    Toast.makeText(context, "Store address saved!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // ROW 5: Store Contact Phone (Country Code Selector + Digits)
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.Phone,
                    iconTint = OnSurfaceVariantLight,
                    title = "Store Contact Phone",
                    value = storePhoneDisplay,
                    isExpanded = expandedSection == "phone",
                    onToggle = {
                        expandedSection = if (expandedSection == "phone") null else "phone"
                    },
                    testTag = "row_store_phone"
                ) {
                    var tempPhoneCountry by remember(storePhoneCountry) { mutableStateOf(storePhoneCountry) }
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
                                    Text(
                                        text = countryCodeToFlagEmoji(tempPhoneCountry.code),
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = tempPhoneCountry.dialCode,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurfaceLight
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Change country",
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
                                label = "Phone Digits",
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
                                Text(
                                    text = "Preview: ${tempPhoneCountry.dialCode} ${tempPhoneDigits.trim()}",
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
                                text = "Cancel",
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = "Save",
                                onClick = {
                                    val fullFormatted = if (tempPhoneDigits.isNotBlank()) {
                                        "${tempPhoneCountry.dialCode} ${tempPhoneDigits.trim()}"
                                    } else {
                                        ""
                                    }
                                    val current = userProfile ?: UserProfile()
                                    reportViewModel.saveUserProfile(current.copy(phone = fullFormatted))
                                    reportViewModel.saveBusinessProfile(bProfile.copy(phone = fullFormatted))
                                    reportViewModel.updateBusiness(UpdateBusinessRequest(phone = fullFormatted))
                                    expandedSection = null
                                    Toast.makeText(context, "Phone number saved!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (showCountryPickerModal) {
                            CountryCodePickerDialog(
                                selectedCountry = tempPhoneCountry,
                                onCountrySelected = { newCountry ->
                                    tempPhoneCountry = newCountry
                                    showCountryPickerModal = false
                                },
                                onDismiss = { showCountryPickerModal = false }
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // ROW 6: Store Contact Email
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.Email,
                    iconTint = OnSurfaceVariantLight,
                    title = "Store Contact Email",
                    value = bProfile.email.ifBlank { userProfile?.email.orEmpty() }.ifBlank { "store@example.com" },
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
                            label = "Contact Email Address",
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
                                text = "Cancel",
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = "Save",
                                onClick = {
                                    val cleanMail = tempEmail.trim()
                                    val current = userProfile ?: UserProfile()
                                    reportViewModel.saveUserProfile(current.copy(email = cleanMail))
                                    reportViewModel.saveBusinessProfile(bProfile.copy(email = cleanMail))
                                    reportViewModel.updateBusiness(UpdateBusinessRequest(email = cleanMail))
                                    expandedSection = null
                                    Toast.makeText(context, "Email updated successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // ROW 7: Change Password
                // -------------------------------------------------------------
                InlineEditableProfileRow(
                    icon = Icons.Outlined.Key,
                    iconTint = OnSurfaceVariantLight,
                    title = "Change Password",
                    value = "Change administrator master pass...",
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
                        newPassword.isEmpty() -> Triple("", Color.Transparent, 0f)
                        strengthScore <= 1 -> Triple("Weak", ErrorRedLight, 0.25f)
                        strengthScore == 2 -> Triple("Fair", WarningOrange, 0.5f)
                        strengthScore == 3 -> Triple("Good", InfoBlue, 0.75f)
                        else -> Triple("Strong", SuccessGreen, 1f)
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
                                placeholder = "Enter current password",
                                isError = currentPasswordError != null,
                                supportingText = {
                                    currentPasswordError?.let { Text(it, color = ErrorRedLight) }
                                },
                                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                trailingIcon = {
                                    IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                        Icon(
                                            imageVector = if (currentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (currentPasswordVisible) "Hide password" else "Show password",
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
                                    confirmPasswordError = "Passwords do not match"
                                } else {
                                    confirmPasswordError = null
                                }
                            },
                            label = stringResource(R.string.new_password_label),
                            placeholder = "Enter new strong password",
                            isError = newPasswordError != null,
                            supportingText = {
                                newPasswordError?.let { Text(it, color = ErrorRedLight) }
                            },
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
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
                                    Text("Password Strength", style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariantLight, fontSize = 11.sp))
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
                                    confirmPasswordError = "Passwords do not match"
                                } else {
                                    confirmPasswordError = null
                                }
                            },
                            label = stringResource(R.string.confirm_password_label),
                            placeholder = "Re-enter new password",
                            isError = confirmPasswordError != null,
                            supportingText = {
                                confirmPasswordError?.let { Text(it, color = ErrorRedLight) }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
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
                                text = "Cancel",
                                onClick = { expandedSection = null },
                                modifier = Modifier.weight(1f)
                            )
                            LojiaPrimaryButton(
                                text = "Save",
                                onClick = {
                                    var hasError = false
                                    if (currentPasswordHash.isNotBlank()) {
                                        if (currentPassword.isBlank()) {
                                            currentPasswordError = "Current password is required"
                                            hasError = true
                                        } else if (!SecurityUtils.verifySecret(currentPassword, currentPasswordHash)) {
                                            currentPasswordError = "Incorrect current password"
                                            hasError = true
                                        }
                                    }
                                    if (newPassword.length < 6) {
                                        newPasswordError = "Password must be at least 6 characters"
                                        hasError = true
                                    }
                                    if (newPassword != confirmPassword) {
                                        confirmPasswordError = "Passwords do not match"
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

            Spacer(modifier = Modifier.height(24.dp))
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
                Toast.makeText(context, "Store location saved from Google Maps!", Toast.LENGTH_SHORT).show()
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

// =============================================================================
// INLINE EDITABLE PROFILE ROW (LojiaSettingsCard Accordion Spec)
// =============================================================================

@Composable
fun InlineEditableProfileRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    editContent: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, OutlineLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Collapsed Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = OnSurfaceVariantLight,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value.ifBlank { "Not set" },
                        fontSize = 13.sp,
                        color = OnSurfaceVariantLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextHintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded Inline Editor Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = OutlineVariantLight,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    editContent()
                }
            }
        }
    }
}

// =============================================================================
// Helper Components & Text Fields
// =============================================================================

@Composable
fun ProfileDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    testTag: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                color = OnSurfaceVariantLight,
                fontWeight = FontWeight.Medium
            )
        },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(text = placeholder, color = TextHintColor) }
        } else null,
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = OnSurfaceLight,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OnSurfaceLight,
            unfocusedTextColor = OnSurfaceLight,
            focusedContainerColor = PureWhite,
            unfocusedContainerColor = PureWhite,
            disabledContainerColor = SurfaceVariantLight,
            errorContainerColor = ErrorContainer,
            focusedLabelColor = PrimaryIndigoLight,
            unfocusedLabelColor = OnSurfaceVariantLight,
            focusedPlaceholderColor = TextHintColor,
            unfocusedPlaceholderColor = TextHintColor,
            focusedBorderColor = PrimaryIndigoLight,
            unfocusedBorderColor = OutlineLight,
            cursorColor = PrimaryIndigoLight,
            errorBorderColor = ErrorRedLight
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    )
}

// =============================================================================
// Helper Functions & Dialogs for Phone & Country Selection
// =============================================================================

/**
 * Parses raw stored phone strings into a paired (AppCountry, LocalNumberDigits)
 */
fun parsePhoneNumberWithCountry(rawPhone: String, defaultCountry: AppCountry): Pair<AppCountry, String> {
    val clean = rawPhone.trim()
    if (clean.isBlank()) return Pair(defaultCountry, "")

    // 1. Check exact country dial code prefix (e.g. "+880", "+966", "+1")
    for (country in AppCountry.entries) {
        if (clean.startsWith(country.dialCode)) {
            val local = clean.removePrefix(country.dialCode).trim()
            return Pair(country, local)
        }
    }
    // 2. Check if starts with "+" then digits
    if (clean.startsWith("+")) {
        val digits = clean.removePrefix("+")
        for (country in AppCountry.entries) {
            val countryDigits = country.dialCode.removePrefix("+")
            if (digits.startsWith(countryDigits)) {
                val local = digits.removePrefix(countryDigits).trim()
                return Pair(country, local)
            }
        }
    }
    return Pair(defaultCountry, clean)
}

/**
 * Material 3 Searchable Country Code & Dial Code Picker Dialog
 */
@Composable
fun CountryCodePickerDialog(
    selectedCountry: AppCountry,
    onCountrySelected: (AppCountry) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val countries = remember { AppCountry.entries }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) countries else {
            countries.filter { country ->
                val localizedName = context.getString(country.nameRes)
                localizedName.contains(searchQuery, ignoreCase = true) ||
                country.displayName.contains(searchQuery, ignoreCase = true) ||
                country.code.contains(searchQuery, ignoreCase = true) ||
                country.dialCode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .testTag("dialog_select_country_code"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.auth_select_country_code),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight,
                            fontSize = 17.sp
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_country), fontSize = 13.5.sp, color = TextHintColor) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = OnSurfaceVariantLight) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = OnSurfaceLight,
                        fontSize = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurfaceLight,
                        unfocusedTextColor = OnSurfaceLight,
                        focusedContainerColor = BackgroundLight,
                        unfocusedContainerColor = BackgroundLight,
                        focusedBorderColor = PrimaryIndigoLight,
                        unfocusedBorderColor = OutlineLight,
                        cursorColor = PrimaryIndigoLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("search_country_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCountries) { country ->
                        val isSelected = country == selectedCountry
                        val flag = countryCodeToFlagEmoji(country.code)
                        val countryName = stringResource(country.nameRes)

                        Surface(
                            onClick = {
                                onCountrySelected(country)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PrimaryContainerLight else Color.Transparent,
                            modifier = Modifier.fillMaxWidth().testTag("country_option_${country.code}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = flag, fontSize = 22.sp)
                                    Column {
                                        Text(
                                            text = countryName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) PrimaryIndigoLight else OnSurfaceLight,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${country.displayName} (${country.code})",
                                            color = OnSurfaceVariantLight,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryContainerLight else SurfaceVariantLight
                                ) {
                                    Text(
                                        text = country.dialCode,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PrimaryIndigoDark else OnSurfaceVariantLight,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
