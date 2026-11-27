package com.lojia.shiftreport.auth

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.fragment.app.FragmentActivity
import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.AppDatabase
import com.lojia.shiftreport.data.AppLanguage
import com.lojia.shiftreport.data.BusinessProfile
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.data.UserProfile
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.util.SecurityUtils
import com.lojia.shiftreport.util.isPinUsable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * INTERNATIONAL POS & BANKING STANDARD: AuthScreen (Login Tab & Navigation)
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * CRITICAL SECURITY ARCHITECTURE (Foodics POS / Banking Standards):
 * 1. [rememberMe] starts `false` by default on clean state. POS terminals must
 *    never default to remembering user credentials across shared shifts.
 * 2. If a remembered username was previously stored in EncryptedSharedPreferences,
 *    it is pre-filled on startup and [rememberMe] is set to `true`.
 * 3. Immediate clearing: When the operator manually unchecks the checkbox,
 *    [SecureStorageManager.clearRememberedUsername] is invoked immediately without
 *    waiting for the next login attempt.
 * 4. Passwords are NEVER persisted to EncryptedSharedPreferences, plain prefs, or
 *    databases under the Remember Me flow. Only the operator username identifier
 *    is saved. Passwords exist only ephemerally in RAM during cryptographic auth.
 * 5. Instant screen-off and process backgrounding continue to lock the terminal
 *    via [AppSessionManager]. Remember Me NEVER bypasses session locking.
 */
@Composable
fun AuthScreen(
    activity: FragmentActivity? = null,
    userProfile: UserProfile? = null,
    businessProfile: BusinessProfile? = null,
    language: AppLanguage = AppLanguage.ENGLISH,
    onAuthenticated: () -> Unit,
    onSaveUserProfile: ((UserProfile) -> Unit)? = null,
    onSaveBusinessProfile: ((BusinessProfile) -> Unit)? = null
) {
    val context = LocalContext.current
    val hostActivity = activity ?: (context as? FragmentActivity)

    BiometricLockScreen(
        activity = hostActivity ?: return,
        userProfile = userProfile,
        businessProfile = businessProfile,
        language = language,
        onAuthenticated = onAuthenticated,
        onSaveUserProfile = onSaveUserProfile,
        onSaveBusinessProfile = onSaveBusinessProfile
    )
}

/**
 * Standalone Login Tab Composable complying strictly with POS standards.
 */
