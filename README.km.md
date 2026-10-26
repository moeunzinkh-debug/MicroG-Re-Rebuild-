# MicroG-RE `7.3.0-dev.2` & Original microG Huawei (`-hw`) — ការ Build APK (Debug & Release) និងការការពារបញ្ហា Malware (ភាសាខ្មែរ)

Repository នេះបង្កើត APK ចំនួន **២ ប្រព័ន្ធ (២ Editions)** ទាំង **Release APK** និង **Debug APK** ដោយស្វ័យប្រវត្តិ និងគ្មានបញ្ហាព្រមាន Malware ពេលដំឡើង៖

1. **Original microG — Huawei Edition (`-hw`)** (`com.google.android.gms` + `com.android.vending` Companion)
   - សម្រាប់ទូរស័ព្ទ **Huawei គ្រប់ម៉ូដែលទាំងអស់** រួមទាំង **Huawei Nova 4e (`MAR-AL00`, Kirin 710, HarmonyOS 3.0.0)**, Nova 2/3/5/7/8/9/10/11/12/13, P20/P30/P40/P50/P60/Pura 70/80, Mate 10/20/30/40/50/60/70, MatePad និង Honor (ទាំង **HarmonyOS 2.0 / 3.0.0 / 3.1 / 4.0 / 4.2 / 4.3** និង **EMUI 8–15** ពី Android 5.0 API 21 ឡើងទៅ)។
   - រួមបញ្ចូល `com.huawei.signature.diff.SignatureService` សម្រាប់ Huawei Signature Spoofing, ផែនទី Huawei HMS Maps + MapLibre/VTM, ប៊ូតុង **"Huawei"** ពេលចូលគណនី Google និងការកែបញ្ហាចូលគណនី Aurora Store / VPN។
2. **MicroG-RE `7.3.0-dev.2`** (`app.revanced.android.gms`)
   - សម្រាប់កម្មវិធីដែលបាន Patch រួច (YouTube ReVanced / Morphe, YT Music, Reddit, TikTok, Google Photos ។ល។) លើទូរស័ព្ទគ្រប់ម៉ាក រួមទាំង Huawei Nova 4e (`MAR-AL00`) ផងដែរ។
   - **អាចដំឡើងទន្ទឹមគ្នា (Side-by-Side)** ជាមួយ Original microG Huawei នៅលើទូរស័ព្ទ Huawei តែមួយបាន 100% ដោយមិនជាន់ឈ្មោះកញ្ចប់កម្មវិធី (`Package Name`) គ្នាឡើយ!

---

## ១. តារាងប្រភេទ APK សម្រាប់ទូរស័ព្ទ Huawei (Nova 4e `MAR-AL00` HarmonyOS 3.0.0 និងគ្រប់ម៉ូដែល)

សម្រាប់ទូរស័ព្ទ **Huawei** អ្នកគួរដំឡើង APK ទាំង **២ ឯកសារជាគូ** (ព្រោះវាត្រូវបានចុះហត្ថលេខាដោយសោ RSA 4096-bit ដូចគ្នាបេះបិទ ដើម្បីចែករំលែកគណនី Google តាមសិទ្ធិ `GOOGLE_ACCOUNT_CHANGE`)៖

| ល.រ | ឯកសារ APK (Huawei `-hw` Edition) | Package Name | ការប្រើប្រាស់ដែលណែនាំ |
|---|---|---|---|
| **១ក** | `com.google.android.gms-255070302-hw-arm64-v8a.apk` | `com.google.android.gms` | **ណែនាំសម្រាប់ Huawei Nova 4e (`MAR-AL00` Kirin 710 64-bit) និងទូរស័ព្ទ Huawei 64-bit គ្រប់ម៉ូដែល** — ទំហំតូច (~48 MB) និងលឿនបំផុត |
| **១ខ** | `com.google.android.gms-255070302-hw.apk` | `com.google.android.gms` | **Universal Huawei Release** (`arm64-v8a` + `armeabi-v7a` + `x86` + `x86_64`) — ដំណើរការលើ Huawei គ្រប់ម៉ូដែលទាំង 32-bit និង 64-bit 100% |
| **១គ** | `com.google.android.gms-255070302-hw-armeabi-v7a.apk` | `com.google.android.gms` | សម្រាប់ទូរស័ព្ទ Huawei ស៊េរីចាស់ ឬ ROM 32-bit (Enjoy / Y-series ជំនាន់មុន) |
| **២** | `com.android.vending-84507030-hw.apk` | `com.android.vending` | **microG Companion / FakeStore (Huawei Release)** — ដំឡើងគូជាមួយ `com.google.android.gms` សម្រាប់ផ្ទៀងផ្ទាត់អាជ្ញាប័ណ្ណកម្មវិធី/ហ្គេម, In-App Billing និងតំណ `market://` |
| **Debug** | `com.google.android.gms-255070302-hw-debug*.apk` + `com.android.vending-84507030-hw-debug.apk` | ដូចខាងលើ (`debuggable=true`) | សម្រាប់អ្នកអភិវឌ្ឍន៍ (Developer) ត្រួតពិនិត្យ `adb logcat` ប៉ុណ្ណោះ |

