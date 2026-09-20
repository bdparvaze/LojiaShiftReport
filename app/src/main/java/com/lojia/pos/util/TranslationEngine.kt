package com.lojia.pos.util

import android.content.Context
import com.lojia.pos.data.AppDatabase
import com.lojia.pos.data.TranslationCacheEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * High-Performance Dynamic Translation Helper with Offline Caching.
 * 
 * Multi-Tier Translation Architecture:
 * 1. In-Memory Concurrent Cache (0ms instant lookup)
 * 2. Instant Preloaded Core Seed Dictionary (offline fallback for essential POS/Shift keywords)
 * 3. Room Database Persistent Cache (persists dynamically cached translations across app restarts)
 * 4. Configurable translation endpoints for expanded localization
 */
object TranslationEngine {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val memoryCache = ConcurrentHashMap<String, String>()
    private val pendingTranslations = ConcurrentHashMap.newKeySet<String>()
    
    @Volatile
    private var database: AppDatabase? = null

    private val _translationUpdates = MutableStateFlow(0L)
    val translationUpdates: StateFlow<Long> = _translationUpdates.asStateFlow()

    // Core Instant Seed Dictionary for Zero-Latency Offline Experience
    private val seedDictionary = mapOf(
        // Arabic (ar)
        "POS Register_ar" to "نقطة البيع",
        "Shift Reports_ar" to "تقارير الوردية",
        "Shift Report_ar" to "تقرير الوردية",
        "Daily Shift Reports_ar" to "تقارير الوردية",
        "Daily Shift Report_ar" to "تقرير الوردية",
        "Settings_ar" to "الإعدادات",
        "Inventory_ar" to "المخزون",
        "Receipts_ar" to "الإيصالات",
        "Cashier_ar" to "الكاشير",
        "Shift_ar" to "الوردية",
        "Date_ar" to "التاريخ",
        "Total_ar" to "المجموع",
        "Subtotal_ar" to "المجموع الفرعي",
        "VAT_ar" to "ضريبة القيمة المضافة",
        "Cash_ar" to "نقداً",
        "Card_ar" to "بطاقة",
        "Save_ar" to "حفظ",
        "Cancel_ar" to "إلغاء",
        "Close_ar" to "إغلاق",
        "Open Shift_ar" to "فتح الوردية",
        "Close Shift_ar" to "إغلاق الوردية",
        "Customer_ar" to "العميل",
        "Walk-in Customer_ar" to "عميل عام",
        "Discount_ar" to "خصم",
        "Add Product_ar" to "إضافة منتج",
        "Pay In_ar" to "إيداع نقدي",
        "Pay Out_ar" to "سحب نقدي",
        "Grand Total_ar" to "المجموع الكلي",
        "Gross Sales_ar" to "إجمالي المبيعات",
        "Net Cash_ar" to "صافي النقد",
        "Expenses_ar" to "المصروفات",
        "Staff Advances_ar" to "سلف الموظفين",
        "Unpaid Bills_ar" to "فواتير غير مدفوعة",
        "Due Credit_ar" to "آجل / ذمم",
        "Drawer_ar" to "درج النقد",
        "Search_ar" to "بحث...",
        "Language_ar" to "اللغة",
        "Country / Currency_ar" to "الدولة / العملة",
        "Print_ar" to "طباعة",
        "Share_ar" to "مشاركة",
        "Export PDF_ar" to "تصدير PDF",
        "Export CSV_ar" to "تصدير CSV",

        // Bengali (bn)
        "POS Register_bn" to "পিওএস রেজিস্টার",
        "Biometric Authentication_bn" to "বায়োমেট্রিক আনলক",
        "Biometric Authentication_ar" to "المصادقة البيومترية",
        "Biometric Authentication_es" to "Autenticación biométrica",
        "Biometric Authentication_fr" to "Authentification biométrique",
        "Biometric Authentication_hi" to "बायोमेट्रिक प्रमाणीकरण",
        "Unlock with Fingerprint_bn" to "আঙুলের ছাপ ব্যবহার করুন",
        "Unlock with Fingerprint_ar" to "استخدم بصمة الإصبع",
        "Unlock with Fingerprint_es" to "Desbloquear con huella",
        "Unlock with Fingerprint_fr" to "Déverrouiller avec empreinte",
        "Unlock with Fingerprint_hi" to "फिंगरप्रिंट से अनलॉक करें",
        "Touch fingerprint sensor_bn" to "অ্যাপ আনলক করতে আঙুল রাখুন",
        "Touch fingerprint sensor_ar" to "المصادقة الآمنة",
        "Touch fingerprint sensor_es" to "Toca el sensor de huellas",
        "Touch fingerprint sensor_fr" to "Touchez le capteur d'empreintes",
        "Touch fingerprint sensor_hi" to "फिंगरप्रिंट सेंसर को स्पर्श करें",
        "Biometric not recognized. Please try again._bn" to "আঙুলের ছাপ মেলেনি, আবার চেষ্টা করুন",
        "Biometric not recognized. Please try again._ar" to "لم يتم التعرف على بصمة الإصبع. حاول مجدداً",
        "Biometric not recognized. Please try again._es" to "Huella no reconocida. Inténtalo de nuevo.",
        "Biometric not recognized. Please try again._fr" to "Empreinte non reconnue. Veuillez réessayer.",
        "Biometric not recognized. Please try again._hi" to "बायোমেট্রিক সনাক্ত করা যায়নি। আবার চেষ্টা করুন।",
        "Incorrect PIN. Try again._bn" to "ভুল পিন! আবার চেষ্টা করুন",
        "Incorrect PIN. Try again._ar" to "رمز PIN غير صحيح. حاول مجدداً",
        "Incorrect PIN. Try again._es" to "PIN incorrecto. Inténtalo de nuevo.",
        "Incorrect PIN. Try again._fr" to "PIN incorrect. Réessayez.",
        "Incorrect PIN. Try again._hi" to "गलत पिन! पुनः प्रयास करें।",
        "Enter your 6-digit PIN_bn" to "আপনার ৬-সংখ্যার পিন প্রবেশ করান",
        "Enter your 6-digit PIN_ar" to "أدخل رمز PIN المكون من 6 أرقام",
        "Enter your 6-digit PIN_es" to "Introduce tu PIN de 6 dígitos",
        "Enter your 6-digit PIN_fr" to "Entrez votre code PIN à 6 chiffres",
        "Enter your 6-digit PIN_hi" to "अपना 6 अंकों का पिन दर्ज करें",
        "Forgot PIN?_bn" to "পিন ভুলে গেছেন?",
        "Forgot PIN?_ar" to "هل نسيت رمز PIN؟",
        "Forgot PIN?_es" to "¿Olvidaste el PIN?",
        "Forgot PIN?_fr" to "PIN oublié ?",
        "Forgot PIN?_hi" to "पिन भूल गए?",
        "Password Assistance_bn" to "পাসওয়ার্ড সহায়তা",
        "Password Assistance_ar" to "مساعدة كلمة المرور",
        "Password Assistance_es" to "Asistencia con contraseña",
        "Password Assistance_fr" to "Assistance mot de passe",
        "Password Assistance_hi" to "पासवर्ड सहायता",
        "If you forgot your PIN, please use your fingerprint or contact your store administrator._bn" to "পিন ভুলে গেলে বায়োমেট্রিক ফিঙ্গারপ্রিন্ট ব্যবহার করুন অথবা স্টোর অ্যাডমিনিস্ট্রেটরের সাথে যোগাযোগ করুন।",
        "If you forgot your PIN, please use your fingerprint or contact your store administrator._ar" to "إذا نسيت رمز PIN، يرجى استخدام بصمة الإصبع أو التواصل مع مدير المتجر.",
        "If you forgot your PIN, please use your fingerprint or contact your store administrator._es" to "Si olvidaste tu PIN, usa tu huella o contacta al administrador.",
        "If you forgot your PIN, please use your fingerprint or contact your store administrator._fr" to "Si vous avez oublié votre code PIN, utilisez votre empreinte digitale ou contactez l'administrateur.",
        "If you forgot your PIN, please use your fingerprint or contact your store administrator._hi" to "यदि आप अपना पिन भूल गए हैं, तो कृपया अपने फिंगरप्रिंट का उपयोग करें या व्यवस्थापक से संपर्क करें।",
        "Exit Application?_bn" to "অ্যাপ থেকে প্রস্থান করবেন?",
        "Exit Application?_ar" to "الخروج من التطبيق؟",
        "Exit Application?_es" to "¿Salir de la aplicación?",
        "Exit Application?_fr" to "Quitter l'application ?",
        "Exit Application?_hi" to "ऐप से बाहर निकलें?",
        "Are you sure you want to exit the application?_bn" to "আপনি কি নিশ্চিত যে আপনি অ্যাপ্লিকেশনটি বন্ধ করতে চান?",
        "Are you sure you want to exit the application?_ar" to "هل أنت متأكد أنك تريد إغلاق التطبيق؟",
        "Are you sure you want to exit the application?_es" to "¿Estás seguro de que deseas salir de la aplicación?",
        "Are you sure you want to exit the application?_fr" to "Êtes-vous sûr de vouloir quitter l'application ?",
        "Are you sure you want to exit the application?_hi" to "क्या आप वाकई एप्लिकेशन से बाहर निकलना चाहते हैं?",
        "Exit_bn" to "প্রস্থান",
        "Exit_ar" to "خروج",
        "Exit_es" to "Salir",
        "Exit_fr" to "Quitter",
        "Exit_hi" to "बाहर निकलें",
        "OK_bn" to "ঠিক আছে",
        "OK_ar" to "حسناً",
        "OK_es" to "Aceptar",
        "OK_fr" to "OK",
        "OK_hi" to "ठीक है",
        "Biometric Unlock_bn" to "বায়োমেট্রিক আনলক",
        "Biometric Unlock_ar" to "فتح بالبصمة",
        "Biometric Unlock_es" to "Desbloqueo biométrico",
        "Biometric Unlock_fr" to "Déverrouillage biométrique",
        "Biometric Unlock_hi" to "बायोमेट्रिक अनलॉक",
        "Backspace_bn" to "মুছুন",
        "Backspace_ar" to "مسح",
        "Backspace_es" to "Borrar",
        "Backspace_fr" to "Effacer",
        "Backspace_hi" to "मिटाएं",
        "Shift Reports_bn" to "শিফট রিপোর্ট",
        "Shift Report_bn" to "শিফট রিপোর্ট",
        "Daily Shift Reports_bn" to "শিফট রিপোর্ট",
        "Daily Shift Report_bn" to "শিফট রিপোর্ট",
        "Settings_bn" to "সেটিংস",
        "Inventory_bn" to "ইনভেন্টরি / স্টক",
        "Receipts_bn" to "রসিদ",
        "Cashier_bn" to "ক্যাশিয়ার",
        "Shift_bn" to "শিফট",
        "Date_bn" to "তারিখ",
        "Total_bn" to "মোট",
        "Subtotal_bn" to "সাবটোটাল",
        "VAT_bn" to "ভ্যাট / ট্যাক্স",
        "Cash_bn" to "ক্যাশ",
        "Card_bn" to "কার্ড",
        "Save_bn" to "সংরক্ষণ",
        "Cancel_bn" to "বাতিল",
        "Close_bn" to "বন্ধ",
        "Open Shift_bn" to "শিফট শুরু",
        "Close Shift_bn" to "শিফট বন্ধ",
        "Customer_bn" to "গ্রাহক",
        "Walk-in Customer_bn" to "সাধারণ গ্রাহক",
        "Discount_bn" to "ছাড়",
        "Add Product_bn" to "পণ্য যোগ করুন",
        "Pay In_bn" to "ক্যাশ ইন",
        "Pay Out_bn" to "ক্যাশ আউট",
        "Grand Total_bn" to "সর্বমোট",
        "Gross Sales_bn" to "মোট বিক্রি",
        "Net Cash_bn" to "নিট ক্যাশ",
        "Expenses_bn" to "খরচ",
        "Staff Advances_bn" to "স্টাফ অগ্রিম",
        "Unpaid Bills_bn" to "বকেয়া বিল",
        "Due Credit_bn" to "বাকি / ঋণ",
        "Drawer_bn" to "ক্যাশ ড্রয়ার",
        "Search_bn" to "অনুসন্ধান...",
        "Language_bn" to "ভাষা",
        "Country / Currency_bn" to "দেশ / মুদ্রা",
        "Print_bn" to "প্রিন্ট",
        "Share_bn" to "শেয়ার",
        "Export PDF_bn" to "পিডিএফ এক্সপোর্ট",
        "Export CSV_bn" to "সিএসভি এক্সপোর্ট",
        "Backup_bn" to "ব্যাকআপ",
        "Data Backup & Restore_bn" to "ব্যাকআপ",
        "Data Backup and Restore_bn" to "ব্যাকআপ",
        "Data Backup_bn" to "ব্যাকআপ",
        "Backup & Restore_bn" to "ব্যাকআপ",
        "Backup and Restore_bn" to "ব্যাকআপ",
        "ডাটা ব্যাকআপ ও পুনরুদ্ধার_bn" to "ব্যাকআপ",
        "ডাটা ব্যাকআপ এবং পুনরুদ্ধার_bn" to "ব্যাকআপ",
        "ব্যাকআপ ও পুনরুদ্ধার_bn" to "ব্যাকআপ",
        "ব্যাকআপ এবং পুনরুদ্ধার_bn" to "ব্যাকআপ",
        "ডাটা ব্যাকআপ_bn" to "ব্যাকআপ",
        "ব্যাকআপ_bn" to "ব্যাকআপ",
        "Backup_en" to "Backup",
        "Data Backup & Restore_en" to "Backup",
        "Data Backup and Restore_en" to "Backup",
        "Data Backup_en" to "Backup",
        "Backup & Restore_en" to "Backup",
        "Backup and Restore_en" to "Backup",
        "ডাটা ব্যাকআপ ও পুনরুদ্ধার_en" to "Backup",
        "ব্যাকআপ ও পুনরুদ্ধার_en" to "Backup",
        "ব্যাকআপ_en" to "Backup",

        "Cashier Management_bn" to "ক্যাশিয়ার",
        "Cashier Management_en" to "Cashier",
        "Cashier Management_es" to "Cajero",
        "Cashier Management_ar" to "الكاشير",
        "Cashier Management_hi" to "कैशियर",
        "Cashier Management_id" to "Kasir",
        "Cashier Management_fr" to "Caissier",
        "Cashier Management_de" to "Kassierer",
        "Cashier Management_ja" to "キャッシャー",
        "Cashier Management_tr" to "Kasiyer",
        "Cashier Management_ur" to "کیشیئر",
        "Cashier Management_ru" to "Кассиры",
        "Cashier Management_zh" to "收银员",
        "ক্যাশিয়ার ব্যবস্থাপনা_bn" to "ক্যাশিয়ার",
        "ক্যাশিয়ার ব্যবস্থাপনা_en" to "Cashier",
        "ক্যাশিয়ার ম্যানেজমেন্ট_bn" to "ক্যাশিয়ার",
        "ক্যাশিয়ার ম্যানেজমেন্ট_en" to "Cashier",

        "Daily Shift Report_en" to "Shift Report",
        "Daily Shift Reports_en" to "Shift Reports",
        "Daily Shift Report_bn" to "শিফট রিপোর্ট",
        "Daily Shift Reports_bn" to "শিফট রিপোর্ট",
        "Daily Shift Report_es" to "Informe de turno",
        "Daily Shift Reports_es" to "Informes de turno",
        "Daily Shift Report_ar" to "تقرير الوردية",
        "Daily Shift Reports_ar" to "تقارير الوردية",
        "Daily Shift Report_hi" to "शिफ्ट रिपोर्ट",
        "Daily Shift Report_id" to "Laporan Shift",
        "Daily Shift Report_fr" to "Rapport de quart",
        "Daily Shift Report_de" to "Schichtbericht",
        "Daily Shift Report_ja" to "シフトレポート",
        "Daily Shift Report_tr" to "Vardiya Raporu",
        "Daily Shift Report_ur" to "شفٹ رپورٹ",
        "Daily Shift Report_ru" to "Отчет по смене",
        "Daily Shift Report_zh" to "交班报告",
        "দৈনিক শিফট রিপোর্ট_bn" to "শিফট রিপোর্ট",
        "দৈনিক শিফট রিপোর্ট_en" to "Shift Report",

        "Confirm PIN Number_bn" to "পিন নম্বর কনফার্ম করুন",
        "Confirm PIN Number_ar" to "تأكيد رقم التعريف الشخصي (PIN)",
        "Confirm PIN Number_ur" to "پن نمبر کی تصدیق کریں",
        "Confirm PIN Number_hi" to "पिन नंबर की पुष्टि करें",
        "Confirm PIN Number_es" to "Confirmar número PIN",
        "Confirm PIN Number_fr" to "Confirmer le code PIN",
        "Confirm PIN Number_de" to "PIN-Nummer bestätigen",
        "Confirm PIN Number_id" to "Konfirmasi Nomor PIN",
        "Confirm PIN Number_ja" to "PINコードを確認",
        "Confirm PIN Number_ru" to "Подтвердите PIN-код",
        "Confirm PIN Number_tr" to "PIN Numarasını Doğrulayın",
        "Confirm PIN Number_zh" to "确认 PIN 码",
        "Confirm PIN Number_en" to "Confirm PIN Number",

        "Confirm PIN_bn" to "পিন নম্বর কনফার্ম করুন",
        "Confirm PIN_ar" to "تأكيد رقم الـ PIN",
        "Confirm PIN_ur" to "پن کی تصدیق کریں",
        "Confirm PIN_hi" to "पिन की पुष्टि करें",
        "Confirm PIN_es" to "Confirmar PIN",
        "Confirm PIN_fr" to "Confirmer le PIN",
        "Confirm PIN_de" to "PIN bestätigen",
        "Confirm PIN_id" to "Konfirmasi PIN",
        "Confirm PIN_ja" to "PINを確認",
        "Confirm PIN_ru" to "Подтвердите PIN",
        "Confirm PIN_tr" to "PIN'i Doğrula",
        "Confirm PIN_zh" to "确认 PIN",
        "Confirm PIN_en" to "Confirm PIN",

        "Switch to:_bn" to "সুইচ:",
        "Switch to:_ar" to "التبديل إلى:",
        "Switch to:_ur" to "سوئچ کریں:",
        "Switch to:_hi" to "स्विच करें:",
        "Switch to:_es" to "Cambiar a:",
        "Switch to:_fr" to "Basculer vers :",
        "Switch to:_de" to "Wechseln zu:",
        "Switch to:_id" to "Beralih ke:",
        "Switch to:_ja" to "切り替え先:",
        "Switch to:_ru" to "Перейти в:",
        "Switch to:_tr" to "Geçiş yap:",
        "Switch to:_zh" to "切换至：",
        "Switch to:_en" to "Switch to:",

        "Switch to_bn" to "সুইচ",
        "Switch to_ar" to "التبديل إلى",
        "Switch to_ur" to "سوئچ کریں",
        "Switch to_hi" to "स्विच करें",
        "Switch to_es" to "Cambiar a",
        "Switch to_fr" to "Passer à",
        "Switch to_de" to "Wechseln zu",
        "Switch to_id" to "Beralih ke",
        "Switch to_ja" to "切り替え先",
        "Switch to_ru" to "Перейти в",
        "Switch to_tr" to "Geçiş yap",
        "Switch to_zh" to "切换至",
        "Switch to_en" to "Switch to",

        "Performance Analytics_en" to "Analytics",
        "Performance Analytics_bn" to "অ্যানালিটিক্স",
        "Performance Analytics_es" to "Analítica",
        "Performance Analytics_ar" to "التحليلات",
        "Performance Analytics_hi" to "एनालिटिक्स",
        "Performance Analytics_id" to "Analitik",
        "Performance Analytics_fr" to "Analyses",
        "Performance Analytics_de" to "Analysen",
        "Performance Analytics_ja" to "分析",
        "Performance Analytics_tr" to "Analitik",
        "Performance Analytics_ur" to "اینالیٹکس",
        "Performance Analytics_ru" to "Аналитика",
        "Performance Analytics_zh" to "分析",
        "Analytics_bn" to "অ্যানালিটিক্স",
        "Analytics_en" to "Analytics",
        "পারফরম্যান্স অ্যানালিটিক্স_bn" to "অ্যানালিটিক্স",
        "পারফরম্যান্স অ্যানালিটিক্স_en" to "Analytics",

        // Spanish (es)
        "POS Register_es" to "Caja Registradora",
        "Shift Reports_es" to "Informes de Turno",
        "Settings_es" to "Configuración",
        "Inventory_es" to "Inventario",
        "Receipts_es" to "Recibos",
        "Cashier_es" to "Cajero",
        "Shift_es" to "Turno",
        "Date_es" to "Fecha",
        "Total_es" to "Total",
        "Subtotal_es" to "Subtotal",
        "Save_es" to "Guardar",
        "Cancel_es" to "Cancelar",
        "Open Shift_es" to "Abrir Turno",
        "Close Shift_es" to "Cerrar Turno",
        "Customer_es" to "Cliente",
        "Walk-in Customer_es" to "Cliente General",

        // French (fr)
        "POS Register_fr" to "Caisse Enregistreuse",
        "Shift Reports_fr" to "Rapports de Quart",
        "Settings_fr" to "Paramètres",
        "Inventory_fr" to "Inventaire",
        "Receipts_fr" to "Reçus",
        "Cashier_fr" to "Caissier",
        "Shift_fr" to "Quart",
        "Date_fr" to "Date",
        "Total_fr" to "Total",
        "Subtotal_fr" to "Sous-total",
        "Save_fr" to "Enregistrer",
        "Cancel_fr" to "Annuler",
        "Open Shift_fr" to "Ouvrir le Quart",
        "Close Shift_fr" to "Fermer le Quart",
        "Customer_fr" to "Client",
        "Walk-in Customer_fr" to "Client de Passage",

        // Hindi (hi)
        "POS Register_hi" to "पीओएस रजिस्टर",
        "Shift Reports_hi" to "शिफ्ट रिपोर्ट",
        "Settings_hi" to "सेटिंग्स",
        "Inventory_hi" to "इन्वेंट्री",
        "Receipts_hi" to "रसीदें",
        "Cashier_hi" to "कैशियर",
        "Shift_hi" to "शिफ्ट",
        "Date_hi" to "तारीख",
        "Total_hi" to "कुल",
        "Subtotal_hi" to "उप-कुल",
        "Save_hi" to "सहेजें",
        "Cancel_hi" to "रद्द करें",
        "Open Shift_hi" to "शिफ्ट शुरू करें",
        "Close Shift_hi" to "शिफ्ट बंद करें",
        "Customer_hi" to "ग्राहक",
        "Walk-in Customer_hi" to "सामान्य ग्राहक",

        // Urdu (ur)
        "POS Register_ur" to "پی او ایس رجسٹر",
        "Shift Reports_ur" to "شفٹ رپورٹس",
        "Settings_ur" to "ترتیبات",
        "Inventory_ur" to "انوینٹری",
        "Receipts_ur" to "رسیدیں",
        "Cashier_ur" to "کیشیئر",
        "Shift_ur" to "شفٹ",
        "Date_ur" to "تاریخ",
        "Total_ur" to "کل",
        "Subtotal_ur" to "ذیلی کل",
        "Save_ur" to "محفوظ کریں",
        "Cancel_ur" to "منسوخ کریں",
        "Open Shift_ur" to "شفٹ کھولیں",
        "Close Shift_ur" to "شفٹ بند کریں",
        "Customer_ur" to "گاہک",
        "Walk-in Customer_ur" to "عام گاہک",

        // German (de)
        "POS Register_de" to "Kassenregister",
        "Shift Reports_de" to "Schichtberichte",
        "Settings_de" to "Einstellungen",
        "Inventory_de" to "Inventar",
        "Cashier_de" to "Kassierer",
        "Total_de" to "Gesamt",
        "Subtotal_de" to "Zwischensumme",
        "Save_de" to "Speichern",
        "Cancel_de" to "Abbrechen",

        // Chinese (zh)
        "POS Register_zh" to "POS 收银机",
        "Shift Reports_zh" to "班次报告",
        "Settings_zh" to "设置",
        "Inventory_zh" to "库存",
        "Cashier_zh" to "收银员",
        "Total_zh" to "总计",
        "Subtotal_zh" to "小计",
        "Save_zh" to "保存",
        "Cancel_zh" to "取消"
    )

