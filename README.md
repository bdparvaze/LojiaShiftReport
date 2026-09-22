# LojiaShiftReport 📊📄

*(Scroll down for Bengali & Arabic versions / বাংলা ও আরবি সংস্করণের জন্য নিচে স্ক্রোল করুন)*

**LojiaShiftReport** is an offline-first Android application specifically designed for retail and food service businesses to manage daily cashier shifts, financial reconciliations, shift handovers, and store documents. Built with modern Kotlin and Jetpack Compose, it pairs robust shift tracking with an integrated Document Scanner module.

---

## 🌟 Core Modules & Features

### 1. 📊 Shift Management & Reporting
* **Shift Handover & Reconciliation:** Open, pause, and close shifts with automated expected cash calculation, actual cash counting, pay-ins, pay-outs, and variance analysis.
* **Payment Method Breakdown:** Comprehensive breakdown of Gross Cash, MADA / Card payments, Digital Wallets, Expenses, Staff Meals, and Muassel tracking.
* **Shift PDF Generation & Printing:** Generate professional shift summaries with QR codes and store branding ready for sharing, exporting, or printing.
* **Sales Analytics & Comparison:** Daily, weekly, and monthly trend analytics, payment distribution visualizers, and shift performance comparisons.
* **Employee & Role Management:** Role-based security (Admin vs. Cashier) with biometric unlock and PIN-protected sensitive actions.

### 2. 📄 Document Scanner Module
* **Smart Scanning:** Powered by Google ML Kit Document Scanner with automatic edge detection, perspective correction, and multi-page scanning.
* **Camera & Gallery Fallback:** Seamless fallback capture allowing photo import and document creation even on devices without Google Play Services ML Kit.
* **Optical Character Recognition (OCR):** Automatic on-device text extraction from captured receipts, invoices, and contracts.
* **Multi-Format Export:** Export and share scanned documents as PDF or Microsoft Word (.docx).
* **Document Security:** Add password protection and encryption to sensitive scanned documents.

### 3. 🌐 Multilingual & Localized
* **3 Supported Languages:** Full UI localization in English, Bengali (বাংলা), and Arabic (العربية) with proper RTL layout support.
* **Multi-Currency & VAT Support:** Configure national currencies (SAR, BDT, USD, AED, etc.) and regional tax rates.

### 4. 🔒 Offline-First Architecture
* **Local Source of Truth:** High-performance Room Database (SQLite) with encrypted preferences ensures 100% functionality without internet.
* **Backup & Restore:** Full offline backup generation (JSON) and restore capability, plus optional background cloud synchronization.

---

## 🛠️ Tech Stack

* **Platform:** Android (minSdk 26, targetSdk 34)
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material Design 3)
* **Local Database:** Room Database (SQLite) with KSP
* **Architecture:** MVVM (Model-View-ViewModel) + Coroutines & Flow
* **Document Scanning & OCR:** Google ML Kit Document Scanner & Vision Text Recognition
* **Build System:** Gradle (Kotlin DSL)

---

## 📂 Project Structure

```text
com.lojia.shiftreport
│
├── MainActivity.kt                # Main Entry Point and Navigation Host
│
├── auth/                          # 🔐 Authentication, Biometrics, PIN Lock & Roles
│   ├── AdminAuthDialog.kt
│   ├── BiometricAuthManager.kt
│   ├── BiometricLockScreen.kt
│   └── LojiaAuthComponents.kt
│
├── data/                          # 💾 Room Database, Entities, DAOs & Repositories
│   ├── AppDatabase.kt
│   ├── Models.kt
│   ├── ReportDao.kt
│   ├── PreferencesRepository.kt
│   └── ShiftReportRepository.kt
│
├── report/                        # 📊 Shift Reports, Analytics, Ledgers & PDF Previews
│   ├── DashboardScreen.kt
│   ├── ShiftReportScreen.kt
│   ├── ShiftReportLedgerTabs.kt
│   ├── ShiftReportPreviewDialog.kt
│   └── ReportViewModel.kt
│
├── scanner/                       # 📄 Document Scanner, OCR, Word Export & PDF Security
│   ├── DocumentScannerScreen.kt
│   ├── DocumentScannerComponents.kt
│   ├── DocumentScannerDialogs.kt
│   ├── DocumentScannerViewModel.kt
│   ├── OcrTextExtractor.kt
│   ├── DocxExporter.kt
│   └── ScannedDocument.kt
│
├── settings/                      # ⚙️ Store Profile, Cashiers, Backup, Currency & Language
│   ├── SettingsReportSection.kt
│   ├── CashierManagementSection.kt
│   └── LanguageSettingsComponent.kt
│
├── ui/                            # 🎨 Theme, Typography, Reusable UI Components
│   ├── common/
│   │   ├── LojiaTextField.kt
│   │   ├── AppDrawer.kt
│   │   └── Components.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
└── util/                          # 🛠️ Utilities (PDF Generator, Security, Locale)
    ├── PdfReportGenerator.kt
    ├── LocaleManager.kt
    └── SecurityUtils.kt
```

---

## 🇧🇩 বাংলা বিবরণ (Bengali)

**LojiaShiftReport** হলো রিটেল দোকান এবং রেস্তোরাঁর জন্য একটি অফলাইন-ফার্স্ট অ্যান্ড্রয়েড শিফট ম্যানেজমেন্ট এবং ডকুমেন্ট স্ক্যানার অ্যাপ।

### প্রধান সুবিধাসমূহ:
* **শিফট হিসাব ও ক্যাশ ব্যালেন্সিং:** ক্যাশিয়ারের শিফট শুরু, ক্যাশ ইন/আউট, কার্ড ও ডিজিটাল পেমেন্ট হিসাব এবং ক্যাশ অমিল (Variance) নিরূপণ।
* **শিফট রিপোর্ট ও পিডিএফ প্রিন্ট:** তাত্ক্ষণিক শিফট রিপোর্ট তৈরি এবং কিউআর কোডসহ পিডিএফ রসিদ প্রিন্ট বা শেয়ার।
* **ডকুমেন্ট স্ক্যানার ও ওসিআর:** ক্যামেরা বা গ্যালারি থেকে রসিদ/চালান স্ক্যান, টেক্সট এক্সট্রাকশন (OCR), ওয়ার্ড (.docx) তৈরি এবং পাসওয়ার্ড দিয়ে সুরক্ষিত পিডিএফ তৈরি।
* **৩টি ভাষা সাপোর্ট:** সম্পূর্ণ বাংলা, ইংরেজি এবং আরবি ভাষা সমর্থন।

---

## 🇸🇦 الوصف بالعربية (Arabic)

**LojiaShiftReport** هو تطبيق أندرويد متكامل يعمل بدون إنترنت لإدارة ورديات الكاشير، تدقيق الصندوق النقدي، ومسح المستندات ضوئياً.

### الميزات الرئيسية:
* **إدارة الورديات وتدقيق النقد:** فتح وإغلاق الورديات، حساب المبيعات النقدية وشبكة مدى والمحافظ الرقمية، وحساب الفروقات النقدية.
* **تقارير الوردية وطباعة PDF:** توليد تقارير الوردية بتنسيق PDF احترافي مع رمز الاستجابة السريعة (QR).
* **ماسح المستندات الضوئي:** مسح الإيصالات والفواتير، استخراج النصوص (OCR)، وتصديرها بصيغة Word أو PDF محمي بكلمة مرور.
* **دعم 3 لغات:** دعم كامل للغات العربية، الإنجليزية، والبنجالية مع دعم اتجاه الكتابة من اليمين لليسار (RTL).
