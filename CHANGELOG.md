# Changelog

## v1.5.0

- Split into separate Phone and Fire TV APKs using product flavors.
- Phone APK: `com.jojinjohn.safepulse` with Quick Settings tile.
- TV APK: `com.jojinjohn.safepulse.tv` with leanback launcher, optimized for Fire TV remote.
- Added Android Quick Settings tile for easy on/off toggle from notification shade (Phone only).
- Strict YouTube blocking is now ON by default for stronger ad blocking.
- Expanded bundled blocklist from 49 to 223 domains covering more ad networks, trackers, and annoyances.
- Expanded hardcoded ad domain list from 57 to 110+ entries including popads, propellerads, exoclick, and more.
- Expanded tracker domain list from 21 to 52 entries including fullstory, clarity.ms, pendo, heap, and more.
- Added regional Google ad service domains for 15+ countries.
- Added Facebook/Meta ad infrastructure domains (fbcdn.net, graph.facebook.net).
- Added more consent management platforms and redirect/affiliate tracking domains.
- Added YouTube-specific ad label patterns (pagead, pubads, preroll, midroll, etc.).
- Expanded DoH bypass prevention with more DNS-over-HTTPS providers.
- Fixed VPN buffer size to handle full 65K IP packets.
- Fixed StatsStore to use in-memory caching with periodic disk flush for better performance.
- Stats are no longer wiped immediately when stopping protection.
- Fixed notification bitmap decoded on main thread (now cached).
- Added IP header length validation for malformed packets.
- Added concurrent blocklist update guard to prevent duplicate downloads.
- Enabled ProGuard/R8 minification and resource shrinking for release builds.
- Updated targetSdk to 34 for Play Store compliance.
- Added compileOptions for Java 11 compatibility.
- Added FOREGROUND_SERVICE_SPECIAL_USE permission for Android 14+.
- Added ACCESS_NETWORK_STATE permission for network connectivity checks.
- Fixed colorAccent mismatch (now matches app primary color).
- Fixed BootReceiver to handle QUICKBOOT_POWERON and add error handling.
- Added AndroidManifest foregroundServiceType for Android 14+.
- Cleaned up dead code ternaries for PendingIntent flags.
- Users can turn off Strict YouTube blocking in Advanced options if videos break.

## v1.4.0

- Added Advanced option for Strict YouTube blocking.
- Strict YouTube mode is off by default and can be turned off if videos get stuck.
- Updated the blocking test to show whether shared YouTube video delivery hosts are blocked in strict mode.

## v1.3.1

- Simplified the app navigation labels for normal users.
- Renamed technical settings into plain language.
- Moved DNS provider, app bypass, allowlist, and block rule source editing behind Advanced options.
- Simplified diagnostic and test wording.

## v1.3.0

- Added Checks tab with diagnostics for VPN, DNS, blocklists, Private DNS, battery, notifications, and device mode.
- Added blocking self-test using the same rule decisions as the VPN service.
- Added Privacy Policy link and Changelog link inside the app.
- Added GitHub Actions debug APK build workflow.
- Added more safe Google ads / IMA SDK DNS rules without blocking shared YouTube video delivery hosts.

## v1.2.1

- Added device-aware update asset selection to prevent phone/tablet and TV/Fire TV APK update collisions.
- Published a universal APK asset for all supported device types.

## v1.2.0

- Added Android TV and Fire TV launcher support.
- Added TV banner asset.
- Added D-pad focus behavior and larger TV controls.

## v1.1.0

- Added free in-app update checker using GitHub Releases.
- Added Update now flow from the About screen.

## v1.0.0

- Initial SafePulse Android APK release.
