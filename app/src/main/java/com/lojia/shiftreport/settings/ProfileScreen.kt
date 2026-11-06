package com.lojia.shiftreport.settings

import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.window.DialogProperties
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.data.UserProfile
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.settings.ProfileImageCropper.ProfileImageCropperDialog
import com.lojia.shiftreport.util.SecurityUtils

private val ProfileBlue = Color(0xFF2563EB)
private val ProfileBg = Color(0xFFF9FAFB)
private val ProfileTextBlack = Color(0xFF111827)
private val ProfileTextGray = Color(0xFF6B7280)
private val ProfileBorderLight = Color(0xFFE5E7EB)
private val ProfileRed = Color(0xFFDC2626)
private val ProfileRedBg = Color(0xFFFEF2F2)

private val RECOVERY_QUESTIONS = listOf(
    "What was your first pet's name?",
    "What city were you born in?",
    "What is your mother's maiden name?",
    "What was your first school?",
    "What is your favorite teacher?"
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

    // Read real registration & profile data
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val displayFullName = remember(userProfile, refreshTrigger) {
        prefs.getFullName()
            .ifBlank { "${prefs.getFirstName()} ${prefs.getLastName()}".trim() }
            .ifBlank { userProfile?.fullName.orEmpty() }
            .ifBlank { "Store Owner" }
    }
    val displayUsername = remember(userProfile, refreshTrigger) {
        prefs.getSavedUsername()
            .ifBlank { userProfile?.username.orEmpty() }
            .ifBlank { "admin" }
    }
    val displayEmail = remember(userProfile, bProfile, refreshTrigger) {
        prefs.getEmail()
            .ifBlank { userProfile?.email.orEmpty() }
            .ifBlank { bProfile.email }
            .ifBlank { "Not set" }
    }
    val displayPhone = remember(userProfile, bProfile, refreshTrigger) {
        prefs.getPhone()
            .ifBlank { userProfile?.phone.orEmpty() }
            .ifBlank { bProfile.phone }
            .ifBlank { "Not set" }
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
            .background(ProfileBg)
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
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
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
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap!!,
                        contentDescription = "Profile Picture",
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
                            .background(Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayFullName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfileBlue
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ProfileBlue)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = displayFullName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ProfileTextBlack,
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
                    Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    showCropperDialog = false
                    imageUriToCrop = null
                }
            )
        }

        // SECTION 1: PERSONAL INFORMATION
        ProfileSectionTitle("PERSONAL INFORMATION")

        // 1. Full Name
        ProfileItemCard(
            icon = Icons.Default.Person,
            title = "Full Name",
            value = displayFullName,
            onClick = { showEditNameDialog = true }
        )

        // 2. User ID (READ-ONLY)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ProfileBg)
                .drawBehind {
                    drawRoundRect(
                        color = Color(0xFFD1D5DB),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }
                .clickable {
                    clipboardManager.setText(AnnotatedString(displayUsername))
                    Toast.makeText(
                        context,
                        "User ID cannot be changed. Permanent login ID.",
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
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = ProfileTextGray,
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
                            text = "User ID",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfileTextBlack
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Cannot be changed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = ProfileTextGray
                            )
                        }
                    }
                    Text(
                        text = displayUsername,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ProfileTextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy User ID",
                    tint = ProfileBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 3. Email Address
        ProfileItemCard(
            icon = Icons.Default.Email,
            title = "Email Address",
            value = displayEmail,
            onClick = { showEditEmailDialog = true }
        )

        // 4. Phone Number
        ProfileItemCard(
            icon = Icons.Default.Phone,
            title = "Phone Number",
            value = displayPhone,
            onClick = { showEditPhoneDialog = true }
        )

        // 5. Address
        ProfileItemCard(
            icon = Icons.Default.LocationOn,
            title = "Address",
            value = if (addressCount > 0) {
                "$addressCount addresses saved"
            } else {
                "Add store or branch address"
            },
            onClick = {
                reportViewModel.selectReportSettingsMenu("saved_addresses")
            }
        )

        // SECTION 2: SECURITY & RECOVERY
        ProfileSectionTitle("SECURITY & RECOVERY")

        // 6. Recovery Question
        ProfileItemCard(
            icon = Icons.Default.HelpOutline,
            title = "Recovery Question",
            value = if (displayRecoveryQuestion.isNotBlank()) {
                "$displayRecoveryQuestion ••••"
            } else {
                "Set security recovery question"
            },
            onClick = { showEditRecoveryDialog = true }
        )

        // 7. Password
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, ProfileBorderLight),
            modifier = Modifier
                .fillMaxWidth()
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
                        .background(ProfileRedBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = ProfileRed,
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
                        text = "Password",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileTextBlack
                    )
                    Text(
                        text = "•••••••• • Change administrator master password",
                        fontSize = 13.sp,
                        color = ProfileTextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
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
                requireAdminPin("Update Full Name") {
                    val full = "$first $last".trim()
                    prefs.saveFirstName(first)
                    prefs.saveLastName(last)
                    prefs.saveFullName(full)
                    val current = userProfile ?: UserProfile()
                    reportViewModel.saveUserProfile(current.copy(fullName = full))
                    refreshTrigger++
                    Toast.makeText(context, "Full name updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // B. Edit Email Dialog
    if (showEditEmailDialog) {
        EditEmailDialog(
            currentEmail = if (displayEmail == "Not set") "" else displayEmail,
            onDismiss = { showEditEmailDialog = false },
            onSave = { newEmail ->
                showEditEmailDialog = false
                requireAdminPin("Update Email Address") {
                    prefs.saveEmail(newEmail)
                    val current = userProfile ?: UserProfile()
                    reportViewModel.saveUserProfile(current.copy(email = newEmail))
                    reportViewModel.saveBusinessProfile(bProfile.copy(email = newEmail))
                    refreshTrigger++
                    Toast.makeText(context, "Email updated - Verification sent", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // C. Edit Phone Dialog
    if (showEditPhoneDialog) {
        EditPhoneDialog(
            currentPhone = if (displayPhone == "Not set") "" else displayPhone,
            onDismiss = { showEditPhoneDialog = false },
            onSave = { newPhone ->
                showEditPhoneDialog = false
                requireAdminPin("Update Phone Number") {
                    prefs.savePhone(newPhone)
                    val current = userProfile ?: UserProfile()
                    reportViewModel.saveUserProfile(current.copy(phone = newPhone))
                    reportViewModel.saveBusinessProfile(bProfile.copy(phone = newPhone))
                    refreshTrigger++
                    Toast.makeText(context, "Phone number updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // D. Edit Recovery Question Dialog
    if (showEditRecoveryDialog) {
        EditRecoveryQuestionDialog(
            currentQuestion = displayRecoveryQuestion.ifBlank { RECOVERY_QUESTIONS.first() },
            onDismiss = { showEditRecoveryDialog = false },
            onSave = { question, answer ->
                showEditRecoveryDialog = false
                requireAdminPin("Update Recovery Question") {
                    val hash = SecurityUtils.hashSecret(answer)
                    prefs.saveRecoveryQuestion(question)
                    prefs.saveRecoveryAnswerHash(hash)
                    val current = userProfile ?: UserProfile()
                    reportViewModel.saveUserProfile(
                        current.copy(
                            securityQuestion = question,
                            securityAnswer = hash
                        )
                    )
                    refreshTrigger++
                    Toast.makeText(context, "Recovery question updated", Toast.LENGTH_SHORT).show()
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
                requireAdminPin("Change Master Password") {
                    val newHash = SecurityUtils.hashSecret(newPassword)
                    val current = userProfile ?: UserProfile()
                    reportViewModel.saveUserProfile(current.copy(passwordHash = newHash))
                    Toast.makeText(context, "Password changed. Please sign in again.", Toast.LENGTH_SHORT).show()
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
        color = ProfileBlue,
        modifier = Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .drawBehind {
                drawLine(
                    color = ProfileBlue,
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
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, ProfileBorderLight),
        modifier = Modifier
            .fillMaxWidth()
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
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ProfileBlue,
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
                    color = ProfileTextBlack
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = ProfileTextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ProfileBlue)
                        }
                        Text("Edit Full Name", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ProfileTextBlack)
                    }

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name *") },
                        singleLine = true,
                        isError = firstName.isBlank(),
                        supportingText = if (firstName.isBlank()) {
                            { Text("First name is required") }
                        } else null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name *") },
                        singleLine = true,
                        isError = lastName.isBlank(),
                        supportingText = if (lastName.isBlank()) {
                            { Text("Last name is required") }
                        } else null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ProfileBlue.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "This will be shown as your full name in reports and profile.",
                            fontSize = 12.sp,
                            color = ProfileBlue,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = ProfileTextGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(firstName.trim(), lastName.trim()) },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(containerColor = ProfileBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = ProfileBlue)
                        }
                        Text("Edit Email Address", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ProfileTextBlack)
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *") },
                        singleLine = true,
                        isError = email.isNotBlank() && !isEmailValid,
                        supportingText = if (email.isNotBlank() && !isEmailValid) {
                            { Text("Enter a valid email address", color = ProfileRed) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ProfileBlue.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Verification email will be sent to new address.",
                            fontSize = 12.sp,
                            color = ProfileBlue,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = ProfileTextGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(email.trim()) },
                            enabled = isEmailValid && isDifferent,
                            colors = ButtonDefaults.buttonColors(containerColor = ProfileBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
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
    var phone by remember { mutableStateOf(currentPhone) }
    val digitsCount = phone.count { it.isDigit() }
    val isValid = digitsCount in 10..15

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = ProfileBlue)
                        }
                        Text("Edit Phone Number", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ProfileTextBlack)
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number *") },
                        placeholder = { Text("+966 50 000 0000") },
                        singleLine = true,
                        isError = phone.isNotBlank() && !isValid,
                        supportingText = if (phone.isNotBlank() && !isValid) {
                            { Text("Enter 10 to 15 digits", color = ProfileRed) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = ProfileTextGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(phone.trim()) },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(containerColor = ProfileBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = ProfileBlue)
                        }
                        Text("Edit Recovery Question", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ProfileTextBlack)
                    }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedQuestion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Security Question") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            RECOVERY_QUESTIONS.forEach { q ->
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

                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text("Answer * (min 3 chars)") },
                        singleLine = true,
                        visualTransformation = if (answerVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { answerVisible = !answerVisible }) {
                                Icon(
                                    imageVector = if (answerVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "This will be used to recover your account if you forget password. Keep it secure.",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = ProfileTextGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(selectedQuestion, answer.trim()) },
                            enabled = answer.trim().length >= 3,
                            colors = ButtonDefaults.buttonColors(containerColor = ProfileBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
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
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val strengthLabel = when {
        newPass.length < 8 -> "Weak"
        newPass.any { it.isDigit() } && newPass.any { !it.isLetterOrDigit() } -> "Strong"
        else -> "Medium"
    }
    val strengthColor = when (strengthLabel) {
        "Strong" -> Color(0xFF10B981)
        "Medium" -> Color(0xFFF59E0B)
        else -> ProfileRed
    }

    val isValid = currentPass.isNotEmpty() &&
        newPass.length >= 8 &&
        newPass != currentPass &&
        confirmPass == newPass

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ProfileRedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = ProfileRed)
                        }
                        Text("Change Password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ProfileTextBlack)
                    }

                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it; errorText = null },
                        label = { Text("Current Password *") },
                        singleLine = true,
                        visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurrent = !showCurrent }) {
                                Icon(if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; errorText = null },
                        label = { Text("New Password * (min 8 chars)") },
                        singleLine = true,
                        visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNew = !showNew }) {
                                Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (newPass.isNotEmpty()) {
                        Text(
                            text = "Password strength: $strengthLabel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = strengthColor
                        )
                    }

                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it; errorText = null },
                        label = { Text("Confirm New Password *") },
                        singleLine = true,
                        isError = confirmPass.isNotEmpty() && confirmPass != newPass,
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            color = ProfileRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = ProfileTextGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val currentOk = currentPasswordHash.isBlank() ||
                                    SecurityUtils.verifySecretWithUpgrade(currentPass, currentPasswordHash).isMatch
                                if (!currentOk) {
                                    errorText = "Current password is incorrect"
                                } else {
                                    onSave(newPass)
                                }
                            },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(containerColor = ProfileRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Change", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
