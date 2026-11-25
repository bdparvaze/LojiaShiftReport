package com.lojia.shiftreport.settings

import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.util.calculateInSampleSize
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.data.UserProfile
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.settings.ProfileImageCropper.ProfileImageCropperDialog
import androidx.compose.ui.platform.testTag
import com.lojia.shiftreport.ui.common.LojiaDialog
import com.lojia.shiftreport.ui.common.LojiaTextField
import com.lojia.shiftreport.ui.common.lojiaDefaultTextFieldColors
import com.lojia.shiftreport.ui.theme.BackgroundLight
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.ErrorContainerLight
import com.lojia.shiftreport.ui.theme.ErrorRedLight
import com.lojia.shiftreport.ui.theme.LocalDepthStyle
import com.lojia.shiftreport.ui.theme.OnBackgroundLight
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OutlineLight
import com.lojia.shiftreport.ui.theme.PrimaryBlueDark
import com.lojia.shiftreport.ui.theme.PrimaryBlueLight
import com.lojia.shiftreport.ui.theme.PrimaryContainerLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.SuccessGreen
import com.lojia.shiftreport.ui.theme.SurfaceVariantLight
import com.lojia.shiftreport.ui.theme.TextHintColor
import com.lojia.shiftreport.ui.theme.WarningContainer
import com.lojia.shiftreport.ui.theme.WarningOrange
import com.lojia.shiftreport.ui.theme.WarningText
import com.lojia.shiftreport.ui.theme.cardRise
import com.lojia.shiftreport.ui.theme.depthCard
import com.lojia.shiftreport.ui.theme.depthRaisedButton
import com.lojia.shiftreport.ui.theme.depthSecondaryButton
import com.lojia.shiftreport.ui.theme.depthSemanticCard
import com.lojia.shiftreport.util.CountryDetector
import com.lojia.shiftreport.util.SecurityUtils
import com.lojia.shiftreport.util.buildE164Phone
import com.lojia.shiftreport.util.isValidPhone
import com.lojia.shiftreport.util.normalizeLocalInput