@Composable
fun AuthScreenLoginTab(
    userProfile: UserProfile?,
    language: AppLanguage = AppLanguage.ENGLISH,
    onAuthenticated: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepository = remember(context) { PreferencesRepository.getInstance(context) }
    val secureStorageManager = SecureStorageManager

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) } // MUST be false by default - International POS standard
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load saved username on start: Foodics / Banking standard
    LaunchedEffect(Unit) {
        val savedUsername = preferencesRepository.getRememberedUsername()
        if (!savedUsername.isNullOrBlank()) {
            username = savedUsername
            rememberMe = true // If username saved, show ticked
        } else {
            rememberMe = false
        }
    }

    val primaryIndigo = PrimaryBlueLight
    val charcoalText = OnSurfaceLight
    val mediumGray = OnSurfaceVariantLight
    val hintGray = DepthTokens.InsetPlaceholderColor
    val borderGray = OutlineLight
    val errorRed = ErrorRedLight

    fun handleSignIn() {
        focusManager.clearFocus()
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()

        if (trimmedUser.isEmpty() || trimmedPass.isEmpty()) {
            errorMessage = context.getString(R.string.auth_toast_please_enter_credentials)
            return
        }

        errorMessage = null
        isLoading = true

        coroutineScope.launch {
            delay(300)
            isLoading = false

            // POS Security Standard: Save or clear ONLY the username. NEVER save password.
            if (rememberMe) {
                secureStorageManager.saveRememberedUsername(trimmedUser)
            } else {
                secureStorageManager.clearRememberedUsername()
            }

            val profile = userProfile
            var isProfileValid = false

            if (profile != null && profile.username.isNotBlank() &&
                isPinUsable(trimmedPass) && isPinUsable(profile.passwordHash)
            ) {
                val usernameMatch = (trimmedUser.equals(profile.username, ignoreCase = true) ||
                        trimmedUser.equals(profile.email, ignoreCase = true))
                val verifyResult = SecurityUtils.verifySecretWithUpgrade(trimmedPass, profile.passwordHash)
                if (usernameMatch && verifyResult.isMatch) {
                    isProfileValid = true
                    if (verifyResult.newHashToStore != null) {
                        try {
                            val db = AppDatabase.getInstance(context)
                            db.reportDao().saveUserProfile(profile.copy(passwordHash = verifyResult.newHashToStore))
                        } catch (_: Exception) {}
                    }
                }
            }

            if (isProfileValid) {
                // Clear transient password from memory immediately
                password = ""
                preferencesRepository.saveUserSession(
                    username = trimmedUser,
                    fullName = userProfile?.fullName.orEmpty(),
                    email = userProfile?.email.orEmpty(),
                    rememberMe = rememberMe
                )
                Toast.makeText(context, context.getString(R.string.auth_toast_login_success), Toast.LENGTH_SHORT).show()
                onAuthenticated()
            } else {
                errorMessage = context.getString(R.string.auth_toast_invalid_credentials)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingXs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Operator Username Field
        val usernameLabel = stringResource(R.string.auth_username_or_email)
        com.lojia.shiftreport.ui.common.LojiaTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = null
            },
            label = {
                Text(
                    text = usernameLabel,
                    fontFamily = FontFamily.Default,
                    fontSize = DepthTypeTokens.AuthFieldLabelSize
                )
            },
            contentDescription = usernameLabel,
            placeholder = {
                Text(
                    text = stringResource(R.string.auth_username_or_email),
                    fontFamily = FontFamily.Default,
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
                fontFamily = FontFamily.Default,
                fontSize = DepthTypeTokens.AuthFieldInputSize,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("etLoginUser")
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        // Password Field with Eye Toggle
        val passwordLabel = stringResource(R.string.auth_password)
        com.lojia.shiftreport.ui.common.LojiaTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = {
                Text(
                    text = passwordLabel,
                    fontFamily = FontFamily.Default,
                    fontSize = DepthTypeTokens.AuthFieldLabelSize
                )
            },
            contentDescription = passwordLabel,
            placeholder = {
                Text(
                    text = stringResource(R.string.auth_password),
                    fontFamily = FontFamily.Default,
                    fontSize = DepthTypeTokens.InsetPlaceholderSize,
                    color = hintGray
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconField)
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { isPasswordVisible = !isPasswordVisible },
                    modifier = Modifier.size(Dimens.MinTouchTarget)
                ) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = stringResource(if (isPasswordVisible) R.string.cd_hide_password else R.string.cd_show_password),
                        tint = mediumGray,
                        modifier = Modifier.size(Dimens.IconAction)
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(Dimens.RadiusMd),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { handleSignIn() }
            ),
            textStyle = TextStyle(
                color = charcoalText,
                fontFamily = FontFamily.Default,
                fontSize = DepthTypeTokens.AuthFieldInputSize,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("etLoginPass")
        )

        Spacer(modifier = Modifier.height(Dimens.AuthFieldGap))

        // Remember Me & Forgot Password Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Remember Me Checkbox with immediate clear on uncheck
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val newState = !rememberMe
                        rememberMe = newState
                        if (!newState) {
                            coroutineScope.launch {
                                secureStorageManager.clearRememberedUsername()
                                preferencesRepository.clearRememberedUsername()
                            }
                        }
                    }
                    .padding(end = Dimens.SpacingSm, top = Dimens.AuthCompactGap, bottom = Dimens.AuthCompactGap)
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
                            contentDescription = stringResource(R.string.auth_remember_me_selected_cd),
                            tint = PureWhite,
                            modifier = Modifier.size(Dimens.AuthCheckIconSize)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text(
                    text = stringResource(R.string.auth_remember_me),
                    fontFamily = FontFamily.Default,
                    fontSize = DepthTypeTokens.AuthRowLinkTextSize,
                    fontWeight = FontWeight.Medium,
                    color = mediumGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier.defaultMinSize(minHeight = Dimens.MinTouchTarget),
                contentPadding = PaddingValues(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs)
            ) {
                Text(
                    text = stringResource(R.string.auth_forgot_password),
                    fontFamily = FontFamily.Default,
                    fontSize = DepthTypeTokens.AuthRowLinkTextSize,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryIndigo
                )
            }
        }

        errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .depthSemanticCard(
                        backgroundColor = NegativeBannerSurface,
                        borderColor = NegativeBannerBorder,
                        shape = RoundedCornerShape(Dimens.RadiusSm),
                        cornerRadius = Dimens.RadiusSm
                    )
            ) {
                Text(
                    text = msg,
                    fontFamily = FontFamily.Default,
                    fontSize = DepthTypeTokens.AuthErrorBannerTextSize,
                    color = errorRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingXs))

        // Raised Sign In Button
        Button(
            onClick = { handleSignIn() },
            enabled = !isLoading,
            shape = RoundedCornerShape(Dimens.RadiusMd),
            elevation = null,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                contentColor = PureWhite,
                disabledContentColor = OnSurfaceVariantLight
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.ButtonHeightPrimary)
                .depthRaisedButton(
                    enabled = !isLoading,
                    cornerRadius = Dimens.RadiusMd,
                    minHeight = Dimens.ButtonHeightPrimary
                )
                .testTag("btnSignIn")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.IconAction),
                    color = PureWhite,
                    strokeWidth = Dimens.ProgressStrokeWidth
                )
            } else {
                Text(
                    text = stringResource(R.string.auth_sign_in_btn),
                    fontFamily = FontFamily.Default,
                    fontSize = DepthTypeTokens.PrimaryButtonTextSize,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.AuthSectionGap))

        // Register link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.auth_no_account) + " ",
                fontFamily = FontFamily.Default,
                fontSize = DepthTypeTokens.AuthFooterTextSize,
                color = mediumGray
            )
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .clickable { onRegisterClick() }
                    .padding(horizontal = Dimens.AuthCompactGap),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.auth_register_here),
                    fontFamily = FontFamily.Default,
                    fontSize = DepthTypeTokens.AuthFooterTextSize,
                    fontWeight = FontWeight.Bold,
                    color = primaryIndigo
                )
            }
        }
    }
}
