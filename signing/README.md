# Stable release signing identity (committed on purpose)

## Why this file is in the repository

Every GitHub Actions run used to generate a **brand new** 4096-bit RSA keystore, so each published
APK had a different signing certificate. Android answers that with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, which means the only way to move to a newer build was to
uninstall the old one first — losing accounts, settings and app data.

`microg-stable-release.p12` fixes that: the same certificate is used by every build, for every
edition, forever. Android then treats a newer APK as a normal **in-place update**.

| Item | Value |
|---|---|
| Keystore | `signing/microg-stable-release.p12` (PKCS#12, RSA 4096, SHA256withRSA, 10 000 days) |
| Alias | `microg-release` |
| Password | see `microg-stable-release.password` |
| Certificate SHA-256 | see `EXPECTED_CERT_SHA256.txt` (`867AB5C2…33EE`) |
| Subjects | `CN=MicroG Personal Self-Signed Release, OU=Personal Sideload Not For Play Store, O=Personal Use Only, L=Phnom Penh, ST=Phnom Penh, C=KH` |

CI refuses to build if the keystore's certificate no longer matches
`EXPECTED_CERT_SHA256.txt`, so the signing identity can never change silently.

## Security trade-off (read this)

The private key is public because it is committed. That is acceptable **only** because these APKs are
personal, self-signed, sideloaded builds that are explicitly **not** distributed through the Play
Store, F-Droid or any other store that trusts a signing identity:

- A signature protects against *updates* from someone else. Anyone who can convince you to install an
  APK manually can already do so with any key, so the practical risk added here is small.
- The key is **not** accepted by Google, Huawei or any store; it cannot be used to sign anything that
  those services would trust.
- If you ever want a private identity, set the repository secrets `KEYSTORE_B64`,
  `KEYSTORE_ENTRY_ALIAS`, `KEYSTORE_PASSWORD` (and optionally `KEYSTORE_ENTRY_PASSWORD`) to your own
  keystore — the workflows prefer those secrets over this file:

```bash
keytool -genkeypair -v -storetype PKCS12 -keystore my-release.p12 \
  -alias my-release -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 10000 \
  -dname "CN=My Name, O=Personal, C=KH"
base64 -w0 my-release.p12 > my-release.b64   # paste into the KEYSTORE_B64 secret
```

## One-time migration

Builds published **before** this stable keystore existed were signed with throw-away keys that no
longer exist. Moving to the stable-signed build therefore needs **one** uninstall:

```bash
# Back up first if you care about the accounts/settings:
adb backup -f microg.ab org.microg.android.gms   # optional, only on older Android

adb uninstall org.microg.android.gms             # or: adb uninstall app.revanced.android.gms
adb install org.microg.android.gms-255070302-personal-arm64-v8a.apk
```

After that single uninstall, every future build installs directly over the previous one.