---

## ២. របៀបដំឡើង និងកំណត់លើ Huawei Nova 4e (`MAR-AL00`, HarmonyOS 3.0.0) និង Huawei ផ្សេងៗ

### ជំហានទី ១៖ បិទ Pure Mode ("纯净模式") បណ្ដោះអាសន្នពេលដំឡើង APK (សម្រាប់ម៉ូដែលចិន `MAR-AL00`)
នៅលើទូរស័ព្ទ Huawei ម៉ូដែលចិន (`MAR-AL00` Nova 4e ដែលប្រើ **HarmonyOS 3.0.0**) ប្រព័ន្ធមានមុខងារ **Pure Mode (纯净模式)** ដែលរារាំងការដំឡើង APK ក្រៅពី Huawei AppGallery៖
1. ចូលទៅកាន់ **Settings (ការកំណត់ / 设置) → System & updates (ប្រព័ន្ធ និងការអាប់ដេត / 系统和更新) → Pure Mode (纯净模式)**។
2. បិទ **Enhanced Protection (增强防护)** ឬចុច **Exit (退出)** បណ្ដោះអាសន្ន។
3. ប្រសិនបើធ្លាប់ដំឡើង microG ឬ GBox/Gspace កំណែចាស់ពីមុន សូម **Uninstall `microG Services` និង `microG Companion / Google Play Store` ចាស់ចេញជាមុនសិន** (ដើម្បីជៀសវាងកំហុស `Signature mismatch / 签名不一致`)។

### ជំហានទី ២៖ ដំឡើង APK ទាំង ២ តាមលំដាប់
1. ដំឡើង **`com.google.android.gms-255070302-hw-arm64-v8a.apk`** (ឬកំណែ Universal `com.google.android.gms-255070302-hw.apk`)។
2. ដំឡើង **`com.android.vending-84507030-hw.apk`** (microG Companion)។
3. *(ជម្រើសបន្ថែម)* ប្រសិនបើអ្នកចង់ប្រើ **YouTube ReVanced / Morphe** ដោយគ្មានពាណិជ្ជកម្ម សូមដំឡើងបន្ថែម **`microg-7.3.0-dev.2-icon-arm64-v8a.apk`** (`MicroG RE`) — វាអាចរស់នៅជាមួយគ្នាលើ Nova 4e ដោយគ្មានបញ្ហាអ្វីទាំងអស់!

### ជំហានទី ៣៖ ចូលគណនី Google តាមប៊ូតុង **"Huawei"**
1. បើកកម្មវិធី **`microG Settings`** ពីអេក្រង់ដើម។
2. ចូលទៅ **`Self-Check`** → ចុចផ្ដល់សិទ្ធិ (Permissions) សំខាន់ៗដូចជា Location (`Allow all the time` / 始终允许), Storage, Phone និង Notifications។
3. ចូលទៅ **`Google Device Registration`** → ប្រាកដថាបើក **ON**។
4. ចូលទៅ **`Cloud Messaging`** → ប្រាកដថាបើក **ON** (សម្រាប់ទទួលសារជូនដំណឹង Push Notifications)។
5. ចូលទៅ **`Google Accounts` → `Add Google account`** → នៅលើអេក្រង់ដំបូង សូមចុចប៊ូតុង **`Huawei`** (នៅខាងឆ្វេងប៊ូតុង Sign in) ដើម្បីឱ្យ microG ក្លែងទម្រង់ឧបករណ៍សម្រាប់ Kirin 710 / HarmonyOS 3.0.0 ហើយចូលគណនី Gmail របស់អ្នកដោយរលូន!

