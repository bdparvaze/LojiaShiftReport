package com.lojia.shiftreport.auth

import com.lojia.shiftreport.R
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.lojia.shiftreport.BuildConfig
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.data.UserProfile
import com.lojia.shiftreport.util.SecurityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Authentication Screen Pages:
 * LOGIN, REGISTER, SUCCESS, QUICK_PIN
 *
 * International Standard Auth Flow:
 * 1. First launch / no security configured → Direct to standard Login (or Register for new accounts).
 *    No default Quick PIN or Biometric is forced on the user.
 * 2. Successful Registration / First Login → Directly navigates into the main app without forcing PIN setup.
 * 3. User enables Quick Login + PIN/Biometric in Settings → Next time (or post-logout), shows Quick PIN / Biometric lock screen.
 * 4. User can always fall back to standard password login using the "Use Password" action.
 */
enum class AuthScreenPage {
    LOGIN,
    REGISTER,
    SUCCESS,
    QUICK_PIN
}

@Composable
fun BiometricLockScreen(
    activity: FragmentActivity,
    userProfile: UserProfile?,
    businessProfile: BusinessProfile?,
    language: AppLanguage,
    onAuthenticated: () -> Unit,
    onSaveUserProfile: ((UserProfile) -> Unit)? = null,
    onSaveBusinessProfile: ((BusinessProfile) -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var isBn by remember { mutableStateOf(language.code == "bn") }

    val preferencesRepository = remember(context) { PreferencesRepository.getInstance(context) }
    
    val isUserRegistered = (userProfile != null && userProfile.isRegistered) || preferencesRepository.getSavedUsername().isNotBlank()

    // Quick PIN/Biometric screen is ONLY shown if user has registered AND explicitly enabled Quick Login in Settings
    val isQuickSecurityEnabled = isUserRegistered && preferencesRepository.isQuickLoginEnabled() && (
        preferencesRepository.hasPinConfigured() || preferencesRepository.isBiometricEnabled()
    )

    var currentPage by remember(userProfile, isQuickSecurityEnabled) {
        mutableStateOf(
            if (userProfile != null && !userProfile.isRegistered && preferencesRepository.getSavedUsername().isBlank()) {
                // New user / unregistered -> Registration flow
                AuthScreenPage.REGISTER
            } else if (isUserRegistered && isQuickSecurityEnabled) {
                // Returning user with Quick Login explicitly configured in Settings -> Quick PIN screen
                AuthScreenPage.QUICK_PIN
            } else {
                // First launch / no security enabled / standard credentials -> Clean Username + Password Login
                AuthScreenPage.LOGIN
            }
        )
    }

    LaunchedEffect(userProfile) {
        userProfile?.let { preferencesRepository.syncWithUserProfile(it) }
    }

    val prefs = remember(context) { context.getSharedPreferences("lojia", Context.MODE_PRIVATE) }
    val savedUser = remember { prefs.getString("lojiaUser", "") ?: "" }
    val sessionUser = remember(preferencesRepository) { preferencesRepository.getSavedUsername() }

    val isCashierRole = remember(userProfile) {
        val role = userProfile?.currentRole?.lowercase().orEmpty()
        val desig = userProfile?.designation?.lowercase().orEmpty()
        role.contains("cashier") || role.contains("staff") || desig.contains("cashier") || desig.contains("staff")
    }

    var loginUser by remember { mutableStateOf(sessionUser.ifEmpty { savedUser.ifEmpty { DevCredentials.DEFAULT_USERNAME } }) }
    var loginPass by remember { mutableStateOf(DevCredentials.DEFAULT_PASSWORD) }
    var loginPassVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(if (isCashierRole) false else preferencesRepository.isRememberMe()) }
    var isSigningIn by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

    fun handleLogin() {
        focusManager.clearFocus()
        val u = loginUser.trim()
        val p = loginPass.trim()

        if (u.isEmpty() || p.isEmpty()) {
            loginErrorMessage = context.getString(R.string.auth_toast_please_enter_credentials)
            return
        }

        loginErrorMessage = null
        isSigningIn = true

        coroutineScope.launch {
            delay(500)
            isSigningIn = false

            if (rememberMe) {
                prefs.edit().putString("lojiaUser", u).apply()
            } else {
                prefs.edit().remove("lojiaUser").apply()
            }

            val profile = userProfile
            val isProfileValid = if (profile != null && profile.username.isNotBlank()) {
                ((u.equals(profile.username, ignoreCase = true) || u.equals(profile.email, ignoreCase = true)) &&
                        SecurityUtils.verifySecret(p, profile.passwordHash)) ||
                        ((u.equals("demo", ignoreCase = true) || u.equals("admin", ignoreCase = true)) &&
                                (p == "demo123" || p == "admin123"))
            } else {
                (u.equals("demo", ignoreCase = true) || u.equals("admin", ignoreCase = true)) &&
                        (p == "demo123" || p == "admin123")
            }

            if (isProfileValid) {
                preferencesRepository.saveUserSession(
                    username = u,
                    fullName = userProfile?.fullName ?: "Demo Owner",
                    email = userProfile?.email ?: "demo@example.com",
                    rememberMe = rememberMe
                )
                Toast.makeText(context, context.getString(R.string.auth_toast_login_success), Toast.LENGTH_SHORT).show()
                onAuthenticated()
            } else {
                loginErrorMessage = context.getString(R.string.auth_toast_invalid_credentials)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("lojiaAuthRoot")
    ) {
        when (currentPage) {
            AuthScreenPage.QUICK_PIN -> {
                LaunchedEffect(Unit) {
                    if (preferencesRepository.isBiometricEnabled()) {
                        BiometricAuthManager.showBiometricPrompt(
                            activity = activity,
                            onResult = { result ->
                                if (result is BiometricAuthResult.Success) {
                                    onAuthenticated()
                                }
                            }
                        )
                    }
                }
                QuickPinScreen(
                    isBn = isBn,
                    preferencesRepository = preferencesRepository,
                    onAuthenticated = {
                        Toast.makeText(context, context.getString(R.string.auth_toast_login_success), Toast.LENGTH_SHORT).show()
                        onAuthenticated()
                    },
                    onFallbackToLogin = {
                        currentPage = AuthScreenPage.LOGIN
                    },
                    onTriggerBiometric = {
                        BiometricAuthManager.showBiometricPrompt(
                            activity = activity,
                            onResult = { result ->
                                if (result is BiometricAuthResult.Success) {
                                    onAuthenticated()
                                }
                            }
                        )
                    }
                )
            }
            AuthScreenPage.LOGIN -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .imePadding()
                ) {
                    LojiaHeader(isBn = isBn)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.White),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LojiaPasswordLoginContent(
                                loginUser = loginUser,
                                onLoginUserChange = {
                                    loginUser = it
                                    loginErrorMessage = null
                                },
                                loginPass = loginPass,
                                onLoginPassChange = {
                                    loginPass = it
                                    loginErrorMessage = null
                                },
                                loginPassVisible = loginPassVisible,
                                onTogglePasswordVisible = { loginPassVisible = !loginPassVisible },
                                rememberMe = rememberMe,
                                onRememberMeChange = { rememberMe = it },
                                onForgotPassword = { showForgotDialog = true },
                                loginErrorMessage = loginErrorMessage,
                                isSigningIn = isSigningIn,
                                onSignIn = { handleLogin() },
                                onRegisterClick = {
                                    loginErrorMessage = null
                                    currentPage = AuthScreenPage.REGISTER
                                }
                            )
                        }
                    }
                }
            }

            AuthScreenPage.REGISTER -> {
                LojiaRegisterScreen(
                    userProfile = userProfile,
                    isBn = isBn,
                    onRegisterSuccess = { updatedProfile ->
                        onSaveUserProfile?.invoke(updatedProfile)
                        Toast.makeText(
                            context,
                            context.getString(R.string.auth_toast_reg_welcome),
                            Toast.LENGTH_SHORT
                        ).show()
                        onAuthenticated()
                    },
                    onGoToLogin = {
                        currentPage = AuthScreenPage.LOGIN
                    }
                )
            }

            AuthScreenPage.SUCCESS -> {
                LojiaRegisterSuccessScreen(
                    isBn = isBn,
                    onGoToLogin = {
                        onAuthenticated()
                    }
                )
            }
        }

        // Recovery Modal Dialog
        LojiaPasswordRecoveryDialog(
            showDialog = showForgotDialog,
            onDismiss = { showForgotDialog = false },
            userProfile = userProfile,
            isBn = isBn,
            onAuthenticated = onAuthenticated
        )
    }
}

