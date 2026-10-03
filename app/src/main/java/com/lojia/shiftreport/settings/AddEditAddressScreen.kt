package com.lojia.shiftreport.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.data.SavedAddress
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.ui.common.rememberPhoneNumberHint
import com.lojia.shiftreport.ui.common.splitPhoneNumber
import com.lojia.shiftreport.ui.theme.BackgroundLight
import com.lojia.shiftreport.ui.theme.ErrorRedLight
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OutlineLight
import com.lojia.shiftreport.ui.theme.OutlineVariantLight
import com.lojia.shiftreport.ui.theme.PrimaryContainerLight
import com.lojia.shiftreport.ui.theme.PrimaryIndigoDark
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.SuccessContainer
import com.lojia.shiftreport.ui.theme.SuccessGreen
import com.lojia.shiftreport.ui.theme.SurfaceLight
import com.lojia.shiftreport.ui.theme.SurfaceVariantLight
import com.lojia.shiftreport.ui.theme.TextDisabledColor
import com.lojia.shiftreport.ui.theme.TextHintColor
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddressScreen(
    viewModel: ReportViewModel,
    addressId: String? = null,   // null = new, non-null = edit
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    BackHandler { onBack() }

    val currentAppCountry by viewModel.currentCountry.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedCountry by remember { mutableStateOf(currentAppCountry) }
    var dialCode by remember { mutableStateOf("+966") }
    var mobileNumber by remember { mutableStateOf("") }
    var alternatePhone by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var addressLine by remember { mutableStateOf("") }
    var arabicAddressLine by remember { mutableStateOf("") }
    var aptSuite by remember { mutableStateOf("") }
    var addressLabel by remember { mutableStateOf("") }
    var mapLat by remember { mutableDoubleStateOf(0.0) }
    var mapLng by remember { mutableDoubleStateOf(0.0) }
    var isDefault by remember { mutableStateOf(false) }
    var createdAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showCountrySheet by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }

    // Sync initial dial code with current app country when adding a new address
    LaunchedEffect(addressId) {
        if (addressId != null) {
            val existing = viewModel.getSavedAddressById(addressId)
            if (existing != null) {
                selectedCountry = AppCountry.fromCode(existing.countryCode)
                dialCode = existing.dialCode.ifBlank { selectedCountry.dialCode }
                val codes = AppCountry.entries.mapNotNull { it.dialCode.takeIf { c -> c.isNotBlank() } }
                val (_, strippedMobile) = if (existing.phone.startsWith("+")) {
                    splitPhoneNumber(existing.phone, codes)
                } else {
                    dialCode to existing.phone
                }
                mobileNumber = strippedMobile
                alternatePhone = existing.alternatePhone
                firstName = existing.firstName
                lastName = existing.lastName
                addressLine = existing.addressLine
                arabicAddressLine = existing.arabicAddressLine
                aptSuite = existing.aptSuite
                addressLabel = existing.addressLabel
                mapLat = existing.mapLat
                mapLng = existing.mapLng
                isDefault = existing.isDefault
                createdAt = existing.createdAt
            }
        } else {
            selectedCountry = currentAppCountry
            dialCode = currentAppCountry.dialCode.ifBlank { "+966" }
        }
    }

    val requestPhoneHint = rememberPhoneNumberHint(
        onPhoneSelected = { phone ->
            val codes = AppCountry.entries.mapNotNull { it.dialCode.takeIf { c -> c.isNotBlank() } }
            val (code, number) = splitPhoneNumber(phone, codes)
            dialCode = code
            mobileNumber = number
            AppCountry.entries.find { it.dialCode == code }?.let { matched ->
                selectedCountry = matched
            }
        },
        onError = { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }
    )

    val canSave = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            mobileNumber.isNotBlank() &&
            addressLine.isNotBlank()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryIndigoLight,
        unfocusedBorderColor = OutlineLight,
        disabledBorderColor = OutlineLight,
        focusedContainerColor = SurfaceLight,
        unfocusedContainerColor = SurfaceLight,
        disabledContainerColor = SurfaceLight,
        focusedTextColor = OnSurfaceLight,
        unfocusedTextColor = OnSurfaceLight,
        disabledTextColor = OnSurfaceLight,
        cursorColor = PrimaryIndigoLight
    )

    Scaffold(
        containerColor = BackgroundLight,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (addressId != null) "Edit Address" else "Add New Address",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_add_edit_address_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryIndigoLight,
                    titleContentColor = PureWhite,
                    navigationIconContentColor = PureWhite
                )
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceLight,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            val savedAddress = SavedAddress(
                                id = addressId ?: UUID.randomUUID().toString(),
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                phone = mobileNumber.trim(),
                                alternatePhone = alternatePhone.trim(),
                                countryCode = selectedCountry.code,
                                dialCode = dialCode.trim(),
                                addressLine = addressLine.trim(),
                                arabicAddressLine = arabicAddressLine.trim(),
                                aptSuite = aptSuite.trim(),
                                addressLabel = addressLabel.trim(),
                                mapLat = mapLat,
                                mapLng = mapLng,
                                isDefault = isDefault,
                                createdAt = createdAt
                            )
                            viewModel.saveAddress(savedAddress)
                            onSaved()
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRedLight,
                            contentColor = PureWhite,
                            disabledContainerColor = TextDisabledColor,
                            disabledContentColor = PureWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_save_address")
                    ) {
                        Text(
                            text = "Save",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            // Encryption Banner
            Surface(
                color = SuccessContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "All information is encrypted",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SuccessGreen
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // =============================================================
                // SECTION 1: Country/region
                // =============================================================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Country/region",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCountrySheet = true }
                            .testTag("field_address_country")
                    ) {
                        OutlinedTextField(
                            value = "${countryCodeToFlagEmoji(selectedCountry.code)}  ${selectedCountry.displayName}",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = "Select country",
                                    tint = OnSurfaceVariantLight
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // =============================================================
                // SECTION 2: Address
                // =============================================================
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Address",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )

                    // Mini-map thumbnail card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = BorderStroke(1.dp, OutlineLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = addressLine.ifBlank { "Tap Edit to select on map" },
                                    fontSize = 13.sp,
                                    fontWeight = if (addressLine.isBlank()) FontWeight.Normal else FontWeight.Medium,
                                    color = if (addressLine.isBlank()) TextHintColor else OnSurfaceLight,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clickable { showMapPicker = true }
                                    .testTag("btn_address_map_thumbnail")
                            ) {
                                Surface(
                                    color = PrimaryContainerLight,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Map,
                                            contentDescription = "Select on Map",
                                            tint = PrimaryIndigoLight,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    shape = RoundedCornerShape(topStart = 8.dp),
                                    color = OnSurfaceLight.copy(alpha = 0.78f)
                                ) {
                                    Text(
                                        text = "Edit",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PureWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Street address text input (also populated by map picker)
                    OutlinedTextField(
                        value = addressLine,
                        onValueChange = { addressLine = it },
                        label = {
                            Text(
                                text = "Street address *",
                                fontSize = 13.sp,
                                color = OnSurfaceVariantLight
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Street, building, district",
                                fontSize = 13.sp,
                                color = TextHintColor
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_address_line")
                    )

                    // Apt, suite, unit (optional)
                    OutlinedTextField(
                        value = aptSuite,
                        onValueChange = { aptSuite = it },
                        label = {
                            Text(
                                text = "Apt, suite, unit (optional)",
                                fontSize = 13.sp,
                                color = OnSurfaceVariantLight
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_apt_suite")
                    )

                    // Address label with Suggestion chip on trailing
                    OutlinedTextField(
                        value = addressLabel,
                        onValueChange = { addressLabel = it },
                        label = {
                            Text(
                                text = "Address label (optional)",
                                fontSize = 13.sp,
                                color = OnSurfaceVariantLight
                            )
                        },
                        trailingIcon = {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = PrimaryContainerLight,
                                border = BorderStroke(1.dp, PrimaryIndigoLight),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        addressLabel = if (addressLabel == "Main Store") "Warehouse" else "Main Store"
                                    }
                                    .testTag("chip_address_label_suggestion")
                            ) {
                                Text(
                                    text = "Suggestion",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryIndigoDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_address_label")
                    )
                }

                // =============================================================
                // SECTION 3: Contact information
                // =============================================================
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Contact information",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = {
                            Text(
                                text = "First name *",
                                fontSize = 13.sp,
                                color = OnSurfaceVariantLight
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_first_name")
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = {
                            Text(
                                text = "Last name *",
                                fontSize = 13.sp,
                                color = OnSurfaceVariantLight
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_last_name")
                    )

                    // Row: [dialCode 96.dp readonly] + [Mobile number * with AutoAwesome trailing icon]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = dialCode,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    text = "Code",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .width(96.dp)
                                .testTag("input_mobile_dial_code")
                        )

                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { raw ->
                                mobileNumber = raw.filter { it.isDigit() || it == ' ' || it == '-' }
                            },
                            label = {
                                Text(
                                    text = "Mobile number *",
                                    fontSize = 13.sp,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = requestPhoneHint,
                                    modifier = Modifier.testTag("btn_phone_hint_autofill")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "Auto-fill phone number",
                                        tint = PrimaryIndigoLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_mobile_number")
                        )
                    }

                    // Row: [dialCode 96.dp readonly] + [Alternate phone number]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = dialCode,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    text = "Code",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .width(96.dp)
                                .testTag("input_alt_dial_code")
                        )

                        OutlinedTextField(
                            value = alternatePhone,
                            onValueChange = { raw ->
                                alternatePhone = raw.filter { it.isDigit() || it == ' ' || it == '-' }
                            },
                            label = {
                                Text(
                                    text = "Alternate phone number",
                                    fontSize = 13.sp,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_alternate_phone")
                        )
                    }
                }
            }
        }
    }

    // Country Picker Bottom Sheet
    if (showCountrySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var countrySearch by remember { mutableStateOf("") }
        val filteredCountries = remember(countrySearch) {
            if (countrySearch.isBlank()) {
                AppCountry.entries
            } else {
                val q = countrySearch.trim().lowercase()
                AppCountry.entries.filter {
                    it.displayName.lowercase().contains(q) ||
                            it.code.lowercase().contains(q) ||
                            it.dialCode.contains(q)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showCountrySheet = false },
            sheetState = sheetState,
            containerColor = SurfaceLight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Country/region",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    IconButton(onClick = { showCountrySheet = false }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = OnSurfaceVariantLight
                        )
                    }
                }

                OutlinedTextField(
                    value = countrySearch,
                    onValueChange = { countrySearch = it },
                    placeholder = {
                        Text(
                            text = "Search country...",
                            fontSize = 13.sp,
                            color = TextHintColor
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = OnSurfaceVariantLight
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = OutlineVariantLight)

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filteredCountries, key = { it.code }) { country ->
                        val isSelected = country == selectedCountry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCountry = country
                                    dialCode = country.dialCode.ifBlank { "+966" }
                                    showCountrySheet = false
                                }
                                .background(if (isSelected) PrimaryContainerLight else SurfaceLight)
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = countryCodeToFlagEmoji(country.code),
                                    fontSize = 20.sp,
                                    color = OnSurfaceLight
                                )
                                Text(
                                    text = country.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = OnSurfaceLight
                                )
                                Text(
                                    text = country.dialCode,
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariantLight
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariantLight)
                    }
                }
            }
        }
    }

    // Google Maps Location Picker Modal
    if (showMapPicker) {
        GoogleMapsLocationPickerModal(
            initialAddress = addressLine,
            initialLat = mapLat,
            initialLng = mapLng,
            onLocationConfirmed = { confirmedAddr, lat, lng ->
                addressLine = confirmedAddr
                mapLat = lat
                mapLng = lng
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
    }
}