### ជំហានទី ៤៖ បើកការដំណើរការផ្ទៃខាងក្រោយលើ HarmonyOS 3.0.0 / EMUI (សំខាន់សម្រាប់ Notification)
1. ចូលទៅ **Settings → Battery (ថ្ម / 电池) → App launch (ការបើកកម្មវិធី / 应用启动管理)**។
2. ស្វែងរក **`microG Services`** → បិទ *Manage automatically* ហើយបើកទាំង ៣៖
   - **Auto-launch (បើកស្វ័យប្រវត្តិ / 允许自启动)** = ON
   - **Secondary launch (អនុញ្ញាតឱ្យកម្មវិធីផ្សេងហៅ / 允许关联启动)** = ON
   - **Run in background (ដំណើរការផ្ទៃខាងក្រោយ / 允许后台活动)** = ON
3. ដំឡើង **Aurora Store** ដើម្បីទាញយកកម្មវិធី Android នានាជំនួស Play Store។

### ចំណាំបច្ចេកទេសសំខាន់អំពី HarmonyOS 3.0.0 (`MAR-AL00`) និង Signature Spoofing
- **កម្មវិធីទូទៅ និងការប្រើប្រាស់ប្រចាំថ្ងៃ** (Push Notifications លើ Telegram / WhatsApp / Messenger / Discord / កម្មវិធីធនាគារ, ផែនទី Location ក្នុងកម្មវិធីដឹកជញ្ជូនដូចជា Grab / Foodpanda / PassApp, ការចូលគណនី Google ក្នុង Aurora Store និងកម្មវិធីភាគីទីបីជាច្រើន)៖ **ដំណើរការពេញលេញ 100%** ជាមួយ APK នេះនៅលើ Nova 4e (`MAR-AL00`, HarmonyOS 3.0.0)!
- **ចំពោះកម្មវិធីផ្លូវការរបស់ក្រុមហ៊ុន Google ផ្ទាល់** (ដូចជា Gmail, Google Maps, Google Calendar, YouTube ផ្លូវការមិនទាន់ Patch)៖
  - ប្រព័ន្ធប្រតិបត្តិការ Huawei ចាប់ផ្ដើមគាំទ្រ Native OS Signature Spoofing (`com.huawei.signature.diff`) ចាប់ពី **HarmonyOS 3.1 / 4.0 / 4.2 និង EMUI 13.1 / 14+** (ហើយទទួលស្គាល់តែសោឯកជនផ្លូវការរបស់គម្រោង `microg/GmsCore` ប៉ុណ្ណោះ)។ នៅលើ **HarmonyOS 3.0.0** ដើម (មិនទាន់ Root) ប្រព័ន្ធ OS មិនបន្លំហត្ថលេខាឱ្យកម្មវិធីផ្លូវការរបស់ Google ទេ។
  - **ដំណោះស្រាយល្អបំផុតលើ Nova 4e (`MAR-AL00`, HarmonyOS 3.0.0)**៖ សម្រាប់ YouTube, YT Music និង Google Photos សូមប្រើកំណែ **Morphe / ReVanced** ជាមួយ **MicroG-RE (`microg-7.3.0-dev.2-icon-arm64-v8a.apk`)** រីឯសម្រាប់កម្មវិធីទូទៅ និង Notification ប្រើ **Original microG Huawei (`com.google.android.gms-255070302-hw-arm64-v8a.apk` + `com.android.vending-84507030-hw.apk`)** ទន្ទឹមគ្នា!

---

## ៣. តារាងប្រភេទ APK សម្រាប់ MicroG-RE `7.3.0-dev.2` (`app.revanced.android.gms`)

| ប្រភេទ APK | ឈ្មោះឯកសារ | ការប្រើប្រាស់ដែលណែនាំ |
|---|---|---|
| **Release (ARM64)** | `microg-7.3.0-dev.2-icon-arm64-v8a.apk` | សម្រាប់ទូរស័ព្ទ 64-bit (រួមទាំង Nova 4e `MAR-AL00`) ប្រើជាមួយ YouTube / YT Music ReVanced & Morphe |
| **Release (Universal)** | `microg-7.3.0-dev.2.apk` | សម្រាប់គ្រប់ឧបករណ៍ទាំងអស់ (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) |
| **Release (ARMv7 32-bit)** | `microg-7.3.0-dev.2-icon-armeabi-v7a.apk` | សម្រាប់ទូរស័ព្ទស៊េរីចាស់ ឬ ROM 32-bit |
| **Release (No-Icon)** | `microg-7.3.0-dev.2-noicon-arm64-v8a.apk` | លាក់រូប MicroG RE ពីអេក្រង់ដើម (Launcher) |
| **Debug** | `microg-7.3.0-dev.2-debug-icon-arm64-v8a.apk` | សម្រាប់អ្នកអភិវឌ្ឍន៍ត្រួតពិនិត្យ Logcat |

