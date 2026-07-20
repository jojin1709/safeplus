package com.jojinjohn.safepulse;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AppSettings {
    private static final int SETTINGS_VERSION = 12;
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
    private static final String KEY_STRICT_YOUTUBE = "strict_youtube";
    private static final String KEY_ANTI_TRACKING = "anti_tracking";
    private static final String KEY_NEVER_CONSENT = "never_consent";
    private static final String KEY_REDIRECT_PROTECTION = "redirect_protection";
    private static final String KEY_ADULT_BLOCK = "adult_block";
    private static final String KEY_DNS_PROVIDER = "dns_provider";
    private static final String KEY_CUSTOM_DNS = "custom_dns";
    private static final String KEY_BLOCKLIST_URL = "blocklist_url";
    private static final String KEY_ALLOWLIST = "allowlist";
    private static final String KEY_BYPASS_PACKAGES = "bypass_packages";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_PAUSE_UNTIL = "pause_until";
    private static final String KEY_SCHEDULE_ENABLED = "schedule_enabled";
    private static final String KEY_SCHEDULE_START_HOUR = "schedule_start_hour";
    private static final String KEY_SCHEDULE_START_MIN = "schedule_start_min";
    private static final String KEY_SCHEDULE_END_HOUR = "schedule_end_hour";
    private static final String KEY_SCHEDULE_END_MIN = "schedule_end_min";
    private static final String KEY_UPDATE_INTERVAL = "update_interval";

    public static final String DNS_CLOUDFLARE = "Cloudflare";
    public static final String DNS_GOOGLE = "Google";
    public static final String DNS_QUAD9 = "Quad9";
    public static final String DNS_ADGUARD = "AdGuard";
    public static final String DNS_CUSTOM = "Custom";

    public static final String PRESET_OISD = "OISD (Aggressive)";
    public static final String PRESET_ENERGIZED = "Energized Lite";
    public static final String PRESET_ADGUARD = "AdGuard Default";
    public static final String PRESET_DEFAULT = "Default (4 sources)";

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
                .putBoolean(KEY_STRICT_YOUTUBE, preferences.getBoolean(KEY_STRICT_YOUTUBE, false))
                .putBoolean(KEY_ANTI_TRACKING, preferences.getBoolean(KEY_ANTI_TRACKING, true))
                .putBoolean(KEY_NEVER_CONSENT, preferences.getBoolean(KEY_NEVER_CONSENT, true))
                .putBoolean(KEY_REDIRECT_PROTECTION, preferences.getBoolean(KEY_REDIRECT_PROTECTION, true))
                .putBoolean(KEY_ADULT_BLOCK, preferences.getBoolean(KEY_ADULT_BLOCK, false))
                .putString(KEY_DNS_PROVIDER, preferences.getString(KEY_DNS_PROVIDER, DNS_ADGUARD))
                .putString(KEY_CUSTOM_DNS, preferences.getString(KEY_CUSTOM_DNS, ""))
                .putString(KEY_BLOCKLIST_URL, existingBlocklistUrl)
                .putString(KEY_ALLOWLIST, preferences.getString(KEY_ALLOWLIST, ""))
                .putString(KEY_BYPASS_PACKAGES, preferences.getString(KEY_BYPASS_PACKAGES, ""))
                .putBoolean(KEY_DARK_MODE, preferences.getBoolean(KEY_DARK_MODE, false))
                .putLong(KEY_PAUSE_UNTIL, preferences.getLong(KEY_PAUSE_UNTIL, 0L))
                .putBoolean(KEY_SCHEDULE_ENABLED, preferences.getBoolean(KEY_SCHEDULE_ENABLED, false))
                .putInt(KEY_SCHEDULE_START_HOUR, preferences.getInt(KEY_SCHEDULE_START_HOUR, 22))
                .putInt(KEY_SCHEDULE_START_MIN, preferences.getInt(KEY_SCHEDULE_START_MIN, 0))
                .putInt(KEY_SCHEDULE_END_HOUR, preferences.getInt(KEY_SCHEDULE_END_HOUR, 7))
                .putInt(KEY_SCHEDULE_END_MIN, preferences.getInt(KEY_SCHEDULE_END_MIN, 0))
                .putString(KEY_UPDATE_INTERVAL, preferences.getString(KEY_UPDATE_INTERVAL, "24"));
        editor.apply();
    }

    // === Pause Timer ===
    public static boolean isPaused(Context context) {
        return System.currentTimeMillis() < getPauseUntil(context);
    }

    public static long getPauseUntil(Context context) {
        return prefs(context).getLong(KEY_PAUSE_UNTIL, 0L);
    }

    public static void pauseFor(Context context, long minutes) {
        long until = System.currentTimeMillis() + (minutes * 60 * 1000);
        prefs(context).edit().putLong(KEY_PAUSE_UNTIL, until).apply();
    }

    public static void resumeNow(Context context) {
        prefs(context).edit().putLong(KEY_PAUSE_UNTIL, 0L).apply();
    }

    // === Schedule ===
    public static boolean isScheduleEnabled(Context context) {
        return prefs(context).getBoolean(KEY_SCHEDULE_ENABLED, false);
    }

    public static void setScheduleEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply();
    }

    public static int getScheduleStartHour(Context context) {
        return prefs(context).getInt(KEY_SCHEDULE_START_HOUR, 22);
    }

    public static void setScheduleStartHour(Context context, int hour) {
        prefs(context).edit().putInt(KEY_SCHEDULE_START_HOUR, hour).apply();
    }

    public static int getScheduleStartMin(Context context) {
        return prefs(context).getInt(KEY_SCHEDULE_START_MIN, 0);
    }

    public static void setScheduleStartMin(Context context, int min) {
        prefs(context).edit().putInt(KEY_SCHEDULE_START_MIN, min).apply();
    }

    public static int getScheduleEndHour(Context context) {
        return prefs(context).getInt(KEY_SCHEDULE_END_HOUR, 7);
    }

    public static void setScheduleEndHour(Context context, int hour) {
        prefs(context).edit().putInt(KEY_SCHEDULE_END_HOUR, hour).apply();
    }

    public static int getScheduleEndMin(Context context) {
        return prefs(context).getInt(KEY_SCHEDULE_END_MIN, 0);
    }

    public static void setScheduleEndMin(Context context, int min) {
        prefs(context).edit().putInt(KEY_SCHEDULE_END_MIN, min).apply();
    }

    public static boolean isInScheduledPause(Context context) {
        if (!isScheduleEnabled(context)) return false;
        long now = System.currentTimeMillis();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int min = cal.get(java.util.Calendar.MINUTE);
        int current = hour * 60 + min;
        int start = getScheduleStartHour(context) * 60 + getScheduleStartMin(context);
        int end = getScheduleEndHour(context) * 60 + getScheduleEndMin(context);
        if (start > end) {
            return current >= start || current < end;
        }
        return current >= start && current < end;
    }

    // === Blocklist Presets ===
    public static String[] blocklistPresets() {
        return new String[]{PRESET_DEFAULT, PRESET_OISD, PRESET_ENERGIZED, PRESET_ADGUARD};
    }

    public static String getPresetUrls(String preset) {
        if (PRESET_OISD.equals(preset)) {
            return "https://big.oisd.nl/\nhttps://raw.githubusercontent.com/StevenBlack/hosts/master/hosts";
        }
        if (PRESET_ENERGIZED.equals(preset)) {
            return "https://blocklistproject.github.io/Lists/ads.txt\nhttps://blocklistproject.github.io/Lists/tracking.txt\nhttps://blocklistproject.github.io/Lists/porn.txt";
        }
        if (PRESET_ADGUARD.equals(preset)) {
            return "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt\nhttps://adguardteam.github.io/HostlistsRegistry/assets/filter_2.txt";
        }
        return DEFAULT_BLOCKLIST_URL;
    }

    // === Update Interval ===
    public static String getUpdateInterval(Context context) {
        return prefs(context).getString(KEY_UPDATE_INTERVAL, "24");
    }

    public static void setUpdateInterval(Context context, String hours) {
        prefs(context).edit().putString(KEY_UPDATE_INTERVAL, hours).apply();
    }

    public static long getUpdateIntervalMillis(Context context) {
        try {
            return Long.parseLong(getUpdateInterval(context)) * 3600 * 1000;
        } catch (Exception e) {
            return 24 * 3600 * 1000;
        }
    }

    // === Core Settings ===
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

    public static boolean isStrictYoutubeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_STRICT_YOUTUBE, false);
    }

    public static void setStrictYoutubeEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_STRICT_YOUTUBE, enabled).apply();
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

    public static boolean isAdultBlockEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ADULT_BLOCK, false);
    }

    public static void setAdultBlockEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ADULT_BLOCK, enabled).apply();
    }

    // === DNS ===
    public static String getDnsProvider(Context context) {
        return prefs(context).getString(KEY_DNS_PROVIDER, DNS_ADGUARD);
    }

    public static void setDnsProvider(Context context, String provider) {
        prefs(context).edit().putString(KEY_DNS_PROVIDER, provider).apply();
    }

    public static String getCustomDns(Context context) {
        return prefs(context).getString(KEY_CUSTOM_DNS, "");
    }

    public static void setCustomDns(Context context, String dns) {
        prefs(context).edit().putString(KEY_CUSTOM_DNS, dns.trim()).apply();
    }

    public static List<String> dnsProviders() {
        return Arrays.asList(DNS_CLOUDFLARE, DNS_GOOGLE, DNS_QUAD9, DNS_ADGUARD, DNS_CUSTOM);
    }

    public static String[] getDnsServers(Context context) {
        String provider = getDnsProvider(context);
        if (DNS_CUSTOM.equals(provider)) {
            String custom = getCustomDns(context);
            if (!custom.isEmpty()) {
                return new String[]{custom};
            }
            return new String[]{"94.140.14.14", "94.140.15.15"};
        }
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

    // === Blocklist ===
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

    // === Allowlist ===
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

    public static void removeAllowedDomain(Context context, String domain) {
        Set<String> allowlist = getAllowlist(context);
        allowlist.remove(normalizeDomain(domain));
        saveLines(context, KEY_ALLOWLIST, allowlist);
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

    // === Bypass Packages ===
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

    public static List<InstalledApp> getInstalledApps(Context context) {
        List<InstalledApp> apps = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo info : packages) {
            String name = pm.getApplicationLabel(info).toString();
            apps.add(new InstalledApp(info.packageName, name, isPackageBypassed(context, info.packageName)));
        }
        apps.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return apps;
    }

    // === Dark Mode ===
    public static boolean isDarkModeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkModeEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    // === Export / Import ===
    public static String exportSettings(Context context) {
        try {
            JSONObject json = new JSONObject();
            json.put("auto_update", isAutoUpdateEnabled(context));
            json.put("boot_start", isBootStartEnabled(context));
            json.put("doh_guard", isDohGuardEnabled(context));
            json.put("aggressive_blocking", isAggressiveBlockingEnabled(context));
            json.put("strict_youtube", isStrictYoutubeEnabled(context));
            json.put("anti_tracking", isAntiTrackingEnabled(context));
            json.put("never_consent", isNeverConsentEnabled(context));
            json.put("redirect_protection", isRedirectProtectionEnabled(context));
            json.put("adult_block", isAdultBlockEnabled(context));
            json.put("dns_provider", getDnsProvider(context));
            json.put("custom_dns", getCustomDns(context));
            json.put("blocklist_url", getBlocklistUrl(context));
            json.put("dark_mode", isDarkModeEnabled(context));
            json.put("schedule_enabled", isScheduleEnabled(context));
            json.put("schedule_start_hour", getScheduleStartHour(context));
            json.put("schedule_start_min", getScheduleStartMin(context));
            json.put("schedule_end_hour", getScheduleEndHour(context));
            json.put("schedule_end_min", getScheduleEndMin(context));
            JSONArray allowlistArr = new JSONArray();
            for (String d : getAllowlist(context)) allowlistArr.put(d);
            json.put("allowlist", allowlistArr);
            JSONArray bypassArr = new JSONArray();
            for (String p : getBypassPackages(context)) bypassArr.put(p);
            json.put("bypass_packages", bypassArr);
            return json.toString(2);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static boolean importSettings(Context context, String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            SharedPreferences.Editor editor = prefs(context).edit();
            if (json.has("auto_update")) editor.putBoolean(KEY_AUTO_UPDATE, json.getBoolean("auto_update"));
            if (json.has("boot_start")) editor.putBoolean(KEY_BOOT_START, json.getBoolean("boot_start"));
            if (json.has("doh_guard")) editor.putBoolean(KEY_DOH_GUARD, json.getBoolean("doh_guard"));
            if (json.has("aggressive_blocking")) editor.putBoolean(KEY_AGGRESSIVE_BLOCKING, json.getBoolean("aggressive_blocking"));
            if (json.has("strict_youtube")) editor.putBoolean(KEY_STRICT_YOUTUBE, json.getBoolean("strict_youtube"));
            if (json.has("anti_tracking")) editor.putBoolean(KEY_ANTI_TRACKING, json.getBoolean("anti_tracking"));
            if (json.has("never_consent")) editor.putBoolean(KEY_NEVER_CONSENT, json.getBoolean("never_consent"));
            if (json.has("redirect_protection")) editor.putBoolean(KEY_REDIRECT_PROTECTION, json.getBoolean("redirect_protection"));
            if (json.has("adult_block")) editor.putBoolean(KEY_ADULT_BLOCK, json.getBoolean("adult_block"));
            if (json.has("dns_provider")) editor.putString(KEY_DNS_PROVIDER, json.getString("dns_provider"));
            if (json.has("custom_dns")) editor.putString(KEY_CUSTOM_DNS, json.getString("custom_dns"));
            if (json.has("blocklist_url")) editor.putString(KEY_BLOCKLIST_URL, json.getString("blocklist_url"));
            if (json.has("dark_mode")) editor.putBoolean(KEY_DARK_MODE, json.getBoolean("dark_mode"));
            if (json.has("schedule_enabled")) editor.putBoolean(KEY_SCHEDULE_ENABLED, json.getBoolean("schedule_enabled"));
            if (json.has("schedule_start_hour")) editor.putInt(KEY_SCHEDULE_START_HOUR, json.getInt("schedule_start_hour"));
            if (json.has("schedule_start_min")) editor.putInt(KEY_SCHEDULE_START_MIN, json.getInt("schedule_start_min"));
            if (json.has("schedule_end_hour")) editor.putInt(KEY_SCHEDULE_END_HOUR, json.getInt("schedule_end_hour"));
            if (json.has("schedule_end_min")) editor.putInt(KEY_SCHEDULE_END_MIN, json.getInt("schedule_end_min"));
            if (json.has("allowlist")) {
                JSONArray arr = json.getJSONArray("allowlist");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.length(); i++) {
                    if (i > 0) sb.append("\n");
                    sb.append(arr.getString(i));
                }
                editor.putString(KEY_ALLOWLIST, sb.toString());
            }
            if (json.has("bypass_packages")) {
                JSONArray arr = json.getJSONArray("bypass_packages");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.length(); i++) {
                    if (i > 0) sb.append("\n");
                    sb.append(arr.getString(i));
                }
                editor.putString(KEY_BYPASS_PACKAGES, sb.toString());
            }
            editor.apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // === Helpers ===
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

    public static final class InstalledApp {
        public final String packageName;
        public final String name;
        public boolean bypassed;

        public InstalledApp(String packageName, String name, boolean bypassed) {
            this.packageName = packageName;
            this.name = name;
            this.bypassed = bypassed;
        }
    }
}
