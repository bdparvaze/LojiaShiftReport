# LojiaShiftReport - Architecture Guide 🏗️

LojiaShiftReport is built with Clean Architecture principles and MVVM (Model-View-ViewModel) using Jetpack Compose, Kotlin Coroutines & Flow, and Room Database.

---

## 1. Modular Structure

The codebase is organized into distinct domain and feature packages under `com.lojia.shiftreport`:

```text
com.lojia.shiftreport
│
├── MainActivity.kt                # App Entry Point & Navigation Host (Crossfade Routing)
│
├── auth/                          # 🔐 Authentication & Security Layer
│   ├── AdminAuthDialog.kt         # PIN authorization dialog for restricted operations
│   ├── BiometricAuthManager.kt    # Android BiometricPrompt wrapper
│   ├── BiometricLockScreen.kt     # App auto-lock and PIN unlock screen
│   ├── LojiaAuthComponents.kt     # Registration, login fields & country dial picker
│   └── LojiaAuthModels.kt         # User profile and registration state models
│
├── data/                          # 💾 Data & Persistence Layer
│   ├── AppDatabase.kt             # Room Database with automated migration handlers
│   ├── Models.kt                  # ShiftReport, ShiftSession, CashMovement, User, Country
│   ├── ReportDao.kt               # Shift report queries, analytics aggregations & user prefs
│   ├── PreferencesRepository.kt   # Encrypted SharedPreferences for tokens & quick login
│   └── ShiftReportRepository.kt   # Central repository orchestrating local Room & cloud sync
│
├── report/                        # 📊 Shift Reports & Analytics Module
│   ├── DashboardScreen.kt         # Shift performance metrics & financial analytics
│   ├── ShiftReportScreen.kt       # Active shift creation, cash counting & history list
│   ├── ShiftReportLedgerTabs.kt   # Multi-tab shift details & expense breakdown
│   ├── ShiftReportPreviewDialog.kt# Thermal & PDF preview dialog with QR verification
│   └── ReportViewModel.kt         # ViewModel managing shift states, metrics & exports
│
├── scanner/                       # 📄 Document Scanner & OCR Module
│   ├── DocumentScannerScreen.kt   # Main scanner screen with Activity context bridge
│   ├── DocumentScannerComponents.kt# Scan header card, document card & empty states
│   ├── DocumentScannerDialogs.kt  # Save, rename, batch delete, OCR & password dialogs
│   ├── DocumentScannerViewModel.kt# Document persistence, Word export & PDF encryption
│   ├── OcrTextExtractor.kt        # Google ML Kit on-device text recognition
│   ├── DocxExporter.kt            # Microsoft Word (.docx) document generator
│   ├── PdfSecurityManager.kt      # PDF password protection and encryption
│   └── ScannedDocument.kt         # Room entity & DAO for scanned documents
│
├── settings/                      # ⚙️ Settings & Configuration Module
│   ├── SettingsReportSection.kt   # Business profile, currency, country, tax & backups
│   ├── CashierManagementSection.kt# Cashier PIN setup and role assignments
│   └── LanguageSettingsComponent.kt# Dynamic in-app locale switcher (EN, BN, AR)
│
├── ui/                            # 🎨 Shared UI, Theme & Design System
│   ├── common/
│   │   ├── LojiaTextField.kt      # Standardized input fields with validation states
│   │   ├── AppDrawer.kt           # Side navigation drawer
│   │   └── Components.kt          # Shared cards, badges, statistics chips & headers
│   └── theme/
│       ├── Color.kt               # Material 3 color system (Primary Indigo, Slate, Emerald)
│       ├── Theme.kt               # Dynamic color schemes & typography bindings
│       └── Type.kt                # Poppins & system typography definitions
│
└── util/                          # 🛠️ Utility Functions & Helpers
    ├── PdfReportGenerator.kt      # Vector-based PDF receipt & shift summary builder
    ├── LocaleManager.kt           # Runtime language switching & RTL direction support
    └── SecurityUtils.kt           # SHA-256 secret hashing & cryptographic helpers
```

---

## 2. Key Architecture Principles

1. **Strict Offline-First**: Local Room Database is the single source of truth for all shift records, financial numbers, and scanned documents. No network latency blocks user operations.
2. **Feature Isolation**: Shift reporting and Document scanning operate with independent DAOs and ViewModels to prevent cross-module coupling.
3. **Robust Intent & Context Management**: Document scanning and PDF viewing utilize guaranteed Activity context references with proper Intent flags to prevent crashes.
4. **Multilingual Architecture**: Dynamic runtime language changing between English, Bengali, and Arabic without app restart, preserving full RTL layout hierarchy.