---

## ៤. វិធានការបច្ចេកទេសការពារបញ្ហាព្រមាន Malware ពេលដំឡើង (លើគ្រប់ម៉ូដែល Huawei និង Android)

ទាំង **Original microG Huawei (`com.google.android.gms` & `com.android.vending`)** និង **MicroG-RE (`app.revanced.android.gms`)** ត្រូវបានការពារដោយវិធានការ ៥ ជាន់ ដើម្បីកុំឱ្យ **Huawei Optimizer (Avast / Tencent AVL), HarmonyOS Security Guard, Oppo/Realme, Xiaomi, Samsung និង Google Play Protect** ចាប់ច្រឡំថាជាមេរោគ៖

1. **ដកសិទ្ធិ `SYSTEM_ALERT_WINDOW` និង `ReCaptchaOverlayService` ចេញទាំងស្រុង** — ជំនួសដោយ `ReCaptchaActivity` Dialog ធម្មតា មិនឱ្យ Antivirus (Avast / Tencent AVL លើ Huawei) ចាប់ថាជា Adware។
2. **ដកសិទ្ធិ `REQUEST_INSTALL_PACKAGES`, `INSTALL_PACKAGES`, `DELETE_PACKAGES` ចេញពីទាំង `play-services-core` និង `vending-app`** — ការពារមិនឱ្យប្រព័ន្ធស្កេនលើ HarmonyOS 3.0.0 / Pure Mode ចាត់ទុកថាជា *Trojan APK Dropper* (ការ Update បើកទាញយកតាម Browser ជំនួសវិញ)។
3. **ដកសិទ្ធិអានសារ SMS (`RECEIVE_SMS`, `READ_SMS`) និងចាក់សោដោយ `tools:node="remove"`** — ទប់ស្កាត់បណ្ណាល័យខាងក្រៅមិនឱ្យបញ្ចូលសិទ្ធិរសើបពេល Merge Manifest។
4. **ចុះហត្ថលេខាឌីជីថល RSA 4096-bit (`SHA256withRSA`) គ្រប់ទម្រង់ v1 + v2 + v3** — មិនប្រើសោតេស្តសាធារណៈ (`CN=Android Debug` ឬ AOSP `testkey`) ឡើយ ហើយចុះហត្ថលេខាដោយសោដូចគ្នាបេះបិទរវាង `com.google.android.gms` និង `com.android.vending`។
5. **ការផ្ទៀងផ្ទាត់ស្វ័យប្រវត្តិ ២ ជាន់ក្នុង CI** — ត្រួតពិនិត្យគ្រប់ `AndroidManifest.xml` ទាំង **100 ឯកសារ** (`scripts/check_sensitive_manifest_permissions.py`) និងពិនិត្យ APK ផ្ទាល់តាម `apksigner verify` + `aapt2 dump permissions` (`scripts/verify_built_apk.sh`)។

---

## ៥. ការបែងចែក Tag ដាច់ដោយឡែក (Separate Git Tags, Release Channels & Logcat Tags) សម្រាប់ងាយស្រួល Debug និង Fix

ដើម្បីឱ្យការត្រួតពិនិត្យកំហុស (Debug) និងការកែប្រែបញ្ហា (Fix) រវាង **MicroG-RE** និង **MicroG Original (Huawei)** ដាច់ស្រឡះពីគ្នា មិនច្រឡំគ្នា ប្រព័ន្ធត្រូវបានបែងចែក Tag ជា **៤ កម្រិត**៖

### ក. Git Tags & GitHub Releases ដាច់ដោយឡែកពីគ្នា
| ប្រភេទកម្មវិធី | ទម្រង់ Git Tag (Release Tag) | Workflow សម្រាប់ Build ដាច់ដោយឡែក | ឯកសារ APK ក្នុង Release |
|---|---|---|---|
| **MicroG-RE** (`app.revanced.android.gms`) | **`microg-re-v7.3.0-dev.2`** (`microg-re-v*`) | `.github/workflows/tag_microg_re.yml` | មានតែ `microg-7.3.0-dev.2*.apk` + `SHA256SUMS-microg-re.txt` |
| **MicroG Original Huawei** (`com.google.android.gms` + `com.android.vending`) | **`microg-original-hw-v0.3.17.255070`** (`microg-original-hw-v*`) | `.github/workflows/tag_microg_original_hw.yml` | មានតែ `com.google.android.gms-*-hw*.apk` + `com.android.vending-*-hw*.apk` + `SHA256SUMS-microg-original-hw.txt` |

