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
│   ├── BiometricHelper.kt         # Helpers for biometric registration and states
│   ├── BiometricLockScreen.kt     # App auto-lock and PIN unlock screen
│   ├── LojiaAuthComponents.kt     # Registration, login fields & country dial picker
│   ├── LojiaAuthDialogs.kt        # Security and lockout management dialogs
│   ├── LojiaAuthModels.kt         # User profile and registration state models
│   ├── LojiaRegisterScreen.kt     # Registration flow for first time administrator setup
│   ├── QuickPinScreen.kt          # PIN passcode screen for cashiers and admin
│   └── SecurityHelper.kt          # Security and verification functions
│
├── data/                          # 💾 Data & Persistence Layer
│   ├── AppDatabase.kt             # Room Database with automated migration handlers
│   ├── ConfigurationSyncManager.kt# Manages synchronization logic for business configs
│   ├── Models.kt                  # ShiftReport, ShiftSession, CashMovement, User, Country
│   ├── PreferencesRepository.kt   # Preferences repository for user configs and runtime settings
│   ├── ReportDao.kt               # Shift report queries, analytics aggregations & user prefs
│   ├── SavedAddressDao.kt         # DAO for managing saved business addresses
│   └── ShiftReportRepository.kt   # Central repository orchestrating local Room & data flows
│
├── permission/                    # 🛂 Runtime Permission Handling
│   └── PermissionManager.kt       # Activity-result based runtime permission handling wrapper
│
├── printer/                       # 🖨️ ESC/POS Thermal Printing Module
│   ├── BluetoothPrinterManager.kt # Core printer communication & cash drawer kick pulse
│   ├── PrinterSetupDialog.kt      # Configures network and Bluetooth receipt printers
│   └── ThermalBitmapRenderer.kt   # RTL Arabic & Bengali custom bitmap line renderer
│
├── report/                        # 📊 Shift Reports & Analytics Module
│   ├── AddEntryDialog.kt          # Input dialog for recording shift revenue or expenses
│   ├── DashboardScreen.kt         # Shift performance metrics & financial analytics
│   ├── MonthlySalesCharts.kt      # Visual charts for sales trends and payment metrics
│   ├── MonthlySalesSummaryView.kt # Summary metrics view for monthly sales tracking
│   ├── PosReconciliationSummary.kt# Shift financial reconciliation calculations and details
│   ├── ReportViewModel.kt         # ViewModel managing shift states, metrics & exports
│   ├── ShiftReportArchivesTab.kt  # Historic shift lists and PDF exports
│   ├── ShiftReportComponents.kt   # Dynamic input cards and UI layout blocks for reports
│   ├── ShiftReportLedgerTabs.kt   # Multi-tab shift details & expense breakdown
│   ├── ShiftReportModels.kt       # Data structures specific to reporting state
│   └── ShiftReportScreen.kt       # Active shift creation, cash counting & history list
│
├── scanner/                       # 📄 Document Scanner & OCR Module
│   ├── CamScannerCropView.kt      # Custom canvas view for perspective cropping handle adjustments
│   ├── CamScannerEditorScreen.kt  # Manual edge alignment and cropping screen
│   ├── CameraXScannerView.kt      # Edge-detecting custom camera scanner view
│   ├── DocumentImageFilter.kt     # Image enhancement filters (B&W, high contrast, magic color)
│   ├── DocumentScannerComponents.kt# Scan header card, document card & empty states
│   ├── DocumentScannerDialogs.kt  # Save, rename, batch delete, OCR & password dialogs
│   ├── DocumentScannerScreen.kt   # Main scanner screen with Activity context bridge
│   ├── DocumentScannerViewModel.kt# Document persistence, Word export & PDF encryption
│   ├── DocxExporter.kt            # Microsoft Word (.docx) document generator
│   ├── EdgeDetector.kt            # On-device canvas-based edge detection logic
│   ├── OcrImagePreprocessor.kt    # Grayscale, binarization and threshold helper for OCR
│   ├── OcrTextExtractor.kt        # Google ML Kit on-device text recognition
│   ├── PdfProtector.kt            # PDF password protection and encryption
│   ├── PerspectiveTransformHelper.kt# Mathematical perspective correction using OpenCV/native matrices
│   └── ScannedDocument.kt         # Room entity & DAO for scanned documents
│
├── settings/                      # ⚙️ Settings & Configuration Module
│   ├── AddCashierDialog.kt        # Cashier entry and creation dialog with authorization
│   ├── AddEditAddressScreen.kt    # Screen to add or update business addresses
│   ├── BusinessLogoPickerCard.kt  # Upload, crop, and store logo image
│   ├── CashierManagementSection.kt# Cashier lists, role controls, and active cashier tables
│   ├── DataPreservationDialogs.kt # Setup and recovery dialogs for data preservation on uninstall
│   ├── GoogleMapsLocationPickerModal.kt # Interactive maps for location coordinates setup
│   ├── LanguageSettingsComponent.kt# Locale selector displaying native languages
│   ├── ProfileScreen.kt           # Store profile edit, password reset, and address lists
│   ├── RegionalPreferencesSection.kt# Currency and regional formatting selection
│   ├── SavedAddressesScreen.kt    # List display of multiple physical store branches
│   ├── SettingsCommonDialogs.kt   # Diagnostic, backup, and restore helper modals
│   ├── SettingsReportSection.kt   # Business profile, currency, country, tax & backups
│   └── SettingsScreen.kt          # Host view managing settings layout routing
│
├── sync/                          # 🔄 Cloud Data Synchronization
│   ├── FirebaseCloudSyncManager.kt# Service manager handling scheduled sync actions
│   ├── FirebaseCloudSyncScheduler.kt# Schedules background sync tasks
│   └── FirebaseCloudSyncWorker.kt # Standard Worker carrying out remote synchronization
│
├── ui/                            # 🎨 Shared UI, Theme & Design System
│   ├── common/
│   │   ├── AppDrawer.kt           # Side navigation drawer
│   │   ├── Components.kt          # Shared cards, badges, statistics chips & headers
│   │   ├── LojiaComponents.kt     # Standard list, card, and action container shapes
│   │   ├── LojiaDialog.kt         # Standardized, scrollable and responsive dialog layouts
│   │   ├── LojiaTextField.kt      # Standardized input fields with validation states
│   │   ├── OtpInputField.kt       # Dynamic passcode or numeric entry fields
│   │   ├── PdfPreviewDialog.kt    # In-app PDF renderer & interactive action dialog
│   │   └── PhoneNumberHint.kt     # Automated phone code detection dropdown
│   └── theme/
│       ├── Color.kt               # Material 3 color system (Primary Blue, Slate, Emerald)
│       ├── Dimens.kt              # App density, padding, spacing & standard sizes
│       ├── Theme.kt               # Dynamic color schemes & typography bindings
│       └── Type.kt                # Poppins & system typography definitions
│
└── util/                          # 🛠️ Utility Functions & Helpers
    ├── AppLanguageManager.kt      # Internal language setup and configuration helper
    ├── CountryDetector.kt         # System configuration-based country detector
    ├── CurrencyUtils.kt           # Formatting patterns and symbols per currency
    ├── DateTimeFormatUtils.kt     # Locale-aware date and time formatting
    ├── ExportHelper.kt            # Saves files to local scoped folders
    ├── GoogleDriveManager.kt      # Handles backup transfers to remote cloud drive
    ├── LanguagePreferences.kt     # Key-value storage for selected interface language
    ├── LocaleManager.kt           # Runtime language switching & RTL direction support
    ├── MoneyFormat.kt             # Safe financial calculations using minor units (Long)
    ├── NotificationHelper.kt      # Builds app system notifications
    ├── OfflineBackupManager.kt    # Encryption-safe database JSON backup and recovery
    ├── PdfPalette.kt              # Standard color palette for PDF report visual themes
    ├── PdfReportGenerator.kt      # Vector-based PDF receipt & shift summary builder
    ├── PdfShareUtils.kt           # Native Android document sharing & opening utility
    ├── SecurityUtils.kt           # PBKDF2/SHA-256 secret hashing & cryptographic helpers
    ├── ShiftReportSyncScheduler.kt# Triggers local notification or periodic sync workers
    ├── ShiftReportSyncWorker.kt   # Background worker for offline-safe tasks
    └── UiText.kt                  # UI text wrapper supporting translatable strings
```

---

## 2. Key Architecture Principles

1. **Strict Offline-First**: Local Room Database is the single source of truth for all shift records, financial numbers, and scanned documents. No network latency blocks user operations.
2. **Feature Isolation**: Shift reporting and Document scanning operate with independent DAOs and ViewModels to prevent cross-module coupling.
3. **Robust Intent & Context Management**: Document scanning and PDF viewing utilize guaranteed Activity context references with proper Intent flags to prevent crashes.
4. **Multilingual Architecture**: Dynamic runtime language changing between English, Bengali, and Arabic without app restart, preserving full RTL layout hierarchy.
