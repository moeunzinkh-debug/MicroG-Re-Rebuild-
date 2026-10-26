# MicroG-RE 7.3.0-dev.2 — ការ Build APK (Debug & Release) និងការការពារបញ្ហាព្រមាន Malware (ភាសាខ្មែរ)

## ១. ព័ត៌មានកំណែ (Version)

Repository នេះត្រូវបានរៀបចំ និង Build ចេញពីកំណែថ្មីបំផុត **`7.3.0-dev.2`** (Version Code: `255070302`, Commit ប្រភព៖ `ef356b5f44f376eadd301ce4b836b54a5a093445`) ពីទំព័រផ្លូវការ [MorpheApp/MicroG-RE Releases](https://github.com/MorpheApp/MicroG-RE/releases)។

កំណែ `7.3.0-dev.2` រួមបញ្ចូលទាំងការកែបញ្ហាទាំងអស់ពី `7.2.2` និងការកែលម្អបន្ថែម 21 commits (ដូចជា Google Cast sessions, Dark Mode សម្រាប់អេក្រង់ទាំងអស់, Material You dynamic colors លើ Android 12+, Google Prompt sign-in និងការកែលម្អ network timeouts)។

---

## ២. ប្រភេទ APK ដែលបាន Build (Debug និង Release)

ប្រព័ន្ធ Build បង្កើតទាំង **Release APK** និង **Debug APK** ដោយបែងចែកតាមស្ថាបត្យកម្ម CPU (ABI) និងរូបតំណាង (Icon)៖

| ប្រភេទ APK | ឈ្មោះឯកសារឧទាហរណ៍ | ការប្រើប្រាស់ដែលណែនាំ |
|---|---|---|
| **Release (ARM64)** | `microg-7.3.0-dev.2-icon-arm64-v8a.apk` | **ណែនាំសម្រាប់ទូរស័ព្ទទំនើបស្ទើរទាំងអស់ (64-bit)** — ទំហំតូចជាងពាក់កណ្តាល (~45 MB) ដំណើរការលឿន និងរលូន |
| **Release (Universal)** | `microg-7.3.0-dev.2.apk` | សម្រាប់គ្រប់ឧបករណ៍ទាំងអស់ (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) |
| **Release (ARMv7 32-bit)** | `microg-7.3.0-dev.2-icon-armeabi-v7a.apk` | សម្រាប់ទូរស័ព្ទស៊េរីចាស់ ឬ ROM 32-bit |
| **Release (No-Icon)** | `microg-7.3.0-dev.2-noicon-arm64-v8a.apk` | ដូចកំណែធម្មតា ប៉ុន្តែលាក់រូប MicroG ពីអេក្រង់ដើម (Launcher) |
| **Debug (ARM64 / Universal)** | `microg-7.3.0-dev.2-debug-icon-arm64-v8a.apk` | សម្រាប់អ្នកអភិវឌ្ឍន៍ (Developer) ត្រួតពិនិត្យ Logcat និងធ្វើតេស្តកំហុស (`android:debuggable="true"`) |

> **ចំណាំសំខាន់៖** សម្រាប់ការប្រើប្រាស់ប្រចាំថ្ងៃ សូមដំឡើង **Release APK** (`microg-7.3.0-dev.2-icon-arm64-v8a.apk` ឬ `microg-7.3.0-dev.2.apk`) ព្រោះ Debug APK បើកមុខងារ `debuggable="true"` ដែលអាចធ្វើឱ្យកម្មវិធីធនាគារ ឬប្រព័ន្ធសុវត្ថិភាពទូរស័ព្ទខ្លះដាស់តឿន។

---

## ៣. វិធានការបច្ចេកទេសការពារបញ្ហាព្រមាន Malware ពេលដំឡើង

មូលហេតុចម្បងដែលធ្វើឱ្យទូរស័ព្ទ (Oppo/Realme ColorOS, OnePlus OxygenOS, Xiaomi HyperOS, Samsung Auto Blocker និង Google Play Protect) ចាប់ច្រឡំ APK ថាជា Malware/Adware/Dropper ត្រូវបានដោះស្រាយដូចខាងក្រោម៖

1. **ដកសិទ្ធិ `SYSTEM_ALERT_WINDOW` និង `ReCaptchaOverlayService` ចេញទាំងស្រុង**
   - កំណែមុនៗត្រូវបាន Antivirus លើ Realme/Oppo/OnePlus (Avast & Tencent AVL) ចាប់ច្រឡំថាជា *"Intrusive ads / Risky adware"* ដោយសារការបង្ហាញផ្ទាំង WebView ជាន់លើកម្មវិធីផ្សេង។ ឥឡូវនេះប្រើ `ReCaptchaActivity` ជា Dialog ធម្មតាជំនួសវិញ។
2. **ដកសិទ្ធិ `REQUEST_INSTALL_PACKAGES` ចេញពី Manifest និងកែ `AppUpdater`**
   - APK ដែលចុះហត្ថលេខាដោយសោថ្មី (Custom Key) ហើយស្នើសុំសិទ្ធិ `REQUEST_INSTALL_PACKAGES` រួមជាមួយ `INTERNET` តែងត្រូវបានប្រព័ន្ធស្កេនចាត់ទុកថាជា *APK Dropper*។ យើងបានដកសិទ្ធិនេះចេញ ហើយឱ្យមុខងារ Update បើកទាញយកតាម Browser របស់ប្រព័ន្ធដោយសុវត្ថិភាពជំនួសវិញ (មិនប៉ះពាល់មុខងារគណនី Google, YouTube/Morphe, Notification ឬ Location ឡើយ)។
3. **ដកសិទ្ធិអានសារ SMS (`RECEIVE_SMS`, `READ_SMS`) និងចាក់សោដោយ `tools:node="remove"`**
   - ការពារមិនឱ្យបណ្ណាល័យខាងក្រៅ (AAR dependencies) បញ្ចូលសិទ្ធិរសើបទាំងនេះចូលទៅក្នុង APK ពេល Merge Manifest ឡើយ។
4. **ចុះហត្ថលេខាឌីជីថល (APK Signing) កម្រិត RSA 4096-bit (`SHA256withRSA`) គ្រប់ទម្រង់ v1 + v2 + v3**
   - ទាំង **Release APK** និង **Debug APK** មិនប្រើសោតេស្តសាធារណៈ (`CN=Android Debug` ឬ AOSP `testkey`) ដែល Antivirus ចាប់ថាមានហានិភ័យនោះទេ ហើយបើកពេញលេញទាំង **Signature Scheme v1, v2 និង v3** ស្របតាមស្តង់ដារ Android 11–16 (`targetSdk 34`)។
5. **ការផ្ទៀងផ្ទាត់ស្វ័យប្រវត្តិ 2 ជាន់ក្នុង CI**
   - ជាន់ទី 1៖ `scripts/check_sensitive_manifest_permissions.py` ពិនិត្យគ្រប់ `AndroidManifest.xml` ទាំង 98 ឯកសារ។
   - ជាន់ទី 2៖ `scripts/verify_built_apk.sh` ប្រើ `apksigner verify` និង `aapt2 dump permissions` ត្រួតពិនិត្យផ្ទាល់លើឯកសារ `.apk` ដែល Build រួច។

### កំណត់សម្គាល់សុវត្ថិភាពអំពី Google Play Protect
ដោយសារ APK ដែល Build ដោយខ្លួនឯងប្រើសោចុះហត្ថលេខាថ្មី (មិនមែនសោរបស់ក្រុមហ៊ុនធំដែលមានអ្នកដំឡើងរាប់សែននាក់រួចហើយ), នៅពេលដំឡើងលើកដំបូង Google Play Protect អាចបង្ហាញផ្ទាំង **"Unrecognized app developer — App scan recommended"**៖
- សូមចុច **"Scan app" (ស្កេនកម្មវិធី)** ដើម្បីឱ្យ Google Play Protect ពិនិត្យឃើញថាកម្មវិធីគ្មានកូដគ្រោះថ្នាក់ រួចចុចដំឡើងធម្មតា។
- **កុំបិទ Google Play Protect** ដើម្បីបង្ខំដំឡើង APK ដែលមិនស្គាល់ប្រភពឱ្យសោះ។
- ប្រសិនបើទូរស័ព្ទមានដំឡើង MicroG-RE ផ្លូវការពីមុន សូម **Uninstall កំណែចាស់ជាមុន** មុននឹងដំឡើង APK ដែល Build ដោយសោថ្មី (ព្រោះ Android មិនអនុញ្ញាតឱ្យដំឡើងជាន់លើកម្មវិធីដែលមាន Signature ខុសគ្នាទេ)។

---

## ៤. របៀប Build ដោយខ្លួនឯង (Local & GitHub Actions)

### ក. តាមរយៈ GitHub Actions (ងាយស្រួលបំផុត)
1. ចូលទៅកាន់ផ្ទាំង **Actions** ក្នុង GitHub Repository នេះ។
2. ជ្រើសរើស Workflow **`Gradle build`** (សម្រាប់ទាញយក Artifacts ទាំង Debug និង Release) ឬ **`Release`** (សម្រាប់បង្កើត GitHub Release ដោយស្វ័យប្រវត្តិ)។
3. ដើម្បីរក្សាសោចុះហត្ថលេខាដដែលសម្រាប់ការ Update លើកក្រោយៗ (មិនបាច់ Uninstall កំណែចាស់) សូមបង្កើត Keystore តាម `bash scripts/generate_keystore.sh` រួចបម្លែង `keystore/release.p12` ជា Base64 (`base64 -w0 keystore/release.p12`) ដាក់ក្នុង GitHub Repository Secrets៖
   - `KEYSTORE_B64`
   - `KEYSTORE_PASSWORD`
   - `KEYSTORE_ENTRY_ALIAS`
   - `KEYSTORE_ENTRY_PASSWORD`

### ខ. ការ Build លើកុំព្យូទ័រផ្ទាល់ (JDK 17 + Android SDK 36)
```bash
# 1. បង្កើត Keystore RSA 4096-bit (សម្រាប់ទាំង Release និង Debug)
bash scripts/generate_keystore.sh

# 2. ត្រួតពិនិត្យសិទ្ធិរសើបក្នុង Manifests
python3 scripts/check_sensitive_manifest_permissions.py

# 3. Build Release APK (ARM64 និង Universal)
./gradlew :play-services-core:assembleDefaultRelease -Pabi=arm64-v8a
./gradlew :play-services-core:assembleDefaultRelease

# 4. Build Debug APK (ARM64 និង Universal)
./gradlew :play-services-core:assembleDefaultDebug -Pabi=arm64-v8a
./gradlew :play-services-core:assembleDefaultDebug

# 5. ផ្ទៀងផ្ទាត់ហត្ថលេខា v1+v2+v3 និងសិទ្ធិក្នុង APK
bash scripts/verify_built_apk.sh play-services-core/build/outputs/apk/default/*/*.apk
```