- **AppUpdater ដាច់តាម Tag Channel**៖ `MicroG-RE` ពិនិត្យតែ Release Tag ដែលផ្ដើមដោយ `microg-re-v*` រីឯ `MicroG Original Huawei` ពិនិត្យតែ Release Tag ដែលផ្ដើមដោយ `microg-original-hw-v*` ប៉ុណ្ណោះ។

### ខ. Logcat Debug Tags ដាច់ដោយឡែក (ពេលដំឡើងទាំង ២ លើទូរស័ព្ទតែមួយ)
នៅពេលដំឡើងទាំង ២ នៅលើទូរស័ព្ទ Huawei តែមួយ (ឧ. Nova 4e `MAR-AL00`) អ្នកអាចចាប់យក Log ដាច់ដោយឡែកភ្លាមៗតាម `adb logcat`៖

```bash
# មើល Log តែខាង MicroG Original (Huawei) ប៉ុណ្ណោះ (Login, Auth, Spoof, HwSig, Updater)៖
adb logcat | grep "MicroG-Orig:"

# មើល Log តែសេវា Huawei Signature Spoofing (SignatureService / InitProvider / InitReceiver)៖
adb logcat -s MicroG-Orig:HwSig MicroG-Orig:HwInit MicroG-Orig:HwBoot

# មើល Log តែខាង MicroG-RE (ReVanced / Morphe) ប៉ុណ្ណោះ៖
adb logcat | grep "MicroG-RE:"
```

នៅក្នុងផ្ទាំង **Settings → About** ក៏បង្ហាញស្លាកសម្គាល់ច្បាស់លាស់ដែរ៖
- `7.3.0-dev.2 [MicroG-RE]`
- `0.3.17.255070-hw [MicroG-Original-HW]`

### គ. របៀបបញ្ចេញកំណែជួសជុល (Hotfix Tag) ដាច់ដោយឡែក
- កែបញ្ហាតែខាង **Huawei Original**៖ បង្កើត Tag ថ្មីឧ. `microg-original-hw-v0.3.17.255071` (ឬចុច Run workflow `Release` → ជ្រើស `edition = microg-original-hw`)។
- កែបញ្ហាតែខាង **MicroG-RE**៖ បង្កើត Tag ថ្មីឧ. `microg-re-v7.3.0-dev.3` (ឬចុច Run workflow `Release` → ជ្រើស `edition = microg-re`)។

---

## ៦. របៀប Build លើកុំព្យូទ័រផ្ទាល់ (JDK 17 + Android SDK 36)

```bash
# 1. បង្កើត Keystore RSA 4096-bit (ប្រើរួមគ្នាសម្រាប់ GMS និង Vending Companion)
bash scripts/generate_keystore.sh

# 2. ត្រួតពិនិត្យសិទ្ធិរសើបក្នុង Manifests ទាំង 100
python3 scripts/check_sensitive_manifest_permissions.py

# 3. Build Original microG Huawei (-hw) Release & Debug + Companion FakeStore (-hw)
./gradlew :play-services-core:assembleHuaweiRelease -PbasePackageName=com.google -Pabi=arm64-v8a
./gradlew :play-services-core:assembleHuaweiRelease -PbasePackageName=com.google
./gradlew :vending-app:assembleHuaweiRelease -PbasePackageName=com.google

./gradlew :play-services-core:assembleHuaweiDebug -PbasePackageName=com.google -Pabi=arm64-v8a
./gradlew :play-services-core:assembleHuaweiDebug -PbasePackageName=com.google
./gradlew :vending-app:assembleHuaweiDebug -PbasePackageName=com.google

# 4. Build MicroG-RE Release & Debug
./gradlew :play-services-core:assembleDefaultRelease -Pabi=arm64-v8a
./gradlew :play-services-core:assembleDefaultDebug -Pabi=arm64-v8a

# 5. ផ្ទៀងផ្ទាត់ហត្ថលេខា v1+v2+v3 និងសិទ្ធិក្នុង APK
bash scripts/verify_built_apk.sh \
  play-services-core/build/outputs/apk/*/*/*.apk \
  vending-app/build/outputs/apk/*/*/*.apk
```
