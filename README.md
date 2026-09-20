# Lojia System - Business Management App 🛒📊

*(Scroll down for the Bengali version / বাংলা সংস্করণের জন্য নিচে স্ক্রোল করুন)*

**Lojia System** is a robust, modern Android Point-of-Sale (POS) and business management application built with Google AI Studio. It is designed to streamline daily retail operations, sales tracking, shift management, and business reporting—all within a seamless offline-first mobile experience.

---

## 🌟 Features

*   **Point of Sale (POS):** Fast, intuitive checkout process with barcode scanning, custom modifiers, and discount support.
*   **Shift Management:** Open and close shifts, track starting cash, expected cash drawer, actual cash counts, and record notes/variances.
*   **Employee & Role Management:** Role-based access control (Admin vs. Cashier). Cashiers handle daily operations while Admins control voids, settings, and sensitive reports through PIN authentication.
*   **Comprehensive Reports:** View daily, weekly, and monthly sales analytics, shift ledgers, net cash flow, tax (VAT) summaries, and generate PDF receipts.
*   **Multi-language Support (Internationalization):** Dynamically switch between multiple languages and regional formats (currencies, locales).
*   **Offline-First:** Fully functional without an internet connection, ensuring business continuity.

## 🛠️ Tech Stack

*   **Platform:** Android
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material Design 3)
*   **Database:** Room Database (SQLite) (Version 2.6.1) for robust local persistence
*   **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines & Flow
*   **Build System:** Gradle (Kotlin DSL)

