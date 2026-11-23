package com.lojia.shiftreport.auth

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager as AndroidXBiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.data.UserProfile
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.SecurityUtils
import com.lojia.shiftreport.util.isPinUsable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    data class Error(val errorCode: Int, val errString: CharSequence) : BiometricAuthResult()
    object Failed : BiometricAuthResult()
    object UserCanceled : BiometricAuthResult()
    object UsePinFallback : BiometricAuthResult()
}

enum class AuthScreenPage {
    LOGIN,
    REGISTER,
    SUCCESS,
    QUICK_PIN
}

/**
 * Unified Biometric & Authentication Manager for Lojia POS.
 * Combines BiometricAuthManager, BiometricHelper, and BiometricLockScreen into a single cohesive unit.
 */
object BiometricManager {

    private const val KEY_BIOMETRIC_PREF_ENABLED = "biometric_auth_pref_enabled"

    /**
     * Checks if biometric hardware is present and ready for authentication.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val manager = AndroidXBiometricManager.from(context)
        return manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == AndroidXBiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Alias for isBiometricAvailable.
     */
    fun isHardwareAvailable(context: Context): Boolean = isBiometricAvailable(context)

    /**
     * Returns raw AndroidX biometric authentication capability code.
     */
    fun canAuthenticate(context: Context): Int {
        val manager = AndroidXBiometricManager.from(context)
        return manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
    }

