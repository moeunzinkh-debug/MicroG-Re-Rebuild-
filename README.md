# microG Services

> **កំណត់សម្គាល់ជាភាសាខ្មែរ៖** កំណែ និងព័ត៌មានអំពីការព្រមានពេលដំឡើងមាននៅ [README.km.md](README.km.md)។ Clone នេះប្រើ `7.3.0-dev.2` (pre-release) មិនមែន stable `7.3.0` ទេ។


# ⚙️ MicroG RE (GmsCore Redesign)

microG Services is a FLOSS (Free/Libre Open Source Software) framework to allow applications designed for Google Play Services to run on systems, where Play Services is not available.

### Download variants

Six variants are offered:

- `microg-version.apk` — Universal
- `microg-version-noicon.apk` — Universal, icon hidden
- `microg-version-icon-arm64-v8a.apk` — ARM64
- `microg-version-icon-armeabi-v7a.apk` — ARMv7 (32-bit)
- `microg-version-noicon-arm64-v8a.apk` — ARM64, icon hidden
- `microg-version-noicon-armeabi-v7a.apk` — ARMv7 (32-bit), icon hidden

`-noicon` is identical in features and behavior except the MicroG icon is hidden in the device launcher.
The app can be opened from the app info found in the device system app list.

`arm64-v8a` contains only ARM64 native libraries. The app is less than half the size of the regular release.
Most modern phones can use this release.

`armeabi-v7a` contains only 32-bit ARM native libraries and is for devices using a 32-bit Android userspace, including some devices with ARM64-capable CPUs.

If you are unsure which variant to use, download the universal APK (`microg-version.apk`).

## Translations

If you'd like to help translate MicroG please see [here](https://morphe.software/translate).


License
-------
    Copyright 2013-2025 microG Project Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
