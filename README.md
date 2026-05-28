# SafePulse APK

This is a small Android VPNService app for SafePulse (`com.jojinjohn.safepulse`). Protection starts only when the user taps **Start protection**. The service runs in the foreground with a notification, and reboot start is controlled by the in-app **Start after reboot** setting.

It blocks DNS lookups for domains listed in `app/src/main/assets/blocklist.txt` plus remote sources configured inside the app. This is not a direct port of the browser extension. Android apps cannot use Chrome/Firefox extension APIs to block every request across the phone, so a VPN/DNS-style service is the normal APK approach.

## Build

Install Android Studio or the Android SDK, then run:

```bash
gradle assembleDebug
```

The debug APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For a signed release build, create `keystore.properties` from `keystore.properties.example`, create the matching `.jks` key locally, then build:

```bash
gradle assembleRelease
```

The signed release APK is created at:

```text
app/build/outputs/apk/release/app-release.apk
```

Do not commit `keystore.properties` or any `.jks` file. They are ignored because the same release key is required for future app updates.

## Releases and Updates

GitHub pushes update the source code only. Existing Android users do not automatically receive updates just because code is pushed to GitHub.

To update an installed APK:

1. Increase `versionCode` in `app/build.gradle`.
2. Build a new signed release APK with the same release keystore.
3. Upload the APK to a new GitHub Release.
4. Users download and install the new APK, or the app needs a future in-app updater / Play Store distribution.

Android accepts the update only when the package id stays `com.jojinjohn.safepulse`, the APK is signed with the same key, and `versionCode` is higher than the installed version.

## Notes

- The UI is responsive for Android phones, small screens, foldables, and tablets. Content is centered on large screens and the dashboard adapts its stats layout for tablet width.
- DNS blocking works across apps that use normal system DNS.
- The default DNS provider is AdGuard, and the app automatically merges multiple remote blocklists.
- Allowlist, DNS provider, auto-update, boot start, module toggles, and per-app bypass controls are available in the Protection tab.
- The DNS engine handles IPv4 DNS, IPv6 VPN DNS packets, larger UDP responses, and TCP fallback for truncated DNS answers.
- It cannot guarantee ad-free YouTube in the official app. YouTube can serve ads and videos from the same Google video infrastructure, and blocking that infrastructure can stop videos too.
