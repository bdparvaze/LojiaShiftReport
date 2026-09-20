# Lojia System - Architecture Guide 🏗️

এই গাইডটি Lojia System অ্যান্ড্রয়েড প্রজেক্টের অভ্যন্তরীণ আর্কিটেকচার, ফোল্ডার স্ট্রাকচার এবং কোডবেস কীভাবে সাজানো হয়েছে তা ব্যাখ্যা করে।

## ১. প্রজেক্ট স্ট্রাকচার (Folder Hierarchy)

প্রজেক্টটি ক্লিন আর্কিটেকচার (Clean Architecture) এবং ফিচার-ভিত্তিক (Feature-by-Feature) মডুলারিজেশন মেনে সাজানো হয়েছে। এর ফলে কোড সহজে খুঁজে পাওয়া যায় এবং রক্ষণাবেক্ষণ (Maintenance) করা সহজ হয়।

প্রধান ফোল্ডার স্ট্রাকচারটি নিচে দেওয়া হলো (`app/src/main/java/com/lojia/pos/` এর অধীনে):

```text
com.lojia.pos
│
├── MainActivity.kt                # অ্যাপের মূল এন্ট্রি পয়েন্ট এবং ন্যাভিগেশন হোস্ট
│
├── auth/                          # 🔐 অথেনটিকেশন এবং নিরাপত্তা
│   ├── AdminAuthDialog.kt         # অ্যাডমিন পিন ভেরিফিকেশন ডায়ালগ
│   ├── BiometricAuthManager.kt    # ফিঙ্গারপ্রিন্ট/ফেইস আনলক লজিক
│   ├── BiometricLockScreen.kt     # বায়োমেট্রিক স্ক্রিনের ইউআই
│   └── MpinScreen.kt              # পিন/পাসওয়ার্ড এন্ট্রি স্ক্রিন
│
├── data/                          # 💾 ডেটা লেয়ার (Local DB & Repositories)
│   ├── AppDatabase.kt             # Room Database এর মূল কনফিগারেশন
│   ├── Models.kt                  # অ্যাপের ডেটা ক্লাসসমূহ (Entities)
│   ├── POSDao.kt                  # POS সম্পর্কিত Database Queries
│   ├── ReportDao.kt               # রিপোর্ট এবং শিফট সম্পর্কিত Queries
│   └── ShiftReportRepository.kt   # UI এবং Database এর মধ্যে ডেটা আদান-প্রদানকারী লেয়ার
│
├── pos/                           # 🛒 পয়েন্ট অফ সেল (কোর ফিচার)
│   ├── PosScreen.kt               # মূল সেলস এবং কার্ট ইউআই (UI)
│   ├── PosViewModel.kt            # POS এর বিজনেস লজিক এবং স্টেট ম্যানেজমেন্ট (MVVM)
│   ├── InventoryScreen.kt         # প্রোডাক্ট এবং ইনভেন্টরি ম্যানেজমেন্ট
│   ├── CashManagementScreen.kt    # ক্যাশ-ইন / ক্যাশ-আউট স্ক্রিন
│   └── BarcodeScannerDialog.kt    # ক্যামেরা দিয়ে বারকোড স্ক্যানিং লজিক
│
├── report/                        # 📊 রিপোর্ট এবং অ্যানালিটিক্স
│   ├── DashboardScreen.kt         # দৈনিক এবং সাপ্তাহিক সেলস ড্যাশবোর্ড
│   ├── ShiftReportScreen.kt       # শিফট খোলা/বন্ধ করা এবং ক্যাশ মেলানোর ইউআই
│   ├── ShiftReportLedgerTabs.kt   # শিফটের বিস্তারিত হিসাবের ট্যাব
│   └── ReportViewModel.kt         # রিপোর্টিং মডিউলের বিজনেস লজিক
│
├── settings/                      # ⚙️ অ্যাপ সেটিংস এবং কনফিগারেশন
│   ├── SettingsShopSection.kt     # স্টোরের সাধারণ তথ্য এবং কারেন্সি সেটিংস
│   ├── CashierManagementSection.kt# ক্যাশিয়ার এবং অ্যাডমিন রোল ম্যানেজমেন্ট
│   └── SettingsReportSection.kt   # প্রিন্টার এবং লোকাল/ল্যাঙ্গুয়েজ সেটিংস
│
├── ui/                            # 🎨 শেয়ার্ড ইউজার ইন্টারফেস (Shared UI)
│   ├── common/
│   │   ├── LojiaTextField.kt      # গ্লোবাল কাস্টম ইনপুট ফিল্ড (Alignment fix সহ)
│   │   ├── AppDrawer.kt           # অ্যাপের সাইড ন্যাভিগেশন মেনু
│   │   └── PdfPreviewDialog.kt    # পিডিএফ রসিদ দেখানোর ডায়ালগ
│   └── theme/
│       ├── Color.kt, Theme.kt     # গ্লোবাল ম্যাটেরিয়াল কালার এবং থিম কনফিগারেশন
│       └── Type.kt                # ফন্ট এবং টাইপোগ্রাফি স্টাইল
│
└── util/                          # 🛠️ ইউটিলিটি এবং হেল্পার ফাংশন
    ├── PdfReportGenerator.kt      # ডায়নামিক পিডিএফ ইনভয়েস তৈরি করার কোড
    ├── LocaleManager.kt           # অ্যাপের ভাষা পরিবর্তনের লজিক
    ├── ExportHelper.kt            # ডেটা এক্সপোর্ট (CSV/Excel) করার কোড
    └── PrintHelper.kt             # ব্লুটুথ থার্মাল প্রিন্টারে প্রিন্ট করার কোড
```

## আর্কিটেকচারাল প্যাটার্ন (MVVM)
এই প্রজেক্টটি **MVVM (Model-View-ViewModel)** আর্কিটেকচার মেনে চলে:
*   **View (Jetpack Compose):** শুধুমাত্র ইউআই (UI) রেন্ডার করে এবং ইউজারের ইভেন্টগুলো (ক্লিক, টাইপিং) ViewModel এ পাঠায়। (`PosScreen`, `ShiftReportScreen`)
*   **ViewModel:** বিজনেস লজিক প্রসেস করে এবং UI এর জন্য State ধারণ করে (`MutableStateFlow`)। (`PosViewModel`, `ReportViewModel`)
*   **Model/Repository:** ডেটাবেস (Room) থেকে ডেটা নিয়ে আসে এবং সেভ করে। (`ShiftReportRepository`, `AppDatabase`)
