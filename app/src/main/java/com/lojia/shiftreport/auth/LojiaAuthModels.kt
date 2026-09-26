package com.lojia.shiftreport.auth

import com.lojia.shiftreport.R
import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.*
import com.lojia.shiftreport.ui.common.*
import com.lojia.shiftreport.ui.theme.*
import com.lojia.shiftreport.auth.*
import com.lojia.shiftreport.report.*
import com.lojia.shiftreport.settings.*

import androidx.compose.ui.graphics.Color

object LojiaColors {
    val P800 = Color(0xFF001B3E)
    val P700 = Color(0xFF284777)
    val P600 = Color(0xFF415F91)
    val P500 = Color(0xFF415F91) // Deep Indigo #415F91
    val P400 = Color(0xFF5D7BAE)
    val P100 = Color(0xFFD6E3FF)
    val P50 = Color(0xFFEEF3FF)

    val G500 = Color(0xFF006C5B) // Subtle Teal/Cyan #006C5B
    val G200 = Color(0xFF72F8DC)
    val G100 = Color(0xFFD4F9EE)

    val R500 = Color(0xFFBA1A1A) // Standard MD3 Error #BA1A1A
    val R100 = Color(0xFFFFDAD6)
    val R200 = Color(0xFFF9DEDC)

    val A400 = Color(0xFF8C5000)

    val N900 = Color(0xFF191C20) // High-contrast Charcoal #191C20
    val N700 = Color(0xFF2E3135)
    val N600 = Color(0xFF44474E) // Medium Gray #44474E
    val N500 = Color(0xFF74777F) // Outline/Hint Gray
    val N400 = Color(0xFF8E9099)
    val N300 = Color(0xFFCAC4D0)
    val N200 = Color(0xFFE2E2E9)
    val N100 = Color(0xFFF0F0F7)
    val N50 = Color(0xFFF9F9FF)  // Cool Off-White/Light Gray Background #F9F9FF
    val White = Color(0xFFFFFFFF) // Pure White Surface #FFFFFF

    val CanvasBg = Color(0xFFF9F9FF)
    val OkBg = Color(0xFFD4F9EE)
}

data class LojiaCountry(
    val name: String,
    val code: String,
    val dial: String,
    val flag: String
)

