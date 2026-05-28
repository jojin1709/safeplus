package com.jojinjohn.safepulse;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AppSettings {
    private static final int SETTINGS_VERSION = 8;
    private static final String OLD_DEFAULT_BLOCKLIST_URL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts";
    public static final String DEFAULT_BLOCKLIST_URL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts\n"
            + "https://raw.githubusercontent.com/AdAway/adaway.github.io/master/hosts.txt\n"
            + "https://big.oisd.nl/\n"
            + "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt";

    private static final String PREFS = "blocker_settings";
    private static final String KEY_SETTINGS_VERSION = "settings_version";
    private static final String KEY_AUTO_UPDATE = "auto_update";
    private static final String KEY_BOOT_START = "boot_start";
    private static final String KEY_DOH_GUARD = "doh_guard";
    private static final String KEY_AGGRESSIVE_BLOCKING = "aggressive_blocking";
    private static final String KEY_ANTI_TRACKING = "anti_tracking";
    private static final String KEY_NEVER_CONSENT = "never_consent";
    private static final String KEY_REDIRECT_PROTECTION = "redirect_protection";
    private static final String KEY_DNS_PROVIDER = "dns_provider";
    private static final String KEY_BLOCKLIST_URL = "blocklist_url";
    private static final String KEY_ALLOWLIST = "allowlist";
    private static final String KEY_BYPASS_PACKAGES = "bypass_packages";
    private static final String KEY_DARK_MODE = "dark_mode";

    public static final String DNS_CLOUDFLARE = "Cloudflare";
    public static final String DNS_GOOGLE = "Google";
    public static final String DNS_QUAD9 = "Quad9";
    public static final String DNS_ADGUARD = "AdGuard";

    private AppSettings() {
    }

    public static void migrateDefaults(Context context) {
        SharedPreferences preferences = prefs(context);
        if (preferences.getInt(KEY_SETTINGS_VERSION, 1) >= SETTINGS_VERSION) return;

        String existingBlocklistUrl = preferences.getString(KEY_BLOCKLIST_URL, DEFAULT_BLOCKLIST_URL);
        if (existingBlocklistUrl == null
                || existingBlocklistUrl.trim().isEmpty()
                || OLD_DEFAULT_BLOCKLIST_URL.equals(existingBlocklistUrl.trim())) {
            existingBlocklistUrl = DEFAULT_BLOCKLIST_URL;
        }

        SharedPreferences.Editor editor = preferences.edit()
                .putInt(KEY_SETTINGS_VERSION, SETTINGS_VERSION)
                .putBoolean(KEY_AUTO_UPDATE, preferences.getBoolean(KEY_AUTO_UPDATE, true))
                .putBoolean(KEY_BOOT_START, preferences.getBoolean(KEY_BOOT_START, false))
                .putBoolean(KEY_DOH_GUARD, preferences.getBoolean(KEY_DOH_GUARD, true))
                .putBoolean(KEY_AGGRESSIVE_BLOCKING, preferences.getBoolean(KEY_AGGRESSIVE_BLOCKING, true))
                .putBoolean(KEY_ANTI_TRACKING, preferences.getBoolean(KEY_ANTI_TRACKING, true))
                .putBoolean(KEY_NEVER_CONSENT, preferences.getBoolean(KEY_NEVER_CONSENT, true))
                .putBoolean(KEY_REDIRECT_PROTECTION, preferences.getBoolean(KEY_REDIRECT_PROTECTION, true))
                .putString(KEY_DNS_PROVIDER, preferences.getString(KEY_DNS_PROVIDER, DNS_ADGUARD))
                .putString(KEY_BLOCKLIST_URL, existingBlocklistUrl)
                .putString(KEY_ALLOWLIST, preferences.getString(KEY_ALLOWLIST, ""))
                .putString(KEY_BYPASS_PACKAGES, preferences.getString(KEY_BYPASS_PACKAGES, ""))
                .putBoolean(KEY_DARK_MODE, preferences.getBoolean(KEY_DARK_MODE, false));
        editor.apply();
    }

    public static boolean isAutoUpdateEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_UPDATE, true);
    }

    public static void setAutoUpdateEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply();
    }

    public static boolean isBootStartEnabled(Context context) {
        return prefs(context).getBoolean(KEY_BOOT_START, false);
    }

    public static void setBootStartEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_BOOT_START, enabled).apply();
    }

    public static boolean isDohGuardEnabled(Context context) {
        return prefs(context).getBoolean(KEY_DOH_GUARD, true);
    }

    public static void setDohGuardEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DOH_GUARD, enabled).apply();
    }

    public static boolean isAggressiveBlockingEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AGGRESSIVE_BLOCKING, true);
    }

    public static void setAggressiveBlockingEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AGGRESSIVE_BLOCKING, enabled).apply();
    }

    public static boolean isAntiTrackingEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ANTI_TRACKING, true);
    }

    public static void setAntiTrackingEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ANTI_TRACKING, enabled).apply();
    }

    public static boolean isNeverConsentEnabled(Context context) {
        return prefs(context).getBoolean(KEY_NEVER_CONSENT, true);
    }

    public static void setNeverConsentEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_NEVER_CONSENT, enabled).apply();
    }

    public static boolean isRedirectProtectionEnabled(Context context) {
        return prefs(context).getBoolean(KEY_REDIRECT_PROTECTION, true);
    }

    public static void setRedirectProtectionEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_REDIRECT_PROTECTION, enabled).apply();
    }

    public static String getDnsProvider(Context context) {
        return prefs(context).getString(KEY_DNS_PROVIDER, DNS_ADGUARD);
    }

    public static void setDnsProvider(Context context, String provider) {
        prefs(context).edit().putString(KEY_DNS_PROVIDER, provider).apply();
    }

    public static List<String> dnsProviders() {
        return Arrays.asList(DNS_CLOUDFLARE, DNS_GOOGLE, DNS_QUAD9, DNS_ADGUARD);
    }

    public static String[] getDnsServers(Context context) {
        String provider = getDnsProvider(context);
        if (DNS_GOOGLE.equals(provider)) {
            return new String[]{"8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"};
        }
        if (DNS_QUAD9.equals(provider)) {
            return new String[]{"9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9"};
        }
        if (DNS_ADGUARD.equals(provider)) {
            return new String[]{"94.140.14.14", "94.140.15.15", "2a10:50c0::ad1:ff", "2a10:50c0::ad2:ff"};
        }
        return new String[]{"1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"};
    }

    public static String getBlocklistUrl(Context context) {
        String source = prefs(context).getString(KEY_BLOCKLIST_URL, DEFAULT_BLOCKLIST_URL);
        if (source == null || source.trim().isEmpty() || OLD_DEFAULT_BLOCKLIST_URL.equals(source.trim())) {
            return DEFAULT_BLOCKLIST_URL;
        }
        return source;
    }

    public static List<String> getBlocklistUrls(Context context) {
        List<String> urls = new ArrayList<>();
        String raw = getBlocklistUrl(context);
        if (raw == null) return urls;
        for (String line : raw.split("\\n")) {
            String url = line.trim();
            if (!url.isEmpty() && !url.startsWith("#")) urls.add(url);
        }
        return urls;
    }

    public static void setBlocklistUrl(Context context, String url) {
        prefs(context).edit().putString(KEY_BLOCKLIST_URL, url.trim()).apply();
    }

    public static Set<String> getAllowlist(Context context) {
        return parseLines(prefs(context).getString(KEY_ALLOWLIST, ""));
    }

    public static boolean addAllowedDomain(Context context, String domain) {
        String normalized = normalizeDomain(domain);
        if (normalized.isEmpty()) return false;
        Set<String> allowlist = getAllowlist(context);
        boolean added = allowlist.add(normalized);
        saveLines(context, KEY_ALLOWLIST, allowlist);
        return added;
    }

    public static void clearAllowlist(Context context) {
        prefs(context).edit().putString(KEY_ALLOWLIST, "").apply();
    }

    public static boolean isAllowed(Context context, String host) {
        if (host == null || host.isEmpty()) return false;
        Set<String> allowlist = getAllowlist(context);
        String name = normalizeDomain(host);
        while (!name.isEmpty()) {
            if (allowlist.contains(name)) return true;
            int dot = name.indexOf('.');
            if (dot < 0) break;
            name = name.substring(dot + 1);
        }
        return false;
    }

    public static Set<String> getBypassPackages(Context context) {
        return parseLines(prefs(context).getString(KEY_BYPASS_PACKAGES, ""));
    }

    public static void setPackageBypassed(Context context, String packageName, boolean bypassed) {
        Set<String> packages = getBypassPackages(context);
        if (bypassed) {
            packages.add(packageName);
        } else {
            packages.remove(packageName);
        }
        saveLines(context, KEY_BYPASS_PACKAGES, packages);
    }

    public static boolean isPackageBypassed(Context context, String packageName) {
        return getBypassPackages(context).contains(packageName);
    }

    public static boolean isDarkModeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkModeEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    private static Set<String> parseLines(String raw) {
        Set<String> values = new LinkedHashSet<>();
        if (raw == null || raw.isEmpty()) return values;
        for (String line : raw.split("\\n")) {
            String normalized = normalizeDomain(line);
            if (!normalized.isEmpty()) values.add(normalized);
        }
        return values;
    }

    private static void saveLines(Context context, String key, Set<String> values) {
        List<String> sorted = new ArrayList<>(values);
        prefs(context).edit().putString(key, String.join("\n", sorted)).apply();
    }

    private static String normalizeDomain(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replace("https://", "").replace("http://", "");
        int slash = normalized.indexOf('/');
        if (slash >= 0) normalized = normalized.substring(0, slash);
        if (normalized.startsWith("*.")) normalized = normalized.substring(2);
        while (normalized.startsWith(".")) normalized = normalized.substring(1);
        return normalized;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
