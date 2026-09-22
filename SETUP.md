# LojiaShiftReport - সেটআপ গাইড 🛠️

এই গাইডটি ডেভেলপারদের **LojiaShiftReport** অ্যান্ড্রয়েড প্রজেক্টটি লোকাল মেশিনে সেটআপ করতে, বিল্ড করতে এবং রান করতে সাহায্য করবে।

---

## ১. প্রয়োজনীয়তা (Prerequisites)

প্রজেক্টটি সফলভাবে রান করার জন্য সিস্টেমে নিম্নলিখিত টুলসগুলো থাকতে হবে:

*   **JDK (Java Development Kit):** JDK 17 রেকমেন্ডেড।
*   **Android Studio:** Android Studio Koala, Ladybug বা এর পরবর্তী সংস্করণ।
*   **Gradle:** প্রজেক্টে Gradle Wrapper কনফিগার করা আছে।
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
Android Studio ইনস্টল করার সময় Android SDK স্বয়ংক্রিয়ভাবে কনফিগার হয়। আপনার `ANDROID_HOME` সেট করা আছে কিনা নিশ্চিত করুন:
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

---

## ৩. প্রজেক্ট ক্লোন করা (Cloning the Project)

টার্মিনাল বা কমান্ড প্রম্পটে নিচের কমান্ডটি চালান:

```bash
git clone https://github.com/bdparvaze/Lojia-system.git
cd Lojia-system
```

---

## ৪. Gradle Sync করা (Syncing Gradle)

১. **Android Studio** খুলুন।
২. `File > Open` এ গিয়ে প্রজেক্ট ফোল্ডারটি নির্বাচন করুন।
৩. Android Studio স্বয়ংক্রিয়ভাবে প্রজেক্ট ইনডেক্সিং এবং **Gradle Sync** সম্পন্ন করবে।

---

## ৫. বিল্ড করা (Building the App)

**Debug Build তৈরি করতে:**
```bash
./gradlew assembleDebug
```
*আউটপুট APK লোকেশন: `app/build/outputs/apk/debug/`*

**Release Build তৈরি করতে:**
```bash
./gradlew assembleRelease
```

---

## ৬. ডিভাইস বা এমুলেটরে রান করা

১. ডিভাইস বা এমুলেটর সিলেক্ট করুন।
২. **Run** বাটনে ক্লিক করুন অথবা কীবোর্ডে `Shift + F10` চাপুন।
