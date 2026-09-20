# Lojia System - প্রজেক্ট সেটআপ গাইড 🛠️

এই গাইডটি ডেভেলপারদের **Lojia System** অ্যান্ড্রয়েড প্রজেক্টটি লোকাল মেশিনে সেটআপ করতে, বিল্ড করতে এবং রান করতে সাহায্য করবে।

---

## ১. প্রয়োজনীয়তা (Prerequisites)

প্রজেক্টটি সফলভাবে রান করার জন্য আপনার সিস্টেমে নিম্নলিখিত টুলসগুলো ইনস্টল থাকতে হবে:

*   **JDK (Java Development Kit):** JDK 17 রেকমেন্ডেড (আধুনিক অ্যান্ড্রয়েড এবং গ্রেডল এর জন্য)।
*   **Android Studio:** Android Studio Koala, Jellyfish বা এর নতুন কোনো ভার্সন।
*   **Gradle:** প্রজেক্টে Gradle Wrapper দেওয়া আছে (আলাদা করে ইনস্টল করার দরকার নেই)।
*   **Git:** সোর্স কোড ক্লোন করার জন্য।

---

## ২. পরিবেশ সেটআপ (Environment Setup)

### `JAVA_HOME` সেটআপ:
আপনার সিস্টেমে `JAVA_HOME` ভেরিয়েবলটি JDK 17 এর ডিরেক্টরিতে পয়েন্ট করা থাকতে হবে।
*   **Windows:** `Environment Variables` থেকে `JAVA_HOME` সেট করুন।
*   **macOS/Linux:** আপনার `.bashrc` বা `.zshrc` ফাইলে নিচের লাইনটি যোগ করুন:
    ```bash
    export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
    ```

### Android SDK সেটআপ:
Android Studio ইনস্টল করার সময় Android SDK স্বয়ংক্রিয়ভাবে ইনস্টল হয়। আপনার `ANDROID_HOME` সেট করা আছে কিনা নিশ্চিত করুন:
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

---

## ৩. প্রজেক্ট ক্লোন করা (Cloning the Project)

টার্মিনাল বা কমান্ড প্রম্পট খুলুন এবং নিচের কমান্ডটি রান করুন:

```bash
git clone https://github.com/bdparvaze/Lojia-system.git
cd lojia-system
```

---

## ৪. Gradle Sync করা (Syncing Gradle)

১. **Android Studio** খুলুন।
২. `File > Open` এ গিয়ে ক্লোন করা `lojia-system` ফোল্ডারটি নির্বাচন করুন।
৩. Android Studio স্বয়ংক্রিয়ভাবে প্রজেক্ট ইনডেক্সিং এবং **Gradle Sync** শুরু করবে।
৪. যদি স্বয়ংক্রিয়ভাবে শুরু না হয়, তবে উপরের ডান দিকের কোণায় থাকা **"Sync Project with Gradle Files"** (হাতির আইকন 🐘) এ ক্লিক করুন।

> 📸 *[এখানে Gradle Sync আইকনের স্ক্রিনশট যুক্ত করুন]*

---

## ৫. বিল্ড করা (Building the App)

টার্মিনাল থেকে বা Android Studio-এর মাধ্যমে আপনি অ্যাপ বিল্ড করতে পারেন।

**Debug Build তৈরি করতে:**
```bash
./gradlew assembleDebug
```
*এটি `app/build/outputs/apk/debug/` ফোল্ডারে একটি `.apk` তৈরি করবে।*

**Release Build তৈরি করতে:**
```bash
./gradlew assembleRelease
```
*দ্রষ্টব্য: রিলিজ বিল্ডের জন্য `build.gradle.kts` এ সঠিক signingConfig কনফিগার করা থাকতে হবে।*

---

## ৬. Emulator বা Device সেটআপ (Emulator/Device Setup)

**Physical Device (আসল ফোন):**
১. আপনার ফোনের Settings থেকে `Developer Options` চালু করুন।
২. `USB Debugging` অন করুন।
৩. ডেটা ক্যাবল দিয়ে ফোনটি কম্পিউটারের সাথে যুক্ত করুন।

**Emulator (ভার্চুয়াল ডিভাইস):**
১. Android Studio থেকে `Device Manager` এ যান।
২. `Create Device` এ ক্লিক করে একটি নতুন এমুলেটর (যেমন: Pixel 7, API 34) তৈরি এবং চালু করুন।

---

## ৭. প্রথম রান (First Run)

১. Android Studio-এর উপরের টুলবার থেকে আপনার কানেক্টেড ডিভাইস বা এমুলেটরটি সিলেক্ট করুন।
২. **Run** বাটনে (▶️ প্লে আইকন) ক্লিক করুন অথবা কীবোর্ডে `Shift + F10` চাপুন।
৩. গ্রেডল বিল্ড শেষ হওয়ার পর অ্যাপটি আপনার ডিভাইসে ইন্সটল এবং চালু হবে।

> 📸 *[এখানে Android Studio Run বাটন এবং Device Dropdown এর স্ক্রিনশট যুক্ত করুন]*

---

## ৮. ট্রাবলশুটিং (Troubleshooting Common Issues)

*   **সমস্যা: "Unresolved reference" বা গ্রেডল সিঙ্ক এরর।**
    *   **সমাধান:** `Build > Clean Project` করুন। এরপর `Build > Rebuild Project` এ ক্লিক করুন। টার্মিনাল থেকে করতে চাইলে:
        ```bash
        ./gradlew clean
        ```
*   **সমস্যা: ক্যাশ বা ইনডেক্সিং সমস্যা।**
    *   **সমাধান:** `File > Invalidate Caches / Restart` এ ক্লিক করুন এবং "Invalidate and Restart" নির্বাচন করুন।
*   **সমস্যা: JDK ভার্সন অমিল।**
    *   **সমাধান:** Android Studio এর `Settings (Preferences) > Build, Execution, Deployment > Build Tools > Gradle` এ গিয়ে **Gradle JDK** হিসেবে `jbr-17` বা আপনার ইনস্টল করা `JDK 17` সিলেক্ট করুন।

---

*Happy Coding! 🎉*