val LOJIA_COUNTRIES = listOf(
    LojiaCountry("Afghanistan", "AF", "+93", "🇦🇫"),
    LojiaCountry("Albania", "AL", "+355", "🇦🇱"),
    LojiaCountry("Algeria", "DZ", "+213", "🇩🇿"),
    LojiaCountry("Andorra", "AD", "+376", "🇦🇩"),
    LojiaCountry("Angola", "AO", "+244", "🇦🇴"),
    LojiaCountry("Argentina", "AR", "+54", "🇦🇷"),
    LojiaCountry("Armenia", "AM", "+374", "🇦🇲"),
    LojiaCountry("Australia", "AU", "+61", "🇦🇺"),
    LojiaCountry("Austria", "AT", "+43", "🇦🇹"),
    LojiaCountry("Azerbaijan", "AZ", "+994", "🇦🇿"),
    LojiaCountry("Bahrain", "BH", "+973", "🇧🇭"),
    LojiaCountry("Bangladesh", "BD", "+880", "🇧🇩"),
    LojiaCountry("Belarus", "BY", "+375", "🇧🇾"),
    LojiaCountry("Belgium", "BE", "+32", "🇧🇪"),
    LojiaCountry("Bolivia", "BO", "+591", "🇧🇴"),
    LojiaCountry("Bosnia", "BA", "+387", "🇧🇦"),
    LojiaCountry("Brazil", "BR", "+55", "🇧🇷"),
    LojiaCountry("Bulgaria", "BG", "+359", "🇧🇬"),
    LojiaCountry("Cambodia", "KH", "+855", "🇰🇭"),
    LojiaCountry("Cameroon", "CM", "+237", "🇨🇲"),
    LojiaCountry("Canada", "CA", "+1", "🇨🇦"),
    LojiaCountry("Chile", "CL", "+56", "🇨🇱"),
    LojiaCountry("China", "CN", "+86", "🇨🇳"),
    LojiaCountry("Colombia", "CO", "+57", "🇨🇴"),
    LojiaCountry("Croatia", "HR", "+385", "🇭🇷"),
    LojiaCountry("Cuba", "CU", "+53", "🇨🇺"),
    LojiaCountry("Cyprus", "CY", "+357", "🇨🇾"),
    LojiaCountry("Czech Republic", "CZ", "+420", "🇨🇿"),
    LojiaCountry("Denmark", "DK", "+45", "🇩🇰"),
    LojiaCountry("Ecuador", "EC", "+593", "🇪🇨"),
    LojiaCountry("Egypt", "EG", "+20", "🇪🇬"),
    LojiaCountry("Estonia", "EE", "+372", "🇪🇪"),
    LojiaCountry("Ethiopia", "ET", "+251", "🇪🇹"),
    LojiaCountry("Finland", "FI", "+358", "🇫🇮"),
    LojiaCountry("France", "FR", "+33", "🇫🇷"),
    LojiaCountry("Georgia", "GE", "+995", "🇬🇪"),
    LojiaCountry("Germany", "DE", "+49", "🇩🇪"),
    LojiaCountry("Ghana", "GH", "+233", "🇬🇭"),
    LojiaCountry("Greece", "GR", "+30", "🇬🇷"),
    LojiaCountry("Guatemala", "GT", "+502", "🇬🇹"),
    LojiaCountry("Honduras", "HN", "+504", "🇭🇳"),
    LojiaCountry("Hong Kong", "HK", "+852", "🇭🇰"),
    LojiaCountry("Hungary", "HU", "+36", "🇭🇺"),
    LojiaCountry("Iceland", "IS", "+354", "🇮🇸"),
    LojiaCountry("India", "IN", "+91", "🇮🇳"),
    LojiaCountry("Indonesia", "ID", "+62", "🇮🇩"),
    LojiaCountry("Iran", "IR", "+98", "🇮🇷"),
    LojiaCountry("Iraq", "IQ", "+964", "🇮🇶"),
    LojiaCountry("Ireland", "IE", "+353", "🇮🇪"),
    LojiaCountry("Israel", "IL", "+972", "🇮🇱"),
    LojiaCountry("Italy", "IT", "+39", "🇮🇹"),
    LojiaCountry("Japan", "JP", "+81", "🇯🇵"),
    LojiaCountry("Jordan", "JO", "+962", "🇯🇴"),
    LojiaCountry("Kazakhstan", "KZ", "+7", "🇰🇿"),
    LojiaCountry("Kenya", "KE", "+254", "🇰🇪"),
    LojiaCountry("Kuwait", "KW", "+965", "🇰🇼"),
    LojiaCountry("Kyrgyzstan", "KG", "+996", "🇰🇬"),
    LojiaCountry("Latvia", "LV", "+371", "🇱🇻"),
    LojiaCountry("Lebanon", "LB", "+961", "🇱🇧"),
    LojiaCountry("Libya", "LY", "+218", "🇱🇾"),
    LojiaCountry("Lithuania", "LT", "+370", "🇱🇹"),
    LojiaCountry("Luxembourg", "LU", "+352", "🇱🇺"),
    LojiaCountry("Malaysia", "MY", "+60", "🇲🇾"),
    LojiaCountry("Maldives", "MV", "+960", "🇲🇻"),
    LojiaCountry("Malta", "MT", "+356", "🇲🇹"),
    LojiaCountry("Mexico", "MX", "+52", "🇲🇽"),
    LojiaCountry("Moldova", "MD", "+373", "🇲🇩"),
    LojiaCountry("Mongolia", "MN", "+976", "🇲🇳"),
    LojiaCountry("Morocco", "MA", "+212", "🇲🇦"),
    LojiaCountry("Mozambique", "MZ", "+258", "🇲🇿"),
    LojiaCountry("Myanmar", "MM", "+95", "🇲🇲"),
    LojiaCountry("Nepal", "NP", "+977", "🇳🇵"),
    LojiaCountry("Netherlands", "NL", "+31", "🇳🇱"),
    LojiaCountry("New Zealand", "NZ", "+64", "🇳🇿"),
    LojiaCountry("Nicaragua", "NI", "+505", "🇳🇮"),
    LojiaCountry("Nigeria", "NG", "+234", "🇳🇬"),
    LojiaCountry("Norway", "NO", "+47", "🇳🇴"),
    LojiaCountry("Oman", "OM", "+968", "🇴🇲"),
    LojiaCountry("Pakistan", "PK", "+92", "🇵🇰"),
    LojiaCountry("Palestine", "PS", "+970", "🇵🇸"),
    LojiaCountry("Panama", "PA", "+507", "🇵🇦"),
    LojiaCountry("Paraguay", "PY", "+595", "🇵🇾"),
    LojiaCountry("Peru", "PE", "+51", "🇵🇪"),
    LojiaCountry("Philippines", "PH", "+63", "🇵🇭"),
    LojiaCountry("Poland", "PL", "+48", "🇵🇱"),
    LojiaCountry("Portugal", "PT", "+351", "🇵🇹"),
    LojiaCountry("Qatar", "QA", "+974", "🇶🇦"),
    LojiaCountry("Romania", "RO", "+40", "🇷🇴"),
    LojiaCountry("Russia", "RU", "+7", "🇷🇺"),
    LojiaCountry("Saudi Arabia", "SA", "+966", "🇸🇦"),
    LojiaCountry("Senegal", "SN", "+221", "🇸🇳"),
    LojiaCountry("Serbia", "RS", "+381", "🇷🇸"),
    LojiaCountry("Singapore", "SG", "+65", "🇸🇬"),
    LojiaCountry("Slovakia", "SK", "+421", "🇸🇰"),
    LojiaCountry("Slovenia", "SI", "+386", "🇸🇮"),
    LojiaCountry("Somalia", "SO", "+252", "🇸🇴"),
    LojiaCountry("South Africa", "ZA", "+27", "🇿🇦"),
    LojiaCountry("South Korea", "KR", "+82", "🇰🇷"),
    LojiaCountry("Spain", "ES", "+34", "🇪🇸"),
    LojiaCountry("Sri Lanka", "LK", "+94", "🇱🇰"),
    LojiaCountry("Sudan", "SD", "+249", "🇸🇩"),
    LojiaCountry("Sweden", "SE", "+46", "🇸🇪"),
    LojiaCountry("Switzerland", "CH", "+41", "🇨🇭"),
    LojiaCountry("Syria", "SY", "+963", "🇸🇾"),
    LojiaCountry("Taiwan", "TW", "+886", "🇹🇼"),
    LojiaCountry("Tajikistan", "TJ", "+992", "🇹🇯"),
    LojiaCountry("Tanzania", "TZ", "+255", "🇹🇿"),
    LojiaCountry("Thailand", "TH", "+66", "🇹🇭"),
    LojiaCountry("Tunisia", "TN", "+216", "🇹🇳"),
    LojiaCountry("Turkey", "TR", "+90", "🇹🇷"),
    LojiaCountry("Turkmenistan", "TM", "+993", "🇹🇲"),
    LojiaCountry("Uganda", "UG", "+256", "🇺🇬"),
    LojiaCountry("Ukraine", "UA", "+380", "🇺🇦"),
    LojiaCountry("United Arab Emirates", "AE", "+971", "🇦🇪"),
    LojiaCountry("United Kingdom", "GB", "+44", "🇬🇧"),
    LojiaCountry("United States", "US", "+1", "🇺🇸"),
    LojiaCountry("Uruguay", "UY", "+598", "🇺🇾"),
    LojiaCountry("Uzbekistan", "UZ", "+998", "🇺🇿"),
    LojiaCountry("Venezuela", "VE", "+58", "🇻🇪"),
    LojiaCountry("Vietnam", "VN", "+84", "🇻🇳"),
    LojiaCountry("Yemen", "YE", "+967", "🇾🇪"),
    LojiaCountry("Zambia", "ZM", "+260", "🇿🇲"),
    LojiaCountry("Zimbabwe", "ZW", "+263", "🇿🇼")
)

