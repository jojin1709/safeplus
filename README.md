# SafePulse

SafePulse is a clean Android DNS/VPN protection app built by [Jojin John](https://www.linkedin.com/in/jojin-john/). It helps block ad, tracker, consent, redirect, and risky-domain requests across Android apps that use normal system DNS.

The app starts only when the user taps **Start protection**. It runs as an Android `VpnService` with a foreground notification and can be stopped from inside the app at any time.

SafePulse is designed to be simple by default. Normal users can use **Home**, **Activity**, **Settings**, **Check**, and **Help** without needing technical DNS knowledge. Expert controls stay inside **Settings > Advanced options**.

## Download

Get the latest APK from GitHub Releases:

```text
https://github.com/jojin1709/safeplus/releases/latest
```

Android may ask for permission to install apps from the browser or file manager. The app also asks for VPN approval because DNS filtering is handled through Android's VPN service APIs.

## Features

- DNS-level ad and tracker blocking
- Anti-tracking, never-consent, redirect-protection, and encrypted-DNS guard options
- Auto-updating remote blocklists
- Allowlist for domains that should not be blocked
- Per-app bypass controls
- First-install setup checklist for VPN, notification, battery, and Private DNS settings
- Live blocked/checked/allowed counters
- Recent blocked domain list and searchable logs
- Light and dark mode
- Responsive UI for phones, foldables, tablets, Android TV, and Fire TV
- Android TV / Fire TV launcher support with a remote-friendly interface
- In-app diagnostics screen for VPN, DNS, blocklist, Private DNS, battery, notification, and device-mode status
- Blocking self-test that checks the same rule decisions used by the VPN service
- Simple default settings with advanced DNS, allowlist, app bypass, and rule-source controls hidden until needed
- Optional **Strict YouTube blocking** in Advanced options for testing stronger YouTube blocking
- Free in-app update checker using GitHub Releases

## Important Limits

SafePulse is a DNS/VPN-style blocker. It can block domain lookups, but it cannot rewrite app screens like a browser extension.

- It cannot remove empty ad containers inside apps or websites.
- It cannot guarantee official YouTube app ad blocking because YouTube can serve ads and videos from shared Google video infrastructure.
- Apps using Private DNS, DoH, DoT, hardcoded DNS, or their own encrypted network stack may bypass DNS filtering.
- Blocking too aggressively can break videos, logins, payments, or app loading, so SafePulse keeps playback-critical hosts safer by default.
- Streaming apps on Fire TV may still show ads when ads and video streams come from the same service infrastructure.

SafePulse blocks known YouTube ad, stats, Google ads, and IMA SDK DNS hosts where it can do that safely. It does not block broad `googlevideo.com` delivery hosts because those hosts can carry the actual video stream.

Advanced users can turn on **Settings > Advanced options > Strict YouTube blocking** to test stronger blocking. This mode may block shared YouTube video/API hosts, so videos, thumbnails, or the official YouTube app may stop loading. Turn it off to return to normal video-friendly mode.

## Fire TV / Android TV

SafePulse includes Android TV launcher support, a TV banner, larger controls, and D-pad focus behavior for remotes. The same APK can be sideloaded onto compatible Android TV and Amazon Fire TV devices.

On Fire TV, install the APK from the latest GitHub Release, approve unknown-app installation if needed, then open SafePulse from the apps screen. The first start still requires Android's VPN permission so SafePulse can filter DNS requests locally.

## Updates

SafePulse includes **About > App updates**.

The updater checks:

```text
https://api.github.com/repos/jojin1709/safeplus/releases/latest
```

When a newer release is available, the app shows **Update now** and opens the APK download. Android still requires the user to approve the installation.

SafePulse uses a device-aware update picker:

- Phone and tablet builds should use `phone`, `mobile`, `tablet`, or `universal` in the APK file name.
- Android TV and Fire TV builds should use `tv`, `fire`, `leanback`, or `universal` in the APK file name.
- A universal APK can update every supported device when one APK is intended for all devices.
- Phone/tablet devices will not auto-pick a TV-only APK, and TV/Fire TV devices will not auto-pick a mobile-only APK.

For updates to work:

1. Keep the package id as `com.jojinjohn.safepulse`.
2. Increase `versionCode` in `app/build.gradle`.
3. Build the APK with the same release signing key.
4. Publish the APK in a new GitHub Release with a clear asset name, for example `SafePulse-universal-v1.4.0.apk`.

## Diagnostics

Open **Checks** in the bottom navigation to see:

- VPN running state
- VPN permission status
- selected DNS provider and upstream servers
- bundled and remote blocklist counts
- Private DNS status
- battery and notification status
- phone/tablet vs Android TV / Fire TV mode
- a blocking test for known ad, tracker, redirect, YouTube-safe, and normal domains

The blocking test is honest: it checks whether SafePulse's current rules would block the domain. If protection is stopped, it reports that the test is rules-only and not live traffic filtering.

## Build

Install Android Studio or the Android SDK, then run:

```bash
gradle assembleDebug
```

The debug APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For a signed release build, create `keystore.properties` from `keystore.properties.example`, create the matching local `.jks` key, then run:

```bash
gradle assembleRelease
```

The signed release APK is created at:

```text
app/build/outputs/apk/release/app-release.apk
```

Do not commit `keystore.properties` or any `.jks` file. The same release key is required for future app updates.

## GitHub Actions

This repository includes a free GitHub Actions workflow that builds the debug APK on pushes, pull requests, and manual runs. Signed release APKs still need the private release keystore and should be built locally or with encrypted CI secrets.

## Real-Device Testing

Before publishing a release, test on at least one Android phone and one Android TV / Fire TV device:

1. Install the latest universal APK.
2. Open SafePulse and approve VPN permission.
3. Complete the setup checklist.
4. Start protection.
5. Open **Checks** and run the blocking test.
6. Browse a normal website to confirm loading still works.
7. Open YouTube or a streaming app to confirm playback is not broken.

## Project Structure

```text
app/src/main/java/com/jojinjohn/safepulse/   Android Java source
app/src/main/res/                            UI resources and app icons
app/src/main/assets/blocklist.txt            Bundled starter blocklist
build-release.ps1                            Local release build helper
keystore.properties.example                  Signing config template
```

## Privacy

SafePulse runs locally on the device. DNS statistics and logs are stored locally for the dashboard. The in-app updater contacts GitHub Releases only to check whether a newer APK exists.

## Ownership

SafePulse, its source code, UI, logo, name, APK, and release assets are proprietary work by Jojin John.

This repository is public for transparency and release distribution, but it is **not open source**. See [LICENSE](LICENSE) for the full restrictions.
