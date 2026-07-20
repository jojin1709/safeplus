# SafePulse

SafePulse is a clean Android DNS/VPN protection app built by [Jojin John](https://www.linkedin.com/in/jojin-john/). It blocks ad, tracker, consent-wall, redirect, and risky-domain requests across Android apps that use normal system DNS.

The app starts only when the user taps **Start protection**. It runs as an Android `VpnService` with a foreground notification and can be stopped from inside the app at any time.

SafePulse is designed to be simple by default. Normal users can use **Home**, **Activity**, **Settings**, **Checks**, and **Help** without needing technical DNS knowledge. Expert controls stay inside **Settings > Advanced options**.

## Contents

- [Download](#download)
- [Features](#features)
- [Important Limits](#important-limits)
- [Fire TV / Android TV](#fire-tv--android-tv)
- [Updates](#updates)
- [Diagnostics](#diagnostics)
- [Build](#build)
- [GitHub Actions](#github-actions)
- [Real-Device Testing](#real-device-testing)
- [Project Structure](#project-structure)
- [Privacy](#privacy)
- [Ownership](#ownership)

## Download

Get the latest APK from GitHub Releases:

```text
https://github.com/jojin1709/safeplus/releases/latest
```

SafePulse is available as two separate APKs:

- **SafePulse-phone-v1.5.0.apk** — For Android phones and tablets. Includes a Quick Settings tile for easy toggle.
- **SafePulse-tv-v1.5.0.apk** — For Amazon Fire TV and Android TV. Optimized for remote-control navigation with a leanback launcher.

Android may ask for permission to install apps from the browser or file manager. The app also asks for VPN approval because DNS filtering is handled through Android's VPN service APIs.

## Features

- DNS-level ad and tracker blocking
- Anti-tracking, consent-wall blocking, redirect protection, and encrypted-DNS guard options
- Auto-updating remote blocklists
- Allowlist for domains that should not be blocked
- Per-app bypass controls
- First-install setup checklist for VPN, notification, battery, and Private DNS settings
- Live blocked/checked/allowed counters
- Recent blocked domain list and searchable logs
- Light and dark mode
- Responsive UI for phones, foldables, tablets, Android TV, and Fire TV
- Android TV / Fire TV launcher support with a remote-friendly interface
- Android Quick Settings tile for easy on/off toggle from the notification shade
- In-app diagnostics screen for VPN, DNS, blocklist, Private DNS, battery, notification, and device-mode status
- Blocking self-test that checks the same rule decisions used by the VPN service
- Simple default settings, with advanced DNS, allowlist, app-bypass, and rule-source controls hidden until needed
- Optional **Strict YouTube blocking** in Advanced options for stronger YouTube ad blocking (ON by default — see [Important Limits](#important-limits))
- Free in-app update checker using GitHub Releases

## Important Limits

SafePulse is a DNS/VPN-style blocker. It can block domain lookups, but it cannot rewrite app screens like a browser extension can.

- It cannot remove empty ad containers inside apps or websites.
- It cannot guarantee official YouTube app ad blocking, because YouTube can serve ads and videos from shared Google video infrastructure.
- Apps using Private DNS, DoH, DoT, hardcoded DNS, or their own encrypted network stack may bypass DNS filtering entirely.
- Blocking too aggressively can break videos, logins, payments, or app loading, so SafePulse keeps playback-critical hosts safer by default.
- Streaming apps on Fire TV may still show ads when ads and video streams come from the same service infrastructure.

SafePulse blocks known YouTube ad, stats, Google ads, and IMA SDK DNS hosts where it can do so safely. It does not block broad `googlevideo.com` delivery hosts, because those hosts can also carry the actual video stream.

Advanced users can turn on **Settings > Advanced options > Strict YouTube blocking** to test stronger blocking. This mode may block shared YouTube video/API hosts, so videos, thumbnails, or the official YouTube app may stop loading. Turn it off to return to normal, video-friendly mode. Strict YouTube blocking is **ON by default** for stronger ad blocking.

## Fire TV / Android TV

SafePulse includes Android TV launcher support, a TV banner, larger controls, and D-pad focus behavior for remotes. The TV APK is specifically optimized for Fire TV and Android TV devices.

On Fire TV: install the TV APK from the latest GitHub Release, approve unknown-app installation if needed, then open SafePulse from the apps screen. The first start still requires Android's VPN permission so SafePulse can filter DNS requests locally.

## Updates

SafePulse includes **About > App updates**.

The updater checks:

```text
https://api.github.com/repos/jojin1709/safeplus/releases/latest
```

When a newer release is available, the app shows **Update now** and opens the APK download. Android still requires the user to approve the installation.

SafePulse uses a device-aware update picker:

- Phone APK (`SafePulse-phone-v*.apk`) updates phone and tablet devices.
- TV APK (`SafePulse-tv-v*.apk`) updates Fire TV and Android TV devices.
- Each device type only picks its matching APK.

For updates to work:

1. Keep the package id as `com.jojinjohn.safepulse`.
2. Increase `versionCode` in `app/build.gradle`.
3. Build both APKs with the same release signing key.
4. Publish both APKs in a new GitHub Release with clear asset names:
   - `SafePulse-phone-v1.5.0.apk`
   - `SafePulse-tv-v1.5.0.apk`

## Diagnostics

Open **Checks** in the bottom navigation to see:

- VPN running state
- VPN permission status
- Selected DNS provider and upstream servers
- Bundled and remote blocklist counts
- Private DNS status
- Battery and notification status
- Phone/tablet vs. Android TV / Fire TV mode
- A blocking test for known ad, tracker, redirect, YouTube-safe, and normal domains

The blocking test is honest: it checks whether SafePulse's current rules would block the domain. If protection is stopped, it reports that the test is rules-only and not live traffic filtering.

## Build

Install Android Studio or the Android SDK, then run:

```bash
gradle assemblePhoneDebug    # Phone/tablet APK
gradle assembleTvDebug       # Fire TV / Android TV APK
```

The debug APKs are created at:

```text
app/build/outputs/apk/phone/debug/   # Phone APK
app/build/outputs/apk/tv/debug/      # TV APK
```

For a signed release build, create `keystore.properties` from `keystore.properties.example`, create the matching local `.jks` key, then run:

```bash
gradle assemblePhoneRelease   # Phone/tablet release APK
gradle assembleTvRelease      # Fire TV / Android TV release APK
```

Or use the build scripts:

```powershell
.\build-release.ps1    # Builds both Phone and TV release APKs
.\build-apk.ps1        # Builds both Phone and TV debug APKs
```

Do not commit `keystore.properties` or any `.jks` file. The same release key is required for future app updates.

## GitHub Actions

This repository includes a free GitHub Actions workflow that builds both Phone and TV debug APKs on pushes, pull requests, and manual runs. Signed release APKs still need the private release keystore, and should be built locally or with encrypted CI secrets.

## Real-Device Testing

Before publishing a release, test on at least one Android phone and one Fire TV / Android TV device:

1. Install the Phone APK on your phone and the TV APK on your Fire TV.
2. Open SafePulse and approve VPN permission.
3. Complete the setup checklist.
4. Start protection.
5. Open **Checks** and run the blocking test.
6. Browse a normal website to confirm loading still works.
7. Open YouTube or a streaming app to confirm playback is not broken.
8. Test the Quick Settings tile on phone (swipe down notification shade).

## Project Structure

```text
app/src/main/                     Shared code (VPN, blocklist, stats, UI)
app/src/phone/                    Phone-specific manifest + Quick Settings tile
app/src/tv/                       TV-specific manifest + leanback launcher
app/src/main/assets/blocklist.txt Bundled starter blocklist (223 domains)
build-release.ps1                 Release build script (both flavors)
build-apk.ps1                     Debug build script (both flavors)
keystore.properties.example       Signing config template
```

## Privacy

SafePulse runs locally on the device. DNS statistics and logs are stored locally for the dashboard. The in-app updater contacts GitHub Releases only to check whether a newer APK exists.

## Ownership

SafePulse, its source code, UI, logo, name, APK, and release assets are proprietary work by Jojin John.

This repository is public for transparency and release distribution, but it is **not open source**. See [LICENSE](LICENSE) for the full restrictions.
