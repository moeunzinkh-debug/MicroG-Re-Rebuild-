# MicroG-RE — ព័ត៌មានជាភាសាខ្មែរ

## កំណែដែលបានដាក់ក្នុង repository នេះ

កំណែនេះយកប្រភពពី tag ផ្លូវការ [`7.3.0-dev.2`](https://github.com/MorpheApp/MicroG-RE/releases/tag/7.3.0-dev.2) នៃ [MorpheApp/MicroG-RE](https://github.com/MorpheApp/MicroG-RE)។ Commit ប្រភព៖ `ef356b5f44f376eadd301ce4b836b54a5a093445`។

**ចំណាំ៖** តាមទំព័រ Releases នៅថ្ងៃទី 26 តុលា 2026 កំណែ `7.3.0` ដែល stable មិនទាន់ចេញទេ។ `7.3.0-dev.2` គឺជា pre-release ចុងក្រោយក្នុងខ្សែ 7.3.0; កំណែ stable ចុងក្រោយបង្ហាញជា `7.2.2`។ ដូច្នេះកុំចាត់ទុក `7.3.0-dev.2` ថាជា stable ឬលុបពាក្យ `-dev.2` ចេញពី version។

Repository នេះមាន **source code** ប៉ុណ្ណោះ មិនបានដាក់ APK ឬ signing key របស់អ្នកបោះផ្សាយទេ។ APK ដែល build ដោយខ្លួនឯងអាចមាន signature ខុសពី APK ផ្លូវការ ហើយអាចមិនអាច update លើ installation ចាស់បាន។

## ការកាត់បន្ថយការព្រមាន malware ពេលដំឡើង

ការព្រមានរបស់ Play Protect ឬកម្មវិធីសុវត្ថិភាពរបស់ក្រុមហ៊ុនទូរស័ព្ទ មិនអាចបញ្ជាក់ដោយខ្លួនឯងថា APK មាន malware ឬថាគ្មាន malware បានទេ។ សញ្ញាព្រមានអាចកើតពីការវាយតម្លៃរបស់ Play Protect/OEM លើ APK និងសិទ្ធិដែលវាស្នើសុំ។ ក៏មិនមានការកែ source code ដែលអាចធានាថា Play Protect នឹងឈប់ព្រមានគ្រប់ឧបករណ៍ដែរ។

កំណែមុនៗមានករណីដែលការស្នើសុំសិទ្ធិអាន SMS ធ្វើឱ្យការដំឡើងត្រូវបានរារាំង។ ប្រភព 7.3.0-dev.2 មិនស្នើសុំសិទ្ធិ `RECEIVE_SMS`, `READ_SMS`, `SYSTEM_ALERT_WINDOW` ឬសេវា Accessibility/Notification Listener ទេ។ ខ្ញុំបានដកឃ្លាដែលទុកសេចក្ដីប្រកាសសិទ្ធិទាំងនេះជាកូដ comment ដើម្បីឱ្យ manifest មើលទៅច្បាស់ និងបន្ថែមការត្រួតពិនិត្យ regression នៅ `scripts/check_sensitive_manifest_permissions.py`។ CI រត់ការត្រួតពិនិត្យនេះ ដើម្បីជូនដំណឹងបើមានការបន្ថែមសិទ្ធិទាំងនេះវិញ។

ការត្រួតពិនិត្យនេះពិនិត្យ **Android source manifests** ប៉ុណ្ណោះ—វាមិនមែនជា malware scanner ឬការធានាសុវត្ថិភាពពេញលេញទេ។ ប្រសិនបើទូរស័ព្ទនៅតែព្រមាន សូមទុក Play Protect ឱ្យបើក និងកុំដំឡើង APK ដែលមិនអាចផ្ទៀងផ្ទាត់ប្រភពបាន។

### ផ្ទៀងផ្ទាត់ APK ផ្លូវការ

ទាញយកតែពីទំព័រ [GitHub Releases របស់ MorpheApp/MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases/tag/7.3.0-dev.2)។ សម្រាប់ APK universal `microg-7.3.0-dev.2.apk`, GitHub API បង្ហាញ SHA-256៖

```text
2812a3af27bff650e29e4ca046b9ce2973c03c15679c0940911519e5117aa741
```

ពិនិត្យ hash លើឯកសារដែលបានទាញយក៖

```bash
sha256sum microg-7.3.0-dev.2.apk
```

តម្លៃត្រូវតែដូចខាងលើ។ បើខុស ឬឧបករណ៍បង្ហាញការព្រមានដែលមិនអាចផ្ទៀងផ្ទាត់បាន សូមកុំបន្តដំឡើង; រាយការណ៍ទៅអ្នកបោះផ្សាយ ឬអ្នកផ្គត់ផ្គង់កម្មវិធីសុវត្ថិភាព។ កុំបិទការការពារសុវត្ថិភាពដើម្បីបង្ខំដំឡើង។

## រត់ការត្រួតពិនិត្យ manifest ដោយដៃ

```bash
python3 scripts/check_sensitive_manifest_permissions.py
```

> MicroG-RE ជា fork ដែលបង្កើតឡើងសម្រាប់កម្មវិធីដែលបាន patch ដោយ Morphe; វាមិនមែនជាកម្មវិធីផ្លូវការរបស់ Google ទេ។
