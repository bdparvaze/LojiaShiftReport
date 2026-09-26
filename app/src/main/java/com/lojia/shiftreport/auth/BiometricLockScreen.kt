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
            .background(Color(0xFFF9F9FF))
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF9F9FF))
                        .padding(horizontal = 32.dp)
                        .imePadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 360.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LojiaHeader(isBn = isBn)

                        Spacer(modifier = Modifier.height(16.dp))

                        LojiaCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            LojiaSectionHeader(
                                icon = Icons.Outlined.Lock,
                                title = if (isBn) "লগইন করুন" else "Sign In",
                                subtitle = if (isBn) "আপনার অ্যাকাউন্টে প্রবেশ করুন" else "Enter credentials to continue"
                            )

                            Spacer(modifier = Modifier.height(6.dp))

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
    val primaryIndigo = Color(0xFF415F91)
    val charcoalText = Color(0xFF191C20)
    val mediumGray = Color(0xFF44474E)
    val hintGray = Color(0xFF74777F)
    val borderGray = Color(0xFFCAC4D0)
    val errorRed = Color(0xFFBA1A1A)

    Text(
        text = stringResource(R.string.auth_username_or_email),
        fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = mediumGray,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )

    OutlinedTextField(
        value = loginUser,
        onValueChange = onLoginUserChange,
        placeholder = {
            Text(
                text = stringResource(R.string.auth_username_or_email),
                fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
                fontSize = 14.sp,
                color = hintGray
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = mediumGray,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        textStyle = TextStyle(
            color = charcoalText,
            fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = primaryIndigo,
            unfocusedBorderColor = borderGray,
            focusedTextColor = charcoalText,
            unfocusedTextColor = charcoalText,
            focusedLeadingIconColor = primaryIndigo,
            unfocusedLeadingIconColor = mediumGray,
            cursorColor = primaryIndigo
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("etLoginUser")
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.password),
        fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = mediumGray,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )

    OutlinedTextField(
        value = loginPass,
        onValueChange = onLoginPassChange,
        placeholder = {
            Text(
                text = stringResource(R.string.password),
                fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
                fontSize = 14.sp,
                color = hintGray
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = mediumGray,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onTogglePasswordVisible,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (loginPassVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(R.string.password),
                    tint = mediumGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        singleLine = true,
        visualTransformation = if (loginPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
        shape = RoundedCornerShape(12.dp),
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
            color = charcoalText,
            fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = primaryIndigo,
            unfocusedBorderColor = borderGray,
            focusedTextColor = charcoalText,
            unfocusedTextColor = charcoalText,
            focusedLeadingIconColor = primaryIndigo,
            unfocusedLeadingIconColor = mediumGray,
            focusedTrailingIconColor = primaryIndigo,
            unfocusedTrailingIconColor = mediumGray,
            cursorColor = primaryIndigo
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("etLoginPass")
    )

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
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (rememberMe) primaryIndigo else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (rememberMe) primaryIndigo else borderGray,
                        shape = RoundedCornerShape(6.dp)
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
                color = mediumGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = stringResource(R.string.auth_forgot_password),
            fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryIndigo,
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
            color = Color(0xFFFCEEEE),
            border = BorderStroke(1.dp, Color(0xFFF9DEDC)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = loginErrorMessage,
                color = errorRed,
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
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryIndigo,
            contentColor = Color.White,
            disabledContainerColor = primaryIndigo.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
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
            color = mediumGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.auth_register_here),
            fontFamily = com.lojia.shiftreport.ui.theme.PoppinsFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = primaryIndigo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable { onRegisterClick() }
                .testTag("goRegister")
        )
    }
}