object LojiaStrings {
    fun get(key: String, isBn: Boolean = false): String {
        return getEn(key)
    }

    fun getEn(key: String): String {
        return when (key) {
            "tagline" -> "SECURE BUSINESS LEDGER"
            "loginTitle" -> "Account Login"
            "loginSub" -> "Enter your credentials to continue"
            "usernameOrEmail" -> "Username or Email"
            "phUserOrEmail" -> "Enter username or email"
            "password" -> "Password"
            "phPassword" -> "Enter password"
            "rememberMe" -> "Remember me"
            "forgotPw" -> "Forgot password?"
            "signInBtn" -> "Sign In"
            "signingIn" -> "Signing in…"
            "sslText" -> "Local secure storage · On-device database"
            "noAccount" -> "Don't have an account?"
            "registerHere" -> "Register here"
            "profileTitle" -> "Personal Profile"
            "profileSub" -> "Your identity within the business"
            "firstName" -> "First name"
            "phFirstName" -> "First name"
            "lastName" -> "Last name"
            "phLastName" -> "Last name"
            "username" -> "Username"
            "userTip" -> "3–20 chars · letters, numbers, _ only"
            "phUsername" -> "Enter username"
            "workEmail" -> "Work email"
            "phEmail" -> "Work email"
            "phoneNumber" -> "Phone number"
            "phSearch" -> "Search…"
            "phPhone" -> "Phone number"
            "secTitle" -> "Security Details"
            "secSub" -> "Keep your account protected"
            "phPwMin" -> "Min. 8 characters"
            "pwStrengthLabel" -> "Password strength"
            "confirmPw" -> "Confirm password"
            "phReEnterPw" -> "Re-enter password"
            "recTitle" -> "Recovery & Compliance"
            "recSub" -> "Helps you regain access if needed"
            "secQuestion" -> "Security question"
            "chooseQuestion" -> "Choose a question…"
            "sq1" -> "What was your first pet's name?"
            "sq2" -> "What city were you born in?"
            "sq3" -> "What is your mother's maiden name?"
            "sq4" -> "What was your first school's name?"
            "sq5" -> "What was your childhood nickname?"
            "secAnswer" -> "Security answer"
            "phAnswer" -> "Your answer"
            "ansHint" -> "Stored encrypted · never shown to anyone"
            "termsText" -> "I agree to the Terms of Service and Privacy Policy, and certify I will use Lojia in compliance with business policies."
            "regBtn" -> "Register Business Account"
            "processing" -> "Processing…"
            "alreadyAccount" -> "Already have an account?"
            "signInLink" -> "Sign in"
            "successTitle" -> "Account Created! 🎉"
            "successSub" -> "Welcome to Lojia. Your business account has been successfully created. You can now sign in."
            "goToLogin" -> "Go to Login"
            "errRequired" -> "Required"
            "okGood" -> "✓ Looks good"
            "errUserLength" -> "3–20 characters required"
            "okUserAvail" -> "✓ Username available"
            "errValidEmail" -> "Enter a valid email"
            "okValidEmail" -> "✓ Valid email"
            "errValidPhone" -> "Enter a valid phone number (7–15 digits)"
            "errPwMin" -> "Min. 8 characters required"
            "errPwMatch" -> "Passwords do not match"
            "okPwMatch" -> "✓ Passwords match"
            "errSelQuestion" -> "Please select a question"
            "reqLen" -> "Min 8 characters"
            "reqCase" -> "Upper & lowercase"
            "reqNum" -> "Number"
            "reqSym" -> "Special char"
            "weak" -> "Weak"
            "fair" -> "Fair"
            "good" -> "Good"
            "strong" -> "Strong"
            "loginSuccess" -> "Login successful!"
            "loginFailed" -> "Incorrect username or password. Please try again."
            "regProgress" -> "Registration Progress"
            "allFieldsCompleted" -> "✅ All information 100% completed! You can now create your account."
            "pendingFieldsNotice" -> "Fill in the remaining fields to enable the Register button:"
            "btnDisabledHint" -> "⚠️ Register button will be enabled after 100% data is entered"
            "fieldFirstName" -> "First name"
            "fieldLastName" -> "Last name"
            "fieldUsername" -> "Username (3–20 letters/digits)"
            "fieldEmail" -> "Work email (valid format)"
            "fieldPhone" -> "Phone number (7–15 digits)"
            "fieldPassword" -> "Password (min. 8 characters)"
            "fieldConfirmPw" -> "Confirm password (must match)"
            "fieldSecQuestion" -> "Select security question"
            "fieldSecAnswer" -> "Security answer"
            "fieldTerms" -> "Accept terms and conditions"
            else -> key
        }
    }
}

enum class FieldValidationState {
    DEFAULT,
    ERROR,
    SUCCESS
}