## 🚀 Installation Instructions

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/bdparvaze/Lojia-system.git
    ```
2.  **Open the Project:**
    Launch **Android Studio** and select `File > Open`, then choose the cloned `lojia-system` directory.
3.  **Sync Project:**
    Allow Android Studio to sync the Gradle files and download required dependencies.
4.  **Run the App:**
    Connect an Android device or start an emulator, then click the **Run** button (Shift + F10) in Android Studio.

## 📂 Project Structure

The codebase is modularized by feature for maintainability:

*   `app/src/main/java/com/lojia/pos/pos/` - Point of Sale, Cart, Inventory, and Checkout UI.
*   `app/src/main/java/com/lojia/pos/report/` - Shift management, Dashboards, Ledgers, and Analytics.
*   `app/src/main/java/com/lojia/pos/settings/` - Store configuration, Roles, Taxes, Printers, and Languages.
*   `app/src/main/java/com/lojia/pos/auth/` - Biometric lock, Admin PIN authentication, and role authorization.
*   `app/src/main/java/com/lojia/pos/data/` - Room Database configurations, DAOs, and Data Models.
*   `app/src/main/java/com/lojia/pos/util/` - Helpers (e.g., PDF generation, Currency formatting).

## 🤝 How to Contribute

Contributions make the open-source community an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

# লোজিয়া সিস্টেম (Lojia System) - বিজনেস ম্যানেজমেন্ট অ্যাপ 🛒📊

**লোজিয়া সিস্টেম** হলো গুগল এআই স্টুডিও (Google AI Studio) দ্বারা তৈরি একটি আধুনিক এবং শক্তিশালী অ্যান্ড্রয়েড পয়েন্ট-অফ-সেল (POS) এবং ব্যবসা পরিচালনা অ্যাপ্লিকেশন। এটি প্রতিদিনের কেনাবেচা, সেলস ট্র্যাকিং, শিফট ম্যানেজমেন্ট এবং রিপোর্টিংয়ের কাজগুলোকে একটি চমৎকার অফলাইন-ফার্স্ট মোবাইল অভিজ্ঞতার মাধ্যমে সহজ করে তোলে।

---

## 🌟 বৈশিষ্ট্যসমূহ (Features)

*   **পয়েন্ট অফ সেল (POS):** বারকোড স্ক্যানিং, কাস্টম মডিফায়ার এবং ডিসকাউন্ট সাপোর্ট সহ দ্রুত এবং সহজ চেকআউট প্রক্রিয়া।
*   **শিফট ম্যানেজমেন্ট (Shifts):** শিফট খোলা এবং বন্ধ করা, প্রারম্ভিক ক্যাশ, প্রত্যাশিত ক্যাশ ড্রয়ার, প্রকৃত ক্যাশ গণনা এবং নোট বা গরমিল রেকর্ড করা।
*   **এমপ্লয়ি এবং রোল ম্যানেজমেন্ট (Employee Management):** রোল-ভিত্তিক অ্যাক্সেস কন্ট্রোল (অ্যাডমিন এবং ক্যাশিয়ার)। ক্যাশিয়াররা প্রতিদিনের কাজ পরিচালনা করে, অন্যদিকে অ্যাডমিনরা পিন (PIN) ভেরিফিকেশনের মাধ্যমে ভয়েড, সেটিংস এবং গুরুত্বপূর্ণ রিপোর্ট নিয়ন্ত্রণ করে।
*   **বিস্তারিত রিপোর্ট এবং অ্যানালিটিক্স (Reports):** দৈনিক, সাপ্তাহিক এবং মাসিক সেলস অ্যানালিটিক্স, শিফট লেজার, নিট ক্যাশ ফ্লো, ট্যাক্স (VAT) সামারি দেখা এবং পিডিএফ (PDF) রসিদ তৈরি করা।
*   **একাধিক ভাষা সমর্থন (Multi-language):** সহজেই বিভিন্ন ভাষা এবং আঞ্চলিক ফরম্যাটের (কারেন্সি, লোকাল) মাঝে পরিবর্তন করার সুবিধা।
*   **অফলাইন-ফার্স্ট:** ইন্টারনেট সংযোগ ছাড়াই সম্পূর্ণ রূপে কাজ করতে সক্ষম, যা নিরবচ্ছিন্ন ব্যবসা নিশ্চিত করে।

## 🛠️ প্রযুক্তি স্ট্যাক (Tech Stack)

*   **প্ল্যাটফর্ম:** অ্যান্ড্রয়েড (Android)
*   **প্রোগ্রামিং ভাষা:** কোটলিন (Kotlin)
*   **ইউআই ফ্রেমওয়ার্ক:** জেটপ্যাক কম্পোজ (Jetpack Compose - Material 3)
*   **ডেটাবেস:** রুম ডেটাবেস (Room Database - SQLite) (Version 2.6.1)
*   **আর্কিটেকচার:** MVVM (Model-View-ViewModel)
*   **বিল্ড সিস্টেম:** গ্রেডল (Gradle)

## 🚀 ইন্সটলেশন গাইড (Installation Instructions)

১. **রিপোজিটরি ক্লোন করুন:**
    ```bash
    git clone https://github.com/bdparvaze/Lojia-system.git
    ```
২. **প্রজেক্ট খুলুন:**
    **Android Studio** চালু করুন, `File > Open` নির্বাচন করুন এবং ক্লোন করা `lojia-system` ফোল্ডারটি সিলেক্ট করুন।
৩. **প্রজেক্ট সিঙ্ক করুন:**
    অ্যান্ড্রয়েড স্টুডিওকে গ্রেডল ফাইল সিঙ্ক এবং প্রয়োজনীয় ডিপেন্ডেন্সি ডাউনলোড করার অনুমতি দিন।
৪. **অ্যাপ রান করুন:**
    একটি অ্যান্ড্রয়েড ডিভাইস কানেক্ট করুন অথবা এমুলেটর চালু করুন, তারপর অ্যান্ড্রয়েড স্টুডিওতে **Run** বাটনে (Shift + F10) ক্লিক করুন।

## 📂 প্রজেক্ট স্ট্রাকচার (Project Structure)

কোডবেসটি রক্ষণাবেক্ষণের সুবিধার জন্য বিভিন্ন ফিচারে ভাগ করা হয়েছে:

*   `app/src/main/java/com/lojia/pos/pos/` - কেনাবেচা (POS), কার্ট, ইনভেন্টরি এবং চেকআউট UI।
*   `app/src/main/java/com/lojia/pos/report/` - শিফট ম্যানেজমেন্ট, ড্যাশবোর্ড, লেজার এবং অ্যানালিটিক্স।
*   `app/src/main/java/com/lojia/pos/settings/` - স্টোর কনফিগারেশন, রোলস, ট্যাক্স, প্রিন্টার এবং ভাষা।
*   `app/src/main/java/com/lojia/pos/auth/` - বায়োমেট্রিক লক, অ্যাডমিন পিন এবং রোল ভেরিফিকেশন।
*   `app/src/main/java/com/lojia/pos/data/` - রুম ডেটাবেস কনফিগারেশন, DAO এবং ডেটা মডেল।
*   `app/src/main/java/com/lojia/pos/util/` - হেল্পার ফাংশন (যেমন: PDF তৈরি, কারেন্সি ফরমেটিং)।

## 🤝 কীভাবে কন্ট্রিবিউট করবেন (How to Contribute)

ওপেন সোর্স কমিউনিটিকে আরও সমৃদ্ধ করতে আপনার যেকোনো অবদান সাদরে গৃহীত হবে।

১. প্রজেক্টটি Fork করুন
২. আপনার ফিচার ব্রাঞ্চ তৈরি করুন (`git checkout -b feature/AmazingFeature`)
৩. আপনার পরিবর্তনগুলো Commit করুন (`git commit -m 'Add some AmazingFeature'`)
৪. ব্রাঞ্চে Push করুন (`git push origin feature/AmazingFeature`)
৫. একটি Pull Request ওপেন করুন

## 📄 লাইসেন্স (License)

এটি MIT লাইসেন্সের অধীনে ডিস্ট্রিবিউট করা হয়েছে। বিস্তারিত জানতে `LICENSE` ফাইলটি দেখুন।
