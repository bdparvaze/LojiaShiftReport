package com.lojia.shiftreport.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.data.AppCountry
import com.lojia.shiftreport.data.SavedAddress
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.common.lojiaDefaultTextFieldColors
import com.lojia.shiftreport.ui.common.rememberPhoneNumberHint
import com.lojia.shiftreport.ui.common.splitPhoneNumber
import com.lojia.shiftreport.ui.theme.BackgroundLight
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.LojiaGradientHeader
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OutlineVariantLight
import com.lojia.shiftreport.ui.theme.PositiveBannerBorder
import com.lojia.shiftreport.ui.theme.PrimaryContainerLight
import com.lojia.shiftreport.ui.theme.PrimaryIndigoDark
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.SuccessContainer
import com.lojia.shiftreport.ui.theme.SuccessTextDark
import com.lojia.shiftreport.ui.theme.SurfaceLight
import com.lojia.shiftreport.ui.theme.TextHintColor
import com.lojia.shiftreport.ui.theme.cardRise
import com.lojia.shiftreport.ui.theme.depthCard
import com.lojia.shiftreport.ui.theme.depthRaisedButton
import com.lojia.shiftreport.ui.theme.depthSemanticCard
import com.lojia.shiftreport.util.CountryDetector
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

    val context = LocalContext.current
    val detectedCountry = remember(context) { CountryDetector.detectDefaultAppCountry(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedCountry by remember { mutableStateOf(detectedCountry) }
    var dialCode by remember { mutableStateOf(detectedCountry.dialCode) }
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
    var pendingAddressToSave by remember { mutableStateOf<SavedAddress?>(null) }

    // Sync initial country & dial code from device detector for a new address, or saved values when editing
    LaunchedEffect(addressId) {
        if (addressId != null) {
            val existing = viewModel.getSavedAddressById(addressId)
            if (existing != null) {
                val resolvedCountry = if (existing.countryCode.isNotBlank()) {
                    AppCountry.fromCode(existing.countryCode)
                } else {
                    detectedCountry
                }
                selectedCountry = resolvedCountry
                dialCode = existing.dialCode.ifBlank { resolvedCountry.dialCode }
                val codes = AppCountry.entries.mapNotNull { it.dialCode.takeIf { c -> c.isNotBlank() } }
                val (_, strippedMobile) = if (existing.phone.startsWith("+")) {
                    splitPhoneNumber(existing.phone, codes, dialCode)
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
            selectedCountry = detectedCountry
            dialCode = detectedCountry.dialCode
        }
    }

    val requestPhoneHint = rememberPhoneNumberHint(
        onPhoneSelected = { phone ->
            val codes = AppCountry.entries.mapNotNull { it.dialCode.takeIf { c -> c.isNotBlank() } }
            val (code, number) = splitPhoneNumber(
                fullPhone = phone,
                availableDialCodes = codes,
                defaultDialCode = dialCode.ifBlank { detectedCountry.dialCode }
            )
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

    val textFieldColors = lojiaDefaultTextFieldColors().copy(
        disabledTextColor = OnSurfaceLight
    )

    Scaffold(
        containerColor = BackgroundLight,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LojiaGradientHeader(
                shape = RoundedCornerShape(
                    bottomStart = Dimens.RadiusXl,
                    bottomEnd = Dimens.RadiusXl
                ),
                contentPadding = PaddingValues(Dimens.SpacingNone)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(if (addressId != null) R.string.edit_address else R.string.add_new_address),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_add_edit_address_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = PureWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = PureWhite,
                        navigationIconContentColor = PureWhite
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        bottomBar = {
            Surface(
                color = SurfaceLight,
                border = BorderStroke(Dimens.RowDividerWidth, OutlineVariantLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd)
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
                            pendingAddressToSave = savedAddress
                        },
                        enabled = canSave,
                        elevation = null,
                        shape = RoundedCornerShape(Dimens.RadiusPill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = PureWhite,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = OnSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(
                                enabled = canSave,
                                isDestructive = true,
                                cornerRadius = Dimens.RadiusPill
                            )
                            .testTag("btn_save_address")
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (canSave) PureWhite else OnSurfaceVariantLight
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm)
                    .depthSemanticCard(
                        backgroundColor = SuccessContainer,
                        borderColor = PositiveBannerBorder
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessTextDark,
                        modifier = Modifier.size(Dimens.IconSm)
                    )
                    Text(
                        text = stringResource(R.string.all_info_encrypted),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = SuccessTextDark
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.SpacingLg)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLg)
            ) {
                // =============================================================
                // SECTION 1: Country/region
                // =============================================================
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                    Text(
                        text = stringResource(R.string.country_region_label),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.MinTouchTarget)
                            .clickable { showCountrySheet = true }
                            .testTag("field_address_country")
                    ) {
                        LojiaTextField(
                            value = "${countryCodeToFlagEmoji(selectedCountry.code)}  ${stringResource(selectedCountry.nameRes)}",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = stringResource(R.string.cd_select_country),
                                    tint = OnSurfaceVariantLight
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // =============================================================
                // SECTION 2: Address
                // =============================================================
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                    Text(
                        text = stringResource(R.string.address),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )

                    // Mini-map thumbnail card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardRise(0)
                            .depthCard()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimens.AddressMapThumbSize),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(Dimens.CardPadding),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = addressLine.ifBlank { stringResource(R.string.address_tap_edit_map) },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (addressLine.isBlank()) FontWeight.Normal else FontWeight.Medium,
                                    color = if (addressLine.isBlank()) TextHintColor else OnSurfaceLight,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(Dimens.AddressMapThumbSize)
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
                                            contentDescription = stringResource(R.string.cd_select_on_map),
                                            tint = PrimaryIndigoLight,
                                            modifier = Modifier.size(Dimens.IconXl)
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    shape = RoundedCornerShape(topStart = Dimens.RadiusMd),
                                    color = OnSurfaceLight.copy(alpha = 0.78f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.edit_item),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = PureWhite,
                                        modifier = Modifier.padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs)
                                    )
                                }
                            }
                        }
                    }

                    // Street address text input (also populated by map picker)
                    val streetAddressLabel = stringResource(R.string.street_address_required)
                    LojiaTextField(
                        value = addressLine,
                        onValueChange = { addressLine = it },
                        label = {
                            Text(
                                text = streetAddressLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurfaceVariantLight
                            )
                        },
                        contentDescription = streetAddressLabel,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.street_building_district_hint),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextHintColor
                            )
                        },
                        singleLine = false,
                        maxLines = 2,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_address_line")
                    )

                    // Apt, suite, unit (optional)
                    val aptSuiteLabel = stringResource(R.string.apt_suite_optional)
                    LojiaTextField(
                        value = aptSuite,
                        onValueChange = { aptSuite = it },
                        label = {
                            Text(
                                text = aptSuiteLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurfaceVariantLight
                            )
                        },
                        contentDescription = aptSuiteLabel,
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_apt_suite")
                    )

                    // Address label with Suggestion chip on trailing
                    val storeSuggestion = stringResource(R.string.address_tag_store)
                    val warehouseSuggestion = stringResource(R.string.address_tag_warehouse)
                    val addressLabelOptional = stringResource(R.string.address_label_optional)
                    LojiaTextField(
                        value = addressLabel,
                        onValueChange = { addressLabel = it },
                        label = {
                            Text(
                                text = addressLabelOptional,
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurfaceVariantLight
                            )
                        },
                        contentDescription = addressLabelOptional,
                        trailingIcon = {
                            Surface(
                                shape = RoundedCornerShape(Dimens.RadiusPill),
                                color = PrimaryContainerLight,
                                border = BorderStroke(Dimens.BorderThin, PrimaryIndigoLight),
                                modifier = Modifier
                                    .padding(end = Dimens.SpacingSm)
                                    .clip(RoundedCornerShape(Dimens.RadiusPill))
                                    .clickable {
                                        addressLabel = if (addressLabel == storeSuggestion) warehouseSuggestion else storeSuggestion
                                    }
                                    .testTag("chip_address_label_suggestion")
                            ) {
                                Text(
                                    text = stringResource(R.string.suggestion_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryIndigoDark,
                                    modifier = Modifier.padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs)
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_address_label")
                    )
                }

                // =============================================================
                // SECTION 3: Contact information
                // =============================================================
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                    Text(
                        text = stringResource(R.string.contact_information_header),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )

                    val firstNameLabel = stringResource(R.string.profile_first_name_label)
                    LojiaTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = {
                            Text(
                                text = firstNameLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurfaceVariantLight
                            )
                        },
                        contentDescription = firstNameLabel,
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_first_name")
                    )

                    val lastNameLabel = stringResource(R.string.profile_last_name_label)
                    LojiaTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = {
                            Text(
                                text = lastNameLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurfaceVariantLight
                            )
                        },
                        contentDescription = lastNameLabel,
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_last_name")
                    )

                    // Row: [dialCode readonly] + [Mobile number * with AutoAwesome trailing icon]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val mobileDialCodeLabel = stringResource(R.string.address_dial_code_label)
                        LojiaTextField(
                            value = dialCode,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    text = mobileDialCodeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            contentDescription = mobileDialCodeLabel,
                            singleLine = true,
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            colors = textFieldColors,
                            modifier = Modifier
                                .width(Dimens.DialCodeFieldWidth)
                                .testTag("input_mobile_dial_code")
                        )

                        val mobileRequiredLabel = stringResource(R.string.address_mobile_required)
                        LojiaTextField(
                            value = mobileNumber,
                            onValueChange = { raw ->
                                mobileNumber = raw.filter { it.isDigit() || it == ' ' || it == '-' }
                            },
                            label = {
                                Text(
                                    text = mobileRequiredLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            contentDescription = mobileRequiredLabel,
                            trailingIcon = {
                                IconButton(
                                    onClick = requestPhoneHint,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_phone_hint_autofill")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = stringResource(R.string.cd_autofill_phone),
                                        tint = PrimaryIndigoLight,
                                        modifier = Modifier.size(Dimens.IconAction)
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            colors = textFieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_mobile_number")
                        )
                    }

                    // Row: [dialCode readonly] + [Alternate phone number]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val altDialCodeLabel = stringResource(R.string.address_dial_code_label)
                        LojiaTextField(
                            value = dialCode,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    text = altDialCodeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            contentDescription = altDialCodeLabel,
                            singleLine = true,
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            colors = textFieldColors,
                            modifier = Modifier
                                .width(Dimens.DialCodeFieldWidth)
                                .testTag("input_alt_dial_code")
                        )

                        val alternatePhoneLabel = stringResource(R.string.alternate_phone_label)
                        LojiaTextField(
                            value = alternatePhone,
                            onValueChange = { raw ->
                                alternatePhone = raw.filter { it.isDigit() || it == ' ' || it == '-' }
                            },
                            label = {
                                Text(
                                    text = alternatePhoneLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurfaceVariantLight
                                )
                            },
                            contentDescription = alternatePhoneLabel,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(Dimens.RadiusMd),
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
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(
                topStart = Dimens.BottomSheetCornerRadius,
                topEnd = Dimens.BottomSheetCornerRadius
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Dimens.BottomSheetMaxHeight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.address_select_country_region),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    IconButton(
                        onClick = { showCountrySheet = false },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = OnSurfaceVariantLight
                        )
                    }
                }

                LojiaTextField(
                    value = countrySearch,
                    onValueChange = { countrySearch = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_country_hint),
                            style = MaterialTheme.typography.titleMedium,
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
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingXs)
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                HorizontalDivider(thickness = Dimens.RowDividerWidth, color = OutlineVariantLight)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = Dimens.SpacingXs)
                ) {
                    items(filteredCountries, key = { it.code }) { country ->
                        val isSelected = country == selectedCountry
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingXxs)
                                .heightIn(min = Dimens.MinTouchTarget)
                                .depthCard(accent = isSelected, shape = RoundedCornerShape(Dimens.RadiusMd))
                                .clickable {
                                    selectedCountry = country
                                    dialCode = country.dialCode.ifBlank { detectedCountry.dialCode }
                                    showCountrySheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = countryCodeToFlagEmoji(country.code),
                                        style = MaterialTheme.typography.displayLarge,
                                        color = OnSurfaceLight
                                    )
                                    Text(
                                        text = stringResource(country.nameRes),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = OnSurfaceLight
                                    )
                                    Text(
                                        text = country.dialCode,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariantLight
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = stringResource(R.string.cd_selected),
                                        tint = PrimaryIndigoLight,
                                        modifier = Modifier.size(Dimens.IconMd)
                                    )
                                }
                            }
                        }
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

    pendingAddressToSave?.let { addrToSave ->
        AdminAuthDialog(
            title = stringResource(R.string.admin_authorization_required),
            subtitle = stringResource(R.string.saved_addresses_title),
            onDismissRequest = { pendingAddressToSave = null },
            onAuthSuccess = {
                pendingAddressToSave = null
                viewModel.saveAddress(addrToSave)
                onSaved()
            }
        )
    }
}