    fun init(context: Context) {
        if (database == null) {
            database = AppDatabase.getInstance(context)
            // Preload database cache in memory and purge obsolete backup/restore and cashier management translations
            scope.launch {
                try {
                    database?.translationDao()?.clearBackupTranslations()
                    database?.translationDao()?.clearCashierTranslations()
                    database?.translationDao()?.clearDailyShiftTranslations()
                    database?.translationDao()?.clearPerformanceAnalyticsTranslations()
                    val allCached = database?.translationDao()?.getAllTranslationsForLanguage("") ?: emptyList()
                    for (item in allCached) {
                        val key = item.cacheKey.lowercase()
                        val text = item.translatedText.lowercase()
                        if (!key.contains("backup") && !key.contains("restore") && !key.contains("পুনরুদ্ধার") &&
                            !text.contains("restore") && !text.contains("পুনরুদ্ধার") &&
                            !key.contains("cashier management") && !text.contains("ব্যবস্থাপনা") && !text.contains("ম্যানেজমেন্ট") &&
                            !key.contains("daily shift") && !text.contains("দৈনিক শিফট") && !key.contains("দৈনিক শিফট") &&
                            !key.contains("performance analytics") && !text.contains("পারফরম্যান্স অ্যানালিটিক্স") && !key.contains("পারফরম্যান্স অ্যানালিটিক্স")) {
                            memoryCache[item.cacheKey] = item.translatedText
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun isCashierString(text: String): Boolean {
        val clean = text.trim().lowercase()
        return clean == "cashier" ||
                clean == "cashiers" ||
                clean == "cashier management" ||
                clean == "cashiers & staff" ||
                clean == "cashiers and staff" ||
                clean == "cashier & management" ||
                clean == "cashier and management" ||
                clean == "ক্যাশিয়ার" ||
                clean == "ক্যাশিয়ার ব্যবস্থাপনা" ||
                clean == "ক্যাশিয়ার ম্যানেজমেন্ট" ||
                clean == "ক্যাশিয়ার ও ব্যবস্থাপনা" ||
                clean.contains("cashier management") ||
                clean.contains("ক্যাশিয়ার ব্যবস্থাপনা") ||
                clean.contains("ক্যাশিয়ার ম্যানেজমেন্ট")
    }

    private fun getCanonicalCashierText(targetLanguageCode: String): String {
        return when (targetLanguageCode.lowercase().trim()) {
            "bn" -> "ক্যাশিয়ার"
            "en" -> "Cashier"
            "ar" -> "الكاشير"
            "es" -> "Cajero"
            "hi" -> "कैशियर"
            "ur" -> "کیشیئر"
            "id" -> "Kasir"
            "tr" -> "Kasiyer"
            "ru" -> "Кассиры"
            "fr" -> "Caissier"
            "ja" -> "キャッシャー"
            "zh", "zh-cn", "zh-rcn" -> "收银员"
            "de" -> "Kassierer"
            else -> "Cashier"
        }
    }

    private fun isShiftReportString(text: String): Boolean {
        val clean = text.trim().lowercase()
        return clean == "daily shift report" ||
                clean == "daily shift reports" ||
                clean == "daily shift" ||
                clean == "shift report" ||
                clean == "shift reports" ||
                clean == "দৈনিক শিফট রিপোর্ট" ||
                clean == "শিফট রিপোর্ট" ||
                clean == "দৈনিক শিফট" ||
                clean.contains("daily shift report") ||
                clean.contains("দৈনিক শিফট রিপোর্ট")
    }

    private fun getCanonicalShiftReportText(targetLanguageCode: String): String {
        return when (targetLanguageCode.lowercase().trim()) {
            "bn" -> "শিফট রিপোর্ট"
            "en" -> "Shift Report"
            "ar" -> "تقرير الوردية"
            "es" -> "Informe de turno"
            "hi" -> "शिफ्ट रिपोर्ट"
            "ur" -> "شفٹ رپورٹ"
            "id" -> "Laporan Shift"
            "tr" -> "Vardiya Raporu"
            "ru" -> "Отчет по смене"
            "fr" -> "Rapport de quart"
            "ja" -> "シフトレポート"
            "zh", "zh-cn", "zh-rcn" -> "交班报告"
            "de" -> "Schichtbericht"
            else -> "Shift Report"
        }
    }

    private fun isAnalyticsString(text: String): Boolean {
        val clean = text.trim().lowercase()
        return clean == "performance analytics" ||
                clean == "analytics" ||
                clean == "পারফরম্যান্স অ্যানালিটিক্স" ||
                clean == "অ্যানালিটিক্স" ||
                clean.contains("performance analytics") ||
                clean.contains("পারফরম্যান্স অ্যানালিটিক্স")
    }

    private fun getCanonicalAnalyticsText(targetLanguageCode: String): String {
        return when (targetLanguageCode.lowercase().trim()) {
            "bn" -> "অ্যানালিটিক্স"
            "en" -> "Analytics"
            "ar" -> "التحليلات"
            "es" -> "Analítica"
            "hi" -> "एनालिटिक्स"
            "ur" -> "اینالیٹکس"
            "id" -> "Analitik"
            "tr" -> "Analitik"
            "ru" -> "Аналитика"
            "fr" -> "Analyses"
            "ja" -> "分析"
            "zh", "zh-cn", "zh-rcn" -> "分析"
            "de" -> "Analysen"
            else -> "Analytics"
        }
    }

    private fun isBackupString(text: String): Boolean {
        val clean = text.trim().lowercase()
        return clean == "backup" ||
                clean.contains("data backup") ||
                clean.contains("backup & restore") ||
                clean.contains("backup and restore") ||
                clean.contains("ডাটা ব্যাকআপ") ||
                clean.contains("পুনরুদ্ধার") ||
                clean == "ব্যাকআপ" ||
                clean.contains("backup &") ||
                clean.contains("backup and") ||
                clean.contains("restore")
    }

    private fun getCanonicalBackupText(targetLanguageCode: String): String {
        return when (targetLanguageCode.lowercase().trim()) {
            "bn" -> "ব্যাকআপ"
            "en" -> "Backup"
            "ar" -> "نسخ احتياطي"
            "es" -> "Copia de seguridad"
            "hi" -> "बैकअप"
            "ur" -> "بیک اپ"
            "id" -> "Cadangan"
            "tr" -> "Yedekleme"
            "ru" -> "Резервная копия"
            "fr" -> "Sauvegarde"
            "ja" -> "バックアップ"
            "zh", "zh-cn", "zh-rcn" -> "备份"
            "de" -> "Sicherung"
            else -> "Backup"
        }
    }

    /**
     * Translates any plain English string into the requested target language code in real time.
     * 1. If English or empty, returns instantly.
     * 2. Checks In-Memory Cache.
     * 3. Checks Preloaded Seed Dictionary.
     * 4. Queries Room Database and Cloud Translation API asynchronously.
     */
    fun translate(sourceText: String, targetLanguageCode: String): String {
        val cleanText = sourceText.trim()
        if (cleanText.isBlank()) return sourceText

        if (isBackupString(cleanText)) {
            return getCanonicalBackupText(targetLanguageCode)
        }

        if (isCashierString(cleanText)) {
            return getCanonicalCashierText(targetLanguageCode)
        }

        if (isShiftReportString(cleanText)) {
            return getCanonicalShiftReportText(targetLanguageCode)
        }

        if (isAnalyticsString(cleanText)) {
            return getCanonicalAnalyticsText(targetLanguageCode)
        }

        if (targetLanguageCode.equals("en", ignoreCase = true)) {
            return sourceText
        }
        val cacheKey = "${cleanText}_$targetLanguageCode"

        // 1. In-Memory Cache hit
        val cached = memoryCache[cacheKey]
        if (cached != null) {
            return cached
        }

        // 2. Instant Seed Dictionary
        val seedHit = seedDictionary[cacheKey]
        if (seedHit != null) {
            memoryCache[cacheKey] = seedHit
            return seedHit
        }

        // 3. Queue asynchronous Room lookup and Remote Fetch
        if (pendingTranslations.add(cacheKey)) {
            scope.launch {
                fetchAndCache(cleanText, targetLanguageCode, cacheKey)
            }
        }

        return sourceText
    }

    /**
     * Synchronous blocking translate helper for non-composable environments (e.g. PDF generation, export)
     */
    suspend fun translateSync(sourceText: String, targetLanguageCode: String): String {
        val cleanText = sourceText.trim()
        if (cleanText.isBlank()) return sourceText

        if (isBackupString(cleanText)) {
            return getCanonicalBackupText(targetLanguageCode)
        }

        if (isCashierString(cleanText)) {
            return getCanonicalCashierText(targetLanguageCode)
        }

        if (isShiftReportString(cleanText)) {
            return getCanonicalShiftReportText(targetLanguageCode)
        }

        if (isAnalyticsString(cleanText)) {
            return getCanonicalAnalyticsText(targetLanguageCode)
        }

        if (targetLanguageCode.equals("en", ignoreCase = true)) {
            return sourceText
        }
        val cacheKey = "${cleanText}_$targetLanguageCode"

        val cached = memoryCache[cacheKey]
        if (cached != null) return cached

        val seedHit = seedDictionary[cacheKey]
        if (seedHit != null) return seedHit

        val dbHit = withContext(Dispatchers.IO) {
            try {
                database?.translationDao()?.getTranslation(cacheKey)
            } catch (e: Exception) {
                null
            }
        }
        if (!dbHit.isNullOrBlank()) {
            memoryCache[cacheKey] = dbHit
            return dbHit
        }

        val remoteHit = withContext(Dispatchers.IO) {
            fetchFromGoogleTranslate(cleanText, targetLanguageCode)
        }
        if (!remoteHit.isNullOrBlank()) {
            memoryCache[cacheKey] = remoteHit
            withContext(Dispatchers.IO) {
                try {
                    database?.translationDao()?.saveTranslation(
                        TranslationCacheEntity(
                            cacheKey = cacheKey,
                            sourceText = cleanText,
                            targetLanguage = targetLanguageCode,
                            translatedText = remoteHit
                        )
                    )
                } catch (_: Exception) {}
            }
            _translationUpdates.value = System.currentTimeMillis()
            return remoteHit
        }

        return sourceText
    }

    private suspend fun fetchAndCache(sourceText: String, targetLanguageCode: String, cacheKey: String) {
        try {
            if (isBackupString(sourceText) || isBackupString(cacheKey)) {
                val canonical = getCanonicalBackupText(targetLanguageCode)
                memoryCache[cacheKey] = canonical
                pendingTranslations.remove(cacheKey)
                _translationUpdates.value = System.currentTimeMillis()
                return
            }

            if (isCashierString(sourceText) || isCashierString(cacheKey)) {
                val canonical = getCanonicalCashierText(targetLanguageCode)
                memoryCache[cacheKey] = canonical
                pendingTranslations.remove(cacheKey)
                _translationUpdates.value = System.currentTimeMillis()
                return
            }

            if (isShiftReportString(sourceText) || isShiftReportString(cacheKey)) {
                val canonical = getCanonicalShiftReportText(targetLanguageCode)
                memoryCache[cacheKey] = canonical
                pendingTranslations.remove(cacheKey)
                _translationUpdates.value = System.currentTimeMillis()
                return
            }

            if (isAnalyticsString(sourceText) || isAnalyticsString(cacheKey)) {
                val canonical = getCanonicalAnalyticsText(targetLanguageCode)
                memoryCache[cacheKey] = canonical
                pendingTranslations.remove(cacheKey)
                _translationUpdates.value = System.currentTimeMillis()
                return
            }

            // Check Room Database
            val localDbTranslation = withContext(Dispatchers.IO) {
                try {
                    database?.translationDao()?.getTranslation(cacheKey)
                } catch (e: Exception) {
                    null
                }
            }

            if (!localDbTranslation.isNullOrBlank()) {
                memoryCache[cacheKey] = localDbTranslation
                pendingTranslations.remove(cacheKey)
                _translationUpdates.value = System.currentTimeMillis()
                return
            }

            // Remote translation request to Google Translate API / ML Service
            val remoteTranslated = withContext(Dispatchers.IO) {
                fetchFromGoogleTranslate(sourceText, targetLanguageCode)
            }

            if (!remoteTranslated.isNullOrBlank()) {
                memoryCache[cacheKey] = remoteTranslated
                // Persist to Room Database for instant offline reuse
                withContext(Dispatchers.IO) {
                    try {
                        database?.translationDao()?.saveTranslation(
                            TranslationCacheEntity(
                                cacheKey = cacheKey,
                                sourceText = sourceText,
                                targetLanguage = targetLanguageCode,
                                translatedText = remoteTranslated
                            )
                        )
                    } catch (_: Exception) {}
                }
                _translationUpdates.value = System.currentTimeMillis()
            }
        } catch (_: Exception) {
        } finally {
            pendingTranslations.remove(cacheKey)
        }
    }

    private fun fetchFromGoogleTranslate(text: String, targetLang: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encodedText")
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val sentences = jsonArray.getJSONArray(0)
                val builder = StringBuilder()
                for (i in 0 until sentences.length()) {
                    builder.append(sentences.getJSONArray(i).getString(0))
                }
                builder.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