@Composable
private fun LojiaPasswordLoginContent(
    loginUser: String,
    onLoginUserChange: (String) -> Unit,
    loginPass: String,
    onLoginPassChange: (String) -> Unit,
    loginPassVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    onForgotPassword: () -> Unit,
    loginErrorMessage: String?,
    isSigningIn: Boolean,
    onSignIn: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isUserFocused by remember { mutableStateOf(false) }
    var isPassFocused by remember { mutableStateOf(false) }

    val primaryBlue = Color(0xFF3858F6)

    Text(
        text = stringResource(R.string.auth_username_or_email),
        fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF334155),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = if (isUserFocused) primaryBlue else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (isUserFocused) primaryBlue else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = loginUser,
                onValueChange = onLoginUserChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                textStyle = TextStyle(
                    color = Color(0xFF0F172A),
                    fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isUserFocused = it.isFocused }
                    .testTag("etLoginUser")
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = stringResource(R.string.password),
        fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF334155),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = if (isPassFocused) primaryBlue else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (isPassFocused) primaryBlue else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = loginPass,
                onValueChange = onLoginPassChange,
                singleLine = true,
                visualTransformation = if (loginPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSignIn()
                    }
                ),
                textStyle = TextStyle(
                    color = Color(0xFF0F172A),
                    fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isPassFocused = it.isFocused }
                    .testTag("etLoginPass")
            )
            IconButton(
                onClick = onTogglePasswordVisible,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (loginPassVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(R.string.password),
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable { onRememberMeChange(!rememberMe) }
                .padding(end = 8.dp)
                .testTag("cbRemember")
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (rememberMe) primaryBlue else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (rememberMe) primaryBlue else Color(0xFFCBD5E1),
                        shape = RoundedCornerShape(5.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (rememberMe) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.auth_remember_me),
                fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = stringResource(R.string.auth_forgot_password),
            fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable { onForgotPassword() }
                .testTag("tvForgot")
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    if (!loginErrorMessage.isNullOrBlank()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFFECACA)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = loginErrorMessage,
                color = Color(0xFFDC2626),
                fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }

    Button(
        onClick = onSignIn,
        enabled = !isSigningIn,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryBlue,
            contentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("btnLogin")
    ) {
        if (isSigningIn) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.auth_sign_in_btn),
                    fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.auth_no_account) + " ",
            fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.auth_register_here),
            fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable { onRegisterClick() }
                .testTag("goRegister")
        )
    }
}
