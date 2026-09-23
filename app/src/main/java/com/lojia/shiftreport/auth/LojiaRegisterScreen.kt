package com.lojia.shiftreport.auth

import com.lojia.shiftreport.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.data.PreferencesRepository
import com.lojia.shiftreport.data.UserProfile
import com.lojia.shiftreport.util.SecurityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LojiaRegisterScreen(
    userProfile: UserProfile?,
    isBn: Boolean,
    onRegisterSuccess: (UserProfile) -> Unit,
    onGoToLogin: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepository = remember(context) { PreferencesRepository.getInstance(context) }

    // Registration input states
    var rFn by remember { mutableStateOf("") }
    var rLn by remember { mutableStateOf("") }
    var rUn by remember { mutableStateOf("") }
    var rEm by remember { mutableStateOf("") }
    var phoneNum by remember { mutableStateOf("") }

    // Country Detection & Selection
    val defaultCountry = remember(context) {
        com.lojia.shiftreport.util.CountryDetector.detectDefaultLojiaCountry(context)
    }
    var selectedCountry by remember { mutableStateOf(defaultCountry) }
    var showCountryPicker by remember { mutableStateOf(false) }

    var rPw by remember { mutableStateOf("") }
    var rPwVisible by remember { mutableStateOf(false) }
    var rCp by remember { mutableStateOf("") }
    var rCpVisible by remember { mutableStateOf(false) }
    var rSq by remember { mutableStateOf("") }
    var rSa by remember { mutableStateOf("") }
    var agreeTerms by remember { mutableStateOf(false) }
    var isProcessingReg by remember { mutableStateOf(false) }

    // Validation states
    var vFn by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vLn by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vUn by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vEm by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vPhone by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vPw by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vCp by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vSq by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vSa by remember { mutableStateOf(FieldValidationState.DEFAULT) }

    LaunchedEffect(rFn, rLn, rUn, rEm, phoneNum, rPw, rCp, rSq, rSa) {
        vFn = if (rFn.isEmpty()) FieldValidationState.DEFAULT else if (rFn.trim().isNotEmpty()) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        vLn = if (rLn.isEmpty()) FieldValidationState.DEFAULT else if (rLn.trim().isNotEmpty()) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        vUn = if (rUn.isEmpty()) FieldValidationState.DEFAULT else {
            val u = rUn.trim()
            if (u.length in 3..20 && u.all { it.isLetterOrDigit() || it == '_' }) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
        vEm = if (rEm.isEmpty()) FieldValidationState.DEFAULT else {
            val emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex()
            if (emailRegex.matches(rEm.trim())) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
        vPhone = if (phoneNum.isEmpty()) FieldValidationState.DEFAULT else {
            val digits = phoneNum.filter { it.isDigit() }
            if (digits.length in 7..15) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
        vPw = if (rPw.isEmpty()) FieldValidationState.DEFAULT else {
            if (rPw.length >= 8) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
        vCp = if (rCp.isEmpty()) FieldValidationState.DEFAULT else {
            if (rCp == rPw && rPw.isNotEmpty()) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
        vSq = if (rSq.isEmpty()) FieldValidationState.DEFAULT else FieldValidationState.SUCCESS
        vSa = if (rSa.isEmpty()) FieldValidationState.DEFAULT else {
            if (rSa.trim().isNotEmpty()) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
    }

    val isFnValid = rFn.trim().isNotEmpty()
    val isLnValid = rLn.trim().isNotEmpty()
    val isUnValid = rUn.trim().length in 3..20 && rUn.trim().all { it.isLetterOrDigit() || it == '_' }
    val isEmValid = rEm.trim().isNotEmpty() && "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex().matches(rEm.trim())
    val isPhoneValid = phoneNum.filter { it.isDigit() }.length in 7..15
    val isPwValid = rPw.length >= 8
    val isCpValid = rCp.isNotEmpty() && rCp == rPw
    val isSqValid = rSq.isNotBlank()
    val isSaValid = rSa.trim().isNotEmpty()
    val isTermsValid = agreeTerms

    fun handleRegister() {
        focusManager.clearFocus()

        var hasError = false
        if (rFn.trim().isEmpty()) { vFn = FieldValidationState.ERROR; hasError = true }
        if (rLn.trim().isEmpty()) { vLn = FieldValidationState.ERROR; hasError = true }

        val u = rUn.trim()
        if (u.length !in 3..20 || !u.all { it.isLetterOrDigit() || it == '_' }) {
            vUn = FieldValidationState.ERROR
            hasError = true
        }

        val emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex()
        if (!emailRegex.matches(rEm.trim())) {
            vEm = FieldValidationState.ERROR
            hasError = true
        }

        val digits = phoneNum.filter { it.isDigit() }
        if (phoneNum.isNotBlank() && digits.length !in 7..15) {
            vPhone = FieldValidationState.ERROR
            hasError = true
        }

        if (rPw.length < 8) {
            vPw = FieldValidationState.ERROR
            hasError = true
        }

        if (rCp != rPw || rCp.isEmpty()) {
            vCp = FieldValidationState.ERROR
            hasError = true
        }

        if (rSq.isBlank()) {
            vSq = FieldValidationState.ERROR
            hasError = true
        }

        if (rSa.trim().isEmpty()) {
            vSa = FieldValidationState.ERROR
            hasError = true
        }

        if (!agreeTerms) {
            hasError = true
        }

        if (hasError) {
            val toastMsg = if (!agreeTerms) {
                context.getString(R.string.auth_toast_agree_terms)
            } else {
                context.getString(R.string.auth_toast_fill_required)
            }
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
            return
        }

        isProcessingReg = true
        coroutineScope.launch {
            delay(1500)
            isProcessingReg = false

            val fullPhone = "${selectedCountry.dial} $phoneNum".trim()
            val newProfile = (userProfile ?: UserProfile()).copy(
                fullName = "${rFn.trim()} ${rLn.trim()}".trim(),
                username = rUn.trim(),
                email = rEm.trim(),
                passwordHash = SecurityUtils.hashSecret(rPw),
                securityQuestion = rSq,
                securityAnswer = rSa.trim(),
                phone = fullPhone,
                isRegistered = true
            )
            preferencesRepository.saveUserSession(
                username = newProfile.username,
                fullName = newProfile.fullName,
                email = newProfile.email,
                rememberMe = true
            )
            preferencesRepository.syncWithUserProfile(newProfile)
            onRegisterSuccess(newProfile)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LojiaHeader(isBn = isBn)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-14).dp)
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card 1: Personal Profile
            LojiaCard(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                LojiaSectionHeader(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.auth_profile_title),
                    subtitle = stringResource(R.string.auth_profile_sub)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LojiaInputField(
                        value = rFn,
                        onValueChange = { rFn = it },
                        label = stringResource(R.string.auth_first_name),
                        placeholder = stringResource(R.string.auth_ph_first_name),
                        leadingIcon = Icons.Outlined.Person,
                        isRequired = true,
                        isValid = rFn.trim().isNotEmpty(),
                        validationState = vFn,
                        errorMessage = stringResource(R.string.auth_err_required),
                        successMessage = stringResource(R.string.auth_ok_good),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                        modifier = Modifier.weight(1f),
                        testTag = "rFn"
                    )

                    LojiaInputField(
                        value = rLn,
                        onValueChange = { rLn = it },
                        label = stringResource(R.string.auth_last_name),
                        placeholder = stringResource(R.string.auth_ph_last_name),
                        leadingIcon = Icons.Outlined.Person,
                        isRequired = true,
                        isValid = rLn.trim().isNotEmpty(),
                        validationState = vLn,
                        errorMessage = stringResource(R.string.auth_err_required),
                        successMessage = stringResource(R.string.auth_ok_good),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.weight(1f),
                        testTag = "rLn"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LojiaInputField(
                    value = rUn,
                    onValueChange = {
                        rUn = it.filter { ch -> !ch.isWhitespace() }.lowercase()
                    },
                    label = stringResource(R.string.username),
                    placeholder = stringResource(R.string.auth_ph_username),
                    leadingIcon = Icons.Outlined.Person,
                    isRequired = true,
                    isValid = rUn.trim().length in 3..20,
                    validationState = vUn,
                    errorMessage = stringResource(R.string.auth_err_user_length),
                    successMessage = stringResource(R.string.auth_ok_user_avail),
                    infoTooltip = stringResource(R.string.auth_user_tip),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    testTag = "rUn"
                )

                Spacer(modifier = Modifier.height(12.dp))

                LojiaInputField(
                    value = rEm,
                    onValueChange = { rEm = it },
                    label = stringResource(R.string.auth_work_email),
                    placeholder = stringResource(R.string.auth_ph_email),
                    leadingIcon = Icons.Outlined.Email,
                    isRequired = true,
                    isValid = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex().matches(rEm.trim()),
                    validationState = vEm,
                    errorMessage = stringResource(R.string.auth_err_valid_email),
                    successMessage = stringResource(R.string.auth_ok_valid_email),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    testTag = "rEm"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.phone_number),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LojiaColors.N600,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LojiaColors.N50,
                            border = BorderStroke(1.5.dp, LojiaColors.N300),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .widthIn(min = 78.dp)
                                .clickable { showCountryPicker = true }
                                .testTag("ccBtn")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCountry.dial,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LojiaColors.N700
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = LojiaColors.N400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            LojiaInputField(
                                value = phoneNum,
                                onValueChange = { phoneNum = it },
                                label = "",
                                placeholder = stringResource(R.string.auth_ph_phone),
                                leadingIcon = Icons.Outlined.Phone,
                                validationState = vPhone,
                                errorMessage = stringResource(R.string.auth_err_valid_phone),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                testTag = "phoneNum"
                            )
                        }
                    }
                }
            }

            // Card 2: Security Details
            LojiaCard(modifier = Modifier.padding(top = 12.dp)) {
                LojiaSectionHeader(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.auth_sec_title),
                    subtitle = stringResource(R.string.auth_sec_sub)
                )

                LojiaInputField(
                    value = rPw,
                    onValueChange = { rPw = it },
                    label = stringResource(R.string.password),
                    placeholder = stringResource(R.string.auth_ph_pw_min),
                    leadingIcon = Icons.Outlined.Lock,
                    isRequired = true,
                    isValid = rPw.length >= 8,
                    validationState = vPw,
                    errorMessage = stringResource(R.string.auth_err_pw_min),
                    visualTransformation = if (rPwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { rPwVisible = !rPwVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (rPwVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password",
                                tint = if (rPwVisible) LojiaColors.P500 else LojiaColors.N400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    testTag = "rPw"
                )

                LojiaPasswordStrengthMeter(password = rPw, isBn = isBn)

                Spacer(modifier = Modifier.height(12.dp))

                LojiaInputField(
                    value = rCp,
                    onValueChange = { rCp = it },
                    label = stringResource(R.string.auth_confirm_pw),
                    placeholder = stringResource(R.string.auth_ph_reenter_pw),
                    leadingIcon = Icons.Outlined.Shield,
                    isRequired = true,
                    isValid = rCp == rPw && rPw.isNotEmpty(),
                    validationState = vCp,
                    errorMessage = stringResource(R.string.auth_err_pw_match),
                    successMessage = stringResource(R.string.auth_ok_pw_match),
                    visualTransformation = if (rCpVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { rCpVisible = !rCpVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (rCpVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password",
                                tint = if (rCpVisible) LojiaColors.P500 else LojiaColors.N400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    testTag = "rCp"
                )
            }

            // Card 3: Recovery & Compliance
            LojiaCard(modifier = Modifier.padding(top = 12.dp)) {
                LojiaSectionHeader(
                    icon = Icons.Outlined.HelpOutline,
                    title = stringResource(R.string.auth_rec_title),
                    subtitle = stringResource(R.string.auth_rec_sub)
                )

                var showQuestionMenu by remember { mutableStateOf(false) }
                val questionResIds = listOf(
                    R.string.auth_sq1,
                    R.string.auth_sq2,
                    R.string.auth_sq3,
                    R.string.auth_sq4,
                    R.string.auth_sq5
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auth_sec_question),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LojiaColors.N600
                        )
                        Text(
                            text = " *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rSq.isNotEmpty()) LojiaColors.G500 else LojiaColors.R500
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LojiaColors.N50)
                            .border(
                                1.5.dp,
                                if (vSq == FieldValidationState.ERROR) LojiaColors.R500 else LojiaColors.N300,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { showQuestionMenu = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = null,
                                tint = LojiaColors.N400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (rSq.isBlank()) stringResource(R.string.auth_choose_question) else rSq,
                                fontSize = 13.sp,
                                color = if (rSq.isBlank()) LojiaColors.N400 else LojiaColors.N900,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = LojiaColors.N400,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showQuestionMenu,
                            onDismissRequest = { showQuestionMenu = false },
                            modifier = Modifier.background(LojiaColors.White)
                        ) {
                            questionResIds.forEach { resId ->
                                val qText = stringResource(resId)
                                DropdownMenuItem(
                                    text = { Text(text = qText, fontSize = 13.sp, color = LojiaColors.N900) },
                                    onClick = {
                                        rSq = qText
                                        showQuestionMenu = false
                                        vSq = FieldValidationState.SUCCESS
                                    }
                                )
                            }
                        }
                    }

                    if (vSq == FieldValidationState.ERROR) {
                        Text(
                            text = stringResource(R.string.auth_err_sel_question),
                            color = LojiaColors.R500,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 3.dp, start = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LojiaInputField(
                    value = rSa,
                    onValueChange = { rSa = it },
                    label = stringResource(R.string.auth_sec_answer),
                    placeholder = stringResource(R.string.auth_ph_answer),
                    leadingIcon = Icons.Outlined.CheckCircle,
                    isRequired = true,
                    isValid = rSa.trim().isNotEmpty(),
                    validationState = vSa,
                    hintMessage = stringResource(R.string.auth_ans_hint),
                    errorMessage = stringResource(R.string.auth_err_required),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    testTag = "rSa"
                )

                val termsTextFull = stringResource(R.string.auth_terms_text)

                val termsAnnotated = remember(termsTextFull) {
                    buildAnnotatedString {
                        append(termsTextFull)
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 13.dp)
                        .clickable { agreeTerms = !agreeTerms }
                        .testTag("cbTerms"),
                    shape = RoundedCornerShape(10.dp),
                    color = LojiaColors.N50,
                    border = BorderStroke(1.dp, LojiaColors.N200)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Checkbox(
                            checked = agreeTerms,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = LojiaColors.P500,
                                uncheckedColor = LojiaColors.N300
                            ),
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = termsAnnotated,
                            fontSize = 11.5.sp,
                            color = LojiaColors.N500,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LojiaGradientButton(
                text = if (isProcessingReg) stringResource(R.string.auth_processing) else stringResource(R.string.auth_reg_btn),
                onClick = { handleRegister() },
                enabled = !isProcessingReg,
                isLoading = isProcessingReg,
                testTag = "regBtn"
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = LojiaColors.N400,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.auth_ssl_text),
                    fontSize = 10.sp,
                    color = LojiaColors.N400
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auth_already_account) + " ",
                    fontSize = 12.sp,
                    color = LojiaColors.N500
                )
                Text(
                    text = stringResource(R.string.auth_sign_in_link),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LojiaColors.P500,
                    modifier = Modifier
                        .clickable { onGoToLogin() }
                        .padding(4.dp)
                        .testTag("goLogin")
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    if (showCountryPicker) {
        LojiaCountryPickerDialog(
            countries = LOJIA_COUNTRIES,
            selectedDial = selectedCountry.dial,
            onSelectCountry = {
                selectedCountry = it
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
            isBn = isBn
        )
    }
}

@Composable
fun LojiaRegisterSuccessScreen(
    isBn: Boolean,
    onGoToLogin: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pgSuccess")
    ) {
        LojiaHeader(isBn = isBn)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .scale(scale.value)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(LojiaColors.G100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = LojiaColors.G500,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.auth_success_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LojiaColors.N900,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.auth_success_sub),
                    fontSize = 13.sp,
                    color = LojiaColors.N500,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 280.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                LojiaGradientButton(
                    text = stringResource(R.string.auth_go_to_login),
                    onClick = onGoToLogin,
                    modifier = Modifier.widthIn(max = 280.dp),
                    testTag = "goLoginFromSuccess"
                )
            }
        }
    }
}