@Composable
private fun recoveryQuestionOptions(): List<String> = listOf(
    stringResource(R.string.profile_security_q1),
    stringResource(R.string.profile_security_q2),
    stringResource(R.string.profile_security_q3),
    stringResource(R.string.profile_security_q4),
    stringResource(R.string.profile_security_q5)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    reportViewModel: ReportViewModel,
    modifier: Modifier = Modifier,
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val prefs = remember(context) { PreferencesRepository.getInstance(context) }

    val userProfile by reportViewModel.userProfile.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()
    val savedAddresses by reportViewModel.savedAddresses.collectAsState()
    val addressCount = savedAddresses.size
    val bProfile = businessProfile ?: BusinessProfile()

    val storeOwnerLabel = stringResource(R.string.profile_store_owner)
    val notSetText = stringResource(R.string.not_set)

    // Read real registration & profile data
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val displayFullName = remember(userProfile, refreshTrigger, storeOwnerLabel) {
        prefs.getFullName()
            .ifBlank { "${prefs.getFirstName()} ${prefs.getLastName()}".trim() }
            .ifBlank { userProfile?.fullName.orEmpty() }
            .ifBlank { storeOwnerLabel }
    }
    val displayUsername = remember(userProfile, refreshTrigger) {
        prefs.getSavedUsername()
            .ifBlank { userProfile?.username.orEmpty() }
            .ifBlank { "admin" }
    }
    val displayEmail = remember(userProfile, bProfile, refreshTrigger, notSetText) {
        prefs.getEmail()
            .ifBlank { userProfile?.email.orEmpty() }
            .ifBlank { bProfile.email }
            .ifBlank { notSetText }
    }
    val displayPhone = remember(userProfile, bProfile, refreshTrigger, notSetText) {
        prefs.getPhone()
            .ifBlank { userProfile?.phone.orEmpty() }
            .ifBlank { bProfile.phone }
            .ifBlank { notSetText }
    }
    val displayRecoveryQuestion = remember(userProfile, refreshTrigger) {
        prefs.getRecoveryQuestion()
            .ifBlank { userProfile?.securityQuestion.orEmpty() }
    }

    // Dialog states
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var showEditRecoveryDialog by remember { mutableStateOf(false) }
    var showProfileChangePasswordDialog by remember { mutableStateOf(false) }

    // Pending Admin PIN gate action
    var pendingAuthSubtitle by remember { mutableStateOf<String?>(null) }
    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requireAdminPin(subtitle: String, onConfirmed: () -> Unit) {
        pendingAuthSubtitle = subtitle
        pendingAuthAction = onConfirmed
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP: Profile Photo Section - Center (6-Step WhatsApp/Foodics Flow)
        var profileImageUri by remember { mutableStateOf<Uri?>(null) }
        var profileBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var imageUriToCrop by remember { mutableStateOf<Uri?>(null) }
        var showCropperDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val savedUriString = prefs.getProfileImageUri()
            if (savedUriString.isNotBlank()) {
                profileImageUri = Uri.parse(savedUriString)
            }
        }

        LaunchedEffect(profileImageUri) {
            val uri = profileImageUri
            profileBitmap = if (uri != null) {
                runCatching {
                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, boundsOptions)
                    }
                    val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight)
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inJustDecodeBounds = false
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)?.asImageBitmap()
                    }
                }.getOrNull()
            } else {
                null
            }
        }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                imageUriToCrop = uri
                showCropperDialog = true
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(Dimens.CardElevation, CircleShape)
                    .clip(CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .border(3.dp, PureWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap!!,
                        contentDescription = stringResource(R.string.profile_picture),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(PrimaryContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayFullName.trim().firstOrNull()?.uppercaseChar()?.toString()
                                ?: storeOwnerLabel.first().uppercaseChar().toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlueLight
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlueLight)
                        .border(2.dp, PureWhite, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.profile_edit_picture_cd),
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = displayFullName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackgroundLight,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (showCropperDialog && imageUriToCrop != null) {
            ProfileImageCropperDialog(
                imageUri = imageUriToCrop!!,
                onCropSaved = { croppedUri ->
                    profileImageUri = croppedUri
                    prefs.saveProfileImageUri(croppedUri.toString())
                    showCropperDialog = false
                    imageUriToCrop = null
                    Toast.makeText(context, context.getString(R.string.profile_picture_updated_toast), Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    showCropperDialog = false
                    imageUriToCrop = null
                }
            )
        }

        // SECTION 1: PERSONAL INFORMATION
        ProfileSectionTitle(stringResource(R.string.profile_section_personal))

        // 1. Full Name
        ProfileItemCard(
            icon = Icons.Default.Person,
            title = stringResource(R.string.profile_full_name_label),
            value = displayFullName,
            riseIndex = 0,
            onClick = { showEditNameDialog = true }
        )

        // 2. User ID (READ-ONLY)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cardRise(1)
                .depthSemanticCard(
                    backgroundColor = BackgroundLight,
                    borderColor = OutlineLight,
                    shape = RoundedCornerShape(Dimens.RadiusLg)
                )
                .drawBehind {
                    drawRoundRect(
                        color = OutlineLight,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }
                .heightIn(min = Dimens.MinTouchTarget)
                .clickable {
                    clipboardManager.setText(AnnotatedString(displayUsername))
                    Toast.makeText(
                        context,
                        context.getString(R.string.profile_userid_permanent_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.profile_userid_label),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBackgroundLight
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariantLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.profile_cannot_be_changed),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                    Text(
                        text = displayUsername,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OnSurfaceVariantLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.profile_locked_cd),
                    tint = TextHintColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.profile_copy_userid_cd),
                    tint = PrimaryBlueLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 3. Email Address
        ProfileItemCard(
            icon = Icons.Default.Email,
            title = stringResource(R.string.profile_email_label),
            value = displayEmail,
            riseIndex = 2,
            onClick = { showEditEmailDialog = true }
        )

        // 4. Phone Number
        ProfileItemCard(
            icon = Icons.Default.Phone,
            title = stringResource(R.string.profile_phone_label),
            value = displayPhone,
            riseIndex = 3,
            onClick = { showEditPhoneDialog = true }
        )

        // 5. Address
        ProfileItemCard(
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.profile_address_label),
            value = if (addressCount > 0) {
                stringResource(R.string.saved_addresses_count_format, addressCount)
            } else {
                stringResource(R.string.profile_add_address_hint)
            },
            riseIndex = 4,
            onClick = {
                reportViewModel.selectReportSettingsMenu("saved_addresses")
            }
        )

        // SECTION 2: SECURITY & RECOVERY
        ProfileSectionTitle(stringResource(R.string.profile_section_security))

        // 6. Recovery Question
        ProfileItemCard(
            icon = Icons.Default.HelpOutline,
            title = stringResource(R.string.profile_recovery_question_label),
            value = if (displayRecoveryQuestion.isNotBlank()) {
                "$displayRecoveryQuestion ••••"
            } else {
                stringResource(R.string.profile_set_recovery_hint)
            },
            riseIndex = 5,
            onClick = { showEditRecoveryDialog = true }
        )

        // 7. Password
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cardRise(6)
                .depthCard()
                .heightIn(min = Dimens.MinTouchTarget)
                .clickable { showProfileChangePasswordDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ErrorContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = ErrorRedLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.password),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackgroundLight
                    )
                    Text(
                        text = "•••••••• • ${stringResource(R.string.change_password_desc)}",
                        fontSize = 13.sp,
                        color = OnSurfaceVariantLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextHintColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // A. Edit Full Name Dialog
    if (showEditNameDialog) {
        val parts = displayFullName.split(" ", limit = 2)
        EditFullNameDialog(
            initialFirst = prefs.getFirstName().ifBlank { parts.getOrNull(0).orEmpty() },
            initialLast = prefs.getLastName().ifBlank { parts.getOrNull(1).orEmpty() },
            onDismiss = { showEditNameDialog = false },
            onSave = { first, last ->
                showEditNameDialog = false
                requireAdminPin(context.getString(R.string.profile_auth_update_name)) {
                    val full = "$first $last".trim()
                    prefs.saveFirstName(first)
                    prefs.saveLastName(last)
                    prefs.saveFullName(full)
                    val current = userProfile ?: UserProfile()
                    reportViewModel.saveUserProfile(current.copy(fullName = full))
                    refreshTrigger++
                    Toast.makeText(context, context.getString(R.string.profile_name_updated_toast), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // B. Edit Email Dialog
    if (showEditEmailDialog) {
        EditEmailDialog(
            currentEmail = if (displayEmail == notSetText) "" else displayEmail,
            onDismiss = { showEditEmailDialog = false },
            onSave = { newEmail ->
                showEditEmailDialog = false
                requireAdminPin(context.getString(R.string.profile_auth_update_email)) {
                    prefs.saveEmail(newEmail)
                    val current = userProfile ?: UserProfile()
                    reportViewModel.saveUserProfile(current.copy(email = newEmail))
                    reportViewModel.saveBusinessProfile(bProfile.copy(email = newEmail))
                    refreshTrigger++
                    Toast.makeText(context, context.getString(R.string.profile_email_updated_toast), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // C. Edit Phone Dialog
    if (showEditPhoneDialog) {
        EditPhoneDialog(
            currentPhone = if (displayPhone == notSetText) "" else displayPhone,
            onDismiss = { showEditPhoneDialog = false },
            onSave = { newPhone ->
                showEditPhoneDialog = false
                requireAdminPin(context.getString(R.string.profile_auth_update_phone)) {
                    prefs.savePhone(newPhone)
                    val current = userProfile ?: UserProfile()
                    reportViewModel.saveUserProfile(current.copy(phone = newPhone))
                    reportViewModel.saveBusinessProfile(bProfile.copy(phone = newPhone))
                    refreshTrigger++
                    Toast.makeText(context, context.getString(R.string.profile_phone_updated_toast), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // D. Edit Recovery Question Dialog
    if (showEditRecoveryDialog) {
        val defaultRecoveryQuestion = recoveryQuestionOptions().first()
        EditRecoveryQuestionDialog(
            currentQuestion = displayRecoveryQuestion.ifBlank { defaultRecoveryQuestion },
            onDismiss = { showEditRecoveryDialog = false },
            onSave = { question, answer ->
                showEditRecoveryDialog = false
                requireAdminPin(context.getString(R.string.profile_auth_update_recovery)) {
                    val cleanQuestion = question.trim()
                    val hash = com.lojia.shiftreport.auth.SecurityRecoveryVerifier.hashNormalizedAnswer(answer)
                    prefs.saveRecoveryQuestion(cleanQuestion)
                    prefs.saveRecoveryAnswerHash(hash)
                    val current = userProfile ?: UserProfile()
                    reportViewModel.saveUserProfile(
                        current.copy(
                            securityQuestion = cleanQuestion,
                            securityAnswer = hash
                        )
                    )
                    refreshTrigger++
                    Toast.makeText(context, context.getString(R.string.profile_recovery_updated_toast), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // F. Change Password Dialog
    if (showProfileChangePasswordDialog) {
        ProfileChangePasswordDialog(
            currentPasswordHash = userProfile?.passwordHash.orEmpty(),
            onDismiss = { showProfileChangePasswordDialog = false },
            onSave = { newPassword ->
                showProfileChangePasswordDialog = false
                requireAdminPin(context.getString(R.string.profile_auth_change_password)) {
                    val newHash = SecurityUtils.hashSecret(newPassword)
                    val current = userProfile ?: UserProfile()
                    reportViewModel.saveUserProfile(current.copy(passwordHash = newHash))
                    Toast.makeText(context, context.getString(R.string.profile_password_changed_toast), Toast.LENGTH_SHORT).show()
                    onLogoutClick()
                }
            }
        )
    }

    // Unified Admin PIN Gate
    pendingAuthSubtitle?.let { sub ->
        AdminAuthDialog(
            subtitle = sub,
            onDismissRequest = {
                pendingAuthSubtitle = null
                pendingAuthAction = null
            },
            onAuthSuccess = {
                val action = pendingAuthAction
                pendingAuthSubtitle = null
                pendingAuthAction = null
                action?.invoke()
            }
        )
    }
}

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

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryBlueLight,
        modifier = Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .drawBehind {
                drawLine(
                    color = PrimaryBlueLight,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }
            .padding(start = 8.dp)
    )
}

@Composable
private fun ProfileItemCard(
    icon: ImageVector,
    title: String,
    value: String,
    riseIndex: Int = 0,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardRise(riseIndex)
            .depthCard()
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainerLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlueLight,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackgroundLight
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = OnSurfaceVariantLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextHintColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EditFullNameDialog(
    initialFirst: String,
    initialLast: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var firstName by remember { mutableStateOf(initialFirst) }
    var lastName by remember { mutableStateOf(initialLast) }
    val isValid = firstName.isNotBlank() && lastName.isNotBlank()

    LojiaDialog(
        onDismissRequest = onDismiss,
        maxWidth = Dimens.DialogMaxWidth
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.DialogPaddingComfortable)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlueLight)
                }
                Text(stringResource(R.string.profile_edit_full_name_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnBackgroundLight)
            }

            val firstNameLabel = stringResource(R.string.profile_first_name_label)
            LojiaTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(firstNameLabel) },
                contentDescription = firstNameLabel,
                singleLine = true,
                isError = firstName.isBlank(),
                supportingText = if (firstName.isBlank()) {
                    { Text(stringResource(R.string.profile_first_name_required)) }
                } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(Dimens.RadiusMd),
                modifier = Modifier.fillMaxWidth()
            )

            val lastNameLabel = stringResource(R.string.profile_last_name_label)
            LojiaTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text(lastNameLabel) },
                contentDescription = lastNameLabel,
                singleLine = true,
                isError = lastName.isBlank(),
                supportingText = if (lastName.isBlank()) {
                    { Text(stringResource(R.string.profile_last_name_required)) }
                } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(Dimens.RadiusMd),
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .depthSemanticCard(
                        backgroundColor = PrimaryContainerLight,
                        borderColor = OutlineLight,
                        shape = RoundedCornerShape(Dimens.RadiusMd)
                    )
            ) {
                Text(
                    text = stringResource(R.string.profile_full_name_info),
                    fontSize = 12.sp,
                    color = PrimaryBlueDark,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.cancel), color = OnSurfaceVariantLight)
                }
                Button(
                    onClick = { onSave(firstName.trim(), lastName.trim()) },
                    enabled = isValid,
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(enabled = isValid, cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var email by remember { mutableStateOf(currentEmail) }
    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val isDifferent = email.trim() != currentEmail.trim()

    LojiaDialog(
        onDismissRequest = onDismiss,
        maxWidth = Dimens.DialogMaxWidth
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.DialogPaddingComfortable)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlueLight)
                }
                Text(stringResource(R.string.profile_edit_email_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnBackgroundLight)
            }

            val emailRequiredLabel = stringResource(R.string.profile_email_required_label)
            LojiaTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(emailRequiredLabel) },
                contentDescription = emailRequiredLabel,
                singleLine = true,
                isError = email.isNotBlank() && !isEmailValid,
                supportingText = if (email.isNotBlank() && !isEmailValid) {
                    { Text(stringResource(R.string.profile_invalid_email_error), color = ErrorRedLight) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(Dimens.RadiusMd),
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .depthSemanticCard(
                        backgroundColor = PrimaryContainerLight,
                        borderColor = OutlineLight,
                        shape = RoundedCornerShape(Dimens.RadiusMd)
                    )
            ) {
                Text(
                    text = stringResource(R.string.profile_email_verification_note),
                    fontSize = 12.sp,
                    color = PrimaryBlueDark,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.cancel), color = OnSurfaceVariantLight)
                }
                Button(
                    onClick = { onSave(email.trim()) },
                    enabled = isEmailValid && isDifferent,
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(enabled = isEmailValid && isDifferent, cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditPhoneDialog(
    currentPhone: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    val detectedCountry = remember(context) { CountryDetector.detectDefaultAppCountry(context) }
    val initialPair = remember(currentPhone, detectedCountry) {
        if (currentPhone.isNotBlank()) {
            val (parsedCountry, parsedLocal) = parsePhoneNumberWithCountry(currentPhone, detectedCountry)
            parsedCountry to normalizeLocalInput(parsedLocal, parsedCountry)
        } else {
            detectedCountry to ""
        }
    }
    var selectedCountry by remember { mutableStateOf(initialPair.first) }
    var localNumber by remember { mutableStateOf(initialPair.second) }
    var showCountryPicker by remember { mutableStateOf(false) }

    val isValid = localNumber.isNotBlank() && isValidPhone(selectedCountry, localNumber)
    val showError = localNumber.isNotBlank() && !isValid
    val depthStyle = LocalDepthStyle.current
    val fieldColors = lojiaDefaultTextFieldColors().copy(disabledTextColor = OnSurfaceLight)

    LojiaDialog(
        onDismissRequest = onDismiss,
        maxWidth = Dimens.DialogMaxWidth
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.DialogPaddingComfortable)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.EmployeeAvatarTileSize)
                        .clip(CircleShape)
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlueLight)
                }
                Text(
                    text = stringResource(R.string.profile_edit_phone_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackgroundLight
                )
            }

            val countryCodeCd = stringResource(R.string.auth_select_country_code)
            val mobileHint = stringResource(R.string.profile_phone_mobile_hint)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(Dimens.DialCodeFieldWidth)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .clickable { showCountryPicker = true }
                        .testTag("btn_profile_phone_country_code")
                ) {
                    LojiaTextField(
                        value = selectedCountry.dialCode,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        contentDescription = countryCodeCd,
                        singleLine = true,
                        colors = fieldColors,
                        disabledTextColor = OnSurfaceLight,
                        disabledBackgroundOverride = depthStyle.insetBackground,
                        disabledBorderColorOverride = depthStyle.insetBorderColor.copy(alpha = depthStyle.insetBorderAlpha),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                LojiaTextField(
                    value = localNumber,
                    onValueChange = { raw ->
                        localNumber = normalizeLocalInput(raw, selectedCountry)
                    },
                    placeholder = { Text(mobileHint) },
                    contentDescription = mobileHint,
                    singleLine = true,
                    isError = showError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = fieldColors,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_profile_phone_local")
                )
            }

            if (showError) {
                Text(
                    text = stringResource(R.string.profile_phone_invalid),
                    color = ErrorRedLight,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.cancel), color = OnSurfaceVariantLight)
                }
                Button(
                    onClick = { onSave(buildE164Phone(selectedCountry, localNumber)) },
                    enabled = isValid,
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(enabled = isValid, cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showCountryPicker) {
        CountryCodePickerDialog(
            selectedCountry = selectedCountry,
            onCountrySelected = { country ->
                selectedCountry = country
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRecoveryQuestionDialog(
    currentQuestion: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selectedQuestion by remember { mutableStateOf(currentQuestion) }
    var expanded by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }
    var answerVisible by remember { mutableStateOf(false) }

    LojiaDialog(
        onDismissRequest = onDismiss,
        maxWidth = Dimens.DialogMaxWidth
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.DialogPaddingComfortable)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = PrimaryBlueLight)
                }
                Text(stringResource(R.string.profile_edit_recovery_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnBackgroundLight)
            }

            val recoveryQuestions = recoveryQuestionOptions()
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                val securityQuestionLabel = stringResource(R.string.security_question_label)
                LojiaTextField(
                    value = selectedQuestion,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(securityQuestionLabel) },
                    contentDescription = securityQuestionLabel,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    recoveryQuestions.forEach { q ->
                        DropdownMenuItem(
                            text = { Text(q) },
                            onClick = {
                                selectedQuestion = q
                                expanded = false
                            }
                        )
                    }
                }
            }

            val profileAnswerLabel = stringResource(R.string.profile_answer_label)
            LojiaTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text(profileAnswerLabel) },
                contentDescription = profileAnswerLabel,
                singleLine = true,
                visualTransformation = if (answerVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { answerVisible = !answerVisible },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (answerVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                shape = RoundedCornerShape(Dimens.RadiusMd),
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .depthSemanticCard(
                        backgroundColor = WarningContainer,
                        borderColor = WarningOrange.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(Dimens.RadiusMd)
                    )
            ) {
                Text(
                    text = stringResource(R.string.profile_recovery_info),
                    fontSize = 12.sp,
                    color = WarningText,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.cancel), color = OnSurfaceVariantLight)
                }
                Button(
                    onClick = { onSave(selectedQuestion, answer.trim()) },
                    enabled = answer.trim().length >= 3,
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(enabled = answer.trim().length >= 3, cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileChangePasswordDialog(
    currentPasswordHash: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val strengthLevel = when {
        newPass.length < 8 -> 0
        newPass.any { it.isDigit() } && newPass.any { !it.isLetterOrDigit() } -> 2
        else -> 1
    }
    val strengthLabel = when (strengthLevel) {
        0 -> stringResource(R.string.auth_weak)
        2 -> stringResource(R.string.auth_strong)
        else -> stringResource(R.string.profile_strength_medium)
    }
    val strengthColor = when (strengthLevel) {
        2 -> SuccessGreen
        1 -> WarningOrange
        else -> ErrorRedLight
    }

    val isValid = currentPass.isNotEmpty() &&
        newPass.length >= 8 &&
        newPass != currentPass &&
        confirmPass == newPass

    LojiaDialog(
        onDismissRequest = onDismiss,
        maxWidth = Dimens.DialogMaxWidth
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.DialogPaddingComfortable)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ErrorContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = ErrorRedLight)
                }
                Text(stringResource(R.string.change_password_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnBackgroundLight)
            }

            val currentPasswordLabel = stringResource(R.string.profile_current_password_label)
            LojiaTextField(
                value = currentPass,
                onValueChange = { currentPass = it; errorText = null },
                label = { Text(currentPasswordLabel) },
                contentDescription = currentPasswordLabel,
                singleLine = true,
                visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { showCurrent = !showCurrent },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                shape = RoundedCornerShape(Dimens.RadiusMd),
                modifier = Modifier.fillMaxWidth()
            )

            val newPasswordLabel = stringResource(R.string.profile_new_password_label)
            LojiaTextField(
                value = newPass,
                onValueChange = { newPass = it; errorText = null },
                label = { Text(newPasswordLabel) },
                contentDescription = newPasswordLabel,
                singleLine = true,
                visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { showNew = !showNew },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                shape = RoundedCornerShape(Dimens.RadiusMd),
                modifier = Modifier.fillMaxWidth()
            )

            if (newPass.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.profile_password_strength_format, strengthLabel),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = strengthColor
                )
            }

            val confirmPasswordLabel = stringResource(R.string.profile_confirm_password_label)
            LojiaTextField(
                value = confirmPass,
                onValueChange = { confirmPass = it; errorText = null },
                label = { Text(confirmPasswordLabel) },
                contentDescription = confirmPasswordLabel,
                singleLine = true,
                isError = confirmPass.isNotEmpty() && confirmPass != newPass,
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { showConfirm = !showConfirm },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                shape = RoundedCornerShape(Dimens.RadiusMd),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = ErrorRedLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariantLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthSecondaryButton(cornerRadius = Dimens.RadiusMd)
                ) {
                    Text(stringResource(R.string.cancel), color = OnSurfaceVariantLight)
                }
                Button(
                    onClick = {
                        val currentOk = currentPasswordHash.isBlank() ||
                            SecurityUtils.verifySecretWithUpgrade(currentPass, currentPasswordHash).isMatch
                        if (!currentOk) {
                            errorText = context.getString(R.string.profile_current_password_incorrect)
                        } else {
                            onSave(newPass)
                        }
                    },
                    enabled = isValid,
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PureWhite,
                        disabledContentColor = OnSurfaceVariantLight
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .depthRaisedButton(
                            enabled = isValid,
                            isDestructive = true,
                            cornerRadius = Dimens.RadiusMd
                        )
                ) {
                    Text(stringResource(R.string.profile_change_action), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