    /**
     * Checks status with categorized enum (AVAILABLE, NOT_ENROLLED, NO_HARDWARE, UNAVAILABLE).
     */
    fun checkBiometricAvailability(context: Context): BiometricStatus {
        val biometricManager = AndroidXBiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            AndroidXBiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            AndroidXBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            AndroidXBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun getStatusDescription(context: Context, status: BiometricStatus): String {
        return when (status) {
            BiometricStatus.AVAILABLE -> context.getString(R.string.biometric_auth)
            BiometricStatus.NOT_ENROLLED -> context.getString(R.string.biometric_disabled_desc)
            BiometricStatus.NO_HARDWARE -> context.getString(R.string.biometric_not_supported_window)
            BiometricStatus.UNAVAILABLE -> context.getString(R.string.biometric_disabled_desc)
        }
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return SecureStorageManager.getEncryptedPrefs(context).getBoolean(KEY_BIOMETRIC_PREF_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        SecureStorageManager.getEncryptedPrefs(context).edit()
            .putBoolean(KEY_BIOMETRIC_PREF_ENABLED, enabled)
            .apply()
    }

    /**
     * Standard simplified biometric prompt helper.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit = {},
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> }
    ) {
        showBiometricPrompt(
            activity = activity,
            title = activity.getString(R.string.biometric_prompt_default_title),
            subtitle = activity.getString(R.string.biometric_prompt_default_subtitle),
            description = activity.getString(R.string.biometric_auth_description)
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> onSuccess()
                is BiometricAuthResult.Error -> onError(result.errorCode, result.errString)
                is BiometricAuthResult.Failed -> onError(-2, activity.getString(R.string.biometric_auth_failed))
                is BiometricAuthResult.UserCanceled -> onError(-3, activity.getString(R.string.biometric_auth_cancelled))
                else -> onError(-1, activity.getString(R.string.biometric_auth_error))
            }
        }
    }

    /**
     * Full detailed biometric prompt launcher with custom text and result handler.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = activity.getString(R.string.biometric_prompt_default_title),
        subtitle: String = activity.getString(R.string.biometric_prompt_default_subtitle),
        description: String = activity.getString(R.string.biometric_auth_description),
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED) {
                    onResult(BiometricAuthResult.UserCanceled)
                } else {
                    onResult(BiometricAuthResult.Error(errorCode, errString))
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthResult.Failed)
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        val biometricManager = AndroidXBiometricManager.from(activity)
        val canDeviceCred = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == AndroidXBiometricManager.BIOMETRIC_SUCCESS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && canDeviceCred) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            promptInfoBuilder.setNegativeButtonText(activity.getString(R.string.use_security_pin))
        }

        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            onResult(BiometricAuthResult.Error(-1, e.message ?: activity.getString(R.string.biometric_auth_init_error)))
        }
    }

    /**
     * Legacy authenticate helper from BiometricHelper.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(title)
            subtitle?.let { setSubtitle(it) }
            setNegativeButtonText(negativeButtonText)
            setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        }.build()

        prompt.authenticate(promptInfo)
    }

    /**
     * Composable lock screen entry point on BiometricManager.
     */
    @Composable
    fun BiometricLockScreen(onUnlocked: () -> Unit) {
        val context = LocalContext.current
        val activity = context as? FragmentActivity
        if (activity != null) {
            com.lojia.shiftreport.auth.BiometricLockScreen(
                activity = activity,
                userProfile = null,
                businessProfile = null,
                language = AppLanguage.ENGLISH,
                onAuthenticated = onUnlocked
            )
        }
    }
}

// Backward-compatibility aliases so existing modules continue to compile cleanly
typealias BiometricAuthManager = BiometricManager
typealias BiometricHelper = BiometricManager

/**
 * Top-level BiometricLockScreen for Lojia POS.
 * Handles Quick PIN, Biometric prompt, and standard username/password login.
 */
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

    var currentPage by remember(isUserRegistered, isQuickSecurityEnabled) {
        mutableStateOf(
            if (isUserRegistered && isQuickSecurityEnabled) {
                AuthScreenPage.QUICK_PIN
            } else {
                AuthScreenPage.LOGIN
            }
        )
    }

    LaunchedEffect(userProfile) {
        userProfile?.let { preferencesRepository.syncWithUserProfile(it) }
    }

    val secureStorageManager = SecureStorageManager

    var loginUser by remember { mutableStateOf("") }
    var loginPass by remember { mutableStateOf("") }
    var loginPassVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) } // MUST be false by default - Foodics / POS Banking Standard
    var isSigningIn by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

    // Load saved username on start - Foodics / Banking POS standard
    LaunchedEffect(Unit) {
        val savedUsername = preferencesRepository.getRememberedUsername()
        if (!savedUsername.isNullOrBlank()) {
            loginUser = savedUsername
            rememberMe = true
        } else {
            rememberMe = false
        }
        try {
            context.getSharedPreferences("lojia", Context.MODE_PRIVATE).edit().remove("lojiaUser").apply()
        } catch (_: Exception) {}
    }

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
            delay(300)
            isSigningIn = false

            // International POS Security Standard: Save only username, NEVER password
            if (rememberMe) {
                secureStorageManager.saveRememberedUsername(u)
            } else {
                secureStorageManager.clearRememberedUsername()
            }

            val profile = userProfile
            var isProfileValid = false

            if (profile != null && profile.username.isNotBlank() &&
                isPinUsable(p) && isPinUsable(profile.passwordHash)
            ) {
                val usernameMatch = (u.equals(profile.username, ignoreCase = true) || u.equals(profile.email, ignoreCase = true))
                val verifyResult = SecurityUtils.verifySecretWithUpgrade(p, profile.passwordHash)
                if (usernameMatch && verifyResult.isMatch) {
                    isProfileValid = true
                    if (verifyResult.newHashToStore != null) {
                        val db = com.lojia.shiftreport.data.AppDatabase.getInstance(context)
                        db.reportDao().saveUserProfile(profile.copy(passwordHash = verifyResult.newHashToStore))
                    }
                }
            }

            if (isProfileValid) {
                preferencesRepository.saveUserSession(
                    username = u,
                    fullName = userProfile?.fullName.orEmpty(),
                    email = userProfile?.email.orEmpty(),
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
            .background(LocalDepthStyle.current.pageBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("lojiaAuthRoot")
    ) {
        when (currentPage) {
            AuthScreenPage.QUICK_PIN -> {
                LaunchedEffect(Unit) {
                    if (preferencesRepository.isBiometricEnabled()) {
                        BiometricManager.showBiometricPrompt(
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
                        BiometricManager.showBiometricPrompt(
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
                        .background(LocalDepthStyle.current.pageBackground)
                        .imePadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = Dimens.AuthCardMaxWidth + Dimens.AuthScreenSidePadding * 2)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Dimens.AuthScreenSidePadding, vertical = Dimens.SpacingLg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LojiaHeader(isBn = isBn)

                        Spacer(modifier = Modifier.height(10.dp))

                        LojiaCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .cardRise(0)
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
                                onRememberMeChange = { isChecked ->
                                    rememberMe = isChecked
                                    if (!isChecked) {
                                        coroutineScope.launch {
                                            secureStorageManager.clearRememberedUsername()
                                            preferencesRepository.clearRememberedUsername()
                                        }
                                    }
                                },
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
                BackHandler {
                    currentPage = AuthScreenPage.LOGIN
                }
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

/**
 * Simplified single-argument lock screen overload.
 */
@Composable
fun BiometricLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    if (activity != null) {
        BiometricLockScreen(
            activity = activity,
            userProfile = null,
            businessProfile = null,
            language = AppLanguage.ENGLISH,
            onAuthenticated = onUnlocked
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
    val primaryIndigo = PrimaryBlueLight
    val charcoalText = OnSurfaceLight
    val mediumGray = OnSurfaceVariantLight
    val hintGray = DepthTokens.InsetPlaceholderColor
    val borderGray = OutlineLight
    val errorRed = ErrorRedLight

    com.lojia.shiftreport.ui.common.LojiaTextField(
        value = loginUser,
        onValueChange = onLoginUserChange,
        label = {
            Text(
                text = stringResource(R.string.auth_username_or_email),
                fontFamily = PoppinsFontFamily,
                fontSize = DepthTypeTokens.AuthFooterTextSize
            )
        },
        placeholder = {
            Text(
                text = stringResource(R.string.auth_username_or_email),
                fontFamily = PoppinsFontFamily,
                fontSize = DepthTypeTokens.InsetPlaceholderSize,
                color = hintGray
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconField)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(Dimens.RadiusMd),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        textStyle = TextStyle(
            color = charcoalText,
            fontFamily = PoppinsFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("etLoginUser")
    )

    Spacer(modifier = Modifier.height(12.dp))

    com.lojia.shiftreport.ui.common.LojiaTextField(
        value = loginPass,
        onValueChange = onLoginPassChange,
        label = {
            Text(
                text = stringResource(R.string.password),
                fontFamily = PoppinsFontFamily,
                fontSize = DepthTypeTokens.AuthFooterTextSize
            )
        },
        placeholder = {
            Text(
                text = stringResource(R.string.password),
                fontFamily = PoppinsFontFamily,
                fontSize = DepthTypeTokens.InsetPlaceholderSize,
                color = hintGray
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconField)
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onTogglePasswordVisible,
                modifier = Modifier.size(Dimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = if (loginPassVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(R.string.password),
                    modifier = Modifier.size(Dimens.IconField)
                )
            }
        },
        singleLine = true,
        visualTransformation = if (loginPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
        shape = RoundedCornerShape(Dimens.RadiusMd),
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
            fontFamily = PoppinsFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("etLoginPass")
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                .clickable { onRememberMeChange(!rememberMe) }
                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp)
                .testTag("cbRemember")
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.RememberCheckboxSize)
                    .clip(RoundedCornerShape(Dimens.RememberCheckboxRadius))
                    .background(if (rememberMe) primaryIndigo else Color.Transparent)
                    .border(
                        width = Dimens.RememberCheckboxBorderWidth,
                        color = if (rememberMe) primaryIndigo else borderGray,
                        shape = RoundedCornerShape(Dimens.RememberCheckboxRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (rememberMe) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.auth_remember_me),
                fontFamily = PoppinsFontFamily,
                fontSize = DepthTypeTokens.AuthRowLinkTextSize,
                fontWeight = FontWeight.Medium,
                color = mediumGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = stringResource(R.string.auth_forgot_password),
            fontFamily = PoppinsFontFamily,
            fontSize = DepthTypeTokens.AuthRowLinkTextSize,
            fontWeight = FontWeight.SemiBold,
            color = primaryIndigo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                .clip(RoundedCornerShape(Dimens.RadiusSm))
                .clickable { onForgotPassword() }
                .wrapContentHeight(Alignment.CenterVertically)
                .padding(horizontal = 4.dp)
                .testTag("tvForgot")
        )
    }

    Spacer(modifier = Modifier.height(Dimens.SpacingXs))

    if (!loginErrorMessage.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .depthSemanticCard(
                    backgroundColor = NegativeBannerSurface,
                    borderColor = NegativeBannerBorder,
                    shape = RoundedCornerShape(Dimens.RadiusSm),
                    cornerRadius = Dimens.RadiusSm
                )
        ) {
            Text(
                text = loginErrorMessage,
                color = errorRed,
                fontFamily = PoppinsFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }

    LojiaGradientButton(
        text = stringResource(R.string.auth_sign_in_btn),
        onClick = onSignIn,
        enabled = !isSigningIn,
        isLoading = isSigningIn,
        testTag = "btnLogin"
    )

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.auth_no_account) + " ",
            fontFamily = PoppinsFontFamily,
            fontSize = DepthTypeTokens.AuthFooterTextSize,
            color = mediumGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.auth_register_here),
            fontFamily = PoppinsFontFamily,
            fontSize = DepthTypeTokens.AuthFooterTextSize,
            fontWeight = FontWeight.Bold,
            color = primaryIndigo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                .clip(RoundedCornerShape(Dimens.RadiusSm))
                .clickable { onRegisterClick() }
                .wrapContentHeight(Alignment.CenterVertically)
                .padding(horizontal = 6.dp)
                .testTag("goRegister")
        )
    }
}

/**
 * Password Recovery Security Question Dialog
 */
@Composable
fun LojiaPasswordRecoveryDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    userProfile: UserProfile?,
    isBn: Boolean,
    onAuthenticated: () -> Unit
) {
    if (!showDialog) return

    val context = LocalContext.current
    val prefs = remember(context) { PreferencesRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var recoveryAnswer by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    var isAnswerCorrect by remember { mutableStateOf(false) }

    val question = userProfile?.securityQuestion?.trim().orEmpty()
        .ifBlank { prefs.getRecoveryQuestion().trim() }
    val storedAnswer = userProfile?.securityAnswer?.trim().orEmpty()
        .ifBlank { prefs.getRecoveryAnswerHash().trim() }
    val recoveryConfigured = SecurityRecoveryVerifier.isRecoveryConfigured(question, storedAnswer)

    com.lojia.shiftreport.ui.common.LojiaDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.DialogPaddingComfortable),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_password_recovery_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigoLight
            )

            if (!recoveryConfigured) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthInfoBlock(cornerRadius = Dimens.RadiusInputRegister)
                ) {
                    Text(
                        text = stringResource(R.string.auth_recovery_unavailable),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceLight,
                        modifier = Modifier.padding(Dimens.RecoveryInfoPadding)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.auth_answer_security_question),
                    fontSize = 12.sp,
                    color = OnSurfaceVariantLight
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .depthInfoBlock(cornerRadius = Dimens.RadiusInputRegister)
                ) {
                    Column(modifier = Modifier.padding(Dimens.RecoveryInfoPadding)) {
                        Text(
                            text = stringResource(R.string.auth_question_label),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigoLight
                        )
                        Text(
                            text = question,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceLight
                        )
                    }
                }

                if (!isAnswerCorrect) {
                    LojiaInputField(
                        value = recoveryAnswer,
                        onValueChange = {
                            recoveryAnswer = it
                            recoveryError = null
                        },
                        label = stringResource(R.string.auth_answer_label),
                        placeholder = stringResource(R.string.auth_enter_answer_placeholder),
                        errorMessage = recoveryError,
                        validationState = if (recoveryError != null) FieldValidationState.ERROR else FieldValidationState.DEFAULT,
                        testTag = "recoveryAnswer"
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .depthSemanticCard(
                                backgroundColor = SuccessContainer,
                                borderColor = AuthSuccessMint,
                                shape = RoundedCornerShape(Dimens.RadiusSm)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.auth_identity_verified),
                                fontSize = 12.sp,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.auth_recovery_success_msg),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceLight
                            )
                        }
                    }
                }
            }

            val confirmActionContent: @Composable () -> Unit = {
                if (recoveryConfigured) {
                    val incorrectAnswerError = stringResource(R.string.auth_incorrect_answer)
                    if (!isAnswerCorrect) {
                        Button(
                            onClick = {
                                val result = SecurityRecoveryVerifier.verifyAnswerWithUpgrade(
                                    enteredAnswer = recoveryAnswer,
                                    storedAnswerOrHash = storedAnswer
                                )
                                if (result.isMatch) {
                                    if (result.newHashToStore != null) {
                                        prefs.saveRecoveryAnswerHash(result.newHashToStore)
                                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            try {
                                                val db = com.lojia.shiftreport.data.AppDatabase.getInstance(context)
                                                val existing = db.reportDao().getUserProfileOnce()
                                                if (existing != null) {
                                                    db.reportDao().saveUserProfile(
                                                        existing.copy(securityAnswer = result.newHashToStore)
                                                    )
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    isAnswerCorrect = true
                                } else {
                                    recoveryError = incorrectAnswerError
                                }
                            },
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.ButtonHeightPrimary)
                                .depthRaisedButton(
                                    enabled = true,
                                    cornerRadius = Dimens.RadiusMd,
                                    minHeight = Dimens.ButtonHeightPrimary
                                )
                        ) {
                            Text(
                                stringResource(R.string.auth_verify_btn),
                                fontSize = DepthTypeTokens.PrimaryButtonTextSize,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                onDismiss()
                                onAuthenticated()
                            },
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.ButtonHeightPrimary)
                                .depthRaisedButton(
                                    enabled = true,
                                    cornerRadius = Dimens.RadiusMd,
                                    minHeight = Dimens.ButtonHeightPrimary
                                )
                        ) {
                            Text(
                                stringResource(R.string.auth_complete_login_btn),
                                fontSize = DepthTypeTokens.PrimaryButtonTextSize,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            val cancelActionContent: @Composable () -> Unit = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    border = null,
                    elevation = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryBlueLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.ButtonHeightPrimary)
                        .depthSecondaryButton(
                            cornerRadius = Dimens.RadiusMd,
                            minHeight = Dimens.ButtonHeightPrimary
                        )
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        fontSize = DepthTypeTokens.SecondaryButtonTextSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { cancelActionContent() }
                if (recoveryConfigured) {
                    Box(modifier = Modifier.weight(1f)) { confirmActionContent() }
                }
            }
        }
    }
}
